package mucom88.common;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dotnet4j.io.Path;
import dotnet4j.util.compat.Tuple;
import musicDriverInterface.MmlDatum;


public class MUCInfo {
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        title = value;
    }

    private String composer;

    public String getComposer() {
        return composer;
    }

    public void setComposer(String value) {
        composer = value;
    }

    private String author;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String value) {
        author = value;
    }

    private String comment;
    public String getComment() {
        return comment;
    }
    public void setComment(String value) {
        comment = value;
    }
    public void addComment(String value) {
        comment += value;
    }

    private String mucom88;

    public String getMucom88() {
        return mucom88;
    }

    public void setMucom88(String value) {
        mucom88 = value;
    }

    private String date;

    public String getDate() {
        return date;
    }

    public void setDate(String value) {
        date = value;
    }

    private String voice;

    public String getVoice() {
        return voice;
    }

    public void setVoice(String value) {
        voice = value;
    }

    private String[] pcm = new String[6];

    public String[] getPcm() {
        return pcm;
    }

    public void setPcm(String[] value) {
        pcm = value;
    }

    public List<String>[] getPcmAt() {
        return pcmAt;
    }

    public void setPcmAt(List<String>[] value) {
        pcmAt = value;
    }

    private List<String>[] pcmAt = new List[] {
            new ArrayList<String>(),
            new ArrayList<String>(),
            new ArrayList<String>(),
            new ArrayList<String>(),
            new ArrayList<String>(),
            new ArrayList<String>()
    };
    private String driver;

    public String getDriver() {
        return driver;
    }

    public void setDriver(String value) {
        driver = value;
    }

    private String invert;

    public String getInvert() {
        return invert;
    }

    public void setInvert(String value) {
        invert = value;
    }

    private String pcmInvert;

    public String getPcmInvert() {
        return pcmInvert;
    }

    public void setPcmInvert(String value) {
        pcmInvert = value;
    }

    public static final String dotNET = "mucomDotNET";


    private int lines;

    public int getLines() {
        return lines;
    }

    public void setLines(int value) {
        lines = value;
    }

    /**
     * mml中で定義した音色データ
     */
    private byte[] mmlVoiceData;

    public byte[] getMmlVoiceData() {
        return mmlVoiceData;
    }

    public void setMmlVoiceData(byte[] value) {
        mmlVoiceData = value;
    }

    /**
     * ファイルから読み込んだプリセットの音色データ
     */
    private byte[] voiceData;

    public byte[] getVoiceData() {
        return voiceData;
    }

    public void setVoiceData(byte[] value) {
        voiceData = value;
    }

    private byte[] pcmData;

    public byte[] getPcmData() {
        return pcmData;
    }

    public void setPcmData(byte[] value) {
        pcmData = value;
    }

    /**
     * SSG波形データ
     */
    private Map<Integer, byte[]> ssgVoice = new HashMap<>();

    public Map<Integer, byte[]> getSsgVoice() {
        return ssgVoice;
    }

    public void setSsgVoice(Map<Integer, byte[]> value) {
        ssgVoice = value;
    }

    private List<Tuple<Integer, String>> basSrc;

    public List<Tuple<Integer, String>> getBasSrc() {
        return basSrc;
    }

    public void setBasSrc(List<Tuple<Integer, String>> value) {
        basSrc = value;
    }

    public Object document = null;

    private String _fnSrc = null;

    public String getFnSrc() {
        return _fnSrc;
    }

    public void setFnSrc(String value) {
        _fnSrc = value;
        _fnSrcOnlyFile = Path.getFileName(value);
    }

    private String _fnSrcOnlyFile = null;

    public String getFnSrcOnlyFile() {
        return _fnSrcOnlyFile;
    }

    private String workPath;

    public String getWorkPath() {
        return workPath;
    }

    public void setWorkPath(String value) {
        workPath = value;
    }

    private String fnDst;

    public String getFnDst() {
        return fnDst;
    }

    public void setFnDst(String value) {
        fnDst = value;
    }

    // KUMA:作業向けメモリ
    private List<MmlDatum> bufDst; // AutoExtendList

    public List<MmlDatum> getBufDst() {
        return bufDst;
    }

    public void setBufDst(List<MmlDatum> value) {
        assert value instanceof AutoExtendList : value.getClass().getName();
        bufDst = value;
    }

    // KUMA:ページ毎のメモリ
    private List<MmlDatum>[][][] bufPage; // AutoExtendList

    public List<MmlDatum>[][][] getBufPage() {
        return bufPage;
    }

    public void setBufPage(List<MmlDatum>[][][] value) {
        bufPage = value;
    }

    // KUMA:音色用のメモリ(ページ機能使用時のみ)
    private List<MmlDatum> bufUseVoice; // AutoExtendList

    public List<MmlDatum> getBufUseVoice() {
        return bufUseVoice;
    }

    public void setBufUseVoice(List<MmlDatum> value) {
        assert value instanceof AutoExtendList : value.getClass().getName();
        bufUseVoice = value;
    }

    private int srcLinPtr;

    public int getSrcLinPtr() {
        return srcLinPtr;
    }

    public void incSrcLinPtr() {
        srcLinPtr++;
    }

    public void setSrcLinPtr(int value) {
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

    public Tuple<Integer, String> getLin() {
        return lin;
    }

    public void setLin(Tuple<Integer, String> value) {
        lin = value;
    }

    private boolean carry;

    public boolean getCarry() {
        return carry;
    }

    public void setCarry(boolean value) {
        carry = value;
    }

    private boolean errSign;

    public boolean getErrSign() {
        return errSign;
    }

    public void setErrSign(boolean value) {
        errSign = value;
    }

    private List<Integer> bufMac; // AutoExtendList

    public List<Integer> getBufMac() {
        return bufMac;
    }

    public void setBufMac(List<Integer> value) {
        bufMac = value;
    }

    private List<Integer> bufMacStack; // AutoExtendList

    public List<Integer> getBufMacStack() {
        return bufMacStack;
    }

    public void setBufMacStack(List<Integer> value) {
        bufMacStack = value;
    }

    private List<Byte> bufLoopStack; // AutoExtendList

    public List<Byte> getBufLoopStack() {
        return bufLoopStack;
    }

    public void setBufLoopStack(List<Byte> value) {
        bufLoopStack = value;
    }

    /**
     * mml全体で実際に使用した音色番号
     * 関連項目:
     * orig:DEFVOICE
     */
    private List<Integer> bufDefVoice; // AutoExtendList

    public List<Integer> getBufDefVoice() {
        return bufDefVoice;
    }

    public void setBufDefVoice(List<Integer> value) {
        bufDefVoice = value;
    }

    private int useOtoAdr;

    public int getUseOtoAdr() {
        return useOtoAdr;
    }

    public void setUseOtoAdr(int value) {
        useOtoAdr = value;
    }

    private List<Integer> bufTitle; // AutoExtendList

    public List<Integer> getBufTitle() {
        return bufTitle;
    }

    public void setBufTitle(List<Integer> value) {
        bufTitle = value;
    }

    private List<Byte> mmlVoiceDataWork; // AutoExtendList

    public List<Byte> getMmlVoiceDataWork() {
        return mmlVoiceDataWork;
    }

    public void setMmlVoiceDataWork(List<Byte> value) {
        mmlVoiceDataWork = value;
    }

    private int row;

    public int getRow() {
        return row;
    }

    public void setRow(int value) {
        row = value;
    }

    private int col;

    public int getCol() {
        return col;
    }

    public void setCol(int value) {
        col = value;
    }

    private int vm;

    public int getVM() {
        return vm;
    }

    public void setVM(int value) {
        vm = value;
    }

    public enum DriverType {
        normal,
        E,
        em,
        DotNet
    }

    private DriverType driverType = DriverType.normal;

    // mucomDotNET独自機能を使用したか否か
    public DriverType getDriverType() {
        return driverType;
    }

    public void setDriverType(DriverType value) {
        driverType = value;
    }

    private boolean isIDE = false;

    public boolean isIDE() {
        return isIDE;
    }

    public void setIDE(boolean value) {
        isIDE = value;
    }

    private Point skipPoint = new Point(0, 0);

    public Point getSkipPoint() {
        return skipPoint;
    }

    public void setSkipPoint(Point value) {
        skipPoint = value;
    }

    private int skipChannel = -1;

    public int getSkipChannel() {
        return skipChannel;
    }

    public void setSkipChannel(int value) {
        skipChannel = value;
    }

    private boolean isExtendFormat = false;

    public boolean isExtendFormat() {
        return isExtendFormat;
    }

    public void setExtendFormat(boolean value) {
        isExtendFormat = value;
    }

    private boolean carrierCorrection = false;

    public boolean getCarrierCorrection() {
        return carrierCorrection;
    }

    public void setCarrierCorrection(boolean value) {
        carrierCorrection = value;
    }

    private OpmClockMode opmClockMode = OpmClockMode.normal;

    public OpmClockMode getOpmClockMode() {
        return opmClockMode;
    }

    public void setOpmClockMode(OpmClockMode value) {
        opmClockMode = value;
    }

    public enum OpmClockMode {
        normal, X68000
    }

    private boolean ssgExtend = false;

    public boolean getSSGExtend() {
        return ssgExtend;
    }

    public void setSSGExtend(boolean value) {
        ssgExtend = value;
    }

    private List<Byte> useSSGWavNum = new ArrayList<>();

    public List<Byte> getUseSSGWavNum() {
        return useSSGWavNum;
    }

    public void setUseSSGWavNum(List<Byte> value) {
        useSSGWavNum = value;
    }

    private boolean opmPanReverse = false;

    public boolean getOpmPanReverse() {
        return opmPanReverse;
    }

    public void setOpmPanReverse(boolean value) {
        opmPanReverse = value;
    }

    private int opna1RhythmMute = 0;

    public int getOpna1RhythmMute() {
        return opna1RhythmMute;
    }

    public void setOpna1rhythmmute(int value) {
        opna1RhythmMute = value;
    }
    public void oropna1rhythmmute(int value) {
        opna1RhythmMute |= value;
    }

    private int opna2RhythmMute = 0;

    public int getOpna2RhythmMute() {
        return opna2RhythmMute;
    }

    public void setOpna2RhythmMute(int value) {
        opna2RhythmMute = value;
    }
    public void orOpna2RhythmMute(int value) {
        opna2RhythmMute |= value;
    }

    private int opnb1AdpcmAMute = 0;

    public int getOpnb1AdpcmAMute() {
        return opnb1AdpcmAMute;
    }

    public void setOpnb1AdpcmAMute(int value) {
        opnb1AdpcmAMute = value;
    }
    public void orOpnb1AdpcmAMute(int value) {
        opnb1AdpcmAMute |= value;
    }

    private int opnb2AdpcmAMute = 0;

    public int getOpnb2AdpcmAMute() {
        return opnb2AdpcmAMute;
    }

    public void setOpnb2AdpcmAMute(int value) {
        opnb2AdpcmAMute = value;
    }
    public void orOpnb2AdpcmAMute(int value) {
        opnb2AdpcmAMute |= value;
    }

    public void clear() {
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
        pcmInvert = "";
        lines = 0;
        voiceData = null;
        pcmData = null;
        basSrc = new ArrayList<>();
        _fnSrc = "";
        workPath = "";
        fnDst = "";

        // バッファの作成
        bufPage = new List[5][][];
        for (int i = 0; i < 5; i++) {
            bufPage[i] = new List[11][];
            for (int j = 0; j < 11; j++) {
                bufPage[i][j] = new List[10];
                for (int pg = 0; pg < 10; pg++) {
                    bufPage[i][j][pg] = new AutoExtendList<>(MmlDatum.class);
                }
            }
        }
        bufDst = bufPage[0][0][0];

        srcLinPtr = 0;
        srcCPtr = 0;
        bufMac = new AutoExtendList<>(Integer.TYPE);
        bufMacStack = new AutoExtendList<>(Integer.TYPE);
        bufLoopStack = new AutoExtendList<>(Byte.TYPE);
        bufDefVoice = new AutoExtendList<>(Integer.TYPE);
        bufTitle = new AutoExtendList<>(Integer.TYPE);
        mmlVoiceDataWork = new AutoExtendList<>(Byte.TYPE);

        driverType = DriverType.DotNet;//.normal;
        //needNormalMucom = false;
        isIDE = false;
        isExtendFormat = false;
        carrierCorrection = false;
        opmClockMode = OpmClockMode.normal;
        ssgExtend = false;
        opmPanReverse = false;
    }
}
