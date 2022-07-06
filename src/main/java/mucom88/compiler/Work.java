package mucom88.compiler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import musicDriverInterface.CompilerInfo;


public class Work {

    public static final int MAXChips = 5;
    public static final int MAXCH = 11; // OPMは8ですけどね
    public static final int MAXPG = 10;

    //
    public int fmlib = 0; // 0x6000 w

    public int[][][] tCnt; // w
    public int[][][] lCnt; // w
    public int[][][] loopPoint; // w

    public int pcmFlag = 0; // 10*4 w
    public int jClock = 0; // w
    public int loopSp = 0; // w ﾙｰﾌﾟｽﾀｯｸ
    public int mData = 0; // w
    public int datTbl = 0; // w
    public int octave = 0; // b
    public int siftDat = 0; // b
    public int siftDa2 = 0; // b
    public int clock = 0; // b
    public int errLine = 0; // w
    //public int COMNOW = 0; // b
    public int count = 0; // b
    public int volInt = 0; // b
    public int escape = 0; //
    public int minUsf = 0; //
    public int befRst = 0; // b
    public int tieFg = 0; // b
    public int[] otoNum = new int[MAXChips]; // 0xfxxx b
    public int volume = 0; // b
    public int endAdr = 0; // w
    public int octInt = 0; // w
    public int vicAdr = 0; // w
    public static final String titleFmt = "[  MUCOM88 Ver:0.0  ]  Address:0000-0000(0000)         [ 00:00 ] MODE:NORMAL  ";
    public String title = "[  MUCOM88 Ver:0.0  ]  Address:0000-0000(0000)         [ 00:00 ] MODE:NORMAL  ";
    //public byte fmvoiceCnt = 0; // 50
    public byte[] lfoData = new byte[] {1, 0, 0, 0, 0, 0, 0};
    public byte linCfg = 0;
    public int adrStc = 0;
    public byte vpco = 1; // dummyで1としている
    public byte octaveUDFlag = 0;
    public byte volumeUDFlag = 0;
    public boolean pcmInvert = false;
    public int repCount = 0;
    public int tvOfs = 0;
    public int pointC = 0; // LOOPSTART ADR ｶﾞ ｾｯﾃｲｻﾚﾃｲﾙ ADR
    public byte maCfg = 0; // 0>< AS MACRO PRC

    public int tst2Val = 0xc000;

    public int hexFg = 0;

    private byte secCom;

    public byte getSecCom() {
        return secCom;
    }

    public void setSecCom(byte value) {
        secCom = value;
    }

