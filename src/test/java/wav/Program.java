package wav;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import dotnet4j.io.File;
import dotnet4j.io.Path;
import dotnet4j.util.compat.StringUtilities;
import dotnet4j.util.compat.Tuple;
import mdsound.Instrument;
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
    private static final short[] frames = new short[samplingBuffer * 4];
    private static MDSound mds = null;
    private static final short[] emuRenderBuf = new short[2];
    private static IDriver drv = null;
    private static int opmMasterClock = 3579545;
    private static final int opnaMasterClock = 7987200;
    private static final int opnbMasterClock = 8000000;
    private static WaveWriter ww = null;
    private static int loop = 2;

    static void main(String[] args) {

        int fnIndex = analyzeOption(args);

        if (args == null || args.length != fnIndex + 1) {
            throw new IllegalArgumentException("at least one argument is needed(.mub file)");
        }
        if (!File.exists(args[fnIndex])) {
            throw new IllegalArgumentException("file not found");
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

            Ym2608Inst ym2608 = Instrument.getInstrument(Ym2608Inst.class);
            for (int i = 0; i < 2; i++) {
                chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = ym2608;
                chip.samplingRate = SamplingRate;
                chip.clock = opnaMasterClock;
                chip.volume = 0;
                chip.setVolumes.put("FM", ym2608::setVolume);
                chip.setVolumes.put("SSG", ym2608::setVolume);
                chip.setVolumes.put("RHYTHM", ym2608::setVolume);
                chip.setVolumes.put("ADPCM", ym2608::setVolume);
                chip.option = new Object[] {GetApplicationFolder()};
                chips.add(chip);
            }
            Ym2610Inst ym2610 = Instrument.getInstrument(Ym2610Inst.class);
            for (int i = 0; i < 2; i++) {
                chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = ym2610;
                chip.samplingRate = SamplingRate;
                chip.clock = opnbMasterClock;
                chip.volume = 0;
                chip.setVolumes.put("FM", ym2610::setVolume);
                chip.setVolumes.put("SSG", ym2610::setVolume);
                chip.setVolumes.put("ADPCMA", ym2610::setVolume);
                chip.setVolumes.put("ADPCMB", ym2610::setVolume);
                chip.option = new Object[] {GetApplicationFolder()};
                chips.add(chip);
            }
            Ym2151Inst ym2151 = Instrument.getInstrument(Ym2151Inst.class);
            for (int i = 0; i < 1; i++) {
                chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = ym2151;
                chip.samplingRate = SamplingRate;
                chip.clock = opmMasterClock;
                chip.volume = 0;
                chip.option = new Object[] {GetApplicationFolder()};
                chips.add(chip);
            }
            mds = new MDSound();

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
                    Debug.printf(Level.INFO, "%-16s : %s", tag.getItem1(), tag.getItem2());
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
                // If the status is 0 (finished) or less than 0 (error), exit the loop
                if (drv.getStatus() <= 0) {
                    break;
                }

//Debug.printf(Level.FINEST, String.format("%d  %d",frames[0],frames[1]));
                ww.write(frames, 0, samplingBuffer);
            }

            drv.stopMusic();
            drv.stopRendering();
        } catch (Exception e) {
            Debug.printStackTrace(e);
        } finally {
            if (ww != null) {
                ww.close();
            }
        }
    }

    private static int analyzeOption(String[] args) {
        int i = 0;
        loop = 2;

        while (args != null
                && args.length > 0
                && !args[i].isEmpty()
                && args[i] != null
                && args[i].charAt(0) == '-') {
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

    public static String GetApplicationFolder() {
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
                Debug.printf(Level.FINEST, "! r%d c%d", md.linePos.row, md.linePos.col);
            }
        }
        if (dat.address == -1) return;
        //Debug.printf(Level.FINEST, "FM P%d Out:adr[%02x] val[%02x]", (int)dat.address, (int)dat.data,dat.port);
        mds.write(Ym2608Inst.class, (byte) 0, dat.port, dat.address, dat.data);
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
            Debug.printStackTrace(ex);
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
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
                Debug.printf(Level.FINEST, "! OPNA i%d r%d c%d", chipId, md.linePos.row, md.linePos.col);
            }
        }

        if (dat.address == -1) return;
        Debug.printf(Level.FINEST, "Out ChipA:%d Port:%d adr:[%02x] val[%02x]", chipId, dat.port, dat.address, dat.data);

        mds.write(Ym2608Inst.class, chipId, dat.port, dat.address, dat.data);
    }

    private static void writeOPNB(int chipId, ChipDatum dat) {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
                Debug.printf(Level.FINEST, "! OPNB i%d r%d c%d", chipId, md.linePos.row, md.linePos.col);
            }
        }

        if (dat.address == -1) return;
        Debug.printf(Level.FINEST, "Out ChipB:%d Port:%d adr:[%02x] val[%02x]", chipId, dat.port, dat.address, dat.data);

        mds.write(Ym2610Inst.class, chipId, dat.port, dat.address, dat.data);
    }

    private static void writeOPNBAdpcmA(int chipId, byte[] pcmData) {
        mds.inst(Ym2610Inst.class).writeAdpcmA(chipId, pcmData);

    }

    private static void writeOPNBAdpcmB(int chipId, byte[] pcmData) {
        mds.inst(Ym2610Inst.class).writeAdpcmB(chipId, pcmData);

    }

    private static void writeOPM(int chipId, ChipDatum dat) {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
                Debug.printf(Level.FINEST, "! OPM i%d r%d c%d", chipId, md.linePos.row, md.linePos.col);
            }
        }

        if (dat.address == -1) return;
        Debug.printf(Level.FINEST, "Out OPMChip:%d Port:%d adr:[%02x] val[%02x]", chipId, dat.port, dat.address, dat.data);

        mds.write(Ym2151Inst.class, chipId, 0, dat.address, dat.data);
    }
}

