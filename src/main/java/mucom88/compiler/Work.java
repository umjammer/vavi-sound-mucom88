package mucom88.compiler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import musicDriverInterface.CompilerInfo;


public class Work {

    public static final int MAXChips = 5;
    public static final int MAXCH = 11; // OPMは8ですけどね
    public static final int MAXPG = 10;

    //使用しない！
    //public final int T_CLK = 0x8C10;
    //public final int BEFMD = T_CLK + 4 * 11 + 1;//+1ｱﾏﾘ
    //public final int PTMFG = BEFMD + 2;
    //public final int PTMDLY = PTMFG + 1;
    //public final int TONEADR = PTMDLY + 2;
    //public final int SPACE = TONEADR + 2;//2*6BYTE ｱｷ ｶﾞ ｱﾙ
    //public final int DEFVOICE = SPACE + 2 * 6;
    //public final int DEFVSSG = DEFVOICE + 32;
    //public final int JCLOCK = DEFVSSG + 32;
    //public final int JPLINE = JCLOCK + 2;
    //
    public int FMLIB = 0;// 0x6000 w

    public int[][][] tcnt;// MAXCH]; //0x8c10 w
    public int[][][] lcnt;// MAXCH]; //0x8c12 w
    public int[][][] loopPoint;// MAXCH]; //0x8c12 w

    public int pcmFlag = 0;//0x8c10+10*4 w
    public int JCLOCK = 0;//0x8c90 w
    public int LOOPSP = 0;//0xf260 w ﾙｰﾌﾟｽﾀｯｸ
    public int MDATA = 0;// 0xf320 w
    public int DATTBL = 0;// 0xf324 w
    public int OCTAVE = 0;// 0xf326 b
    public int SIFTDAT = 0;// 0xf327 b
    public int SIFTDA2 = 0;// 0xf327 b
    public int CLOCK = 0;// 0xf328 b
    public int ERRLINE = 0;//0xf32e w
    //public int COMNOW = 0;// 0xf330 b
    public int COUNT = 0;// 0xf331 b
    public int VOLINT = 0;// 0xfxxx b
    public int ESCAPE = 0;//
    public int MINUSF = 0;//
    public int BEFRST = 0;// 0xfxxx b
    public int TIEFG = 0;// 0xfxxx b
    public int[] OTONUM = new int[MAXChips];// 0xfxxx b
    public int VOLUME = 0;// 0xfxxx b
    public int ENDADR = 0;// 0xfxxx w
    public int OCTINT = 0;// 0xfxxx w
    public int VICADR = 0;// 0xE300 w
    public String titleFmt = "[  MUCOM88 Ver:0.0  ]  Address:0000-0000(0000)         [ 00:00 ] MODE:NORMAL  "; // 0xf3c8 b
    public String title = "[  MUCOM88 Ver:0.0  ]  Address:0000-0000(0000)         [ 00:00 ] MODE:NORMAL  "; // 0xf3c8 b
    //public byte fmvoiceCnt = 0;//0xf320+50
    public byte[] LFODAT = new byte[] {1, 0, 0, 0, 0, 0, 0};
    public byte LINCFG = 0;
    public int ADRSTC = 0;
    public byte VPCO = 1;//dummyで1としている
    public byte OctaveUDFLG = 0;
    public byte VolumeUDFLG = 0;
    public boolean pcmInvert = false;
    public int REPCOUNT = 0;
    public int TV_OFS = 0;
    public int POINTC = 0;// LOOPSTART ADR ｶﾞ ｾｯﾃｲｻﾚﾃｲﾙ ADR
    public byte MACFG = 0;//0>< AS MACRO PRC

    public int TST2_VAL = 0xc000;

    public int HEXFG = 0;

    private byte SECCOM;

    public byte getSECCOM() {
        return SECCOM;
    }

    public void setSECCOM(byte value) {
        SECCOM = value;
    }

