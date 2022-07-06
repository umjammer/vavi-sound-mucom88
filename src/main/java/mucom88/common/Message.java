package mucom88.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;

import dotnet4j.io.File;
import dotnet4j.io.Path;
import vavi.util.Debug;


/**
 * @deprecated use {@link java.util.ResourceBundle}
 */
@Deprecated
public class Message {

    private static Map<String, String> dicMsg = null;
    private static String otherLangFilename = Path.combine("lang", "message%s.txt");
    private static String englishFilename = Path.combine("lang", "message.txt");

    public static void makeMessageDic(String[] lines) {
        makeMessageDic(lines != null ? parseMesseageDicDatas(lines) : null);
    }

    public static void makeMessageDic(Map<String, String> datas) {
        dicMsg = dicMsg != null ? dicMsg : new HashMap<String, String>();
        if (datas == null) return;
        dicMsg.clear();

        for (var data : datas.entrySet()) {
            if (dicMsg.containsKey(data.getKey())) continue;
            dicMsg.put(data.getKey(), data.getValue());
        }
    }

    public static String get(String code) {
        if (dicMsg == null) loadDefaultMessage();

        return dicMsg.getOrDefault(code, String.format("<no message>(%s)", code));
    }

    /**
     * デフォルトで読み込むメッセージファイル名を変更する
     * @param engFilename ex)lang\message.txt
     * @param otherFilename ex)lang\message%s.txt
     */
    public static void changeFilename(String engFilename, String otherFilename) {
        otherLangFilename = otherFilename;
        englishFilename = engFilename;
    }


    private static Map<String, String> parseMesseageDicDatas(String[] lines) {
        Map<String, String> result = new HashMap<>();
        for (String line : lines) {
            String code;
            String msg;
            try {
                if (line == null) continue;
                if (line.isEmpty()) continue;
                String str = line.trim();
                if (str.isEmpty()) continue;
                if (str.charAt(0) == ';') continue;
                code = str.substring(0, str.indexOf("=")).trim();
                msg = str.substring(str.indexOf("=") + 1, str.length());
                msg = msg.replace("\\r", "\r").replace("\\n", "\n");
            } catch (Exception e) {
                e.printStackTrace();
                // 握りつぶす
                continue;
            }
            result.put(code, msg);
        }
        return result;
    }

    private static void loadDefaultMessage() {
        String[] lines = null;
        try {
            String path = Path.getDirectoryName(System.getProperty("user.location"));
            String lang = Locale.getDefault().getLanguage();
            String file = Path.combine(path, String.format(otherLangFilename, lang));
            file = file.replace('\\', Path.DirectorySeparatorChar).replace('/', Path.DirectorySeparatorChar);
            if (!File.exists(file)) {
                file = Path.combine(path, englishFilename);
                file = file.replace('\\', Path.DirectorySeparatorChar).replace('/', Path.DirectorySeparatorChar);
            }
            List<String> ll = new ArrayList<>();
            Scanner s = new Scanner(file);
            while(s.hasNextLine()) {
                ll.add(s.nextLine());
            }
            lines = ll.toArray(String[]::new);
        } catch (Exception e) {
            e.printStackTrace();
            // 握りつぶす
        }

        makeMessageDic(lines);
    }
}

