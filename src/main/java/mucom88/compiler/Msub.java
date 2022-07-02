package mucom88.compiler;

import dotnet4j.Tuple;
import mdsound.Log;
import mdsound.LogLevel;
import mucom88.common.MUCInfo;
import mucom88.common.Message;
import mucom88.common.MucException;
import mucom88.common.iEncoding;
import musicDriverInterface.MmlDatum;


public class Msub {
    private Work work;
    private final MUCInfo mucInfo;
    public Muc88 muc88;
    private iEncoding enc;

    public byte[] SCORE = {
            0, 0, 0, 0, 0, 0
    };

    // COMMANDs
    public static final byte[] FCOMS = new byte[] {
            0x6c // 'l'LIZM
            , 0x6f //,'o'OCTAVE
            , 0x44 //,'D'DETUNE
            , 0x76 //,'v'VOLUME
            , 0x40 //,'@'SOUND COLOR
            , 0x3e //,'>'OCTAVE UP
            , 0x3c //,'<'OCTAVE DOWN
            , 0x29 //,')'VOLUME UP
            , 0x28 //,'('VOLUME DOWN
            , 0x26 //,'&'TIE
            , 0x79 //,'y'REGISTER WRITE
            , 0x4d //,'M'MODURATION(LFO)
            , 0x72 //,'r'REST
            , 0x5b //,'['LOOP START
            , 0x5d //,']'LOOP END
            , 0x53 //,'S'SE DETUNE
            , 0x4c //,'L'JUMP RESTART ADR
            , 0x71 //,'q'COMMAND OF 'q'
            , 0x45 //,'E'SOFT ENV or Ch3SpMode
            , 0x50 //,'P'MIX PORT
            , 0x77 //,'w'NOIZE WAVE
            , 0x74 //,'t'TEMPO(DIRECT CLOCK)
            , 0x43 //,'C'SET CLOCK
            , 0x21 //,'!'COMPILE END
            , 0x4b //,'K'KEY SHIFT
            , 0x2f //,'/'REPEAT JUMP
            , 0x56 //,'V'TOTAL VOLUME OFFSET
            , 0x5c //,'\'    BEFORE CODE
            //,0x73 //,'s'HARD ENVE SET
            , 0x6d //,'m'HARD ENVE PERIOD
            , 0x6b //,'k'KEY SHIFT 2
            , 0x73 //,'s'KEY ON REVISE
            , 0x25 //,'%'SET LIZM(DIRECT CLOCK)
            , 0x70 //,'p'STEREO PAN
            , 0x48 //,'H'HARD LFO
            , 0x54 //,'T'TEMPO
            , 0x4a //,'J'TAG SET & JUMP TO TAG
            , 0x3b //,';'ﾁｭｳﾔｸ ﾖｳ
            , 0x52 //,'R'ﾘﾊﾞｰﾌﾞ
            , 0x2a //,'*'MACRO
            , 0x3a //,':'RETURN
            , 0x5e //,'^'&ﾄ ｵﾅｼﾞ
            , 0x7c //,'|'ｼｮｳｾﾂ
            , 0x7d //,'}'ﾏｸﾛｴﾝﾄﾞ
            , 0x7b //,'{'ﾎﾟﾙﾀﾒﾝﾄｽﾀｰﾄ
            , 0x23 //,'#'FLAG SET
            , 0x5f //,'_'局地的ポルタメント
            , 0
    };

    public byte[] TONES = new byte[] {
            0x63, 0 // 'c' ,0
            , 0x64, 2 // 'd' ,2
            , 0x65, 4 // 'e' ,4
            , 0x66, 5 // 'f' ,5
            , 0x67, 7 // 'g' ,7
            , 0x61, 9 // 'a' ,9
            , 0x62, 11 // 'b' ,11
    };

    public Msub(Work work, mucom88.common.MUCInfo mucInfo, iEncoding enc) {
        this.work = work;
        this.mucInfo = mucInfo;
        this.enc = enc;
    }

