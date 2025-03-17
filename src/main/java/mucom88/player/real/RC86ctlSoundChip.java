package mucom88.player.real;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import mucom88.player.RSoundChip;
import mucom88.player.SChipType;
import real.NSoundInterface;
import real.nc86ctl.Nc86ctl;
import real.nc86ctl.Nc86ctl.ChipType;
import real.nc86ctl.Nc86ctl.NIGimic2;
import real.nc86ctl.Nc86ctl.NIRealChip;
import real.nscci.NScci;
import real.nscci.NScci.NSCCI_INTERFACE_INFO;
import real.nscci.NScci.NSoundChip;

import static java.lang.System.getLogger;


/**
 * RC86ctlSoundChip.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-02 nsano initial version <br>
 */
public class RC86ctlSoundChip extends RSoundChip {

    private static final Logger logger = getLogger(RC86ctlSoundChip.class.getName());

    public Nc86ctl c86ctl = null;
    public Nc86ctl.NIRealChip realChip = null;
    public Nc86ctl.ChipType chiptype = ChipType.CHIP_UNKNOWN;

    public RC86ctlSoundChip(int soundLocation, int busID, int soundChip) {
        super(soundLocation, busID, soundChip);
    }

    @Override
    public void init() {
        NIRealChip rc = c86ctl.getChipInterface(BusID);
        rc.reset();
        realChip = rc;
        NIGimic2 gm = rc.QueryInterface();
        dClock = gm.getPLLClock();
        chiptype = gm.getModuleType();
        if (chiptype == ChipType.CHIP_YM2608) {
            //setRegister(0x2d, 00);
            //setRegister(0x29, 82);
            //setRegister(0x07, 38);
        }
    }

    @Override
    public void setRegister(int adr, int dat) {
        realChip.out((short) adr, (byte) dat);
    }

    @Override
    public int getRegister(int adr) {
        return realChip.in((short) adr);
    }

    @Override
    public boolean isBufferEmpty() {
        return true;
    }

    /**
     * Master Clock Settings
     * @param mClock The value you want to set
     * @return The actual value set
     */
    @Override
    public int SetMasterClock(int mClock) {
        NIGimic2 gm = realChip.QueryInterface();
        int nowClock = gm.getPLLClock();
        if (nowClock != mClock) {
            gm.setPLLClock(mClock);
        }

        return gm.getPLLClock();
    }

    @Override
    public void setSSGVolume(int vol) {
        NIGimic2 gm = realChip.QueryInterface();
        gm.setSSGVolume(vol);
    }

    public enum EnmRealChipType {
        YM2608(1), YM2151(2), YM2610(3), YM2203(4), YM2612(5), SN76489(7), SPPCM(42), C140(43), SEGAPCM(44);
        final int v;

        EnmRealChipType(int v) {
            this.v = v;
        }
    }

    @Override
    public void OPNAWaitSend(long elapsed, int size) {
        NScci.NSoundInterfaceManager().sendData();
        while (!NScci.NSoundInterfaceManager().isBufferEmpty()) {
            try { Thread.sleep(0); } catch (InterruptedException e) {}
        }
    }

    @Override
    public RSoundChip CheckDevice() {
        SChipType ct = null;
        int iCount;

        nScci = NScci.INSTANCE;
        iCount = NScci.NSoundInterfaceManager().getInterfaceCount();
        if (iCount == 0) {
            nScci.Dispose();
            nScci = null;
            logger.log(Level.ERROR, "Not found SCCI.");
            return null;
        }
scciExit:
        for (int i = 0; i < iCount; i++) {
            NSoundInterface iIntfc = NScci.NSoundInterfaceManager().getInterface(i);
            NSCCI_INTERFACE_INFO iInfo = NScci.NSoundInterfaceManager().getInterfaceInfo(i);
            int sCount = iIntfc.getSoundChipCount();
            for (int s = 0; s < sCount; s++) {
                NSoundChip sc = iIntfc.getSoundChip(s);
                int t = sc.getSoundChipType();
                if (t == 1) {
                    ct = new SChipType();
                    ct.setSoundLocation2B(0);
                    ct.setBusID(i);
                    ct.setSoundChip(s);
                    ct.setChipName(sc.getSoundChipInfo().cSoundChipName);
                    ct.setInterfaceName(iInfo.cInterfaceName);
                    break scciExit;
                }
            }
        }
        RScciSoundChip rssc = null;
        if (ct == null) {
            nScci.Dispose();
            nScci = null;
            logger.log(Level.ERROR, "Not found SCCI(OPNA module).");
            device = 0;
        } else {
            rssc = new RScciSoundChip(0, ct.getBusID(), ct.getSoundChip());
            rssc.scci = nScci;
            rssc.init();
        }
        return rssc;
    }

    protected void close() {
        if (nScci != null) {
            nScci.Dispose();
            nScci = null;
        }
    }
}
