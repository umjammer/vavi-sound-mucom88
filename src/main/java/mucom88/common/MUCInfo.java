package mucom88.common;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dotnet4j.Tuple;
import dotnet4j.io.Path;
import musicDriverInterface.MmlDatum;


public class MUCInfo {
    private String title;

    public String gettitle() {
        return title;
    }

    public void settitle(String value) {
        title = value;
    }

    private String composer;

    public String getcomposer() {
        return composer;
    }

    public void setcomposer(String value) {
        composer = value;
    }

    private String author;

    public String getauthor() {
        return author;
    }

    public void setauthor(String value) {
        author = value;
    }

    private String comment;
    public String getcomment() {
        return comment;
    }
    public void setcomment(String value) {
        comment = value;
    }
    public void addcomment(String value) {
        comment += value;
    }

    private String mucom88;

    public String getmucom88() {
        return mucom88;
    }

    public void setmucom88(String value) {
        mucom88 = value;
    }

    private String date;

    public String getdate() {
        return date;
    }

    public void setdate(String value) {
        date = value;
    }

    private String voice;

    public String getvoice() {
        return voice;
    }

    public void setvoice(String value) {
        voice = value;
    }

    private String[] pcm = new String[6];

    public String[] getpcm() {
        return pcm;
    }

    public void setpcm(String[] value) {
        pcm = value;
    }

    public List<String>[] getpcmAt() {
        return pcmAt;
    }

    public void setpcmAt(List<String>[] value) {
        pcmAt = value;
    }

    private List<String>[] pcmAt = new ArrayList[] {
            new ArrayList<String>(),
            new ArrayList<String>(),
            new ArrayList<String>(),
            new ArrayList<String>(),
            new ArrayList<String>(),
            new ArrayList<String>()
    };
    private String driver;

    public String getdriver() {
        return driver;
    }

    public void setdriver(String value) {
        driver = value;
    }

    private String invert;

    public String getinvert() {
        return invert;
    }

    public void setinvert(String value) {
        invert = value;
    }

    private String pcminvert;

    public String getpcminvert() {
        return pcminvert;
    }

    public void setpcminvert(String value) {
        pcminvert = value;
    }

    public static final String DotNET = "mucomDotNET";


    private int lines;

    public int getlines() {
        return lines;
    }

    public void setlines(int value) {
        lines = value;
    }

    /**
     * mml中で定義した音色データ
     */
    private byte[] mmlVoiceData;

    public byte[] getmmlVoiceData() {
        return mmlVoiceData;
    }

    public void setmmlVoiceData(byte[] value) {
        mmlVoiceData = value;
    }

    /**
     * ファイルから読み込んだプリセットの音色データ
     */
    private byte[] voiceData;

    public byte[] getvoiceData() {
        return voiceData;
    }

    public void setvoiceData(byte[] value) {
        voiceData = value;
    }

    private byte[] pcmData;

    public byte[] getpcmData() {
        return pcmData;
    }

    public void setpcmData(byte[] value) {
        pcmData = value;
    }

    /**
     * SSG波形データ
     */
    private Map<Integer, byte[]> ssgVoice;

    public Map<Integer, byte[]> getssgVoice() {
        return ssgVoice;
    }

    public void setpssgVoice(Map<Integer, byte[]> value) {
        ssgVoice = value;
    }

    private List<Tuple<Integer, String>> basSrc;

    public List<Tuple<Integer, String>> getbasSrc() {
        return basSrc;
    }

    public void setbasSrc(List<Tuple<Integer, String>> value) {
        basSrc = value;
    }

    public Object document = null;

    private String _fnSrc = null;

    public String getfnSrc() {
        return _fnSrc;
    }

    public void setfnSrc(String value) {
        _fnSrc = value;
        _fnSrcOnlyFile = Path.getFileName(value);
    }

    private String _fnSrcOnlyFile = null;

    public String getfnSrcOnlyFile() {
        return _fnSrcOnlyFile;
    }

    private String workPath;

    public String getworkPath() {
        return workPath;
    }

    public void setworkPath(String value) {
        workPath = value;
    }

    private String fnDst;

    public String getfnDst() {
        return fnDst;
    }

    public void setfnDst(String value) {
        fnDst = value;
    }

    //KUMA:作業向けメモリ
    private List<MmlDatum> bufDst;

    public List<MmlDatum> getbufDst() {
        return bufDst;
    }

    public void setbufDst(List<MmlDatum> value) {
        bufDst = value;
    }

    //KUMA:ページ毎のメモリ
    private List<MmlDatum>[][][] bufPage;

