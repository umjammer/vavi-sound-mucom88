package mucom88.driver;

import java.util.Map;

import dotnet4j.util.compat.Tuple;
import musicDriverInterface.MmlDatum;


public class Work {
    public final Object lockObj = new Object();
    public final Object systemInterrupt = new Object();
    public boolean resetPlaySync = false;

    private int _status = 0;

    private MubHeader header;
    public MubHeader getHeader() {
        return header;
    }

    public void setHeader(MubHeader value) {
        header = value;
    }

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

    public int getMDataAdr() {
        return mDataAdr;
    }

    //public int idx { get; internal set; }

    /**
     * カレントのチャンネル
     */
    SoundWork.CHDAT cd;

    public SoundWork.CHDAT getCd() {
        return cd;
    }

    /**
     * カレントのページ
     */
    SoundWork.CHDAT.PGDAT pg;

    public SoundWork.CHDAT.PGDAT getPg() {
        return pg;
    }

    boolean carry;

    public boolean getCarry() {
        return carry;
    }

    int hl;

    public int getHl() {
        return hl;
    }

    byte aReg;

    public byte getA_Reg() {
        return aReg;
    }

    int weight;

    public int getWeight() {
        return weight;
    }

    Object crntMmlDatum;

    public Object getCrntMmlDatum() {
        return crntMmlDatum;
    }

    int maxLoopCount = -1;

    public int getMaxLoopCount() {
        return maxLoopCount;
    }

    int nowLoopCounter = -1;

    public int getNowLoopCounter() {
        return nowLoopCounter;
    }

    int[] rhythmORKeyOff = new int[4];

    public int[] getRhythmORKeyOff() {
        return rhythmORKeyOff;
    }

    int[] rhythmOR = new int[4];

    public int[] getRhythmOR() {
        return rhythmOR;
    }

    boolean abnormalEnd = false;

    public boolean getAbnormalEnd() {
        return abnormalEnd;
    }

    int currentTimer;

    public int getCurrentTimer() {
        return currentTimer;
    }

    Map<Integer, byte[]> ssgVoiceAtMusData;

    public Map<Integer, byte[]> getSsgVoiceAtMusData() {
        return ssgVoiceAtMusData;
    }

    public OPNATimer timerOPNA1 = null;
    public OPNATimer timerOPNA2 = null;
    public OPNATimer timerOPNB1 = null;
    public OPNATimer timerOPNB2 = null;
    public OPMTimer timerOPM = null;

    public long timeCounter = 0L;
    public byte[][] fmVoice = new byte[4][];
    public Tuple<String, short[]>[][] pcmTables = new Tuple[6][];
    public MmlDatum[] mData = null;
    public SoundWork soundWork = null;
    public byte[] fmVoiceAtMusData = null;
    public boolean isDotNET = false;
    public boolean SSGExtend = false;

    public Work() {
        init();
    }

    void init() {
        soundWork = new SoundWork();
        soundWork.init();
    }
}
