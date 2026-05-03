package mucom88.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import mucom88.common.MUCInfo;
import mucom88.common.MucException;


public class Expand {

    static final ResourceBundle rb = ResourceBundle.getBundle("mucom88/message");

    public static final String NumPattern = "0123456789+-.$abcdefABCDEF";

    private final Work work;
    private final MUCInfo mucInfo;
    public Msub msub = null;
    public SMon smon = null;
    public Muc88 muc88 = null;

    public static final short[][] FNUMB = {
            {
                    0x026A, 0x028F, 0x02B6, 0x02DF,
                    0x030B, 0x0339, 0x036A, 0x039E,
                    0x03D5, 0x0410, 0x044E, 0x048F
            },
            {
                    0x0269, 0x028E, 0x02b4, 0x02De,
                    0x0309, 0x0337, 0x0368, 0x039c,
                    0x03d3, 0x040e, 0x044b, 0x048d
            }
    };
    public static final short[][] SNUMB = {
            {
                    0x0EE8, 0x0E12, 0x0D48, 0x0C89,
                    0x0BD5, 0x0B2B, 0x0A8A, 0x09F3,
                    0x0964, 0x08DD, 0x085E, 0x07E6
            },
            {
                    0x0EEe, 0x0E18, 0x0D4d, 0x0C8e,
                    0x0BDa, 0x0B30, 0x0A8f, 0x09F7,
                    0x0968, 0x08e1, 0x0861, 0x07E9
            }
    };
    public static final short[][] FNUMBopm = {
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

    public Expand(Work work, MUCInfo mucInfo) {
        this.work = work;
        this.mucInfo = mucInfo;
    }

    private boolean warningToneFormatFlag = false;

    public void FVTEXT(int vn) {
        int fvfg;
        int fmlib1 = 1; // 0x6001;
        boolean found = false;
        boolean warningFlag;

        for (int i = 0; i < mucInfo.getBasSrc().size(); i++) {
            if (mucInfo.getBasSrc().get(i) == null) continue;
            if (mucInfo.getBasSrc().get(i).getItem2() == null) continue;
            if (mucInfo.getBasSrc().get(i).getItem2().length() < 4) continue;
            if (mucInfo.getBasSrc().get(i).getItem2().charAt(0) != ' ') continue;
            if (mucInfo.getBasSrc().get(i).getItem2().charAt(2) != '@') continue;

            int[] srcCPtr = new int[] {3};
            fvfg = '\0';
            if (mucInfo.getBasSrc().get(i).getItem2().charAt(srcCPtr[0]) == '%') {
                srcCPtr[0]++;
                fvfg = '%';
            } else if (mucInfo.getBasSrc().get(i).getItem2().charAt(srcCPtr[0]) == 'M' || mucInfo.getBasSrc().get(i).getItem2().charAt(srcCPtr[0]) == 'm') {
                srcCPtr[0]++;
                fvfg = 'M';
            }

            int n = msub.readData(mucInfo.getBasSrc().get(i), /* ref */ srcCPtr);
            if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                muc88.writeWarning(rb.getString("W0409"), i, srcCPtr[0]);
            }
            if (n != vn) continue;

            found = true;

            if (fvfg == '%') {
                // reading when %(25bytes format)
                for (int row = 0; row < 6; row++) {
                    i++;
                    srcCPtr[0] = 1;
                    for (int col = 0; col < 4; col++) {
                        int v = msub.readData(mucInfo.getBasSrc().get(i), /* ref */ srcCPtr);
                        if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                            if (!warningToneFormatFlag) muc88.writeWarning(rb.getString("W0409"), i, srcCPtr[0]);
                            warningToneFormatFlag = true;
                        }
                        if (skipSpaceAndTab(i, /* ref */ srcCPtr)) {
                            if (!warningToneFormatFlag) muc88.writeWarning(rb.getString("W0800"), i, srcCPtr[0]); // There is a possibility that it cannot be loaded by mucom88.
                            warningToneFormatFlag = true;
                        }
                        if (NumPattern.indexOf(getMoji(i, srcCPtr[0])) < 0)
                            srcCPtr[0]++; // SKIP','
                        mucInfo.getMmlVoiceDataWork().set(fmlib1++, (byte) (v & 0xff));
                    }
                }

                i++;
                srcCPtr[0] = 2;
                mucInfo.getMmlVoiceDataWork().set(fmlib1, (byte) (msub.readData(mucInfo.getBasSrc().get(i), /* ref */ srcCPtr) & 0xff));
            } else if (fvfg == 'M') {
                // reading when 42 bytes basic format

                List<Byte> voi = new ArrayList<>();

                i++;
                srcCPtr[0] = 2;
                int fb = msub.readData(mucInfo.getBasSrc().get(i), /* ref */ srcCPtr);
                if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                    muc88.writeWarning(rb.getString("W0409"), i, srcCPtr[0]);
                }
                srcCPtr[0]++;
                int alg = msub.readData(mucInfo.getBasSrc().get(i), /* ref */ srcCPtr);
                if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                    muc88.writeWarning(rb.getString("W0409"), i, srcCPtr[0]);
                }
                srcCPtr[0]++;

