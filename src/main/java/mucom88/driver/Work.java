package mucom88.driver;

import java.util.Map;

import dotnet4j.Tuple;
import dotnet4j.Tuple6;
import musicDriverInterface.MmlDatum;


public class Work {
    public final Object lockObj = new Object();
    public final Object SystemInterrupt = new Object();
    public boolean resetPlaySync = false;

    private int _status = 0;

    private MUBHeader header;
    public MUBHeader getHeader() {
        return header;
    }

    public void setHeader(MUBHeader value) {
        header = value;
    }

    int Status;
    public int getStatus() {
        synchronized (lockObj) {
            return _status;
        }
    }

    public void setStatus(int value) {
        synchronized (lockObj) {
            _status = value;
        }
    }

    int mDataAdr;

    public int getmDataAdr() {
        return mDataAdr;
    }

    //public int idx { get; internal set; }

    /**
     * カレントのチャンネル
     */
    SoundWork.CHDAT cd;

    public SoundWork.CHDAT getcd() {
        return cd;
    }

    /**
     * カレントのページ
     */
    SoundWork.CHDAT.PGDAT pg;

    public SoundWork.CHDAT.PGDAT getpg() {
        return pg;
    }

    boolean carry;

    public boolean getcarry() {
        return carry;
    }

    int hl;

    public int gethl() {
        return hl;
    }

    byte A_Reg;

    public byte getA_Reg() {
        return A_Reg;
    }

    int weight;

    public int getweight() {
        return weight;
    }

    Object crntMmlDatum;

    public Object getcrntMmlDatum() {
        return crntMmlDatum;
    }

    int maxLoopCount = -1;

    public int getmaxLoopCount() {
        return maxLoopCount;
    }

    int nowLoopCounter = -1;

    public int getnowLoopCounter() {
        return nowLoopCounter;
    }

    int[] rhythmORKeyOff = new int[4];

    public int[] getrhythmORKeyOff() {
        return rhythmORKeyOff;
    }

    int[] rhythmOR = new int[4];

    public int[] getrhythmOR() {
        return rhythmOR;
    }

    boolean abnormalEnd = false;

    public boolean getabnormalEnd() {
        return abnormalEnd;
    }

    int currentTimer;

    public int getcurrentTimer() {
        return currentTimer;
    }

    Map<Integer, byte[]> ssgVoiceAtMusData;

    public Map<Integer, byte[]> getssgVoiceAtMusData() {
        return ssgVoiceAtMusData;
    }

    public OPNATimer timerOPNA1 = null;
    public OPNATimer timerOPNA2 = null;
    public OPNATimer timerOPNB1 = null;
    public OPNATimer timerOPNB2 = null;
    public OPMTimer timerOPM = null;

    public long timeCounter = 0L;
    public byte[][] fmVoice = new byte[][] {null, null, null, null};
    public Tuple<String, short[]>[][] pcmTables = new Tuple[][] {null, null, null, null, null, null};
    public MmlDatum[] mData = null;
    public SoundWork soundWork = null;
    public byte[] fmVoiceAtMusData = null;
    public boolean isDotNET = false;
    public boolean SSGExtend = false;

    public Work() {
        Init();
    }

    void Init() {
        soundWork = new SoundWork();
        soundWork.Init();
    }
}
