package mucom88.compiler;

import java.util.List;

import mucom88.common.MUCInfo;


public class SMon {
    private MUCInfo mucInfo;

    public SMon(MUCInfo mucInfo) {
        this.mucInfo = mucInfo;
    }

    public void CONVERT() {
        // 9列x4行を4列９行に入れ替える
        byte[] vbuf = new byte[40];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                byte b = mucInfo.getMmlVoiceDataWork().get(row * 9 + col + 1);
                vbuf[col * 4 + row] = b;
            }
        }
        vbuf[36] = mucInfo.getMmlVoiceDataWork().get(37);
        vbuf[38] = mucInfo.getMmlVoiceDataWork().get(38); //次のOPEXにて37に移動するよ

        //OPEX()
        for (int col = 0; col < 10; col++) {
            byte b = vbuf[col * 4 + 1];
            vbuf[col * 4 + 1] = vbuf[col * 4 + 2];
            vbuf[col * 4 + 2] = b;
        }

        //GETPARA
        int a;
        for (int row = 0; row < 4; row++) {
            //MUL/DT
            a = (vbuf[row + 32] & 0xf) * 16 + (vbuf[row + 28] & 0xf);
            mucInfo.getMmlVoiceDataWork().set(row + 1, (byte) a);

            //TL
            mucInfo.getMmlVoiceDataWork().set(row + 5, (byte) vbuf[row + 20]);

            //KS/AR
            a = (vbuf[row + 24] & 0x3) * 64 + (vbuf[row] & 0x1f);
            mucInfo.getMmlVoiceDataWork().set(row + 9, (byte) a);

            //DR
            mucInfo.getMmlVoiceDataWork().set(row + 13, (byte) vbuf[row + 4]);

            //SR
            mucInfo.getMmlVoiceDataWork().set(row + 17, (byte) vbuf[row + 8]);

            //SL/RR
            a = (vbuf[row + 16] & 0xf) * 16 + (vbuf[row + 12] & 0xf);
            mucInfo.getMmlVoiceDataWork().set(row + 21, (byte) a);
        }

        a = (vbuf[36] & 0x7) * 8 + (vbuf[37] & 0x7);
        mucInfo.getMmlVoiceDataWork().set(25, (byte) a);

        ///OPEX() 要らないと思う
        //for (int col = 0; col < 10; col++) {
        //byte b = vbuf[col * 4 + 1];
        //vbuf[col * 4 + 1] = vbuf[col * 4 + 2];
        //vbuf[col * 4 + 2] = b;
        //}
    }

    public void converToPM(List<Byte> voi) {
        int a;
        for (int row = 0; row < 4; row++) {
            int op = row;
            op = (op == 1 ? 2 : (op == 2 ? 1 : op));

            //DT/MUL
            a = voi.get(op * 10 + 2 + 8) * 16 + voi.get(op * 10 + 2 + 7);
            mucInfo.getMmlVoiceDataWork().set(row + 1, (byte) a);

            //TL
            mucInfo.getMmlVoiceDataWork().set(row + 5, voi.get(op * 10 + 2 + 5));

            //KS/AR
            a = voi.get(op * 10 + 2 + 6) * 64 + voi.get(op * 10 + 2 + 0);
            mucInfo.getMmlVoiceDataWork().set(row + 9, (byte) a);

            //DR
            mucInfo.getMmlVoiceDataWork().set(row + 13, voi.get(op * 10 + 2 + 1));

            //DT2/SR
            a = voi.get(op * 10 + 2 + 9) * 64 + voi.get(op * 10 + 2 + 2);
            mucInfo.getMmlVoiceDataWork().set(row + 17, (byte) a);

            //SL/RR
            a = voi.get(op * 10 + 2 + 4) * 16 + voi.get(op * 10 + 2 + 3);
            mucInfo.getMmlVoiceDataWork().set(row + 21, (byte) a);
        }

        a = voi.get(0) * 8 + voi.get(1);
        mucInfo.getMmlVoiceDataWork().set(25, (byte) a);
    }
}

