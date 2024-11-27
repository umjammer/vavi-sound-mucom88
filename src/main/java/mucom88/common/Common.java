package mucom88.common;

import java.awt.Point;
import java.nio.charset.Charset;

import musicDriverInterface.MmlDatum;


public class Common {

    public static final Point EmptyPoint = new Point(0, 0);

    public static int getLE16(MmlDatum[] buf, int adr) {
        int dat = (buf[adr].dat & 0xff) + (buf[adr + 1].dat & 0xff) * 0x100;

        return dat;
    }

    public static int getLE32(MmlDatum[] buf, int adr) {
        int dat = (buf[adr].dat & 0xff) + (buf[adr + 1].dat & 0xff) * 0x100 +
                (buf[adr + 2].dat & 0xff) * 0x10000 + (buf[adr + 3].dat & 0xff) * 0x100_0000;

        return dat;
    }

    public static String getChipName(int ChipIndex) {
        return switch (ChipIndex) {
            case 0, 1 -> "YM2608";
            case 2, 3 -> "YM2610B";
            case 4 -> "YM2151";
            default -> "Unknown";
        };
    }

    public static int getChipNumber(int ChipIndex) {
        return switch (ChipIndex) {
            case 0, 2 -> 0;
            case 1, 3 -> 1;
            case 4 -> 0;
            default -> -1;
        };
    }

    public static Charset fileEncoding = Charset.forName(System.getProperty("mucom88.encoding", "ms932"));
}

