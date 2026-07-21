package mucom88.wav;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
import musicDriverInterface.IDriver;
import musicDriverInterface.MmlDatum;
import vavi.util.Debug;
import vavi.util.compat.Tuple;

import static vavi.util.compat.Util.changeExtension;
import static vavi.util.compat.Util.isNullOrEmpty;


public class Program {

    private static final Logger logger = System.getLogger(Program.class.getName());

    private static final int SamplingRate = 55467; // 44100;
    private static final int samplingBuffer = 1024;
    private final short[] frames = new short[samplingBuffer * 4];
    private MDSound mds = null;
    private final short[] emuRenderBuf = new short[2];
    private IDriver drv = null;
    private static int opmMasterClock = 3579545;
    private static final int opnaMasterClock = 7987200;
    private static final int opnbMasterClock = 8000000;
    private WaveWriter ww = null;
    private int loop = 2;

    static void main(String[] args) {
        Program app = new Program();
        int fnIndex = app.analyzeOption(args);

        if (args == null || args.length != fnIndex + 1) {
            throw new IllegalArgumentException("at least one argument is needed(.mub file)");
        }
        if (!Files.exists(Path.of(args[fnIndex]))) {
            throw new IllegalArgumentException("file not found");
        }

        try {

            app.run(args[fnIndex]);

        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    private void run(String fn) throws IOException {
        try {
            ww = new WaveWriter(SamplingRate);
            ww.open(changeExtension(fn, ".wav"));

            List<MmlDatum> temp = new ArrayList<>();
            byte[] srcBuf = Files.readAllBytes(Path.of(fn));
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
                chip.option = new Object[] {getApplicationFolder()};
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
                chip.option = new Object[] {getApplicationFolder()};
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
                chip.option = new Object[] {getApplicationFolder()};
                chips.add(chip);
            }
            mds = new MDSound();

            List<ChipAction> actions = new ArrayList<>();
            MucomChipAction action;
            action = new MucomChipAction(this::writeOPNAP, null, Program::OPNAWaitSend);
            actions.add(action);
            action = new MucomChipAction(this::writeOPNAS, null, null);
            actions.add(action);
            action = new MucomChipAction(this::writeOPNBP, this::writeOPNBAdpcmP, null);
            actions.add(action);
            action = new MucomChipAction(this::writeOPNBS, this::writeOPNBAdpcmS, null);
            actions.add(action);
            action = new MucomChipAction(this::writeOPMP, null, null);
            actions.add(action);

            drv = new Driver();
            drv.init(actions, buf, null, false, true, false, fn);

            drv.setLoopCount(loop);

            List<Tuple<String, String>> tags = drv.getTags();
            if (tags != null) {
                for (Tuple<String, String> tag : tags) {
                    if (tag.getItem1().isEmpty()) continue;
                    logger.log(Level.INFO, "%-16s : %s".formatted(tag.getItem1(), tag.getItem2()));
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

//logger.log(Level.TRACE, String.format("%d  %d",frames[0],frames[1]));
                ww.write(frames, 0, samplingBuffer);
            }

            drv.stopMusic();
            drv.stopRendering();

        } finally {
            if (ww != null) {
                ww.close();
            }
        }
    }

    private int analyzeOption(String[] args) {
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
                    logger.log(Level.WARNING, e);
                    loop = 2;
                }
            }

            i++;
        }

        return i;
    }

    public static String getApplicationFolder() {
        String path = System.getProperty("user.dir");
        if (!isNullOrEmpty(path)) {
            path += path.charAt(path.length() - 1) == '\\' ? "" : "\\";
        }
        return path;
    }

    private void writeOPNA(ChipDatum dat) {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
                logger.log(Level.TRACE, "! r%d c%d", md.linePos.row, md.linePos.col);
            }
        }
        if (dat.address == -1) return;
        //logger.log(Level.TRACE, "FM P%d Out:adr[%02x] val[%02x]", (int)dat.address, (int)dat.data,dat.port);
        mds.write(Ym2608Inst.class, (byte) 0, dat.port, dat.address, dat.data);
    }

    private static void OPNAWaitSend(long elapsed, int size) {
    }

    private int EmuCallback(short[] buffer, int offset, int count) {
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

    private void writeOPNAP(ChipDatum dat) {
        writeOPNA(0, dat);
    }

    private void writeOPNAS(ChipDatum dat) {
        writeOPNA(1, dat);
    }

    private void writeOPNBP(ChipDatum dat) {
        writeOPNB(0, dat);
    }

    private void writeOPNBS(ChipDatum dat) {
        writeOPNB(1, dat);
    }

    private void writeOPMP(ChipDatum dat) {
        writeOPM(0, dat);
    }

    private void writeOPNBAdpcmP(byte[] pcmData, int s, int e) {
        if (s == 0) writeOPNBAdpcmA(0, pcmData);
        else writeOPNBAdpcmB(0, pcmData);
    }

    private void writeOPNBAdpcmS(byte[] pcmData, int s, int e) {
        if (s == 0) writeOPNBAdpcmA(1, pcmData);
        else writeOPNBAdpcmB(1, pcmData);
    }

    private void writeOPNA(int chipId, ChipDatum dat) {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
                logger.log(Level.TRACE, "! OPNA i%d r%d c%d", chipId, md.linePos.row, md.linePos.col);
            }
        }

        if (dat.address == -1) return;
        logger.log(Level.TRACE, "Out ChipA:%d Port:%d adr:[%02x] val[%02x]", chipId, dat.port, dat.address, dat.data);

        mds.write(Ym2608Inst.class, chipId, dat.port, dat.address, dat.data);
    }

    private void writeOPNB(int chipId, ChipDatum dat) {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
                logger.log(Level.TRACE, "! OPNB i%d r%d c%d".formatted(chipId, md.linePos.row, md.linePos.col));
            }
        }

        if (dat.address == -1) return;
        logger.log(Level.TRACE, "Out ChipB:%d Port:%d adr:[%02x] val[%02x]".formatted(chipId, dat.port, dat.address, dat.data));

        mds.write(Ym2610Inst.class, chipId, dat.port, dat.address, dat.data);
    }

    private void writeOPNBAdpcmA(int chipId, byte[] pcmData) {
        mds.inst(Ym2610Inst.class).writeAdpcmA(chipId, pcmData);
    }

    private void writeOPNBAdpcmB(int chipId, byte[] pcmData) {
        mds.inst(Ym2610Inst.class).writeAdpcmB(chipId, pcmData);
    }

    private void writeOPM(int chipId, ChipDatum dat) {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
                logger.log(Level.TRACE, "! OPM i%d r%d c%d".formatted(chipId, md.linePos.row, md.linePos.col));
            }
        }

        if (dat.address == -1) return;
        logger.log(Level.TRACE, "Out OPMChip:%d Port:%d adr:[%02x] val[%02x]".formatted(chipId, dat.port, dat.address, dat.data));

        mds.write(Ym2151Inst.class, chipId, 0, dat.address, dat.data);
    }
}

