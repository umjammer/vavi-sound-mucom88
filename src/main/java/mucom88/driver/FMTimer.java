package mucom88.driver;

public class FMTimer {
    // タイマーAのオーバーフロー設定値
    public int TimerA;
    // タイマーAのカウンター値
    protected double TimerAcounter;
    // タイマーBのオーバーフロー設定値
    public int TimerB;
    // タイマーBのカウンター値
    protected double TimerBcounter;
    // タイマー制御レジスタ (下位4ビット+7ビット)
    public int TimerReg;
    public double step;

    // ステータスレジスタ (下位2ビット)
    int StatReg;

    public int getStatReg() {
        return StatReg;
    }

    public Runnable CsmKeyOn;

    public FMTimer(int renderingFreq, int masterClock) {
    }

    public void timer() {
        if ((TimerReg & 0x01) != 0) {   // TimerA 動作中
            TimerAcounter += step;
            if (TimerAcounter >= (1024 - TimerA)) {
                StatReg |= ((TimerReg >> 2) & 0x01);
                TimerAcounter -= (1024 - TimerA);
                //if ((TimerReg & 0x80) != 0) CsmKeyOn?.Invoke();
            }
        }

        if ((TimerReg & 0x02) != 0) {   // TimerB 動作中
            TimerBcounter += step;
            if (TimerBcounter >= TimerB) {
                StatReg |= ((TimerReg >> 2) & 0x02);
                TimerBcounter -= TimerB;
            }
        }
    }

    public boolean WriteReg(byte adr, byte data) {
        return false;
    }
}

