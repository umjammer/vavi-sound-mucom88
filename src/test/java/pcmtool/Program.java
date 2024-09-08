package pcmtool;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;

import dotnet4j.io.File;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.FileStream;
import dotnet4j.io.IOException;
import dotnet4j.io.Path;
import dotnet4j.io.Stream;
import dotnet4j.util.compat.StringUtilities;
import mucom88.compiler.pcmTool.AdpcmMaker;
import vavi.util.Debug;


class Program {
    private static String srcFile;

    public static void main(String[] args) {
        int fnIndex = analyzeOption(args);

        if (args.length != fnIndex + 1) {
            Debug.printf(Level.SEVERE, "引数(.mucファイル)１個欲しいよぉ");
            return;
        }
        if (!File.exists(args[fnIndex])) {
            Debug.printf(Level.SEVERE, "ファイルが見つかりません");
            return;
        }

        make(args[fnIndex]);
    }

    private static int analyzeOption(String[] args) {
        int i = 0;
        if (args.length == 0) return i;

        while (args[i] != null && args[i].length() > 0 && args[i].charAt(0) == '-') {
            String op = args[i].substring(1).toUpperCase();

            i++;
        }

        return i;
    }

    private static void make(String fn) {
        try {
            srcFile = fn;

            //sjis crlf
            String[] src = File.readAllText(fn, Charset.forName("MS932")).split("\r\n");

            List<String>[] ret = divider(src);
            byte[][] pcmdata = new byte[6][];
            for (int i = 0; i < 6; i++) {
                pcmdata[i] = null;
                if (ret[i].size() > 0) {
                    pcmdata[i] = getPackedPCM(i, ret[i], Program::appendFileReaderCallback);
                }
            }

            String[] addName = new String[] {
                "_pcm.bin",
                        "_pcm_2nd.bin",
                        "_pcm_3rd_b.bin",
                        "_pcm_4th_b.bin",
                        "_pcm_3rd_a.bin",
                        "_pcm_4th_a.bin",
            } ;
            for (int i = 0; i < 6; i++) {
                if (pcmdata[i] == null) continue;
                String dstFn = Path.combine(Path.getDirectoryName(fn), Path.getFileNameWithoutExtension(fn) + addName[i]);
                File.writeAllBytes(dstFn, pcmdata[i]);
                Debug.printf(Level.INFO, String.format("Write:%s size:%d", dstFn, pcmdata[i].length));
            }
        } catch (Exception ex) {
            Debug.printf(Level.SEVERE, "Fatal error.");
            Debug.printf(Level.SEVERE, " Message:");
            Debug.printf(Level.SEVERE, ex.getMessage());
            Debug.printf(Level.SEVERE, " StackTrace:");
            Debug.printf(Level.SEVERE, Arrays.toString(ex.getStackTrace()));
        }
    }

    private static List<String>[] divider(String[] src) {
        List<String>[] ret = new List[] {
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
        } ;

        for (String lin : src)
        {
            if (StringUtilities.isNullOrEmpty(lin)) continue;
            if (lin.length() < 3) continue;
            if (lin.charAt(0) != '#') continue;
            if (lin.charAt(1) != '@') continue;

            String li = lin.substring(2).toLowerCase();
            //文字列の長いものから比較
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

    private static byte[] getPackedPCM(int i, List<String> list, Function<String, Stream> appendFileReaderCallback) {
        AdpcmMaker adpcmMaker = new AdpcmMaker(i, list, appendFileReaderCallback);
        return adpcmMaker.make();
    }

    private static Stream appendFileReaderCallback(String arg) {

        String fn = Path.combine(Path.getDirectoryName(srcFile), arg);

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
}