    public int REDATA(Tuple<Integer, String> lin, /*ref*/ int[] srcCPtr) {
        mucInfo.setErrSign(false);

        for (int i = 0; i < SCORE.length; i++) SCORE[i] = 0;
        int degit = 5;   // 5ｹﾀ ﾏﾃﾞ

        work.HEXFG = 0;
        work.MINUSF = 0;

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

        if (ch == '$') { //0x24
            work.HEXFG = 1;
            srcCPtr[0]++;
//            goto READ7;
        } else if (ch == '-') { //0x2d
            ch = lin.getItem2().length() > srcCPtr[0] ? lin.getItem2().charAt(srcCPtr[0]) : (char) 0;
            srcCPtr[0]++;
            if (ch < '0' || ch > '9') { //0x30 0x39
//                goto READE;//0ｲｼﾞｮｳ ﾉ ｷｬﾗｸﾀﾅﾗ ﾂｷﾞ
                work.setSECCOM((byte) ch);
                mucInfo.setCarry(true); // NON DATA
                return 0;
            }
            work.MINUSF = 1;   // SET MINUS FLAG
//            goto READ7;
        } else {
            if (ch < '0' || ch > '9') { //0x30 0x39
//                goto READE;//0ｲｼﾞｮｳ ﾉ ｷｬﾗｸﾀﾅﾗ ﾂｷﾞ
                work.setSECCOM((byte) ch);
                mucInfo.setCarry(true); // NON DATA
                return 0;
            }
//            goto READ7;
        }
//READ7:
        srcCPtr[0]--;
        boolean READ1 = false;
        do {
            ch = lin.getItem2().length() > srcCPtr[0] ? lin.getItem2().charAt(srcCPtr[0]) : (char) 0;
            //Z80.A = Mem.LD_8(Z80.HL); // SECOND CHECK
            boolean READF = false;
            if (work.HEXFG != 0) { // goto READC;

                if (ch >= 'a' && ch <= 'f') {
                    ch -= (char) 32;
                }
                //READG:
                if (ch >= 'A' && ch <= 'F') {
                    ch -= (char) 7;
//                        goto READF;
                    READF = true;
                }
            }
//READC:
            if (!READF) {
                if (ch < '0' || ch > '9') {
//                    goto READ1;//9ｲｶﾅﾗ ﾂｷﾞ
                    READ1 = true;
                    break;
                }
            }
//READF:

            SCORE[0] = SCORE[1];
            SCORE[1] = SCORE[2];
            SCORE[2] = SCORE[3];
            SCORE[3] = SCORE[4];
            SCORE[4] = SCORE[5];

            ch -= (char) 0x30;// A= 0 - 9
            SCORE[4] = (byte) ch;
            srcCPtr[0]++; // NEXT TEXT
            degit--;

            if (lin.getItem2().length() == srcCPtr[0]) {
//                goto READ1;
                READ1 = true;
                break;
            }

        } while (degit > 0);

        if (!READ1) {
            ch = lin.getItem2().length() > srcCPtr[0] ? lin.getItem2().charAt(srcCPtr[0]) : (char) 0; // THIRD CHECK
            if (ch >= '0' && ch <= '9') { // goto READ1; // 9ｲｶﾅﾗ ﾂｷﾞ
//READ8:
                mucInfo.setCarry(false);
                mucInfo.setErrSign(true); // ERROR SIGN
                return 0; //RET; 7ｹﾀｲｼﾞｮｳ ﾊ ｴﾗｰ
            }
        }
//READ1:
        int a = 0;
        if (work.HEXFG == 1) {
            for (int i = 1; i < 5; i++) {
                a *= 16;
                a += SCORE[i];
            }
//            goto READA;
        } else {
            //READD:
            for (int i = 0; i < 5; i++) {
                a *= 10;
                a += SCORE[i];
            }

            if (work.MINUSF != 0) {// CHECK MINUS FLAG
                a = -a;
            }
        }
//READA:
        mucInfo.setCarry(false);
        return a;//    RET
//READE:
//        work.setSECCOM((byte) ch);
//        mucInfo.setCarry(true); // NON DATA
//        return 0;
    }

