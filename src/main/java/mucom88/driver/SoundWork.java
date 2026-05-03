package mucom88.driver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import musicDriverInterface.MmlDatum;


/**
 * The work present in the original is defined here.
 */
public class SoundWork {

    public final List<List<CHDAT>> chData = Arrays.asList(
            Arrays.asList(
                    new CHDAT(), // FM Ch1
                    new CHDAT(), // FM Ch2
                    new CHDAT(), // FM Ch3
                    new CHDAT(), // SSG Ch1
                    new CHDAT(), // SSG Ch2
                    new CHDAT(), // SSG Ch3
                    new CHDAT(), // Drums Ch
                    new CHDAT(), // FM Ch4
                    new CHDAT(), // FM Ch5
                    new CHDAT(), // FM Ch6
                    new CHDAT() // ADPCM Ch
            ),
            Arrays.asList(
                    new CHDAT(), // FM Ch1
                    new CHDAT(), // FM Ch2
                    new CHDAT(), // FM Ch3
                    new CHDAT(), // SSG Ch1
                    new CHDAT(), // SSG Ch2
                    new CHDAT(), // SSG Ch3
                    new CHDAT(), // Drums Ch
                    new CHDAT(), // FM Ch4
                    new CHDAT(), // FM Ch5
                    new CHDAT(), // FM Ch6
                    new CHDAT() // ADPCM Ch
            ),
            Arrays.asList(
                    new CHDAT(), // FM Ch1
                    new CHDAT(), // FM Ch2
                    new CHDAT(), // FM Ch3
                    new CHDAT(), // SSG Ch1
                    new CHDAT(), // SSG Ch2
                    new CHDAT(), // SSG Ch3
                    new CHDAT(), // Drums Ch
                    new CHDAT(), // FM Ch4
                    new CHDAT(), // FM Ch5
                    new CHDAT(), // FM Ch6
                    new CHDAT() // ADPCM Ch
            ),
            Arrays.asList(
                    new CHDAT(), // FM Ch1
                    new CHDAT(), // FM Ch2
                    new CHDAT(), // FM Ch3
                    new CHDAT(), // SSG Ch1
                    new CHDAT(), // SSG Ch2
                    new CHDAT(), // SSG Ch3
                    new CHDAT(), // Drums Ch
                    new CHDAT(), // FM Ch4
                    new CHDAT(), // FM Ch5
                    new CHDAT(), // FM Ch6
                    new CHDAT() // ADPCM Ch
            ),
            Arrays.asList(
                    new CHDAT(), // FM Ch1
                    new CHDAT(), // FM Ch2
                    new CHDAT(), // FM Ch3
                    new CHDAT(), // FM Ch4
                    new CHDAT(), // FM Ch5
                    new CHDAT(), // FM Ch6
                    new CHDAT(), // FM Ch7
                    new CHDAT(), // FM Ch8
                    null,
                    null,
                    null
            )
    );

    public int[][] pregBf = null;
    public int[] initPm = null;
    public final int[][] detdat = {
            null, null, null, null
    };
    public final int[][] drmvol = new int[][] {
            null, null, null, null
    };
    public final int[][] drmPanEnable = new int[][] {
            null, null, null, null
    };
    public final int[][] drmPanMode = new int[][] {
            null, null, null, null
    };
    public final int[][] drmPanCounter = new int[][] {
            null, null, null, null
    };
    public final int[][] drmPanCounterWork = new int[][] {
            null, null, null, null
    };
    public final int[][] drmPanValue = new int[][] {
            null, null, null, null
    };
    public int[] opSel = null;
    public int[] TYPE1 = null;
    public int[] TYPE2 = null;
    // DB 8
    public int DMY = 0;
    public int[][] FNUMB = null;
    public int[][] FNUMBopm = null;
    public int[][] SNUMB = null;
    public int[][] PCMNMB = null;
    public int[] SSGDAT = null;

    private int musNum;

    public int getMusNum() {
        return musNum;
    }

