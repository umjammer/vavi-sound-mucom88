package mucom88.compiler.pcmTool;

import java.util.ArrayList;
import java.util.List;

import static dotnet4j.util.compat.CollectionUtilities.toByteArray;


// https://wiki.neogeodev.org/index.php?title=ADPCM_codecs
public class EncAdpcmA {

    static final short[] step_size = {
            16, 17, 19, 21, 23, 25, 28, 31, 34, 37,
            41, 45, 50, 55, 60, 66, 73, 80, 88, 97,
            107, 118, 130, 143, 157, 173, 190, 209, 230, 253,
            279, 307, 337, 371, 408, 449, 494, 544, 598, 658,
            724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552
    }; //49 items

    static final int[] step_adj = {-1, -1, -1, -1, 2, 5, 7, 9, -1, -1, -1, -1, 2, 5, 7, 9};

    // buffers
    /** temp Work buffer, used correct byte order and downsample */
    private short[] inBuffer;
    /** output buffer, this is your PCM file, save it */
    private byte[] outBuffer;

    // decode stuff
    private int[] jedi_table;
    /** ADPCM accumulator, initial condition must be 0 */
    int acc = 0;
    /** ADPCM decoding step, initial condition must be 0 */
    int decstep = 0;

    //encode stuff
    int diff;
    int step;
    int predsample;
    int index;
    /**  previous sample, initial condition must be 0 */
    int prevsample = 0;
    /** previous index, initial condition must be 0 */
    int previndex = 0;

    /** jedi table is used speed up decoding, run this to init the table before encoding. Mame copy-pasta. */
    private void jedi_table_init() {
        int step, nib;

        jedi_table = new int[16 * 49];
        for (step = 0; step < 49; step++) {
            for (nib = 0; nib < 16; nib++) {
                int value = (2 * (nib & 0x07) + 1) * step_size[step] / 8;
                jedi_table[step * 16 + nib] = ((nib & 0x08) != 0) ? -value : value;
            }
        }
    }

    /** decode sub, returns decoded 12bit data */
    private short YM2610_ADPCM_A_Decode(byte code) {
        acc += jedi_table[decstep + code];
        if ((acc & ~0x7ff) != 0) // acc is > 2047
            acc |= ~0xfff;
        else acc &= 0xfff;
        decstep += step_adj[code & 7] * 16;
        if (decstep < 0) decstep = 0;
        if (decstep > 48 * 16) decstep = 48 * 16;
        return (short) acc;
    }

    /** our encoding sub, returns ADPCM nibble */
    private byte YM2610_ADPCM_A_Encode(short sample) {
        int tempstep;
        byte code;

        predsample = prevsample;
        index = previndex;
        step = step_size[index];

        diff = sample - predsample;
        if (diff >= 0)
            code = 0;
        else {
            code = 8;
            diff = -diff;
        }

        tempstep = step;
        if (diff >= tempstep) {
            code |= 4;
            diff -= tempstep;
        }
        tempstep >>= 1;
        if (diff >= tempstep) {
            code |= 2;
            diff -= tempstep;
        }
        tempstep >>= 1;
        if (diff >= tempstep) code |= 1;

        predsample = YM2610_ADPCM_A_Decode(code);

        index += step_adj[code];
        if (index < 0) index = 0;
        if (index > 48) index = 48;

        prevsample = predsample;
        previndex = index;

        return code;
    }

    public EncAdpcmA() {
        jedi_table_init();
    }

    // our main sub, init buffers and runs the encode process
    // enter this with your sound file loaded into buffer