    public List<MmlDatum>[][][] getbufPage() {
        return bufPage;
    }

    public void setbufPage(List<MmlDatum>[][][] value) {
        bufPage = value;
    }

    //KUMA:音色用のメモリ(ページ機能使用時のみ)
    private List<MmlDatum> bufUseVoice;

    public List<MmlDatum> getbufUseVoice() {
        return bufUseVoice;
    }

    public void setbufUseVoice(List<MmlDatum> value) {
        bufUseVoice = value;
    }

    private int srcLinPtr;

    public int getsrcLinPtr() {
        return srcLinPtr;
    }

    public void incSrcLinPtr() {
        srcLinPtr++;
    }

    public void setsrcLinPtr(int value) {
        srcLinPtr = value;
    }

    private int srcCPtr;

    public int getSrcCPtr() {
        return srcCPtr;
    }
    public int getAndIncSrcCPtr() {
        return srcCPtr++;
    }
    public int incAndGetSrcCPtr() {
        return ++srcCPtr;
    }
    public void decSrcCPtr() {
        srcCPtr--;
    }
    public void setSrcCPtr(int value) {
        srcCPtr = value;
    }

    private Tuple<Integer, String> lin;

    public Tuple<Integer, String> getlin() {
        return lin;
    }

    public void setlin(Tuple<Integer, String> value) {
        lin = value;
    }

    private boolean Carry;

    public boolean getCarry() {
        return Carry;
    }

    public void setCarry(boolean value) {
        Carry = value;
    }

    private boolean ErrSign;

    public boolean getErrSign() {
        return ErrSign;
    }

    public void setErrSign(boolean value) {
        ErrSign = value;
    }

    private List<Integer> bufMac;

    public List<Integer> getbufMac() {
        return bufMac;
    }

    public void setbufMac(List<Integer> value) {
        bufMac = value;
    }

    private List<Integer> bufMacStack;

    public List<Integer> getbufMacStack() {
        return bufMacStack;
    }

    public void setbufMacStack(List<Integer> value) {
        bufMacStack = value;
    }

    private List<Byte> bufLoopStack;

    public List<Byte> getbufLoopStack() {
        return bufLoopStack;
    }

    public void setbufLoopStack(List<Byte> value) {
        bufLoopStack = value;
    }

    /**
     * mml全体で実際に使用した音色番号
     * 関連項目:
     * orig:DEFVOICE
     */
    private List<Integer> bufDefVoice;

    public List<Integer> getbufDefVoice() {
        return bufDefVoice;
    }

    public void setbufDefVoice(List<Integer> value) {
        bufDefVoice = value;
    }

    private int useOtoAdr;

    public int getuseOtoAdr() {
        return useOtoAdr;
    }

    public void setuseOtoAdr(int value) {
        useOtoAdr = value;
    }

    private List<Integer> bufTitle;

    public List<Integer> getbufTitle() {
        return bufTitle;
    }

    public void setbufTitle(List<Integer> value) {
        bufTitle = value;
    }

    private List<Byte> mmlVoiceDataWork;

    public List<Byte> getmmlVoiceDataWork() {
        return mmlVoiceDataWork;
    }

    public void setmmlVoiceDataWork(List<Byte> value) {
        mmlVoiceDataWork = value;
    }

    private int row;

    public int getrow() {
        return row;
    }

    public void setrow(int value) {
        row = value;
    }

    private int col;

    public int getcol() {
        return col;
    }

    public void setcol(int value) {
        col = value;
    }

    private int VM;

    public int getVM() {
        return VM;
    }

    public void setVM(int value) {
        VM = value;
    }
    //private boolean needNormalMucom; public boolean getneedNormalMucom() { return needNormalMucom; } public void setneedNormalMucom(boolean value) { needNormalMucom = value; } = false;

    public enum enmDriverType {
        normal,
        E,
        em,
        DotNet
    }

    private enmDriverType _DriverType = enmDriverType.normal;
    //public boolean needEMucom = false;

    //mucomDotNET独自機能を使用したか否か
    public enmDriverType getDriverType() {
        return _DriverType;
    }

    public void setDriverType(enmDriverType value) {
        //if (_DriverType == enmDriverType.normal && value == enmDriverType.DotNet && needNormalMucom)
        //{
        //    throw new MucException(msg.get("E0001"), row, col);
        //}

        //if (_DriverType == enmDriverType.E && needEMucom)
        //    return;

        _DriverType = value;
    }

    private boolean isIDE = false;

    public boolean getisIDE() {
        return isIDE;
    }

    public void setisIDE(boolean value) {
        isIDE = value;
    }