                voi.add((byte) (fb & 0xff));
                voi.add((byte) (alg & 0xff));

                for (int row = 0; row < 4; row++) {
                    i++;
                    srcCPtr[0] = 1;
                    for (int col = 0; col < 10; col++) {
                        int v = msub.readData(mucInfo.getBasSrc().get(i), /* ref */ srcCPtr);
                        if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                            muc88.writeWarning(rb.getString("W0409"), i, srcCPtr[0]);
                        }
                        if (skipSpaceAndTab(i, /* ref */ srcCPtr)) {
                            if (!warningToneFormatFlag) muc88.writeWarning(rb.getString("W0800"), i, srcCPtr[0]); // There is a possibility that it cannot be loaded by mucom88
                            warningToneFormatFlag = true;
                        }
                        if (NumPattern.indexOf(getMoji(i, srcCPtr[0])) < 0)
                            srcCPtr[0]++; // skip ','
                        voi.add((byte) (v & 0xff));
                    }
                }

                smon.converToPM(voi); // 42 BYTE -> 25 BYTE
            } else {
                // reading 38 bytes basic format

                i++;
                srcCPtr[0] = 2;
                int fb = msub.readData(mucInfo.getBasSrc().get(i), /* ref */ srcCPtr);
                if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                    muc88.writeWarning(rb.getString("W0409"), i, srcCPtr[0]);
                }
                srcCPtr[0]++;
                int alg = msub.readData(mucInfo.getBasSrc().get(i), /* ref */ srcCPtr);
                if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                    muc88.writeWarning(rb.getString("W0409"), i, srcCPtr[0]);
                }
                srcCPtr[0]++;

                for (int row = 0; row < 4; row++) {
                    i++;
                    srcCPtr[0] = 1;
                    for (int col = 0; col < 9; col++) {
                        int v = msub.readData(mucInfo.getBasSrc().get(i), /* ref */ srcCPtr);
                        if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                            muc88.writeWarning(rb.getString("W0409"), i, srcCPtr[0]);
                        }

                        if (skipSpaceAndTab(i, /* ref */ srcCPtr)) {
                            if (!warningToneFormatFlag) muc88.writeWarning(rb.getString("W0800"), i, srcCPtr[0]); // There is a possibility that it cannot be loaded by mucom88
                            warningToneFormatFlag = true;
                        }
                        if (NumPattern.indexOf(getMoji(i, srcCPtr[0])) < 0)
                            srcCPtr[0]++; // skip ','

                        mucInfo.getMmlVoiceDataWork().set(fmlib1++, (byte) (v & 0xff));
                    }
                }
                mucInfo.getMmlVoiceDataWork().set(fmlib1++, (byte) (fb & 0xff));
                mucInfo.getMmlVoiceDataWork().set(fmlib1, (byte) (alg & 0xff));
                // Z80.HL = 0x6001;
                smon.CONVERT(); // 38BYTE->25BYTE
            }

            break;
        }

        mucInfo.setCarry(false);
        if (!found) mucInfo.setCarry(true);
    }

    public void SSGTEXT() {
        for (int[] i = new int[] {0}; i[0] < mucInfo.getBasSrc().size(); i[0]++) {
            if (mucInfo.getBasSrc().get(i[0]) == null) continue;
            if (mucInfo.getBasSrc().get(i[0]).getItem2() == null) continue;
            if (mucInfo.getBasSrc().get(i[0]).getItem2().length() < 4) continue;
            if (mucInfo.getBasSrc().get(i[0]).getItem2().charAt(0) != ' ') continue;
            if (mucInfo.getBasSrc().get(i[0]).getItem2().charAt(2) != '@') continue;

            int[] srcCPtr = new int[] {3};
            if (mucInfo.getBasSrc().get(i[0]).getItem2().charAt(srcCPtr[0]) == 'W' ||
                    mucInfo.getBasSrc().get(i[0]).getItem2().charAt(srcCPtr[0]) == 'w') {
                srcCPtr[0]++;
                SSGWaveDefine(/* ref */ i, /* ref */ srcCPtr);
            }
        }

        work.getUseSSGVoice().clear();
    }

    private void SSGWaveDefine(/* ref */ int[] srcRow, /* ref */ int[] srcCPtr) {
        // Get the definition number
        int n = msub.readData(mucInfo.getBasSrc().get(srcRow[0]), /* ref */ srcCPtr);
        if (mucInfo.getCarry() || mucInfo.getErrSign()) {
            muc88.writeWarning(rb.getString("E0800"), srcRow[0], srcCPtr[0]); // The format is invalid. Check for spaces and commas.
        }
        if (!work.getUseSSGVoice().contains(n)) return;

        byte[] v = new byte[64];
        for (int row = 0; row < 4; row++) {

            srcRow[0]++;
            if (mucInfo.getBasSrc().size() == srcRow[0]) {
                throw new MucException(rb.getString("E0800"), srcRow[0], srcCPtr[0]); // The format is invalid. Check for spaces and commas.
            }

            srcCPtr[0] = 1;
            for (int col = 0; col < 16; col++) {
                v[row * 16 + col] = (byte) (msub.readData(mucInfo.getBasSrc().get(srcRow[0]), /* ref */ srcCPtr) & 0xff);
                if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                    throw new MucException(rb.getString("E0800"), srcRow[0], srcCPtr[0]); // The format is invalid. Check for spaces and commas.
                }

                if (skipSpaceAndTab(srcRow[0], /* ref */ srcCPtr)) {
                    if (!warningToneFormatFlag) muc88.writeWarning(rb.getString("W0800"), srcRow[0], srcCPtr[0]); // There is a possibility that it cannot be loaded by mucom88
                    warningToneFormatFlag = true;
                }
                if (NumPattern.indexOf(getMoji(srcRow[0], srcCPtr[0])) < 0)
                    srcCPtr[0]++;// SKIP','
            }
        }

        mucInfo.getSsgVoice().remove(n);
        mucInfo.getSsgVoice().put(n, v);
    }

    private boolean skipSpaceAndTab(int srcRow, /* ref */ int[] srcCPtr) {
        boolean ret = false;
        char c = getMoji(srcRow, srcCPtr[0]);

        while (c == ' ' || c == 0x9) {
            srcCPtr[0]++;
            ret = true;
            c = getMoji(srcRow, srcCPtr[0]);
        }

        return ret;
    }

    private char getMoji(int srcRow, int srcCPtr) {
        char c = srcCPtr < mucInfo.getBasSrc().get(srcRow).getItem2().length()
                ? mucInfo.getBasSrc().get(srcRow).getItem2().charAt(srcCPtr)
                : (char) 0;
        return c;
    }

    /**
     * Portamento Calculation.
     * IN:HL<={CG} then G's text ADR
     * EXIT:DE<=The third change in the M command
     * Z flag = 1, nothing changed
     */
    public int CULPTM(int chipIndex, int startNote, int endNote, int clk) {
        int depth = CULP2Ex(chipIndex, startNote, endNote) / (clk >> 0);

        if (!mucInfo.getCarry()) return depth;
        mucInfo.setCarry(false);
        return -depth; // RET
    }

    public double CULPTMex(int chipIndex, int startNote, int endNote, int clk) {
        double depth = CULP2Ex(chipIndex, startNote, endNote) / (double) clk;

        if (!mucInfo.getCarry()) return depth;
        mucInfo.setCarry(false);
        return -depth; // RET
    }

    public int getEndNote() {
        int DE = work.mData;
        int endNote = msub.STTONE();
        work.mData = DE;
        if (mucInfo.getCarry()) {
            mucInfo.setCarry(true); // SCF
            return 0; // RET
        }
        return endNote;
    }

    public int getDiffNote(int startNote, int endNote) {
        return ctone(endNote) - ctone(startNote);
    }

    private int CULP2Ex(int chipIndex, int startNote, int endNote) {
        int HL;
        boolean up;

        // EXIT:HL <= Range of change
        // CY is a falling waveform
        // No change if Z

        boolean CULP2_Ptn = false;
        Muc88.ChannelType tp = muc88.CHCHK();
        if (tp == Muc88.ChannelType.SSG) {
            CULP2_Ptn = true;
        }

        int C = startNote & 0x0F; // KEY
        if (!CULP2_Ptn) {
            if (chipIndex < 2) work.setFrqBef(FNUMB[0][C]);
            else if (chipIndex < 4) work.setFrqBef(FNUMB[1][C]);
            else work.setFrqBef(FNUMBopm[0][C]);
        } else {
            if (chipIndex < 2) work.setFrqBef(SNUMB[0][C]);
            else if (chipIndex < 4) work.setFrqBef(SNUMB[1][C]);
        }

        int noteNum = ctone(endNote) - ctone(startNote);
        if (noteNum == 0) return 0; // No change
        if (noteNum >= 0) {
            up = !CULP2_Ptn;
        } else {
            noteNum = -noteNum;
            up = CULP2_Ptn;
        }

        if (up) {
            // BEFTONE < NOWTONE (rising)
            HL = culc(1.059463f, work.getFrqBef(), noteNum) - work.getFrqBef();
            mucInfo.setCarry(false);
            return HL;
        }

        // BEFTONE > NOWTONE (falling)
        HL = work.getFrqBef() - culc(0.943874f, work.getFrqBef(), noteNum);
        mucInfo.setCarry(true);
        return HL;
    }

    /**
     * Portamento Calculation.
     * IN:HL<={CG} then G's text ADR
     * EXIT:DE<=The third change in the M command
     * Z flag = 1, nothing changed
     */
    public int CULPTM(int chipIndex) {
        int DE = work.mData;
        int note = msub.STTONE();
        work.mData = DE;
        if (mucInfo.getCarry()) {
            mucInfo.setCarry(true); // SCF
            return 0; // RET
        }

        int depth = CULP2(chipIndex, note) / work.getBefCo(); // Mem.LD_8(BEFCO + 1); ?

        if (!mucInfo.getCarry()) return depth;
        mucInfo.setCarry(false);
        return -depth; // RET
    }

    private int CULP2(int chipIndex, int note) {
        int HL;
        boolean up;

        // EXIT:HL <= Range of change
        // CY is a falling waveform
        // No change if Z

        boolean CULP2_Ptn = false;
        Muc88.ChannelType tp = muc88.CHCHK();
        if (tp == Muc88.ChannelType.SSG) {
            CULP2_Ptn = true;
        }

        int C = work.getBeforeTone()[0] & 0x0F; // KEY
        if (!CULP2_Ptn) {
            if (chipIndex < 2) work.setFrqBef(FNUMB[0][C]);
            else if (chipIndex < 4) work.setFrqBef(FNUMB[1][C]);
            else work.setFrqBef(FNUMBopm[0][C]);
        } else {
            if (chipIndex < 2) work.setFrqBef(SNUMB[0][C]);
            else if (chipIndex < 4) work.setFrqBef(SNUMB[1][C]);
        }

        int noteNum = ctone(note) - ctone(work.getBeforeTone()[0]);
        if (noteNum == 0) return 0; // No change
        if (noteNum >= 0) {
            up = !CULP2_Ptn;
        } else {
            noteNum = -noteNum;
            up = CULP2_Ptn;
        }

        if (up) {
            // BEFTONE < NOWTONE (rising)
            HL = culc(1.059463f, work.getFrqBef(), noteNum) - work.getFrqBef();
            mucInfo.setCarry(false);
            return HL;
        }

        // BEFTONE > NOWTONE (falling)
        HL = work.getFrqBef() - culc(0.943874f, work.getFrqBef(), noteNum);
        mucInfo.setCarry(true);
        return HL;
    }

    private static int culc(float facc, int frq, int amul) {
        float frqbef = frq;
        for (int count = 0; count < amul; count++) {
            frqbef *= facc;
        }
        return (int) frqbef;
    }

    public int ctone(int a) {
        return ((a & 0x0f) + ((a & 0xf0) >> 4) * 12);
    }
}
