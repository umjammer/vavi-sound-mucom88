package mucom88.driver;

/**
 * OPM timer Emulation
 */
class OPMTimer extends FMTimer {

    public OPMTimer(int renderingFreq, int opmMasterClock) {
        super(renderingFreq, opmMasterClock);
        step = opmMasterClock / 64.0 / 1.0 / renderingFreq;
    }

    @Override
    public boolean writeReg(byte adr, byte data) {
        switch (adr) {
        case 0x10:
            timerA &= 0x3;
            timerA |= ((data & 0xff) << 2);
            return true;
        case 0x11:
            timerA &= 0x3fc;
            timerA |= (data & 3);
            return true;
        case 0x12:
            // timerB
            timerB = (256 - (data & 0xff)) << (10 - 6);
            return true;
        case 0x14:
            // Timer Control Register
            timerReg = data & 0x8F;
            statReg &= 0xff - ((data >> 4) & 3);
            return true;
        }
        return false;
    }
}