    private byte[] befTone = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0};

    public byte[] getBeforeTone() {
        return befTone;
    }

    private int befCo;

    public int getBefCo() {
        return befCo;
    }
    public void setBeforeCo(int value) {
        befCo = value;
    }

    private int com;

    public int getCom() {
        return com;
    }
    public void setCom(int value) {
        com = value;
    }

    private byte bfDat;

    public byte getBfDat() {
        return bfDat;
    }
    public void setBfDat(byte value) {
        bfDat = value;
    }

    private byte vdDat;

    public byte getVdDat() {
        return vdDat;
    }
    public void setVdDat(byte value){
         vdDat = value;
    }

    private int line;

    public int getLine() {
        return line;
    }

    private int jpLine = -1;

    public int getJpLine() {
        return jpLine;
    }
    public void setJpLine(int value) {
        jpLine = value;
    }

    private int befMd;

    public int getBefMd() {
        return befMd;
    }
    public void setBefMd(int value) {
        befMd = value;
    }

    private int frqBef;

    public int getFrqBef() {
        return frqBef;
    }
    public void setFrqBef(int value) {
        frqBef = value;
    }

    private int psgMd;

    public int getPsgMd() {
        return psgMd;
    }

    private int keyOnR;

    public int getKeyOnR() {
        return keyOnR;
    }
    public void setKeyOnR(int value) {
        keyOnR = value;
    }

    private int bufStartPtr;

    public int getBufStartPtr() {
        return bufStartPtr;
    }
    public void setBufStartPtr(int value) {
        bufStartPtr = value;
    }

    private int[][][] bufCount;

    public int[][][] getBufCount() {
        return bufCount;
    }

    private int jpCol;

    public int getJpCol() {
        return jpCol;
    }
    public void setJpCol(int value) {
        jpCol = value;
    }

    private List<Integer> jChCom;

    public List<Integer> getJChCom() {
        return jChCom;
    }
    public void setJChCom(List<Integer> value) {
        jChCom = value;
    }

    private boolean rhythmRelMode = false;

    public boolean getRhythmRelMode() {
        return rhythmRelMode;
    }
    public void setRhythmRelMode(boolean value) {
        rhythmRelMode = value;
    }

    private boolean rhythmPanMode = false;

    public boolean getRhythmPanMode() {
        return rhythmPanMode;
    }
    public void setRhythmPanMode(boolean value) {
        rhythmPanMode = value;
    }

    private int rhythmInstNum = 0;

    public int getRhythmInstNum() {
        return rhythmInstNum;
    }
    public void setRhythmInstNum(int value) {
        rhythmInstNum = value;
    }

    private boolean isEnd = false;

    public boolean isEnd() {
        return isEnd;
    }
    public void setEnd(boolean value) {
        isEnd = value;
    }

    private int fmVolMode = 0;

    public int getFmVolMode() {
        return fmVolMode;
    }
    public void setFmVolMode(int value) {
        fmVolMode = value;
    }

    private boolean compEndCmdFlag = false;

    public boolean getCompEndCmdFlag() {
        return compEndCmdFlag;
    }
    public void setCompEndCmdFlag(boolean value) {
        compEndCmdFlag = value;
    }

    private Set<Integer> useSSGVoice = new HashSet<>();

    public Set<Integer> getUseSSGVoice() {
        return useSSGVoice;
    }

    public void setUseSSGVoice(Set<Integer> value) {
        useSSGVoice = value;
    }

    private Set<Integer> usedFMVoiceNumber = new HashSet<>();

    public Set<Integer> getUsedFMVoiceNumber() {
        return usedFMVoiceNumber;
    }

    public void setUsedFMVoiceNumber(Set<Integer> value) {
        usedFMVoiceNumber = value;
    }

    private int partPos = -1;

    public int getPartPos() {
        return partPos;
    }

    public void setPartPos(int value) {
        partPos = value;
    }

    private boolean partReplaceSw = false;

    public boolean getPartReplaceSw() {
        return partReplaceSw;
    }

    public void setPartReplaceSw(boolean value) {
        partReplaceSw = value;
    }

    public int muNum = 0; // b ｺﾝﾊﾟｲﾙﾁｭｳ ﾉ MUSICﾅﾝﾊﾞｰ
    public int otoDat = 1; // w FMｵﾝｼｮｸ ｶﾞ ｶｸﾉｳｻﾚﾙ ｱﾄﾞﾚｽﾄｯﾌﾟ ｶﾞ ﾊｲｯﾃｲﾙ
    public int ssgDat = 3; // w SSG...
    public int muTop = 5; // w ﾐｭｰｼﾞｯｸ ﾃﾞｰﾀ(ｱﾄﾞﾚｽﾃｰﾌﾞﾙ ﾌｸﾑ) ｽﾀｰﾄ ｱﾄﾞﾚｽ
    public CompilerInfo compilerInfo = null;
    public int quantize = 0;
    public int beforeQuantize = 0;
    public int pageNow = 0;
    public int backupMData = 0;
    public int lastMData = 0;
    public int latestNote = 0; // 最後に解析したのが音符の場合は1、休符の場合は2、初期値は0

    public byte porSW = 0;
    public byte porDelta = 0;
    public int porTime = 0;
    public int porOldNote = -1;
    public byte porPin = 0;

    /**
     * 各チップのindex
     */
    public int chipIndex = 0;

    /**
     * 各チップの割当チャンネル
     * 曲全体のトラックはCOMNOWを参照
     */
    public int chipCh = 0;

    public String currentChipName = "";
    public String currentPartType = "";

    public Work() {
        clearCounter();
    }

    public void clearCounter() {
        if (tCnt == null) tCnt = new int[MAXChips][][];
        if (lCnt == null) lCnt = new int[MAXChips][][];
        if (loopPoint == null) loopPoint = new int[MAXChips][][];
        if (bufCount == null) bufCount = new int[MAXChips][][];

        for (int i = 0; i < MAXChips; i++) {
            if (tCnt[i] == null) tCnt[i] = new int[MAXCH][];
            if (lCnt[i] == null) lCnt[i] = new int[MAXCH][];
            if (loopPoint[i] == null) loopPoint[i] = new int[MAXCH][];
            if (bufCount[i] == null) bufCount[i] = new int[MAXCH][];

            for (int j = 0; j < MAXCH; j++) {
                if (tCnt[i][j] == null) tCnt[i][j] = new int[MAXPG];
                if (lCnt[i][j] == null) lCnt[i][j] = new int[MAXPG];
                if (loopPoint[i][j] == null) loopPoint[i][j] = new int[MAXPG];
                if (bufCount[i][j] == null) bufCount[i][j] = new int[MAXPG];

                for (int k = 0; k < MAXPG; k++) {
                    tCnt[i][j][k] = 0;
                    lCnt[i][j][k] = 0;
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

    public static final String Tracks = "ABCDEFGHIJKLMNOPQRSTUVabcdefghijklmnopqrstuvWXYZwxyz";

    public boolean setChipValueFromTrackCharacter(char ch) {
        int no = getTrackNo(ch);
        if (no < 0) return false; // 知らない文字
        chipIndex = no / MAXCH;
        chipCh = no % MAXCH;
        return true; // セットできた
    }

    public static char getTrackCharacterFromChipValue(int chipIndex, int CHIP_CH) {
        int no = chipIndex * MAXCH + CHIP_CH;
        if (no < 0 || no >= Tracks.length()) return Character.MIN_VALUE;
        return Tracks.charAt(no);
    }

    public static int getTrackNo(char ch) {
        return Tracks.indexOf(ch);
    }

    public static char getTrackCharacter(int num) {
        return Tracks.charAt(num);
    }

    /**
     * トラックとして使用できる文字ではない
     */
    public static boolean isNotTrackCharacter(char c) {
        return Tracks.indexOf(c) < 0;
    }
}

