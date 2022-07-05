package mucom88.common;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


public class MyEncoding implements iEncoding {
    private static MyEncoding defaultEncoding;
    private Charset sjis;

    static {
        defaultEncoding = new MyEncoding();
    }

    public MyEncoding() {
        try {
            sjis = Charset.forName("shift_jis");
        } catch (Exception e) {
            sjis = StandardCharsets.UTF_8;
        }
    }

    public static iEncoding Default() {
        return defaultEncoding;
    }

    public byte[] GetSjisArrayFromString(String utfString) {
        return utfString.getBytes(sjis);
    }

    ;

    public String getStringFromSjisArray(byte[] sjisArray) {
        return new String(sjisArray, sjis);
    }

    public String getStringFromSjisArray(byte[] sjisArray, int index, int count) {
        return new String(sjisArray, index, count, sjis);
    }

    public String GetStringFromUtfArray(byte[] utfArray) {
        return new String(utfArray, StandardCharsets.UTF_8);
    }

    public byte[] GetUtfArrayFromString(String utfString) {
        return utfString.getBytes(StandardCharsets.UTF_8);
    }
}