    public void setMusNum(int value) {
        musNum = value;
    }

    private int c2Num;

    public int getC2Num() {
        return c2Num;
    }

    public void setC2Num(int value) {
        c2Num = value;
    }

    public void incC2NUM() {
        c2Num++;
    }

    private int chNum;

    public int getChNum() {
        return chNum;
    }

    public void setChNum(int value) {
        chNum = value;
    }

    public void incChNum() {
        chNum++;
    }

    private int pvMode;

    public int getPvMode() {
        return pvMode;
    }

    public void setPvMode(int value) {
        pvMode = value;
    }

    private static final int muTop = 5;

    public int getMuTop() {
        return muTop;
    }

    private int timerB;

    public int getTimerB() {
        return timerB;
    }

    public void setTimerB(int value) {
        timerB = value;
    }

    private int tbTop;

    public int getTbTop() {
        return tbTop;
    }

    public void setTbTop(int value) {
        tbTop = value;
    }

    public void addTbTop(int value) {
        tbTop += value;
    }

    private int noTsb2;

    public int getNoTsb2() {
        return noTsb2;
    }

    public void setNoTsb2(int value) {
        noTsb2 = value;
    }

    private boolean useTimerA = false;

    public boolean useTimerA() {
        return useTimerA;
    }

    public void useTimerA(boolean value) {
        useTimerA = value;
    }

    private int TIMER_A = 10;

    public int getTimerA() {
        return TIMER_A;
    }

    public void setTimerA(int value) {
        TIMER_A = value;
    }

    public boolean ch3SpMode(int chip) {
        return (PLSET1_VAL[chip] & 0x40) != 0;
    }

    public final int[] PLSET1_VAL = new int[5];
    public final int[] PLSET2_VAL = new int[5];

    private final int[] pcmLr = new int[6];

    public int[] getPcmLr() {
        return pcmLr;
    }

    private int fmPort;

    public int getFmPort() {
        return fmPort;
    }

    public void setFmPort(int value) {
        fmPort = value;
    }

    private int ssgF1;

    public int getSsgF1() {
        return ssgF1;
    }

    public void setSsgF1(int value) {
        ssgF1 = value;
    }

    private int drmF1;

    public int getDrmF1() {
        return drmF1;
    }

    public void setDrmF1(int value) {
        drmF1 = value;
    }

    private int pcmFlg;

    public int getPcmFlg() {
        return pcmFlg;
    }

    public void setPcmFlg(int value) {
        pcmFlg = value;
    }

    private int ready = 0xff;

    public int getReady() {
        return ready;
    }

    public void setReady(int value) {
        ready = value;
    }

    private int rhythm;

    public int getRhythm() {
        return rhythm;
    }

    public void setRhythm(int value) {
        rhythm = value;
    }

    private final int[] deltN = new int[4];

    public int[] getDeltN() {
        return deltN;
    }

    private int fNum;

    public int getFNum() {
        return fNum;
    }

    public void setFNum(int value) {
        fNum = value;
    }

    private int fmSub8Val;

    public int getFmSub8Val() {
        return fmSub8Val;
    }

    private int fPortVal = 0xa4;

    public int getFPortVal() {
        return fPortVal;
    }

    public void setFPortVal(int value) {
        fPortVal = value;
    }

    private int pcmNum;

    public int getPcmNum() {
        return pcmNum;
    }

    public void setPcmNum(int value) {
        pcmNum = value;
    }

    private int pOut;

    public int getPOut() {
        return pOut;
    }

    public void setPOut(int value) {
        pOut = value;
    }

    private final int[] STTADR = new int[4];

    public int[] getSTTADR() {
        return STTADR;
    }

    private final int[] ENDADR = new int[4];

    public int[] getENDADR() {
        return ENDADR;
    }

    private int TOTALV;

    public int getTOTALV() {
        return TOTALV;
    }

    private static final int OTODAT = 1;

    public int getOTODAT() {
        return OTODAT;
    }

    private int LFOP6_VAL;

