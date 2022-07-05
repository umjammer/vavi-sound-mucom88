package wav;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import dotnet4j.io.File;
import dotnet4j.io.Path;
import dotnet4j.util.compat.StringUtilities;
import dotnet4j.util.compat.Tuple;
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
import musicDriverInterface.iDriver;
import vavi.util.Debug;


class Program {

    private static final int SamplingRate = 55467; // 44100;
    private static final int samplingBuffer = 1024;
    private static short[] frames = new short[samplingBuffer * 4];
    private static MDSound mds = null;
    private static short[] emuRenderBuf = new short[2];
    private static iDriver drv = null;
    private static int opmMasterClock = 3579545;
    private static final int opnaMasterClock =7987200;
    private static final int opnbMasterClock =8000000;
    private static WaveWriter ww = null;
    private static int loop = 2;

    public static int main(String[] args) {
        Log.writeLine = Program::WriteLine;
//#if DEBUG
        //Log.writeLine += WriteLineF;
//            Log.level = LogLevel.INFO;//.TRACE;
//#else
        Log.level = LogLevel.INFO;
//#endif
        int fnIndex = AnalyzeOption(args);

        if (args == null || args.length != fnIndex + 1) {
            Debug.printf(Level.SEVERE, "引数(.mubファイル)１個欲しいよぉ");
            return -1;
        }
        if (!File.exists(args[fnIndex])) {
            Debug.printf(Level.SEVERE, "ファイルが見つかりません");
            return -1;
        }

        try {
            ww = new WaveWriter(SamplingRate);
            ww.Open(Path.combine(
                    Path.getDirectoryName(args[fnIndex]),
                    Path.getFileNameWithoutExtension(args[fnIndex]) + ".wav")
            );

            List<MmlDatum> bl = new ArrayList<>();
            byte[] srcBuf = File.readAllBytes(args[fnIndex]);
            for (byte b : srcBuf)bl.add(new MmlDatum(b));
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
                chip = new MDSound.Chip();
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
                chip.option = new Object[] {GetApplicationFolder()};
                lstChips.add(chip);
            }
            mds = new MDSound(SamplingRate, samplingBuffer, lstChips.toArray(MDSound.Chip[]::new));

//#if NETCOREAPP
//            System.Text.Encoding.RegisterProvider(System.Text.CodePagesEncodingProvider.Instance);
//#endif
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
                            , true
                            , false
                            , args[fnIndex]
                    }
            );

            drv.SetLoopCount(loop);

            List<Tuple<String, String>> tags = ((Driver) drv).GetTags();
            if (tags != null) {
                for (Tuple < String, String > tag : tags)
                {
                    if (tag.getItem1().isEmpty()) continue;
                    Debug.printf(Level.INFO, String.format("%-16s : %d", tag.getItem1(), tag.getItem2()));
                }
            }

            drv.StartRendering((int) SamplingRate,
                    Arrays.asList(
                            new Tuple<>("YM2608", opnaMasterClock)
                            , new Tuple<>("YM2608", opnaMasterClock)
                            , new Tuple<>("YM2610B", opnbMasterClock)
                            , new Tuple<>("YM2610B", opnbMasterClock)
                            , new Tuple<>("YM2151", opmMasterClock)
                    ).toArray(Tuple[]::new)
            );

            drv.MusicSTART(0);

            while (true) {

                EmuCallback(frames, 0, samplingBuffer);
                //ステータスが0(終了)又は0未満(エラー)の場合はループを抜けて終了
                if (drv.GetStatus() <= 0) {
                    break;
                }

                //Debug.printf(Level.FINEST, String.format("{0}  {1}",frames[0],frames[1]));
                ww.Write(frames, 0, samplingBuffer);
            }

            drv.MusicSTOP();
            drv.StopRendering();
        } catch(Exception e) {
        } finally {
            if (ww != null) {
                ww.Close();
            }
        }

        return 0;
    }

    static void WriteLine(LogLevel level, String msg) {
        System.err.printf("[%-7s] %s", level, msg);
    }

    private static int AnalyzeOption(String[] args) {
        int i = 0;
        loop = 2;

        while (args != null
                && args.length > 0
                && args[i].length() > 0
                && args[i] != null
                && args[i].charAt(0) == '-') {
            String op = args[i].substring(1).toUpperCase();
            if (op.length() > 2 && op.substring(0, 2).equals("L=")) {
                try {
                    loop = Integer.parseInt(op.substring(2));
                } catch (NumberFormatException e) {
                    loop = 2;
                }
            }

            i++;
        }

        return i;
    }

    public static String GetApplicationFolder() {
        String path = Path.getDirectoryName(System.getProperty("user.dir"));
        if (!StringUtilities.isNullOrEmpty(path)) {
            path += path.charAt(path.length() - 1) == '\\' ? "" : "\\";
        }
        return path;
    }

    private static void OPNAWrite(ChipDatum dat) {
        if (dat != null && dat.addtionalData != null) {
            MmlDatum md = (MmlDatum) dat.addtionalData;
            if (md.linePos != null) {
                Debug.printf(Level.FINEST, String.format("! r%d c%d"
                        , md.linePos.row
                        , md.linePos.col
                ));
            }
        }
        if (dat.address == -1) return;
        //Debug.printf(Level.FINEST, String.format("FM P{2} Out:Adr[{0:x02}] val[{1:x02}]", (int)dat.address, (int)dat.data,dat.port));
        mds.writeYM2608((byte) 0, (byte) dat.port, (byte) dat.address, (byte) dat.data);
    }

    private static void OPNAWaitSend(long elapsed, int size) {
        return;
    }

    private static int EmuCallback(short[] buffer, int offset, int count) {
        try {
            long bufCnt = count / 2;

            for (int i = 0; i < bufCnt; i++) {
                mds.update(emuRenderBuf, 0, 2, drv::Rendering);

                buffer[offset + i * 2 + 0] = emuRenderBuf[0];
                buffer[offset + i * 2 + 1] = emuRenderBuf[1];

            }
        } catch (Exception ex) {
            //Debug.printf(Level.SEVERE, String.format("{0} {1}", ex.Message, ex.StackTrace));
        }

        return count;
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
                Debug.printf(Level.FINEST, String.format("! OPNA i%d r%d c%d"
                        , chipId
                        , md.linePos.row
                        , md.linePos.col
                ));
            }
        }

        if (dat.address == -1) return;
        Debug.printf(Level.FINEST, String.format("Out ChipA:%d Port:%d Adr:[%02x] val[%02x]", chipId, dat.port, (int) dat.address, (int) dat.data));

        mds.writeYM2608((byte) chipId, (byte) dat.port, (byte) dat.address, (byte) dat.data);
    }

    private static void OPNBWrite(int chipId, ChipDatum dat) {
        if (dat != null && dat.addtionalData != null) {
            MmlDatum md = (MmlDatum) dat.addtionalData;
            if (md.linePos != null) {
                Debug.printf(Level.FINEST, String.format("! OPNB i%d r%d c%d"
                        , chipId
                        , md.linePos.row
                        , md.linePos.col
                ));
            }
        }

        if (dat.address == -1) return;
        Debug.printf(Level.FINEST, String.format("Out ChipB:%d Port:%d Adr:[%02x] val[%02x]", chipId, dat.port, (int) dat.address, (int) dat.data));

        mds.writeYM2610((byte) chipId, (byte) dat.port, (byte) dat.address, (byte) dat.data);
    }

    private static void OPNBWrite_AdpcmA(int chipId, byte[] pcmData) {
        mds.WriteYM2610_SetAdpcmA((byte) chipId, pcmData);

    }

    private static void OPNBWrite_AdpcmB(int chipId, byte[] pcmData) {
        mds.WriteYM2610_SetAdpcmB((byte) chipId, pcmData);

    }

    private static void OPMWrite(int chipId, ChipDatum dat) {
        if (dat != null && dat.addtionalData != null) {
            MmlDatum md = (MmlDatum) dat.addtionalData;
            if (md.linePos != null) {
                Debug.printf(Level.FINEST, String.format("! OPM i%d r%d c%d"
                        , chipId
                        , md.linePos.row
                        , md.linePos.col
                ));
            }
        }

        if (dat.address == -1) return;
        Debug.printf(Level.FINEST, String.format("Out OPMChip:%d Port:%d Adr:[%02x] val[%02x]", chipId, dat.port, (int) dat.address, (int) dat.data));

        mds.writeYM2151((byte) chipId, (byte) dat.address, (byte) dat.data);
    }
}

