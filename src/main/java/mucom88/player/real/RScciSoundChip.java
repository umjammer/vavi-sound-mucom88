package mucom88.player.real;

import java.util.logging.Level;

import mucom88.player.RSoundChip;
import mucom88.player.SChipType;
import vavi.util.Debug;


/**
 * RScciSoundChip.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-02 nsano initial version <br>
 */
public class RScciSoundChip extends RSoundChip {
    public NScci.NScci scci = null;
    private NSoundChip realChip = null;

    public RScciSoundChip(int soundLocation, int busID, int soundChip) {
        super(soundLocation, busID, soundChip);
    }

    @Override
    public void init() {
        NSoundInterface nsif = scci.NSoundInterfaceManager_.getInterface(BusID);
        NSoundChip nsc = nsif.getSoundChip(SoundChip);
        realChip = nsc;
        dClock = (int) nsc.getSoundChipClock();

        //chipの種類ごとに初期化コマンドを送りたい場合
        switch (nsc.getSoundChipType()) {
        case (int) EnmRealChipType.YM2608:
            //setRegister(0x2d, 00);
            //setRegister(0x29, 82);
            //setRegister(0x07, 38);
            break;
        }
    }

    @Override
    public void setRegister(int adr, int dat) {
        realChip.setRegister(adr, dat);
    }

    @Override
    public int getRegister(int adr) {
        return realChip.getRegister(adr);
    }

    @Override
    public boolean isBufferEmpty() {
        return realChip.isBufferEmpty();
    }

    /**
     * マスタークロックの設定
     * <param name="mClock">設定したい値
     * <returns>実際設定された値</returns>
     */
    @Override
    public int SetMasterClock(int mClock) {
        //SCCIはクロックの変更不可

        return (int) realChip.getSoundChipClock();
    }

    @Override
    public void setSSGVolume(byte vol) {
        //SCCIはSSG音量の変更不可
    }

    @Override
    public void OPNAWaitSend(long elapsed, int size) {
        //サイズと経過時間から、追加でウエイトする。
        int m = Math.max((int) (size / 20 - elapsed), 0);//20 閾値(magic number)
        try { Thread.sleep(m); } catch (InterruptedException e) {}

        //ポートも一応見る
        int n = nc86ctl.getNumberOfChip();
        for (int i = 0; i < n; i++) {
            NIRealChip rc = nc86ctl.getChipInterface(i);
            if (rc != null) {
                while ((rc/*. @in*/(0x0) & 0x83) != 0)
                    Thread.sleep(0);
                while ((rc/*. @in*/(0x100) & 0xbf) != 0)
                    Thread.sleep(0);
            }
        }
    }

    @Override
    public RSoundChip CheckDevice() {
        SChipType ct = null;
        int iCount = 0;

        nc86ctl = new Nc86ctl.Nc86ctl();
        nc86ctl.initialize();
        iCount = nc86ctl.getNumberOfChip();
        if (iCount == 0) {
            nc86ctl.deinitialize();
            nc86ctl = null;
            Debug.printf(Level.SEVERE, "Not found G.I.M.I.C.");
            return null;
        }
        for (int i = 0; i < iCount; i++) {
            NIRealChip rc = nc86ctl.getChipInterface(i);
            NIGimic2 gm = rc.QueryInterface();
            ChipType cct = gm.getModuleType();
            int o = -1;
            if (cct == ChipType.CHIP_YM2608 || cct == ChipType.CHIP_YMF288 || cct == ChipType.CHIP_YM2203) {
                ct = new SChipType();
                ct.setSoundLocation(-1);
                ct.setBusID(i);
                String seri = gm.getModuleInfo().Serial;
                try {
                    o = Integer.parseInt(seri);
                } catch (NumberFormatException e) {
                    Debug.println(Level.WARNING, e);
                    o = -1;
                    ct = null;
                    continue;
                }
                ct.setSoundChip(o);
                ct.setChipName(gm.getModuleInfo().Devname);
                ct.setInterfaceName(gm.getMBInfo().Devname);
                break;
            }
        }
        RC86ctlSoundChip rsc = null;
        if (ct == null) {
            nc86ctl.deinitialize();
            nc86ctl = null;
            Debug.printf(Level.SEVERE, "Not found G.I.M.I.C.(OPNA module)");
        } else {
            rsc = new RC86ctlSoundChip(-1, ct.getBusID(), ct.getSoundChip());
            rsc.c86ctl = nc86ctl;
            rsc.init();

            rsc.SetMasterClock(7987200);//SoundBoardII
            rsc.setSSGVolume((byte) 63);//PC-8801
        }
        return rsc;
    }

    protected void finalize() {
        if (nc86ctl != null) {
            nc86ctl.deinitialize();
            nc86ctl = null;
        }
    }
}
