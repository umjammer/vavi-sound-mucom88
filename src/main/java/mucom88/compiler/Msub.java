package mucom88.compiler;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.ResourceBundle;

import mucom88.common.MUCInfo;
import mucom88.common.MucException;
import musicDriverInterface.MmlDatum;
import vavi.util.compat.Tuple;

import static java.lang.System.getLogger;


public class Msub {

    private static final Logger logger = getLogger(Msub.class.getName());

    private static final ResourceBundle rb = ResourceBundle.getBundle("mucom88/message");

    private final Work work;
    private final MUCInfo mucInfo;
    public Muc88 muc88;

    private final int[] scores = {
            0, 0, 0, 0, 0, 0
    };

    // COMMANDs
    private static final int[] FCOMS = {
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
            0x3b,  // ';' for comment
            0x52,  // 'R' Reverb
            0x2a,  // '*' MACRO
            0x3a,  // ':' RETURN
            0x5e,  // '^' same as the &
            0x7c,  // '|' measure
            0x7d,  // '}' macro end
            0x7b,  // '{' Portamento Start
            0x23,  // '#' FLAG SET
            0x5f,  // '_' Local portamento
//            (byte) '~', // Reverse TIE
            (byte) '\'', // Memo
            0
    };

    private final int[] TONES = {
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

    /** REDATA */
    public int readData(Tuple<Integer, String> lin, /* ref */ int[] srcCPtr) {
        mucInfo.setErrSign(false);

        Arrays.fill(scores, 0);
        int digit = 5; // Up to 5 digits

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
            ch = lin.getItem2().length() > srcCPtr[0] ? lin.getItem2().charAt(srcCPtr[0]) : 0;
            srcCPtr[0]++;
        } while (ch == ' ' || ch == '\t');

        if (ch == '$') { // 0x24
            work.hexFg = 1;
            srcCPtr[0]++;
//            goto READ7;
        } else if (ch == '-') { // 0x2d
            ch = lin.getItem2().length() > srcCPtr[0] ? lin.getItem2().charAt(srcCPtr[0]) : 0;
            srcCPtr[0]++;
            if (ch < '0' || ch > '9') { // 0x30 0x39
//                goto READE; // If 0 or more characters, next
                work.setSecCom(ch);
logger.log(Level.DEBUG, "not valid number: " + ch);
                mucInfo.setCarry(true); // NON DATA
                return 0;
            }
            work.minUsf = 1;   // SET MINUS FLAG
//            goto READ7;
        } else {
            if (ch < '0' || ch > '9') { // 0x30 0x39
//                goto READE; // If 0 or more characters, next
logger.log(Level.DEBUG, "not valid number: " + ch + ", \"" + lin.getItem2() + "\", " + srcCPtr[0]);
                work.setSecCom(ch);
                mucInfo.setCarry(true); // NON DATA
                return 0;
            }
//            goto READ7;
        }
//READ7:
        srcCPtr[0]--;
READ1: {
        do {
            ch = lin.getItem2().length() > srcCPtr[0] ? lin.getItem2().charAt(srcCPtr[0]) : 0;
READF: {
            //z80.A = mem.ld_8(Z80.HL); // SECOND CHECK
            if (work.hexFg != 0) { // goto READC;

                if (ch >= 'a' && ch <= 'f') {
                    ch -= 32;
                }
                //READG:
                if (ch >= 'A' && ch <= 'F') {
                    ch -= 7;
                    break READF;
                }
            }
//READC:
            if (ch < '0' || ch > '9') {
//                goto READ1; // If 9 or less, next
                break READ1;
            }
/*READF:*/}
            scores[0] = scores[1];
            scores[1] = scores[2];
            scores[2] = scores[3];
            scores[3] = scores[4];
            scores[4] = scores[5];

            ch -= 0x30; // A= 0 - 9
            scores[4] = ch & 0xff;
            srcCPtr[0]++; // NEXT TEXT
            digit--;

            if (lin.getItem2().length() == srcCPtr[0]) {
//                        goto READ1;
                break READ1;
            }

        } while (digit > 0);

        ch = lin.getItem2().length() > srcCPtr[0] ? lin.getItem2().charAt(srcCPtr[0]) : 0; // THIRD CHECK
        if (ch >= '0' && ch <= '9') { // goto READ1; // If 9 or less, next
//READ8:
            mucInfo.setCarry(false);
            mucInfo.setErrSign(true); // ERROR SIGN
logger.log(Level.DEBUG, "over 7 digits");
            return 0; // RET; Anything over 7 digits is an error
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

    public boolean MCMP_DE(String strDE, Tuple<Integer, String> lin, /* ref */ int[] srcCPtr) {
        try {
            String trgDE = strDE.substring(0, strDE.indexOf("\0"));
            if (trgDE.isEmpty()) return false;

            byte[] bHL = new byte[trgDE.length()];
            for (int i = 0; i < trgDE.length(); i++) {
                bHL[i] = (byte) (lin.getItem2().length() > srcCPtr[0] ? lin.getItem2().charAt(srcCPtr[0]) & 0xff : 0);
                srcCPtr[0]++;
            }
            String trgHL = new String(bHL);
            if (trgHL.equals(trgDE)) {
                return true;
            }
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
        return false;
    }

    public void MWRIT2(MmlDatum dat) {
        //logger.log(Level.TRACE, "%2x".formatted(dat.dat));
        mucInfo.getBufDst().set(work.mData++, dat);

        if (work.mData - work.getBufStartPtr() > 0xffff) {
            throw new MucException(rb.getString("E0200"), mucInfo.getRow(), mucInfo.getCol());
        }

        muc88.DispHex4(work.mData, 36);
    }

    public void MWRITE(MmlDatum cmdNo, MmlDatum cmdDat) {
        //logger.log(Level.TRACE, "%2x".formatted(cmdNo));
        mucInfo.getBufDst().set(work.mData++, cmdNo);
        //logger.log(Level.TRACE, "%2x".formatted(cmdDat));
        mucInfo.getBufDst().set(work.mData++, cmdDat);

        if (work.mData - work.getBufStartPtr() > 0xffff) {
            throw new MucException(rb.getString("E0200"), mucInfo.getRow(), mucInfo.getCol());
        }

        muc88.DispHex4(work.mData, 36);
    }

    public void MWRITE(MmlDatum cmdNo, MmlDatum... cmdDats) {
        mucInfo.getBufDst().set(work.mData++, cmdNo);

        for (MmlDatum d : cmdDats) {
            mucInfo.getBufDst().set(work.mData++, d);
            if (work.mData - work.getBufStartPtr() > 0xffff) {
                throw new MucException(rb.getString("E0200"), mucInfo.getRow(), mucInfo.getCol());
            }
        }

        muc88.DispHex4(work.mData, 36);
    }

    public int ERRT(Tuple<Integer, String> lin, /* ref */ int[] ptr, String cmdMsg) {
        ptr[0]++;
        int n = readData(lin, /* ref */ ptr);
        if (mucInfo.getCarry()) { // Could not read the value
logger.log(Level.DEBUG, lin.getItem2());
            throw new MucException(rb.getString("E0201").formatted(cmdMsg), mucInfo.getRow(), mucInfo.getCol());
        } else {
            if (mucInfo.getErrSign()) {
                //ERRORIF();
                return -1;
            }
        }

        return n;
    }

    public int FMCOMC(int c) {
        for (int i = 0; i < FCOMS.length; i++) {
            if (FCOMS[i] == 0) {
                break;
            }
            if (FCOMS[i] == c) {
//logger.log(Level.TRACE, "%d".formatted(c));
                return i + 1;
            }
        }
//logger.log(Level.TRACE, "%d!".formatted(c));
        return 0;
    }

    private int oldNote = 0;

    /**
     * @after error: {@link MUCInfo#getCarry()} true
     * @return 0: error
     */
    public int STTONE() {
        char c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : 0;

        logger.log(Level.TRACE, String.valueOf(c));

        for (int[] i = new int[1]; i[0] < 7; i[0]++) {
            if (c == TONES[i[0] * 2]) {
                mucInfo.setCarry(false);
                return toNext(i);
            }
        }

        if (c == 'x') {
            mucInfo.setCarry(false);
            return TONEXT2();
        }

logger.log(Level.DEBUG, "error: %c not in %s".formatted(c, Arrays.toString(TONES)));
        mucInfo.setCarry(true);
        return 0;
    }

    private int toNext(int[] n) {
        n[0] = TONES[n[0] * 2 + 1];
        int[] o = {work.octave};

        mucInfo.incAndGetSrcCPtr();
        char c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;

        if (c == '+') {
            if (n[0] == 11) { // KEY='b'?
                n[0] = 0xff;
                o[0]++;
                if (o[0] == 8) {
                    o[0] = 7;
                }
            }
            n[0]++;
        } else if (c == '-') {
            if (n[0] == 0) {
                n[0] = 12;
                o[0]--;
                if (o[0] < 0) {
                    o[0] = 0;
                }
            }
            n[0]--;
        } else {
            mucInfo.getAndDecSrcCPtr();
        }

        siftKey(/* ref */ o, /* ref */ n);
        mucInfo.setCarry(false);
        return (((o[0] & 0xf) << 4) | (n[0] & 0xf)) & 0xff;
    }

    private byte TONEXT2() {
        int[] n = {oldNote};
        int[] o = {work.octave};

        siftKey(/* ref */ o, /* ref */ n);
        mucInfo.setCarry(false);
        return (byte) (((o[0] & 0xf) << 4) | (n[0] & 0xf));
    }

    /** KEYSIFT */
    private void siftKey(/* ref */ int[] oct, /* ref */ int[] n) {
        int shift = (work.siftDat & 0xff) + (work.siftDa2 & 0xff);
        if (shift == 0) return;

        //mucInfo.Carry = (oct * 12 + n > 0xff);
        n[0] = oct[0] * 12 + n[0];

        oct[0] = (n[0] + shift) / 12;
        n[0] = (n[0] + shift) % 12;
    }

    /**
     * Reading note length
     * @return
     * 0...normal
     * -1...WARNING
     */
    public int STLIZM(Tuple<Integer, String> lin, /* ref */ int[] ptr, /* out */ int[] clk) {
        char c = ptr[0] < lin.getItem2().length()
                ? lin.getItem2().charAt(ptr[0])
                : (char) 0;
        int n;
        clk[0] = 0;

        if (c == '%') {
            ptr[0]++;
            n = readData(lin, /* ref */ ptr);
            if (mucInfo.getCarry()) { // Could not read the value
                ptr[0]--;
                throw new MucException(rb.getString("E0499"), lin.getItem1(), ptr[0]);
            }
            if (mucInfo.getErrSign()) {
                throw new MucException(rb.getString("E0500"), lin.getItem1(), ptr[0]);
            }

            clk[0] = n & 0xff;
            if (n < 0 || n > 255) {
                return -1;
            }
            return 0;
        }

        int w = 0;
        n = readData(lin, /* ref */ ptr);
        if (n < 0 || n > 255) {
            w = -1;
        }
        n = n & 0xff;

        if (mucInfo.getCarry()) { // Could not read the value
            ptr[0]--;
            n = work.count;
        } else {
            if (mucInfo.getErrSign()) {
                throw new MucException(rb.getString("E0501"), lin.getItem1(), ptr[0]);
            }
            if (work.clock < n) { // It is not possible to specify notes smaller than the clock.
                throw new MucException(rb.getString("E0502").formatted(n), lin.getItem1(), ptr[0]);
                // clock<E then ERROR
            }

            n = work.clock / n; // Convert to clock
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
            throw new MucException(rb.getString("E0503").formatted(n), lin.getItem1(), ptr[0]);
        }

        clk[0] = n;
        return w;
    }

    public void ERRSN() {
        throw new UnsupportedOperationException();
    }
}