    private Point skipPoint = new Point(0, 0);

    public Point getskipPoint() {
        return skipPoint;
    }

    public void setskipPoint(Point value) {
        skipPoint = value;
    }

    private int skipChannel = -1;

    public int getskipChannel() {
        return skipChannel;
    }

    public void setskipChannel(int value) {
        skipChannel = value;
    }

    private boolean isExtendFormat = false;

    public boolean getisExtendFormat() {
        return isExtendFormat;
    }

    public void setisExtendFormat(boolean value) {
        isExtendFormat = value;
    }

    private boolean carriercorrection = false;

    public boolean getcarriercorrection() {
        return carriercorrection;
    }

    public void setcarriercorrection(boolean value) {
        carriercorrection = value;
    }

    private enmOpmClockMode opmclockmode = enmOpmClockMode.normal;

    public enmOpmClockMode getopmclockmode() {
        return opmclockmode;
    }

    public void setopmclockmode(enmOpmClockMode value) {
        opmclockmode = value;
    }

    public enum enmOpmClockMode {
        normal, X68000
    }

    private boolean SSGExtend = false;

    public boolean getSSGExtend() {
        return SSGExtend;
    }

    public void setSSGExtend(boolean value) {
        SSGExtend = value;
    }

    private List<Byte> useSSGWavNum = new ArrayList<>();

    public List<Byte> getuseSSGWavNum() {
        return useSSGWavNum;
    }

    public void setuseSSGWavNum(List<Byte> value) {
        useSSGWavNum = value;
    }

    private boolean opmpanreverse = false;

    public boolean getopmpanreverse() {
        return opmpanreverse;
    }

    public void setopmpanreverse(boolean value) {
        opmpanreverse = value;
    }

    private int opna1rhythmmute = 0;

    public int getopna1rhythmmute() {
        return opna1rhythmmute;
    }

    public void setopna1rhythmmute(int value) {
        opna1rhythmmute = value;
    }
    public void oropna1rhythmmute(int value) {
        opna1rhythmmute |= value;
    }

    private int opna2rhythmmute = 0;

    public int getopna2rhythmmute() {
        return opna2rhythmmute;
    }

    public void setopna2rhythmmute(int value) {
        opna2rhythmmute = value;
    }
    public void oropna2rhythmmute(int value) {
        opna2rhythmmute |= value;
    }

    private int opnb1adpcmamute = 0;

    public int getopnb1adpcmamute() {
        return opnb1adpcmamute;
    }

    public void setopnb1adpcmamute(int value) {
        opnb1adpcmamute = value;
    }
    public void oropnb1adpcmamute(int value) {
        opnb1adpcmamute |= value;
    }

    private int opnb2adpcmamute = 0;

    public int getopnb2adpcmamute() {
        return opnb2adpcmamute;
    }

    public void setopnb2adpcmamute(int value) {
        opnb2adpcmamute = value;
    }
    public void oropnb2adpcmamute(int value) {
        opnb2adpcmamute |= value;
    }

    public void Clear() {
        title = "";
        composer = "";
        author = "";
        comment = "";
        mucom88 = "";
        date = "";
        voice = "";
        for (int i = 0; i < 6; i++) {
            pcm[i] = "";
            pcmAt[i].clear();
        }
        driver = "";
        invert = "";
        pcminvert = "";
        lines = 0;
        voiceData = null;
        pcmData = null;
        basSrc = new ArrayList<>();
        _fnSrc = "";
        workPath = "";
        fnDst = "";

        //バッファの作成
        bufPage = new ArrayList[5][][];
        for (int i = 0; i < 5; i++) {
            bufPage[i] = new ArrayList[11][];
            for (int j = 0; j < 11; j++) {
                bufPage[i][j] = new ArrayList[10];
                for (int pg = 0; pg < 10; pg++) {
                    bufPage[i][j][pg] = new ArrayList<>();
                }
            }
        }
        bufDst = bufPage[0][0][0];

        srcLinPtr = 0;
        srcCPtr = 0;
        bufMac = new ArrayList<>();
        bufMacStack = new ArrayList<>();
        bufLoopStack = new ArrayList<>();
        bufDefVoice = new ArrayList<>();
        bufTitle = new ArrayList<>();
        mmlVoiceDataWork = new ArrayList<>();

        _DriverType = enmDriverType.DotNet;//.normal;
        //needNormalMucom = false;
        isIDE = false;
        isExtendFormat = false;
        carriercorrection = false;
        opmclockmode = enmOpmClockMode.normal;
        SSGExtend = false;
        opmpanreverse = false;
    }
}
