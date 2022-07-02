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

import dotnet4j.TriConsumer;
import dotnet4j.Tuple;
import dotnet4j.io.File;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.FileStream;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Path;
import dotnet4j.io.Stream;
import dotnet4j.util.compat.StringUtilities;
import mdsound.Log;
import mdsound.LogLevel;
import mucom88.common.Common;
import mucom88.common.MubException;
import mucom88.common.iEncoding;
import mucom88.common.MyEncoding;
import musicDriverInterface.ChipAction;
import musicDriverInterface.ChipDatum;
import musicDriverInterface.GD3Tag;
import musicDriverInterface.MmlDatum;
import musicDriverInterface.enmTag;
import musicDriverInterface.iDriver;
import vavi.util.serdes.Serdes;


public class Driver implements iDriver {

    public static final int cOPNAMasterClock = 7987200;
    public static final int cOPNBMasterClock = 8000000;
    public static final int cOPMMasterClock_X68k = 4000000;
    public static final int cOPMMasterClock_Normal = 3579545;

    public byte[][] pcm = new byte[6][];
    public int[] pcmStartPos = new int[6];

    private MUBHeader header = null;
    private List<Tuple<String, String>> tags = null;
    private String[] pcmType = new String[6];
    private Consumer<ChipDatum> WriteOPNAP;
    private Consumer<ChipDatum> WriteOPNAS;
    private Consumer<ChipDatum> WriteOPNBP;
    private Consumer<ChipDatum> WriteOPNBS;
    private Consumer<ChipDatum> WriteOPMP;
    private TriConsumer<byte[], Integer, Integer> WriteOPNBAdpcmAP;
    private TriConsumer<byte[], Integer, Integer> WriteOPNBAdpcmBP;
    private TriConsumer<byte[], Integer, Integer> WriteOPNBAdpcmAS;
    private TriConsumer<byte[], Integer, Integer> WriteOPNBAdpcmBS;
    private BiConsumer<Long, Integer> WaitSendOPNA;
    private String[] fnVoicedat = {"", "", "", ""};
    private String[] fnPcm = {"", "", "", "", "", ""};

    private int renderingFreq = 44100;

    private int opnaMasterClock = (int) cOPNAMasterClock;
    private int opnbMasterClock = (int) cOPNBMasterClock;
    private int opmMasterClock = (int) cOPMMasterClock_Normal;

    private Work work = new Work();
    private Music2 music2 = null;
    private Object lockObjWriteReg = new Object();

    public enum Command {
        MusicSTART, MusicSTOP, FaDeOut, EFfeCt, RETurnWork
    }

    private iEncoding enc = null;

    public Driver(iEncoding enc /*= null*/) {
        this.enc = enc != null ? enc : MyEncoding.Default();
    }

    public void Init(List<ChipAction> chipsConsumer, MmlDatum[] srcBuf, Function<String, Stream> appendFileReaderCallback, Object addtionalOption) {
        List<Consumer<ChipDatum>> lstChipWrite = new ArrayList<>();
        List<TriConsumer<byte[], Integer, Integer>> lstChipWriteAdpcm = new ArrayList<>();
        List<BiConsumer<Long, Integer>> lstChipWaitSend = new ArrayList<>();

        for (ChipAction ca : chipsConsumer) {
            lstChipWrite.add(ca::writeRegister);
            lstChipWriteAdpcm.add(ca::writePCMData);
            lstChipWaitSend.add(ca::waitSend);
        }
        InitT(lstChipWrite, lstChipWriteAdpcm, lstChipWaitSend, srcBuf, addtionalOption, appendFileReaderCallback);
    }