    /**
     * input buffer, load your sound file into this
     */
    public byte[] YM_ADPCM_A_Encode(byte[] buffer, boolean is16bit) {
        int i;

        // reset to initial conditions
        acc = 0;
        decstep = 0;
        prevsample = 0;
        previndex = 0;

        if (is16bit) {
            // watch out for odd data count & allocate buffers
            if ((buffer.length / 2) % 2 != 0) {
                inBuffer = new short[(buffer.length / 2) + 1];
                inBuffer[inBuffer.length - 1] = 0x00;
            } else inBuffer = new short[buffer.length / 2];

            // fix byte order and downscale data to 12 bits
            for (i = 0; i < buffer.length; i += 2) {
                inBuffer[i / 2] = (short) ((buffer[i] & 0xff) | ((buffer[i + 1] & 0xff) << 8));
                inBuffer[i / 2] >>= 4;
            }
        } else {
            if (buffer.length % 2 != 0) {
                inBuffer = new short[buffer.length + 1];
                inBuffer[inBuffer.length - 1] = 0x00;
            } else inBuffer = new short[buffer.length];

            for (i = 0; i < buffer.length; i++) {
                inBuffer[i] = (short) (buffer[i] - 128);
                inBuffer[i] <<= 4;
            }
        }

        int outSize = inBuffer.length / 2;
        outSize = (outSize % 0x100) != 0 ? ((outSize / 0x100) + 1) * 0x100 : outSize;
        outBuffer = new byte[outSize];

        // actual encoding
        for (i = 0; i < inBuffer.length; i += 2) {
            outBuffer[i / 2] = (byte) ((YM2610_ADPCM_A_Encode(inBuffer[i]) << 4) | YM2610_ADPCM_A_Encode(inBuffer[i + 1]) & 0xff);
        }
        // padding
        for (i = i / 2; i < outBuffer.length; i++) {
            outBuffer[i] = (byte) ((YM2610_ADPCM_A_Encode((byte) 0x00) << 4) | YM2610_ADPCM_A_Encode((byte) 0x00) & 0xff);
        }

        return outBuffer;
    }


    static long[] stepsizeTable = {57, 57, 57, 57, 77, 102, 128, 153, 57, 57, 57, 57, 77, 102, 128, 153};
    //private byte[] buffer;  //our input buffer, load your sample file into this before encoding
    //private byte[] outBuffer; //our output buffer, this is your PCM file, save it after encoding. Its size has to be allocated to buffer.length / 4 (16 bits per sample to 4 bits per sample)
    //private int outBufferIndex = 0; //reset to 0 before each encoding

    public byte[] YM_ADPCM_B_Encode(byte[] buffer, boolean is16bit, boolean isYM2610B, boolean isY8950/* =false*/) {
        int lpc, flag;
        long i, dn, xn, stepSize;
        byte adpcm;
        byte adpcmPack = 0;
        short src;
        List<Byte> outBuffer = new ArrayList<>();

        xn = 0;
        stepSize = 127;
        flag = 0;

        int pad = isYM2610B ? 0x100 : 0x4;
        if (isY8950) pad = 0x20;

        int size = (buffer.length / (is16bit ? 2 : 1));
        size = (((size / 2) % pad) == 0) ? size : (size / 2 / pad + 1) * pad * 2;
        for (lpc = 0; lpc < size; lpc++) {
            if (is16bit) {
                if (buffer.length > lpc * 2 + 1)
                    src = (short) ((buffer[lpc * 2] & 0xff) | (buffer[lpc * 2 + 1] & 0xff) << 8); //16 bit samples, + fixing byte order
                else src = 0;
            } else {
                if (buffer.length > lpc)
                    src = (short) ((buffer[lpc] - 128) << 8);
                else src = 0;
            }

            dn = src - xn;
            i = (Math.abs(dn) << 16) / (stepSize << 14);
            if (i > 7) i = 7;
            adpcm = (byte) i;
            i = (adpcm * 2 + 1) * stepSize / 8;
            if (dn < 0) {
                adpcm |= 0x8;
                xn -= i;
            } else {
                xn += i;
            }
            stepSize = (stepsizeTable[adpcm] * stepSize) / 64;
            if (stepSize < 127)
                stepSize = 127;
            else if (stepSize > 24576)
                stepSize = 24576;
            if (flag == 0) {
                adpcmPack = (byte) (adpcm << 4);
                flag = 1;
            } else {
                adpcmPack |= adpcm;
                //outBuffer[outBufferIndex++] = adpcmPack;
                outBuffer.add(adpcmPack);
                flag = 0;
            }
        }

        return toByteArray(outBuffer);
    }
}

