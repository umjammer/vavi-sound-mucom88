package mucom88.driver;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

import dotnet4j.io.File;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.FileStream;
import dotnet4j.io.Path;
import dotnet4j.io.Stream;
import dotnet4j.util.compat.StringUtilities;
import dotnet4j.util.compat.TriConsumer;
import dotnet4j.util.compat.Tuple;
import mucom88.common.Common;
import mucom88.common.MubException;
import mucom88.common.MyEncoding;
import mucom88.common.iEncoding;
import musicDriverInterface.ChipAction;
import musicDriverInterface.ChipDatum;
import musicDriverInterface.GD3Tag;
import musicDriverInterface.MmlDatum;
import musicDriverInterface.Tag;
import musicDriverInterface.IDriver;
import vavi.util.Debug;
import vavi.util.serdes.Serdes;


public class Driver implements IDriver {

    public static final int cOPNAMasterClock = 7987200;
    public static final int cOPNBMasterClock = 8000000;
    public static final int cOPMMasterClock_X68k = 4000000;
    public static final int cOPMMasterClock_Normal = 3579545;

    public byte[][] pcm = new byte[6][];
    public int[] pcmStartPos = new int[6];

    private MubHeader header = null;
    private List<Tuple<String, String>> tags = null;
    private String[] pcmType = new String[6];
    private Consumer<ChipDatum> writeOPNAP;
    private Consumer<ChipDatum> writeOPNAS;
    private Consumer<ChipDatum> writeOPNBP;
    private Consumer<ChipDatum> writeOPNBS;
    private Consumer<ChipDatum> writeOPMP;
    private TriConsumer<byte[], Integer, Integer> writeOPNBAdpcmAP;
    private TriConsumer<byte[], Integer, Integer> writeOPNBAdpcmBP;
    private TriConsumer<byte[], Integer, Integer> writeOPNBAdpcmAS;
    private TriConsumer<byte[], Integer, Integer> writeOPNBAdpcmBS;
    private BiConsumer<Long, Integer> waitSendOPNA;
    private String[] fnVoiceDat = {"", "", "", ""};
    private String[] fnPcm = {"", "", "", "", "", ""};

    private int renderingFreq = 44100;

    private int opnaMasterClock = cOPNAMasterClock;
    private int opnbMasterClock = cOPNBMasterClock;
    private int opmMasterClock = cOPMMasterClock_Normal;

    private Work work = new Work();
    private Music2 music2 = null;
    private final Object lockObjWriteReg = new Object();

    public enum Command {
        MusicSTART, MusicSTOP, FaDeOut, EFfeCt, RETurnWork
    }

    private iEncoding enc = MyEncoding.Default();

    public void init(List<ChipAction> chipsConsumer, MmlDatum[] srcBuf, Function<String, Stream> appendFileReaderCallback, Object... additionalOption) {
        List<Consumer<ChipDatum>> lstChipWrite = new ArrayList<>();
        List<TriConsumer<byte[], Integer, Integer>> lstChipWriteAdpcm = new ArrayList<>();
        List<BiConsumer<Long, Integer>> lstChipWaitSend = new ArrayList<>();

        for (ChipAction ca : chipsConsumer) {
            lstChipWrite.add(ca::writeRegister);
            lstChipWriteAdpcm.add(ca::writePCMData);
            lstChipWaitSend.add(ca::waitSend);
        }
        initT(lstChipWrite, lstChipWriteAdpcm, lstChipWaitSend, srcBuf, additionalOption, appendFileReaderCallback);
    }

