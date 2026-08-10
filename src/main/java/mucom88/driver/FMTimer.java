package mucom88.driver;

class FMTimer {

    /** Timer A overflow setting */
    int timerA;
    /** Timer A counter value */
    private double timerACounter;
    /** Timer B overflow setting */
    int timerB;
    /** Timer B counter value */
    private double timerBCounter;
    /** Timer control register (lower 4 bits + 7 bits) */
    int timerReg;
    double step;

    /** Status register (lowest 2 bits) */
    int statReg;

    public int getStatReg() {
        return statReg;
    }

    public Runnable csmKeyOn;

    FMTimer(int renderingFreq, int masterClock) {
    }

    public void timer() {
        if ((timerReg & 0x01) != 0) { // TimerA is running
            timerACounter += step;
            if (timerACounter >= (1024 - timerA)) {
                statReg |= ((timerReg >> 2) & 0x01);
                timerACounter -= (1024 - timerA);
                //if ((timerReg & 0x80) != 0) csmKeyOn?.Invoke();
            }
        }

        if ((timerReg & 0x02) != 0) { // TimerB is running
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

