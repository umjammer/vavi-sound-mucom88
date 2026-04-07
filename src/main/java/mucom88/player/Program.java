package mucom88.player;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import dotnet4j.io.File;
import dotnet4j.util.compat.StopWatch;
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
import musicDriverInterface.IDriver;
import musicDriverInterface.MmlDatum;
import vavi.util.ByteUtil;

import static java.lang.System.getLogger;
import static vavi.sound.SoundUtil.volume;


public class Program {

    private static final Logger logger = getLogger(Program.class.getName());

    static class KeyboardHook {
        static final AtomicBoolean typed = new AtomicBoolean();
        static {
            try {
                GlobalScreen.registerNativeHook();
            } catch (NativeHookException e) {
                throw new IllegalStateException("There was a problem registering the native hook.", e);
            }
            GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
                @Override
                public void nativeKeyTyped(NativeKeyEvent nativeEvent) {
                    typed.set(true);
                }
            });
        }
        static boolean kbhit() {
            return typed.get();
        }
    }

    private static SourceDataLine audioOutput = null;
    private static Thread threadMain = null;
    private static StopWatch sw = null;
    private static double swFreq = 0;
    public static boolean threadClosed = false;
    private static final Object lockObj = new Object();
    private static boolean _trdStopped = true;
    private static boolean threadStopped;

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

    private static final int SamplingRate = 55467; // 44100;
    private static final int SamplingBuffer = 1024;
    private static final short[] frames = new short[SamplingBuffer * 4];
    private static MDSound mds = null;
    private static final short[] emuRenderBuf = new short[2];
    private static IDriver driver = null;
    private static int opmMasterClock = 3579545;
    private static final int opnaMasterClock = 7987200;
    private static final int opnbMasterClock = 8000000;
    private static int device = 0;
    private static int loop = 0;

    private static boolean loadADPCMOnly = false;
    private static boolean isLoadADPCM = true;

    private static RSoundChip rsc;

    public static void main(String[] args) {

        int fnIndex = analyzeOption(args);

        if (args.length != fnIndex + 1) {
            logger.log(Level.TRACE, "one argument is needed (.mub file)");
            System.exit(-1);
        }
        if (!File.exists(args[fnIndex])) {
            logger.log(Level.TRACE, "file not found");
            System.exit(-1);
        }

        rsc = checkDevice();

        try {

            int latency = 1000;

            switch (device) {
            case 0:
                audioOutput = AudioSystem.getSourceDataLine(new AudioFormat(SamplingRate, 16, 2, true, false));
                audioOutput.open();
                break;
            case 1:
            case 2:
                threadMain = new Thread(Program::realCallback);
                threadMain.setPriority(Thread.MAX_PRIORITY);
                threadMain.setDaemon(true);
                threadMain.setName("trdVgmReal");
                sw = StopWatch.startNew();
                swFreq = StopWatch.Frequency;
                break;
            }

            List<MmlDatum> temp = new ArrayList<>();
logger.log(Level.DEBUG, args[fnIndex]);
            byte[] srcBuf = File.readAllBytes(args[fnIndex]);
            for (byte b : srcBuf) temp.add(new MmlDatum(b & 0xff));
            MmlDatum[] buf = temp.toArray(MmlDatum[]::new);

            MubHeader header = new MubHeader(buf);
            header.getTags();
            if (header.opmClockMode == MubHeader.enmOPMClockMode.X68000) opmMasterClock = Driver.cOPMMasterClock_X68k;

            List<MDSound.Chip> chips = new ArrayList<>();
            MDSound.Chip chip;

            for (int i = 0; i < 2; i++) {
                Ym2608Inst ym2608 = Instrument.getInstrument(Ym2608Inst.class);
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
            for (int i = 0; i < 2; i++) {
                Ym2610Inst ym2610 = Instrument.getInstrument(Ym2610Inst.class);
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
            for (int i = 0; i < 1; i++) {
                chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = Instrument.getInstrument(Ym2151Inst.class);
                chip.samplingRate = SamplingRate;
                chip.clock = opmMasterClock;
                chip.volume = 0;
                chip.option = null;
                chips.add(chip);
            }
            mds = new MDSound(SamplingRate, SamplingBuffer, chips);

            List<ChipAction> actions = new ArrayList<>();
            actions.add(new MucomChipAction(Program::writeOPNAP, null, Program::sendOPNAWait));
            actions.add(new MucomChipAction(Program::writeOPNAS, null, null));
            actions.add(new MucomChipAction(Program::writeOPNBP, Program::writeOPNBAdpcmP, null));
            actions.add(new MucomChipAction(Program::writeOPNBS, Program::writeOPNBAdpcmS, null));
            actions.add(new MucomChipAction(Program::writeOPMP, null, null));

            driver = new Driver();
            driver.init(actions, buf, null, false, isLoadADPCM, loadADPCMOnly, args[fnIndex]);

            if (header.SSGExtend) {
//                mds.ChangeYM2608_PSGMode(0, 1); // new impl @see "TAG106"
//                mds.ChangeYM2608_PSGMode(1, 1);
//                mds.ChangeYM2610_PSGMode(0, 1);
//                mds.ChangeYM2610_PSGMode(1, 1);
            }

            List<Tuple<String, String>> tags = driver.getTags();
            if (tags != null) {
                for (Tuple<String, String> tag : tags) {
                    if (tag.getItem1().isEmpty()) continue;
                    logger.log(Level.DEBUG, "%-16s : %s".formatted(tag.getItem1(), tag.getItem2()));
                }
            }

            if (loadADPCMOnly) return;

            driver.startRendering(SamplingRate,
                    new Tuple<>("YM2608", opnaMasterClock),
                    new Tuple<>("YM2608", opnaMasterClock),
                    new Tuple<>("YM2610B", opnbMasterClock),
                    new Tuple<>("YM2610B", opnbMasterClock),
                    new Tuple<>("YM2151", opmMasterClock));

            switch (device) {
            case 0:
                audioOutput.start();
                volume(audioOutput, 0.2f);
                break;
            case 1:
            case 2:
                threadMain.start();
                break;
            }

            driver.startMusic(0);

            System.out.println("Press any key to exit");

            while (true) {
                Thread.yield();
                if (KeyboardHook.kbhit()) {
                    break;
                }

                emu();

                // If the status is 0 (done) or less than 0 (error), exit the loop.
                if (driver.getStatus() <= 0) {
                    if (driver.getStatus() == 0) {
                        Thread.sleep((int) (latency * 2.0)); // Wait for latency * 2 until the actual voice is fully pronounced
                    }
                    break;
                }
            }

            driver.stopMusic();
            driver.stopRendering();
        } catch (Exception ex) {
            logger.log(Level.ERROR, "Failed to play");
            logger.log(Level.ERROR, "message:%s".formatted(ex.getMessage()));
//logger.log(Level.ERROR, "stackTrace:%s".formatted(Arrays.stream(ex.getStackTrace()).map(Object::toString).collect(Collectors.joining("\n"))));
            logger.log(Level.ERROR, ex.getMessage(), ex);
        } finally {
            if (audioOutput != null) {
                audioOutput.stop();
                while (audioOutput.isRunning()) {
                    audioOutput.drain();
                }
                audioOutput.close();
            }
            if (threadMain != null) {
                threadClosed = true;
                while (!threadStopped) {
                    Thread.yield();
                }
            }
        }
    }

    private static void sendOPNAWait(long elapsed, int size) {
        // nothing to do for emu
    }

    private static RSoundChip checkDevice() {
        // nothing to do for emu
        return null;
    }

    private static int analyzeOption(String[] args) {
        int i = 0;

        device = 0;
        loop = 0;
        isLoadADPCM = true;

        while (i < args.length && args[i] != null && !args[i].isEmpty() && args[i].charAt(0) == '-') {
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

            if (op.length() > 2 && op.startsWith("L=")) {
                try {
                    loop = Integer.parseInt(op.substring(2));
                } catch (NumberFormatException e) {
                    logger.log(Level.WARNING, e);
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

    public static String getApplicationFolder() {
        Path path = Path.of(System.getProperty("mdsound.pcm.path", System.getProperty("user.dir")));
logger.log(Level.DEBUG, "path: [" + path + "]");
        return path.toString();
    }

    private static void emu() {
        try {
            byte[] buffer = new byte[16];

            for (int i = 0; i < buffer.length / 4; i++) {
                int r = mds.update(emuRenderBuf, 0, 2, Program::doOneFrame);

                ByteUtil.writeLeShort(emuRenderBuf[0], buffer, i * 4 + 0);
                ByteUtil.writeLeShort(emuRenderBuf[1], buffer, i * 4 + 2);
//logger.log(Level.TRACE, "%04x, %04x".formatted(emuRenderBuf[0], emuRenderBuf[1]));
            }

            audioOutput.write(buffer, 0, buffer.length);
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
    }

    private static void realCallback() {
        double o = sw.getElapsedMilliseconds() / swFreq;
        double step = 1 / (double) SamplingRate;

        threadStopped = false;
        try {
            while (!threadClosed) {
                Thread.yield();

                double el1 = sw.getElapsedMilliseconds() / swFreq;
                if (el1 - o < step) continue;
                if (el1 - o >= step * SamplingRate / 100.0) { // Threshold 10ms
                    do {
                        o += step;
                    } while (el1 - o >= step);
                } else {
                    o += step;
                }

                doOneFrame();
            }
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
        threadStopped = true;
    }

    private static void doOneFrame() {
        driver.render();
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
logger.log(Level.TRACE, "! OPNA i%d r%d c%d".formatted(chipId, md.linePos.row, md.linePos.col));
            }
        }

        if (dat.address == -1) return;
logger.log(Level.TRACE, "Out ChipA:%d Port:%d adr:[%02x] val[%02x]".formatted(chipId, dat.port, dat.address, dat.data));

        switch (device) {
        case 0:
            mds.write(Ym2608Inst.class, chipId, dat.port, dat.address, dat.data);
            break;
        case 1:
        case 2:
            rsc.setRegister(dat.port * 0x100 + dat.address, dat.data);
            break;
        }
    }

    private static void writeOPNB(int chipId, ChipDatum dat) {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
logger.log(Level.TRACE, "! OPNB i%d r%d c%d".formatted(chipId, md.linePos.row, md.linePos.col));
            }
        }

        if (dat.address == -1) return;
logger.log(Level.TRACE, "Out ChipB:%d Port:%d adr:[%02x] val[%02x]".formatted(chipId, dat.port, dat.address, dat.data));

        switch (device) {
        case 0:
            mds.write(Ym2610Inst.class, chipId, dat.port, dat.address, dat.data);
            break;
        case 1:
        case 2:
            rsc.setRegister(dat.port * 0x100 + dat.address, dat.data);
            break;
        }
    }

    private static void writeOPM(int chipId, ChipDatum dat) {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
logger.log(Level.TRACE, "! OPM i%d r%d c%d".formatted(chipId, md.linePos.row, md.linePos.col));
            }
        }

        if (dat.address == -1) return;

if (dat.address == 0x27) {
 logger.log(Level.TRACE, "Out ChipOPM:%d Port:%d adr:[%02x] val[%02x]".formatted(chipId, dat.port, dat.address, dat.data));
}
        switch (device) {
        case 0:
            mds.write(Ym2151Inst.class, chipId, 0, dat.address, dat.data);
            break;
        case 1:
        case 2:
            rsc.setRegister(dat.address, dat.data);
            break;
        }
    }

    private static void writeOPNBAdpcmA(int chipId, byte[] pcmData) {
        switch (device) {
        case 0:
            mds.inst(Ym2610Inst.class).writeAdpcmA(chipId, pcmData);
            break;
        case 1:
        case 2:
            break;
        }
    }

    private static void writeOPNBAdpcmB(int chipId, byte[] pcmData) {
        switch (device) {
        case 0:
            mds.inst(Ym2610Inst.class).writeAdpcmB(chipId, pcmData);
            break;
        case 1:
        case 2:
            break;
        }
    }
}
