package mucom88.compiler;

import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.logging.Level;

import dotnet4j.util.compat.Tuple;
import mucom88.common.MUCInfo;
import mucom88.common.MucException;
import musicDriverInterface.MmlDatum;
import vavi.util.Debug;


public class Msub {

    static final ResourceBundle rb = ResourceBundle.getBundle("lang/message");

    private Work work;
    private final MUCInfo mucInfo;
    public Muc88 muc88;

    public byte[] scores = {
            0, 0, 0, 0, 0, 0
    };

    // COMMANDs
    public static final byte[] FCOMS = new byte[] {
            0x6c,  // 'l' LIZM
            0x6f,  // 'o' octave
            0x44,  // 'D' DETUNE
            0x76,  // 'v' volume
            0x40,  // '@' SOUND COLOR
            0x3e,  // '>' octave UP
            0x3c,  // '<' octave DOWN
            0x29,  // ')' volume UP
            0x28,  // '(' volume DOWN
            0x26,  // '&' TIE
            0x79,  // 'y' REGISTER WRITE
            0x4d,  // 'M' MODURATION(LFO)
            0x72,  // 'r' REST
            0x5b,  // '[' LOOP START
            0x5d,  // ']' LOOP END
            0x53,  // 'S' SE DETUNE
            0x4c,  // 'L' JUMP RESTART ADR
            0x71,  // 'q' COMMAND OF 'q'
            0x45,  // 'E' SOFT ENV or Ch3SpMode
            0x50,  // 'P' MIX PORT
            0x77,  // 'w' NOIZE WAVE
            0x74,  // 't' TEMPO(DIRECT clock)
            0x43,  // 'C' SET clock
            0x21,  // '!' COMPILE END
            0x4b,  // 'K' KEY SHIFT
            0x2f,  // '/' REPEAT JUMP
            0x56,  // 'V' TOTAL volume OFFSET
            0x5c,  // '\' BEFORE CODE
//            0x73,  // 's' HARD ENVE SET
            0x6d,  // 'm' HARD ENVE PERIOD
            0x6b,  // 'k' KEY SHIFT 2
            0x73,  // 's' KEY ON REVISE
            0x25,  // '%' SET LIZM(DIRECT clock)
            0x70,  // 'p' STEREO PAN
            0x48,  // 'H' HARD LFO
            0x54,  // 'T' TEMPO
            0x4a,  // 'J' TAG SET & JUMP TO TAG
            0x3b,  // ';' ﾁｭｳﾔｸ ﾖｳ
            0x52,  // 'R' ﾘﾊﾞｰﾌﾞ
            0x2a,  // '*' MACRO
            0x3a,  // ':' RETURN
            0x5e,  // '^' &ﾄ ｵﾅｼﾞ
            0x7c,  // '|' ｼｮｳｾﾂ
            0x7d,  // '}' ﾏｸﾛｴﾝﾄﾞ
            0x7b,  // '{' ﾎﾟﾙﾀﾒﾝﾄｽﾀｰﾄ
            0x23,  // '#' FLAG SET
            0x5f,  // '_' 局地的ポルタメント
            0
    };

    public byte[] TONES = new byte[] {
            0x63, 0, // 'c'
            0x64, 2, // 'd'
            0x65, 4, // 'e'
            0x66, 5, // 'f'
            0x67, 7, // 'g'
            0x61, 9, // 'a'
            0x62, 11 // 'b'
    };

    public Msub(Work work, mucom88.common.MUCInfo mucInfo) {
        this.work = work;
        this.mucInfo = mucInfo;
    }

