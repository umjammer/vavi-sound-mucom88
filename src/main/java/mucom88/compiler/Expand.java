package mucom88.compiler;

import java.util.ArrayList;
import java.util.List;

import mucom88.common.MUCInfo;
import mucom88.common.Message;


public class Expand {

    private Work work;
    private MUCInfo mucInfo;
    public Msub msub = null;
    public SMon smon = null;
    public Muc88 muc88 = null;

    public short[][] FNUMB = new short[][] {
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
    public short[][] SNUMB = new short[][] {
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
    public short[][] FNUMBopm = new short[][] {
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

    public Expand(Work work, MUCInfo mucInfo) {
        this.work = work;
        this.mucInfo = mucInfo;
    }

    public void FVTEXT(int vn) {
        int fvfg;
        int fmlib1 = 1;// 0x6001;
        boolean found = false;

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

            int n = msub.REDATA(mucInfo.getBasSrc().get(i), /*ref*/ srcCPtr);
            if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                muc88.WriteWarning(Message.get("W0409"), i, srcCPtr[0]);
            }
            if (n != vn) continue;

            found = true;

            if (fvfg == '%') {
                // ---%(25BYTEｼｷ) ﾉ ﾄｷ ﾉ ﾖﾐｺﾐ---

                for (int row = 0; row < 6; row++) {
                    i++;
                    srcCPtr[0] = 1;
                    for (int col = 0; col < 4; col++) {
                        byte v = (byte) msub.REDATA(mucInfo.getBasSrc().get(i), /*ref*/ srcCPtr);
                        if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                            muc88.WriteWarning(Message.get("W0409"), i, srcCPtr[0]);
                        }
                        srcCPtr[0]++;// SKIP','
                        mucInfo.getMmlVoiceDataWork().set(
                                fmlib1++
                                , v
                        );
                    }
                }

                i++;
                srcCPtr[0] = 2;
                mucInfo.getMmlVoiceDataWork().set(fmlib1, (byte) msub.REDATA(mucInfo.getBasSrc().get(i), /*ref*/ srcCPtr));
            } else if (fvfg == 'M') {
                // 42 ﾊﾞｲﾄﾍﾞｰｼｯｸﾎｳｼｷﾉﾄｷﾉ ﾖﾐｺﾐ

                List<Byte> voi = new ArrayList<>();

                i++;
                srcCPtr[0] = 2;
                int fb = msub.REDATA(mucInfo.getBasSrc().get(i), /*ref*/ srcCPtr);
                if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                    muc88.WriteWarning(Message.get("W0409"), i, srcCPtr[0]);
                }
                srcCPtr[0]++;
                int alg = msub.REDATA(mucInfo.getBasSrc().get(i), /*ref*/ srcCPtr);
                if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                    muc88.WriteWarning(Message.get("W0409"), i, srcCPtr[0]);
                }
                srcCPtr[0]++;

                voi.add((byte) fb);
                voi.add((byte) alg);

                for (int row = 0; row < 4; row++) {
                    i++;
                    srcCPtr[0] = 1;
                    for (int col = 0; col < 10; col++) {
                        byte v = (byte) msub.REDATA(mucInfo.getBasSrc().get(i), /*ref*/ srcCPtr);
                        if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                            muc88.WriteWarning(Message.get("W0409"), i, srcCPtr[0]);
                        }
                        srcCPtr[0]++; // skip ','
                        voi.add((byte) v);
                    }
                }