    private byte[] BEFTONE = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0};

    public byte[] getBEFTONE() {
        return BEFTONE;
    }

    private int BEFCO;

    public int getBEFCO() {
        return BEFCO;
    }
    public void setBEFCO(int value) {
        BEFCO = value;
    }

    private int com;

    public int getcom() {
        return com;
    }
    public void setcom(int value) {
        com = value;
    }

    private byte BFDAT;

    public byte getBFDAT() {
        return BFDAT;
    }
    public void setBFDAT(byte value) {
        BFDAT = value;
    }

    private byte VDDAT;

    public byte getVDDAT() {
        return VDDAT;
    }
    public void setVDDAT(byte value){
         VDDAT = value;
    }

    private int LINE;

    public int getLINE() {
        return LINE;
    }

    private int JPLINE = -1;

    public int getJPLINE() {
        return JPLINE;
    }
    public void setJPLINE(int value) {
        JPLINE = value;
    }

    private int BEFMD;

    public int getBEFMD() {
        return BEFMD;
    }
    public void setBEFMD(int value) {
        BEFMD = value;
    }

    private int FRQBEF;

    public int getFRQBEF() {
        return FRQBEF;
    }
    public void setFRQBEF(int value) {
        FRQBEF = value;
    }

    private int PSGMD;

    public int getPSGMD() {
        return PSGMD;
    }

    private int KEYONR;

    public int getKEYONR() {
        return KEYONR;
    }
    public void setKEYONR(int value) {
        KEYONR = value;
    }

    private int bufStartPtr;

    public int getbufStartPtr() {
        return bufStartPtr;
    }
    public void setbufStartPtr(int value) {
        bufStartPtr = value;
    }

    private int[][][] bufCount;

    public int[][][] getbufCount() {
        return bufCount;
    }

    private int JPCOL;

    public int getJPCOL() {
        return JPCOL;
    }
    public void setJPCOL(int value) {
        JPCOL = value;
    }

    private List<Integer> JCHCOM;

    public List<Integer> getJCHCOM() {
        return JCHCOM;
    }
    public void setJCHCOM(List<Integer> value) {
        JCHCOM = value;
    }

    private boolean rhythmRelMode = false;

    public boolean getrhythmRelMode() {
        return rhythmRelMode;
    }
    public void setrhythmRelMode(boolean value) {
        rhythmRelMode = value;
    }

    private boolean rhythmPanMode = false;

    public boolean getrhythmPanMode() {
        return rhythmPanMode;
    }
    public void setrhythmPanMode(boolean value) {
        rhythmPanMode = value;
    }

    private int rhythmInstNum = 0;

    public int getrhythmInstNum() {
        return rhythmInstNum;
    }
    public void setrhythmInstNum(int value) {
        rhythmInstNum = value;
    }

    private boolean isEnd = false;

    public boolean getisEnd() {
        return isEnd;
    }
    public void setisEnd(boolean value) {
        isEnd = value;
    }

    private int FMVolMode = 0;

    public int getFMVolMode() {
        return FMVolMode;
    }
    public void setFMVolMode(int value) {
        FMVolMode = value;
    }

    private boolean CompEndCmdFlag = false;

    public boolean getCompEndCmdFlag() {
        return CompEndCmdFlag;
    }
    public void setCompEndCmdFlag(boolean value) {
        CompEndCmdFlag = value;
    }

    private Set<Integer> useSSGVoice = new HashSet<>();

    public Set<Integer> getuseSSGVoice() {
        return useSSGVoice;
    }

    public void setuseSSGVoice(Set<Integer> value) {
        useSSGVoice = value;
    }

    private Set<Integer> usedFMVoiceNumber = new HashSet<Integer>();

    public Set<Integer> getusedFMVoiceNumber() {
        return usedFMVoiceNumber;
    }

    public void setusedFMVoiceNumber(Set<Integer> value) {
        usedFMVoiceNumber = value;
    }

    private int partPos = -1;

    public int getpartPos() {
        return partPos;
    }

    public void setpartPos(int value) {
        partPos = value;
    }

    private boolean partReplaceSw = false;

    public boolean getpartReplaceSw() {
        return partReplaceSw;
    }

    public void setpartReplaceSw(boolean value) {
        partReplaceSw = value;
    }

    public int MU_NUM = 0;// 0xC200 b ｺﾝﾊﾟｲﾙﾁｭｳ ﾉ MUSICﾅﾝﾊﾞｰ
    public int OTODAT = 1;// 0xc201 w FMｵﾝｼｮｸ ｶﾞ ｶｸﾉｳｻﾚﾙ ｱﾄﾞﾚｽﾄｯﾌﾟ ｶﾞ ﾊｲｯﾃｲﾙ
    public int SSGDAT = 3;// 0xc203 w SSG...
    public int MU_TOP = 5;// 0xc205 w ﾐｭｰｼﾞｯｸ ﾃﾞｰﾀ(ｱﾄﾞﾚｽﾃｰﾌﾞﾙ ﾌｸﾑ) ｽﾀｰﾄ ｱﾄﾞﾚｽ
    public CompilerInfo compilerInfo = null;
    public int quantize = 0;
    public int beforeQuantize = 0;
    public int pageNow = 0;
    public int backupMDATA = 0;
    public int lastMDATA = 0;
    public int latestNote = 0;// 最後に解析したのが音符の場合は1、休符の場合は2、初期値は0

    public byte porSW = 0;
    public byte porDelta = 0;
    public int porTime = 0;
    public int porOldNote = -1;
    public byte porPin = 0;

    /**
     * 各チップのindex
     */
    public int ChipIndex = 0;

    /**
     * 各チップの割当チャンネル
     * 曲全体のトラックはCOMNOWを参照
     */
    public int CHIP_CH = 0;

    public String currentChipName = "";
    public String currentPartType = "";

    public Work() {
        ClearCounter();
    }

    public void ClearCounter() {
        if (tcnt == null) tcnt = new int[MAXChips][][];
        if (lcnt == null) lcnt = new int[MAXChips][][];
        if (loopPoint == null) loopPoint = new int[MAXChips][][];
        if (bufCount == null) bufCount = new int[MAXChips][][];

        for (int i = 0; i < MAXChips; i++) {
            if (tcnt[i] == null) tcnt[i] = new int[MAXCH][];
            if (lcnt[i] == null) lcnt[i] = new int[MAXCH][];
            if (loopPoint[i] == null) loopPoint[i] = new int[MAXCH][];
            if (bufCount[i] == null) bufCount[i] = new int[MAXCH][];

            for (int j = 0; j < MAXCH; j++) {
                if (tcnt[i][j] == null) tcnt[i][j] = new int[MAXPG];
                if (lcnt[i][j] == null) lcnt[i][j] = new int[MAXPG];
                if (loopPoint[i][j] == null) loopPoint[i][j] = new int[MAXPG];
                if (bufCount[i][j] == null) bufCount[i][j] = new int[MAXPG];

                for (int k = 0; k < MAXPG; k++) {
                    tcnt[i][j][k] = 0;
                    lcnt[i][j][k] = 0;
                    loopPoint[i][j][k] = 0;
                    bufCount[i][j][k] = 0;
                }
            }
        }
    }


    // 各チップのトラック
    // ABCDEFGHIJK OPNA ...1
    // LMNOPQRSTUV OPNA ...2
    // abcdefghijk OPNB ...3
    // lmnopqrstuv OPNB ...4
    // WXYZwxyz    OPM  ...5

    public String Tracks = "ABCDEFGHIJKLMNOPQRSTUVabcdefghijklmnopqrstuvWXYZwxyz";

    public boolean SetChipValueFromTrackCharacter(char ch) {
        int no = GetTrackNo(ch);
        if (no < 0) return false;//知らない文字
        ChipIndex = no / MAXCH;
        CHIP_CH = no % MAXCH;
        return true;//セットできた
    }

    public char GetTrackCharacterFromChipValue(int chipIndex, int CHIP_CH) {
        int no = chipIndex * MAXCH + CHIP_CH;
        if (no < 0 || no >= Tracks.length()) return Character.MIN_VALUE;
        return Tracks.charAt(no);
    }

    public int GetTrackNo(char ch) {
        return Tracks.indexOf(ch);
    }

    public char GetTrackCharacter(int num) {
        return Tracks.charAt(num);
    }

    /**
     * トラックとして使用できる文字ではない
     * <param name="c"></param>
     * <returns></returns>
     */
    public boolean IsNotTrackCharacter(char c) {
        return Tracks.indexOf(c) < 0;
    }
}