    public boolean MCMP_DE(String strDE, Tuple<Integer, String> lin, /*ref*/ int srcCPtr) {
        try {
            String trgDE = strDE.substring(0, strDE.indexOf("\0"));
            if (trgDE.length() < 1) return false;

            byte[] bHL = new byte[trgDE.length()];
            for (int i = 0; i < trgDE.length(); i++) {
                bHL[i] = (byte) (lin.getItem2().length() > srcCPtr ? lin.getItem2().charAt(srcCPtr) : 0);
                srcCPtr++;
            }
            String trgHL = enc.GetStringFromUtfArray(bHL);// Encoding.UTF8.GetString(bHL);
            if (trgHL.equals(trgDE)) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public void MWRIT2(MmlDatum dat) {
        //Console.WriteLine("{0:x2}", dat.dat);
        mucInfo.getbufDst().set(work.MDATA++, dat);

        if (work.MDATA - work.getbufStartPtr() > 0xffff) {
            throw new MucException(
                    Message.get("E0200")
                    , mucInfo.getrow(), mucInfo.getcol());
        }

        muc88.DispHex4(work.MDATA, 36);
    }

    public void MWRITE(MmlDatum cmdNo, MmlDatum cmdDat) {
        //Common.WriteLine("{0:x2}", cmdNo);
        mucInfo.getbufDst().set(work.MDATA++, cmdNo);
        //Common.WriteLine("{0:x2}", cmdDat);
        mucInfo.getbufDst().set(work.MDATA++, cmdDat);

        if (work.MDATA - work.getbufStartPtr() > 0xffff) {
            throw new MucException(
                    Message.get("E0200")
                    , mucInfo.getrow(), mucInfo.getcol());
        }

        muc88.DispHex4(work.MDATA, 36);
    }

    public int ERRT(Tuple<Integer, String> lin, /*ref*/ int[] ptr, String cmdMsg) {
        ptr[0]++;
        int n = REDATA(lin, /*ref*/ ptr);
        if (mucInfo.getCarry())//数値読み取れなかった
        {
            throw new MucException(
                    String.format(Message.get("E0201"), cmdMsg)
                    , mucInfo.getrow(), mucInfo.getcol());
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
                //Common.WriteLine("{0}", c);
                return i + 1;
            }
        }
        //Common.WriteLine("{0}!", c);
        return 0;
    }

    public byte STTONE() {
        char c = mucInfo.getSrcCPtr() < mucInfo.getlin().getItem2().length()
                ? mucInfo.getlin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;

        Log.writeLine(LogLevel.TRACE, String.valueOf(c));

        for (int i = 0; i < 7; i++) {
            if (c == TONES[i * 2]) {
                mucInfo.setCarry(false);
                return TONEXT((byte) i);
            }
        }

        mucInfo.setCarry(true);
        return 0;
    }

    private byte TONEXT(byte n) {
        n = TONES[n * 2 + 1];
        int o = work.OCTAVE;

        mucInfo.incAndGetSrcCPtr();
        char c = mucInfo.getSrcCPtr() < mucInfo.getlin().getItem2().length()
                ? mucInfo.getlin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;

        if (c == '+') {
            if (n == 11)// KEY='b'?
            {
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
            ;
        }

        KEYSIFT(/*ref*/ o, /*ref*/ n);
        mucInfo.setCarry(false);
        return (byte) (((o & 0xf) << 4) | (n & 0xf));
    }

    public void KEYSIFT(/*ref*/ int oct,/*ref*/ byte n) {
        int shift = (byte) work.SIFTDAT + (byte) work.SIFTDA2;
        if (shift == 0) return;

        //mucInfo.Carry = (oct * 12 + n > 0xff);
        n = (byte) (oct * 12 + n);

        oct = (n + shift) / 12;
        n = (byte) ((n + shift) % 12);
    }

    /**
     * 音長のよみとり
     * <returns>
     * 0...normal
     * -1...WARNING
     * </returns>
     */
    public int STLIZM(Tuple<Integer, String> lin,/*ref*/ int[] ptr,/*out*/ byte[] clk) {
        char c = ptr[0] < lin.getItem2().length()
                ? lin.getItem2().charAt(ptr[0])
                : (char) 0;
        int n;
        clk[0] = 0;

        if (c == '%') {
            ptr[0]++;
            n = REDATA(lin, /*ref*/ ptr);
            if (mucInfo.getCarry())//数値読み取れなかった
            {
                ptr[0]--;
                throw new MucException(Message.get("E0499"), lin.getItem1(), ptr[0]);//ERRORSN
            }
            if (mucInfo.getErrSign()) {
                throw new MucException(Message.get("E0500"), lin.getItem1(), ptr[0]);//ERRORIF
            }

            clk[0] = (byte) n;
            if (n < 0 || n > 255) {
                return -1;
            }
            return 0;
        }

        int w = 0;
        n = REDATA(lin, /*ref*/ ptr);
        if (n < 0 || n > 255) {
            w = -1;
        }
        n = (byte) n;

        if (mucInfo.getCarry())//数値読み取れなかった
        {
            ptr[0]--;
            n = work.COUNT;
        } else {
            if (mucInfo.getErrSign()) {
                throw new MucException(Message.get("E0501"), lin.getItem1(), ptr[0]);//ERRORSN
            }
            if (work.CLOCK < n)//clock以上の細かい音符は指定できないようにしている
            {
                throw new MucException(String.format(Message.get("E0502"), n), lin.getItem1(), ptr[0]);//ERRORIF
                //CLOCK<E ﾃﾞ ERROR
            }

            n = work.CLOCK / n;//clockに変換
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
            throw new MucException(String.format(Message.get("E0503"), n), lin.getItem1(), ptr[0]);//ERROROF
        }

        clk[0] = (byte) n;
        return w;
    }

    public void ERRSN() {
        throw new UnsupportedOperationException();
    }
}