    public int readData(Tuple<Integer, String> lin, /*ref*/ int[] srcCPtr) {
        mucInfo.setErrSign(false);

        Arrays.fill(scores, (byte) 0);
        int digit = 5; // 5ｹﾀ ﾏﾃﾞ

        work.hexFg = 0;
        work.minUsf = 0;

//READ0: FIRST CHECK
        char ch;

        do {
            if (lin.getItem2().length() == srcCPtr[0]) {
                srcCPtr[0]++;
                mucInfo.setCarry(true); // NON DATA
                return 0;
            }
            ch = lin.getItem2().length() > srcCPtr[0] ? lin.getItem2().charAt(srcCPtr[0]) : (char) 0;
            srcCPtr[0]++;
        } while (ch == ' ' || ch == '\t');

        if (ch == '$') {
            work.hexFg = 1;
            srcCPtr[0]++;
//            goto READ7;
        } else if (ch == '-') {
            ch = lin.getItem2().length() > srcCPtr[0] ? lin.getItem2().charAt(srcCPtr[0]) : (char) 0;
            srcCPtr[0]++;
            if (ch < '0' || ch > '9') {
//                goto READE;//0ｲｼﾞｮｳ ﾉ ｷｬﾗｸﾀﾅﾗ ﾂｷﾞ
                work.setSecCom((byte) ch);
Debug.println(Level.FINE, "not valid number: " + ch);
                mucInfo.setCarry(true); // NON DATA
                return 0;
            }
            work.minUsf = 1;   // SET MINUS FLAG
//            goto READ7;
        } else {
            if (ch < '0' || ch > '9') {
//                goto READE;//0ｲｼﾞｮｳ ﾉ ｷｬﾗｸﾀﾅﾗ ﾂｷﾞ
Debug.println(Level.FINE, "not valid number: " + ch);
                work.setSecCom((byte) ch);
                mucInfo.setCarry(true); // NON DATA
                return 0;
            }
//            goto READ7;
        }
//READ7:
        srcCPtr[0]--;
READ1: {
        do {
            ch = lin.getItem2().length() > srcCPtr[0] ? lin.getItem2().charAt(srcCPtr[0]) : (char) 0;
READF: {
            //z80.A = mem.ld_8(Z80.HL); // SECOND CHECK
            if (work.hexFg != 0) { // goto READC;

                if (ch >= 'a' && ch <= 'f') {
                    ch -= (char) 32;
                }
                //READG:
                if (ch >= 'A' && ch <= 'F') {
                    ch -= (char) 7;
                    break READF;
                }
            }
//READC:
            if (ch < '0' || ch > '9') {
//                goto READ1;//9ｲｶﾅﾗ ﾂｷﾞ
                break READ1;
            }
/*READF:*/}
            scores[0] = scores[1];
            scores[1] = scores[2];
            scores[2] = scores[3];
            scores[3] = scores[4];
            scores[4] = scores[5];

            ch -= (char) 0x30; // A= 0 - 9
            scores[4] = (byte) ch;
            srcCPtr[0]++; // NEXT TEXT
            digit--;

            if (lin.getItem2().length() == srcCPtr[0]) {
//                        goto READ1;
                break READ1;
            }

        } while (digit > 0);

        ch = lin.getItem2().length() > srcCPtr[0] ? lin.getItem2().charAt(srcCPtr[0]) : (char) 0; // THIRD CHECK
        if (ch >= '0' && ch <= '9') { // goto READ1; // 9ｲｶﾅﾗ ﾂｷﾞ
//READ8:
            mucInfo.setCarry(false);
            mucInfo.setErrSign(true); // ERROR SIGN
Debug.println(Level.FINE, "over 7 digits");
            return 0; // RET; 7ｹﾀｲｼﾞｮｳ ﾊ ｴﾗｰ
        }
/*READ1:*/}
        int a = 0;
        if (work.hexFg == 1) {
            for (int i = 1; i < 5; i++) {
                a *= 16;
                a += scores[i];
            }
//                    goto READA;
        } else {
//READD:
            for (int i = 0; i < 5; i++) {
                a *= 10;
                a += scores[i];
            }

            if (work.minUsf != 0) { // CHECK MINUS FLAG
                a = -a;
            }
        }
//READA:
        mucInfo.setCarry(false);
        return a; // RET
//READE:
//        work.setSecCom((byte) ch);
//        mucInfo.setCarry(true); // NON DATA
//        return 0;
    }

