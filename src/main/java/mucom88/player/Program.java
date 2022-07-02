package mucom88.player;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import NScci;
import Nc86ctl;
import System.IO;
import System.Threading;
import dotnet4j.Tuple;
import dotnet4j.io.File;
import dotnet4j.io.Path;
import dotnet4j.util.compat.StopWatch;
import dotnet4j.util.compat.StringUtilities;
import javassist.tools.reflect.Reflection;
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


class Program {
    private static DirectSoundOut audioOutput = null;
    public TriFunction<short[], Integer, Integer, Integer> naudioCallBack;
    private static naudioCallBack callBack = null;
    private static Thread trdMain = null;
    private static StopWatch sw = null;
    private static double swFreq = 0;
    public static boolean trdClosed = false;
    private static final Object lockObj = new Object();
    private static boolean _trdStopped = true;
    private static boolean trdStopped;

    public boolean gettrdStopped() {
        synchronized (lockObj) {
            return _trdStopped;
        }
    }

    public void settrdStopped(boolean value) {
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

    private static NScci.NScci nScci;
    private static Nc86ctl.Nc86ctl nc86ctl;
    private static RSoundChip rsc;

    static int Main(String[] args) {
        Log.writeLine += WriteLine;
//#if DEBUG
        //Log.writeLine += WriteLineF;
//            Log.level = LogLevel.TRACE;// TRACE;
//#else
        Log.level = LogLevel.INFO;
//#endif
        int fnIndex = AnalyzeOption(args);

        if (args == null || args.length != fnIndex + 1) {
            Log.writeLine(LogLevel.ERROR, "引数(.mubファイル)１個欲しいよぉ");
            return -1;
        }
        if (!File.exists(args[fnIndex])) {
            Log.writeLine(LogLevel.ERROR, "ファイルが見つかりません");
            return -1;
        }

        rsc = CheckDevice();

        try {

            SineWaveProvider16 waveProvider;
            int latency = 1000;

            switch (device) {
            case 0:
                waveProvider = new SineWaveProvider16();
                waveProvider.SetWaveFormat((int) SamplingRate, 2);
                callBack = EmuCallback;
                audioOutput = new DirectSoundOut(latency);
                audioOutput.Init(waveProvider);
                break;
            case 1:
            case 2:
                trdMain = new Thread(new ThreadStart(RealCallback));
                trdMain.Priority = ThreadPriority.Highest;
                trdMain.IsBackground = true;
                trdMain.Name = "trdVgmReal";
                sw = Stopwatch.StartNew();
                swFreq = Stopwatch.Frequency;
                break;
            }

//#if NETCOREAPP
            System.Text.Encoding.RegisterProvider(System.Text.CodePagesEncodingProvider.Instance);
//#endif

            List<MmlDatum> bl = new ArrayList<MmlDatum>();
            byte[] srcBuf = File.readAllBytes(args[fnIndex]);
            for (byte b : srcBuf) bl.add(new MmlDatum(b));
            MmlDatum[] blary = bl.toArray(new MmlDatum[0]);

            MUBHeader mh = new MUBHeader(blary, MyEncoding.Default());
            mh.GetTags();
            if (mh.OPMClockMode == MUBHeader.enmOPMClockMode.X68000) opmMasterClock = Driver.cOPMMasterClock_X68k;

            List<MDSound.Chip> lstChips = new ArrayList<MDSound.Chip>();
            MDSound.Chip chip = null;

            Ym2608 ym2608 = new Ym2608();
            for (int i = 0; i < 2; i++) {
                chip = new MDSound.Chip() {{
                    type = MDSound.InstrumentType.YM2608;
                    ID = (byte) i;
                    Instrument = ym2608;
                    Update = ym2608::Update;
                    Start = ym2608::Start;
                    Stop = ym2608::Stop;
                    Reset = ym2608::Reset;
                    SamplingRate = SamplingRate;
                    Clock = opnaMasterClock;
                    Volume = 0;
                    Option = new Object[] {GetApplicationFolder()};
                }};
                lstChips.add(chip);
            }
            Ym2610 ym2610 = new Ym2610();
            for (int i = 0; i < 2; i++) {
                chip = new MDSound.Chip() {{
                    type = MDSound.InstrumentType.YM2610;
                    ID = (byte) i;
                    Instrument = ym2610;
                    Update = ym2610.Update;
                    Start = ym2610.Start;
                    Stop = ym2610.Stop;
                    Reset = ym2610.Reset;
                    SamplingRate = SamplingRate;
                    Clock = opnbMasterClock;
                    Volume = 0;
                    Option = new Object[] {GetApplicationFolder()};
                }};
                lstChips.add(chip);
            }
            Ym2151 ym2151 = new Ym2151();
            for (int i = 0; i < 1; i++) {
                chip = new MDSound.Chip() {{
                    type = MDSound.InstrumentType.YM2151;
                    ID = (byte) i;
                    Instrument = ym2151;
                    Update = ym2151::update;
                    Start = ym2151::start;
                    Stop = ym2151::stop;
                    Reset = ym2151::reset;
                    SamplingRate = SamplingRate;
                    Clock = opmMasterClock;
                    Volume = 0;
                    Option = null;
                }};
                lstChips.add(chip);
            }
            mds = new MDSound(SamplingRate, samplingBuffer
                    , lstChips.toArray(MDSound.Chip[]::new));


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
            drv.Init(
                    lca
                    , blary
                    , null
                    , new Object[] {
                            false
                            , isLoadADPCM
                            , loadADPCMOnly
                            , args[fnIndex]
                    }
            );

            if (mh.SSGExtend) {
                mds.ChangeYM2608_PSGMode(0, 1);
                mds.ChangeYM2608_PSGMode(1, 1);
                mds.ChangeYM2610_PSGMode(0, 1);
                mds.ChangeYM2610_PSGMode(1, 1);
            }

            List<Tuple<String, String>> tags = drv.GetTags();
            if (tags != null) {
                for (Tuple<String, String> tag : tags) {
                    if (tag.getItem1() == "") continue;
                    Log.writeLine(LogLevel.INFO, String.format("{0,-16} : {1}", tag.getItem1(), tag.getItem2()));
                }
            }

            if (loadADPCMOnly) return 0;

            drv.StartRendering(SamplingRate,
                    Arrays.<Tuple<String, Integer>>asList(
                            new Tuple<>("YM2608", (int) opnaMasterClock)
                            , new Tuple<>("YM2608", (int) opnaMasterClock)
                            , new Tuple<>("YM2610B", (int) opnbMasterClock)
                            , new Tuple<>("YM2610B", (int) opnbMasterClock)
                            , new Tuple<>("YM2151", (int) opmMasterClock)
                    ).toArray(new Tuple[0]));

            switch (device) {
            case 0:
                audioOutput.Play();
                break;
            case 1:
            case 2:
                trdMain.Start();
                break;
            }

            drv.MusicSTART(0);

            Log.writeLine(LogLevel.INFO, "終了する場合は何かキーを押してください");

            while (true) {
                Thread.sleep(1);
                if (System.in.available() != 0) {
                    break;
                }
                //ステータスが0(終了)又は0未満(エラー)の場合はループを抜けて終了
                if (drv.GetStatus() <= 0) {
                    if (drv.GetStatus() == 0) {
                        Thread.sleep((int) (latency * 2.0));//実際の音声が発音しきるまでlatency*2の分だけ待つ
                    }
                    break;
                }
            }

            drv.MusicSTOP();
            drv.StopRendering();
        } catch (Exception ex) {
            Log.writeLine(LogLevel.FATAL, "演奏失敗");
            Log.writeLine(LogLevel.FATAL, String.format("message:%s", ex.getMessage()));
            Log.writeLine(LogLevel.FATAL, String.format("stackTrace:%s", Arrays.toString(ex.getStackTrace())));
        } finally {
            if (audioOutput != null) {
                audioOutput.stop();
                while (audioOutput.PlaybackState == PlaybackState.Playing) {
                    Thread.sleep(1);
                }
                audioOutput.Dispose();
                audioOutput = null;
            }
            if (trdMain != null) {
                trdClosed = true;
                while (!trdStopped) {
                    Thread.sleep(1);
                }
            }
            if (nc86ctl != null) {
                nc86ctl.deinitialize();
                nc86ctl = null;
            }
            if (nScci != null) {
                nScci.Dispose();
                nScci = null;
            }
        }

        return 0;
    }

    private static void OPNAWaitSend(long elapsed, int size) {
        switch (device) {
        case 0://EMU
            return;
        case 1://GIMIC

            //サイズと経過時間から、追加でウエイトする。
            int m = Math.max((int) (size / 20 - elapsed), 0);//20 閾値(magic number)
            Thread.sleep(m);

            //ポートも一応見る
            int n = nc86ctl.getNumberOfChip();
            for (int i = 0; i < n; i++) {
                NIRealChip rc = nc86ctl.getChipInterface(i);
                if (rc != null) {
                    while ((rc. @in(0x0) &0x83) !=0)
                    Thread.Sleep(0);
                    while ((rc. @in(0x100) &0xbf) !=0)
                    Thread.Sleep(0);
                }
            }

            break;
        case 2://SCCI
            nScci.NSoundInterfaceManager_.sendData();
            while (!nScci.NSoundInterfaceManager_.isBufferEmpty()) {
                Thread.sleep(0);
            }
            break;
        }
    }

    private static RSoundChip CheckDevice() {
        SChipType ct = null;
        int iCount = 0;

        switch (device) {
        case 1://GIMIC存在チェック
            nc86ctl = new Nc86ctl.Nc86ctl();
            nc86ctl.initialize();
            iCount = nc86ctl.getNumberOfChip();
            if (iCount == 0) {
                nc86ctl.deinitialize();
                nc86ctl = null;
                Log.WriteLine(LogLevel.ERROR, "Not found G.I.M.I.C.");
                device = 0;
                break;
            }
            for (int i = 0; i < iCount; i++) {
                NIRealChip rc = nc86ctl.getChipInterface(i);
                NIGimic2 gm = rc.QueryInterface();
                ChipType cct = gm.getModuleType();
                int o = -1;
                if (cct == ChipType.CHIP_YM2608 || cct == ChipType.CHIP_YMF288 || cct == ChipType.CHIP_YM2203) {
                    ct = new SChipType();
                    ct.SoundLocation = -1;
                    ct.BusID = i;
                    String seri = gm.getModuleInfo().Serial;
                    if (!int.TryParse(seri, out o)) {
                        o = -1;
                        ct = null;
                        continue;
                    }
                    ct.SoundChip = o;
                    ct.ChipName = gm.getModuleInfo().Devname;
                    ct.InterfaceName = gm.getMBInfo().Devname;
                    break;
                }
            }
            RC86ctlSoundChip rsc = null;
            if (ct == null) {
                nc86ctl.deinitialize();
                nc86ctl = null;
                Log.WriteLine(LogLevel.ERROR, "Not found G.I.M.I.C.(OPNA module)");
                device = 0;
            } else {
                rsc = new RC86ctlSoundChip(-1, ct.BusID, ct.SoundChip);
                rsc.c86ctl = nc86ctl;
                rsc.init();

                rsc.SetMasterClock(7987200);//SoundBoardII
                rsc.setSSGVolume(63);//PC-8801
            }
            return rsc;
        case 2://SCCI存在チェック
            nScci = new NScci.NScci();
            iCount = nScci.NSoundInterfaceManager_.getInterfaceCount();
            if (iCount == 0) {
                nScci.Dispose();
                nScci = null;
                Log.WriteLine(LogLevel.ERROR, "Not found SCCI.");
                device = 0;
                break;
            }
            for (int i = 0; i < iCount; i++) {
                NSoundInterface iIntfc = nScci.NSoundInterfaceManager_.getInterface(i);
                NSCCI_INTERFACE_INFO iInfo = nScci.NSoundInterfaceManager_.getInterfaceInfo(i);
                int sCount = iIntfc.getSoundChipCount();
                for (int s = 0; s < sCount; s++) {
                    NSoundChip sc = iIntfc.getSoundChip(s);
                    int t = sc.getSoundChipType();
                    if (t == 1) {
                        ct = new SChipType();
                        ct.SoundLocation = 0;
                        ct.BusID = i;
                        ct.SoundChip = s;
                        ct.ChipName = sc.getSoundChipInfo().cSoundChipName;
                        ct.InterfaceName = iInfo.cInterfaceName;
                                goto scciExit;
                    }
                }
            }
            scciExit:
            ;
            RScciSoundChip rssc = null;
            if (ct == null) {
                nScci.Dispose();
                nScci = null;
                Log.WriteLine(LogLevel.ERROR, "Not found SCCI(OPNA module).");
                device = 0;
            } else {
                rssc = new RScciSoundChip(0, ct.BusID, ct.SoundChip);
                rssc.scci = nScci;
                rssc.init();
            }
            return rssc;
        }

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

            if (op.length() > 10 && op.substring(0, 10).equals("LOADADPCM=")) {
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

    //private static long traceLine = 0;
    private static void WriteLineF(LogLevel level, String msg) {
        //traceLine++;
        //if (traceLine < 48434) return;
        //File.AppendAllText(@"C:\Users\kuma\Desktop\new.log", String.Format("[{0,-7}] {1}" + Environment.NewLine, level, msg));
    }

    static void WriteLine(LogLevel level, String msg) {
        System.err.printf("[%-7d] %s", level, msg);
    }

    private static int EmuCallback(short[] buffer, int offset, int count) {
        try {
            long bufCnt = count / 2;

            for (int i = 0; i < bufCnt; i++) {
                mds.update(emuRenderBuf, 0, 2, this::OneFrame);

                buffer[offset + i * 2 + 0] = emuRenderBuf[0];
                buffer[offset + i * 2 + 1] = emuRenderBuf[1];

            }
        } catch (Exception ex) {
            //Log.WriteLine(LogLevel.FATAL, String.Format("{0} {1}", ex.Message, ex.StackTrace));
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
                //Log.WriteLine(LogLevel.TRACE, String.Format("! OPNA i{0} r{1} c{2}"
                //, chipId
                //, md.linePos.row
                //, md.linePos.col
                //));
            }
        }

        if (dat.address == -1) return;
        //Log.WriteLine(LogLevel.TRACE, String.Format("Out ChipA:{0} Port:{1} Adr:[{2:x02}] val[{3:x02}]", chipId, dat.port, (int)dat.address, (int)dat.data));

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
                //Log.WriteLine(LogLevel.TRACE, String.Format("! OPNB i{0} r{1} c{2}"
                //, chipId
                //, md.linePos.row
                //, md.linePos.col
                //));
            }
        }

        if (dat.address == -1) return;
        //Log.WriteLine(LogLevel.TRACE, String.Format("Out ChipB:{0} Port:{1} Adr:[{2:x02}] val[{3:x02}]", chipId, dat.port, (int)dat.address, (int)dat.data));

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
                //Log.WriteLine(LogLevel.TRACE, String.Format("! OPM i{0} r{1} c{2}"
                //    , chipId
                //    , md.linePos.row
                //    , md.linePos.col
                //    ));
            }
        }

        if (dat.address == -1) return;

        //if (dat.address == 0x27)// && d <= 0x1d) {
        //    Log.WriteLine(LogLevel.TRACE, String.Format("Out ChipOPM:{0} Port:{1} Adr:[{2:x02}] val[{3:x02}]", chipId, dat.port, (int)dat.address, (int)dat.data));
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

    public static class SineWaveProvider16 extends WaveProvider16 {

        @Override
        public int Read(short[] buffer, int offset, int sampleCount) {
            return callBack(buffer, offset, sampleCount);
        }
    }
}
