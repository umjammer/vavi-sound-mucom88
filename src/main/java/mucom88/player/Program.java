package mucom88.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

import dotnet4j.io.File;
import dotnet4j.io.Path;
import dotnet4j.util.compat.StopWatch;
import dotnet4j.util.compat.StringUtilities;
import dotnet4j.util.compat.TriFunction;
import dotnet4j.util.compat.Tuple;
import mdsound.Log;
import mdsound.LogLevel;
import mdsound.MDSound;
import mdsound.Ym2151;
import mdsound.Ym2608;
import mdsound.Ym2610;
import mucom88.common.MucomChipAction;
import mucom88.common.MyEncoding;
import mucom88.driver.Driver;
import mucom88.driver.MUBHeader;
import musicDriverInterface.ChipAction;
import musicDriverInterface.ChipDatum;
import musicDriverInterface.MmlDatum;
import vavi.util.Debug;


class Program {
    private static SourceDataLine audioOutput = null;
    public interface naudioCallBack extends TriFunction<short[], Integer, Integer, Integer>{}
    private static naudioCallBack callBack = null;
    private static Thread trdMain = null;
    private static StopWatch sw = null;
    private static double swFreq = 0;
    public static boolean trdClosed = false;
    private static final Object lockObj = new Object();
    private static boolean _trdStopped = true;
    private static boolean trdStopped;

    public boolean getTrdStopped() {
        synchronized (lockObj) {
            return _trdStopped;
        }
    }

    public void setTrdStopped(boolean value) {
        synchronized (lockObj) {
            _trdStopped = value;
        }
    }

    private static final int SamplingRate = 55467;//44100;
    private static final int samplingBuffer = 1024;
    private static short[] frames = new short[samplingBuffer * 4];
    private static MDSound mds = null;
    private static short[] emuRenderBuf = new short[2];
    private static musicDriverInterface.iDriver drv = null;
    private static int opmMasterClock = 3579545;
    private static final int opnaMasterClock = 7987200;
    private static final int opnbMasterClock = 8000000;
    private static int device = 0;
    private static int loop = 0;

    private static boolean loadADPCMOnly = false;
    private static boolean isLoadADPCM = true;

    private static RSoundChip rsc;

