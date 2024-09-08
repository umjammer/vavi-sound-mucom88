package mucom88.player;

public abstract class RSoundChip {
    protected int SoundLocation;
    protected int BusID;
    protected int SoundChip;

    public int dClock = 3579545;

    public RSoundChip(int soundLocation, int busID, int soundChip) {
        SoundLocation = soundLocation;
        BusID = busID;
        SoundChip = soundChip;
    }

    public abstract void init();

    public abstract void setRegister(int adr, int dat);

    public abstract int getRegister(int adr);

    public abstract boolean isBufferEmpty();

    public abstract int SetMasterClock(int mClock);

    public abstract void setSSGVolume(int vol);

    public abstract void OPNAWaitSend(long elapsed, int size);

    public abstract RSoundChip CheckDevice();
}