    private void init(
            String fileName,
            List<Consumer<ChipDatum>> lstChipWrite,
            List<TriConsumer<byte[], Integer, Integer>> lstChipWriteAdpcm,
            List<BiConsumer<Long, Integer>> opnaWaitSend,
            boolean notSoundBoard2, boolean isLoadADPCM, boolean loadADPCMOnly, Function<String, Stream> appendFileReaderCallback/* =null*/) {
        if (!Path.getExtension(fileName).equalsIgnoreCase(".xml")) {
            byte[] srcBuf = File.readAllBytes(fileName);
            if (srcBuf.length < 1) return;
            init(lstChipWrite, lstChipWriteAdpcm, opnaWaitSend, notSoundBoard2, srcBuf, isLoadADPCM, loadADPCMOnly, appendFileReaderCallback != null ? appendFileReaderCallback : createAppendFileReaderCallback(Path.getDirectoryName(fileName)));
        } else {
            try (InputStream sr = Files.newInputStream(java.nio.file.Path.of(fileName))) {
                List<MmlDatum> s = new ArrayList<>();
                while (sr.available() > 0) {
                    MmlDatum m = new MmlDatum();
                    s.add(Serdes.Util.deserialize(sr, m));
                }
                initT(lstChipWrite, lstChipWriteAdpcm, opnaWaitSend, s.toArray(MmlDatum[]::new), new Object[] {notSoundBoard2, isLoadADPCM, loadADPCMOnly}, appendFileReaderCallback);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void init(String fileName, List<Consumer<ChipDatum>> lstChipWrite, List<TriConsumer<byte[], Integer, Integer>> lstChipWriteAdpcm, List<BiConsumer<Long, Integer>> opnaWaitSend, boolean notSoundBoard2, byte[] srcBuf, boolean isLoadADPCM, boolean loadADPCMOnly) {
        if (srcBuf == null || srcBuf.length < 1) return;
        init(lstChipWrite, lstChipWriteAdpcm, opnaWaitSend, notSoundBoard2, srcBuf, isLoadADPCM, loadADPCMOnly, createAppendFileReaderCallback(Path.getDirectoryName(fileName)));
    }

    private void init(List<Consumer<ChipDatum>> lstChipWrite, List<TriConsumer<byte[], Integer, Integer>> lstChipWriteAdpcm, List<BiConsumer<Long, Integer>> opnaWaitSend, boolean notSoundBoard2, byte[] srcBuf, boolean isLoadADPCM, boolean loadADPCMOnly, Function<String, Stream> appendFileReaderCallback) {
        if (srcBuf == null || srcBuf.length < 1) return;
        List<MmlDatum> bl = new ArrayList<>();
        for (byte b : srcBuf) bl.add(new MmlDatum(b & 0xff));
        initT(lstChipWrite, lstChipWriteAdpcm, opnaWaitSend, bl.toArray(MmlDatum[]::new), new Object[] {notSoundBoard2, isLoadADPCM, loadADPCMOnly}, appendFileReaderCallback);
    }

    private void init(String fileName, List<Consumer<ChipDatum>> lstChipWrite, List<TriConsumer<byte[], Integer, Integer>> lstChipWriteAdpcm, List<BiConsumer<Long, Integer>> chipWaitSend, MmlDatum[] srcBuf, Object addtionalOption) {
        if (srcBuf == null || srcBuf.length < 1) return;
        initT(lstChipWrite, lstChipWriteAdpcm, chipWaitSend, srcBuf, addtionalOption, createAppendFileReaderCallback(Path.getDirectoryName(fileName)));
    }

    private void initT(List<Consumer<ChipDatum>> lstChipWrite, List<TriConsumer<byte[], Integer, Integer>> lstChipWriteAdpcm, List<BiConsumer<Long, Integer>> chipWaitSend, MmlDatum[] srcBuf, Object addtionalOption, Function<String, Stream> appendFileReaderCallback) {
        if (srcBuf == null || srcBuf.length < 1) return;

        boolean notSoundBoard2 = (boolean) ((Object[]) addtionalOption)[0];
        boolean isLoadADPCM = (boolean) ((Object[]) addtionalOption)[1];
        boolean loadADPCMOnly = (boolean) ((Object[]) addtionalOption)[2];
        String filename = (String) ((Object[]) addtionalOption)[3];
        appendFileReaderCallback = appendFileReaderCallback != null ? appendFileReaderCallback : createAppendFileReaderCallback(Path.getDirectoryName(filename));

        work = new Work();
        header = new MubHeader(srcBuf);
        work.mData = getDATA();
        work.setHeader(header);
        tags = getTags();
        getFileNameFromTag();
        for (int i = 0; i < 4; i++) {
            work.fmVoice[i] = getFMVoiceFromFile(i, appendFileReaderCallback);
            pcm[i] = getPCMFromSrcBuf(i) != null ? getPCMFromSrcBuf(i) : getPCMDataFromFile(i, appendFileReaderCallback);
            work.pcmTables[i] = getPCMTable(i);
        }
        for (int i = 4; i < 6; i++) {
            pcm[i] = getPCMFromSrcBuf(i) != null ? getPCMFromSrcBuf(i) : getPCMDataFromFile(i, appendFileReaderCallback);
            work.pcmTables[i] = getPCMTable(i);
        }

        if (pcm[2] != null && pcmType[2].isEmpty()) {
            transformOPNAPCMtoOPNBPCM(2);
            pcmStartPos[2] = 0;
        }
        if (pcm[3] != null && pcmType[3].isEmpty()) {
            transformOPNAPCMtoOPNBPCM(3);
            pcmStartPos[3] = 0;
        }
        if (pcm[4] != null && pcmType[4].isEmpty()) {
            transformOPNAPCMtoOPNBPCM(4);
            pcmStartPos[4] = 0;
        }
        if (pcm[5] != null && pcmType[5].isEmpty()) {
            transformOPNAPCMtoOPNBPCM(5);
            pcmStartPos[5] = 0;
        }

        work.isDotNET = isDotNETFromTAG();
        work.SSGExtend = isSSGExtendFromTAG();

        writeOPNAP = lstChipWrite.get(0);
        writeOPNAS = lstChipWrite.get(1);
        writeOPNBP = lstChipWrite.get(2);
        writeOPNBS = lstChipWrite.get(3);
        writeOPMP = lstChipWrite.get(4);
        writeOPNBAdpcmAP = lstChipWriteAdpcm.get(2);
        writeOPNBAdpcmBP = lstChipWriteAdpcm.get(2);
        writeOPNBAdpcmAS = lstChipWriteAdpcm.get(3);
        writeOPNBAdpcmBS = lstChipWriteAdpcm.get(3);
        waitSendOPNA = chipWaitSend.get(0);

        // PCMを送信する
        if (pcm != null) {
            if (isLoadADPCM) {
                for (int i = 0; i < 2; i++) {
                    if (pcm[i] == null) continue;
                    ChipDatum[] pcmSendData = getPCMSendData(0, i, 0);

                    var sw = System.currentTimeMillis();
                    if (i == 0) for (ChipDatum dat : pcmSendData) writeOPNAPRegister(dat);
                    if (i == 1) for (ChipDatum dat : pcmSendData) writeOPNASRegister(dat);

                    waitSendOPNA.accept(sw - System.currentTimeMillis(), pcmSendData.length);
                }

                List<Byte> buf = new ArrayList<>();
                if (pcm[2] != null) {
                    buf.clear();
                    for (int i = pcmStartPos[2]; i < pcm[2].length; i++) buf.add(pcm[2][i]);
                    writeOPNBPAdpcmB(mdsound.Common.toByteArray(buf));
                }
                if (pcm[3] != null) {
                    buf.clear();
                    for (int i = pcmStartPos[3]; i < pcm[3].length; i++) buf.add(pcm[3][i]);
                    writeOPNBPAdpcmB(mdsound.Common.toByteArray(buf));
                }
                if (pcm[4] != null) {
                    buf.clear();
                    for (int i = pcmStartPos[4]; i < pcm[4].length; i++) buf.add(pcm[4][i]);
                    writeOPNBPAdpcmA(mdsound.Common.toByteArray(buf));
                }
                if (pcm[5] != null) {
                    buf.clear();
                    for (int i = pcmStartPos[5]; i < pcm[5].length; i++) buf.add(pcm[5][i]);
                    writeOPNBPAdpcmA(mdsound.Common.toByteArray(buf));
                }
            }
        }

        if (loadADPCMOnly) return;

        music2 = new Music2(work, this::writeOPNAPRegister, this::writeOPNASRegister, this::writeOPNBPRegister, this::writeOPNBSRegister, this::writeOPMPRegister);
        music2.notSoundBoard2 = notSoundBoard2;
    }

    public void setMuteFlag(int chip, int ch, int page, boolean flg) {
        if (music2 == null) return;
        music2.setMuteFlag(chip, ch, page, flg);
    }

    public void setAllMuteFlag(boolean flg) {
        if (music2 == null) return;
        music2.setAllMuteFlag(flg);
    }

    private void transformOPNAPCMtoOPNBPCM(int v) {
        List<List<Byte>> pcmData = new ArrayList<>();
        List<Byte> dest = new ArrayList<>(0);
        for (int i = 0; i < work.pcmTables[v].length; i++) {
            pcmData.add(new ArrayList<>());
            List<Byte> one = pcmData.get(i);
            for (int p = (work.pcmTables[v][i].getItem2()[0] << 2); p < (work.pcmTables[v][i].getItem2()[1] << 2) + 16; p++) {
                one.add(pcm[v][p + 0x400]); // 0x400 ヘッダのサイズ
            }
        }

        int tblPtr = 0;
        for (int i = 0; i < work.pcmTables[v].length; i++) {
            dest.addAll(pcmData.get(i));
            for (int j = 0; j < 256 - (pcmData.get(i).size() % 256); j++) dest.add((byte) 0x00);

            short stAdr = (short) (tblPtr >> 8);
            int length = pcmData.get(i).size() + 256 - (pcmData.get(i).size() % 256);
            tblPtr += length != 0 ? (length - 0x100) : 0;
            short edAdr = (short) (tblPtr >> 8);
            tblPtr += length != 0 ? 0x100 : 0;
            work.pcmTables[v][i] = new Tuple<>(work.pcmTables[v][i].getItem1(), new short[] {stAdr, edAdr, 0, work.pcmTables[v][i].getItem2()[3]});
        }
        pcm[v] = mdsound.Common.toByteArray(dest);
    }

    private boolean isDotNETFromTAG() {
        if (tags == null) return false;
        for (Tuple<String, String> tag : tags) {
            if (tag.getItem1().equals("driver")) {
                if (tag.getItem2().equalsIgnoreCase("mucomdotnet")) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isSSGExtendFromTAG() {
        if (tags == null) return false;
        for (Tuple<String, String> tag : tags) {
            if (tag.getItem1().equals("ssgextend")) {
                String ssgextval = tag.getItem2().toLowerCase();
                if (ssgextval.equals("on") || ssgextval.equals("yes") || ssgextval.equals("y") || ssgextval.equals("1") || ssgextval.equals("true") || ssgextval.equals("t")) {
                    return true;
                }
            }
        }

        return false;
    }

    private Function<String, Stream> createAppendFileReaderCallback(String dir) {
        return fileName -> {
            if (!StringUtilities.isNullOrEmpty(dir)) {
                var path = Path.combine(dir, fileName);
                if (File.exists(path)) {
                    return new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.Read);
                }
            }
            if (File.exists(fileName)) {
                return new FileStream(fileName, FileMode.Open, FileAccess.Read, FileShare.Read);
            }
            return null;
        };
    }

    //
    // data Information
    //

    public MmlDatum[] getDATA() {
        return header.getDATA();
    }

    public List<Tuple<String, String>> getTags() {
        if (header == null) {
            throw new MubException("Header information not found.");
        }
        return header.getTags();
    }

    public byte[] getPCMFromSrcBuf(int id) {
        if (header.mupb == null)
            return header.getPCM(id);
        else {
            if (header.mupb.getPcms().length <= id) return null;
            return (header.mupb.getPcms()[id].getData() == null || header.mupb.getPcms()[id].getData().length < 1) ? null : header.mupb.getPcms()[id].getData();
        }
    }

    public Tuple<String, short[]>[] getPCMTable(int id) {
        if (pcm == null) return null;
        if (pcm[id] == null) return null;

        List<Tuple<String, short[]>> pcmTable = new ArrayList<>();
        int infTable = 0x0000;
        int adr, whl, eadr;
        byte[] pcmName = new byte[17];
        int maxPcm = 32;

        String fcc = "";
        if (pcm[id].length > 4)
            fcc = String.valueOf((char) pcm[id][0]) + ((char) pcm[id][1]) + ((char) pcm[id][2]) + ((char) pcm[id][3]);
        pcmType[id] = fcc;
        switch (fcc) {
        case "mda ": // OPNA ADPCM
        case "mdbb": // OPNB ADPCM-B
        case "mdba": // OPNB ADPCM-A
            int cnt = pcm[id][4] + (pcm[id][5] << 8) + 1;
            int p = 6;
            for (int i = 0; i < cnt; i++) {
                List<Byte> b = new ArrayList<>();
                while (pcm[id][p] != 0x0) b.add(pcm[id][p++]);
                String item1 = enc.getStringFromSjisArray(mdsound.Common.toByteArray(b));
                p++;
                p++;
                short[] item2 = new short[4];
                item2[0] = (short) (pcm[id][p + 2] | (pcm[id][p + 3] * 0x100));
                item2[1] = (short) (pcm[id][p + 4] | (pcm[id][p + 5] * 0x100));
                item2[2] = (short) 0;
                item2[3] = (short) (pcm[id][p + 0] | (pcm[id][p + 1] * 0x100));
                Tuple<String, short[]> pd = new Tuple<>(item1, item2);
                pcmTable.add(pd);
                p += 6;
            }
            pcmStartPos[id] = p;
            break;
        default: // mucom88
            pcmType[id] = "";
            for (int i = 0; i < maxPcm; i++) {
                adr = pcm[id][infTable + 28] | (pcm[id][infTable + 29] * 0x100); // >>2済み開始アドレス
                whl = pcm[id][infTable + 30] | (pcm[id][infTable + 31] * 0x100); // 生レングス
                eadr = adr + (whl >> 2); // !
                if (pcm[id][i * 32] != 0) {
                    short[] item2 = new short[4];
                    item2[0] = (short) adr;
                    item2[1] = (short) eadr;
                    item2[2] = (short) 0;
                    item2[3] = (short) (pcm[id][infTable + 26] | (pcm[id][infTable + 27] * 0x100));
                    System.arraycopy(pcm[id], i * 32, pcmName, 0, 16);
                    pcmName[16] = 0;
                    String item1 = enc.getStringFromSjisArray(pcmName);

                    Tuple<String, short[]> pd = new Tuple<>(item1, item2);
                    pcmTable.add(pd);
                    //Debug.printf("#PCM%d $%04x $%04x %s", i + 1, adr, eadr, new String(pcmName, Charset.forName("shift_jis")));
                }
                infTable += 32;
            }
            pcmStartPos[id] = 0x400;
            break;
        }

        return pcmTable.toArray(Tuple[]::new);
    }

    public ChipDatum[] getPCMSendData(int c, int id, int tp) {
        if (pcm == null) return null;
        if (pcm[id] == null) return null;
        if (c != 0) return null;
        if (tp != 0) return null;

        int startAddress = 0;
        List<ChipDatum> dat = Arrays.asList(
                new ChipDatum(0, 0x29, 0x83), // CH 4-6 ENABLE
                new ChipDatum(0x1, 0x00, 0x20),
                new ChipDatum(0x1, 0x00, 0x21),
                new ChipDatum(0x1, 0x00, 0x00),

                new ChipDatum(0x1, 0x10, 0x00),
                new ChipDatum(0x1, 0x10, 0x80),

                new ChipDatum(0x1, 0x00, 0x61),
                new ChipDatum(0x1, 0x00, 0x68),
                new ChipDatum(0x1, 0x01, 0x00),
                new ChipDatum(0x1, 0x02, (byte) ((startAddress >> 2) & 0xff)),
                new ChipDatum(0x1, 0x03, (byte) ((startAddress >> 10) & 0xff)),
                new ChipDatum(0x1, 0x04, 0xff),
                new ChipDatum(0x1, 0x05, 0xff),
                new ChipDatum(0x1, 0x0c, 0xff),
                new ChipDatum(0x1, 0x0d, 0xff)
        );

        // データ転送
        int infoSize = pcmStartPos[id];
        for (int i = 0; i < pcm[id].length - infoSize; i++) {
            dat.add(new ChipDatum(0x1, 0x08, pcm[id][infoSize + i]));
            //Debug.printf("#PCMDATA adr:%04x dat:%02x", (infoSize + i) >> 2, pcmdata[infoSize + i]);
        }
        dat.add(new ChipDatum(0x1, 0x00, 0x00));
        dat.add(new ChipDatum(0x1, 0x10, 0x80));

        return dat.toArray(ChipDatum[]::new);
    }

    //
    // rendering
    //

    public void startRendering(int renderingFreq, Tuple<String, Integer>... chipMasterClocks) {
        synchronized (work.systemInterrupt) {

            work.timeCounter = 0L;
            this.renderingFreq = renderingFreq <= 0 ? 44100 : renderingFreq;
            this.opnaMasterClock = 7987200;
            if (chipMasterClocks != null && chipMasterClocks.length > 0) {
                this.opnaMasterClock = chipMasterClocks[0].getItem2() <= 0 ? 7987200 : chipMasterClocks[0].getItem2();
            }
            if (chipMasterClocks != null && chipMasterClocks.length > 2) {
                this.opnbMasterClock = chipMasterClocks[2].getItem2() <= 0 ? 8000000 : chipMasterClocks[2].getItem2();
            }
            if (chipMasterClocks != null && chipMasterClocks.length > 4) {
                this.opmMasterClock = chipMasterClocks[4].getItem2() <= 0 ? 3579545 : chipMasterClocks[4].getItem2();
            }
            work.timerOPNA1 = new OPNATimer(renderingFreq, opnaMasterClock);
            work.timerOPNA2 = new OPNATimer(renderingFreq, opnaMasterClock);
            Debug.printf(Level.FINEST, "OPNA MasterClock %d", opnaMasterClock);
            work.timerOPNB1 = new OPNATimer(renderingFreq, opnbMasterClock);
            work.timerOPNB2 = new OPNATimer(renderingFreq, opnbMasterClock);
            Debug.printf(Level.FINEST, "OPNB MasterClock %d", opnbMasterClock);
            work.timerOPM = new OPMTimer(renderingFreq, opmMasterClock);
            Debug.printf(Level.FINEST, "OPM  MasterClock %d", opmMasterClock);
            Debug.printf(Level.FINEST, "Start rendering.");
        }
    }

    public void stopRendering() {
        synchronized (work.systemInterrupt) {
            if (work.getStatus() > 0) work.setStatus(0);
            Debug.printf(Level.FINEST, "Stop rendering.");
        }
    }

    public void render() {
        if (work.getStatus() < 0) return;

        try {
            music2.render();
        } catch (Exception e) {
            work.setStatus(-1);
            throw e;
        }
    }

    public void writeOPNAPRegister(ChipDatum reg) {
        synchronized (lockObjWriteReg) {
            if (reg.port == 0) {
                boolean ret = work.timerOPNA1.writeReg((byte) reg.address, (byte) reg.data);
                if (ret)
                    work.currentTimer = 0;
            }
            writeOPNAP.accept(reg);
        }
    }

    public void writeOPNASRegister(ChipDatum reg) {
        synchronized (lockObjWriteReg) {
            if (reg.port == 0) {
                boolean ret = work.timerOPNA2.writeReg((byte) reg.address, (byte) reg.data);
                if (ret)
                    work.currentTimer = 1;
            }
            writeOPNAS.accept(reg);
        }
    }

    public void writeOPNBPRegister(ChipDatum reg) {
        synchronized (lockObjWriteReg) {
            if (reg.port == 0) {
                boolean ret = work.timerOPNB1.writeReg((byte) reg.address, (byte) reg.data);
                if (ret)
                    work.currentTimer = 2;
            }
            writeOPNBP.accept(reg);
        }
    }

    public void writeOPNBSRegister(ChipDatum reg) {
        synchronized (lockObjWriteReg) {
            if (reg.port == 0) {
                boolean ret = work.timerOPNB2.writeReg((byte) reg.address, (byte) reg.data);
                if (ret)
                    work.currentTimer = 3;
            }
            writeOPNBS.accept(reg);
        }
    }

    public void writeOPMPRegister(ChipDatum reg) {
        synchronized (lockObjWriteReg) {
            boolean ret = work.timerOPM.writeReg((byte) reg.address, (byte) reg.data);
            if (ret)
                work.currentTimer = 4;
            writeOPMP.accept(reg);
        }
    }

    public void writeOPNBPAdpcmA(byte[] pcmdata) {
        if (pcmdata == null) return;
        synchronized (lockObjWriteReg) {
            writeOPNBAdpcmAP.accept(pcmdata, 0, 0);
        }
    }

    public void writeOPNBPAdpcmB(byte[] pcmdata) {
        if (pcmdata == null) return;
        synchronized (lockObjWriteReg) {
            writeOPNBAdpcmBP.accept(pcmdata, 1, 0);
        }
    }

    public void writeOPNBSAdpcmA(byte[] pcmdata) {
        if (pcmdata == null) return;
        synchronized (lockObjWriteReg) {
            writeOPNBAdpcmAS.accept(pcmdata, 0, 0);
        }
    }

    public void writeOPNBSAdpcmB(byte[] pcmdata) {
        if (pcmdata == null) return;
        synchronized (lockObjWriteReg) {
            writeOPNBAdpcmBS.accept(pcmdata, 1, 0);
        }
    }

    //
    // Command
    //

    public void startMusic(int musicNumber) {
        Debug.printf(Level.FINEST, "演奏開始");
        music2.MSTART(musicNumber);
        music2.SkipCount((int) header.jumpCount);
    }

    public void stopMusic() {
        Debug.printf(Level.FINEST, "演奏停止");
        music2.MSTOP();
    }

    public void fadeOut() {
        Debug.printf(Level.FINEST, "フェードアウト");
        music2.FDO();
    }

    public Object getWork() {
        Debug.printf(Level.FINEST, "ワークエリア取得");
        return music2.RETW();
    }

    public void shotEffect() {
        Debug.printf(Level.FINEST, "効果音");
        music2.EFC();
    }

    public int getStatus() {
        return work.getStatus();
    }

    private void getFileNameFromTag() {
        if (tags == null) return;
        for (Tuple<String, String> tag : tags) {
            switch (tag.getItem1()) {
            case "voice":
                fnVoiceDat[0] = tag.getItem2();
                break;
            case "pcm":
                fnPcm[0] = tag.getItem2();
                break;
            case "pcmOPNA_P":
                fnPcm[0] = tag.getItem2();
                break;
            case "pcmOPNA_S":
                fnPcm[1] = tag.getItem2();
                break;
            case "pcmOPNB_B_P":
                fnPcm[2] = tag.getItem2();
                break;
            case "pcmOPNB_B_S":
                fnPcm[3] = tag.getItem2();
                break;
            case "pcmOPNB_A_P":
                fnPcm[4] = tag.getItem2();
                break;
            case "pcmOPNB_A_S":
                fnPcm[5] = tag.getItem2();
                break;
            }
        }
    }

    private byte[] getFMVoiceFromFile(int id, Function<String, Stream> appendFileReaderCallback) {
        try {
            fnVoiceDat[id] = StringUtilities.isNullOrEmpty(fnVoiceDat[id]) ? "voice.dat" : fnVoiceDat[id];

            try (Stream vd = appendFileReaderCallback.apply(fnVoiceDat[id])) {
                return mdsound.Common.readAllBytes(vd);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final String[] defaultPCMFileName = new String[] {
            "mucompcm.bin",
            "mucompcm_2nd.bin",
            "mucompcm_3rd_B.bin",
            "mucompcm_4th_B.bin",
            "mucompcm_3rd_A.bin",
            "mucompcm_4th_A.bin"
    };

    private byte[] getPCMDataFromFile(int id, Function<String, Stream> appendFileReaderCallback) {
        try {
            fnPcm[id] = StringUtilities.isNullOrEmpty(fnPcm[id]) ? defaultPCMFileName[id] : fnPcm[id];

            try (Stream pd = appendFileReaderCallback.apply(fnPcm[id])) {
                return mdsound.Common.readAllBytes(pd);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int setLoopCount(int loopCounter) {
        work.maxLoopCount = loopCounter;
        return 0;
    }

    public GD3Tag getGD3TagInfo(byte[] srcBuf) {
        int tagData = Common.getLE32(srcBuf, 0x000c);
        int tagsize = Common.getLE32(srcBuf, 0x0010);
        if (srcBuf[0] == 'm' && srcBuf[1] == 'u' && srcBuf[2] == 'P' && srcBuf[3] == 'b') {
            tagData = Common.getLE32(srcBuf, 0x0012);
            tagsize = Common.getLE32(srcBuf, 0x0016);
        }

        if (tagData == 0) return null;

        List<Byte> lb = new ArrayList<>();
        for (int i = 0; i < tagsize; i++) {
            lb.add(srcBuf[tagData + i]);
        }

        List<Tuple<String, String>> tags = getTagsByteArray(mdsound.Common.toByteArray(lb));
        GD3Tag gt = new GD3Tag();

        for (Tuple<String, String> tag : tags) {
            switch (tag.getItem1()) {
            case "title":
                addItemAry(gt, Tag.Title, tag.getItem2());
                addItemAry(gt, Tag.TitleJ, tag.getItem2());
                break;
            case "composer":
                addItemAry(gt, Tag.Composer, tag.getItem2());
                addItemAry(gt, Tag.ComposerJ, tag.getItem2());
                break;
            case "author":
                addItemAry(gt, Tag.Artist, tag.getItem2());
                addItemAry(gt, Tag.ArtistJ, tag.getItem2());
                break;
            case "comment":
                addItemAry(gt, Tag.Note, tag.getItem2());
                break;
            case "mucom88":
                addItemAry(gt, Tag.RequestDriverVersion, tag.getItem2());
                break;
            case "date":
                addItemAry(gt, Tag.ReleaseDate, tag.getItem2());
                break;
            case "driver":
                addItemAry(gt, Tag.DriverName, tag.getItem2());
                break;
            }
        }

        return gt;
    }

    private List<Tuple<String, String>> getTagsByteArray(byte[] buf) {
        var text = Arrays.stream(enc.getStringFromSjisArray(buf).split("\n"))
                .filter(x -> x.indexOf("#") == 0).toArray(String[]::new);

        List<Tuple<String, String>> tags = new ArrayList<>();
        for (String v : text) {
            try {
                int p = v.indexOf(' ');
                String tag = "";
                String ele = "";
                if (p >= 0) {
                    tag = v.substring(1, 1 + p).trim().toLowerCase();
                    ele = v.substring(p + 1).trim();
                    Tuple<String, String> item = new Tuple<String, String>(tag, ele);
                    tags.add(item);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return tags;
    }

    private void addItemAry(GD3Tag gt, Tag tag, String item) {
        if (!gt.items.containsKey(tag))
            gt.items.put(tag, new String[] {item});
        else {
            String[] dmy = gt.items.get(tag);
            dmy = new String[dmy.length + 1];
            dmy[dmy.length - 1] = item;
            gt.items.put(tag, dmy);
        }
    }

    public int getNowLoopCounter() {
        try {
            return work.nowLoopCounter;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void setDriverSwitch(Object... param) {
        if (param[0] instanceof String) {
            String cmd = (String) param[0];
            if (cmd.equals("AllMute")) {
                setAllMuteFlag((boolean) param[1]);
            } else if (cmd.equals("SetMute")) {
                setMuteFlag((int) param[1], (int) param[2], (int) param[3], (boolean) param[4]);
            }

        }
    }

    public void writeRegister(ChipDatum reg) {
        throw new UnsupportedOperationException();
    }

    public byte[] getPCMFromSrcBuf() {
        throw new UnsupportedOperationException();
    }

    public Tuple<String, short[]>[] getPCMTable() {
        throw new UnsupportedOperationException();
    }

    public ChipDatum[] getPCMSendData() {
        throw new UnsupportedOperationException();
    }
}