    static void main(String[] args) {
        Log.writeLine = Program::WriteLine;
        Log.level = LogLevel.INFO;

        int fnIndex = AnalyzeOption(args);

        if (args.length != fnIndex + 1) {
            Debug.printf(Level.SEVERE, "引数(.mubファイル)１個欲しいよぉ");
            System.exit(-1);
        }
        if (!File.exists(args[fnIndex])) {
            Debug.printf(Level.SEVERE, "ファイルが見つかりません");
            System.exit(-1);
        }

        rsc = CheckDevice();

        try {

            int latency = 1000;

            switch (device) {
            case 0:
                callBack = Program::EmuCallback;
                audioOutput = AudioSystem.getSourceDataLine(new AudioFormat(SamplingRate, 16, 2, true, false));
                audioOutput.open();
                break;
            case 1:
            case 2:
                trdMain = new Thread(Program::RealCallback);
                trdMain.setPriority(Thread.MAX_PRIORITY);
                trdMain.setDaemon(true);
                trdMain.setName("trdVgmReal");
                sw = StopWatch.startNew();
                swFreq = StopWatch.Frequency;
                break;
            }

            List<MmlDatum> bl = new ArrayList<>();
            byte[] srcBuf = File.readAllBytes(args[fnIndex]);
            for (byte b : srcBuf) bl.add(new MmlDatum(b));
            MmlDatum[] blary = bl.toArray(MmlDatum[]::new);

            MUBHeader mh = new MUBHeader(blary, MyEncoding.Default());
            mh.GetTags();
            if (mh.OPMClockMode == MUBHeader.enmOPMClockMode.X68000) opmMasterClock = Driver.cOPMMasterClock_X68k;

            List<MDSound.Chip> lstChips = new ArrayList<>();
            MDSound.Chip chip = null;

            Ym2608 ym2608 = new Ym2608();
            for (int i = 0; i < 2; i++) {
                chip = new MDSound.Chip();
                chip.type = MDSound.InstrumentType.YM2608;
                chip.id = (byte) i;
                chip.instrument = ym2608;
                chip.update = ym2608::update;
                chip.start = ym2608::start;
                chip.stop = ym2608::stop;
                chip.reset = ym2608::reset;
                chip.samplingRate = SamplingRate;
                chip.clock = opnaMasterClock;
                chip.volume = 0;
                chip.option = new Object[] {GetApplicationFolder()};
                lstChips.add(chip);
            }
            Ym2610 ym2610 = new Ym2610();
            for (int i = 0; i < 2; i++) {
                chip = new MDSound.Chip() ;
                chip.type = MDSound.InstrumentType.YM2610;
                chip.id = (byte) i;
                chip.instrument = ym2610;
                chip.update = ym2610::update;
                chip.start = ym2610::start;
                chip.stop = ym2610::stop;
                chip.reset = ym2610::reset;
                chip.samplingRate = SamplingRate;
                chip.clock = opnbMasterClock;
                chip.volume = 0;
                chip.option = new Object[] {GetApplicationFolder()};
                lstChips.add(chip);
            }
            Ym2151 ym2151 = new Ym2151();
            for (int i = 0; i < 1; i++) {
                chip = new MDSound.Chip();
                chip.type = MDSound.InstrumentType.YM2151;
                chip.id = (byte) i;
                chip.instrument = ym2151;
                chip.update = ym2151::update;
                chip.start = ym2151::start;
                chip.stop = ym2151::stop;
                chip.reset = ym2151::reset;
                chip.samplingRate = SamplingRate;
                chip.clock = opmMasterClock;
                chip.volume = 0;
                chip.option = null;
                lstChips.add(chip);
            }
            mds = new MDSound(SamplingRate, samplingBuffer, lstChips.toArray(MDSound.Chip[]::new));

            List<ChipAction> lca = new ArrayList<>();
            MucomChipAction ca;
            ca = new MucomChipAction(Program::OPNAWriteP, null, Program::OPNAWaitSend);
            lca.add(ca);
            ca = new MucomChipAction(Program::OPNAWriteS, null, null);
            lca.add(ca);
            ca = new MucomChipAction(Program::OPNBWriteP, Program::OPNBWriteAdpcmP, null);
            lca.add(ca);
            ca = new MucomChipAction(Program::OPNBWriteS, Program::OPNBWriteAdpcmS, null);
            lca.add(ca);
            ca = new MucomChipAction(Program::OPMWriteP, null, null);
            lca.add(ca);

            drv = new Driver(null);
            drv.Init(lca, blary, null,
                    new Object[] {false, isLoadADPCM, loadADPCMOnly, args[fnIndex]}
            );

            if (mh.SSGExtend) {
//                mds.ChangeYM2608_PSGMode(0, 1); // new impl @see "TAG106"
//                mds.ChangeYM2608_PSGMode(1, 1);
//                mds.ChangeYM2610_PSGMode(0, 1);
//                mds.ChangeYM2610_PSGMode(1, 1);
            }

            List<Tuple<String, String>> tags = drv.GetTags();
            if (tags != null) {
                for (Tuple<String, String> tag : tags) {
                    if (tag.getItem1().isEmpty()) continue;
                    Debug.printf("%-16s : %s", tag.getItem1(), tag.getItem2());
                }
            }

            if (loadADPCMOnly) return;

            drv.StartRendering(SamplingRate, Arrays.asList(
                    new Tuple<>("YM2608", opnaMasterClock),
                    new Tuple<>("YM2608", opnaMasterClock),
                    new Tuple<>("YM2610B", opnbMasterClock),
                    new Tuple<>("YM2610B", opnbMasterClock),
                    new Tuple<>("YM2151", opmMasterClock)).toArray(Tuple[]::new));

            switch (device) {
            case 0:
                audioOutput.start();
                break;
            case 1:
            case 2:
                trdMain.start();
                break;
            }

            drv.MusicSTART(0);

            Debug.printf("終了する場合は何かキーを押してください");

            while (true) {
                Thread.sleep(1);
                if (System.in.available() != 0) {
                    break;
                }
                // ステータスが 0 (終了)又は 0 未満(エラー)の場合はループを抜けて終了
                if (drv.GetStatus() <= 0) {
                    if (drv.GetStatus() == 0) {
                        Thread.sleep((int) (latency * 2.0)); // 実際の音声が発音しきるまで latency * 2 の分だけ待つ
                    }
                    break;
                }
            }

            drv.MusicSTOP();
            drv.StopRendering();
        } catch (Exception ex) {
            Debug.printf(Level.SEVERE, "演奏失敗");
            Debug.printf(Level.SEVERE, String.format("message:%s", ex.getMessage()));
            Debug.printf(Level.SEVERE, String.format("stackTrace:%s", Arrays.toString(ex.getStackTrace())));
        } finally {
            if (audioOutput != null) {
                audioOutput.stop();
                while (audioOutput.isRunning()) {
                    audioOutput.drain();
                }
                audioOutput.close();
            }
            if (trdMain != null) {
                trdClosed = true;
                while (!trdStopped) {
                    try { Thread.sleep(1); } catch (InterruptedException e) {}
                }
            }
        }
    }

