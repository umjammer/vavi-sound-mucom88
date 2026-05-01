package mucom88.pcmtool;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import mucom88.compiler.pcmTool.AdpcmMaker;

import static mucom88.common.Common.charset;
import static vavi.util.compat.Util.changeExtension;
import static vavi.util.compat.Util.isNullOrEmpty;


class Program {

    private static final Logger logger = System.getLogger(Program.class.getName());

    private String srcFile;

    static void main(String[] args) {
        Program app = new Program();
        int fnIndex = app.analyzeOption(args);

        if (args.length != fnIndex + 1) {
            logger.log(Level.ERROR, "at least one argument is needed(.muc file)");
            return;
        }
        if (!Files.exists(Path.of(args[fnIndex]))) {
            logger.log(Level.ERROR, "file not found");
            return;
        }

        try {

            app.make(args[fnIndex]);

        } catch (Exception ex) {
            logger.log(Level.ERROR, "Fatal error.");
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
    }

    private int analyzeOption(String[] args) {
        int i = 0;
        if (args.length == 0) return i;

        while (args[i] != null && !args[i].isEmpty() && args[i].charAt(0) == '-') {
            String op = args[i].substring(1).toUpperCase();

            i++;
        }

        return i;
    }

    private void make(String fn) throws IOException {
        srcFile = fn;

        // sjis crlf
        String[] src = Files.readAllLines(Path.of(fn), charset).toArray(String[]::new);

        List<String>[] ret = divider(src);
        byte[][] pcmdata = new byte[6][];
        for (int i = 0; i < 6; i++) {
            pcmdata[i] = null;
            if (!ret[i].isEmpty()) {
                pcmdata[i] = getPackedPCM(i, ret[i], this::appendFileReaderCallback);
            }
        }

        String[] addName = {
                "_pcm.bin",
                "_pcm_2nd.bin",
                "_pcm_3rd_b.bin",
                "_pcm_4th_b.bin",
                "_pcm_3rd_a.bin",
                "_pcm_4th_a.bin",
        };
        for (int i = 0; i < 6; i++) {
            if (pcmdata[i] == null) continue;
            String dstFn = changeExtension(fn, addName[i]);
            Files.write(Path.of(dstFn), pcmdata[i]);
            logger.log(Level.INFO, "Write:%s size:%d".formatted(dstFn, pcmdata[i].length));
        }
    }

    private static List<String>[] divider(String[] src) {
        List<String>[] ret = new List[] {
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
        };

        for (String lin : src) {
            if (isNullOrEmpty(lin)) continue;
            if (lin.length() < 3) continue;
            if (lin.charAt(0) != '#') continue;
            if (lin.charAt(1) != '@') continue;

            String li = lin.substring(2).toLowerCase();
            // Compare strings starting from the longest
            if (li.indexOf("pcm_3rd_b") == 0) {
                ret[2].add(lin.substring(2 + 9));
            } else if (li.indexOf("pcm_4th_b") == 0) {
                ret[3].add(lin.substring(2 + 9));
            } else if (li.indexOf("pcm_3rd_a") == 0) {
                ret[4].add(lin.substring(2 + 9));
            } else if (li.indexOf("pcm_4th_a") == 0) {
                ret[5].add(lin.substring(2 + 9));
            } else if (li.indexOf("pcm_2nd") == 0) {
                ret[1].add(lin.substring(2 + 7));
            } else if (li.indexOf("pcm") == 0) {
                ret[0].add(lin.substring(2 + 3));
            }
        }

        return ret;
    }

    private static byte[] getPackedPCM(int i, List<String> list, Function<String, InputStream> appendFileReaderCallback) throws IOException {
        AdpcmMaker adpcmMaker = new AdpcmMaker(i, list, appendFileReaderCallback);
        return adpcmMaker.make();
    }

    private InputStream appendFileReaderCallback(String arg) {

        Path fn = Path.of(srcFile).getParent().resolve(arg);

        if (!Files.exists(fn)) return null;

        InputStream strm;
        try {
            strm = Files.newInputStream(fn);
        } catch (java.io.IOException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            strm = null;
        }

        return strm;
    }
}
