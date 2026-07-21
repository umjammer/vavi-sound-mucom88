package mucom88.driver;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import mucom88.common.MubException;
import musicDriverInterface.ChipAction;
import musicDriverInterface.ChipDatum;
import musicDriverInterface.IDriver;
import musicDriverInterface.MetaData;
import musicDriverInterface.MetaData.Tag;
import musicDriverInterface.MmlDatum;
import vavi.util.ByteUtil;
import vavi.util.compat.TriConsumer;
import vavi.util.compat.Tuple;

import static java.lang.System.getLogger;
import static mucom88.common.Common.charset;
import static vavi.util.compat.Util.isNullOrEmpty;


public class Driver implements IDriver {

    private static final Logger logger = getLogger(Driver.class.getName());

    public static final int cOPNAMasterClock = 7987200;
    public static final int cOPNBMasterClock = 8000000;
    public static final int cOPMMasterClock_X68k = 4000000;
    public static final int cOPMMasterClock_Normal = 3579545;

    public final byte[][] pcm = new byte[6][];
    public final int[] pcmStartPos = new int[6];

    private MubHeader header = null;
    private List<Tuple<String, String>> tags = null;
    private final String[] pcmType = new String[6];
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
    private final String[] fnVoiceDat = {"", "", "", ""};
    private final String[] fnPcm = {"", "", "", "", "", ""};

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

