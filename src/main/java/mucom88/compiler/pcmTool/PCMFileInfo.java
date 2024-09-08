package mucom88.compiler.pcmTool;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;

import dotnet4j.io.File;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Path;
import dotnet4j.io.Stream;
import mucom88.common.MucException;
import vavi.util.Debug;


public class PCMFileInfo {
    private int number;

    public int getNumber() {
        return number;
    }

    private String name;

    public String getName() {
        return name;
    }

    private String fileName;

    public String getFileName() {
        return fileName;
    }

    private int volume;

    public int getVolume() {
        return volume;
    }

    private int length = -1;

    public int getLength() {
        return length;
    }

    private byte[] raw = null;

    public byte[] getRaw() {
        return raw;
    }

    private byte[] encData = null;

    public byte[] getEncData() {
        return encData;
    }

    private boolean[] is16bit;

    public PCMFileInfo(List<String> itemList, Function<String, Stream> appendFileReaderCallback/* = null*/) {
        if (itemList == null) return;

        int n;
        if (itemList.size() > 0) {
            String item = itemList.get(0).toLowerCase().trim();
            if (item.length() > 1 && item.charAt(0) == '$') {
                n = Integer.parseInt(item.substring(1), 16);
                number = n;
            } else {
                try {
                    number = Integer.parseInt(item);
                } catch (NumberFormatException e) {
                    if (item.length() > 1 && item.charAt(0) == 'o') {
                        n = Integer.parseInt(item.substring(1, 1 + 1));
                        if (item.length() > 2) {
                            String[] note = new String[] {"c", "c+", "d", "d+", "e", "f", "f+", "g", "g+", "a", "a+", "b"};
                            for (int i = 0; i < note.length; i++) {
                                if (!note[i].equals(item.substring(2))) continue;
                                n = n * 16 + i + 1;
                                number = n;
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (itemList.size() > 1) name = itemList.get(1);
        if (itemList.size() > 2) fileName = itemList.get(2);
        if (itemList.size() > 3) volume = Integer.parseInt(itemList.get(3));

        byte[] buf;
        try (Stream pd = appendFileReaderCallback.apply(fileName)) {
            buf = ReadAllBytes(pd);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (buf == null) {
            if (File.exists(fileName)) {
                boolean[] isRaw = new boolean[1];
                int[] samplerate = new int[1];
                raw = GetPCMDataFromFile("", fileName, volume, /*out*/ isRaw, /*out*/ is16bit, /*out*/ samplerate);
                if (raw != null) length = (short) raw.length;
                else
                    throw new MucException(String.format("Fail get pcm data from file[%s].", fileName));
            } else {
                Debug.printf(Level.WARNING, "file[%s] not found", fileName);
            }
        } else {
            boolean[] isRaw = new boolean[1];
            int[] samplerate = new int[1];
            raw = GetPCMDataFromFile(buf, volume, /*out*/ isRaw, /*out*/ is16bit, /*out*/ samplerate);
            if (raw != null) length = (short) raw.length;
            else
                throw new MucException(String.format("Fail get pcm data from file[%s].", fileName));
        }

    }

    /**
     * ストリームから一括でバイナリを読み込む
     */
    private byte[] ReadAllBytes(Stream stream) {
        if (stream == null) return null;

        var buf = new byte[8192];
        try (var ms = new MemoryStream()) {
            while (true) {
                var r = stream.read(buf, 0, buf.length);
                if (r < 1) {
                    break;
                }
                ms.write(buf, 0, r);
            }
            return ms.toArray();
        }
    }

    public void Encode(FormatType formatType) {
        EncAdpcmA enc = new EncAdpcmA();

        switch (formatType) {
        case mucom88:
        case mucomDotNET_OPNA_ADPCM:
            encData = enc.YM_ADPCM_B_Encode(raw, is16bit[0], false, false);
            break;
        case mucomDotNET_OPNB_ADPCMA:
            encData = enc.YM_ADPCM_A_Encode(raw, is16bit[0]);
            break;
        case mucomDotNET_OPNB_ADPCMB:
            encData = enc.YM_ADPCM_B_Encode(raw, is16bit[0], true, false);
            break;
        }
        length = encData.length;
    }

    public static byte[] GetPCMDataFromFile(String path, String fileName, int vol, /*out*/ boolean[] isRaw, /*out*/ boolean[] is16bit, /*out*/ int[] samplerate) {
        String fnPcm = Path.combine(path, fileName).replace('\\', Path.DirectorySeparatorChar).replace('/', Path.DirectorySeparatorChar);

        isRaw[0] = false;
        is16bit[0] = false;
        samplerate[0] = 8000;

        if (!File.exists(fnPcm)) {
            Debug.printf(Level.SEVERE, "File not found.");
            return null;
        }

        // ファイルの読み込み
        byte[] buf = File.readAllBytes(fnPcm);

        if (!Path.getExtension(fileName).toUpperCase().trim().equals(".WAV")) {
            isRaw[0] = true;
            return buf;
        }

        return GetPCMDataFromFile(buf, vol, /*out*/ isRaw, /*out*/ is16bit, /*out*/ samplerate);
    }

    public static byte[] GetPCMDataFromFile(byte[] buf, int vol, /*out*/ boolean[] isRaw, /*out*/ boolean[] is16bit, /*out*/ int[] samplerate) {
        isRaw[0] = false;
        is16bit[0] = false;
        samplerate[0] = 8000;

        if (buf.length < 4) {
            Debug.printf(Level.SEVERE, "This file is not wave.");
            return null;
        }
        if (buf[0] != 'R' || buf[1] != 'I' || buf[2] != 'F' || buf[3] != 'F') {
            Debug.printf(Level.SEVERE, "This file is not wave.");
            return null;
        }

        // サイズ取得
        int fSize = buf[0x4] + buf[0x5] * 0x100 + buf[0x6] * 0x10000 + buf[0x7] * 0x1000000;

        if (buf[0x8] != 'W' || buf[0x9] != 'A' || buf[0xa] != 'V' || buf[0xb] != 'E') {
            Debug.printf(Level.SEVERE, "This file is not wave.");
            return null;
        }

        try {
            int p = 12;
            byte[] des = null;

            while (p < fSize + 8) {
                if (buf[p + 0] == 'f' && buf[p + 1] == 'm' && buf[p + 2] == 't' && buf[p + 3] == ' ') {
                    p += 4;
                    int size = buf[p + 0] + buf[p + 1] * 0x100 + buf[p + 2] * 0x10000 + buf[p + 3] * 0x1000000;
                    p += 4;
                    int format = buf[p + 0] + buf[p + 1] * 0x100;
                    if (format != 1) {
                        Debug.printf(Level.SEVERE, "isn't Mono.");
                        return null;
                    }

                    int channels = buf[p + 2] + buf[p + 3] * 0x100;
                    if (channels != 1) {
                        Debug.printf(Level.SEVERE, "isn't Mono.");
                        return null;
                    }

                    samplerate[0] = buf[p + 4] + buf[p + 5] * 0x100 + buf[p + 6] * 0x10000 + buf[p + 7] * 0x1000000;
                    if (samplerate[0] != 8000 && samplerate[0] != 16000 && samplerate[0] != 18500 && samplerate[0] != 14000) {
//                        Debug.printf(Level.WARNING, "Unknown samplerate.");
//                        return null;
                    }

                    int bytepersec = buf[p + 8] + buf[p + 9] * 0x100 + buf[p + 10] * 0x10000 + buf[p + 11] * 0x1000000;
                    if (bytepersec != 8000) {
//                        msgBox.setWrnMsg(String.Format("PCMファイル：仕様とは異なる平均データ割合です。(%s)", bytepersec));
//                        return null;
                    }

                    int bitswidth = buf[p + 14] + buf[p + 15] * 0x100;
                    if (bitswidth != 8 && bitswidth != 16) {
                        Debug.printf(Level.SEVERE, "Unknown bitswidth.");
                        return null;
                    }

                    is16bit[0] = bitswidth == 16;

                    int blockalign = buf[p + 12] + buf[p + 13] * 0x100;
                    if (blockalign != (is16bit[0] ? 2 : 1)) {
                        Debug.printf(Level.SEVERE, "Unknown blockalign.");
                        return null;
                    }

                    p += size;
                } else if (buf[p + 0] == 'd' && buf[p + 1] == 'a' && buf[p + 2] == 't' && buf[p + 3] == 'a') {
                    p += 4;
                    int size = buf[p + 0] + buf[p + 1] * 0x100 + buf[p + 2] * 0x10000 + buf[p + 3] * 0x1000000;
                    p += 4;

                    des = new byte[size];
                    System.arraycopy(buf, p, des, 0x00, size);
                    p += size;
                } else {
                    p += 4;

                    if (p > buf.length - 4) {
                        p = fSize + 8;
                        break;
                    }

                    int size = buf[p + 0] + buf[p + 1] * 0x100 + buf[p + 2] * 0x10000 + buf[p + 3] * 0x1000000;
                    p += 4;

                    p += size;
                }
            }

            // volumeの加工
            if (is16bit[0]) {
                for (int i = 0; i < des.length; i += 2) {
                    // 16bitのwavファイルはsignedのデータのためそのままボリューム変更可能
                    int b = (int) ((short) ((des[i] & 0xff) | ((des[i + 1] & 0xff) << 8)) * vol * 0.01);
                    b = (b > 0x7fff) ? 0x7fff : b;
                    b = (b < -0x8000) ? -0x8000 : b;
                    des[i] = (byte) (b & 0xff);
                    des[i + 1] = (byte) ((b & 0xff00) >> 8);
                }
            } else {
                for (int i = 0; i < des.length; i++) {
                    // 8bitのwavファイルはunsignedのデータのためsignedのデータに変更してからボリューム変更する
                    int d = des[i];
                    // signed化
                    d -= 0x80;
                    d = (int) (d * vol * 0.01);
                    //clip
                    d = (d > 127) ? 127 : d;
                    d = (d < -128) ? -128 : d;
                    // unsigned化
                    d += 0x80;

                    des[i] = (byte) d;
                }
            }

            return des;
        } catch (Exception e) {
            Debug.printf(Level.SEVERE, "Unknown error: " + e);
            return null;
        }
    }
}
