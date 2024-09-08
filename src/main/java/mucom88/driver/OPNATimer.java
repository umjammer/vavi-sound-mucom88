package mucom88.driver;

//
// OPNA timer エミュレーション
//
public class OPNATimer extends FMTimer {

    public OPNATimer(int renderingFreq, int opnaMasterClock) {
        super(renderingFreq, opnaMasterClock);
        step = opnaMasterClock / 72.0 / 2.0 / renderingFreq;
    }

    @Override
    public boolean writeReg(byte adr, byte data) {
        switch (adr) {
        // timerA
        case 0x24:
            timerA &= 0x3;
            timerA |= ((data & 0xff) << 2);
            return true;
        case 0x25:
            timerA &= 0x3fc;
            timerA |= (data & 3);
            return true;
        case 0x26:
            // timerB
            timerB = (256 - (data & 0xff)) << 4;
            return true;
        case 0x27:
            // タイマー制御レジスタ
            timerReg = data & 0x8F;
            statReg &= 0xFF - ((data >> 4) & 3);
            return true;
        }
        return false;
    }
}
