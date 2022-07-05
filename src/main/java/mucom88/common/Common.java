package mucom88.common;

import java.awt.Point;

import musicDriverInterface.MmlDatum;


public class Common {

    public static final Point EmptyPoint = new Point(0, 0);

    public static int getBE16(byte[] buf, int adr) {
        if (buf == null || buf.length - 1 < adr + 1) {
            throw new IndexOutOfBoundsException();
        }

        int dat;
        dat = (int) buf[adr] * 0x100 + (int) buf[adr + 1];

        return dat;
    }

    public static int getLE16(byte[] buf, int adr) {
        if (buf == null || buf.length - 1 < adr + 1) {
            throw new IndexOutOfBoundsException();
        }

        int dat;
        dat = (int) buf[adr] + (int) buf[adr + 1] * 0x100;

        return dat;
    }

    public static int getLE16(MmlDatum[] buf, int adr) {
        if (buf == null || buf.length - 1 < adr + 1) {
            throw new IndexOutOfBoundsException();
        }

        int dat;
        dat = (int) buf[adr].dat + (int) buf[adr + 1].dat * 0x100;

        return dat;
    }

    public static int getLE24(byte[] buf, int adr) {
        if (buf == null || buf.length - 1 < adr + 2) {
            throw new IndexOutOfBoundsException();
        }

        int dat;
        dat = (int) buf[adr] + (int) buf[adr + 1] * 0x100 + (int) buf[adr + 2] * 0x10000;

        return dat;
    }

    public static int getLE32(byte[] buf, int adr) {
        if (buf == null || buf.length - 1 < adr + 3) {
            throw new IndexOutOfBoundsException();
        }

        int dat;
        dat = (int) buf[adr] + (int) buf[adr + 1] * 0x100 + (int) buf[adr + 2] * 0x10000 + (int) buf[adr + 3] * 0x100_0000;

        return dat;
    }

    public static int getLE32(MmlDatum[] buf, int adr) {
        if (buf == null || buf.length - 1 < adr + 3) {
            throw new IndexOutOfBoundsException();
        }

        int dat;
        dat = (int) buf[adr].dat + (int) buf[adr + 1].dat * 0x100 + (int) buf[adr + 2].dat * 0x10000 + (int) buf[adr + 3].dat * 0x100_0000;

        return dat;
    }

    public static String GetChipName(int ChipIndex) {
        switch (ChipIndex) {
        case 0:
        case 1:
            return "YM2608";
        case 2:
        case 3:
            return "YM2610B";
        case 4:
            return "YM2151";
        default:
            return "Unknown";
        }
    }

    public static int GetChipNumber(int ChipIndex) {
        switch (ChipIndex) {
        case 0:
        case 2:
            return 0;
        case 1:
        case 3:
            return 1;
        case 4:
            return 0;
        default:
            return -1;
        }
    }
}

