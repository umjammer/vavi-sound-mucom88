package mucom88.console;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ResourceBundle;

import mucom88.common.MubException;
import mucom88.common.MucException;
import mucom88.compiler.Compiler;
import musicDriverInterface.MmlDatum;
import vavi.util.serdes.Serdes;

import static vavi.util.compat.Util.changeExtension;


public class Program {

    private static final Logger logger = System.getLogger(Program.class.getName());

    private static final ResourceBundle rb = ResourceBundle.getBundle("mucom88/message");

    private String srcFile;
    private boolean isXml = false;
    private static boolean isTest = false;

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        Program app = new Program();
        int fnIndex = app.analyzeOption(args);

        if (args.length < 1 + fnIndex) {
            logger.log(Level.INFO, rb.getString("I0600"));
            return;
        }

        try {

            app.compile(args[fnIndex], (args.length > fnIndex + 1 ? args[fnIndex + 1] : null));

        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage());
            logger.log(Level.ERROR, Arrays.toString(ex.getStackTrace()));
            if (isTest) throw ex;
        }
    }

    public static String getCompledFilename(Path p) {
        return p.getFileName().toString().replaceFirst("\\.muc$", ".mub");
    }

    private void compile(String srcFile, String destFile /* = null */) {
        try {
            Path path = Path.of(srcFile);
            if (path.getFileName().toString().lastIndexOf('.') == -1)
                path = Path.of(srcFile + ".muc");

            this.srcFile = path.toAbsolutePath().toString();

            Compiler compiler = new Compiler();
            compiler.init();

            //compiler.setCompileSwitch("IDE");
            //compiler.setCompileSwitch("SkipPoint=R19:C30");

            if (!isXml) {
                String destFileName;
                if (destFile != null) {
                    destFileName = destFile;
                } else {
                    destFileName = path.getParent().resolve(getCompledFilename(path)).toString();
                }
logger.log(Level.DEBUG, srcFile + " -> " + destFileName);
                if (!Files.exists(path)) {
                    logger.log(Level.ERROR, String.format(rb.getString("E0601"), srcFile));
                    return;
                }

                boolean isSuccess;
                try (InputStream sourceMML = Files.newInputStream(Path.of(srcFile));
                     ByteArrayOutputStream destCompiledBin = new ByteArrayOutputStream()) {
                    var data = compiler.compile(sourceMML, this::appendFileReaderCallback);
                    if (data == null) {
                        isSuccess = false;
                    } else {
                        for (MmlDatum datum : data) {
                            if (datum == null) {
                                destCompiledBin.write((byte) 0);
                            } else {
                                destCompiledBin.write((byte) (datum.dat & 0xff));
                            }
                        }
                        isSuccess = true;
                    }

                    if (isSuccess) {
                        destCompiledBin.flush();
                        byte[] destbuf = destCompiledBin.toByteArray();
                        Files.write(Path.of(destFileName), destbuf);
                    }
                }
            } else {
                String destFileName = changeExtension(srcFile, ".xml");
                if (destFile != null) {
                    destFileName = destFile;
                }
                MmlDatum[] dest;

                try (InputStream sourceMML = Files.newInputStream(Path.of(srcFile))) {
                    dest = compiler.compile(sourceMML, this::appendFileReaderCallback);
                }
if (dest.length == 0) {
 logger.log(Level.WARNING, "no data");
}
                try (OutputStream sw = Files.newOutputStream(Path.of(destFileName))) {
                    for (var d : dest)
                        Serdes.Util.serialize(sw, d);
                }
            }
        } catch (MubException | MucException ex) {
            System.err.println(ex.getMessage());
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage());
            logger.log(Level.ERROR, Arrays.toString(ex.getStackTrace()));
        }
    }

    private InputStream appendFileReaderCallback(String arg) {

        Path fn = Path.of(srcFile).getParent().resolve(arg);

        if (!Files.exists(fn)) {
logger.log(Level.INFO, "file not found: " + fn);
            return null;
        }

        InputStream strm;
        try {
            strm = Files.newInputStream(fn);
        } catch (java.io.IOException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            strm = null;
        }

        return strm;
    }

    private int analyzeOption(String[] args) {
        if (args.length < 1) return 0;

        int i = 0;
        while (i < args.length && !args[i].isEmpty() && args[i].charAt(0) == '-') {
            String op = args[i].substring(1).toUpperCase();

            if (op.equals("XML")) {
                isXml = true;
                break;
            }

            i++;
        }

        return i;
    }
}