    public int getLFOP6_VAL() {
        return LFOP6_VAL;
    }

    private int FLGADR;

    public int getFLGADR() {
        return FLGADR;
    }

    public void setFLGADR(int value) {
        FLGADR = value;
    }

    private int NEWFNM;

    public int getNEWFNM() {
        return NEWFNM;
    }

    public void setNEWFNM(int value) {
        NEWFNM = value;
    }

    private long RANDUM = 0;

    public long getRANDUM() {
        return RANDUM;
    }

    public void setRANDUM(long value) {
        RANDUM = value;
    }

    private int KEY_FLAG = 0;

    public int getKEY_FLAG() {
        return KEY_FLAG;
    }

    public void setKEY_FLAG(int value) {
        KEY_FLAG = value;
    }

    private int currentChip;

    public int getCurrentChip() {
        return currentChip;
    }

    public void setCurrentChip(int value) {
        currentChip = value;
    }

    private int currentCh;

    public int getCurrentCh() {
        return currentCh;
    }
    public void setCurrentCh(int value) {
        currentCh = value;
    }

    private final int[][] PCMaSTTADR = new int[][] {
            new int[6], new int[6]
    };

    public int[][] getPCMaSTTADR() {
        return PCMaSTTADR;
    }

    public final int[][] PCMaENDADR = new int[][] {
            new int[6], new int[6]
    };

    public int[][] getPCMaENDADR() {
        return PCMaENDADR;
    }

    /** PMS/AMS/LR DATA */
    public static final int[] PALDAT = {
            0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0,
            0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0,
            0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

            0xC0, 0xC0, 0xC0, 0xC0, 0xC0, 0xC0, 0xC0, 0xC0, 0xC0, 0xC0,
            0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0,
            0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0,
            0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0
    };

    // volume data

    public static final int[] FMVDAT = { // volume data (FM)
            0x36, 0x33, 0x30, 0x2d,
            0x2a, 0x28, 0x25, 0x22, //  0,  1,  2,  3
            0x20, 0x1d, 0x1a, 0x18, //  4,  5,  6,  7
            0x15, 0x12, 0x10, 0x0d, //  8,  9, 10, 11
            0x0a, 0x08, 0x05, 0x02  // 12, 13, 14, 15
    };

    public static final int[] CRYDAT = { // carrier / modulator data
            0x08,
            0x08, // each bits represent carrier / modulator
            0x08, //
            0x08, // Bit=1 carrier
            0x0c, //     0 modulator
            0x0e, //
            0x0e, // Bit0=OP 1 , Bit1=OP 2 ... etc
            0x0f
    };