    public boolean MCMP_DE(String strDE, Tuple<Integer, String> lin, /*ref*/ int[] srcCPtr) {
        try {
            String trgDE = strDE.substring(0, strDE.indexOf("\0"));
            if (trgDE.length() < 1) return false;

            byte[] bHL = new byte[trgDE.length()];
            for (int i = 0; i < trgDE.length(); i++) {
                bHL[i] = (byte) (lin.getItem2().length() > srcCPtr[0] ? lin.getItem2().charAt(srcCPtr[0]) : 0);
                srcCPtr[0]++;
            }
            String trgHL = new String(bHL);
            if (trgHL.equals(trgDE)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void MWRIT2(MmlDatum dat) {
        //Debug.printf("%2x", dat.dat);
        mucInfo.getBufDst().set(work.mData++, dat);

        if (work.mData - work.getBufStartPtr() > 0xffff) {
            throw new MucException(rb.getString("E0200"), mucInfo.getRow(), mucInfo.getCol());
        }

        muc88.DispHex4(work.mData, 36);
    }

    public void MWRITE(MmlDatum cmdNo, MmlDatum cmdDat) {
        //Debug.printf("%2x", cmdNo);
        mucInfo.getBufDst().set(work.mData++, cmdNo);
        //Debug.printf("%2x", cmdDat);
        mucInfo.getBufDst().set(work.mData++, cmdDat);

        if (work.mData - work.getBufStartPtr() > 0xffff) {
            throw new MucException(rb.getString("E0200"), mucInfo.getRow(), mucInfo.getCol());
        }

        muc88.DispHex4(work.mData, 36);
    }

    public int ERRT(Tuple<Integer, String> lin, /*ref*/ int[] ptr, String cmdMsg) {
        ptr[0]++;
        int n = readData(lin, /*ref*/ ptr);
        if (mucInfo.getCarry()) { // 数値読み取れなかった
            throw new MucException(String.format(rb.getString("E0201"), cmdMsg), mucInfo.getRow(), mucInfo.getCol());
        } else {
            if (mucInfo.getErrSign()) {
                //ERRORIF();
                return -1;
            }
        }

        return n;
    }

    public int FMCOMC(char c) {
        for (int i = 0; i < FCOMS.length; i++) {
            if (FCOMS[i] == 0) {
                break;
            }
            if (FCOMS[i] == c) {
                //Debug.printf("%d", c);
                return i + 1;
            }
        }
        //Debug.printf("%d!", c);
        return 0;
    }

    /**
     * @after error: {@link MUCInfo#getCarry()} true
     * @return 0: error
     */
    public byte STTONE() {
        char c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;

        Debug.printf(Level.FINEST, String.valueOf(c));

        for (int i = 0; i < 7; i++) {
            if (c == TONES[i * 2]) {
                mucInfo.setCarry(false);
                return toNext((byte) i);
            }
        }

Debug.printf(Level.FINE, "error: %d not in %s", (int) c, Arrays.toString(TONES));
        mucInfo.setCarry(true);
        return 0;
    }

    private byte toNext(byte n) {
        n = TONES[n * 2 + 1];
        int o = work.octave;

        mucInfo.incAndGetSrcCPtr();
        char c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;

        if (c == '+') {
            if (n == 11) { // KEY='b'?
                n = (byte) 0xff;
                o++;
                if (o == 8) {
                    o = 7;
                }
            }
            n++;
        } else if (c == '-') {
            if (n == 0) {
                n = 12;
                o--;
                if (o < 0) {
                    o = 0;
                }
            }
            n--;
        } else {
            mucInfo.decSrcCPtr();
        }

        siftKey(/*ref*/ o, /*ref*/ n);
        mucInfo.setCarry(false);
        return (byte) (((o & 0xf) << 4) | (n & 0xf));
    }

    public void siftKey(/*ref*/ int oct,/*ref*/ byte n) {
        int shift = (byte) work.siftDat + (byte) work.siftDa2;
        if (shift == 0) return;

        //mucInfo.Carry = (oct * 12 + n > 0xff);
        n = (byte) (oct * 12 + n);

        oct = (n + shift) / 12;
        n = (byte) ((n + shift) % 12);
    }

    /**
     * 音長のよみとり
     * @return
     * 0...normal
     * -1...WARNING
     */
    public int STLIZM(Tuple<Integer, String> lin,/*ref*/ int[] ptr,/*out*/ byte[] clk) {
        char c = ptr[0] < lin.getItem2().length()
                ? lin.getItem2().charAt(ptr[0])
                : (char) 0;
        int n;
        clk[0] = 0;

        if (c == '%') {
            ptr[0]++;
            n = readData(lin, /*ref*/ ptr);
            if (mucInfo.getCarry()) { // 数値読み取れなかった
                ptr[0]--;
                throw new MucException(rb.getString("E0499"), lin.getItem1(), ptr[0]);
            }
            if (mucInfo.getErrSign()) {
                throw new MucException(rb.getString("E0500"), lin.getItem1(), ptr[0]);
            }

            clk[0] = (byte) n;
            if (n < 0 || n > 255) {
                return -1;
            }
            return 0;
        }

        int w = 0;
        n = readData(lin, /*ref*/ ptr);
        if (n < 0 || n > 255) {
            w = -1;
        }
        n = (byte) n;

        if (mucInfo.getCarry()) // 数値読み取れなかった
        {
            ptr[0]--;
            n = work.count;
        } else {
            if (mucInfo.getErrSign()) {
                throw new MucException(rb.getString("E0501"), lin.getItem1(), ptr[0]);
            }
            if (work.clock < n) { // clock以上の細かい音符は指定できないようにしている
                throw new MucException(String.format(rb.getString("E0502"), n), lin.getItem1(), ptr[0]);
                // clock<E ﾃﾞ ERROR
            }

            n = work.clock / n; // clockに変換
        }

        int a = n;
        do {
            c = ptr[0] < lin.getItem2().length()
                    ? lin.getItem2().charAt(ptr[0])
                    : (char) 0;
            if (c != '.') break;
            ptr[0]++;
            a /= 2;
            n += a;
        } while (true);

        if (n > 255) {
            throw new MucException(String.format(rb.getString("E0503"), n), lin.getItem1(), ptr[0]);
        }

        clk[0] = (byte) n;
        return w;
    }

    public void ERRSN() {
        throw new UnsupportedOperationException();
    }
}

