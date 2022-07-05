package mucom88.player;


public class SChipType {

    private boolean useEmu = true;

    public boolean getUseEmu() {
        return useEmu;
    }

    public void setUseEmu(boolean value) {
        useEmu = value;
    }

    private boolean useEmu2 = false;

    public boolean getUseEmu2() {
        return useEmu2;
    }

    public void setUseEmu2(boolean value) {
        useEmu2 = value;
    }

    private boolean useEmu3 = false;

    public boolean getUseEmu3() {
        return useEmu3;
    }

    public void setUseEmu3(boolean value) {
        useEmu3 = value;
    }

    private boolean useScci = false;

    public boolean getUseScci() {
        return useScci;
    }

    public void setUseScci(boolean value) {
        useScci = value;
    }

    private String interfaceName = "";

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String value) {
        interfaceName = value;
    }

    private int soundLocation = -1;

    public int getSoundLocation() {
        return soundLocation;
    }

    public void setSoundLocation(int value) {
        soundLocation = value;
    }

    private int busID = -1;

    public int getBusID() {
        return busID;
    }

    public void setBusID(int value) {
        busID = value;
    }

    private int soundChip = -1;

    public int getSoundChip() {
        return soundChip;
    }

    public void setSoundChip(int value) {
        soundChip = value;
    }

    private String chipName = "";

    public String getChipName() {
        return chipName;
    }

    public void setChipName(String value) {
        chipName = value;
    }

    private boolean useScci2 = false;

    public boolean getUseScci2() {
        return useScci2;
    }

    public void setUseScci2(boolean value) {
        useScci2 = value;
    }

    private String interfaceName2A = "";

    public String getInterfaceName2A() {
        return interfaceName2A;
    }

    public void setInterfaceName2A(String value) {
        interfaceName2A = value;
    }

    private int soundLocation2A = -1;

    public int getSoundLocation2A() {
        return soundLocation2A;
    }

    public void setSoundLocation2A(int value) {
        soundLocation2A = value;
    }

    private int busID2A = -1;

    public int getBusID2A() {
        return busID2A;
    }

    public void setBusID2A(int value) {
        busID2A = value;
    }

    private int soundChip2A = -1;

    public int getSoundChip2A() {
        return soundChip2A;
    }

    public void setSoundChip2A(int value) {
        soundChip2A = value;
    }

    private String chipName2A = "";

    public String getChipName2A() {
        return chipName2A;
    }

    public void setChipName2A(String value) {
        chipName2A = value;
    }

    private String interfaceName2B = "";

    public String getInterfaceName2B() {
        return interfaceName2B;
    }

    public void setInterfaceName2B(String value) {
        interfaceName2B = value;
    }

    private int soundLocation2B = -1;

    public int getSoundLocation2B() {
        return soundLocation2B;
    }

    public void setSoundLocation2B(int value) {
        soundLocation2B = value;
    }

    private int busID2B = -1;

    public int getBusID2B() {
        return busID2B;
    }

    public void setBusID2B(int value) {
        busID2B = value;
    }

    private int soundChip2B = -1;

    public int getSoundChip2B() {
        return soundChip2B;
    }

    public void setSoundChip2B(int value) {
        soundChip2B = value;
    }

    private String chipName2B = "";

    public String getChipName2B() {
        return chipName2B;
    }

    public void setChipName2B(String value) {
        chipName2B = value;
    }

    private boolean useWait = true;

    public boolean getUseWait() {
        return useWait;
    }

    public void setUseWait(boolean value) {
        useWait = value;
    }

    private boolean useWaitBoost = false;

    public boolean getUseWaitBoost() {
        return useWaitBoost;
    }

    public void setUseWaitBoost(boolean value) {
        useWaitBoost = value;
    }

    private boolean onlyPCMEmulation = false;

    public boolean getOnlyPCMEmulation() {
        return onlyPCMEmulation;
    }

    public void setOnlyPCMEmulation(boolean value) {
        onlyPCMEmulation = value;
    }

    private int latencyForEmulation = 0;

    public int getLatencyForEmulation() {
        return latencyForEmulation;
    }

    public void setLatencyForEmulation(int value) {
        latencyForEmulation = value;
    }

    private int latencyForScci = 0;

    public int getLatencyForScci() {
        return latencyForScci;
    }

    public void setLatencyForScci(int value) {
        latencyForScci = value;
    }

    public SChipType Copy() {
        SChipType ct = new SChipType();
        ct.useEmu = this.useEmu;
        ct.useEmu2 = this.useEmu2;
        ct.useEmu3 = this.useEmu3;
        ct.useScci = this.useScci;
        ct.soundLocation = this.soundLocation;

        ct.busID = this.busID;
        ct.interfaceName = this.interfaceName;
        ct.soundChip = this.soundChip;
        ct.chipName = this.chipName;
        ct.useScci2 = this.useScci2;
        ct.soundLocation2A = this.soundLocation2A;

        ct.interfaceName2A = this.interfaceName2A;
        ct.busID2A = this.busID2A;
        ct.soundChip2A = this.soundChip2A;
        ct.chipName2A = this.chipName2A;
        ct.soundLocation2B = this.soundLocation2B;

        ct.interfaceName2B = this.interfaceName2B;
        ct.busID2B = this.busID2B;
        ct.soundChip2B = this.soundChip2B;
        ct.chipName2B = this.chipName2B;

        ct.useWait = this.useWait;
        ct.useWaitBoost = this.useWaitBoost;
        ct.onlyPCMEmulation = this.onlyPCMEmulation;
        ct.latencyForEmulation = this.latencyForEmulation;
        ct.latencyForScci = this.latencyForScci;

        return ct;
    }
}
