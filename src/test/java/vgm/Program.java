package vgm;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import dotnet4j.io.File;
import dotnet4j.io.Path;
import dotnet4j.util.compat.StringUtilities;
import dotnet4j.util.compat.Tuple;
import mucom88.common.MucomChipAction;
import mucom88.driver.Driver;
import mucom88.driver.MubHeader;
import musicDriverInterface.ChipAction;
import musicDriverInterface.ChipDatum;
import musicDriverInterface.MmlDatum;
import musicDriverInterface.IDriver;
import vavi.util.Debug;


class Program {
    private static final int SamplingRate = 44100; // vgm format freq
    private static int opmMasterClock = 3579545;
    private static final int opnaMasterClock = 7987200;
    private static final int opnbMasterClock = 8000000;

    private static IDriver driver = null;
    private static VgmWriter writer = null;
    private static int loop = 2;
    private static List<Tuple<String, String>> tags = null;

    public static void main(String[] args) {
        int fnIndex = analyzeOption(args);

        if (args == null || args.length != fnIndex + 1) {
            System.err.println("引数(.mubファイル)１個欲しいよぉ");
            System.exit(-1);
        }
        if (!File.exists(args[fnIndex])) {
            System.err.println("ファイルが見つかりません");
            System.exit(-1);
        }

        try {
            writer = new VgmWriter();
            writer.open(Path.combine(Path.getDirectoryName(args[fnIndex]),
                    Path.getFileNameWithoutExtension(args[fnIndex]) + ".vgm"));

            List<ChipAction> actions = new ArrayList<>();
            MucomChipAction action;
            action = new MucomChipAction(Program::writeOPNAP, null, Program::sendOPNAWait);
            actions.add(action);
            action = new MucomChipAction(Program::writeOPNAS, null, null);
            actions.add(action);
            action = new MucomChipAction(Program::writeOPNBP, Program::writeOPNBAdpcmP, null);
            actions.add(action);
            action = new MucomChipAction(Program::writeOPNBS, Program::writeOPNBAdpcmS, null);
            actions.add(action);
            action = new MucomChipAction(Program::writeOPMP, null, null);
            actions.add(action);

            List<MmlDatum> temp = new ArrayList<>();
            byte[] srcBuf = File.readAllBytes(args[fnIndex]);
            for (byte b : srcBuf) temp.add(new MmlDatum(b & 0xff));
            writer.useChipsFromMub(srcBuf);
            MmlDatum[] buf = temp.toArray(MmlDatum[]::new);

            MubHeader header = new MubHeader(buf);
            header.getTags();
            if (header.opmClockMode == MubHeader.enmOPMClockMode.X68000) opmMasterClock = Driver.cOPMMasterClock_X68k;

            driver = new Driver();
            driver.init(actions, temp.toArray(MmlDatum[]::new), null, false, true, false, args[fnIndex]);

            driver.setLoopCount(loop);

            tags = driver.getTags();
            if (tags != null) {
                for (Tuple<String, String> tag : tags) {
                    if (tag.getItem1().isEmpty()) continue;
                    Debug.printf(Level.INFO, String.format("%-16s : %s", tag.getItem1(), tag.getItem2()));
                }
            }

            for (int i = 0; i < 2; i++) {
                byte[] pcmSrcData = ((Driver) driver).pcm[i];
                if (pcmSrcData != null) {
                    int pcmStartPos = ((Driver) driver).pcmStartPos[i];
                    if (pcmStartPos < pcmSrcData.length) {
                        byte[] pcmData = new byte[pcmSrcData.length - pcmStartPos];
                        System.arraycopy(pcmSrcData, pcmStartPos, pcmData, 0, pcmData.length - pcmStartPos);
                        if (pcmData.length > 0) writer.writeAdpcm((byte) i, pcmData);
                    }
                }
            }

            driver.startRendering(SamplingRate,
                    new Tuple<>("YM2608", opnaMasterClock),
                    new Tuple<>("YM2608", opnaMasterClock),
                    new Tuple<>("YM2610B", opnbMasterClock),
                    new Tuple<>("YM2610B", opnbMasterClock),
                    new Tuple<>("YM2151", opmMasterClock)
            );

            driver.startMusic(0);

            while (true) {

                driver.render();
                writer.incrementWaitCOunter();

                //ステータスが0(終了)又は0未満(エラー)の場合はループを抜けて終了
                if (driver.getStatus() <= 0) {
                    break;
                }
            }

            driver.stopMusic();
            driver.stopRendering();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close(tags, opnaMasterClock, opnbMasterClock, opmMasterClock);
            }
        }
    }

    private static int analyzeOption(String[] args) {
        int i = 0;
        loop = 2;

        while (args != null &&
                args.length > 0 &&
                args[i].length() > 0 &&
                args[i] != null &&
                args[i].charAt(0) == '-') {
            String op = args[i].substring(1).toUpperCase();
            if (op.length() > 2 && op.startsWith("L=")) {
                try {
                    loop = Integer.parseInt(op.substring(2));
                } catch (NumberFormatException e) {
                    Debug.println(Level.WARNING, e);
                    loop = 2;
                }
            }

            i++;
        }

        return i;
    }

    public static String getApplicationFolder() {
        String path = Path.getDirectoryName(System.getProperty("user.dir"));
        if (!StringUtilities.isNullOrEmpty(path)) {
            path += path.charAt(path.length() - 1) == '\\' ? "" : "\\";
        }
        return path;
    }

    private static void writeOPNA(ChipDatum dat) {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
                Debug.printf(Level.FINEST, String.format("! r%d c%d", md.linePos.row, md.linePos.col));
            }
        }
        if (dat.address == -1) return;

        writer.writeYM2608(0, (byte) dat.port, (byte) dat.address, (byte) dat.data);
    }

    private static void sendOPNAWait(long elapsed, int size) {
    }

    private static void writeOPNAP(ChipDatum dat) {
        writeOPNA(0, dat);
    }

    private static void writeOPNAS(ChipDatum dat) {
        writeOPNA(1, dat);
    }

    private static void writeOPNBP(ChipDatum dat) {
        writeOPNB(0, dat);
    }

    private static void writeOPNBS(ChipDatum dat) {
        writeOPNB(1, dat);
    }

    private static void writeOPMP(ChipDatum dat) {
        writeOPM(0, dat);
    }

    private static void writeOPNBAdpcmP(byte[] pcmData, int s, int e) {
        if (s == 0) writeOPNBAdpcmA(0, pcmData);
        else writeOPNBAdpcmB(0, pcmData);
    }

    private static void writeOPNBAdpcmS(byte[] pcmData, int s, int e) {
        if (s == 0) writeOPNBAdpcmA(1, pcmData);
        else writeOPNBAdpcmB(1, pcmData);
    }

    private static void writeOPNA(int chipId, ChipDatum dat) {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
                Debug.printf(Level.FINEST, String.format("! OPNA i%d r%d c%d", chipId, md.linePos.row, md.linePos.col));
            }
        }
        if (dat.address == -1) return;

        Debug.printf(Level.FINEST, String.format("Out ChipA:%d Port:%d adr:[%02x] val[%02x]", chipId, dat.port, dat.address, dat.data));

        writer.writeYM2608(chipId, (byte) dat.port, (byte) dat.address, (byte) dat.data);
    }

    private static void writeOPNB(int chipId, ChipDatum dat) {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
                Debug.printf(Level.FINEST, String.format("! OPNB i%d r%d c%d", chipId, md.linePos.row, md.linePos.col));
            }
        }
        if (dat.address == -1) return;

        Debug.printf(Level.FINEST, String.format("Out ChipB:%d Port:%d adr:[%02x] val[%02x]", chipId, dat.port, dat.address, dat.data));

        writer.writeYM2610(chipId, (byte) dat.port, (byte) dat.address, (byte) dat.data);
    }

    private static void writeOPNBAdpcmA(int chipId, byte[] pcmData) {
        writer.writeYM2610SetAdpcmA(chipId, pcmData);
    }

    private static void writeOPNBAdpcmB(int chipId, byte[] pcmData) {
        writer.writeYM2610SetAdpcmB(chipId, pcmData);
    }

    private static void writeOPM(int chipId, ChipDatum dat) {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
                Debug.printf(Level.FINEST, String.format("! OPM i%d r%d c%d", chipId, md.linePos.row, md.linePos.col));
            }
        }
        if (dat.address == -1) return;

        Debug.printf(Level.FINEST, String.format("Out OPM Chip:%d Port:%d adr:[%02x] val[%02x]", chipId, dat.port, dat.address, dat.data));

        writer.writeYM2151(chipId, (byte) dat.address, (byte) dat.data);
    }
}