    private static void OPNAWaitSend(long elapsed, int size) {
        // nothing to do for emu
    }

    private static RSoundChip CheckDevice() {
        // nothing to do for emu
        return null;
    }

    private static int AnalyzeOption(String[] args) {
        int i = 0;

        device = 0;
        loop = 0;
        isLoadADPCM = true;

        while (i < args.length && args[i] != null && args[i].length() > 0 && args[i].charAt(0) == '-') {
            String op = args[i].substring(1).toUpperCase();
            if (op.equals("D=EMU")) {
                device = 0;
            }
            if (op.equals("D=GIMIC")) {
                device = 1;
            }
            if (op.equals("D=SCCI")) {
                device = 2;
            }
            if (op.equals("D=WAVE")) {
                device = 3;
            }

            if (op.length() > 2 && op.substring(0, 2).equals("L=")) {
                try {
                    loop = Integer.parseInt(op.substring(2));
                } catch (NumberFormatException e) {
                    loop = 0;
                }
            }

            if (op.length() > 10 && op.startsWith("LOADADPCM=")) {
                if (op.substring(10).equals("ONLY")) {
                    loadADPCMOnly = true;
                    isLoadADPCM = true;
                } else {
                    loadADPCMOnly = false;
                    isLoadADPCM = !Boolean.parseBoolean(op.substring(10));
                }
            }

            i++;
        }

        if (device == 3 && loop == 0) loop = 1;

        return i;
    }

    public static String GetApplicationFolder() {
        String path = Path.getDirectoryName(System.getProperty("user.dir"));
        if (!StringUtilities.isNullOrEmpty(path)) {
            path += path.charAt(path.length() - 1) == '\\' ? "" : "\\";
        }
        return path;
    }

    static void WriteLine(LogLevel level, String msg) {
        System.err.printf("[%-7d] %s", level, msg);
    }

    private static int EmuCallback(short[] buffer, int offset, int count) {
        try {
            long bufCnt = count / 2;

            for (int i = 0; i < bufCnt; i++) {
                mds.update(emuRenderBuf, 0, 2, Program::OneFrame);

                buffer[offset + i * 2 + 0] = emuRenderBuf[0];
                buffer[offset + i * 2 + 1] = emuRenderBuf[1];

            }
        } catch (Exception ex) {
            //Debug.printf(Level.SEVERE, String.Format("{0} {1}", ex.Message, ex.StackTrace));
        }

        return count;
    }

    private static void RealCallback() {
        double o = sw.getElapsedMilliseconds() / swFreq;
        double step = 1 / (double) SamplingRate;

        trdStopped = false;
        try {
            while (!trdClosed) {
                Thread.sleep(0);

                double el1 = sw.getElapsedMilliseconds() / swFreq;
                if (el1 - o < step) continue;
                if (el1 - o >= step * SamplingRate / 100.0) // 閾値10ms
                {
                    do {
                        o += step;
                    } while (el1 - o >= step);
                } else {
                    o += step;
                }

                OneFrame();
            }
        } catch (Exception e) {
        }
        trdStopped = true;
    }

    private static void OneFrame() {
        drv.Rendering();
    }

    private static void OPNAWriteP(ChipDatum dat) {
        OPNAWrite(0, dat);
    }

    private static void OPNAWriteS(ChipDatum dat) {
        OPNAWrite(1, dat);
    }

