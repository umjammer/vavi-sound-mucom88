package wav;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import dotnet4j.io.File;
import dotnet4j.io.Path;
import dotnet4j.util.compat.StringUtilities;
import dotnet4j.util.compat.Tuple;
import mdsound.MDSound;
import mdsound.instrument.Ym2151Inst;
import mdsound.instrument.Ym2608Inst;
import mdsound.instrument.Ym2610Inst;
import mucom88.common.MucomChipAction;
import mucom88.driver.Driver;
import mucom88.driver.MubHeader;
import musicDriverInterface.ChipAction;
import musicDriverInterface.ChipDatum;
import musicDriverInterface.MmlDatum;
import musicDriverInterface.IDriver;
import vavi.util.Debug;


class Program {

    private static final int SamplingRate = 55467; // 44100;
    private static final int samplingBuffer = 1024;
    private static short[] frames = new short[samplingBuffer * 4];
    private static MDSound mds = null;
    private static short[] emuRenderBuf = new short[2];
    private static IDriver drv = null;
    private static int opmMasterClock = 3579545;
    private static final int opnaMasterClock = 7987200;
    private static final int opnbMasterClock = 8000000;
    private static WaveWriter ww = null;
    private static int loop = 2;

    public static int main(String[] args) {

        int fnIndex = analyzeOption(args);

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
            ww.open(Path.combine(
                    Path.getDirectoryName(args[fnIndex]),
                    Path.getFileNameWithoutExtension(args[fnIndex]) + ".wav")
            );

            List<MmlDatum> temp = new ArrayList<>();
            byte[] srcBuf = File.readAllBytes(args[fnIndex]);
            for (byte b : srcBuf) temp.add(new MmlDatum(b & 0xff));
            MmlDatum[] buf = temp.toArray(MmlDatum[]::new);

            MubHeader mh = new MubHeader(buf);
            mh.getTags();
            if (mh.opmClockMode == MubHeader.enmOPMClockMode.X68000) opmMasterClock = Driver.cOPMMasterClock_X68k;

            List<MDSound.Chip> chips = new ArrayList<>();
            MDSound.Chip chip;

            Ym2608Inst ym2608 = new Ym2608Inst();
            for (int i = 0; i < 2; i++) {
                chip = new MDSound.Chip();
                chip.id = (byte) i;
                chip.instrument = ym2608;
                chip.samplingRate = SamplingRate;
                chip.clock = opnaMasterClock;
                chip.volume = 0;
                chip.option = new Object[] {GetApplicationFolder()};
                chips.add(chip);
            }
            Ym2610Inst ym2610 = new Ym2610Inst();
            for (int i = 0; i < 2; i++) {
                chip = new MDSound.Chip();
                chip.id = (byte) i;
                chip.instrument = ym2610;
                chip.samplingRate = SamplingRate;
                chip.clock = opnbMasterClock;
                chip.volume = 0;
                chip.option = new Object[] {GetApplicationFolder()};
                chips.add(chip);
            }
            Ym2151Inst ym2151 = new Ym2151Inst();
            for (int i = 0; i < 1; i++) {
                chip = new MDSound.Chip();
                chip.id = (byte) i;
                chip.instrument = ym2151;
                chip.samplingRate = SamplingRate;
                chip.clock = opmMasterClock;
                chip.volume = 0;
                chip.option = new Object[] {GetApplicationFolder()};
                chips.add(chip);
            }
            mds = new MDSound(SamplingRate, samplingBuffer, chips.toArray(MDSound.Chip[]::new));

            List<ChipAction> actions = new ArrayList<>();
            MucomChipAction action;
            action = new MucomChipAction(Program::writeOPNAP, null, Program::OPNAWaitSend);
            actions.add(action);
            action = new MucomChipAction(Program::writeOPNAS, null, null);
            actions.add(action);
            action = new MucomChipAction(Program::writeOPNBP, Program::writeOPNBAdpcmP, null);
            actions.add(action);
            action = new MucomChipAction(Program::writeOPNBS, Program::writeOPNBAdpcmS, null);
            actions.add(action);
            action = new MucomChipAction(Program::writeOPMP, null, null);
            actions.add(action);

            drv = new Driver();
            drv.init(actions, buf, null, false, true, false, args[fnIndex]);

            drv.setLoopCount(loop);

            List<Tuple<String, String>> tags = drv.getTags();
            if (tags != null) {
                for (Tuple<String, String> tag : tags) {
                    if (tag.getItem1().isEmpty()) continue;
                    Debug.printf(Level.INFO, String.format("%-16s : %s", tag.getItem1(), tag.getItem2()));
                }
            }

            drv.startRendering(SamplingRate,
                    new Tuple<>("YM2608", opnaMasterClock),
                    new Tuple<>("YM2608", opnaMasterClock),
                    new Tuple<>("YM2610B", opnbMasterClock),
                    new Tuple<>("YM2610B", opnbMasterClock),
                    new Tuple<>("YM2151", opmMasterClock)
            );

            drv.startMusic(0);

            while (true) {

                EmuCallback(frames, 0, samplingBuffer);
                //ステータスが0(終了)又は0未満(エラー)の場合はループを抜けて終了
                if (drv.getStatus() <= 0) {
                    break;
                }

                //Debug.printf(Level.FINEST, String.format("%d  %d",frames[0],frames[1]));
                ww.write(frames, 0, samplingBuffer);
            }

            drv.stopMusic();
            drv.stopRendering();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ww != null) {
                ww.close();
            }
        }

        return 0;
    }

    private static int analyzeOption(String[] args) {
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
                    Debug.println(Level.WARNING, e);
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

    private static void writeOPNA(ChipDatum dat) {
        if (dat != null && dat.addtionalData != null) {
            MmlDatum md = (MmlDatum) dat.addtionalData;
            if (md.linePos != null) {
                Debug.printf(Level.FINEST, String.format("! r%d c%d", md.linePos.row, md.linePos.col));
            }
        }
        if (dat.address == -1) return;
        //Debug.printf(Level.FINEST, String.format("FM P%d Out:adr[%02x] val[%02x]", (int)dat.address, (int)dat.data,dat.port));
        mds.writeYm2608((byte) 0, (byte) dat.port, (byte) dat.address, (byte) dat.data);
    }

    private static void OPNAWaitSend(long elapsed, int size) {
        return;
    }

    private static int EmuCallback(short[] buffer, int offset, int count) {
        try {
            long bufCnt = count / 2;

            for (int i = 0; i < bufCnt; i++) {
                mds.update(emuRenderBuf, 0, 2, drv::render);

                buffer[offset + i * 2 + 0] = emuRenderBuf[0];
                buffer[offset + i * 2 + 1] = emuRenderBuf[1];

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return count;
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
        if (dat != null && dat.addtionalData != null) {
            MmlDatum md = (MmlDatum) dat.addtionalData;
            if (md.linePos != null) {
                Debug.printf(Level.FINEST, String.format("! OPNA i%d r%d c%d", chipId, md.linePos.row, md.linePos.col));
            }
        }

        if (dat.address == -1) return;
        Debug.printf(Level.FINEST, String.format("Out ChipA:%d Port:%d adr:[%02x] val[%02x]", chipId, dat.port, (int) dat.address, (int) dat.data));

        mds.writeYm2608((byte) chipId, (byte) dat.port, (byte) dat.address, (byte) dat.data);
    }

    private static void writeOPNB(int chipId, ChipDatum dat) {
        if (dat != null && dat.addtionalData != null) {
            MmlDatum md = (MmlDatum) dat.addtionalData;
            if (md.linePos != null) {
                Debug.printf(Level.FINEST, String.format("! OPNB i%d r%d c%d", chipId, md.linePos.row, md.linePos.col));
            }
        }

        if (dat.address == -1) return;
        Debug.printf(Level.FINEST, String.format("Out ChipB:%d Port:%d adr:[%02x] val[%02x]", chipId, dat.port, dat.address, dat.data));

        mds.writeYm2610((byte) chipId, (byte) dat.port, (byte) dat.address, (byte) dat.data);
    }

    private static void writeOPNBAdpcmA(int chipId, byte[] pcmData) {
        mds.writeYm2610SetAdpcmA((byte) chipId, pcmData);

    }

    private static void writeOPNBAdpcmB(int chipId, byte[] pcmData) {
        mds.writeYm2610SetAdpcmB((byte) chipId, pcmData);

    }

    private static void writeOPM(int chipId, ChipDatum dat) {
        if (dat != null && dat.addtionalData != null) {
            MmlDatum md = (MmlDatum) dat.addtionalData;
            if (md.linePos != null) {
                Debug.printf(Level.FINEST, String.format("! OPM i%d r%d c%d", chipId, md.linePos.row, md.linePos.col));
            }
        }

        if (dat.address == -1) return;
        Debug.printf(Level.FINEST, String.format("Out OPMChip:%d Port:%d adr:[%02x] val[%02x]", chipId, dat.port, dat.address, dat.data));

        mds.writeYm2151((byte) chipId, (byte) dat.address, (byte) dat.data);
    }
}