    /**
     * @param additionalOption 0: notSoundBoard2, 1: isLoadADPCM, 2: loadADPCMOnly, 3: filename
     */
    @Override
    public void init(List<ChipAction> chipsConsumer, MmlDatum[] srcBuf, Function<String, InputStream> appendFileReaderCallback, Object... additionalOption) {
        List<Consumer<ChipDatum>> lstChipWrite = new ArrayList<>();
        List<TriConsumer<byte[], Integer, Integer>> lstChipWriteAdpcm = new ArrayList<>();
        List<BiConsumer<Long, Integer>> lstChipWaitSend = new ArrayList<>();

        for (ChipAction ca : chipsConsumer) {
            lstChipWrite.add(ca::writeRegister);
            lstChipWriteAdpcm.add(ca::writePCMData);
            lstChipWaitSend.add(ca::waitSend);
        }

        if (srcBuf == null || srcBuf.length < 1) throw new IllegalArgumentException("src is null");

        boolean notSoundBoard2 = (boolean) ((Object[]) additionalOption)[0];
        boolean isLoadADPCM = (boolean) ((Object[]) additionalOption)[1];
        boolean loadADPCMOnly = (boolean) ((Object[]) additionalOption)[2];
        String filename = (String) ((Object[]) additionalOption)[3];
        appendFileReaderCallback = appendFileReaderCallback != null ? appendFileReaderCallback : createAppendFileReaderCallback(Path.of(filename).getParent().toString());

        work = new Work();
        header = new MubHeader(srcBuf);
        work.mData = getData();
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

        work.isDotNET = isExtendMucomFromTAG();
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
        waitSendOPNA = lstChipWaitSend.getFirst();

        // Transmit PCM
        if (pcm != null) {
            if (isLoadADPCM) {
                for (int i = 0; i < 2; i++) {
                    if (pcm[i] == null) continue;
                    ChipDatum[] pcmSendData = getPCMSendData(0, i, 0);

                    var sw = System.currentTimeMillis();
                    if (i == 0) for (ChipDatum dat : pcmSendData) writeOPNAPRegister(dat);
                    if (i == 1) for (ChipDatum dat : pcmSendData) writeOPNASRegister(dat);

                    waitSendOPNA.accept(System.currentTimeMillis() - sw, pcmSendData.length);
                }

                List<Byte> buf = new ArrayList<>();
                if (pcm[2] != null) {
                    buf.clear();
                    for (int i = pcmStartPos[2]; i < pcm[2].length; i++) buf.add(pcm[2][i]);
                    writeOPNBPAdpcmB(ByteUtil.toByteArray(buf));
                }
                if (pcm[3] != null) {
                    buf.clear();
                    for (int i = pcmStartPos[3]; i < pcm[3].length; i++) buf.add(pcm[3][i]);
                    writeOPNBPAdpcmB(ByteUtil.toByteArray(buf));
                }
                if (pcm[4] != null) {
                    buf.clear();
                    for (int i = pcmStartPos[4]; i < pcm[4].length; i++) buf.add(pcm[4][i]);
                    writeOPNBPAdpcmA(ByteUtil.toByteArray(buf));
                }
                if (pcm[5] != null) {
                    buf.clear();
                    for (int i = pcmStartPos[5]; i < pcm[5].length; i++) buf.add(pcm[5][i]);
                    writeOPNBPAdpcmA(ByteUtil.toByteArray(buf));
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
            for (int p = ((work.pcmTables[v][i].getItem2()[0] & 0xffff) << 2); p < ((work.pcmTables[v][i].getItem2()[1] & 0xffff) << 2) + 16; p++) {
                one.add(pcm[v][p + 0x400]); // 0x400 header size
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
        pcm[v] = ByteUtil.toByteArray(dest);
    }

    private boolean isExtendMucomFromTAG() {
        if (tags == null) return false;

        for (Tuple<String, String> tag : tags) {
            if (tag.getItem1().equals("driver")) continue;

            String drv = tag.getItem2().toLowerCase().trim();
            if (drv.equals("mucomdotnet") ||
                    drv.equals("mucom88em") ||
                    drv.equals("mucom88e")) {
                return true;
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

    private static Function<String, InputStream> createAppendFileReaderCallback(String dir) {
        return fileName -> {
            try {
                if (!isNullOrEmpty(dir)) {
                    Path path = Path.of(dir, fileName);
                    if (Files.exists(path)) {
                        return Files.newInputStream(path);
                    }
                }
                if (Files.exists(Path.of(fileName))) {
                    return Files.newInputStream(Path.of(fileName));
                }
            } catch (IOException _) {
            }
            return null;
        };
    }

    //
    // data Information
    //

    @Override
    public MmlDatum[] getData() {
        return header.getData();
    }

    @Override
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
            int cnt = (pcm[id][4] & 0xff) + ((pcm[id][5] & 0xff) << 8) + 1;
            int p = 6;
            for (int i = 0; i < cnt; i++) {
                List<Byte> b = new ArrayList<>();
                while (pcm[id][p] != 0x0) b.add(pcm[id][p++]);
                String item1 = new String(ByteUtil.toByteArray(b), charset);
                p++;
                p++;
                short[] item2 = new short[4];
                item2[0] = (short) ((pcm[id][p + 2] & 0xff) | ((pcm[id][p + 3] & 0xff) * 0x100));
                item2[1] = (short) ((pcm[id][p + 4] & 0xff) | ((pcm[id][p + 5] & 0xff) * 0x100));
                item2[2] = (short) 0;
                item2[3] = (short) ((pcm[id][p + 0] & 0xff) | ((pcm[id][p + 1] & 0xff) * 0x100));
                Tuple<String, short[]> pd = new Tuple<>(item1, item2);
                pcmTable.add(pd);
                p += 6;
            }
            pcmStartPos[id] = p;
            break;
        default: // mucom88
            pcmType[id] = "";
            for (int i = 0; i < maxPcm; i++) {
                adr = (pcm[id][infTable + 28] & 0xff) | ((pcm[id][infTable + 29] & 0xff) * 0x100); // Start Address which is ">>2"
                whl = (pcm[id][infTable + 30] & 0xff) | ((pcm[id][infTable + 31] & 0xff) * 0x100); // raw length
                eadr = adr + (whl >> 2); // !
                if (pcm[id][i * 32] != 0) {
                    short[] item2 = new short[4];
                    item2[0] = (short) adr;
                    item2[1] = (short) eadr;
                    item2[2] = (short) 0;
                    item2[3] = (short) ((pcm[id][infTable + 26] & 0xff) | ((pcm[id][infTable + 27] & 0xff) * 0x100));
                    System.arraycopy(pcm[id], i * 32, pcmName, 0, 16);
                    pcmName[16] = 0;
                    String item1 = new String(pcmName, charset);

                    Tuple<String, short[]> pd = new Tuple<>(item1, item2);
                    pcmTable.add(pd);
                    //logger.log(Level.TRACE, "shift_jis");
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
        List<ChipDatum> dat = new ArrayList<>(Arrays.asList(
                new ChipDatum(0, 0x29, 0x83), // CH 4-6 ENABLE
                new ChipDatum(0x1, 0x00, 0x20),
                new ChipDatum(0x1, 0x00, 0x21),
                new ChipDatum(0x1, 0x00, 0x00),

                new ChipDatum(0x1, 0x10, 0x00),
                new ChipDatum(0x1, 0x10, 0x80),

                new ChipDatum(0x1, 0x00, 0x61),
                new ChipDatum(0x1, 0x00, 0x68),
                new ChipDatum(0x1, 0x01, 0x00),
                new ChipDatum(0x1, 0x02, startAddress >> 2),
                new ChipDatum(0x1, 0x03, startAddress >> 10),
                new ChipDatum(0x1, 0x04, 0xff),
                new ChipDatum(0x1, 0x05, 0xff),
                new ChipDatum(0x1, 0x0c, 0xff),
                new ChipDatum(0x1, 0x0d, 0xff)
        ));

        // Data Transfer
        int infoSize = pcmStartPos[id];
        for (int i = 0; i < pcm[id].length - infoSize; i++) {
            dat.add(new ChipDatum(0x1, 0x08, pcm[id][infoSize + i] & 0xff));
            //logger.log(Level.TRACE, "#PCMDATA adr:%04x dat:%02x".formatted((infoSize + i) >> 2, pcmdata[infoSize + i]));
        }
        dat.add(new ChipDatum(0x1, 0x00, 0x00));
        dat.add(new ChipDatum(0x1, 0x10, 0x80));

        return dat.toArray(ChipDatum[]::new);
    }

    //
    // rendering
    //

    @Override
    @SafeVarargs
    public final void startRendering(int renderingFreq, Tuple<String, Integer>... chipMasterClocks) {
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
logger.log(Level.TRACE, "OPNA MasterClock %d".formatted(opnaMasterClock));
            work.timerOPNB1 = new OPNATimer(renderingFreq, opnbMasterClock);
            work.timerOPNB2 = new OPNATimer(renderingFreq, opnbMasterClock);
logger.log(Level.TRACE, "OPNB MasterClock %d".formatted(opnbMasterClock));
            work.timerOPM = new OPMTimer(renderingFreq, opmMasterClock);
logger.log(Level.TRACE, "OPM  MasterClock %d".formatted(opmMasterClock));
logger.log(Level.TRACE, "Start rendering.");
        }
    }

    @Override
    public void stopRendering() {
        synchronized (work.systemInterrupt) {
            if (work.getStatus() > 0) work.setStatus(0);
logger.log(Level.TRACE, "Stop rendering.");
        }
    }

    @Override
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
                boolean ret = work.timerOPNA1 != null ? work.timerOPNA1.writeReg((byte) reg.address, (byte) reg.data) : false;
                if (ret)
                    work.currentTimer = 0;
            }
            writeOPNAP.accept(reg);
        }
    }

    public void writeOPNASRegister(ChipDatum reg) {
        synchronized (lockObjWriteReg) {
            if (reg.port == 0) {
                boolean ret = work.timerOPNA2 != null ? work.timerOPNA2.writeReg((byte) reg.address, (byte) reg.data) : false;
                if (ret)
                    work.currentTimer = 1;
            }
            writeOPNAS.accept(reg);
        }
    }

    public void writeOPNBPRegister(ChipDatum reg) {
        synchronized (lockObjWriteReg) {
            if (reg.port == 0) {
                boolean ret = work.timerOPNB1 != null ? work.timerOPNB1.writeReg((byte) reg.address, (byte) reg.data) : false;
                if (ret)
                    work.currentTimer = 2;
            }
            writeOPNBP.accept(reg);
        }
    }

    public void writeOPNBSRegister(ChipDatum reg) {
        synchronized (lockObjWriteReg) {
            if (reg.port == 0) {
                boolean ret = work.timerOPNB2 != null ? work.timerOPNB2.writeReg((byte) reg.address, (byte) reg.data) : false;
                if (ret)
                    work.currentTimer = 3;
            }
            writeOPNBS.accept(reg);
        }
    }

    public void writeOPMPRegister(ChipDatum reg) {
        synchronized (lockObjWriteReg) {
            boolean ret = work.timerOPM != null ? work.timerOPM.writeReg((byte) reg.address, (byte) reg.data) : false;
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

    @Override
    public void startMusic(int musicNumber) {
        logger.log(Level.TRACE, "Start Playing");
        music2.MSTART(musicNumber);
        music2.skipCount(header.jumpCount);
    }

    @Override
    public void stopMusic() {
        logger.log(Level.TRACE, "Stop Playing");
        music2.MSTOP();
    }

    @Override
    public void fadeOut() {
        logger.log(Level.TRACE, "Fadeout");
        music2.FDO();
    }

    @Override
    public Map<String, Object> getWork() {
        logger.log(Level.TRACE, "Get Work Area");
        return Map.of("work", music2.RETW());
    }

    @Override
    public void shotEffect() {
        logger.log(Level.TRACE, "Sound effects");
        music2.EFC();
    }

    @Override
    public int getStatus() {
        return work.getStatus();
    }

    private void getFileNameFromTag() {
        if (tags == null) return;
        for (Tuple<String, String> tag : tags) {
            // Tag names are in lowercase.
            switch (tag.getItem1()) {
            case "voice":
                fnVoiceDat[0] = tag.getItem2();
                break;
            case "pcm":
            case "pcm_1st":
            case "pcmopna_p":
                fnPcm[0] = tag.getItem2();
                break;
            case "pcm_2nd":
            case "pcmopna_s":
                fnPcm[1] = tag.getItem2();
                break;
            case "pcm_3rd":
            case "pcmopnb_b_p":
                fnPcm[2] = tag.getItem2();
                break;
            case "pcm_4th":
            case "pcmopnb_b_s":
                fnPcm[3] = tag.getItem2();
                break;
            case "pcm_5th":
            case "pcmopnb_a_p":
                fnPcm[4] = tag.getItem2();
                break;
            case "pcm_6th":
            case "pcmopnb_a_s":
                fnPcm[5] = tag.getItem2();
                break;
            }
        }
    }

    private byte[] getFMVoiceFromFile(int id, Function<String, InputStream> appendFileReaderCallback) {
        try {
            fnVoiceDat[id] = isNullOrEmpty(fnVoiceDat[id]) ? "voice.dat" : fnVoiceDat[id];

            try (InputStream vd = appendFileReaderCallback.apply(fnVoiceDat[id])) {
                return vd != null ? vd.readAllBytes() : null;
            }
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            return null;
        }
    }

    private static final String[] defaultPCMFileName = {
            "mucompcm.bin",
            "mucompcm_2nd.bin",
            "mucompcm_3rd_B.bin",
            "mucompcm_4th_B.bin",
            "mucompcm_3rd_A.bin",
            "mucompcm_4th_A.bin"
    };

    private byte[] getPCMDataFromFile(int id, Function<String, InputStream> appendFileReaderCallback) {
        try {
            fnPcm[id] = isNullOrEmpty(fnPcm[id]) ? defaultPCMFileName[id] : fnPcm[id];

            try (InputStream pd = appendFileReaderCallback.apply(fnPcm[id])) {
                return pd != null ? pd.readAllBytes() : null;
            }
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public int setLoopCount(int loopCounter) {
        work.maxLoopCount = loopCounter;
        return 0;
    }

    @Override
    public MetaData getMetaData(byte[] srcBuf) {
        int tagData = ByteUtil.readLeInt(srcBuf, 0x000c);
        int tagsize = ByteUtil.readLeInt(srcBuf, 0x0010);
        if (srcBuf[0] == 'm' && srcBuf[1] == 'u' && srcBuf[2] == 'P' && srcBuf[3] == 'b') {
            tagData = ByteUtil.readLeInt(srcBuf, 0x0012);
            tagsize = ByteUtil.readLeInt(srcBuf, 0x0016);
        }

        if (tagData == 0) return null;

        List<Byte> lb = new ArrayList<>();
        for (int i = 0; i < tagsize; i++) {
            lb.add(srcBuf[tagData + i]);
        }

        List<Tuple<String, String>> tags = getTagsByteArray(ByteUtil.toByteArray(lb));
        MetaData metaData = new MetaData();

        for (Tuple<String, String> tag : tags) {
            switch (tag.getItem1()) {
            case "title":
                metaData.add(Tag.Title, tag.getItem2());
                metaData.add(Tag.TitleJ, tag.getItem2());
                break;
            case "composer":
                metaData.add(Tag.Composer, tag.getItem2());
                metaData.add(Tag.ComposerJ, tag.getItem2());
                break;
            case "author":
                metaData.add(Tag.Artist, tag.getItem2());
                metaData.add(Tag.ArtistJ, tag.getItem2());
                break;
            case "comment":
                metaData.add(Tag.Note, tag.getItem2());
                break;
            case "mucom88":
                metaData.add(Tag.RequestDriverVersion, tag.getItem2());
                break;
            case "date":
                metaData.add(Tag.ReleaseDate, tag.getItem2());
                break;
            case "driver":
                metaData.add(Tag.DriverName, tag.getItem2());
                break;
            case "artwork":
                metaData.add(Tag.Artwork, tag.getItem2());
                break;
            }
        }

        return metaData;
    }

    private static List<Tuple<String, String>> getTagsByteArray(byte[] buf) {
        var text = Arrays.stream(new String(buf, charset).split("\r\n|\r|\n"))
                .filter(x -> x.indexOf("#") == 0).toArray(String[]::new);

        List<Tuple<String, String>> tags = new ArrayList<>();
        for (String v : text) {
            try {
                int p = v.indexOf(' ');
                String tag;
                String ele;
                if (p >= 0) {
                    tag = v.substring(1, 1 + p).trim().toLowerCase();
                    ele = v.substring(p + 1).trim();
                    Tuple<String, String> item = new Tuple<>(tag, ele);
                    tags.add(item);
                }
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        }

        return tags;
    }

    @Override
    public int getNowLoopCounter() {
        try {
            return work.nowLoopCounter;
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            return -1;
        }
    }

    @Override
    public void setDriverSwitch(Object... param) {
        if (param[0] instanceof String cmd) {
            if (cmd.equals("AllMute")) {
                setAllMuteFlag((boolean) param[1]);
            } else if (cmd.equals("SetMute")) {
                setMuteFlag((int) param[1], (int) param[2], (int) param[3], (boolean) param[4]);
            }
        }
    }

    @Override
    public void writeRegister(ChipDatum reg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getPCMFromSrcBuf() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Tuple<String, short[]>[] getPCMTable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChipDatum[] getPCMSendData() {
        throw new UnsupportedOperationException();
    }
}
