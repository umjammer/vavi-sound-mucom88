package mucom88.vgm;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import mucom88.common.MucomChipAction;
import mucom88.driver.Driver;
import mucom88.driver.MubHeader;
import musicDriverInterface.ChipAction;
import musicDriverInterface.ChipDatum;
import musicDriverInterface.IDriver;
import musicDriverInterface.MmlDatum;
import vavi.util.compat.Tuple;

import static vavi.util.compat.Util.changeExtension;
import static vavi.util.compat.Util.isNullOrEmpty;


public class Program {

    private static final Logger logger = System.getLogger(Program.class.getName());

    private static final int SamplingRate = 44100; // vgm format freq
    private static int opmMasterClock = 3579545;
    private static final int opnaMasterClock = 7987200;
    private static final int opnbMasterClock = 8000000;

    private IDriver driver = null;
    private VgmWriter writer = null;
    private int loop = 2;
    private List<Tuple<String, String>> tags = null;

    public static void main(String[] args) {
        Program app = new Program();
        int fnIndex = app.analyzeOption(args);

        if (args == null || args.length != fnIndex + 1) {
            System.err.println("at least one argument is needed (.mub file).");
            System.exit(-1);
        }
        if (!Files.exists(Path.of(args[fnIndex]))) {
            System.err.println("File not found");
            System.exit(-1);
        }

        try {

            app.run(args[fnIndex]);

        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    private void run(String fn) throws IOException {
        try {
            writer = new VgmWriter();
            writer.open(changeExtension(fn, ".vgm"));

            List<ChipAction> actions = new ArrayList<>();
            MucomChipAction action;
            action = new MucomChipAction(this::writeOPNAP, null, this::sendOPNAWait);
            actions.add(action);
            action = new MucomChipAction(this::writeOPNAS, null, null);
            actions.add(action);
            action = new MucomChipAction(this::writeOPNBP, this::writeOPNBAdpcmP, null);
            actions.add(action);
            action = new MucomChipAction(this::writeOPNBS, this::writeOPNBAdpcmS, null);
            actions.add(action);
            action = new MucomChipAction(this::writeOPMP, null, null);
            actions.add(action);

            List<MmlDatum> temp = new ArrayList<>();
            byte[] srcBuf = Files.readAllBytes(Path.of(fn));
            for (byte b : srcBuf) temp.add(new MmlDatum(b & 0xff));
            writer.useChipsFromMub(srcBuf);
            MmlDatum[] buf = temp.toArray(MmlDatum[]::new);

            MubHeader header = new MubHeader(buf);
            header.getTags();
            if (header.opmClockMode == MubHeader.enmOPMClockMode.X68000) opmMasterClock = Driver.cOPMMasterClock_X68k;

            driver = new Driver();
            driver.init(actions, temp.toArray(MmlDatum[]::new), null,
                    false, true, false, fn);

            driver.setLoopCount(loop);

            tags = driver.getTags();
            if (tags != null) {
                for (Tuple<String, String> tag : tags) {
                    if (tag.getItem1().isEmpty()) continue;
logger.log(Level.INFO, "%-16s : %s".formatted(tag.getItem1(), tag.getItem2()));
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

                // Exit loop if status is 0 (end) or less than 0 (error)
                if (driver.getStatus() <= 0) {
                    break;
                }
            }

            driver.stopMusic();
            driver.stopRendering();
        } finally {
            if (writer != null) {
                writer.close(tags, opnaMasterClock, opnbMasterClock, opmMasterClock);
            }
        }
    }

    private int analyzeOption(String[] args) {
        int i = 0;
        loop = 2;

        while (args != null &&
                args.length > 0 &&
                !args[i].isEmpty() &&
                args[i] != null &&
                args[i].charAt(0) == '-') {
            String op = args[i].substring(1).toUpperCase();
            if (op.length() > 2 && op.startsWith("L=")) {
                try {
                    loop = Integer.parseInt(op.substring(2));
                } catch (NumberFormatException e) {
                    logger.log(Level.WARNING, e.toString());
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

    private void writeOPNA(ChipDatum dat) throws IOException {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
                logger.log(Level.TRACE, "! r%d c%d".formatted(md.linePos.row, md.linePos.col));
            }
        }
        if (dat.address == -1) return;

        writer.writeYM2608(0, (byte) dat.port, (byte) dat.address, (byte) dat.data);
    }

    private void sendOPNAWait(long elapsed, int size) {
    }

    private void writeOPNAP(ChipDatum dat) {
        try {
            writeOPNA(0, dat);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeOPNAS(ChipDatum dat) {
        try {
            writeOPNA(1, dat);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeOPNBP(ChipDatum dat) {
        try {
            writeOPNB(0, dat);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeOPNBS(ChipDatum dat) {
        try {
            writeOPNB(1, dat);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeOPMP(ChipDatum dat) {
        try {
            writeOPM(0, dat);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeOPNBAdpcmP(byte[] pcmData, int s, int e) {
        try {
            if (s == 0) writeOPNBAdpcmA(0, pcmData);
            else writeOPNBAdpcmB(0, pcmData);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void writeOPNBAdpcmS(byte[] pcmData, int s, int e) {
        try {
            if (s == 0) writeOPNBAdpcmA(1, pcmData);
            else writeOPNBAdpcmB(1, pcmData);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void writeOPNA(int chipId, ChipDatum dat) throws IOException {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
                logger.log(Level.TRACE, "! OPNA i%d r%d c%d".formatted(chipId, md.linePos.row, md.linePos.col));
            }
        }
        if (dat.address == -1) return;

        logger.log(Level.TRACE, "Out ChipA:%d Port:%d adr:[%02x] val[%02x]".formatted(chipId, dat.port, dat.address, dat.data));

        writer.writeYM2608(chipId, (byte) dat.port, (byte) dat.address, (byte) dat.data);
    }

    private void writeOPNB(int chipId, ChipDatum dat) throws IOException {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
                logger.log(Level.TRACE, "! OPNB i%d r%d c%d".formatted(chipId, md.linePos.row, md.linePos.col));
            }
        }
        if (dat.address == -1) return;

        logger.log(Level.TRACE, "Out ChipB:%d Port:%d adr:[%02x] val[%02x]".formatted(chipId, dat.port, dat.address, dat.data));

        writer.writeYM2610(chipId, (byte) dat.port, (byte) dat.address, (byte) dat.data);
    }

    private void writeOPNBAdpcmA(int chipId, byte[] pcmData) throws IOException {
        writer.writeYM2610SetAdpcmA(chipId, pcmData);
    }

    private void writeOPNBAdpcmB(int chipId, byte[] pcmData) throws IOException {
        writer.writeYM2610SetAdpcmB(chipId, pcmData);
    }

    private void writeOPM(int chipId, ChipDatum dat) throws IOException {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
                logger.log(Level.TRACE, "! OPM i%d r%d c%d".formatted(chipId, md.linePos.row, md.linePos.col));
            }
        }
        if (dat.address == -1) return;

        logger.log(Level.TRACE, "Out OPM Chip:%d Port:%d adr:[%02x] val[%02x]".formatted(chipId, dat.port, dat.address, dat.data));

        writer.writeYM2151(chipId, (byte) dat.address, (byte) dat.data);
    }
}
