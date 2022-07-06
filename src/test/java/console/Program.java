package console;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.logging.Level;

import dotnet4j.io.BufferedStream;
import dotnet4j.io.File;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.FileStream;
import dotnet4j.io.IOException;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import mdsound.Log;
import mdsound.LogLevel;
import mucom88.common.MubException;
import mucom88.common.MucException;
import mucom88.compiler.Compiler;
import musicDriverInterface.ICompiler;
import musicDriverInterface.MmlDatum;
import vavi.util.Debug;
import vavi.util.serdes.Serdes;


class Program {

    private static ResourceBundle rb = ResourceBundle.getBundle("lang/message");

    private static String srcFile;
    private static boolean isXml = false;

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        int fnIndex = analyzeOption(args);

        if (args.length < 1 + fnIndex) {
            Debug.printf(Level.INFO, rb.getString("I0600"));
            return;
        }

        try {

            compile(args[fnIndex], (args.length > fnIndex + 1 ? args[fnIndex + 1] : null));

        } catch (Exception ex) {
            Debug.printf(Level.SEVERE, ex.getMessage());
            Debug.printf(Level.SEVERE, Arrays.toString(ex.getStackTrace()));
        }
    }

    static void writeLine(LogLevel level, String msg) {
        System.err.printf("[%-7s] %s", level, msg);
    }

    static void writeLine(String msg) {
        System.err.printf(msg);
    }

    static String getCompledFilename(Path p) {
        return p.getFileName().toString().replaceFirst("\\.muc$", ".mub");
    }

    static void compile(String srcFile, String destFile/* = null*/) {
        try {
            Path path = Path.of(srcFile);
            if (path.getFileName().toString().lastIndexOf('.') == -1)
                path = Path.of(srcFile + ".muc");

            Program.srcFile = path.toAbsolutePath().toString();

            Compiler compiler = new Compiler();
            compiler.init();

            //compiler.SetCompileSwitch("IDE");
            //compiler.SetCompileSwitch("SkipPoint=R19:C30");

            if (!isXml) {
                String destFileName;
                if (destFile != null) {
                    destFileName = destFile;
                } else {
                    destFileName = path.getParent().resolve(getCompledFilename(path)).toString();
                }
Debug.println(Level.FINE, srcFile + " -> " + destFileName);
                if (!Files.exists(path)) {
                    Debug.printf(Level.SEVERE, String.format(rb.getString("E0601"), srcFile));
                    return;
                }

                boolean isSuccess = false;
                try (FileStream sourceMML = new FileStream(srcFile, FileMode.Open, FileAccess.Read, FileShare.Read);
                     MemoryStream destCompiledBin = new MemoryStream();
                     Stream bufferedDestStream = new BufferedStream(destCompiledBin)) {
                    isSuccess = compiler.compile(sourceMML, bufferedDestStream, Program::appendFileReaderCallback);

                    if (isSuccess) {
                        bufferedDestStream.flush();
                        byte[] destbuf = destCompiledBin.toArray();
                        File.writeAllBytes(destFileName, destbuf);
                    }
                }
            } else {
                String destFileName = dotnet4j.io.Path.combine(
                        dotnet4j.io.Path.getDirectoryName(dotnet4j.io.Path.getFullPath(srcFile)),
                        String.format("%s.xml", dotnet4j.io.Path.getFileNameWithoutExtension(srcFile)));
                if (destFile != null) {
                    destFileName = destFile;
                }
                MmlDatum[] dest = null;

                try (FileStream sourceMML = new FileStream(srcFile, FileMode.Open, FileAccess.Read, FileShare.Read)) {
                    dest = compiler.compile(sourceMML, Program::appendFileReaderCallback);
                }
if (dest.length == 0) {
 Debug.println(Level.WARNING, "no data");
}
                try (OutputStream sw = Files.newOutputStream(Path.of(destFileName))) {
                    for (var d : dest)
                        Serdes.Util.serialize(sw, d);
                }
            }
        } catch (MubException | MucException ex) {
            System.err.println(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
Debug.println(Level.SEVERE, ex.getMessage());
Debug.println(Level.SEVERE, Arrays.toString(ex.getStackTrace()));
        }
    }

    private static Stream appendFileReaderCallback(String arg) {

        String fn = dotnet4j.io.Path.combine(dotnet4j.io.Path.getDirectoryName(srcFile), arg);

        if (!File.exists(fn)) return null;

        FileStream strm;
        try {
            strm = new FileStream(fn, FileMode.Open, FileAccess.Read, FileShare.Read);
        } catch (IOException e) {
            e.printStackTrace();
            strm = null;
        }

        return strm;
    }

    private static int analyzeOption(String[] args) {
        if (args.length < 1) return 0;

        int i = 0;
        while (i < args.length && args[i].length() > 0 && args[i].charAt(0) == '-') {
            String op = args[i].substring(1).toUpperCase();

            if (op.equals("OFFLOG=WARNING")) {
                Log.off = LogLevel.WARNING.ordinal();
            }

            if (op.equals("XML")) {
                isXml = true;
            }

            i++;
        }

        return i;
    }
}
