package mucom88.driver;

public class FMTimer {
    /** タイマーAのオーバーフロー設定値 */
    public int timerA;
    /** タイマーAのカウンター値 */
    protected double timerACounter;
    /** タイマーBのオーバーフロー設定値 */
    public int timerB;
    /** タイマーBのカウンター値 */
    protected double timerBCounter;
    /** タイマー制御レジスタ (下位4ビット+7ビット) */
    public int timerReg;
    public double step;

    /** ステータスレジスタ (下位2ビット) */
    int statReg;

    public int getStatReg() {
        return statReg;
    }

    public Runnable csmKeyOn;

    public FMTimer(int renderingFreq, int masterClock) {
    }

    public void timer() {
        if ((timerReg & 0x01) != 0) { // timerA 動作中
            timerACounter += step;
            if (timerACounter >= (1024 - timerA)) {
                statReg |= ((timerReg >> 2) & 0x01);
                timerACounter -= (1024 - timerA);
                //if ((timerReg & 0x80) != 0) csmKeyOn?.Invoke();
            }
        }

        if ((timerReg & 0x02) != 0) { // timerB 動作中
            timerBCounter += step;
            if (timerBCounter >= timerB) {
                statReg |= ((timerReg >> 2) & 0x02);
                timerBCounter -= timerB;
            }
        }
    }

    public boolean writeReg(byte adr, byte data) {
        return false;
    }
}

