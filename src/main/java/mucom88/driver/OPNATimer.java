package mucom88.driver;

//
//  OPNA timer エミュレーション
//
public class OPNATimer extends FMTimer {

    public OPNATimer(int renderingFreq, int opnaMasterClock) {
        super(renderingFreq, opnaMasterClock);
        step = opnaMasterClock / 72.0 / 2.0 / (double) renderingFreq;
    }

    @Override
    public boolean WriteReg(byte adr, byte data) {
        switch (adr) {
        // TimerA
        case 0x24:
            TimerA &= 0x3;
            TimerA |= (data << 2);
            return true;
        case 0x25:
            TimerA &= 0x3fc;
            TimerA |= (data & 3);
            return true;
        case 0x26:
            // TimerB
            TimerB = (256 - data) << 4;
            return true;
        case 0x27:
            // タイマー制御レジスタ
            TimerReg = data & 0x8F;
            StatReg &= 0xFF - ((data >> 4) & 3);
            return true;
        }
        return false;
    }
}