                smon.converToPM(voi); // 42 BYTE -> 25 BYTE
            } else {
                // 38 ﾊﾞｲﾄﾍﾞｰｼｯｸﾎｳｼｷﾉﾄｷﾉ ﾖﾐｺﾐ

                i++;
                srcCPtr[0] = 2;
                int fb = msub.REDATA(mucInfo.getBasSrc().get(i), /*ref*/ srcCPtr);
                if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                    muc88.WriteWarning(Message.get("W0409"), i, srcCPtr[0]);
                }
                srcCPtr[0]++;
                int alg = msub.REDATA(mucInfo.getBasSrc().get(i), /*ref*/ srcCPtr);
                if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                    muc88.WriteWarning(Message.get("W0409"), i, srcCPtr[0]);
                }
                srcCPtr[0]++;

                for (int row = 0; row < 4; row++) {
                    i++;
                    srcCPtr[0] = 1;
                    for (int col = 0; col < 9; col++) {
                        byte v = (byte) msub.REDATA(mucInfo.getBasSrc().get(i), /*ref*/ srcCPtr);
                        if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                            muc88.WriteWarning(Message.get("W0409"), i, srcCPtr[0]);
                        }
                        srcCPtr[0]++; // skip ','
                        mucInfo.getMmlVoiceDataWork().set(
                                fmlib1++
                                , v
                        );
                    }
                }
                mucInfo.getMmlVoiceDataWork().set(fmlib1++, (byte) fb);
                mucInfo.getMmlVoiceDataWork().set(fmlib1, (byte) alg);
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
                SSGWaveDefine(/*ref*/ i, /*ref*/ srcCPtr);
            }
        }

        work.getuseSSGVoice().clear();
    }

    private void SSGWaveDefine(/*ref*/ int[] srcRow, /*ref*/ int[] srcCPtr) {
        // 定義番号を得る
        int n = msub.REDATA(mucInfo.getBasSrc().get(srcRow[0]), /*ref*/ srcCPtr);
        if (mucInfo.getCarry() || mucInfo.getErrSign()) {
            muc88.WriteWarning(Message.get("Wxxxx"), srcRow[0], srcCPtr[0]);
        }
        if (!work.getuseSSGVoice().contains(n)) return;

        byte[] v = new byte[64];
        for (int row = 0; row < 4; row++) {

            srcRow[0]++;
            if (mucInfo.getBasSrc().size() == srcRow[0]) {
                muc88.WriteWarning(Message.get("Wxxxx"), srcRow[0], srcCPtr[0]);
                return;
            }

            srcCPtr[0] = 1;
            for (int col = 0; col < 16; col++) {
                v[row * 16 + col] = (byte) msub.REDATA(mucInfo.getBasSrc().get(srcRow[0]), /*ref*/ srcCPtr);
                if (mucInfo.getCarry() || mucInfo.getErrSign()) {
                    muc88.WriteWarning(Message.get("Wxxxx"), srcRow[0], srcCPtr[0]);
                }
                srcCPtr[0]++;// SKIP','
            }
        }

        if (mucInfo.getSsgVoice().containsKey(n)) {
            mucInfo.getSsgVoice().remove(n);
        }
        mucInfo.getSsgVoice().put(n, v);
    }

    /**
     * ﾎﾟﾙﾀﾒﾝﾄ ｹｲｻﾝ
     * IN:HL<={CG}ﾀﾞｯﾀﾗ GﾉﾃｷｽﾄADR
     * EXIT:DE<=Mｺﾏﾝﾄﾞﾉ 3ﾊﾞﾝﾒ ﾉ ﾍﾝｶﾘｮｳ
     * Zﾌﾗｸﾞ=1 ﾅﾗ ﾍﾝｶｼﾅｲ
     */
    public int CULPTM(int chipIndex, byte startNote, byte endNote, byte clk) {
        int depth = CULP2Ex(chipIndex, startNote, endNote) / (byte) (clk >> 0);

        if (!mucInfo.getCarry()) return depth;
        mucInfo.setCarry(false);
        return -depth; // RET
    }

    public double CULPTMex(int chipIndex, byte startNote, byte endNote, byte clk) {
        double depth = CULP2Ex(chipIndex, startNote, endNote) / (double) clk;

        if (!mucInfo.getCarry()) return depth;
        mucInfo.setCarry(false);
        return -depth; // RET
    }

    public byte getEndNote() {
        int DE = work.MDATA;
        byte endNote = msub.STTONE();
        work.MDATA = DE;
        if (mucInfo.getCarry()) {
            mucInfo.setCarry(true); // SCF
            return 0; // RET
        }
        return endNote;
    }

    public int getDiffNote(byte startNote, byte endNote) {
        return ctone(endNote) - ctone(startNote);
    }

    private int CULP2Ex(int chipIndex, byte startNote, byte endNote) {
        int HL;
        boolean up;

        // EXIT:HL <= ﾍﾝｶﾊﾝｲ
        // CY ﾅﾗ ｻｶﾞﾘﾊｹｲ
        // Z ﾅﾗ ﾍﾝｶｾｽﾞ

        boolean CULP2_Ptn = false;
        Muc88.ChannelType tp = muc88.CHCHK();
        if (tp == Muc88.ChannelType.SSG) {
            CULP2_Ptn = true;
        }

        int C = startNote & 0x0F; // KEY
        if (!CULP2_Ptn) {
            if (chipIndex < 2) work.setFRQBEF(FNUMB[0][C]);
            else if (chipIndex < 4) work.setFRQBEF(FNUMB[1][C]);
            else work.setFRQBEF(FNUMBopm[0][C]);
        } else {
            if (chipIndex < 2) work.setFRQBEF(SNUMB[0][C]);
            else if (chipIndex < 4) work.setFRQBEF(SNUMB[1][C]);
        }

        int noteNum = ctone(endNote) - ctone(startNote);
        if (noteNum == 0) return 0; // 変化なし
        if (noteNum >= 0) {
            up = !CULP2_Ptn;
        } else {
            noteNum = (byte) -noteNum;
            up = CULP2_Ptn;
        }

        if (up) {
            // BEFTONE < NOWTONE (ｱｶﾞﾘ)
            HL = culc(1.059463f, work.getFRQBEF(), noteNum) - work.getFRQBEF();
            mucInfo.setCarry(false);
            return HL;
        }

        // BEFTONE > NOWTONE (ｻｶﾞﾘ)
        HL = work.getFRQBEF() - culc(0.943874f, work.getFRQBEF(), noteNum);
        mucInfo.setCarry(true);
        return HL;
    }

    /**
     * ﾎﾟﾙﾀﾒﾝﾄ ｹｲｻﾝ
     * IN:HL<={CG}ﾀﾞｯﾀﾗ GﾉﾃｷｽﾄADR
     * EXIT:DE<=Mｺﾏﾝﾄﾞﾉ 3ﾊﾞﾝﾒ ﾉ ﾍﾝｶﾘｮｳ
     * Zﾌﾗｸﾞ=1 ﾅﾗ ﾍﾝｶｼﾅｲ
     */
    public int CULPTM(int chipIndex) {
        int DE = work.MDATA;
        byte note = msub.STTONE();
        work.MDATA = DE;
        if (mucInfo.getCarry()) {
            mucInfo.setCarry(true);//  SCF
            return 0;//    RET
        }

        int depth = CULP2(chipIndex, note) / work.getBEFCO();//Mem.LD_8(BEFCO + 1); ?

        if (!mucInfo.getCarry()) return depth;
        mucInfo.setCarry(false);
        return -depth;//    RET
    }

    private int CULP2(int chipIndex, byte note) {
        int HL;
        boolean up;

        // EXIT:HL <= ﾍﾝｶﾊﾝｲ
        // CY ﾅﾗ ｻｶﾞﾘﾊｹｲ
        // Z ﾅﾗ ﾍﾝｶｾｽﾞ

        boolean CULP2_Ptn = false;
        Muc88.ChannelType tp = muc88.CHCHK();
        if (tp == Muc88.ChannelType.SSG) {
            CULP2_Ptn = true;
        }

        int C = work.getBEFTONE()[0] & 0x0F; // KEY
        if (!CULP2_Ptn) {
            if (chipIndex < 2) work.setFRQBEF(FNUMB[0][C]);
            else if (chipIndex < 4) work.setFRQBEF(FNUMB[1][C]);
            else work.setFRQBEF(FNUMBopm[0][C]);
        } else {
            if (chipIndex < 2) work.setFRQBEF(SNUMB[0][C]);
            else if (chipIndex < 4) work.setFRQBEF(SNUMB[1][C]);
        }

        int noteNum = ctone(note) - ctone(work.getBEFTONE()[0]);
        if (noteNum == 0) return 0;//変化なし
        if (noteNum >= 0) {
            up = !CULP2_Ptn;
        } else {
            noteNum = (byte) -noteNum;
            up = CULP2_Ptn;
        }

        if (up) {
            // BEFTONE < NOWTONE (ｱｶﾞﾘ)
            HL = culc(1.059463f, work.getFRQBEF(), noteNum) - work.getFRQBEF();
            mucInfo.setCarry(false);
            return HL;
        }

        // BEFTONE > NOWTONE (ｻｶﾞﾘ)
        HL = work.getFRQBEF() - culc(0.943874f, work.getFRQBEF(), noteNum);
        mucInfo.setCarry(true);
        return HL;
    }

    private int culc(float facc, int frq, int amul) {
        float frqbef = (float) frq;
        for (int count = 0; count < amul; count++) {
            frqbef *= facc;
        }
        return (short) (int) frqbef;
    }

    public byte ctone(byte a) {
        return (byte) ((a & 0x0f) + ((a & 0xf0) >> 4) * 12);
    }
}

