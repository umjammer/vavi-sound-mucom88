package mucom88.wav;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import vavi.util.ByteUtil;


public class WaveWriter {

    private RandomAccessFile dest = null;
    private int len = 0;
    private final int sampleFreq;

    public WaveWriter(int samplingFreq /* = 44100 */) {
        sampleFreq = samplingFreq;
    }

    public void open(String fullPath) throws IOException {
        if (dest != null) close();
        dest = new RandomAccessFile(fullPath, "rw");

        List<Byte> des = new ArrayList<>();
        len = 0;

        // 'RIFF'
        des.add((byte) 'R');
        des.add((byte) 'I');
        des.add((byte) 'F');
        des.add((byte) 'F');
        // size
        int fileSize = len + 36;
        des.add((byte) ((fileSize & 0xff) >> 0));
        des.add((byte) ((fileSize & 0xff00) >> 8));
        des.add((byte) ((fileSize & 0xff0000) >> 16));
        des.add((byte) ((fileSize & 0xff000000) >> 24));
        // 'WAVE'
        des.add((byte) 'W');
        des.add((byte) 'A');
        des.add((byte) 'V');
        des.add((byte) 'E');
        // 'fmt '
        des.add((byte) 'f');
        des.add((byte) 'm');
        des.add((byte) 't');
        des.add((byte) ' ');
        // size(16)
        des.add((byte) 0x10);
        des.add((byte) 0);
        des.add((byte) 0);
        des.add((byte) 0);
        // format(1)
        des.add((byte) 0x01);
        des.add((byte) 0x00);
        // Number of channels (stereo)
        des.add((byte) 0x02);
        des.add((byte) 0x00);
        // Sampling frequency (44100Hz)
        des.add((byte) ((sampleFreq & 0xff) >> 0));
        des.add((byte) ((sampleFreq & 0xff00) >> 8));
        des.add((byte) ((sampleFreq & 0xff0000) >> 16));
        des.add((byte) ((sampleFreq & 0xff000000) >> 24));
        // Average Data Percentage
        des.add((byte) 0x10);
        des.add((byte) 0xb1);
        des.add((byte) 0x02);
        des.add((byte) 0); //10 B1 02 00
        // Block size(4)
        des.add((byte) 0x04);
        des.add((byte) 0x00);
        // Bit depth (16bit)
        des.add((byte) 0x10);
        des.add((byte) 0x00);

        // 'data'
        des.add((byte) 'd');
        des.add((byte) 'a');
        des.add((byte) 't');
        des.add((byte) 'a');
        // size(data size)
        des.add((byte) ((len & 0xff) >> 0));
        des.add((byte) ((len & 0xff00) >> 8));
        des.add((byte) ((len & 0xff0000) >> 16));
        des.add((byte) ((len & 0xff000000) >> 24));

        // output
        dest.write(ByteUtil.toByteArray(des), 0, des.size());
    }

    public void close() throws IOException {
        if (dest == null) return;

        dest.seek(4);
        int fsize = len + 36;
        dest.writeByte((byte) ((fsize & 0xff) >> 0));
        dest.writeByte((byte) ((fsize & 0xff00) >> 8));
        dest.writeByte((byte) ((fsize & 0xff0000) >> 16));
        dest.writeByte((byte) ((fsize & 0xff000000) >> 24));

        dest.seek(40);
        dest.writeByte((byte) ((len & 0xff) >> 0));
        dest.writeByte((byte) ((len & 0xff00) >> 8));
        dest.writeByte((byte) ((len & 0xff0000) >> 16));
        dest.writeByte((byte) ((len & 0xff000000) >> 24));

        dest.close();
        dest = null;
    }

    public void write(short[] buffer, int offset, int sampleCount) throws IOException {
        if (dest == null) return;

        for (int i = 0; i < sampleCount; i++) {
            dest.writeByte((byte) (buffer[offset + i] & 0xff));
            dest.writeByte((byte) ((buffer[offset + i] & 0xff00) >> 8));
        }
        len += sampleCount * 2;
    }
}

