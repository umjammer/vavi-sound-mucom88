package mucom88.driver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import musicDriverInterface.MmlDatum;


/**
 * オリジナルに存在するワークはここに定義する
 */
public class SoundWork {

    public List<List<CHDAT>> chData = Arrays.asList(
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

    public byte[][] pregBf = null;
    public byte[] initPm = null;
    public short[][] detdat = new short[][] {
            null, null, null, null
    };
    public byte[][] drmvol = new byte[][] {
            null, null, null, null
    };
    public byte[][] drmPanEnable = new byte[][] {
            null, null, null, null
    };
    public byte[][] drmPanMode = new byte[][] {
            null, null, null, null
    };
    public byte[][] drmPanCounter = new byte[][] {
            null, null, null, null
    };
    public byte[][] drmPanCounterWork = new byte[][] {
            null, null, null, null
    };
    public byte[][] drmPanValue = new byte[][] {
            null, null, null, null
    };
    public byte[] opSel = null;
    public byte[] TYPE1 = null;
    public byte[] TYPE2 = null;
    // DB 8
    public byte DMY = 0;
    public short[][] FNUMB = null;
    public short[][] FNUMBopm = null;
    public short[][] SNUMB = null;
    public short[][] PCMNMB = null;
    public byte[] SSGDAT = null;

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

    private int muTop = 5;

    public int getMuTop() {
        return muTop;
    }

    private byte timerB;

    public byte getTimerB() {
        return timerB;
    }

    public void setTimerB(byte value) {
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

    public boolean ch3SpMode(int chip) {
        return (PLSET1_VAL[chip] & 0x40) != 0;
    }

    public byte[] PLSET1_VAL = new byte[5];
    public byte[] PLSET2_VAL = new byte[5];

    private int[] pcmLr = new int[6];

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

    private int[] deltN = new int[4];

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

    private byte fPortVal = (byte) 0xa4;

    public byte getFPortVal() {
        return fPortVal;
    }

    public void setFPortVal(byte value) {
        fPortVal = value;
    }

    private byte pcmNum;

    public byte getPcmNum() {
        return pcmNum;
    }

    public void setPcmNum(byte value) {
        pcmNum = value;
    }

    private byte pOut;

    public byte getPOut() {
        return pOut;
    }

    public void setPOut(byte value) {
        pOut = value;
    }

    private int[] STTADR = new int[4];

    public int[] getSTTADR() {
        return STTADR;
    }

    private int[] ENDADR = new int[4];

    public int[] getENDADR() {
        return ENDADR;
    }

    private byte TOTALV;

    public byte getTOTALV() {
        return TOTALV;
    }

    private int OTODAT = 1;

    public int getOTODAT() {
        return OTODAT;
    }

    private byte LFOP6_VAL;

    public byte getLFOP6_VAL() {
        return LFOP6_VAL;
    }

    private byte FLGADR;

    public byte getFLGADR() {
        return FLGADR;
    }

    public void setFLGADR(byte value) {
        FLGADR = value;
    }

    private int NEWFNM;

    public int getNEWFNM() {
        return NEWFNM;
    }

    public void setNEWFNM(int value) {
        NEWFNM = value;
    }

    private short RANDUM = 0;

    public short getRANDUM() {
        return RANDUM;
    }

    public void setRANDUM(short value) {
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

    private int[][] PCMaSTTADR = new int[][] {
            new int[6], new int[6]
    };

    public int[][] getPCMaSTTADR() {
        return PCMaSTTADR;
    }

    public int[][] PCMaENDADR = new int[][] {
            new int[6], new int[6]
    };

    public int[][] getPCMaENDADR() {
        return PCMaENDADR;
    }

    /** PMS/AMS/LR DATA */
    public byte[] PALDAT = new byte[] {
            (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0,
            (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0,
            (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // DUMMY
            (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0,
            (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0,
            (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0
    };

    // ﾎﾞﾘｭｰﾑ ﾃﾞｰﾀ

    public static final byte[] FMVDAT = new byte[] { // ﾎﾞﾘｭｰﾑ ﾃﾞｰﾀ(FM)
            0x36, 0x33, 0x30, 0x2D,
            0x2A, 0x28, 0x25, 0x22, //  0,  1,  2,  3
            0x20, 0x1D, 0x1A, 0x18, //  4,  5,  6,  7
            0x15, 0x12, 0x10, 0x0D, //  8,  9, 10, 11
            0x0a, 0x08, 0x05, 0x02  // 12, 13, 14, 15
    };

    public static final byte[] CRYDAT = new byte[] { // ｷｬﾘｱ / ﾓｼﾞｭﾚｰﾀ ﾉ ﾃﾞｰﾀ
            0x08,
            0x08, // ｶｸ ﾋﾞｯﾄ ｶﾞ ｷｬﾘｱ/ﾓｼﾞｭﾚｰﾀ ｦ ｱﾗﾜｽ
            0x08, //
            0x08, // Bit=1 ｶﾞ ｷｬﾘｱ
            0x0C, //     0 ｶﾞ ﾓｼﾞｭﾚｰﾀ
            0x0E, //
            0x0E, // Bit0=OP 1 , Bit1=OP 2 ... etc
            0x0F
    };

    void init() {
        for (int chipIndex = 0; chipIndex < 4; chipIndex++) {
            for (int i = 0; i < chData.get(chipIndex).get(0).pgDat.size(); i++) {
                chData.get(chipIndex).get(0).pgDat.get(i).lengthCounter = 1;
                chData.get(chipIndex).get(0).pgDat.get(i).instrumentNumber = 24;
                chData.get(chipIndex).get(0).pgDat.get(i).volume = 10;
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


        pregBf = new byte[][] {
                new byte[9], new byte[9], new byte[9], new byte[9]
        };
        initPm = new byte[] {0, 0, 0, 0, 0, 56, 0, 0, 0};
        for (int i = 0; i < 4; i++) {
            detdat[i] = new short[] {
                    0, 0, 0, 0
            };
            drmvol[i] = new byte[] {
                    (byte) 0xc0, (byte) 0xc0, (byte) 0xc0, (byte) 0xc0, (byte) 0xc0, (byte) 0xc0
            };
            drmPanCounter[i] = new byte[] {
                    0, 0, 0, 0, 0, 0
            };
            drmPanCounterWork[i] = new byte[] {
                    0, 0, 0, 0, 0, 0
            };
            drmPanEnable[i] = new byte[] {
                    0, 0, 0, 0, 0, 0
            };
            drmPanMode[i] = new byte[] {
                    0, 0, 0, 0, 0, 0
            };
            drmPanValue[i] = new byte[] {
                    0, 0, 0, 0, 0, 0
            };
        }
        opSel = new byte[] {
                (byte) 0xa6, (byte) 0xac, (byte) 0xad, (byte) 0xae
        };
        DMY = 8;
        TYPE1 = new byte[] {0x032, 0x044, 0x046};
        TYPE2 = new byte[] {(byte) 0x0AA, (byte) 0x0A8, (byte) 0x0AC};
        FNUMB = new short[][] {
                new short[] {
                        0x026A, 0x028F, 0x02B6, 0x02DF,
                        0x030B, 0x0339, 0x036A, 0x039E,
                        0x03D5, 0x0410, 0x044E, 0x048F
                },
                new short[] {
                        0x0269, 0x028E, 0x02b4, 0x02De,
                        0x0309, 0x0337, 0x0368, 0x039c,
                        0x03d3, 0x040e, 0x044b, 0x048d
                }
        };
        FNUMBopm = new short[][] {
                new short[] {
                        0x0000, 0x0040, 0x0080, 0x00c0,
                        0x0100, 0x0140, 0x0180, 0x01c0,
                        0x0200, 0x0240, 0x0280, 0x02c0
                },
                new short[] {
                        0x0000 - 59 - 64, 0x0040 - 59 - 64, 0x0080 - 59 - 64, 0x00c0 - 59 - 64,
                        0x0100 - 59 - 64, 0x0140 - 59 - 64, 0x0180 - 59 - 64, 0x01c0 - 59 - 64,
                        0x0200 - 59 - 64, 0x0240 - 59 - 64, 0x0280 - 59 - 64, 0x02c0 - 59 - 64
                }
        };
        SNUMB = new short[][] {
                new short[] {
                        0x0EE8, 0x0E12, 0x0D48, 0x0C89,
                        0x0BD5, 0x0B2B, 0x0A8A, 0x09F3,
                        0x0964, 0x08DD, 0x085E, 0x07E6
                },
                new short[] {
                        0x0EEe, 0x0E18, 0x0D4d, 0x0C8e,
                        0x0BDa, 0x0B30, 0x0A8f, 0x09F7,
                        0x0968, 0x08e1, 0x0861, 0x07E9
                }
        };
        PCMNMB = new short[][] {
                // OPNA (7987200Hz) note:Aを8kHz(基準?)で再生
                //  0x7BFE+200 = 0x7CC6
                //  0x7CC6 >> 5 =0x3E6(998)
                //  998 = 7987200Hz / 8000Hz
                new short[] {
                        0x49BA + 200, 0x4E1C + 200, 0x52C1 + 200, 0x57AD + 200,
                        0x5CE4 + 200, 0x626A + 200, 0x6844 + 200, 0x6E77 + 200,
                        0x7509 + 200, 0x7BFE + 200, (short) (0x835E + 200), (short) (0x8B2D + 200)
                },
                // OPNB (8000000Hz)
                // C :8000000/8000*(261.626/440)*32=19027.3454545454(0x4A53)
                // C#:8000000/8000*(277.183/440)*32=20158.7636363636(0x4EBF)
                // D :8000000/8000*(293.665/440)*32=21357.4545454545(0x536D)
                // D#:8000000/8000*(311.127/440)*32=22627.4181818182(0x5863)
                // E :8000000/8000*(329.628/440)*32=23972.9454545455(0x5DA5)
                // F :8000000/8000*(349.228/440)*32=25398.4         (0x6336)
                // F#:8000000/8000*(369.994/440)*32=26908.6545454545(0x691D)
                // G :8000000/8000*(391.995/440)*32=28508.7272727273(0x6F5D)
                // G#:8000000/8000*(415.305/440)*32=30204           (0x75FC)
                // A :8000000/8000*(440.000/440)*32=32000           (0x7D00)
                // A#:8000000/8000*(466.164/440)*32=33902.8363636364(0x846F)
                // B :8000000/8000*(493.883/440)*32=35918.7636363636(0x8C4F)
                new short[] {
                        0x4A53, 0x4EBF, 0x536D, 0x5863,
                        0x5DA5, 0x6336, 0x691D, 0x6F5D,
                        0x75FC, 0x7D00, (short) 0x846F, (short) 0x8C4F
                }
        };

        SSGDAT = new byte[] {
                (byte) 255, (byte) 255, (byte) 255, (byte) 255, 0, (byte) 255, // E
                (byte) 255, (byte) 255, (byte) 255, (byte) 200, 0, 10,
                (byte) 255, (byte) 255, (byte) 255, (byte) 200, 1, 10,
                (byte) 255, (byte) 255, (byte) 255, (byte) 190, 0, 10,
                (byte) 255, (byte) 255, (byte) 255, (byte) 190, 1, 10,
                (byte) 255, (byte) 255, (byte) 255, (byte) 170, 0, 10,
                40, 70, 14, (byte) 190, 0, 15,
                120, 030, (byte) 255, (byte) 255, 0, 10,
                (byte) 255, (byte) 255, (byte) 255, (byte) 225, 8, 15,
                (byte) 255, (byte) 255, (byte) 255, 1, (byte) 255, (byte) 255,
                (byte) 255, (byte) 255, (byte) 255, (byte) 200, 8, (byte) 255,
                (byte) 255, (byte) 255, (byte) 255, (byte) 220, 20, 8,
                (byte) 255, (byte) 255, (byte) 255, (byte) 255, 0, 10,
                (byte) 255, (byte) 255, (byte) 255, (byte) 255, 0, 10,
                120, 80, (byte) 255, (byte) 255, 0, (byte) 255,
                (byte) 255, (byte) 255, (byte) 255, (byte) 220, 0, (byte) 255 // 6*16
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

        private byte fmVolMode = 0;

        public byte getFmVolMode() {
            return fmVolMode;
        }

        public void setFmVolMode(byte value) {
            fmVolMode = value;
        }

        private byte[] fmVolUserTable = new byte[20];

        public byte[] getFmVolUserTable() {
            return fmVolUserTable;
        }

        private byte[] currentFMVolTable;

        public byte[] getCurrentFMVolTable() {
            return currentFMVolTable;
        }

        public void setCurrentFMVolTable(byte[] value) {
            currentFMVolTable = value;
        }

        public static class PGDAT {
            public MmlDatum[] mData = null;

            /** DB1 LENGTH ｶｳﾝﾀｰ IX+ 0 */
            public int lengthCounter = 1;
            /** DB24 ｵﾝｼｮｸ ﾅﾝﾊﾞｰ1 */
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
            /** DB0 ｱﾙｺﾞﾘｽﾞﾑ No. 7(FM) */
            public int algo = 0;
            public int feedback = 0;
            /** DB8 VOL.REG.No. 7 */
            public int volReg = 0;
            /** DB 0 ﾁｬﾝﾈﾙ ﾅﾝﾊﾞｰ 8 */
            public int channelNumber = 0;
            /** DW0 ﾃﾞﾁｭｰﾝ DATA9,10 */
            public int detune = 0;
            /** DB0  for TLLFO11 */
            public int TLlfo = 0;
            /** DB0 SOFT ENVE COUNTER11 */
            public int softEnvelopeCounter = 0;
            /** DB0 for ﾘﾊﾞｰﾌﾞ12 */
            public int reverb = 0;
            /** DS5 SOFT ENVE DUMMY13-17  */
//            public int[] softEnvelopeDummy = new int[5];
            /** SOFT ENVE12-17 KUMA: 12:AL 13:AR 14:DR 15:SR 16:SL 17:RR */
            public int[] softEnvelopeParam = new int[6];
            /** rev vol? 17 */
            public int reverbVol = 0;
            /** DB0 qｵﾝﾀｲｽﾞ18 */
            public int quantize = 0;
            /** DB0 LFO DELAY19 */
            public int lfoDelay = 0;
            /** DB0 WORK20 */
            public int lfoDelayWork = 0;
            /** DB0 LFO COUNTER21 */
            public int lfoCounter = 0;
            /** DB0 WORK22 */
            public int lfoCounterWork = 0;
            /** DW0 LFO ﾍﾝｶﾘｮｳ 2BYTE23,24 */
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
            /** KUMA:外部から操作されるmuteフラグ */
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
            public byte hardEnvelopValue = 0;
            /** DW0 ﾘﾀｰﾝｱﾄﾞﾚｽ34,35 */
            public int returnAddress = 0;
            /** DB0,0 36,37 (ｱｷ) */
            public int reserve = 0;

            /** DB ? ;パーン 38 */
            public byte panEnable = 0;
            /** DB ? ;パーン モード 39 */
            public byte panMode = 0;
            /** DB ? ;パーン カウンター 40 */
            public byte panCounterWork = 0;
            /** DB ? ;パーン カウンター 41 */
            public byte panCounter = 0;
            /** DB ? ;パーン 値 42 */
            public byte panValue = 3;

            private boolean musicEnd;

            public boolean getMusicEnd() {
                return musicEnd;
            }

            public void setMusicEnd(boolean value) {
                musicEnd = value;
            }

            public byte tlLfoSlot;

            public byte getTlLfoSlot() {
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
            /** keyOnSlot制御向け */
            public byte keyOnSlot = (byte) 0xf0;
            public byte[] kd = new byte[4];
            public byte[] kdWork = new byte[4];
            /** ページが使用するスロット(bit) */
            public byte useSlot = 0x0f;

            private byte backupMIXPort = 0x38;

            public byte getBackupMIXPort() {
                return backupMIXPort;
            }

            public void setBackupMIXPort(byte value) {
                backupMIXPort = value;
            }

            private byte backupNoiseFrq = 0;

            public byte getBackupNoiseFrq() {
                return backupNoiseFrq;
            }

            public void setBackupNoiseFrq(byte value) {
                backupNoiseFrq = value;
            }

            private byte backupHardEnv = 0;

            public byte getBackupHardEnv() {
                return backupHardEnv;
            }

            public void setBackupHardEnv(byte value) {
                backupHardEnv = value;
            }

            private byte backupHardEnvFine = 0;

            public byte getBackupHardEnvFine() {
                return backupHardEnvFine;
            }

            public void setBackupHardEnvFine(byte value) {
                backupHardEnvFine = value;
            }

            private byte backupHardEnvCoarse = 0;

            public byte getBackupHardEnvCoarse() {
                return backupHardEnvCoarse;
            }

            public void setBackupHardEnvCoarse(byte value) {
                backupHardEnvCoarse = value;
            }

            private byte[] tlDirectTable = new byte[] {
                    (byte) 255, (byte) 255, (byte) 255, (byte) 255
            };

            public byte[] getTlDirectTable() {
                return tlDirectTable;
            }

            public void setTlDirectTable(byte[] value) {
                tlDirectTable = value;
            }

            private int ssgWfNum = 0;

            public int getSsgWfNum() {
                return ssgWfNum;
            }

            public void setSsgWfNum(int value) {
                ssgWfNum = value;
            }

            public byte[] vTl = new byte[] {
                    0, 0, 0, 0
            };

            // portamento処理

            // Work
            public boolean portaFlg = false;
            public boolean portaContFlg = false;
            public int portaWorkClock = 0;
            // 設定値
            public int portaStNote = 0;
            public int portaEdNote = 0;
            public int portaTotalClock = 0;
            public double portaBeforeFNum = 0;
            public boolean enableKeyOff = true;
        }
    }
}