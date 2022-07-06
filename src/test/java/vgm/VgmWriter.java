package vgm;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.SeekOrigin;
import dotnet4j.util.compat.Tuple;
import mdsound.Common;
import vavi.util.Debug;


public class VgmWriter {
    private FileStream dest = null;
    private long waitCounter = 0;
    public static final byte[] hDat = new byte[] {
            // 00 'Vgm '          Eof offset           version number
            0x56, 0x67, 0x6d, 0x20, 0x00, 0x00, 0x00, 0x00, 0x71, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 10                 GD3 offset(no use)   Total # samples
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 20                 Rate(NTSC 60Hz)
            0x00, 0x00, 0x00, 0x00, 0x3c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 30                 VGMdataofs(0x100~)
            0x00, 0x00, 0x00, 0x00, (byte) 0xcc, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 40
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // YM2608 clock(7987200 0x0079_e000)
            0x00, (byte) 0xe0, 0x79, 0x40,
            // YM2610 clock(8000000 0x007a_1200)
            0x00, 0x00, 0x00, 0x00,
            // 50
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 60
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 70
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 80
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 90
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // A0
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // B0
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // C0
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // D0
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // E0
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // F0
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    private int[] useChips;

    public long totalSample;

    public void writeYM2608(int v, byte port, byte address, byte data) {
        if (dest == null) return;
        if (useChips[0 + v] == 0) return;

        if (waitCounter != 0) {
            totalSample += waitCounter;

            //waitコマンド出力
            Debug.printf(Level.FINEST
                    , String.format("wait:%d", waitCounter)
            );

            if (waitCounter <= 882 * 3) {
                while (waitCounter > 882) {
                    dest.writeByte((byte) 0x63);
                    waitCounter -= 882;
                }
                while (waitCounter > 735) {
                    dest.writeByte((byte) 0x62);
                    waitCounter -= 735;
                }
            }

            while (waitCounter > 0) {
                dest.writeByte((byte) 0x61);
                dest.writeByte((byte) waitCounter);
                dest.writeByte((byte) (waitCounter >> 8));
                waitCounter -= (waitCounter & 0xffff);
            }

            waitCounter = 0;
        }

        Debug.printf(Level.FINEST
                , String.format("p:%d a:%d d:%d", port, address, data)
        );

        dest.writeByte((byte) ((v == 0 ? 0x56 : 0xa6) + (port & 1)));
        dest.writeByte(address);
        dest.writeByte(data);

    }

    public void writeYM2610(int v, byte port, byte address, byte data) {
        if (dest == null) return;

        if (useChips[2 + v] == 0) return;

        if (waitCounter != 0) {
            totalSample += waitCounter;

            //waitコマンド出力
            Debug.printf(Level.FINEST
                    , String.format("wait:%d", waitCounter)
            );

            if (waitCounter <= 882 * 3) {
                while (waitCounter > 882) {
                    dest.writeByte((byte) 0x63);
                    waitCounter -= 882;
                }
                while (waitCounter > 735) {
                    dest.writeByte((byte) 0x62);
                    waitCounter -= 735;
                }
            }

            while (waitCounter > 0) {
                dest.writeByte((byte) 0x61);
                dest.writeByte((byte) waitCounter);
                dest.writeByte((byte) (waitCounter >> 8));
                waitCounter -= (waitCounter & 0xffff);
            }

            waitCounter = 0;
        }

        Debug.printf(Level.FINEST
                , String.format("p:%d a:%d d:%d", port, address, data)
        );

        dest.writeByte((byte) ((v == 0 ? 0x58 : 0xa8) + (port & 1)));
        dest.writeByte(address);
        dest.writeByte(data);

    }

    public void writeYM2151(int v, byte address, byte data) {
        if (dest == null) return;
        if (useChips[4 + v] == 0) return;

        if (waitCounter != 0) {
            totalSample += waitCounter;

            //waitコマンド出力
            Debug.printf(Level.FINEST
                    , String.format("wait:%d", waitCounter)
            );

            if (waitCounter <= 882 * 3) {
                while (waitCounter > 882) {
                    dest.writeByte((byte) 0x63);
                    waitCounter -= 882;
                }
                while (waitCounter > 735) {
                    dest.writeByte((byte) 0x62);
                    waitCounter -= 735;
                }
            }

            while (waitCounter > 0) {
                dest.writeByte((byte) 0x61);
                dest.writeByte((byte) waitCounter);
                dest.writeByte((byte) (waitCounter >> 8));
                waitCounter -= (waitCounter & 0xffff);
            }

            waitCounter = 0;
        }

        Debug.printf(Level.FINEST, String.format("a:%d d:%d", address, data));

        dest.writeByte((byte) (v == 0 ? 0x54 : 0xa4));
        dest.writeByte(address);
        dest.writeByte(data);

    }

    public void close(List<Tuple<String, String>> tags, int opnaMasterClock, int opnbMasterClock, int opmMasterClock) {
        if (dest == null) return;

        // ヘッダ、フッタの調整

        // end of data
        dest.writeByte((byte) 0x66);

        // Total # samples
        dest.setPosition(0x18);
        dest.writeByte((byte) (totalSample & 0xff));
        dest.writeByte((byte) ((totalSample >> 8) & 0xff));
        dest.writeByte((byte) ((totalSample >> 16) & 0xff));
        dest.writeByte((byte) ((totalSample >> 24) & 0xff));

        //tag
        if (tags != null) {
            GD3 gd3 = new GD3();
            for (Tuple<String, String> tag : tags) {
                switch (tag.getItem1()) {
                case "title":
                    gd3.trackName = tag.getItem2();
                    gd3.trackNameJ = tag.getItem2();
                    break;
                case "composer":
                    gd3.composer = tag.getItem2();
                    gd3.composerJ = tag.getItem2();
                    break;
                case "author":
                    gd3.vgmBy = tag.getItem2();
                    break;
                case "comment":
                    gd3.notes = tag.getItem2();
                    break;
                case "mucom88":
                    gd3.version = tag.getItem2();
                    gd3.notes = tag.getItem2();
                    break;
                case "date":
                    gd3.converted = tag.getItem2();
                    break;
                }
            }

            byte[] tagBytes = gd3.make();
            dest.seek(0, SeekOrigin.End);
            long gd3ofs = dest.getLength() - 0x14;
            for (byte b : tagBytes) dest.writeByte(b);

            //Tag offset
            if (tagBytes.length > 0) {
                dest.setPosition(0x14);
                dest.writeByte((byte) (gd3ofs & 0xff));
                dest.writeByte((byte) ((gd3ofs >> 8) & 0xff));
                dest.writeByte((byte) ((gd3ofs >> 16) & 0xff));
                dest.writeByte((byte) ((gd3ofs >> 24) & 0xff));
            }
        }

        //EOF offset
        dest.setPosition(0x4);
        dest.writeByte((byte) ((dest.getLength() - 4) & 0xff));
        dest.writeByte((byte) (((dest.getLength() - 4) >> 8) & 0xff));
        dest.writeByte((byte) (((dest.getLength() - 4) >> 16) & 0xff));
        dest.writeByte((byte) (((dest.getLength() - 4) >> 24) & 0xff));

        //YM2608 offset
        dest.setPosition(0x48);
        dest.writeByte((byte) 0);
        dest.writeByte((byte) 0);
        dest.writeByte((byte) 0);
        dest.writeByte((byte) 0);

        //YM2610 offset
        dest.setPosition(0x4c);
        dest.writeByte((byte) 0);
        dest.writeByte((byte) 0);
        dest.writeByte((byte) 0);
        dest.writeByte((byte) 0);

        //YM2151 offset
        dest.setPosition(0x30);
        dest.writeByte((byte) 0);
        dest.writeByte((byte) 0);
        dest.writeByte((byte) 0);
        dest.writeByte((byte) 0);

        for (int i = 0; i < 5; i++) {
            if (useChips[i] == 0 || useChips[i] > 5) continue;
            switch (useChips[i]) {
            case 1:
            case 2:
                dest.setPosition(0x48);
                dest.writeByte((byte) (opnaMasterClock >> 0));
                dest.writeByte((byte) (opnaMasterClock >> 8));
                dest.writeByte((byte) (opnaMasterClock >> 16));
                if (useChips[i] == 1) dest.writeByte((byte) 0);
                else dest.writeByte((byte) 0x40);
                break;
            case 3:
            case 4:
                dest.setPosition(0x4c);
                dest.writeByte((byte) (opnbMasterClock >> 0));
                dest.writeByte((byte) (opnbMasterClock >> 8));
                dest.writeByte((byte) (opnbMasterClock >> 16));
                if (useChips[i] == 3) dest.writeByte((byte) 0);
                else dest.writeByte((byte) 0x40);
                break;
            case 5:
                dest.setPosition(0x30);
                dest.writeByte((byte) (opmMasterClock >> 0));
                dest.writeByte((byte) (opmMasterClock >> 8));
                dest.writeByte((byte) (opmMasterClock >> 16));
                if (useChips[i] == 5) dest.writeByte((byte) 0);
                else dest.writeByte((byte) 0x40);
                break;
            }
        }

        dest.close();
        dest = null;
    }

    public void open(String fullPath) {
        if (dest != null) close(null, 0, 0, 0);
        dest = new FileStream(fullPath, FileMode.Create, FileAccess.Write);

        List<Byte> des = new ArrayList<>();

        //ヘッダの出力
        dest.write(hDat, 0, hDat.length);

    }

    public void incrementWaitCOunter() {
        waitCounter++;
    }

    public void writeAdpcm(byte chipId, byte[] adpcmData) {
        if (useChips[chipId] == 0 || adpcmData == null || adpcmData.length < 1) return;

        dest.writeByte((byte) 0x67);
        dest.writeByte((byte) 0x66);
        dest.writeByte((byte) 0x81);

        writePCMData(chipId, adpcmData);
    }

    public void writeYM2610SetAdpcmA(byte chipId, byte[] pcmData) {
        dest.writeByte((byte) 0x67);
        dest.writeByte((byte) 0x66);
        dest.writeByte((byte) 0x82);

        writePCMData(chipId, pcmData);
    }

    public void writeYM2610SetAdpcmB(byte chipId, byte[] pcmData) {

        dest.writeByte((byte) 0x67);
        dest.writeByte((byte) 0x66);
        dest.writeByte((byte) 0x83);

        writePCMData(chipId, pcmData);
    }

    private void writePCMData(byte chipId, byte[] pcmData) {
        int size = pcmData.length;

        long sizeOfData = size + 8 + chipId * 0x8000_0000;
        dest.writeByte((byte) (sizeOfData >> 0));
        dest.writeByte((byte) (sizeOfData >> 8));
        dest.writeByte((byte) (sizeOfData >> 16));
        dest.writeByte((byte) (sizeOfData >> 24));

        dest.writeByte((byte) (size >> 0));
        dest.writeByte((byte) (size >> 8));
        dest.writeByte((byte) (size >> 16));
        dest.writeByte((byte) (size >> 24));

        int startAddress = 0;
        dest.writeByte((byte) (startAddress >> 0));
        dest.writeByte((byte) (startAddress >> 8));
        dest.writeByte((byte) (startAddress >> 16));
        dest.writeByte((byte) (startAddress >> 24));

        for (int i = 0x0; i < pcmData.length; i++) {
            dest.writeByte(pcmData[i]);
        }
    }

    public void useChipsFromMub(byte[] buf) {
        List<Integer> ret = new ArrayList<>();
        ret.add(1); // 1: OPNA
        ret.add(0); // 0: unuse
        ret.add(0);
        ret.add(0);
        ret.add(0);
        useChips = Common.toIntArray(ret);

        //dest.writeByte(0x56); dest.writeByte(0x29); dest.writeByte(0x82);
        //writeAdpcm(0, new byte[65536]);

        // 標準的な mub ファイル
        if (buf[0] == 0x4d &&
                buf[1] == 0x55 &&
                buf[2] == 0x43 &&
                buf[3] == 0x38) {
            return;
        }
        // 標準的な mub ファイル
        if (buf[0] == 0x4d &&
                buf[1] == 0x55 &&
                buf[2] == 0x42 &&
                buf[3] == 0x38) {
            return;
        }
        // 拡張 mubファイル？
        if (buf[0] != 'm' ||
                buf[1] != 'u' ||
                buf[2] != 'P' ||
                buf[3] != 'b') {
            // 見知らぬファイル
            return;
        }

        int chipsCount = buf[0x0009];
        int ptr = 0x0022;
        int[] partCount = new int[chipsCount];
        int[][] pageCount = new int[chipsCount][];
        int[][][] pageLength = new int[chipsCount][][];
        for (int i = 0; i < chipsCount; i++) {
            partCount[i] = buf[ptr + 0x16];
            int instCount = buf[ptr + 0x17];
            ptr += 2 * instCount + 0x18;
            int pcmCount = buf[ptr];
            ptr += 2 * pcmCount + 1;
        }

        for (int i = 0; i < chipsCount; i++) {
            pageCount[i] = new int[partCount[i]];
            pageLength[i] = new int[partCount[i]][];
            for (int j = 0; j < partCount[i]; j++) {
                pageCount[i][j] = buf[ptr++];
            }
        }

        for (int i = 0; i < chipsCount; i++) {
            for (int j = 0; j < partCount[i]; j++) {
                pageLength[i][j] = new int[pageCount[i][j]];
                for (int k = 0; k < pageCount[i][j]; k++) {
                    pageLength[i][j][k] = buf[ptr]
                            + buf[ptr + 1] * 0x100
                            + buf[ptr + 2] * 0x10000
                            + buf[ptr + 3] * 0x1000000;
                    ptr += 8;
                }
            }
        }

        ret.clear();
        ret.add(0);
        ret.add(0);
        ret.add(0);
        ret.add(0);
        ret.add(0);

        if (chipsCount > 0) {
            if (partCount[0] > 0) {
                int n = 0;
                for (int i = 0; i < partCount[0]; i++) {
                    n += pageCount[0][i];
                }
                if (n > 0) ret.set(0, 1);
            }
        }

        if (chipsCount > 1) {
            if (partCount[1] > 0) {
                int n = 0;
                for (int i = 0; i < partCount[1]; i++) {
                    n += pageCount[1][i];
                }
                if (n > 0) {
                    ret.set(1, 2);
                    dest.writeByte((byte) 0xa6);
                    dest.writeByte((byte) 0x29);
                    dest.writeByte((byte) 0x82);
                }
            }
        }

        if (chipsCount > 2) {
            if (partCount[2] > 0) {
                int n = 0;
                for (int i = 0; i < partCount[2]; i++) {
                    n += pageCount[2][i];
                }
                if (n > 0) ret.set(2, 3);
            }
        }

        if (chipsCount > 3) {
            if (partCount[3] > 0) {
                int n = 0;
                for (int i = 0; i < partCount[3]; i++) {
                    n += pageCount[3][i];
                }
                if (n > 0) ret.set(3, 4);
            }
        }

        if (chipsCount > 4) {
            if (partCount[4] > 0) {
                int n = 0;
                for (int i = 0; i < partCount[4]; i++) {
                    n += pageCount[4][i];
                }
                if (n > 0) ret.set(4, 5);
            }
        }

        useChips = Common.toIntArray(ret);
    }
}