    void init() {
        for (int chipIndex = 0; chipIndex < 4; chipIndex++) {
            for (int i = 0; i < chData.get(chipIndex).get(0).pgDat.size(); i++) {
                chData.get(chipIndex).getFirst().pgDat.get(i).lengthCounter = 1;
                chData.get(chipIndex).getFirst().pgDat.get(i).instrumentNumber = 24;
                chData.get(chipIndex).getFirst().pgDat.get(i).volume = 10;
            }

            for (int i = 0; i < chData.get(chipIndex).get(1).pgDat.size(); i++) {
                chData.get(chipIndex).get(1).pgDat.get(i).lengthCounter = 1;
                chData.get(chipIndex).get(1).pgDat.get(i).instrumentNumber = 24;
                chData.get(chipIndex).get(1).pgDat.get(i).volume = 10;
                chData.get(chipIndex).get(1).pgDat.get(i).channelNumber = 1;
            }

            for (int i = 0; i < chData.get(chipIndex).get(2).pgDat.size(); i++) {
                chData.get(chipIndex).get(2).pgDat.get(i).lengthCounter = 1;
                chData.get(chipIndex).get(2).pgDat.get(i).instrumentNumber = 24;
                chData.get(chipIndex).get(2).pgDat.get(i).volume = 10;
                chData.get(chipIndex).get(2).pgDat.get(i).channelNumber = 2;
            }

            for (int i = 0; i < chData.get(chipIndex).get(3).pgDat.size(); i++) {
                chData.get(chipIndex).get(3).pgDat.get(i).lengthCounter = 1;
                chData.get(chipIndex).get(3).pgDat.get(i).instrumentNumber = 0;
                chData.get(chipIndex).get(3).pgDat.get(i).volume = 8;
                chData.get(chipIndex).get(3).pgDat.get(i).volReg = 8;
                chData.get(chipIndex).get(3).pgDat.get(i).channelNumber = 0;
            }

            for (int i = 0; i < chData.get(chipIndex).get(4).pgDat.size(); i++) {
                chData.get(chipIndex).get(4).pgDat.get(i).lengthCounter = 1;
                chData.get(chipIndex).get(4).pgDat.get(i).instrumentNumber = 0;
                chData.get(chipIndex).get(4).pgDat.get(i).volume = 8;
                chData.get(chipIndex).get(4).pgDat.get(i).volReg = 9;
                chData.get(chipIndex).get(4).pgDat.get(i).channelNumber = 2;
            }

            for (int i = 0; i < chData.get(chipIndex).get(5).pgDat.size(); i++) {
                chData.get(chipIndex).get(5).pgDat.get(i).lengthCounter = 1;
                chData.get(chipIndex).get(5).pgDat.get(i).instrumentNumber = 0;
                chData.get(chipIndex).get(5).pgDat.get(i).volume = 8;
                chData.get(chipIndex).get(5).pgDat.get(i).volReg = 10;
                chData.get(chipIndex).get(5).pgDat.get(i).channelNumber = 4;
            }

            for (int i = 0; i < chData.get(chipIndex).get(6).pgDat.size(); i++) {
                chData.get(chipIndex).get(6).pgDat.get(i).lengthCounter = 1;
                chData.get(chipIndex).get(6).pgDat.get(i).volume = 10;
                chData.get(chipIndex).get(6).pgDat.get(i).channelNumber = 2;
            }

            for (int i = 0; i < chData.get(chipIndex).get(7).pgDat.size(); i++) {
                chData.get(chipIndex).get(7).pgDat.get(i).lengthCounter = 1;
                chData.get(chipIndex).get(7).pgDat.get(i).volume = 10;
                chData.get(chipIndex).get(7).pgDat.get(i).channelNumber = 2;
            }

            if (chData.get(chipIndex).get(8) != null) {
                for (int i = 0; i < chData.get(chipIndex).get(8).pgDat.size(); i++) {
                    chData.get(chipIndex).get(8).pgDat.get(i).lengthCounter = 1;
                    chData.get(chipIndex).get(8).pgDat.get(i).volume = 10;
                    chData.get(chipIndex).get(8).pgDat.get(i).channelNumber = 2;
                }
            }

            if (chData.get(chipIndex).get(9) != null) {
                for (int i = 0; i < chData.get(chipIndex).get(9).pgDat.size(); i++) {
                    chData.get(chipIndex).get(9).pgDat.get(i).lengthCounter = 1;
                    chData.get(chipIndex).get(9).pgDat.get(i).volume = 10;
                    chData.get(chipIndex).get(9).pgDat.get(i).channelNumber = 2;
                }
            }

            if (chData.get(chipIndex).get(10) != null) {
                for (int i = 0; i < chData.get(chipIndex).get(10).pgDat.size(); i++) {
                    chData.get(chipIndex).get(10).pgDat.get(i).lengthCounter = 1;
                    chData.get(chipIndex).get(10).pgDat.get(i).volume = 10;
                    chData.get(chipIndex).get(10).pgDat.get(i).channelNumber = 2;
                }
            }

            for (int i = 0; i < chData.get(chipIndex).size(); i++) {
                chData.get(chipIndex).get(i).fmVolMode = 0;
                chData.get(chipIndex).get(i).currentFMVolTable = FMVDAT;
            }
        }

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < chData.get(4).get(i).pgDat.size(); j++) {
                chData.get(4).get(i).pgDat.get(j).lengthCounter = 1;
                chData.get(4).get(i).pgDat.get(j).instrumentNumber = 24;
                chData.get(4).get(i).pgDat.get(j).volume = 10;
                chData.get(4).get(i).pgDat.get(j).channelNumber = i;
            }