    private void Init(
            String fileName,
            List<Consumer<ChipDatum>> lstChipWrite,
            List<TriConsumer<byte[], Integer, Integer>> lstChipWriteAdpcm,
            List<BiConsumer<Long, Integer>> opnaWaitSend,
            boolean notSoundBoard2, boolean isLoadADPCM, boolean loadADPCMOnly, Function<String, Stream> appendFileReaderCallback/* =null*/) {
        if (!Path.getExtension(fileName).toLowerCase().equals(".xml")) {
            byte[] srcBuf = File.readAllBytes(fileName);
            if (srcBuf.length < 1) return;
            Init(lstChipWrite, lstChipWriteAdpcm, opnaWaitSend, notSoundBoard2, srcBuf, isLoadADPCM, loadADPCMOnly, appendFileReaderCallback != null ? appendFileReaderCallback : CreateAppendFileReaderCallback(Path.getDirectoryName(fileName)));
        } else {
            try (InputStream sr = Files.newInputStream(java.nio.file.Path.of(fileName))) {
                List<MmlDatum> s = new ArrayList<>();
                while (sr.available() > 0) {
                    MmlDatum m = new MmlDatum();
                    s.add(Serdes.Util.deserialize(sr, m));
                }
                InitT(lstChipWrite, lstChipWriteAdpcm, opnaWaitSend, s.toArray(new MmlDatum[0]), new Object[] {notSoundBoard2, isLoadADPCM, loadADPCMOnly}, appendFileReaderCallback);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void Init(String fileName, List<Consumer<ChipDatum>> lstChipWrite, List<TriConsumer<byte[], Integer, Integer>> lstChipWriteAdpcm, List<BiConsumer<Long, Integer>> opnaWaitSend, boolean notSoundBoard2, byte[] srcBuf, boolean isLoadADPCM, boolean loadADPCMOnly) {
        if (srcBuf == null || srcBuf.length < 1) return;
        Init(lstChipWrite, lstChipWriteAdpcm, opnaWaitSend, notSoundBoard2, srcBuf, isLoadADPCM, loadADPCMOnly, CreateAppendFileReaderCallback(Path.getDirectoryName(fileName)));
    }

    private void Init(List<Consumer<ChipDatum>> lstChipWrite, List<TriConsumer<byte[], Integer, Integer>> lstChipWriteAdpcm, List<BiConsumer<Long, Integer>> opnaWaitSend, boolean notSoundBoard2, byte[] srcBuf, boolean isLoadADPCM, boolean loadADPCMOnly, Function<String, Stream> appendFileReaderCallback) {
        if (srcBuf == null || srcBuf.length < 1) return;
        List<MmlDatum> bl = new ArrayList<>();
        for (byte b : srcBuf) bl.add(new MmlDatum(b));
        InitT(lstChipWrite, lstChipWriteAdpcm, opnaWaitSend, bl.toArray(new MmlDatum[0]), new Object[] {notSoundBoard2, isLoadADPCM, loadADPCMOnly}, appendFileReaderCallback);
    }

    private void Init(String fileName, List<Consumer<ChipDatum>> lstChipWrite, List<TriConsumer<byte[], Integer, Integer>> lstChipWriteAdpcm, List<BiConsumer<Long, Integer>> chipWaitSend, MmlDatum[] srcBuf, Object addtionalOption) {
        if (srcBuf == null || srcBuf.length < 1) return;
        InitT(lstChipWrite, lstChipWriteAdpcm, chipWaitSend, srcBuf, addtionalOption, CreateAppendFileReaderCallback(Path.getDirectoryName(fileName)));
    }

    private void InitT(List<Consumer<ChipDatum>> lstChipWrite, List<TriConsumer<byte[], Integer, Integer>> lstChipWriteAdpcm, List<BiConsumer<Long, Integer>> chipWaitSend, MmlDatum[] srcBuf, Object addtionalOption, Function<String, Stream> appendFileReaderCallback) {
        if (srcBuf == null || srcBuf.length < 1) return;

        boolean notSoundBoard2 = (boolean) ((Object[]) addtionalOption)[0];
        boolean isLoadADPCM = (boolean) ((Object[]) addtionalOption)[1];
        boolean loadADPCMOnly = (boolean) ((Object[]) addtionalOption)[2];
        String filename = (String) ((Object[]) addtionalOption)[3];
        appendFileReaderCallback = appendFileReaderCallback != null ? appendFileReaderCallback : CreateAppendFileReaderCallback(Path.getDirectoryName(filename));

        work = new Work();
        header = new MUBHeader(srcBuf, enc);
        work.mData = GetDATA();
        work.setHeader(header);
        tags = GetTags();
        GetFileNameFromTag();
        for (int i = 0; i < 4; i++) {
            work.fmVoice[i] = GetFMVoiceFromFile(i, appendFileReaderCallback);
            pcm[i] = GetPCMFromSrcBuf(i) != null ? GetPCMFromSrcBuf(i) : GetPCMDataFromFile(i, appendFileReaderCallback);
            work.pcmTables[i] = GetPCMTable(i);
        }
        for (int i = 4; i < 6; i++) {
            pcm[i] = GetPCMFromSrcBuf(i) != null ? GetPCMFromSrcBuf(i) : GetPCMDataFromFile(i, appendFileReaderCallback);
            work.pcmTables[i] = GetPCMTable(i);
        }

        if (pcm[2] != null && pcmType[2].isEmpty()) {
            TransformOPNAPCMtoOPNBPCM(2);
            pcmStartPos[2] = 0;
        }
        if (pcm[3] != null && pcmType[3].isEmpty()) {
            TransformOPNAPCMtoOPNBPCM(3);
            pcmStartPos[3] = 0;
        }
        if (pcm[4] != null && pcmType[4].isEmpty()) {
            TransformOPNAPCMtoOPNBPCM(4);
            pcmStartPos[4] = 0;
        }
        if (pcm[5] != null && pcmType[5].isEmpty()) {
            TransformOPNAPCMtoOPNBPCM(5);
            pcmStartPos[5] = 0;
        }

        work.isDotNET = IsDotNETFromTAG();
        work.SSGExtend = SSGExtendFromTAG();

        WriteOPNAP = lstChipWrite.get(0);
        WriteOPNAS = lstChipWrite.get(1);
        WriteOPNBP = lstChipWrite.get(2);
        WriteOPNBS = lstChipWrite.get(3);
        WriteOPMP = lstChipWrite.get(4);
        WriteOPNBAdpcmAP = lstChipWriteAdpcm.get(2);
        WriteOPNBAdpcmBP = lstChipWriteAdpcm.get(2);
        WriteOPNBAdpcmAS = lstChipWriteAdpcm.get(3);
        WriteOPNBAdpcmBS = lstChipWriteAdpcm.get(3);
        WaitSendOPNA = chipWaitSend.get(0);

        //PCMを送信する
        if (pcm != null) {
            if (isLoadADPCM) {
                for (int i = 0; i < 2; i++) {
                    if (pcm[i] == null) continue;
                    ChipDatum[] pcmSendData = GetPCMSendData(0, i, 0);

                    var sw = System.currentTimeMillis();
                    if (i == 0) for (ChipDatum dat : pcmSendData) WriteOPNAPRegister(dat);
                    if (i == 1) for (ChipDatum dat : pcmSendData) WriteOPNASRegister(dat);

                    WaitSendOPNA.accept(sw - System.currentTimeMillis(), pcmSendData.length);
                }

                List<Byte> buf = new ArrayList<>();
                if (pcm[2] != null) {
                    buf.clear();
                    for (int i = pcmStartPos[2]; i < pcm[2].length; i++) buf.add(pcm[2][i]);
                    WriteOPNBPAdpcmB(mdsound.Common.toByteArray(buf));
                }
                if (pcm[3] != null) {
                    buf.clear();
                    for (int i = pcmStartPos[3]; i < pcm[3].length; i++) buf.add(pcm[3][i]);
                    WriteOPNBPAdpcmB(mdsound.Common.toByteArray(buf));
                }
                if (pcm[4] != null) {
                    buf.clear();
                    for (int i = pcmStartPos[4]; i < pcm[4].length; i++) buf.add(pcm[4][i]);
                    WriteOPNBPAdpcmA(mdsound.Common.toByteArray(buf));
                }
                if (pcm[5] != null) {
                    buf.clear();
                    for (int i = pcmStartPos[5]; i < pcm[5].length; i++) buf.add(pcm[5][i]);
                    WriteOPNBPAdpcmA(mdsound.Common.toByteArray(buf));
                }

            }
        }

        if (loadADPCMOnly) return;

        music2 = new Music2(work, this::WriteOPNAPRegister, this::WriteOPNASRegister, this::WriteOPNBPRegister, this::WriteOPNBSRegister, this::WriteOPMPRegister);
        music2.notSoundBoard2 = notSoundBoard2;
    }

    public void SetMuteFlg(int chip, int ch, int page, boolean flg) {
        if (music2 == null) return;
        music2.SetMuteFlg(chip, ch, page, flg);
    }

    public void SetAllMuteFlg(boolean flg) {
        if (music2 == null) return;
        music2.SetAllMuteFlg(flg);
    }

    private void TransformOPNAPCMtoOPNBPCM(int v) {
        List<List<Byte>> pcmData = new ArrayList<>();
        List<Byte> dest = new ArrayList<>(0);
        //for (int i = 0; i < 0x400; i++) dest.Add(0);
        for (int i = 0; i < work.pcmTables[v].length; i++) {
            pcmData.add(new ArrayList<Byte>());
            List<Byte> one = pcmData.get(i);
            for (int ptr = (work.pcmTables[v][i].getItem2()[0] << 2); ptr < (work.pcmTables[v][i].getItem2()[1] << 2) + 16; ptr++) {
                one.add(pcm[v][ptr + 0x400]);//0x400 ヘッダのサイズ
            }
        }

        int tblPtr = 0;
        for (int i = 0; i < work.pcmTables[v].length; i++) {
            for (int j = 0; j < pcmData.get(i).size(); j++) dest.add(pcmData.get(i).get(j));
            for (int j = 0; j < 256 - (pcmData.get(i).size() % 256); j++) dest.add((byte) 0x00);

            short stAdr = (short) (tblPtr >> 8);
            int length = pcmData.get(i).size() + 256 - (pcmData.get(i).size() % 256);
            tblPtr += length != 0 ? (length - 0x100) : 0;
            short edAdr = (short) (tblPtr >> 8);
            tblPtr += length != 0 ? 0x100 : 0;
            //ushort stAdr = (ushort)(tblPtr >> 9);
            //int length = pcmData[i].Count + 512 - (pcmData[i].Count % 512);
            //tblPtr += length != 0 ? (length - 0x200) : 0;
            //ushort edAdr = (ushort)(tblPtr >> 9);
            //tblPtr += length != 0 ? 0x200 : 0;
            work.pcmTables[v][i] = new Tuple<String, short[]>(work.pcmTables[v][i].getItem1(), new short[] {stAdr, edAdr, 0, work.pcmTables[v][i].getItem2()[3]});

        }
        pcm[v] = mdsound.Common.toByteArray(dest);
    }

    private boolean IsDotNETFromTAG() {
        if (tags == null) return false;
        for (Tuple<String, String> tag : tags) {
            if (tag.getItem1().equals("driver")) {
                if (tag.getItem2().toLowerCase().equals("mucomdotnet")) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean SSGExtendFromTAG() {
        if (tags == null) return false;
        for (Tuple<String, String> tag : tags) {
            if (tag.getItem1() == "ssgextend") {
                String ssgextval = tag.getItem2().toLowerCase();
                if (ssgextval == "on" || ssgextval == "yes" || ssgextval == "y" || ssgextval == "1" || ssgextval == "true" || ssgextval == "t") {
                    return true;
                }
            }
        }

        return false;
    }

    private Function<String, Stream> CreateAppendFileReaderCallback(String dir) {
        return fname ->
        {
            if (!StringUtilities.isNullOrEmpty(dir)) {
                var path = Path.combine(dir, fname);
                if (File.exists(path)) {
                    return new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.Read);
                }
            }
            if (File.exists(fname)) {
                return new FileStream(fname, FileMode.Open, FileAccess.Read, FileShare.Read);
            }
            return null;
        }
                ;
    }


    //
    //data Information
    //

    public MmlDatum[] GetDATA() {
        return header.GetDATA();
    }

    public List<Tuple<String, String>> GetTags() {
        if (header == null) {
            throw new MubException("Header information not found.");
        }
        return header.GetTags();
    }

    public byte[] GetPCMFromSrcBuf(int id) {
        if (header.mupb == null)
            return header.GetPCM(id);
        else {
            if (header.mupb.getpcms().length <= id) return null;
            return (header.mupb.getpcms()[id].getdata() == null || header.mupb.getpcms()[id].getdata().length < 1) ? null : header.mupb.getpcms()[id].getdata();
        }
    }

    public Tuple<String, short[]>[] GetPCMTable(int id) {
        if (pcm == null) return null;
        if (pcm[id] == null) return null;

        List<Tuple<String, short[]>> pcmtable = new ArrayList<>();
        int inftable = 0x0000;
        int adr, whl, eadr;
        byte[] pcmname = new byte[17];
        int maxpcm = 32;

        String fcc = "";
        if (pcm[id].length > 4)
            fcc = String.valueOf((char) pcm[id][0]) + ((char) pcm[id][1]) + ((char) pcm[id][2]) + ((char) pcm[id][3]);
        pcmType[id] = fcc;
        switch (fcc) {
        case "mda ": // OPNA ADPCM
        case "mdbb": // OPNB ADPCM-B
        case "mdba": // OPNB ADPCM-A
            int cnt = pcm[id][4] + (pcm[id][5] << 8) + 1;
            int ptr = 6;
            for (int i = 0; i < cnt; i++) {
                List<Byte> b = new ArrayList<>();
                while (pcm[id][ptr] != 0x0) b.add(pcm[id][ptr++]);
                String item1 = enc.GetStringFromSjisArray(mdsound.Common.toByteArray(b));
                ptr++;
                ptr++;
                short[] item2 = new short[4];
                item2[0] = (short) (pcm[id][ptr + 2] | (pcm[id][ptr + 3] * 0x100));
                item2[1] = (short) (pcm[id][ptr + 4] | (pcm[id][ptr + 5] * 0x100));
                item2[2] = (short) 0;
                item2[3] = (short) (pcm[id][ptr + 0] | (pcm[id][ptr + 1] * 0x100));
                Tuple<String, short[]> pd = new Tuple<String, short[]>(item1, item2);
                pcmtable.add(pd);
                ptr += 6;
            }
            pcmStartPos[id] = ptr;
            break;
        default: // mucom88
            pcmType[id] = "";
            for (int i = 0; i < maxpcm; i++) {
                adr = pcm[id][inftable + 28] | (pcm[id][inftable + 29] * 0x100);//>>2済み開始アドレス
                whl = pcm[id][inftable + 30] | (pcm[id][inftable + 31] * 0x100);//生レングス
                eadr = adr + (whl >> 2);//!
                if (pcm[id][i * 32] != 0) {
                    short[] item2 = new short[4];
                    item2[0] = (short) adr;
                    item2[1] = (short) eadr;
                    item2[2] = (short) 0;
                    item2[3] = (short) (pcm[id][inftable + 26] | (pcm[id][inftable + 27] * 0x100));
                    System.arraycopy(pcm[id], i * 32, pcmname, 0, 16);
                    pcmname[16] = 0;
                    String item1 = enc.GetStringFromSjisArray(pcmname);

                    Tuple<String, short[]> pd = new Tuple<String, short[]>(item1, item2);
                    pcmtable.add(pd);
                    //log.Write(String.Format("#PCM{0} ${1:x04} ${2:x04} {3}", i + 1, adr, eadr, Encoding.GetEncoding("shift_jis").GetString(pcmname)));
                }
                inftable += 32;
            }
            pcmStartPos[id] = 0x400;
            break;
        }

        return pcmtable.toArray(new Tuple[0]);
    }

    public ChipDatum[] GetPCMSendData(int c, int id, int tp) {
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
        int infosize = pcmStartPos[id];
        for (int cnt = 0; cnt < pcm[id].length - infosize; cnt++) {
            dat.add(new ChipDatum(0x1, 0x08, pcm[id][infosize + cnt]));
            //log.Write(String.Format("#PCMDATA adr:{0:x04} dat:{1:x02}", (infosize + cnt) >> 2, pcmdata[infosize + cnt]));
        }
        dat.add(new ChipDatum(0x1, 0x00, 0x00));
        dat.add(new ChipDatum(0x1, 0x10, 0x80));

        return dat.toArray(new ChipDatum[0]);
    }

    //
    //rendering
    //

    public void StartRendering(int renderingFreq, Tuple<String, Integer>[] chipsMasterClock) {
        synchronized (work.SystemInterrupt) {

            work.timeCounter = 0L;
            this.renderingFreq = renderingFreq <= 0 ? 44100 : renderingFreq;
            this.opnaMasterClock = 7987200;
            if (chipsMasterClock != null && chipsMasterClock.length > 0) {
                this.opnaMasterClock = chipsMasterClock[0].getItem2() <= 0 ? 7987200 : chipsMasterClock[0].getItem2();
            }
            if (chipsMasterClock != null && chipsMasterClock.length > 2) {
                this.opnbMasterClock = chipsMasterClock[2].getItem2() <= 0 ? 8000000 : chipsMasterClock[2].getItem2();
            }
            if (chipsMasterClock != null && chipsMasterClock.length > 4) {
                this.opmMasterClock = chipsMasterClock[4].getItem2() <= 0 ? 3579545 : chipsMasterClock[4].getItem2();
            }
            work.timerOPNA1 = new OPNATimer(renderingFreq, opnaMasterClock);
            work.timerOPNA2 = new OPNATimer(renderingFreq, opnaMasterClock);
            Log.writeLine(LogLevel.TRACE, String.format("OPNA MasterClock %d", opnaMasterClock));
            work.timerOPNB1 = new OPNATimer(renderingFreq, opnbMasterClock);
            work.timerOPNB2 = new OPNATimer(renderingFreq, opnbMasterClock);
            Log.writeLine(LogLevel.TRACE, String.format("OPNB MasterClock %d", opnbMasterClock));
            work.timerOPM = new OPMTimer(renderingFreq, opmMasterClock);
            Log.writeLine(LogLevel.TRACE, String.format("OPM  MasterClock %d", opmMasterClock));
            Log.writeLine(LogLevel.TRACE, "Start rendering.");

        }
    }

    public void StopRendering() {
        synchronized (work.SystemInterrupt) {
            if (work.Status > 0) work.Status = 0;
            Log.writeLine(LogLevel.TRACE, "Stop rendering.");

        }
    }

    public void Rendering() {
        if (work.Status < 0) return;

        try {
            music2.Rendering();
        } catch (Exception e) {
            work.Status = -1;
            throw e;
        }
    }

    public void WriteOPNAPRegister(ChipDatum reg) {
        synchronized (lockObjWriteReg) {
            if (reg.port == 0) {
                boolean ret = work.timerOPNA1.WriteReg((byte) reg.address, (byte) reg.data);
                if (ret)
                    work.currentTimer = 0;
            }
            WriteOPNAP.accept(reg);
        }
    }

    public void WriteOPNASRegister(ChipDatum reg) {
        synchronized (lockObjWriteReg) {
            if (reg.port == 0) {
                boolean ret = work.timerOPNA2.WriteReg((byte) reg.address, (byte) reg.data);
                if (ret)
                    work.currentTimer = 1;
            }
            WriteOPNAS.accept(reg);
        }
    }

    public void WriteOPNBPRegister(ChipDatum reg) {
        synchronized (lockObjWriteReg) {
            if (reg.port == 0) {
                boolean ret = work.timerOPNB1.WriteReg((byte) reg.address, (byte) reg.data);
                if (ret)
                    work.currentTimer = 2;
            }
            WriteOPNBP.accept(reg);
        }
    }

    public void WriteOPNBSRegister(ChipDatum reg) {
        synchronized (lockObjWriteReg) {
            if (reg.port == 0) {
                boolean ret = work.timerOPNB2.WriteReg((byte) reg.address, (byte) reg.data);
                if (ret)
                    work.currentTimer = 3;
            }
            WriteOPNBS.accept(reg);
        }
    }

    public void WriteOPMPRegister(ChipDatum reg) {
        synchronized (lockObjWriteReg) {
            boolean ret = work.timerOPM.WriteReg((byte) reg.address, (byte) reg.data);
            if (ret)
                work.currentTimer = 4;
            WriteOPMP.accept(reg);
        }
    }

    public void WriteOPNBPAdpcmA(byte[] pcmdata) {
        if (pcmdata == null) return;
        synchronized (lockObjWriteReg) {
            WriteOPNBAdpcmAP.accept(pcmdata, 0, 0);
        }
    }

    public void WriteOPNBPAdpcmB(byte[] pcmdata) {
        if (pcmdata == null) return;
        synchronized (lockObjWriteReg) {
            WriteOPNBAdpcmBP.accept(pcmdata, 1, 0);
        }
    }

    public void WriteOPNBSAdpcmA(byte[] pcmdata) {
        if (pcmdata == null) return;
        synchronized (lockObjWriteReg) {
            WriteOPNBAdpcmAS.accept(pcmdata, 0, 0);
        }
    }

    public void WriteOPNBSAdpcmB(byte[] pcmdata) {
        if (pcmdata == null) return;
        synchronized (lockObjWriteReg) {
            WriteOPNBAdpcmBS.accept(pcmdata, 1, 0);
        }
    }

    //
    //Command
    //

    public void MusicSTART(int musicNumber) {
        Log.writeLine(LogLevel.TRACE, "演奏開始");
        music2.MSTART(musicNumber);
        music2.SkipCount((int) header.jumpcount);
    }

    public void MusicSTOP() {
        Log.writeLine(LogLevel.TRACE, "演奏停止");
        music2.MSTOP();
    }

    public void FadeOut() {
        Log.writeLine(LogLevel.TRACE, "フェードアウト");
        music2.FDO();
    }

    public Object GetWork() {
        Log.writeLine(LogLevel.TRACE, "ワークエリア取得");
        return music2.RETW();
    }

    public void ShotEffect() {
        Log.writeLine(LogLevel.TRACE, "効果音");
        music2.EFC();
    }

    public int GetStatus() {
        return work.Status;
    }


    private void GetFileNameFromTag() {
        if (tags == null) return;
        for (Tuple<String, String> tag : tags) {
            switch (tag.getItem1()) {
            case "voice":
                fnVoicedat[0] = tag.getItem2();
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

    private byte[] GetFMVoiceFromFile(int id, Function<String, Stream> appendFileReaderCallback) {
        try {
            fnVoicedat[id] = StringUtilities.isNullOrEmpty(fnVoicedat[id]) ? "voice.dat" : fnVoicedat[id];

            try (Stream vd = appendFileReaderCallback.apply(fnVoicedat[id])) {
                return ReadAllBytes(vd);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String[] defaultPCMFileName = new String[]
            {
                    "mucompcm.bin",
                    "mucompcm_2nd.bin",
                    "mucompcm_3rd_B.bin",
                    "mucompcm_4th_B.bin",
                    "mucompcm_3rd_A.bin",
                    "mucompcm_4th_A.bin"
            };

    private byte[] GetPCMDataFromFile(int id, Function<String, Stream> appendFileReaderCallback) {
        try {
            fnPcm[id] = StringUtilities.isNullOrEmpty(fnPcm[id]) ? defaultPCMFileName[id] : fnPcm[id];

            try (Stream pd = appendFileReaderCallback.apply(fnPcm[id])) {
                return ReadAllBytes(pd);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * ストリームから一括でバイナリを読み込む
     */
    private byte[] ReadAllBytes(Stream stream) {
        if (stream == null) return null;

        var buf = new byte[8192];
        try (var ms = new MemoryStream()) {
            while (true) {
                var r = stream.read(buf, 0, buf.length);
                if (r < 1) {
                    break;
                }
                ms.write(buf, 0, r);
            }
            return ms.toArray();
        }
    }

    public int SetLoopCount(int loopCounter) {
        work.maxLoopCount = loopCounter;
        return 0;
    }

    public GD3Tag GetGD3TagInfo(byte[] srcBuf) {
        int tagdata = Common.getLE32(srcBuf, 0x000c);
        int tagsize = Common.getLE32(srcBuf, 0x0010);
        if (srcBuf[0] == 'm' && srcBuf[1] == 'u' && srcBuf[2] == 'P' && srcBuf[3] == 'b') {
            tagdata = Common.getLE32(srcBuf, 0x0012);
            tagsize = Common.getLE32(srcBuf, 0x0016);
        }

        if (tagdata == 0) return null;
        if (srcBuf == null) return null;

        List<Byte> lb = new ArrayList<>();
        for (int i = 0; i < tagsize; i++) {
            lb.add(srcBuf[tagdata + i]);
        }

        List<Tuple<String, String>> tags = GetTagsByteArray(mdsound.Common.toByteArray(lb));
        GD3Tag gt = new GD3Tag();

        for (Tuple<String, String> tag : tags) {
            switch (tag.getItem1()) {
            case "title":
                addItemAry(gt, enmTag.Title, tag.getItem2());
                addItemAry(gt, enmTag.TitleJ, tag.getItem2());
                break;
            case "composer":
                addItemAry(gt, enmTag.Composer, tag.getItem2());
                addItemAry(gt, enmTag.ComposerJ, tag.getItem2());
                break;
            case "author":
                addItemAry(gt, enmTag.Artist, tag.getItem2());
                addItemAry(gt, enmTag.ArtistJ, tag.getItem2());
                break;
            case "comment":
                addItemAry(gt, enmTag.Note, tag.getItem2());
                break;
            case "mucom88":
                addItemAry(gt, enmTag.RequestDriverVersion, tag.getItem2());
                break;
            case "date":
                addItemAry(gt, enmTag.ReleaseDate, tag.getItem2());
                break;
            case "driver":
                addItemAry(gt, enmTag.DriverName, tag.getItem2());
                break;
            }
        }

        return gt;
    }

    private List<Tuple<String, String>> GetTagsByteArray(byte[] buf) {
        var text = Arrays.stream(enc.GetStringFromSjisArray(buf).split("\r\n"))
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
            }
        }

        return tags;
    }

    private void addItemAry(GD3Tag gt, enmTag tag, String item) {
        if (!gt.dicItem.containsKey(tag))
            gt.dicItem.put(tag, new String[] {item});
        else {
            String[] dmy = gt.dicItem.get(tag);
            dmy = new String[dmy.length + 1];
            dmy[dmy.length - 1] = item;
            gt.dicItem.put(tag, dmy);
        }
    }

    public int GetNowLoopCounter() {
        try {
            return work.nowLoopCounter;
        } catch (Exception e) {
            return -1;
        }
    }

    public void SetDriverSwitch(Object... param) {
        if (param[0] instanceof String) {
            String cmd = (String) param[0];
            if (cmd.equals("AllMute")) {
                SetAllMuteFlg((boolean) param[1]);
            } else if (cmd.equals("SetMute")) {
                SetMuteFlg((int) param[1], (int) param[2], (int) param[3], (boolean) param[4]);
            }

        }
    }

    public void WriteRegister(ChipDatum reg) {
        throw new UnsupportedOperationException();
    }

    public byte[] GetPCMFromSrcBuf() {
        throw new UnsupportedOperationException();
    }

    public Tuple<String, short[]>[] GetPCMTable() {
        throw new UnsupportedOperationException();
    }

    public ChipDatum[] GetPCMSendData() {
        throw new UnsupportedOperationException();
    }
}
