package mucom88.player.real;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import mucom88.player.RSoundChip;
import mucom88.player.SChipType;
import mucom88.player.real.RC86ctlSoundChip.EnmRealChipType;
import real.NSoundInterface;
import real.nc86ctl.Nc86ctl;
import real.nc86ctl.Nc86ctl.ChipType;
import real.nc86ctl.Nc86ctl.NIGimic2;
import real.nc86ctl.Nc86ctl.NIRealChip;
import real.nscci.NScci;
import real.nscci.NScci.NSoundChip;

import static java.lang.System.getLogger;


/**
 * RScciSoundChip.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-02 nsano initial version <br>
 */
public class RScciSoundChip extends RSoundChip {

    private static final Logger logger = getLogger(RScciSoundChip.class.getName());

    NScci scci = null;
    private NSoundChip realChip = null;

    RScciSoundChip(int soundLocation, int busID, int soundChip) {
        super(soundLocation, busID, soundChip);
    }

    @Override
    public void init() {
        NSoundInterface nsif = NScci.NSoundInterfaceManager().getInterface(BusID);
        NSoundChip nsc = nsif.getSoundChip(SoundChip);
        realChip = nsc;
        dClock = (int) nsc.getSoundChipClock();

        // If you want to send initialization commands for each chip type
        switch (EnmRealChipType.values()[nsc.getSoundChipType()]) {
        case YM2608:
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
     * Master Clock Settings
     * @param mClock The value you want to set
     * @return The actual value set
     */
    @Override
    public int setMasterClock(int mClock) {
        // SCCI cannot change the clock

        return (int) realChip.getSoundChipClock();
    }

    @Override
    public void setSSGVolume(int vol) {
        // SCCI cannot change SSG volume
    }

    @Override
    public void opnaWaitSend(long elapsed, int size) {
        // Add additional weight based on size and elapsed time.
        int m = Math.max((int) (size / 20 - elapsed), 0); // 20 Threshold (magic number)
        try { Thread.sleep(m); } catch (InterruptedException e) {}

        // Check the port as well
        int n = nc86ctl.getNumberOfChip();
        for (int i = 0; i < n; i++) {
            NIRealChip rc = nc86ctl.getChipInterface(i);
            if (rc != null) {
                while ((rc.in((short) 0x0) & 0x83) != 0) {
                    try { Thread.sleep(0); } catch (InterruptedException ignore) {}
                }
                while ((rc.in((short) 0x100) & 0xbf) != 0)
                    try { Thread.sleep(0); } catch (InterruptedException ignore) {}
            }
        }
    }

    @Override
    public RSoundChip checkDevice() {
        SChipType ct = null;
        int iCount;

        nc86ctl = Nc86ctl.INSTANCE;
        nc86ctl.initialize();
        iCount = nc86ctl.getNumberOfChip();
        if (iCount == 0) {
            nc86ctl.deinitialize();
            nc86ctl = null;
            logger.log(Level.ERROR, "Not found G.I.M.I.C.");
            return null;
        }
        for (int i = 0; i < iCount; i++) {
            NIRealChip rc = nc86ctl.getChipInterface(i);
            NIGimic2 gm = rc.queryInterface();
            ChipType cct = gm.getModuleType();
            int o;
            if (cct == ChipType.CHIP_YM2608 || cct == ChipType.CHIP_YMF288 || cct == ChipType.CHIP_YM2203) {
                ct = new SChipType();
                ct.setSoundLocation(-1);
                ct.setBusID(i);
                String seri = gm.getModuleInfo().Serial;
                try {
                    o = Integer.parseInt(seri);
                } catch (NumberFormatException e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
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
            logger.log(Level.ERROR, "Not found G.I.M.I.C.(OPNA module)");
        } else {
            rsc = new RC86ctlSoundChip(-1, ct.getBusID(), ct.getSoundChip());
            rsc.c86ctl = nc86ctl;
            rsc.init();

            rsc.setMasterClock(7987200); // SoundBoardII
            rsc.setSSGVolume((byte) 63); // PC-8801
        }
        return rsc;
    }

    protected void close() {
        if (nc86ctl != null) {
            nc86ctl.deinitialize();
            nc86ctl = null;
        }
    }
}