            chData.get(4).get(i).fmVolMode = 0;
            chData.get(4).get(i).currentFMVolTable = FMVDAT;
        }


        pregBf = new int[][] {
                new int[9], new int[9], new int[9], new int[9]
        };
        initPm = new int[] {0, 0, 0, 0, 0, 56, 0, 0, 0};
        for (int i = 0; i < 4; i++) {
            detdat[i] = new int[] {
                    0, 0, 0, 0
            };
            drmvol[i] = new int[] {
                    0xc0, 0xc0, 0xc0, 0xc0, 0xc0, 0xc0
            };
            drmPanCounter[i] = new int[] {
                    0, 0, 0, 0, 0, 0
            };
            drmPanCounterWork[i] = new int[] {
                    0, 0, 0, 0, 0, 0
            };
            drmPanEnable[i] = new int[] {
                    0, 0, 0, 0, 0, 0
            };
            drmPanMode[i] = new int[] {
                    0, 0, 0, 0, 0, 0
            };
            drmPanValue[i] = new int[] {
                    0, 0, 0, 0, 0, 0
            };
        }
        opSel = new int[] {
                0xa6, 0xac, 0xad, 0xae
        };
        DMY = 8;
        TYPE1 = new int[] {0x032, 0x044, 0x046};
        TYPE2 = new int[] {0x0AA, 0x0A8, 0x0AC};
        FNUMB = new int[][] {
                {
                        0x026a, 0x028f, 0x02b6, 0x02df,
                        0x030b, 0x0339, 0x036a, 0x039e,
                        0x03d5, 0x0410, 0x044e, 0x048f
                },
                {
                        0x0269, 0x028e, 0x02b4, 0x02de,
                        0x0309, 0x0337, 0x0368, 0x039c,
                        0x03d3, 0x040e, 0x044b, 0x048d
                }
        };
        FNUMBopm = new int[][] {
                {
                        0x0000, 0x0040, 0x0080, 0x00c0,
                        0x0100, 0x0140, 0x0180, 0x01c0,
                        0x0200, 0x0240, 0x0280, 0x02c0
                },
                {
                        0x0000 - 59 - 64, 0x0040 - 59 - 64, 0x0080 - 59 - 64, 0x00c0 - 59 - 64,
                        0x0100 - 59 - 64, 0x0140 - 59 - 64, 0x0180 - 59 - 64, 0x01c0 - 59 - 64,
                        0x0200 - 59 - 64, 0x0240 - 59 - 64, 0x0280 - 59 - 64, 0x02c0 - 59 - 64
                }
        };
        SNUMB = new int[][] {
                new int[] {
                        0x0ee8, 0x0e12, 0x0d48, 0x0c89,
                        0x0bd5, 0x0b2b, 0x0a8a, 0x09f3,
                        0x0964, 0x08dd, 0x085e, 0x07e6
                },
                new int[] {
                        0x0eee, 0x0e18, 0x0d4d, 0x0c8e,
                        0x0bda, 0x0b30, 0x0a8f, 0x09f7,
                        0x0968, 0x08e1, 0x0861, 0x07e9
                }
        };
        PCMNMB = new int[][] {
                // OPNA (7987200Hz) note:A is played at 8kHz (standard?)
                //  0x7BFE + 200 = 0x7CC6
                //  0x7CC6 >> 5 = 0x3E6(998)
                //  998 = 7987200Hz / 8000Hz
                new int[] {
                        0x49ba + 200, 0x4e1c + 200, 0x52c1 + 200, 0x57ad + 200,
                        0x5ce4 + 200, 0x626a + 200, 0x6844 + 200, 0x6e77 + 200,
                        0x7509 + 200, 0x7bfe + 200, 0x835e + 200, 0x8b2d + 200
                },
                // OPNB (8000000Hz)
                // C : 8000000 / 8000 * (261.626 / 440) * 32 = 19027.3454545454 (0x4A53)
                // C#: 8000000 / 8000 * (277.183 / 440) * 32 = 20158.7636363636 (0x4EBF)
                // D : 8000000 / 8000 * (293.665 / 440) * 32 = 21357.4545454545 (0x536D)
                // D#: 8000000 / 8000 * (311.127 / 440) * 32 = 22627.4181818182 (0x5863)
                // E : 8000000 / 8000 * (329.628 / 440) * 32 = 23972.9454545455 (0x5DA5)
                // F : 8000000 / 8000 * (349.228 / 440) * 32 = 25398.4          (0x6336)
                // F#: 8000000 / 8000 * (369.994 / 440) * 32 = 26908.6545454545 (0x691D)
                // G : 8000000 / 8000 * (391.995 / 440) * 32 = 28508.7272727273 (0x6F5D)
                // G#: 8000000 / 8000 * (415.305 / 440) * 32 = 30204            (0x75FC)
                // A : 8000000 / 8000 * (440.000 / 440) * 32 = 32000            (0x7D00)
                // A#: 8000000 / 8000 * (466.164 / 440) * 32 = 33902.8363636364 (0x846F)
                // B : 8000000 / 8000 * (493.883 / 440) * 32 = 35918.7636363636 (0x8C4F)
                new int[] {
                        0x4a53, 0x4ebf, 0x536d, 0x5863,
                        0x5da5, 0x6336, 0x691d, 0x6f5d,
                        0x75fc, 0x7d00, 0x846f, 0x8c4f
                }
        };

        SSGDAT = new int[] {
                255, 255, 255, 255, 0, 255, // E
                255, 255, 255, 200, 0, 10,
                255, 255, 255, 200, 1, 10,
                255, 255, 255, 190, 0, 10,
                255, 255, 255, 190, 1, 10,
                255, 255, 255, 170, 0, 10,
                40, 70, 14, 190, 0, 15,
                120, 030, 255, 255, 0, 10,
                255, 255, 255, 225, 8, 15,
                255, 255, 255, 1, 255, 255,
                255, 255, 255, 200, 8, 255,
                255, 255, 255, 220, 20, 8,
                255, 255, 255, 255, 0, 10,
                255, 255, 255, 255, 0, 10,
                120, 80, 255, 255, 0, 255,
                255, 255, 255, 220, 0, 255 // 6*16
        };
    }

    public static class CHDAT {
        public List<PGDAT> pgDat = new ArrayList<>();

        private int keyOnCh;

        public int getKeyOnCh() {
            return keyOnCh;
        }

        public void setKeyOnCh(int value) {
            keyOnCh = value;
        }

        private int currentPageNo;

        public int getCurrentPageNo() {
            return currentPageNo;
        }

        public void setCurrentPageNo(int value) {
            currentPageNo = value;
        }

        public int ch3KeyOn;

        public int getCh3KeyOn() {
            return ch3KeyOn;
        }

        private int fmVolMode = 0;

        public int getFmVolMode() {
            return fmVolMode;
        }

        public void setFmVolMode(int value) {
            fmVolMode = value;
        }

        private final int[] fmVolUserTable = new int[20];

        public int[] getFmVolUserTable() {
            return fmVolUserTable;
        }

        private int[] currentFMVolTable;

        public int[] getCurrentFMVolTable() {
            return currentFMVolTable;
        }

        public void setCurrentFMVolTable(int[] value) {
            currentFMVolTable = value;
        }

        public static class PGDAT {
            public MmlDatum[] mData = null;

            /** DB1 LENGTH counter IX+ 0 */
            public int lengthCounter = 1;
            /** DB24 tone number ｰ1 */
            public int instrumentNumber = 24;
            /** DW0 DATA ADDRES WORK2,3 */
            public int dataAddressWork = 0;
            /** DW0 DATA TOP ADDRES4,5 */
            public int dataTopAddress = -1;
            /** DB10 volume DATA6 */
            public int volume = 10;
            /**
             * bit 4 = attack flag
             * bit 5 = decay flag
             * bit 6 = sustain flag
             * bit 7 = soft envelope flag
             */
            public int softEnvelopeFlag = 0;
            /** DB0 Algorithm No. 7(FM) */
            public int algo = 0;
            public int feedback = 0;
            /** DB8 VOL.REG.No. 7 */
            public int volReg = 0;
            /** DB 0 Channel Number 8 */
            public int channelNumber = 0;
            /** DW0 Detune DATA9,10 */
            public int detune = 0;
            /** DB0 for TLLFO11 */
            public int TLlfo = 0;
            /** DB0 SOFT ENVE COUNTER11 */
            public int softEnvelopeCounter = 0;
            /** DB0 for Reverb 12 */
            public int reverb = 0;
//            /** DS5 SOFT ENVE DUMMY 13-17  */
//            public int[] softEnvelopeDummy = new int[5];
            /** SOFT ENVE12-17 KUMA: 12:AL 13:AR 14:DR 15:SR 16:SL 17:RR */
            public final int[] softEnvelopeParam = new int[6];
            /** rev vol? 17 */
            public int reverbVol = 0;
            /** DB0 Quantize 18 */
            public int quantize = 0;
            /** DB0 LFO DELAY19 */
            public int lfoDelay = 0;
            /** DB0 WORK20 */
            public int lfoDelayWork = 0;
            /** DB0 LFO COUNTER21 */
            public int lfoCounter = 0;
            /** DB0 WORK22 */
            public int lfoCounterWork = 0;
            /** DW0 LFO Amount of change 2BYTE23,24 */
            public int lfoDelta = 0;
            /** DW0 WORK25,26 */
            public int lfoDeltaWork = 0;
            /** DB0 LFO PEAK LEVEL27 */
            public int lfoPeak = 0;
            /** DB0 WORK28 */
            public int lfoPeakWork = 0;
            /** DB0 FNUM1 DATA29 */
            public int fnum = 0;
            /** DB0 B/FNUM2 DATA30 */
            public int bfnum2 = 0;
            /** DB00000001B bit7=LFO FLAG31 */
            public boolean lfoflg = false;
            /** bit6=KEYOFF FLAG */
            public boolean keyOffFlag = false;
            /** 5=LFO CONTINUE FLAG */
            public boolean lfoContFlg = false;
            /** 4=TIE FLAG */
            public boolean tieFlg = false;
            /** 3=MUTE FLAG */
            public boolean muteFlg = false;
            /** @since KUMA Externally controlled mute flag */
            public boolean silentFlg = false;
            /** 2=LFO 1SHOT FLAG */
            public boolean lfo1shotFlg = false;
            /** 0=1LOOPEND FLAG */
            public boolean loopEndFlg = false;

            /** DB 0 BEFORE CODE32 */
            public int beforeCode = 0;
            /** bit 7=HardEnvelope FLAG 33 */
            public boolean hardEnveFlg = false;
            /** DB0 bit6=TL LFO FLAG */
            public boolean tlLfoFlag = false;
            /** 5=REVERVE FLAG */
            public boolean reverbFlg = false;
            /** 4=REVERVE MODE */
            public boolean reverbMode = false;
            /** 0-3=hardware Envelope value */
            public int hardEnvelopValue = 0;
            /** DW0 return address 34,35 */
            public int returnAddress = 0;
            /** DB0,0 36,37 (reserved) */
            public int reserve = 0;

            /** DB ? ;pan 38 */
            public int panEnable = 0;
            /** DB ? ;pan mode 39 */
            public int panMode = 0;
            /** DB ? ;pan counter 40 */
            public int panCounterWork = 0;
            /** DB ? ;pan counter 41 */
            public int panCounter = 0;
            /** DB ? ;pan value 42 */
            public int panValue = 3;

            private boolean musicEnd;

            public boolean getMusicEnd() {
                return musicEnd;
            }

            public void setMusicEnd(boolean value) {
                musicEnd = value;
            }

            public int tlLfoSlot;

            public int getTlLfoSlot() {
                return tlLfoSlot;
            }

            private boolean ssgTremoloFlg;

            public boolean getSsgTremoloFlg() {
                return ssgTremoloFlg;
            }

            public void setSsgTremoloFlg(boolean value) {
                ssgTremoloFlg = false;
            }

            private int ssgTremoloVol;

            public int getSsgTremoloVol() {
                return ssgTremoloVol;
            }

            public void setSsgTremoloVol(int value) {
                ssgTremoloVol = value;
            }

            public void addSSGTremoloVol(int value) {
                ssgTremoloVol += value;
            }

            private int loopCounter;

            public int getLoopCounter() {
                return loopCounter;
            }

            public void incloopCounter() {
                loopCounter++;
            }

            private int pageNo;

            public int getPageNo() {
                return pageNo;
            }

            public void setPageNo(int value) {
                pageNo = value;
            }

            public boolean keyOnDelayFlag = false;
            /** for keyOnSlot control */
            public int keyOnSlot = 0xf0;
            public final int[] kd = new int[4];
            public final int[] kdWork = new int[4];
            /** Slots used by the page(bit) */
            public int useSlot = 0x0f;

            private int backupMIXPort = 0x38;

            public int getBackupMIXPort() {
                return backupMIXPort;
            }

            public void setBackupMIXPort(int value) {
                backupMIXPort = value;
            }

            private int backupNoiseFrq = 0;

            public int getBackupNoiseFrq() {
                return backupNoiseFrq;
            }

            public void setBackupNoiseFrq(int value) {
                backupNoiseFrq = value;
            }

            private int backupHardEnv = 0;

            public int getBackupHardEnv() {
                return backupHardEnv;
            }

            public void setBackupHardEnv(int value) {
                backupHardEnv = value;
            }

            private int backupHardEnvFine = 0;

            public int getBackupHardEnvFine() {
                return backupHardEnvFine;
            }

            public void setBackupHardEnvFine(int value) {
                backupHardEnvFine = value;
            }

            private int backupHardEnvCoarse = 0;

            public int getBackupHardEnvCoarse() {
                return backupHardEnvCoarse;
            }

            public void setBackupHardEnvCoarse(int value) {
                backupHardEnvCoarse = value;
            }

            private int[] tlDirectTable = new int[] {
                    255, 255, 255, 255
            };

            public int[] getTlDirectTable() {
                return tlDirectTable;
            }

            public void setTlDirectTable(int[] value) {
                tlDirectTable = value;
            }

            private int ssgWfNum = 0;

            public int getSsgWfNum() {
                return ssgWfNum;
            }

            public void setSsgWfNum(int value) {
                ssgWfNum = value;
            }

            public final int[] vTl = new int[] {
                    0, 0, 0, 0
            };

            // portamento processing

            // Work
            public boolean portaFlg = false;
            public boolean portaContFlg = false;
            public int portaWorkClock = 0;
            // Setting Value
            public int portaStNote = 0;
            public int portaEdNote = 0;
            public int portaTotalClock = 0;
            public double portaBeforeFNum = 0;
            public boolean enableKeyOff = true;
            public boolean useKeyOn = false;

            // Tone Gradation
            public boolean instrumentGradationSwitch = false;
            public int instrumentGradationWait = 0;
            public int instrumentGradationWaitCounter=0;
            public final int[] instrumentGradations = new int[2];
            public int instrumentGradationPointer = 0;
            public final int[] instrumentGradationSt = new int[42];
            public final int[] instrumentGradationEd = new int[42];
            public final int[] instrumentGradationWk = new int[42];
            public final boolean[] instrumentGradationFlg = new boolean[42];
            public boolean instrumentGradationReset = true;
        }
    }
}