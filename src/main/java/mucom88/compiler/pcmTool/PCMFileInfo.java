package mucom88.compiler.pcmTool;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import mucom88.common.MucException;

import static java.lang.System.getLogger;
import static vavi.util.compat.Util.getExtension;


public class PCMFileInfo {

    private static final Logger logger = getLogger(PCMFileInfo.class.getName());

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

    public PCMFileInfo(List<String> itemList, Function<String, InputStream> appendFileReaderCallback /* = null */) throws IOException {
        if (itemList == null) return;

        int n;
        if (!itemList.isEmpty()) {
            String item = itemList.getFirst().toLowerCase().trim();
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
        try (InputStream pd = appendFileReaderCallback.apply(fileName)) {
            buf = pd.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (buf == null) {
            if (Files.exists(Path.of(fileName))) {
                boolean[] isRaw = new boolean[1];
                int[] samplerate = new int[1];
                raw = getPCMDataFromFile("", fileName, volume, /* out */ isRaw, /* out */ is16bit, /* out */ samplerate);
                if (raw != null) length = raw.length & 0xffff;
                else
                    throw new MucException("Fail get pcm data from file[%s].".formatted(fileName));
            } else {
                logger.log(Level.WARNING, "file[%s] not found".formatted(fileName));
            }
        } else {
            boolean[] isRaw = new boolean[1];
            int[] samplerate = new int[1];
            raw = getPCMDataFromFile(buf, volume, /* out */ isRaw, /* out */ is16bit, /* out */ samplerate);
            if (raw != null) length = raw.length & 0xffff;
            else
                throw new MucException("Fail get pcm data from file[%s].".formatted(fileName));
        }
    }

    public void encode(FormatType formatType) {
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

    public static byte[] getPCMDataFromFile(String path, String fileName, int vol, /* out */ boolean[] isRaw, /* out */ boolean[] is16bit, /* out */ int[] samplerate) throws IOException {
        Path fnPcm = Path.of(path, fileName);

        isRaw[0] = false;
        is16bit[0] = false;
        samplerate[0] = 8000;

        if (!Files.exists(fnPcm)) {
            logger.log(Level.ERROR, "File not found.");
            return null;
        }

        // Loading a file
        byte[] buf = Files.readAllBytes(fnPcm);

        if (!getExtension(fileName).toUpperCase().trim().equals(".WAV")) {
            isRaw[0] = true;
            return buf;
        }

        return getPCMDataFromFile(buf, vol, /* out */ isRaw, /* out */ is16bit, /* out */ samplerate);
    }

    public static byte[] getPCMDataFromFile(byte[] buf, int vol, /* out */ boolean[] isRaw, /* out */ boolean[] is16bit, /* out */ int[] samplerate) {
        isRaw[0] = false;
        is16bit[0] = false;
        samplerate[0] = 8000;

        if (buf.length < 4) {
            logger.log(Level.ERROR, "This file is not wave.");
            return null;
        }
        if (buf[0] != 'R' || buf[1] != 'I' || buf[2] != 'F' || buf[3] != 'F') {
            logger.log(Level.ERROR, "This file is not wave.");
            return null;
        }

        // Get size
        int fSize = (buf[0x4] & 0xff) + (buf[0x5] & 0xff) * 0x100 + (buf[0x6] & 0xff) * 0x1_0000 + (buf[0x7] & 0xff) * 0x100_0000;

        if (buf[0x8] != 'W' || buf[0x9] != 'A' || buf[0xa] != 'V' || buf[0xb] != 'E') {
            logger.log(Level.ERROR, "This file is not wave.");
            return null;
        }

        try {
            int p = 12;
            byte[] des = null;

            while (p < fSize + 8) {
                if (buf[p + 0] == 'f' && buf[p + 1] == 'm' && buf[p + 2] == 't' && buf[p + 3] == ' ') {
                    p += 4;
                    int size = (buf[p + 0] & 0xff) + (buf[p + 1] & 0xff) * 0x100 + (buf[p + 2] & 0xff) * 0x1_0000 + (buf[p + 3] & 0xff) * 0x100_0000;
                    p += 4;
                    int format = (buf[p + 0] & 0xff) + (buf[p + 1] & 0xff) * 0x100;
                    if (format != 1) {
                        logger.log(Level.ERROR, "isn't Mono.");
                        return null;
                    }

                    int channels = (buf[p + 2] & 0xff) + (buf[p + 3] & 0xff) * 0x100;
                    if (channels != 1) {
                        logger.log(Level.ERROR, "isn't Mono.");
                        return null;
                    }

                    samplerate[0] = (buf[p + 4] & 0xff) + (buf[p + 5] & 0xff) * 0x100 + (buf[p + 6] & 0xff) * 0x1_0000 + (buf[p + 7] & 0xff) * 0x100_0000;
                    if (samplerate[0] != 8000 && samplerate[0] != 16000 && samplerate[0] != 18500 && samplerate[0] != 14000) {
//                        logger.log(Level.WARNING, "Unknown sample-rate.");
//                        return null;
                    }

                    int bytepersec = (buf[p + 8] & 0xff) + (buf[p + 9] & 0xff) * 0x100 + (buf[p + 10] & 0xff) * 0x1_0000 + ((buf[p + 11] & 0xff) * 0x100_0000);
                    if (bytepersec != 8000) {
//                        msgBox.setWrnMsg(String.Format("PCM files: Average data percentage different from specification.(%s)", bytepersec));
//                        return null;
                    }

                    int bitswidth = (buf[p + 14] & 0xff) + (buf[p + 15] & 0xff) * 0x100;
                    if (bitswidth != 8 && bitswidth != 16) {
                        logger.log(Level.ERROR, "Unknown bits-width.");
                        return null;
                    }

                    is16bit[0] = bitswidth == 16;

                    int blockAlign = (buf[p + 12] & 0xff) + (buf[p + 13] & 0xff) * 0x100;
                    if (blockAlign != (is16bit[0] ? 2 : 1)) {
                        logger.log(Level.ERROR, "Unknown block-align.");
                        return null;
                    }

                    p += size;
                } else if (buf[p + 0] == 'd' && buf[p + 1] == 'a' && buf[p + 2] == 't' && buf[p + 3] == 'a') {
                    p += 4;
                    int size = (buf[p + 0] & 0xff) + (buf[p + 1] & 0xff) * 0x100 + (buf[p + 2] & 0xff) * 0x1_0000 + (buf[p + 3] & 0xff) * 0x100_0000;
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

                    int size = (buf[p + 0] & 0xff) + (buf[p + 1] & 0xff) * 0x100 + (buf[p + 2] & 0xff) * 0x1_0000 + (buf[p + 3] & 0xff) * 0x100_0000;
                    p += 4;

                    p += size;
                }
            }

            // Volume processing
            if (is16bit[0]) {
                for (int i = 0; i < des.length; i += 2) {
                    // 16bit wav files are signed data so you can change the volume as is
                    int b = (int) ((short) ((des[i] & 0xff) | ((des[i + 1] & 0xff) << 8)) * vol * 0.01);
                    b = Math.clamp(b, -0x8000, 0x7fff);
                    des[i] = (byte) (b & 0xff);
                    des[i + 1] = (byte) ((b & 0xff00) >> 8);
                }
            } else {
                for (int i = 0; i < des.length; i++) {
                    // 8-bit wav files are unsigned data, so change them to signed data before changing the volume.
                    int d = des[i];
                    // let it Signed
                    d -= 0x80;
                    d = (int) (d * vol * 0.01);
                    // clip
                    d = Math.clamp(d, -128, 127);
                    // let it Unsigned
                    d += 0x80;

                    des[i] = (byte) d;
                }
            }

            return des;
        } catch (Exception e) {
            logger.log(Level.ERROR, "Unknown error: " + e);
            return null;
        }
    }
}