    private static void OPNBWriteP(ChipDatum dat) {
        OPNBWrite(0, dat);
    }

    private static void OPNBWriteS(ChipDatum dat) {
        OPNBWrite(1, dat);
    }

    private static void OPMWriteP(ChipDatum dat) {
        OPMWrite(0, dat);
    }

    private static void OPNBWriteAdpcmP(byte[] pcmData, int s, int e) {
        if (s == 0) OPNBWrite_AdpcmA(0, pcmData);
        else OPNBWrite_AdpcmB(0, pcmData);
    }

    private static void OPNBWriteAdpcmS(byte[] pcmData, int s, int e) {
        if (s == 0) OPNBWrite_AdpcmA(1, pcmData);
        else OPNBWrite_AdpcmB(1, pcmData);
    }


    private static void OPNAWrite(int chipId, ChipDatum dat) {
        if (dat != null && dat.addtionalData != null) {
            MmlDatum md = (MmlDatum) dat.addtionalData;
            if (md.linePos != null) {
                //Debug.printf(Level.FINEST, String.Format("! OPNA i{0} r{1} c{2}"
                //, chipId
                //, md.linePos.row
                //, md.linePos.col
                //));
            }
        }

        if (dat.address == -1) return;
        //Debug.printf(Level.FINEST, String.Format("Out ChipA:{0} Port:{1} Adr:[{2:x02}] val[{3:x02}]", chipId, dat.port, (int)dat.address, (int)dat.data));

        switch (device) {
        case 0:
            mds.writeYM2608((byte) chipId, (byte) dat.port, (byte) dat.address, (byte) dat.data);
            break;
        case 1:
        case 2:
            rsc.setRegister(dat.port * 0x100 + dat.address, dat.data);
            break;
        }
    }

    private static void OPNBWrite(int chipId, ChipDatum dat) {
        if (dat != null && dat.addtionalData != null) {
            MmlDatum md = (MmlDatum) dat.addtionalData;
            if (md.linePos != null) {
                //Debug.printf(Level.FINEST, String.Format("! OPNB i{0} r{1} c{2}"
                //, chipId
                //, md.linePos.row
                //, md.linePos.col
                //));
            }
        }

        if (dat.address == -1) return;
        //Debug.printf(Level.FINEST, String.Format("Out ChipB:{0} Port:{1} Adr:[{2:x02}] val[{3:x02}]", chipId, dat.port, (int)dat.address, (int)dat.data));

        switch (device) {
        case 0:
            mds.writeYM2610((byte) chipId, (byte) dat.port, (byte) dat.address, (byte) dat.data);
            break;
        case 1:
        case 2:
            rsc.setRegister(dat.port * 0x100 + dat.address, dat.data);
            break;
        }
    }

    private static void OPMWrite(int chipId, ChipDatum dat) {
        if (dat != null && dat.addtionalData != null) {
            MmlDatum md = (MmlDatum) dat.addtionalData;
            if (md.linePos != null) {
                //Debug.printf(Level.FINEST, String.Format("! OPM i{0} r{1} c{2}"
                //    , chipId
                //    , md.linePos.row
                //    , md.linePos.col
                //    ));
            }
        }

        if (dat.address == -1) return;

        //if (dat.address == 0x27)// && d <= 0x1d) {
        //    Debug.printf(Level.FINEST, String.Format("Out ChipOPM:{0} Port:{1} Adr:[{2:x02}] val[{3:x02}]", chipId, dat.port, (int)dat.address, (int)dat.data));
        //}
        switch (device) {
        case 0:
            mds.writeYM2151((byte) chipId, (byte) dat.address, (byte) dat.data);
            break;
        case 1:
        case 2:
            rsc.setRegister(dat.address, dat.data);
            break;
        }
    }

    private static void OPNBWrite_AdpcmA(int chipId, byte[] pcmData) {
        switch (device) {
        case 0:
            mds.WriteYM2610_SetAdpcmA((byte) chipId, pcmData);
            break;
        case 1:
        case 2:
            break;
        }
    }

    private static void OPNBWrite_AdpcmB(int chipId, byte[] pcmData) {
        switch (device) {
        case 0:
            mds.WriteYM2610_SetAdpcmB((byte) chipId, pcmData);
            break;
        case 1:
        case 2:
            break;
        }
    }
}
