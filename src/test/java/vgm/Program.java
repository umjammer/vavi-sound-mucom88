package vgm;

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
import mucom88.common.MucomChipAction;
import mucom88.common.MyEncoding;
import mucom88.driver.Driver;
import mucom88.driver.MUBHeader;
import musicDriverInterface.ChipAction;
import musicDriverInterface.ChipDatum;
import musicDriverInterface.MmlDatum;
import musicDriverInterface.iDriver;
import vavi.util.Debug;
import vgm.VgmWriter;


class Program {
    private static final int SamplingRate = 44100;//vgm format freq
    private static int opmMasterClock = 3579545;
    private static final int opnaMasterClock = 7987200;
    private static final int opnbMasterClock = 8000000;

    private static iDriver drv = null;
    private static VgmWriter vw = null;
    private static int loop = 2;
    private static List<Tuple<String, String>> tags = null;

    static int Main(String[] args) {
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
            vw = new VgmWriter();
            vw.Open(
                    Path.combine(
                            Path.getDirectoryName(args[fnIndex])
                            , Path.getFileNameWithoutExtension(args[fnIndex]) + ".vgm")
            );

//#if NETCOREAPP
//                System.Text.Encoding.RegisterProvider(System.Text.CodePagesEncodingProvider.Instance);
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

            List<MmlDatum> bl = new ArrayList<>();
            byte[] srcBuf = File.readAllBytes(args[fnIndex]);
            for (byte b : srcBuf) bl.add(new MmlDatum(b));
            vw.useChipsFromMub(srcBuf);
            MmlDatum[] blary = bl.toArray(MmlDatum[]::new);

            MUBHeader mh = new MUBHeader(blary, MyEncoding.Default());
            mh.GetTags();
            if (mh.OPMClockMode == MUBHeader.enmOPMClockMode.X68000) opmMasterClock = Driver.cOPMMasterClock_X68k;

            drv = new Driver(null);
            ((Driver) drv).Init(
                    lca
                    , bl.toArray(MmlDatum[]::new)
                    , null
                    , new Object[] {
                            false
                            , true
                            , false
                            , args[fnIndex]
                    }
            );

            drv.SetLoopCount(loop);

            tags = drv.GetTags();
            if (tags != null) {
                for (Tuple<String, String> tag : tags) {
                    if (tag.getItem1().isEmpty()) continue;
                    Debug.printf(Level.INFO, String.format("%-16s : %s", tag.getItem1(), tag.getItem2()));
                }
            }

            for (int i = 0; i < 2; i++) {
                byte[] pcmSrcdata = ((Driver) drv).pcm[i];
                if (pcmSrcdata != null) {
                    int pcmStartPos = ((Driver) drv).pcmStartPos[i];
                    if (pcmStartPos < pcmSrcdata.length) {
                        byte[] pcmdata = new byte[pcmSrcdata.length - pcmStartPos];
                        System.arraycopy(pcmSrcdata, pcmStartPos, pcmdata, 0, pcmdata.length - pcmStartPos);
                        if (pcmdata.length > 0) vw.WriteAdpcm((byte) i, pcmdata);
                    }
                }
            }

            drv.StartRendering((int) SamplingRate,
                    Arrays.asList(
                            new Tuple<>("YM2608", (int) opnaMasterClock)
                            , new Tuple<>("YM2608", (int) opnaMasterClock)
                            , new Tuple<>("YM2610B", (int) opnbMasterClock)
                            , new Tuple<>("YM2610B", (int) opnbMasterClock)
                            , new Tuple<>("YM2151", (int) opmMasterClock)
                    ).toArray(Tuple[]::new)
            );

            drv.MusicSTART(0);

            while (true) {

                drv.Rendering();
                vw.IncrementWaitCOunter();

                //ステータスが0(終了)又は0未満(エラー)の場合はループを抜けて終了
                if (drv.GetStatus() <= 0) {
                    break;
                }

            }

            drv.MusicSTOP();
            drv.StopRendering();
        } catch (Exception e) {
        } finally {
            if (vw != null) {
                vw.Close(tags, opnaMasterClock, opnbMasterClock, opmMasterClock);
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
            if (op.length() > 2 && op.substring(0, 2) == "L=") {
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

        vw.WriteYM2608(0, (byte) dat.port, (byte) dat.address, (byte) dat.data);
    }

    private static void OPNAWaitSend(long elapsed, int size) {
        return;
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

        vw.WriteYM2608((byte) chipId, (byte) dat.port, (byte) dat.address, (byte) dat.data);
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

        vw.WriteYM2610((byte) chipId, (byte) dat.port, (byte) dat.address, (byte) dat.data);
    }

    private static void OPNBWrite_AdpcmA(int chipId, byte[] pcmData) {
        vw.WriteYM2610_SetAdpcmA((byte) chipId, pcmData);

    }

    private static void OPNBWrite_AdpcmB(int chipId, byte[] pcmData) {
        vw.WriteYM2610_SetAdpcmB((byte) chipId, pcmData);

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

        Debug.printf(Level.FINEST, String.format("Out OPM Chip:%d Port:%d Adr:[%02x] val[%02x]", chipId, dat.port, (int) dat.address, (int) dat.data));

        vw.WriteYM2151((byte) chipId, (byte) dat.address, (byte) dat.data);
    }
}
