package mucom88.driver;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import mucom88.common.Common;
import musicDriverInterface.ChipDatum;
import musicDriverInterface.LinePos;
import musicDriverInterface.MmlDatum;
import musicDriverInterface.MmlDatum.MMLType;
import vavi.util.ByteUtil;
import vavi.util.compat.Tuple6;

import static java.lang.System.getLogger;


/**
 * @see "https://gemini.google.com/app/41deee050fcc33a3"
 */
class Music2 {

    private static final Logger logger = getLogger(Music2.class.getName());

    private static final int MAXCH = 11;
    // FM CONTROL COMMAND(s)
    private Runnable[] fmCom = null;
    private Runnable[] fmCom2 = null;
    private Runnable[] lfoTbl = null;
    // PSG COMMAND TABLE
    private Runnable[] psgCom = null;
    private Runnable[] psgCom2 = null; // kuma DotNET dedicated table

    private final Work work;
    private final Consumer<ChipDatum> writeOPNAPRegister;
    private final Consumer<ChipDatum> writeOPNASRegister;
    private final Consumer<ChipDatum> writeOPNBPRegister;
    private final Consumer<ChipDatum> writeOPNBSRegister;
    private final Consumer<ChipDatum> writeOPMPRegister;

    private final byte[] autoPanTable = {2, 3, 1, 3};

    public Music2(Work work,
                  Consumer<ChipDatum> writeOPNAPRegister, Consumer<ChipDatum> writeOPNASRegister,
                  Consumer<ChipDatum> writeOPNBPRegister, Consumer<ChipDatum> writeOPNBSRegister,
                  Consumer<ChipDatum> writeOPMPRegister
    ) {
        this.work = work;
        this.writeOPNAPRegister = writeOPNAPRegister;
        this.writeOPNASRegister = writeOPNASRegister;
        this.writeOPNBPRegister = writeOPNBPRegister;
        this.writeOPNBSRegister = writeOPNBSRegister;
        this.writeOPMPRegister = writeOPMPRegister;
        initMusic2();
        initDrives();
    }

    boolean notSoundBoard2;

    public void MSTART(int musicNumber) {
        synchronized (work.systemInterrupt) {

            work.soundWork.setMusNum(musicNumber);
            AKYOFF();
            SSGOFF();
            WORKINIT();

            CHK(); // added
            INT57();
            ENBL();

            for (int c = 0; c < 5; c++) {
                work.soundWork.setCurrentChip(c);
                work.currentTimer = c;
                TO_NML();
            }
            work.soundWork.setCurrentChip(0);

            work.resetPlaySync = true;
            work.setStatus(1);
            work.currentTimer = 0;

            // Timer interrupt only once (to prevent the initial sound from becoming unstable)
            while ((work.timerOPNA1.statReg & 3) == 0) {
                synchronized (work.systemInterrupt) {
                    work.timerOPNA1.timer();
                    work.timerOPNA2.timer();
                    work.timerOPNB1.timer();
                    work.timerOPNB2.timer();
                    work.timerOPM.timer();
                }
            }
        }
    }

    public void MSTOP() {
        synchronized (work.systemInterrupt) {

            AKYOFF();
            SSGOFF();

            if (work.getStatus() > 0) work.setStatus(0);
        }
    }

    public void FDO() {
        synchronized (work.systemInterrupt) {
        }
    }

    public Object RETW() {
        synchronized (work.systemInterrupt) {
        }
        return null;
    }

    public void EFC() {
        synchronized (work.systemInterrupt) {
            // Work.systemInterrupt = true;
            // Work.systemInterrupt = false;
        }
    }

    public void render() {
        if (work.getStatus() == 0) return;

        synchronized (work.systemInterrupt) {
            // Work.systemInterrupt = true;

            if (work.resetPlaySync) {
                work.resetPlaySync = false;
                ChipDatum dat = new ChipDatum(-1, 0, 0, 0, new MmlDatum(MMLType.ResetPlaySync, null, null, 0));
                writeRegister(0, dat);
            }

            work.timerOPNA1.timer();
            work.timerOPNA2.timer();
            work.timerOPNB1.timer();
            work.timerOPNB2.timer();
            work.timerOPM.timer();

            // logger.log(Level.TRACE, "CurrentTimer:%d".formatted(Work.currentTimer));

            work.timeCounter++;
            boolean flg = switch (work.currentTimer) {
                case 0 -> (work.timerOPNA1.statReg & 3) != 0;
                case 1 -> (work.timerOPNA2.statReg & 3) != 0;
                case 2 -> (work.timerOPNB1.statReg & 3) != 0;
                case 3 -> (work.timerOPNB2.statReg & 3) != 0;
                case 4 -> (work.timerOPM.statReg & 3) != 0;
                default -> false;
            };
            if (flg) {
                PL_SND();
            }

            // Work.systemInterrupt = false;
        }
    }

    public void skipCount(int count) {
        synchronized (work.systemInterrupt) {
            // Work.systemInterrupt = true;
            for (int c = 0; c < 5; c++) {
                for (int i = 0; i < work.soundWork.chData.get(c).size(); i++) {
                    if (work.soundWork.chData.get(c).get(i) == null) continue;
                    for (int j = 0; j < work.soundWork.chData.get(c).get(i).pgDat.size(); j++)
                        work.soundWork.chData.get(c).get(i).pgDat.get(j).muteFlg = true;
                }
            }

            while (count > 0) {
                PL_SND();
                count--;
            }

            for (int c = 0; c < 5; c++) {
                for (int i = 0; i < work.soundWork.chData.get(c).size(); i++) {
                    if (work.soundWork.chData.get(c).get(i) == null) continue;
                    for (int j = 0; j < work.soundWork.chData.get(c).get(i).pgDat.size(); j++)
                        work.soundWork.chData.get(c).get(i).pgDat.get(j).muteFlg = false;
                }
            }
        }
    }

    public void setMuteFlag(int chip, int ch, int page, boolean flg) {
        if (chip < 0 || chip >= work.soundWork.chData.size()) return;
        if (ch < 0 || ch >= work.soundWork.chData.get(chip).size()) return;
        if (page < 0 || page >= work.soundWork.chData.get(chip).get(ch).pgDat.size()) return;
        work.soundWork.chData.get(chip).get(ch).pgDat.get(page).silentFlg = flg;
    }

    public void setAllMuteFlag(boolean flg) {
        for (int c = 0; c < 5; c++) {
            for (int i = 0; i < work.soundWork.chData.get(c).size(); i++) {
                if (work.soundWork.chData.get(c).get(i) == null) continue;
                for (int j = 0; j < work.soundWork.chData.get(c).get(i).pgDat.size(); j++)
                    work.soundWork.chData.get(c).get(i).pgDat.get(j).silentFlg = flg;
            }
        }
    }

    private void initMusic2() {
        setFMCOMTable();
        setLFOTBL();
        setPSGCOM();
        setSoundWork();
    }

    private void setFMCOMTable() {
        fmCom = new Runnable[] {
                this::OTOPST,       // 0xF0 - Sound Set                     '@'
                this::VOLPST,       // 0xF1 - volume SET                    'v'
                this::FRQ_DF,       // 0xF2 - DETUNE(Frequency Shift)       'D'
                this::setQ,         // 0xF3 - SET COMMAND                   'q'
                this::onLfo,        // 0xF4 - LFO SET
                this::REPSTF,       // 0xF5 - REPEAT START SET              '['
                this::REPENF,       // 0xF6 - REPEAT END SET                ']'
                this::MDSET,        // 0xF7 - FM Sound source mode set KUMA 'S' Slot Detune Command
//              this::STEREO,       // 0xF8 - STEREO MODE
                this::STEREO_AMD98, // 0xF8 - STEREO MODE                   'p'
                this::FLGSET,       // 0xF9 - FLAG SET
                this::W_REG,        // 0xFA - COMMAND OF                    'y'
                this::VOLUPF,       // 0xFB - volume UP                     ')'
                this::HLFOON,       // 0xFC - HARD LFO
                this::TIE,          // (CANT USE)
                this::RSKIP,        // 0xFE - REPEAT JUMP                   '/'
                this::SECPRC        // 0xff - to second com
        };

        fmCom2 = new Runnable[] {
                this::PVMCHG,        // 0xff 0xF0 - PCM volume MODE
                this::HRDENV,        // 0xff 0xF1 - HARD ENVE SET 's'  -> 'S'(kuma)
                this::ENVPOD,        // 0xff 0xF2 - HARD ENVE PERIOD 'm'
                this::REVERVE,       // 0xff 0xF3 - Reverb
                this::REVMOD,        // 0xff 0xF4 - Reverb mode
                this::REVSW,         // 0xff 0xF5 - Reverb switch
                this::SetKeyOnDelay, // 0xff 0xF6 - KeyOn delay 'kd' n1,n2,n3,n4
                this::MW_REG,        // 0xff 0xF7 - multi Write Register n1,n2,n3,n4
                this::CH3SP,         // 0xff 0xF8 - Sound effect mode control commands
                this::PORTAON,       // 0xff 0xF9 - Portamento n1,n2,n3 (st ed totalclock)
                this::ENVPSTex,      // 0xff 0xFA - Soft Envelope 'E' n1,n2,n3,n4,n5,n6
                this::FMVolMode,     // 0xff 0xFB - FM volume mode switching
                this::OTOPSTG,       // 0xff 0xFC - FM tone gradation
                this::IDECMD,        // 0xff 0xFD - command for IDE
                this::NTMEAN,        // 0xff 0xFE
                this::NOP            // 0xff 0xff
        };
    }

    private void IDECMD() {
        int len = 0;
        int cnt = 0;
        byte d;
        do {
            d = (byte) work.pg.mData[work.hl++].dat;
            len += (d & 0x7f) << (7 * cnt);
            cnt++;
        } while ((d & 0x80) != 0);

        byte[] cmd = new byte[len];
        for (int i = 0; i < len; i++) {
            cmd[i] = (byte) work.pg.mData[work.hl++].dat;
        }

        // IDE-specific commands
        switch (cmd[0]) {
            case 0x00 -> outDummy(MMLType.PartColor, List.of(cmd)); // PartColor
            case 0x01 -> outDummy(MMLType.Lyric, List.of(cmd)); // Memo
        }
    }

    private void NOP() {
        outDummy();
    }

    private void setLFOTBL() {
        lfoTbl = new Runnable[] {
                this::LFOOFF,
                this::LFOON2,
                this::SETDEL,
                this::SETCO,
                this::setVc2,
                this::SETPEK,
                this::TLLFOorSSGTremolo
        };
    }

    private void setPSGCOM() {
        psgCom = new Runnable[] {
                this::OTOSSG, // 0xF0 - Sound Set          '@'
                this::PSGVOL, // 0xF1 - volume SET
                this::FRQ_DF, // 0xF2 - DETUNE
                this::setQ,   // 0xF3 - COMMAND OF         'q'
                this::onLfo,  // 0xF4 - LFO
                this::REPSTF, // 0xF5 - REPEAT START SET   '['
                this::REPENF, // 0xF6 - REPEAT END SET     ']'
                this::NOISE,  // 0xF7 - MIX PORT           'P'
                this::NOISEW, // 0xF8 - NOIZE PARAMATER    'w'
                this::FLGSET, // 0xF9 - FLAG SET
                this::ENVPST, // 0xFA - SOFT ENVELOPE      'E'
                this::VOLUPS, // 0xFB - volume UP          ')'
                this::OTOSET, // 0xFC - Sound Detemination '@='
                this::TIE,    // 0x
                this::RSKIP,  // 0x
                this::SECPRC  // 0xff - to sec com
        };

        psgCom2 = new Runnable[] {
                this::STEREO_AMD98,   // 0xff 0xF0 - 'p' pan
                this::HRDENV,         // 0xff 0xF1 - HARD ENVE SET 's'  -> 'S'(kuma)
                this::ENVPOD,         // 0xff 0xF2 - HARD ENVE PERIOD 'm'
                this::REVERVE,        // 0xff 0xF3 - Reverb
                this::REVMOD,         // 0xff 0xF4 - Reverb mode
                this::REVSW,          // 0xff 0xF5 - Reverb switch
                this::selectWaveForm, // 0xff 0xF6
                this::MW_REG,         // 0xff 0xF7 - multi Write Register n1,n2,n3,n4
                this::CH3SP,          // 0xff 0xF8 - Sound effect mode control commands
                this::PORTAON,        // 0xff 0xF9 - Portamento n1,n2,n3 (st ed totalclock)
                this::ENVPSTex,       // 0xff 0xFA - Soft Envelope 'E' n1,n2,n3,n4,n5,n6
                this::PANex,          // 0xff 0xFB - Extended Pan n(L*9+R)
                this::NTMEAN,         // 0xff 0xFC
                this::IDECMD,         // 0xff 0xFD - command for IDE
                this::NTMEAN,         // 0xff 0xFE
                this::NOP             // 0xff 0xff
        };
    }

    private void selectWaveForm() {
        int a = work.pg.mData[work.hl++].dat;
        if (a != 0xff) {
            // Waveform Preset Selection
            work.pg.setSsgWfNum(a);
        } else {
            // User Waveform Selection
            work.pg.setSsgWfNum(10 + (work.pg.channelNumber >> 1)); // The reason for ">>1" is that the channelNumber of SSG is 0, 2, and 4.
            a = work.pg.mData[work.hl++].dat;
        }

        // Do not transmit when not in SSG extended mode
        if (!work.SSGExtend) return;

        // update duty cycle

        // WaveForm Send
        if (work.pg.getSsgWfNum() > 9) {
            sendSSGWf(a);
        }
    }

    private void sendSSGWf(int wfNum) {
        if (work.ssgVoiceAtMusData == null) return;
        if (!work.ssgVoiceAtMusData.containsKey(wfNum & 0xff)) return;
        byte[] dat = work.ssgVoiceAtMusData.get(wfNum & 0xff);
        int vch = work.pg.channelNumber >> 1;
        outPSG(0x0d, (0x80 | ((vch & 3) << 4) | (work.pg.hardEnvelopValue & 0xf)));
        for (byte b : dat) outPSG(0x0e, (b + 0x80));
    }

    private void setSoundWork() {
        work.init();
    }

    /**
     * AllKeYOFF(fm only)
     */
    private void AKYOFF() {
        for (int i = 0; i < 5; i++) {
            for (int e = 0; e < (i != 4 ? 7 : 8); e++) {
                ChipDatum dat = new ChipDatum(0, (i != 4 ? 0x28 : 0x08), e & 0xff);
                writeRegister(i, dat);
            }
        }
    }

    /** SSG ALL SOUND OFF */
    private void SSGOFF() {
        ChipDatum dat;
        for (int i = 0; i < 4; i++) {
            for (int b = 0; b < 3; b++) {
                dat = new ChipDatum(0, (0x8 + b), 0x0);
                writeRegister(i, dat);
            }
//            dat = new ChipDatum(0, (byte)0x7, 0x0);
//            writeRegister(i, dat);
        }
    }

    /** volume OR FADEOUT etc RESET */
    private void WORKINIT() {
        work.soundWork.setC2Num(0);
        work.soundWork.setChNum(0);
        work.soundWork.setPvMode(0);

        work.soundWork.setKEY_FLAG(0);
        work.soundWork.setRANDUM(System.currentTimeMillis());

        if (work.getHeader().mupb != null) {
            WORKINITExtendFormat();
            return;
        }

        int num = work.soundWork.getMusNum();
//logger.log(Level.TRACE, "num: " + num);
        work.mDataAdr = work.soundWork.getMuTop();
//logger.log(Level.TRACE, "work.mDataAdr: " + work.mDataAdr + ".formatted(" + work.mData));

        for (int i = 0; i < num; i++) {
            work.mDataAdr += 1 + MAXCH * 4;
            work.mDataAdr = work.soundWork.getMuTop() + Common.getLE16(work.mData, work.mDataAdr);
        }

        work.soundWork.setTimerB((work.mData[work.mDataAdr] != null) ? (work.mData[work.mDataAdr].dat & 0xff) : 200);
        work.soundWork.setTbTop(++work.mDataAdr);

        int ch = 0; // (means CH1DAT)
        for (ch = 0; ch < 6; ch++) {
            FMINIT(0, ch);
            //ch++; // The original is "ix+=WKLENG", but it has been converted into an array.
        }

        work.soundWork.setChNum(0);
        ch = 6; // DRAMDAT
        FMINIT(0, ch);

        work.soundWork.setChNum(0);
        //ix = 7; // CHADAT
        for (ch = 7; ch < 7 + 4; ch++) {
            FMINIT(0, ch);
            // The original is "ix+=WKLENG", but it has been converted into an array.
        }

        work.fmVoiceAtMusData = getVoiceDataAtMusData();

        work.mData = null;
    }

    private void WORKINITExtendFormat() {
        work.mDataAdr = 0;
        work.soundWork.setTimerB(200);
        work.soundWork.setTimerA(200 << 2);
        work.soundWork.setTbTop(0);

        int ch; // (means CH1DAT)
        for (int c = 0; c < 5; c++) {
            work.soundWork.setChNum(0);
            for (ch = 0; ch < (c != 4 ? 6 : 8); ch++) FMINITex(c, ch);
        }

        for (int c = 0; c < 4; c++) {
            work.soundWork.setChNum(0);
            ch = 6; // DRAMDAT
            FMINITex(c, ch);
        }

        for (int c = 0; c < 4; c++) {
            work.soundWork.setChNum(0);
            for (ch = 7; ch < 7 + 4; ch++) FMINITex(c, ch);
        }

        if (work.getHeader().mupb.getInstruments() != null && work.getHeader().mupb.getInstruments().length > 0 && work.getHeader().mupb.getInstruments()[0].getData() != null) {
            work.fmVoiceAtMusData = work.getHeader().mupb.getInstruments()[0].getData();

            // Loading SSG waveform data
            work.ssgVoiceAtMusData = null;
            if (work.getHeader().mupb.getInstruments().length == 2) {
                work.ssgVoiceAtMusData = new HashMap<>();
                byte[] buf = work.getHeader().mupb.getInstruments()[1].getData();
                for (int i = 0; i < buf.length / 65; i++) {
                    byte n = buf[i * 65 + 0];
                    byte[] dat = new byte[64];
                    for (int j = 0; j < 64; j++) dat[j] = buf[i * 65 + j + 1];
                    work.ssgVoiceAtMusData.put(n & 0xff, dat);
                }
            }

        }

        work.mData = null;
    }

    private byte[] getVoiceDataAtMusData() {
        int otoDat = (work.mData[1].dat & 0xff) + (work.mData[2].dat & 0xff) * 0x100 + work.weight;
        int voiCnt = work.mData[otoDat].dat;
        List<Byte> buf = new ArrayList<>();
        buf.add((byte) (work.mData[otoDat++].dat & 0xff));
        for (int i = 0; i < voiCnt * 25; i++) {
            buf.add((byte) (work.mData[otoDat + i].dat & 0xff));
        }
        return ByteUtil.toByteArray(buf);
    }

    private void FMINIT(int chipIndex, int ch) {
        work.soundWork.chData.get(chipIndex).set(ch, new SoundWork.CHDAT());
        work.soundWork.chData.get(chipIndex).get(ch).pgDat = new ArrayList<>();
        work.soundWork.chData.get(chipIndex).get(ch).pgDat.add(new SoundWork.CHDAT.PGDAT());
        work.soundWork.chData.get(chipIndex).get(ch).pgDat.getFirst().lengthCounter = 1;
        work.soundWork.chData.get(chipIndex).get(ch).pgDat.getFirst().volume = 0;
        work.soundWork.chData.get(chipIndex).get(ch).pgDat.getFirst().setMusicEnd(false);
        work.soundWork.chData.get(chipIndex).get(ch).setFmVolMode(0);
        work.soundWork.chData.get(chipIndex).get(ch).setCurrentFMVolTable(SoundWork.FMVDAT);

        // set POINTER again
        int stPtr = Common.getLE16(work.mData, work.soundWork.getTbTop());
        int lpPtr = Common.getLE16(work.mData, work.soundWork.getTbTop() + 2);
        if (lpPtr == 0) lpPtr = -1;

        // next channel
        int nCPtr = Common.getLE16(work.mData, work.soundWork.getTbTop() + 4);

        List<MmlDatum> bf = new ArrayList<>();
        int length = nCPtr - stPtr + (nCPtr < stPtr ? 0x10000 : 0);
//logger.log(Level.TRACE, "stPtr: %d, lpPtr: %d, nCPtr: %d, length: %d".formatted(stPtr, lpPtr, nCPtr, length));
        for (int i = 0; i < length; i++) {
            bf.add(work.mData[work.soundWork.getMuTop() + work.weight + stPtr + i]);
        }
        work.soundWork.chData.get(chipIndex).get(ch).pgDat.getFirst().mData = bf.toArray(MmlDatum[]::new);

        if (nCPtr < stPtr) {
            work.weight += 0x1_0000;
        }

        work.soundWork.chData.get(chipIndex).get(ch).pgDat.getFirst().dataAddressWork = 0; // ix 2,3
        work.soundWork.chData.get(chipIndex).get(ch).pgDat.getFirst().dataTopAddress = lpPtr != -1 ? (lpPtr - stPtr) : -1; // ix 4,5

        work.soundWork.incC2NUM();
        work.soundWork.addTbTop(4);
        if (work.soundWork.getChNum() > 2) {
            // FOR SSG
            work.soundWork.chData.get(chipIndex).get(ch).pgDat.getFirst().volReg = work.soundWork.getChNum() + 5; // ix 7
            work.soundWork.chData.get(chipIndex).get(ch).pgDat.getFirst().channelNumber = (work.soundWork.getChNum() - 3) * 2; // ix 8
            work.soundWork.incChNum();
            return;
        }
        work.soundWork.chData.get(chipIndex).get(ch).pgDat.getFirst().channelNumber = work.soundWork.getChNum(); // ix 8
        work.soundWork.incChNum();
    }

    private void FMINITex(int chipIndex, int ch) {
        work.soundWork.chData.get(chipIndex).set(ch, new SoundWork.CHDAT());
        work.soundWork.chData.get(chipIndex).get(ch).pgDat = new ArrayList<>();
        work.soundWork.chData.get(chipIndex).get(ch).setKeyOnCh(-1); // KUMA No initialization required, but just in case
        work.soundWork.chData.get(chipIndex).get(ch).setCurrentPageNo(0); // KUMA The initial current page is 0.
        work.soundWork.chData.get(chipIndex).get(ch).setFmVolMode(0);
        work.soundWork.chData.get(chipIndex).get(ch).setCurrentFMVolTable(SoundWork.FMVDAT);

        MupbInfo.ChipDefine.ChipPart partInfo = work.getHeader().mupb.getChips()[chipIndex].getParts()[ch];

        for (int i = 0; i < partInfo.getPages().length; i++) {
            MupbInfo.PageDefine pageInfo = partInfo.getPages()[i];

            SoundWork.CHDAT.PGDAT pg = new SoundWork.CHDAT.PGDAT();
            pg.setPageNo(i);
            work.soundWork.chData.get(chipIndex).get(ch).pgDat.add(pg);
            pg.lengthCounter = 1;
            pg.volume = 0;
            pg.keyOffFlag = true;
            pg.setMusicEnd(false);
            pg.dataAddressWork = 0; // ix 2,3
            pg.dataTopAddress = pageInfo.getLoopPoint();
            pg.mData = pageInfo.getData();
            pg.channelNumber = work.soundWork.getChNum(); // ix 8
            pg.setTlDirectTable(new int[] {
                    255, 255, 255, 255
            });

            if (chipIndex != 4) {
                if (work.soundWork.getChNum() > 2) {
                    // FOR SSG
                    pg.volReg = work.soundWork.getChNum() + 5; // ix 7
                    pg.channelNumber = (work.soundWork.getChNum() - 3) * 2; // ix 8
                }
                // For the rhythm channel, set the tone number to 0x3f to enable all drum sounds.
                if (ch == 6) {
                    pg.instrumentNumber = 0x3f;
                }
            }

            pg.panMode = 3;
        }

        work.soundWork.incC2NUM();
        work.soundWork.addTbTop(4);
        work.soundWork.incChNum();
    }

    /**
     * Checking sound board 2 and setting interrupt vector and port
     * (No need to set interrupt vectors or ports)
     */
    private void CHK() {
        work.soundWork.setNoTsb2(notSoundBoard2 ? 1 : 0);
    }

    /** Set the interrupt level and other initial settings */
    private void INT57() {

        // No interrupt settings required

        for (int c = 0; c < 5; c++) {
            work.soundWork.setCurrentChip(c);
            TO_NML();
        }
        work.soundWork.setCurrentChip(0);

        MONO();
        AKYOFF(); // ALL KEY OFF
        SSGOFF();

        for (int c = 0; c < 5; c++) {
            ChipDatum dat;
            if (c != 4) {
                if (c < 2) {
                    dat = new ChipDatum(0, 0x29, 0x83); // CH 4-6 ENABLE
                    writeRegister(c, dat);
                }

                for (int b = 0; b < 6; b++) {
                    dat = new ChipDatum(0, b, 0x00); // SSG registers($00～$05) 0 clear
                    writeRegister(c, dat);
                }

                dat = new ChipDatum(0, 7, 0b0011_1000); // SSG tone mixer initialize
                writeRegister(c, dat);

                if (c < 2) dat = new ChipDatum(0, 0x11, 63); // Rhythm volume initialize(max:63)
                else dat = new ChipDatum(1, 0x01, 63); // ADPCM-A volume initialize(max:63)
                writeRegister(c, dat);

                dat = new ChipDatum(1, 0x06, 0xf0);
                writeRegister(c, dat);
                dat = new ChipDatum(1, 0x07, 0x01);
                writeRegister(c, dat);
            } else {
                // OPM: What should I do?
                dat = new ChipDatum(0, 1, 0x02); // LFO reset
                writeRegister(c, dat);
            }

            if (c == 4) continue;

            // initialize PSG buffer
            System.arraycopy(work.soundWork.initPm, 0, work.soundWork.pregBf[c], 0, work.soundWork.initPm.length);
        }
    }

    // private void TO_NML() {
    //    work.soundWork.PLSET1_VAL = 0x38;
    //    work.soundWork.PLSET2_VAL = 0x3a;

    //    OPNAData dat = new OPNAData(0, 0x27, 0x3a);
    //    writeOPNARegister(dat);
    // }

    /** ALL MONORAL / H.LFO OFF */
    private void MONO() {
        ChipDatum dat;
        work.soundWork.setFmPort(0);
        for (int b = 0; b < 3; b++) {
            dat = new ChipDatum(0, 0xb4 + b, 0xc0); // fm 1-3
            writeRegister(work.soundWork.getCurrentChip(), dat);
        }

        for (int b = 0; b < 6; b++) {
            dat = new ChipDatum(0, 0x18 + b, 0xc0); // rhythm
            writeRegister(work.soundWork.getCurrentChip(), dat);
        }

        work.soundWork.setFmPort(4);
        for (int b = 0; b < 3; b++) {
            dat = new ChipDatum(1, 0xb4 + b, 0xc0); // fm 4-6
            writeRegister(work.soundWork.getCurrentChip(), dat);
        }

        work.soundWork.setFmPort(0);
        dat = new ChipDatum(0, 0x22, 0x00); // lfo freq control
        writeRegister(work.soundWork.getCurrentChip(), dat);
        dat = new ChipDatum(0, 0x12, 0x00); // rhythm test data
        writeRegister(work.soundWork.getCurrentChip(), dat);


        for (int b = 0; b < 7; b++) {
            for (int c = 0; c < 10; c++)
                SoundWork.PALDAT[b * 10 + c] = 0xc0;
        }

        for (int c = 0; c < 4; c++)
            work.soundWork.getPcmLr()[c] = 3;
    }

    private void writeRegister(int id, ChipDatum dat) {
        switch (id) {
        case 0:
            writeOPNAPRegister.accept(dat);
            break;
        case 1:
            writeOPNASRegister.accept(dat);
            break;
        case 2:
            writeOPNBPRegister.accept(dat);
            break;
        case 3:
            writeOPNBSRegister.accept(dat);
            break;
        case 4:
            writeOPMPRegister.accept(dat);
            break;
        }
    }

    /** music interruption ENABLE */
    private void ENBL() {
        if(!work.soundWork.useTimerA())
            STTMB(work.soundWork.getTimerB()); // SET Timer-B
        else
            STTMA(work.soundWork.getTimerA()); // SET Timer-A

        // No need to reset the interrupt vector
        //Z80.A = M_VECTR;
        //Z80.C = Z80.A;
        //Z80.A = PC88.IN(Z80.C);
        //Z80.A &= 0x7F;
        //PC88.OUT(Z80.C, Z80.A);
    }

    private void STTMA(int e) {
        ChipDatum dat;

        for (int c = 0; c < 4; c++) {
            dat = new ChipDatum(0, 0x24, e >> 2);
            writeRegister(c, dat);

            dat = new ChipDatum(0, 0x25, e & 0x3);
            writeRegister(c, dat);

            dat = new ChipDatum(0, 0x27, 0x74);
            writeRegister(c, dat);

            dat = new ChipDatum(0, 0x27, 0x75);
            writeRegister(c, dat);
        }

        dat = new ChipDatum(0, 0x10, e >> 2);
        writeRegister(4, dat);

        dat = new ChipDatum(0, 0x11, e & 0x3);
        writeRegister(4, dat);

        dat = new ChipDatum(0, 0x14, 0x74);
        writeRegister(4, dat);

        dat = new ChipDatum(0, 0x14, 0x75);
        writeRegister(4, dat);

        // No need to reset the interrupt vector
        //Z80.A = 5;
        //PC88.OUT(0xe4, Z80.A);
    }

    /**
     * Timer-B counter set routine.
     * IN: E<= TIMER_B COUNTER
     */
    private void STTMB(int e) {
        ChipDatum dat;

        for (int c = 0; c < 4; c++) {
            dat = new ChipDatum(0, 0x26, e & 0xff);
            writeRegister(c, dat);

            dat = new ChipDatum(0, 0x27, 0x78);
            writeRegister(c, dat);

            dat = new ChipDatum(0, 0x27, 0x7a);
            writeRegister(c, dat);
        }

        dat = new ChipDatum(0, 0x12, e & 0xff);
        writeRegister(4, dat);

        dat = new ChipDatum(0, 0x14, 0x78);
        writeRegister(4, dat);

        dat = new ChipDatum(0, 0x14, 0x7a);
        writeRegister(4, dat);

        // No need to reset the interrupt level
        //Z80.A = 5;
        //PC88.OUT(0xe4, Z80.A);
    }

    /** MUSIC MAIN */
    private void PL_SND() {
        updateTimer();

        //if (Work.dummyCount > 0) {
        //    Work.dummyCount--;
        //    return;
        //}

        DRIVE();
        //FDOUT();

        int n = 0;
        for (int c = 0; c < 5; c++) {
            for (int i = 0; i < 11; i++) {
                if (work.soundWork.chData.get(c).get(i) == null) continue;
                int p = 0;
                for (int j = 0; j < work.soundWork.chData.get(c).get(i).pgDat.size(); j++) {
                    if (work.soundWork.chData.get(c).get(i).pgDat.get(j).getMusicEnd()) p++;
                }
                if (p == work.soundWork.chData.get(c).get(i).pgDat.size()) n++;
            }
        }
        if (n == 11 * 4 + 8)
            work.setStatus(0);
    }

    private void updateTimer() {
        ChipDatum dat;
        if (work.currentTimer != 4) {
            dat = new ChipDatum(0, 0x27, work.soundWork.PLSET1_VAL[work.currentTimer] & 0xff); // TIMER-OFF DATA
            writeRegister(work.currentTimer, dat);
            dat = new ChipDatum(0, 0x27, work.soundWork.PLSET2_VAL[work.currentTimer] & 0xff); // TIMER-ON DATA
            writeRegister(work.currentTimer, dat);
        } else {
            dat = new ChipDatum(0, 0x14, work.soundWork.PLSET1_VAL[work.currentTimer] & 0xff); // TIMER-OFF DATA
            writeRegister(work.currentTimer, dat);
            dat = new ChipDatum(0, 0x14, work.soundWork.PLSET2_VAL[work.currentTimer] & 0xff); // TIMER-ON DATA
            writeRegister(work.currentTimer, dat);
        }
    }

    // CALL FM

    private Tuple6<String, Integer, Integer, Integer, Integer, Runnable>[] drives;
    private Tuple6<String, Integer, Integer, Integer, Integer, Runnable>[] mDrives;

    @SuppressWarnings("unchecked")
    private void initDrives() {
        drives = Arrays.<Tuple6<String, Integer, Integer, Integer, Integer, Runnable>>asList(
                new Tuple6<>("----- FM 1  ", 0, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 2  ", 0, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 3  ", 0, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- SSG 1 ", 0, 0xff, 0, 0x00, this::SSGENT),
                new Tuple6<>("----- SSG 2 ", 0, 0xff, 0, 0x00, this::SSGENT),
                new Tuple6<>("----- SSG 3 ", 0, 0xff, 0, 0x00, this::SSGENT),
                new Tuple6<>("----- Ryhthm", 0, 0x00, 1, 0x00, this::FMENT),
                new Tuple6<>("----- FM 4  ", 4, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 5  ", 4, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 6  ", 4, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- ADPCM ", 0, 0x00, 0, 0xff, this::FMENT)
        ).toArray(Tuple6[]::new);
        mDrives = Arrays.<Tuple6<String, Integer, Integer, Integer, Integer, Runnable>>asList(
                new Tuple6<>("----- FM 1  ", 0, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 2  ", 0, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 3  ", 0, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 4  ", 0, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 5  ", 0, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 6  ", 0, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 7  ", 0, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 8  ", 0, 0x00, 0, 0x00, this::FMENT)
        ).toArray(Tuple6[]::new);
    }

    private void DRIVE() {
        int n = 0;

        int nowLoopCounter = Integer.MAX_VALUE;
        for (int c = 0; c < 5; c++) {
            work.soundWork.setCurrentChip(c);
//            int nowLoopCounter = Integer.MAX_VALUE;
            Tuple6<String, Integer, Integer, Integer, Integer, Runnable>[] drive = (c != 4) ? drives : mDrives;

            for (int i = 0; i < drive.length; i++) {
                //logger.log(Level.TRACE, drives[i].getItem1());

                // KUMA Set of flag parameters
                work.soundWork.setFmPort(drive[i].getItem2());
                work.soundWork.setSsgF1(drive[i].getItem3());
                work.soundWork.setDrmF1(drive[i].getItem4());
                work.soundWork.setPcmFlg(drive[i].getItem5());

                work.cd = work.soundWork.chData.get(c).get(i); // KUMA Switching current partwork
                work.cd.setKeyOnCh(-1); // KUMA Reset pronunciation page information
                if (c != 4) {
                    work.rhythmOR[c] = 0;
                    work.rhythmORKeyOff[c] = 0;
                }

                int m = 0;
                for (int j = 0; j < work.cd.pgDat.size(); j++) {
                    work.soundWork.setCurrentCh(i * 10 + j);
                    work.pg = work.cd.pgDat.get(j); // KUMA Switching current partwork
                    if (!work.pg.getMusicEnd()) {
                        if (work.pg.muteFlg || work.pg.silentFlg)
                            work.soundWork.setReady(0x00); // KUMA: 0x08 (bit3) = MUTE FLAG or mute specified from outside

                        drive[i].getItem6().run(); // KUMA Call processing for each part

                        if (work.pg.muteFlg || work.pg.silentFlg)
                            work.soundWork.setReady(0xff); // KUMA 0x08(bit3)=MUTE FLAG
                    } else {
                        if (work.isDotNET)
                            addEffect();
                    }
                    // KUMA End part count
                    if ((work.pg.dataTopAddress == -1 && work.pg.loopEndFlg)
                            || work.pg.getLoopCounter() >= work.maxLoopCount) m++;

                    // 
                    if ((work.pg.dataTopAddress != -1 || !work.pg.getMusicEnd())
                            && work.pg.getLoopCounter() < nowLoopCounter) {
                        nowLoopCounter = work.pg.getLoopCounter();
                    }
                }
                if (m == work.cd.pgDat.size())
                    n++;

                // Rhythm sound source key on/off control
                if (c < 2) {
                    if (work.rhythmORKeyOff[c] != 0)
                        outPSG(0x10, ((work.rhythmORKeyOff[c] & 0b0011_1111) | 0x80)); // KEY OFF
                    if (work.rhythmOR[c] != 0)
                        outPSG(0x10, (work.rhythmOR[c] & 0b0011_1111)); // KEY ON
                } else if (c < 4) {
                    if (work.rhythmORKeyOff[c] != 0)
                        outPCM(1, 0x0, ((work.rhythmORKeyOff[c] & 0b0011_1111) | 0x80)); // KEY OFF
                    if (work.rhythmOR[c] != 0)
                        outPCM(1, 0x0, (work.rhythmOR[c] & 0b0011_1111)); // KEY ON
                }
            }

            if (nowLoopCounter != Integer.MAX_VALUE) {
                work.nowLoopCounter = nowLoopCounter;
            }
        }

        if (work.maxLoopCount == -1) n = 0;
        if (n == MAXCH * 4 + 8) MSTOP();
        //if (Work.abnormalEnd)
        //    MSTOP();
    }

    private void FMENT() {
        PANNING(); // AMD98
        KeyOnDelaying();

        FMSUB();
        if (work.isDotNET) {
            addEffect();
            return;
        }
        PLLFO();
    }

    private void SSGENT() {
        if (work.SSGExtend) PANNING();
        SSGSUB();
        if (work.isDotNET) {
            addEffect();
            return;
        }
        PLLFO();
    }

    private void addEffect() {

        // !! It is assumed that the latest fNum has been sent before arriving here. !!
        int fNum = work.pg.fnum;
        int deltn = 0;
        if (work.soundWork.getPcmFlg() != 0)
            deltn = work.soundWork.getDeltN()[work.soundWork.getCurrentChip()];

        prcInstrumentGradation();
        prcLFO();
        prcPortamento();

        if ((work.soundWork.getPcmFlg() == 0 && fNum != work.pg.fnum)
                || (work.soundWork.getPcmFlg() != 0 && fNum + deltn != work.pg.fnum + work.soundWork.getDeltN()[work.soundWork.getCurrentChip()])) {

            List<Object> args = new ArrayList<>();
            args.add(work.pg.lfoDeltaWork);
            makeDummyCrrentMmlDatum(MMLType.Lfo, args); // TODO LFODelta

            prcWriteFnum();
        }

        if (work.soundWork.getPcmFlg() != 0) {
            prcSoftEnvelope();
            //logger.log(Level.TRACE, "%d".formatted(Work.aReg));
            // send volume

            if ((work.pg.softEnvelopeFlag & 0x80) != 0) {
                if (work.soundWork.getCurrentChip() < 2)
                    outPCM(0xb, work.aReg);
                else
                    outPCM(0, 0x1b, work.aReg);
            }
        }
    }

    /** Playback routine for FM sound source */
    private void FMSUB() {
        //work.carry = false;
        work.pg.lengthCounter--;
        work.pg.lengthCounter = work.pg.lengthCounter & 0xff;
        if (work.pg.lengthCounter == 0) {
            FMSUB1();
            return;
        }

        if (work.pg.lengthCounter > work.pg.quantize) {
            //if(!Work.carry)
            return;
        }

        // FMSUB0

        if (work.pg.mData[work.pg.dataAddressWork].dat == 0xfd) return; // count OVER ?

        // BIT5,(IX+33)
        if (work.pg.reverbFlg) { // KUMA: 0x20(0b0010_0000)(bit5) = REVERVE FLAG
            FS2();
            return;
        }

        if (checkCh3SpecialMode() ||
                work.soundWork.getDrmF1() != 0 ||
                work.cd.getCurrentPageNo() == work.pg.getPageNo())
            KEYOFF(false);
    }

    private void FS2() {
        STV2((work.pg.volume + work.pg.reverbVol) >> 1);
        work.pg.keyOffFlag = true;
    }

    private void STV2(int c) {
        if (work.soundWork.getCurrentChip() == 4) {
            STV2opm(c);
            return;
        }

        int e;
        if (work.isDotNET) {
            if (work.cd.getFmVolMode() == 2)
                e = 127 - Math.clamp(work.pg.volume, 0, 127);
            else if (work.cd.getFmVolMode() == 3)
                e = 255;
            else {
                //if (work.cd.currentFMVolTable == null)
                //    work.cd.currentFMVolTable = work.soundWork.FMVDAT;
                e = work.cd.getCurrentFMVolTable()[c]; // GET volume DATA
            }
        } else {
assert c >= 0 && c < 20 : work.pg.volume + ", " + work.pg.reverbVol;
            e = SoundWork.FMVDAT[c]; // GET volume DATA
        }

        int d = (0x40 + work.pg.channelNumber) & 0xff; // GET PORT No.

        if (work.pg.algo >= 8) return; // KUMA: The original is unchecked

        c = SoundWork.CRYDAT[work.pg.algo];

        STV3(c, d, e);

        // For parameter display
        List<Object> args = new ArrayList<>();
        if (work.isDotNET && (work.cd.getFmVolMode() == 2 || work.cd.getFmVolMode() == 3))
            args.add(work.pg.volume);
        else
            args.add(work.pg.volume - 4);
        args.add(work.cd.getFmVolMode());

        outDummy(MMLType.Volume, args);
    }

    private void STV3(int c, int d, int e) {
        if (checkCh3SpecialMode()) {
            if ((work.pg.useSlot & 1) != 0) {
                if ((c & (1 << 0)) != 0) { // slot1
                    int v = e;
                    if (work.getHeader().carrierCorrection) v = Math.clamp(e + work.pg.vTl[0], 0, 127);
                    outPSG(d + 0 * 4, v); // If CAREER go to PNGOUT
                }
            }

            if ((work.pg.useSlot & 4) != 0) {
                if ((c & (1 << 1)) != 0) { // slot3
                    int v = e;
                    if (work.getHeader().carrierCorrection) v = Math.clamp(e + work.pg.vTl[1], 0, 127);
                    outPSG(d + 1 * 4, v); // If CAREER go to PNGOUT
                }
            }

            if ((work.pg.useSlot & 2) != 0) {
                if ((c & (1 << 2)) != 0) { // slot2
                    int v = e;
                    if (work.getHeader().carrierCorrection) v = Math.clamp(e + work.pg.vTl[2], 0, 127);
                    outPSG(d + 2 * 4, v); // If CAREER go to PNGOUT
                }
            }

            if ((work.pg.useSlot & 8) != 0) {
                if ((c & (1 << 3)) != 0) { // slot4
                    int v = e;
                    if (work.getHeader().carrierCorrection) v = Math.clamp(e + work.pg.vTl[3], 0, 127);
                    outPSG(d + 3 * 4, v); // If CAREER go to PNGOUT
                }
            }
        } else {
            STV4(c, d, e, 4, work.getHeader().carrierCorrection);
        }
    }

    private void STV2opm(int c) {
        int e;
        if (work.cd.getFmVolMode() == 2)
            e = 127 - Math.clamp(work.pg.volume, 0, 127);
        else if (work.cd.getFmVolMode() == 3)
            e = 255;
        else {
            e = work.cd.getCurrentFMVolTable()[c]; // GET volume DATA
        }

        int d = 0x60 + work.pg.channelNumber; // GET PORT No.

        if (work.pg.algo >= 8) return; // KUMA: The original is unchecked

        c = SoundWork.CRYDAT[work.pg.algo];

        STV3opm(c, d, e);

        // For parameter display
        List<Object> args = new ArrayList<>();
        if (work.isDotNET && (work.cd.getFmVolMode() == 2 || work.cd.getFmVolMode() == 3))
            args.add(work.pg.volume);
        else
            args.add(work.pg.volume - 4);
        args.add(work.cd.getFmVolMode());

        outDummy(MMLType.Volume, args);
    }

    private void STV3opm(int c, int d, int e) {
        STV4(c, d, e, 8, work.getHeader().carrierCorrection);
    }

    private void STV4(int c, int d, int e, int m, boolean caryCor) {
        if ((work.pg.useSlot & 1) != 0) {
            if ((c & (1 << 0)) != 0) {
                int v = e;
                if (e == 255) {
                    v = Math.clamp(work.pg.getTlDirectTable()[0] + work.pg.vTl[0], 0, 127);
                } else {
                    if (caryCor) v = Math.clamp(e + work.pg.vTl[0], 0, 127);
                }
                outPSG(d + 0 * m, v); // if carrier the PSGOUT
            }
        }
        if ((work.pg.useSlot & 4) != 0) {
            if ((c & (1 << 1)) != 0) {
                int v = e;
                if (e == 255) {
                    v = Math.clamp(work.pg.getTlDirectTable()[1] + work.pg.vTl[1], 0, 127);
                } else {
                    if (caryCor) v = Math.clamp(e + work.pg.vTl[1], 0, 127);
                }
                outPSG(d + 1 * m, v); // if carrier the PSGOUT
            }
        }
        if ((work.pg.useSlot & 2) != 0) {
            if ((c & (1 << 2)) != 0) {
                int v = e;
                if (e == 255) {
                    v = Math.clamp(work.pg.getTlDirectTable()[2] + work.pg.vTl[2], 0, 127);
                } else {
                    if (caryCor) v = Math.clamp(e + work.pg.vTl[2], 0, 127);
                }
                outPSG(d + 2 * m, v); // if carrier the PSGOUT
            }
        }
        if ((work.pg.useSlot & 8) != 0) {
            if ((c & (1 << 3)) != 0) {
                int v = e;
                if (e == 255) {
                    v = Math.clamp(work.pg.getTlDirectTable()[3] + work.pg.vTl[3], 0, 127);
                } else {
                    if (caryCor) v = Math.clamp(e + work.pg.vTl[3], 0, 127);
                }
                outPSG(d + 3 * m, v); // if carrier the PSGOUT
            }
        }
    }

    private void outPSG(int d, int e) {
        int port = 0;
        if (d >= 0x30) {
            if (work.soundWork.getFmPort() != 0) {
                port = 1;
            }
        }

        //if (d == 0x27) // && d <= 0x1d) {
        //    logger.log(Level.TRACE, "%x %x".formatted(d, e));
        //}

        ChipDatum dat = new ChipDatum(port, d & 0xff, e & 0xff, 0, work.crntMmlDatum);
        writeRegister(work.soundWork.getCurrentChip(), dat);
        work.crntMmlDatum = null;
    }

    private void outPSG(int c, int p, int d, int e) {
        ChipDatum dat = new ChipDatum(p, d & 0xff, e & 0xff, 0, work.crntMmlDatum);
        writeRegister(c, dat);
        work.crntMmlDatum = null;
    }

    private void outDummy() {
        ChipDatum dat = new ChipDatum(-1, 0, 0, 0, work.crntMmlDatum);
        writeRegister(work.soundWork.getCurrentChip(), dat);
        work.crntMmlDatum = null;
    }

    private void outDummy(MMLType type, List<Object> args) {
        makeDummyCrrentMmlDatum(type, args);
        outDummy();
    }

    private void makeDummyCrrentMmlDatum(MMLType type, List<Object> args) {
        LinePos lp;
        if (work.soundWork.getCurrentChip() != 4) {
            lp = new LinePos(null, "", -1, -1, -1,
                    work.soundWork.getCurrentChip() < 2
                    ? (work.soundWork.getPcmFlg() != 0 ? "ADPCM" : (work.soundWork.getDrmF1() != 0 ? "RHYTHM" : (work.soundWork.getSsgF1() != 0 ? "SSG" : "FM")))
                    : (work.soundWork.getPcmFlg() != 0 ? "ADPCM-B" : (work.soundWork.getDrmF1() != 0 ? "ADPCM-A" : (work.soundWork.getSsgF1() != 0 ? "SSG" : "FM"))),
                    Common.getChipName(work.soundWork.getCurrentChip()),
                    0,
                    Common.getChipNumber(work.soundWork.getCurrentChip()),
                    work.soundWork.getCurrentCh()
            );
        } else {
            lp = new LinePos(null, "", -1, -1, -1,
                    "FM",
                    "YM2151",
                    0,
                    work.soundWork.getCurrentChip() % 2,
                    work.soundWork.getCurrentCh()
            );
        }
        work.crntMmlDatum = new MmlDatum(type, args, lp, 0);
    }

    /** KEY-OFF ROUTINE */
    private void KEYOFF(boolean force /* = false */) {
        if (work.isDotNET && !work.pg.enableKeyOff && !force) return;

        if (work.soundWork.getCurrentChip() == 4) {
            work.pg.kdWork[0] = 0;
            work.pg.kdWork[1] = 0;
            work.pg.kdWork[2] = 0;
            work.pg.kdWork[3] = 0;
            outPSG(0x08, work.pg.channelNumber); // KEY-OFF
            return;
        }

        if (work.soundWork.getPcmFlg() != 0) {
            endPCM();
            return;
        }

        if (work.soundWork.getDrmF1() != 0) {
            if (work.getHeader().mupb == null) {
                // Rhythm Sound Source Key Off
                outPSG(0x10, (work.soundWork.getRhythm() & 0b0011_1111) | 0x80); // get rhythm parameter
            } else {
                work.rhythmORKeyOff[work.soundWork.getCurrentChip()] |= (work.pg.instrumentNumber & 0b0011_1111);
            }
            return;
        }

        work.pg.kdWork[0] = 0;
        work.pg.kdWork[1] = 0;
        work.pg.kdWork[2] = 0;
        work.pg.kdWork[3] = 0;

        if (checkCh3SpecialMode()) {
            work.cd.ch3KeyOn &= ~(work.pg.useSlot << 4);
            int a = work.cd.ch3KeyOn | 0x2;
            outPSG(0x28, a); // KEY-OFF
            //logger.log(Level.TRACE, "KEYOFF : %02x".formatted(a));
        } else {
            outPSG(0x28, work.soundWork.getFmPort() + work.pg.channelNumber); // KEY-OFF
        }
    }

    private void endPCM() {
        if (work.soundWork.getCurrentChip() > 1) {
            PCMEND2610();
            return;
        }

        if (work.cd.getCurrentPageNo() != work.pg.getPageNo()) return;

        if ((work.pg.softEnvelopeFlag & 0x80) == 0) {
            outPCM(0x0b, 0x00);
            outPCM(0x01, 0x00);
            outPCM(0x00, 0x21);
            return;
        }

        work.pg.softEnvelopeFlag &= 0b1000_1111; // STATE 4 (release)
    }

    private void PCMEND2610() {
        if (work.cd.getCurrentPageNo() != work.pg.getPageNo()) return;

        if ((work.pg.softEnvelopeFlag & 0x80) == 0) {
            outPCM(0, 0x1b, 0x00);
            outPCM(0, 0x11, 0x00);
            outPCM(0, 0x10, 0x21);
            return;
        }

        work.pg.softEnvelopeFlag &= 0b1000_1111; // STATE 4 (release)
    }

    /** ADPCM OUT */
    private void outPCM(int d, int e) {
        ChipDatum dat = new ChipDatum(1, d, e, 0, work.crntMmlDatum);
        writeRegister(work.soundWork.getCurrentChip(), dat);
    }

    private void outPCM(int p, int d, int e) {
        ChipDatum dat = new ChipDatum(p, d, e, 0, work.crntMmlDatum);
        writeRegister(work.soundWork.getCurrentChip(), dat);
    }

    /** SET NEW SOUND */
    private void FMSUB1() {
        work.pg.keyOffFlag = true;
        if (work.pg.mData[work.pg.dataAddressWork].dat != 0x0fd) { // count OVER?
            FMSUBC(work.pg.dataAddressWork);
            return;
        }

        work.pg.keyOffFlag = false; // RES KEYOFF FLAG
        FMSUBC(work.pg.dataAddressWork + 1);
    }

    private void FMSUBC(int hl) {

        int a;
        boolean nrFlg = false;
        do {
logger.log(Level.TRACE, "%x".formatted(hl + 0xc200));
            a = work.pg.mData[hl].dat;
            // 00H as end
            while (a == 0) { // Check data end
                work.pg.loopEndFlg = true;

                if (work.pg.dataTopAddress == -1 || nrFlg) {
                    if (nrFlg)
                        work.abnormalEnd = true;
                    endFM(hl); // If DATA TOP ADDRESS is 0000H, BGM
                    return; // Decide when to end, otherwise repeat
                }
                hl = work.pg.dataTopAddress;
                a = work.pg.mData[hl].dat; // get flag & length
                work.pg.incloopCounter();
                //if (work.pg.loopCounter > work.nowLoopCounter) work.nowLoopCounter = work.pg.loopCounter;
                nrFlg = true;
            }

            // Evacuate performance information
            work.crntMmlDatum = work.pg.mData[hl];

            // SET LENGTH
            hl++;
            if (a < 0xf0) break;

            // If DATA is a command, go to FMSUBA
            // Sub-command selection
            // FMSUBA
            a &= 0xf; // A=COMMAND No.(0-F)
            work.hl = hl;
            fmCom[a].run();
            hl = work.hl;
        } while (true);

        nrFlg = false;
        work.pg.lengthCounter = a & 0x7f; // set wait counter


        if ((a & 0x80) != 0) { // BIT7(Rest flag)
            work.crntMmlDatum = work.pg.mData[hl - 1];
            // SET F-NUMBER
            work.pg.dataAddressWork = hl; // set next sound data add

            if (work.pg.reverbMode) {
                if (checkCh3SpecialMode() ||
                        work.cd.getCurrentPageNo() == work.pg.getPageNo() ||
                        work.soundWork.getDrmF1() != 0)
                    KEYOFF(false);
                return;
            }
            if (work.pg.reverbFlg) {
                FS2();
                return;
            }
            if (checkCh3SpecialMode() ||
                    work.cd.getCurrentPageNo() == work.pg.getPageNo() ||
                    work.soundWork.getDrmF1() != 0)
                KEYOFF(false);

            outDummy();

            return;
        }

        if (work.cd.getKeyOnCh() != -1 && work.cd.getKeyOnCh() != work.pg.getPageNo()) {
            work.pg.dataAddressWork = hl + 1; // set next sound data add
            return;
        }

        if (!checkCh3SpecialMode() && work.cd.getCurrentPageNo() != work.pg.getPageNo()) {
            // Switching Process
            RestoreOTOPST();
            restoreSTEREO_AMD98();
        }

        // Current page information set
        work.cd.setCurrentPageNo(work.pg.getPageNo());

        // For notes go to fmsub5
        if (work.pg.keyOffFlag) {
            KEYOFF(false);
        }

        if (!work.soundWork.ch3SpMode(work.soundWork.getCurrentChip())) { // When not in sound effect mode
            FMSUB4(hl);
            return;
        }

        if (work.soundWork.getFmPort() != 0) {
            FMSUB4(hl);
            return;
        }

        if (work.pg.channelNumber == 2) { // CH=3?
            EXMODE(hl);
            return;
        }

        FMSUB4(hl);
    }

    /** End of performance */
    private void endFM(int hl) {
        work.pg.setMusicEnd(true);
        work.pg.dataAddressWork = hl;

        if (work.soundWork.getPcmFlg() != 0) {
            endPCM();
            return;
        }
        if (checkCh3SpecialMode()
                || work.cd.getCurrentPageNo() == work.pg.getPageNo())
            KEYOFF(false);
    }

    private void FMSUB4(int hl) {
        int a, b;
        work.carry = false;

        a = work.pg.mData[hl].dat; // a = b synchronized(octave - 1) & key code data
        work.pg.dataAddressWork = hl + 1; // set next sound data add
        if (!work.pg.keyOffFlag && work.pg.beforeCode == a) {
            work.carry = true;
            return;
        }

        work.pg.beforeCode = a;

        if (work.soundWork.getPcmFlg() != 0) {
//PCMGFQ:
            hl = work.soundWork.PCMNMB[work.soundWork.getCurrentChip() / 2][a & 0b0000_1111] + work.pg.detune;
            a >>>= 4;
            b = a;
//ASUB7:
            while (b != 0) {
                hl >>= 1;
                b--;
            }
//ASUB72:
            work.soundWork.getDeltN()[work.soundWork.getCurrentChip()] = hl;
            work.pg.fnum = 0;
            if (!work.pg.keyOffFlag) {
                LFORST();
            }
            LFORST2();
            play();
            return; // The return value is "carry"
        }

        if (work.soundWork.getDrmF1() == 0) {
//FMGFQ:
            if (work.soundWork.getCurrentChip() != 4) {
                hl = work.soundWork.FNUMB[work.soundWork.getCurrentChip() / 2][(a & 0xff) & 0xf]; // get key code(C, C+, D...B)
                hl |= (a & 0x70) << 7; // get block data
                                       // Adjust for A4-A6 port output
                                       // get fNum2
                                       // a= key code & fNum hi

                hl = hl + work.pg.detune; // get detune data
                                          // detune plus
            } else {
                // OPM dedicated processing
                int val = work.soundWork.FNUMBopm[work.getHeader().opmClockMode == MubHeader.enmOPMClockMode.normal ? 0 : 1][a & 0xf]; // get key code(C, C+, D...B)
                int oct = (a & 0x70) >> 4;
                if (val < 0) {
                    oct--;
                    val += 0x300;
                    if (oct < 0) {
                        oct = 0;
                        val = 0;
                    }
                }

                // Detune Add
                hl = addDetuneToFNumOpm(val | ((oct & 0x7) << 11), (short) work.pg.detune);
            }

            if (!work.pg.tlLfoFlag) {
                work.pg.fnum = hl; // FOR LFO
                // FOR LFO
                work.soundWork.setFNum(hl);
            }
            if (work.pg.keyOffFlag) {
                LFORST();
            }
            LFORST2();
//FMSUB8:
            subFM6(hl, work.soundWork.getFmSub8Val()); // The return value is "carry"
            return;
        }

//DRMFQ:
        if (!work.pg.keyOffFlag) {
            return;
        }
        onDKey(); // The return value is "carry"
    }

    /**
     * FMSUB4 for sound effect mode only
     */
    private void subFM4ex(int hl) {
        int a;
        work.carry = false;

        a = work.pg.mData[hl].dat; // a = b synchronized(octave - 1) & key code data
        work.pg.dataAddressWork = hl + 1; // set next sound data add
        if (!work.pg.keyOffFlag && work.pg.beforeCode == a) {
            work.carry = true;
            return;
        }

        work.pg.beforeCode = a;

        hl = work.soundWork.FNUMB[work.soundWork.getCurrentChip() / 2][a & 0xf]; // get key code(C, C+, D...B)
        hl |= (a & 0x70) << 7; // get block data
        // Adjust for A4-A6 port output
        // get fNum2
        // a= key code & fNum hi

        hl = hl + work.pg.detune; // get detune data
        // DETUNE PLUS
        if (!work.pg.tlLfoFlag) {
            work.pg.fnum = hl; // FOR LFO
            // FOR LFO
            work.soundWork.setFNum(hl);
        }
        if (work.pg.keyOffFlag) {
            LFORST();
        }
        LFORST2();
    }

    /** FMSUB6 */
    private void subFM6(int hl, int bc) {
        if (work.soundWork.getCurrentChip() == 4) {
            subFM6Opm(hl, bc);
            return;
        }

        if (work.isDotNET) {
            hl = addDetuneToFNum(hl, (short) (bc & 0xffff));
        } else {
            hl += bc; // block/fnum1&2 detune plus(for se mode)
        }

        int e = hl >> 8; // block/f-number2 data
//FPORT:
        int d = work.soundWork.getFPortVal(); // port a4h
        d += work.pg.channelNumber;
        outPSG(d, e);

        d -= 4;
        e = hl; // f-number1 data
//FMSUB7:
        outPSG(d, e);

        keyOn();
        work.carry = false;
    }

    /** FMSUB6ex */
    private void subFM6ex(int hl, int bc) {
        if (work.isDotNET) {
            hl = addDetuneToFNum(hl, (short) (bc & 0xffff));
        } else {
            hl += bc; // block / fnum1 & 2 detune plus(for se mode)
        }

        int e = hl >> 8; // block/f-number2 data
//FPORT:
        int d = work.soundWork.getFPortVal(); // port a4h
        d += work.pg.channelNumber;
        outPSG(d, e);

        d -= 4;
        e = hl; // f-number1 data
//FMSUB7:
        outPSG(d, e);
    }

    /** FMSUB6opm */
    private void subFM6Opm(int hl, int bc) {
        hl = addDetuneToFNumOpm(hl, (short) (bc & 0xffff));

        int oct = ((hl & 0x3800) >> 11);
        int note = ((hl & 0x7ff) >> 6);
        note--;
        if (note == 0xff) {
            oct--;
            note = 11;
        }
        note = (note < 3 ? note : (note < 6 ? (note + 1) : (note < 9 ? (note + 2) : (note + 3))));

        int e = ((oct << 4) | (note & 0xff)); // oct:bit6-4 note :bit3-0
        int d = 0x28; // KC address
        d += work.pg.channelNumber;
        outPSG(d, e);

        d += 8; // KF address
        e = ((hl & 0x3f) << 2); // KF (bit:7-2)
        outPSG(d, e);

        keyOnOpm();
        work.carry = false;
    }

    private static int addDetuneToFNum(int fnum, int detune) {
        int block = ((fnum >> 11) & 7);
        int fnum11b = fnum & 0x7ff;

        fnum11b += detune;
        if (detune < 0) {
            while (fnum11b < 0x26a) {
                if (block == 0) {
                    if (fnum11b < 0) fnum11b = 0;
                    break;
                }
                fnum11b += 0x26a;
                block--;
            }
        } else {
            while (fnum11b > 0x26a * 2) {
                if (block == 7) {
                    if (fnum11b > 0x7ff) fnum11b = 0x7ff;
                    break;
                }
                fnum11b -= 0x26a;
                block++;
            }
        }

        return ((block & 7) << 11) | (fnum11b & 0x7ff);
    }

    private static int addDetuneToFNumOpm(int fnum, int detune) {
        int block = ((fnum >> 11) & 7);
        int fnum11b = fnum & 0x7ff;

        fnum11b += detune;
        if (detune < 0) {
            while (fnum11b < 0) { // Less than 0
                if (block == 0) {
                    if (fnum11b < 0) fnum11b = 0; // limit
                    break;
                }

                fnum11b += 0x300;
                //fnum11b &= 0x7ff;
                block--;
            }
        } else {
            while (fnum11b >= 0x300) {
                if (block == 7) {
                    if (fnum11b > 0x300) fnum11b = 0x300;
                    break;
                }
                fnum11b -= 0x300;
                block++;
            }
        }

        return ((block & 7) << 11) | (fnum11b & 0x7ff);
    }

    /** se mode detune setting */
    private void EXMODE(int hl) {
        // fNum calculation

        subFM4ex(hl); // set op1
        if (work.carry) {
            return;
        }

        // Set fNum for each slot of ch3
        if ((work.pg.useSlot & 8) != 0) // slot4
            subFM6ex(work.soundWork.getFNum(), work.soundWork.detdat[work.soundWork.getCurrentChip()][0]);

        work.soundWork.setFPortVal(0xaa);
        if ((work.pg.useSlot & 4) != 0) // slot3
            subFM6ex(work.soundWork.getFNum(), work.soundWork.detdat[work.soundWork.getCurrentChip()][1]);

        work.soundWork.setFPortVal(0xab);
        if ((work.pg.useSlot & 1) != 0) // slot1
            subFM6ex(work.soundWork.getFNum(), work.soundWork.detdat[work.soundWork.getCurrentChip()][2]);

        work.soundWork.setFPortVal(0xac);
        if ((work.pg.useSlot & 2) != 0) // slot2
            subFM6ex(work.soundWork.getFNum(), work.soundWork.detdat[work.soundWork.getCurrentChip()][3]);

        work.soundWork.setFPortVal(0xa4);

        keyOnEx();
        work.carry = false;
    }

    /** reset peak l.&delay */
    private void LFORST() {
        work.pg.lfoDelayWork = work.pg.lfoDelay; // Resetting the lfo delay
        work.pg.lfoContFlg = false; // reset lfo contine flag
    }

    private void LFORST2() {
        work.pg.lfoPeakWork = work.pg.lfoPeak >> 1; // Resetting the lfo peak level
        work.pg.lfoDeltaWork = work.pg.lfoDelta; // Resetting the amount of change
        work.pg.setSsgTremoloVol(0);
        if (!work.pg.tlLfoFlag) {
            return;
        }

        // Volume reset
        int c = Math.min(work.soundWork.getTOTALV() + work.pg.volume, 20); // INPUT VOLUME
        int e;
        if (work.isDotNET) {
            if (work.cd.getFmVolMode() == 2)
                e = 127 - Math.clamp(work.pg.volume, 0, 127);
            else if (work.cd.getFmVolMode() == 3)
                e = 255;
            else {
                //if (work.cd.currentFMVolTable == null)
                //    work.cd.currentFMVolTable = work.soundWork.FMVDAT;
                e = work.cd.getCurrentFMVolTable()[c]; // GET VOLUME DATA
            }
        } else
            e = SoundWork.FMVDAT[c];// GET VOLUME DATA

        work.pg.fnum = work.pg.TLlfo + e & 0xff;
        work.pg.bfnum2 = 0;
        STVOL();

        // TL reconfiguration other than Volume
        int d;
        int m;
        if (work.soundWork.getCurrentChip() != 4) {
            d = 0x40 + work.pg.channelNumber; // GET PORT No.
            m = 4;
        } else {
            d = 0x60 + work.pg.channelNumber; // GET PORT No.
            m = 8;
        }

        STV4(~SoundWork.CRYDAT[work.pg.algo], d, 0, m, true);
    }

    /**
     * adpcm play
     * in:(sttadr)<=Play Start Address
     * (endadr)  <=Play End Address
     * (delt_n)<=Playback Rate
     */
    private void play() {
        if (work.soundWork.getCurrentChip() > 1) {
            play2610();
            return;
        }

        if (work.cd.getKeyOnCh() != -1)
            return; // KUMA: Do not process if another page is already playing
        work.cd.setKeyOnCh(work.pg.getPageNo());

        if (work.soundWork.getReady() == 0) return;

        outPCM(0x0b, 0x00);
        outPCM(0x01, 0x00);
        outPCM(0x00, 0x21);
        outPCM(0x10, 0x08);
        outPCM(0x10, 0x80); // INIT
        outPCM(0x02, work.soundWork.getSTTADR()[work.soundWork.getCurrentChip()] & 0xff); // START ADR
        outPCM(0x03, (work.soundWork.getSTTADR()[work.soundWork.getCurrentChip()] & 0xff) >> 8);
        int eAdr = work.soundWork.getENDADR()[work.soundWork.getCurrentChip()];
        outPCM(0x04, eAdr & 0xff); // END ADR
        outPCM(0x05, (eAdr & 0xff) >> 8);

        if (work.isDotNET && work.pg.keyOffFlag) {
            //if (Work.soundWork.getCurrentChip() == 0)
            //    eAdr -= Work.soundWork.STTADR[Work.soundWork.getCurrentChip()];
            work.pg.lfoContFlg = false; // RESET LFO CONTINE FLAG
            if ((work.pg.softEnvelopeFlag & 0x80) != 0) {
                work.pg.softEnvelopeFlag = 0x90;
                work.pg.softEnvelopeCounter = work.pg.softEnvelopeParam[0]; // KUMA: AL is used as the initial value of the counter.
            }
        }

        outPCM(0x09, work.soundWork.getDeltN()[work.soundWork.getCurrentChip()] & 0xff); // Playback Rate Lower
        outPCM(0x0a, (work.soundWork.getDeltN()[work.soundWork.getCurrentChip()] & 0xff) >> 8); // Playback Rate Upper
        outPCM(0x00, 0xa0);

        int e = work.soundWork.getTOTALV() * 4 + work.pg.volume;
        if ((e & 0xff) >= 250) {
            e = 0;
        }
//PL1:
        if (work.soundWork.getPvMode() != 0) {
            e += work.pg.volReg;
        }
//PL2:
        if ((work.pg.softEnvelopeFlag & 0x80) == 0)
            outPCM(0xb, e); // volume

        e = (work.soundWork.getPcmLr()[work.soundWork.getCurrentChip()] & 3) << 6;
        outPCM(0x01, e); // 1 bit TYPE, L&R OUT

        // Send a signal
        work.soundWork.setPOut(work.soundWork.getPcmNum());
    }

    private void play2610() {
        if (work.cd.getKeyOnCh() != -1)
            return; // KUMA: Do not process if another page is already playing
        work.cd.setKeyOnCh(work.pg.getPageNo());

        if (work.soundWork.getReady() == 0) return;

        if (work.pg.keyOffFlag) {
            outPCM(0, 0x1b, 0x00);
            outPCM(0, 0x11, 0x00);
            outPCM(0, 0x10, 0x21);
            outPCM(0, 0x1c, 0x08);
            outPCM(0, 0x1c, 0x80); // INIT
            outPCM(0, 0x12, (work.soundWork.getSTTADR()[work.soundWork.getCurrentChip()] >> 0) & 0xff); // START ADR
            outPCM(0, 0x13, (work.soundWork.getSTTADR()[work.soundWork.getCurrentChip()] >> 8) & 0xff);
            outPCM(0, 0x14, (work.soundWork.getENDADR()[work.soundWork.getCurrentChip()] >> 0) & 0xff); // END ADR
            outPCM(0, 0x15, (work.soundWork.getENDADR()[work.soundWork.getCurrentChip()] >> 8) & 0xff);
            work.pg.lfoContFlg = false; // RESET LFO CONTINE FLAG
            if ((work.pg.softEnvelopeFlag & 0x80) != 0) {
                work.pg.softEnvelopeFlag = 0x90;
                work.pg.softEnvelopeCounter = work.pg.softEnvelopeParam[0]; // KUMA: AL is used as the initial value of the counter.
            }
        }

        outPCM(0, 0x19, (work.soundWork.getDeltN()[work.soundWork.getCurrentChip()] >> 0) & 0xff); // Playback Rate Lower
        outPCM(0, 0x1a, (work.soundWork.getDeltN()[work.soundWork.getCurrentChip()] >> 8) & 0xff); // Playback Rate Upper
        outPCM(0, 0x10, 0xa0);

        int e = work.soundWork.getTOTALV() * 4 + work.pg.volume;
        if (e >= 250) {
            e = 0;
        }
//PL1:
        if (work.soundWork.getPvMode() != 0) {
            e += work.pg.volReg;
        }
//PL2:
        if ((work.pg.softEnvelopeFlag & 0x80) == 0)
            outPCM(0, 0x1b, e); // volume

        e = (work.soundWork.getPcmLr()[work.soundWork.getCurrentChip()] & 3) << 6;
        outPCM(0, 0x11, e); // 1 bit TYPE, L&R OUT

        // Send a signal
        work.soundWork.setPOut(work.soundWork.getPcmNum());
    }

    private void setAdpcmAAddress(int ach) {

        outPCM(1, 0x10 + ach, (work.soundWork.getPCMaSTTADR()[work.soundWork.getCurrentChip() - 2][ach] >> 0) & 0xff); // START ADR
        outPCM(1, 0x18 + ach, (work.soundWork.getPCMaSTTADR()[work.soundWork.getCurrentChip() - 2][ach] >> 8) & 0xff);
        outPCM(1, 0x20 + ach, (work.soundWork.PCMaENDADR[work.soundWork.getCurrentChip() - 2][ach] >> 0) & 0xff); // END ADR
        outPCM(1, 0x28 + ach, (work.soundWork.PCMaENDADR[work.soundWork.getCurrentChip() - 2][ach] >> 8) & 0xff);

    }

    private void setAdpcmAInstrumentAddress(int ach, int i) {

        if (work.pcmTables[work.soundWork.getCurrentChip() + 2] == null) return;
        if (work.pcmTables[work.soundWork.getCurrentChip() + 2].length < 1) return;

        if (i >= work.pcmTables[work.soundWork.getCurrentChip() + 2].length) return;
        work.soundWork.getPCMaSTTADR()[work.soundWork.getCurrentChip() - 2][ach] = work.pcmTables[work.soundWork.getCurrentChip() + 2][i].getItem2()[0] & 0xffff; // start address
        work.soundWork.PCMaENDADR[work.soundWork.getCurrentChip() - 2][ach] = work.pcmTables[work.soundWork.getCurrentChip() + 2][i].getItem2()[1] & 0xffff; // end address
    }

    /** Rhythm sound source key on */
    private void onDKey() {
        if (work.soundWork.getReady() == 0) return;
        if (work.getHeader().mupb == null) {
            outPSG(0x10, work.soundWork.getRhythm() & work.getHeader().rhythmMute[0]); // KEY ON
        } else {
            work.rhythmOR[work.soundWork.getCurrentChip()] |= (work.pg.instrumentNumber &
                    work.getHeader().rhythmMute[work.soundWork.getCurrentChip()]);

            // Send address
            if (work.soundWork.getCurrentChip() > 1) {
                for (int i = 0; i < 6; i++) {
                    if ((work.pg.instrumentNumber & (1 << i)) != 0) {
                        setAdpcmAInstrumentAddress(i, work.pg.beforeCode);
                        setAdpcmAAddress(i);
                    }
                }
            }
        }
    }

    /** KEY-ON ROUTINE */
    private void keyOn() {
        if (work.soundWork.getReady() == 0) return;
        if (work.cd.getKeyOnCh() != -1) return; // KUMA: Do not process if another page is already playing

        if (work.pg.keyOffFlag && work.pg.instrumentGradationSwitch && work.pg.instrumentGradationReset) {
            instrumentGradationReset();
        }

        int a = 0x04;
        if (work.soundWork.getFmPort() == 0) {
            a = 0x00;
        }

        if (!work.pg.keyOnDelayFlag) {
            a += work.pg.keyOnSlot;
        } else {
            work.pg.keyOnSlot = 0x00;
            if (work.pg.kd[0] == 0) work.pg.keyOnSlot += 0x10;
            if (work.pg.kd[1] == 0) work.pg.keyOnSlot += 0x20;
            if (work.pg.kd[2] == 0) work.pg.keyOnSlot += 0x40;
            if (work.pg.kd[3] == 0) work.pg.keyOnSlot += 0x80;
            a += work.pg.keyOnSlot;

            work.pg.kdWork[0] = work.pg.kd[0];
            work.pg.kdWork[1] = work.pg.kd[1];
            work.pg.kdWork[2] = work.pg.kd[2];
            work.pg.kdWork[3] = work.pg.kd[3];
        }

        // Pronunciation page information set
        work.cd.setKeyOnCh(work.pg.getPageNo());

        // KEYON2:
        a += work.pg.channelNumber;
        outPSG(0x28, a); // KEY-ON
        work.pg.useKeyOn = true;

        if (work.pg.reverbFlg) {
            STVOL();
        }
    }

    private void keyOnEx() {
        if (work.soundWork.getReady() == 0) return;
        //if (Work.cd.keyOnCh != -1) return; // KUMA:Do not process if another page is already playing

        if (work.pg.keyOffFlag && work.pg.instrumentGradationSwitch && work.pg.instrumentGradationReset) {
            instrumentGradationReset();
        }

        int a = 0x02;
        //if (Work.soundWork.FMPORT == 0) {
        //    a = 0x00;
        //}

        if (!work.pg.keyOnDelayFlag) {
            a += work.pg.keyOnSlot;
        } else {
            work.pg.keyOnSlot = 0x00;
            if (work.pg.kd[0] == 0) work.pg.keyOnSlot += 0x10;
            if (work.pg.kd[1] == 0) work.pg.keyOnSlot += 0x20;
            if (work.pg.kd[2] == 0) work.pg.keyOnSlot += 0x40;
            if (work.pg.kd[3] == 0) work.pg.keyOnSlot += 0x80;
            a += work.pg.keyOnSlot;

            work.pg.kdWork[0] = work.pg.kd[0];
            work.pg.kdWork[1] = work.pg.kd[1];
            work.pg.kdWork[2] = work.pg.kd[2];
            work.pg.kdWork[3] = work.pg.kd[3];
        }

        // Pronunciation page information set
        //Work.cd.keyOnCh = Work.pg.getPageNo();

        work.cd.ch3KeyOn |= work.pg.useSlot << 4;
        a &= work.cd.ch3KeyOn | 0xf;
        outPSG(0x28, a); // KEY-ON
        work.pg.useKeyOn = true;

        if (work.pg.reverbFlg) {
            STVOL();
        }
    }

    private void keyOnOpm() {
        if (work.soundWork.getReady() == 0) return;
        if (work.cd.getKeyOnCh() != -1) return; // KUMA: Do not process if another page is already playing

        if (work.pg.keyOffFlag && work.pg.instrumentGradationSwitch && work.pg.instrumentGradationReset) {
            instrumentGradationReset();
        }

        int a = 0x00;
        if (!work.pg.keyOnDelayFlag) {
            a += work.pg.keyOnSlot >> 1;
        } else {
            work.pg.keyOnSlot = 0x00;
            if (work.pg.kd[0] == 0) work.pg.keyOnSlot += 0x10;
            if (work.pg.kd[1] == 0) work.pg.keyOnSlot += 0x20;
            if (work.pg.kd[2] == 0) work.pg.keyOnSlot += 0x40;
            if (work.pg.kd[3] == 0) work.pg.keyOnSlot += 0x80;
            a += (work.pg.keyOnSlot >> 1);

            work.pg.kdWork[0] = work.pg.kd[0];
            work.pg.kdWork[1] = work.pg.kd[1];
            work.pg.kdWork[2] = work.pg.kd[2];
            work.pg.kdWork[3] = work.pg.kd[3];
        }

        // Pronunciation page information set
        work.cd.setKeyOnCh(work.pg.getPageNo());

//KEYON2:
        a += work.pg.channelNumber;
        outPSG(0x08, a); // KEY-ON
        work.pg.useKeyOn = true;

        if (work.pg.reverbFlg) {
            STVOL();
        }
    }

    /** volume */
    private void STVOL() {
        int c;

        // STV1
        c = work.soundWork.getTOTALV() + work.pg.volume; // INPUT volume
        if (c >= 20) {
            c = 0;
        }
//STV12:
        STV2(c);
    }

    private void RestoreOTOPST() {
        if (work.soundWork.getPcmFlg() != 0) {
            restoreOTOPCM();
            return;
        }

        if (work.soundWork.getDrmF1() != 0) {
            restoreOTODRM();
            return;
        }

        STENV();
        STVOL();
    }

    /** Tone Setting Main */
    private void OTOPST() {
        if (work.soundWork.getPcmFlg() != 0) {
            OTOPCM();
            return;
        }

        if (work.soundWork.getDrmF1() != 0) {
            OTODRM();
            return;
        }

        work.pg.instrumentNumber = work.pg.mData[work.hl++].dat;
        work.pg.instrumentGradationSwitch = false;

        // KUMA: Changes the tone only for the current page or when sound effect mode is enabled
        if (!checkCh3SpecialMode() && work.cd.getCurrentPageNo() != work.pg.getPageNo()) return;

        STENV();
        STVOL();
    }

    private void OTODRM() {
        outDummy();
        work.soundWork.setRhythm(work.pg.mData[work.hl++].dat); // set rethm para
        work.pg.instrumentNumber = work.soundWork.getRhythm();
    }

    private void restoreOTODRM() {
        outDummy();
        work.soundWork.setRhythm(work.pg.instrumentNumber);
    }

    private void OTOPCM() {
        if (work.cd.getCurrentPageNo() != work.pg.getPageNo()) {
            work.pg.instrumentNumber = work.pg.mData[work.hl++].dat - 1;
            return;
        }
        outDummy();
        int a = work.pg.mData[work.hl++].dat;
        work.soundWork.setPcmNum(a);
        a--;
        work.pg.instrumentNumber = a;

        if (work.pcmTables != null && work.pcmTables[work.soundWork.getCurrentChip()] != null && work.pcmTables[work.soundWork.getCurrentChip()].length > a) {
            work.soundWork.getSTTADR()[work.soundWork.getCurrentChip()] = work.pcmTables[work.soundWork.getCurrentChip()][a].getItem2()[0] & 0xffff; // start address
            work.soundWork.getENDADR()[work.soundWork.getCurrentChip()] = work.pcmTables[work.soundWork.getCurrentChip()][a].getItem2()[1] & 0xffff; // end address
        }

        if (work.soundWork.getPvMode() == 0) return;

        work.pg.volume = work.pcmTables[work.soundWork.getCurrentChip()][a].getItem2()[3] & 0xffff;
logger.log(Level.TRACE, "work.pg.volume: " + work.pg.volume);
    }

    private void restoreOTOPCM() {
        outDummy();
        work.soundWork.setPcmNum(work.pg.instrumentNumber + 1);
        int a = work.pg.instrumentNumber;

        if (work.pcmTables != null &&
                work.pcmTables[work.soundWork.getCurrentChip()] != null &&
                work.pcmTables[work.soundWork.getCurrentChip()].length > a) {
            work.soundWork.getSTTADR()[work.soundWork.getCurrentChip()] = work.pcmTables[work.soundWork.getCurrentChip()][a].getItem2()[0] & 0xffff; // start address
            work.soundWork.getENDADR()[work.soundWork.getCurrentChip()] = work.pcmTables[work.soundWork.getCurrentChip()][a].getItem2()[1] & 0xffff; // end address
        }

        if (work.soundWork.getPvMode() == 0) return;

        work.pg.volume = work.pcmTables[work.soundWork.getCurrentChip()][a].getItem2()[3] & 0xffff;
logger.log(Level.TRACE, "work.pg.volume: " + work.pg.volume);
    }

    /** Tone Setting subroutine (FM) */
    private void STENV() {
        if (work.soundWork.getCurrentChip() == 4) {
            STENVopm();
            return;
        }

        KEYOFF(false);

        int a = 0x80 + work.pg.channelNumber;
        int e = 0xf;
        int b = 4;
//ENVLP:

        if (checkCh3SpecialMode()) {
            if ((work.pg.useSlot & 1) != 0) outPSG(a, e);
            a += 4;
            if ((work.pg.useSlot & 4) != 0) outPSG(a, e);
            a += 4;
            if ((work.pg.useSlot & 2) != 0) outPSG(a, e);
            a += 4;
            if ((work.pg.useSlot & 8) != 0) outPSG(a, e);
        } else {
            do {
                outPSG(a, e); // Release (RR) Cut Processing
                a += 4;
                b--;
            } while (b != 0);
        }

        int hl = STENV2();
    }

    private boolean checkCh3SpecialMode() {
        return (work.soundWork.getFmPort() == 0
                && work.pg.channelNumber == 2
                && work.soundWork.ch3SpMode(work.soundWork.getCurrentChip()));
    }

    private void STENVopm() {
        KEYOFF(false);

        int a = 0xe0 + work.pg.channelNumber;
        int e = 0xf;
        int b = 4;

        do {
            outPSG(a, e); // Release (RR) Cut Processing
            a += 8;
            b--;
        } while (b != 0);

        STENV2opm();
    }

    /** Volume setting */
    private void VOLPST() {
        outDummy();
        if (work.soundWork.getPcmFlg() != 0) {
            PCMVOL();
            return;
        }

        if (work.soundWork.getDrmF1() != 0) {
            VOLDRM();
            return;
        }

        work.pg.volume = work.pg.mData[work.hl++].dat;
logger.log(Level.TRACE, "work.pg.volume: " + work.pg.volume);
        if (checkCh3SpecialMode() || work.cd.getCurrentPageNo() == work.pg.getPageNo())
            STVOL();
    }

    private void PCMVOL() {
        int e = work.pg.mData[work.hl++].dat;
        if (work.soundWork.getPvMode() != 0) {
            work.pg.volReg = e;
            return;
        }
        work.pg.volume = e;
logger.log(Level.TRACE, "work.pg.volume: " + work.pg.volume);
    }

    private void VOLDRM() {
        int a = work.pg.mData[work.hl++].dat;

        if (work.isDotNET) {
            if ((a & 0x80) != 0) {
                VOLDRMn(a & 0x1f);
                return;
            }
        }

        work.pg.volume = a;
logger.log(Level.TRACE, "work.pg.volume: " + work.pg.volume);
        DVOLSET();
//VOLDR1:
        int b = 6;
        int de = 0; // Work.soundWork.drmvol;
//VOLDR2:
        do {
            a = work.soundWork.drmvol[work.soundWork.getCurrentChip()][de] & 0b1100_0000;
            a |= work.pg.mData[work.hl++].dat;
            work.soundWork.drmvol[work.soundWork.getCurrentChip()][de++] = a;
            if (work.soundWork.getCurrentChip() < 2)
                outPSG((0x18 - b + 6) & 0xff, a);
            else
                outPCM(1, (0x8 - b + 6) & 0xff, a);
            b--;
        } while (b != 0);
    }

    private void VOLDRMn(int a) {
        int inst = work.pg.instrumentNumber;
        for (int i = 0; i < 6; i++) {
            if (((inst >> i) & 1) != 0) {
                int b = (work.soundWork.drmvol[work.soundWork.getCurrentChip()][i] & 0b1100_0000) | a;
                work.soundWork.drmvol[work.soundWork.getCurrentChip()][i] = b;
                if (work.soundWork.getCurrentChip() < 2)
                    outPSG(0x18 + i, b);
                else
                    outPCM(1, 0x8 + i, b);
            }
        }
    }

    /** SET TOTAL RHYTHM VOL */
    private void DVOLSET() {
        int d = 0x11;
        int a = work.pg.volume;
        a &= 0b0011_1111;
        a = work.soundWork.getTOTALV() * 5 + a;
        if (a >= 64) {
            a = 0;
        }
//DV2:
        if (work.soundWork.getCurrentChip() < 2)
            outPSG(d, a);
        else
            outPCM(1, 0x1, a);
    }

    /** detune setting */
    private void FRQ_DF() {

        outDummy();
        work.pg.beforeCode = 0; // if detune clear before code
        int de = (short) ((work.pg.mData[work.hl].dat) + (work.pg.mData[work.hl + 1].dat) * 0x100);
        work.hl += 2;
        int a = work.pg.mData[work.hl++].dat;
        if (a != 0) {
            de += work.pg.detune;
        }
//FD2:
        work.pg.detune = de;
        if (work.soundWork.getPcmFlg() == 0) {
            return;
        }

        if (work.cd.getCurrentPageNo() != work.pg.getPageNo()) return;

        int hl = work.soundWork.getDeltN()[work.soundWork.getCurrentChip()];
        hl += de;
        if (work.soundWork.getCurrentChip() < 2) {
            outPCM(0x09, hl & 0xff);
            outPCM(0x0a, (hl & 0xff00) >> 8);
        } else {
            outPCM(0, 0x19, hl & 0xff);
            outPCM(0, 0x1a, (hl & 0xff00) >> 8);
        }
    }

    /** set q command */
    private void setQ() {
        work.pg.quantize = work.pg.mData[work.hl++].dat;
        work.pg.enableKeyOff = (work.pg.quantize != 255);

        List<Object> args = new ArrayList<>();
        args.add(work.pg.quantize);
        outDummy(MMLType.Gatetime, args);

    }

    /** soft lfo set(reset) */
    private void onLfo() {
        int a = work.pg.mData[work.hl++].dat; // get sub command
        if (a != 0) {
            a--; // lfoTbl;
            lfoTbl[a].run();
            LFODummySend();
            return;
        }
        SETDEL();
        SETCO();
        SETVCT();
        SETPEK();
        work.pg.lfoflg = true; // set lfo flag
        LFODummySend();
    }

    private void LFODummySend() {
        List<Object> args = new ArrayList<>();
        args.add(work.pg.lfoflg);
        outDummy(MMLType.LfoSwitch, args);

        args = new ArrayList<>();
        args.add(work.pg.lfoDelay); // 8bit unsigned
        args.add(work.pg.lfoCounter); // 8bit unsigned
        args.add(work.pg.lfoDelta); // 16bit signed
        args.add(work.pg.lfoPeak); // 8bit unsigned
        outDummy(MMLType.Lfo, args);
    }

    private void SETDEL() {
        int a = work.pg.mData[work.hl++].dat;
        work.pg.lfoDelay = a;
        work.pg.lfoDelayWork = a;
    }

    private void SETCO() {
        int a = work.pg.mData[work.hl++].dat;
        work.pg.lfoCounter = a;
        work.pg.lfoCounterWork = a;
    }

    private void SETVCT() {
        int e = work.pg.mData[work.hl++].dat;
        int d = work.pg.mData[work.hl++].dat;

        work.pg.lfoDelta = e + d * 0x100;
        work.pg.lfoDeltaWork = e + d * 0x100;
    }

    private void SETPEK() {
        int a = work.pg.mData[work.hl++].dat;

        work.pg.lfoPeak = a; // set peak level
        a >>= 1;
        work.pg.lfoPeakWork = a;
    }

    private void LFOOFF() {
        work.pg.lfoflg = false; // reset lfo
    }

    private void LFOON2() {
        work.pg.lfoflg = true; // lfoon
    }

    private void setVc2() {
        SETVCT();
        LFORST();
    }

    private void TLLFOorSSGTremolo() {
        if (work.soundWork.getSsgF1() == 0) {
            TLLFO();
            return;
        }

        SSGTremolo();
    }

    private void TLLFO() {

        int a = work.pg.mData[work.hl++].dat;
        if (a == 0) {
            work.pg.tlLfoFlag = false;
            return;
        }

//TLL2:
        work.pg.tlLfoSlot = a;
        work.pg.tlLfoFlag = true;
        a = work.pg.mData[work.hl++].dat;
        work.pg.fnum = a;
        work.pg.bfnum2 = 0;
        work.pg.TLlfo = a;
    }

    private void SSGTremolo() {
        int a = work.pg.mData[work.hl++].dat;
        if (a == 0) {
            work.pg.setSsgTremoloFlg(false);
            work.pg.setSsgTremoloVol(0);
            return;
        }

        work.pg.setSsgTremoloFlg(true);
        work.pg.setSsgTremoloVol(0);
    }

    /** repeat start set */
    private void REPSTF() {
        int e = work.pg.mData[work.hl++].dat;
        int d = work.pg.mData[work.hl++].dat; // de as rewrite adr offset +1

        int hl = work.hl;
        hl -= 2;
        hl += e + d * 0x100;
        int a = work.pg.mData[hl--].dat;
        work.pg.mData[hl].dat = a & 0xff;
    }

    /** Repeat end setting (FM) */
    private void REPENF() {
        int a = ((work.pg.mData[work.hl].dat - 1) & 0xff); // dec repeat co.
        work.pg.mData[work.hl].dat--;

        if (a == 0) {
            //REPENF2();
            work.pg.mData[work.hl].dat = work.pg.mData[work.hl + 1].dat;
            work.hl += 4;
            return;
        }

        work.hl += 2;

        int e = work.pg.mData[work.hl++].dat;
        int d = work.pg.mData[work.hl--].dat;

        //a &= a;
        work.hl -= e + d * 0x100;
    }

    /** se detune set sub routine */
    private void MDSET() {
        TO_EFC();

        if (work.isDotNET) {
            for (int bc = 0; bc < 4; bc++) {
                int l = work.pg.mData[work.hl++].dat;
                int m = work.pg.mData[work.hl++].dat;
                work.soundWork.detdat[work.soundWork.getCurrentChip()][bc] = l + m * 0x100;
            }
        } else {
            for (int bc = 0; bc < 4; bc++) {
                int a = work.pg.mData[work.hl++].dat;
                work.soundWork.detdat[work.soundWork.getCurrentChip()][bc] = a;
            }
        }
    }

    /** change se mode */
    private void TO_NML() {
        int timer = work.currentTimer;

        if (!work.soundWork.useTimerA()) {
            // timer-B
            work.soundWork.PLSET1_VAL[work.soundWork.getCurrentChip()] = 0x38;
            TNML2(0x3a);
        } else {
            // timer-A
            work.soundWork.PLSET1_VAL[work.soundWork.getCurrentChip()] = 0x34;
            TNML2(0x35);
        }

        work.currentTimer = timer;
    }

    private void TO_EFC() {
        int timer = work.currentTimer;

        if (!work.soundWork.useTimerA()) {
            // timer-B
            work.soundWork.PLSET1_VAL[work.soundWork.getCurrentChip()] = 0x78;
            TNML2(0x7a);
        } else {
            // timer-A
            work.soundWork.PLSET1_VAL[work.soundWork.getCurrentChip()] = 0x74;
            TNML2(0x75);
        }

        work.currentTimer = timer;
    }

    private void TNML2(int a) {
        work.soundWork.PLSET2_VAL[work.soundWork.getCurrentChip()] = a;
        if (work.soundWork.getCurrentChip() != 4) outPSG(0x27, a);
        else outPSG(0x14, a);
    }

    /** STEREO */
    public void STEREO() {
        if (work.soundWork.getDrmF1() != 0) { // goto STE2;
            if (work.soundWork.getPcmFlg() != 0) {
                work.soundWork.getPcmLr()[work.soundWork.getCurrentChip()] = work.pg.mData[work.hl++].dat;
//                return;
            } else {
//STER2:
                int a = work.pg.mData[work.hl++].dat;
                int c = ((a >> 2) & 0x3f) | (a << 6);
                int d = SoundWork.PALDAT[work.soundWork.getFmPort() + work.pg.channelNumber * 10 + work.pg.getPageNo()];
                d = (d & 0b0011_1111) | c;
                SoundWork.PALDAT[work.soundWork.getFmPort() + work.pg.channelNumber * 10 + work.pg.getPageNo()] = d;
                a = 0x0B4 + work.pg.channelNumber;

                if (checkCh3SpecialMode() || work.cd.getCurrentPageNo() == work.pg.getPageNo())
                    outPSG(a, d);
            }
//            return;
        } else {
//STE2:
            int dat = work.pg.mData[work.hl++].dat;
            int c = dat;
            dat &= 0b0000_1111;
            int a = work.soundWork.drmvol[work.soundWork.getCurrentChip()][dat];
            a = ((c << 2) & 0b1100_0000) | (a & 0b0001_1111);
            work.soundWork.drmvol[work.soundWork.getCurrentChip()][dat] = a;

            if (work.cd.getCurrentPageNo() == work.pg.getPageNo()) {
                if (work.soundWork.getCurrentChip() < 2)
                    outPSG(dat + 0x18, a);
                else
                    outPCM(1, dat + 0x8, a);
            }
        }
    }

    private void STEREO_AMD98() {
        int a, c, d;
        if (work.soundWork.getDrmF1() != 0) {
            STEREO_AMD98_RHYTHM();
            return;
        }

        if (work.soundWork.getPcmFlg() != 0) {
            STEREO_AMD98_ADPCM();
            return;
        }

        a = work.pg.mData[work.hl++].dat;
        work.pg.panValue = a;
        work.pg.panMode = a;

        if (a < 4) { // goto STE012;

            outDummy();

            if (work.soundWork.getCurrentChip() != 4 && work.soundWork.getSsgF1() == 0) {
                // Existing process
                c = ((a >> 2) & 0x3f) | (a << 6); // Rotate right twice (rotate left six times is simpler in C#)
                d = SoundWork.PALDAT[work.soundWork.getFmPort() + work.pg.channelNumber * 10 + work.pg.getPageNo()];
                d = (d & 0b0011_1111) | c;
                SoundWork.PALDAT[work.soundWork.getFmPort() + work.pg.channelNumber * 10 + work.pg.getPageNo()] = d;
                a = 0x0B4 + work.pg.channelNumber;

                if (checkCh3SpecialMode() || work.cd.getCurrentPageNo() == work.pg.getPageNo())
                    outPSG(a, d);
            } else if (work.soundWork.getSsgF1() != 0) {
                // Pan & Phrst are updated together when the volume is output, so there is no need to send them to the audio source here.
            } else {
                work.pg.panValue = a;
                a = ((a & 1) << 1) | ((a & 2) >> 1);
                c = (a << 6) | (work.pg.feedback << 3) | work.pg.algo;
                a = 0x20 + work.pg.channelNumber;

                if (work.cd.getCurrentPageNo() == work.pg.getPageNo())
                    outPSG(a, c);
            }

            work.pg.panEnable = 0; // No panning
//            return;
        } else {
//STE012:
            work.pg.panEnable |= 1; // pan is allowed
            // Work.pg.panMode = a;
            work.pg.panCounterWork = work.pg.mData[work.hl].dat;
            work.pg.panCounter = work.pg.mData[work.hl].dat;
            work.hl++;
            switch (a) {
            case 4:
                work.pg.panValue = a = 2; // LEFT index
                break;
            case 5:
                work.pg.panValue = a = 0; // RIGHT index
                break;
            default:
                work.pg.panValue = a = 1; // CENTER index
                break;
            }

            a = autoPanTable[a];

            if (work.soundWork.getCurrentChip() != 4) {
                if (work.soundWork.getSsgF1() != 0) {
                    // Pan & Phrst are updated together when the volume is output, so there is no need to send them to the audio source here.
                    // However, the expanded bread will be reset.
                    int v = (work.pg.channelNumber & 0x3) << 6;
                    if (work.cd.getCurrentPageNo() == work.pg.getPageNo())
                        outPSG(0x0f, v);
                } else {
                    c = a << 6;
                    d = SoundWork.PALDAT[work.soundWork.getFmPort() + work.pg.channelNumber * 10 + work.pg.getPageNo()];
                    d = (d & 0b0011_1111) | c;
                    SoundWork.PALDAT[work.soundWork.getFmPort() + work.pg.channelNumber * 10 + work.pg.getPageNo()] = d;
                    a = 0x0B4 + work.pg.channelNumber;

                    if (checkCh3SpecialMode() || work.cd.getCurrentPageNo() == work.pg.getPageNo())
                        outPSG(a, d);
                }
            } else {
                a = ((a & 1) << 1) | ((a & 2) >> 1);
                c = (a << 6) | (work.pg.feedback << 3) | work.pg.algo;
                a = 0x20 + work.pg.channelNumber;

                if (work.cd.getCurrentPageNo() == work.pg.getPageNo())
                    outPSG(a, c);
            }
        }
    }

    private void PANex() {

        int v = work.pg.mData[work.hl++].dat;
        int l = v / 9;
        int r = v % 9;
        int a = (l != 0 ? 2 : 0) | (r != 0 ? 1 : 0);
        work.pg.panValue = a;
        work.pg.panMode = a;

        outDummy();

        if (work.soundWork.getSsgF1() == 0)
            return;

        work.pg.panEnable = 0; // No panning (no auto panning)

        l = (l != 0) ? (7 - (l - 1)) : 0;
        r = (r != 0) ? (7 - (r - 1)) : 0;
        v = (((work.pg.channelNumber>>1) & 0x3) << 6) | ((l & 0x7) << 3) | (r & 0x7);
        if (work.cd.getCurrentPageNo() == work.pg.getPageNo())
            outPSG(0x0f, v);
    }

    private void STEREO_AMD98_RHYTHM() {
        outDummy();

        // bit 0~3 rythmType RTHCSB
        // bit 4~7 specify pan (1: right, 2: left, 3: center, 4: auto right, 5: auto left, 6: random).
        int a = (work.pg.mData[work.hl].dat >> 4) & 0xff;
        int b = work.pg.mData[work.hl].dat & 0xf;
        work.hl++;
        int c;
        if (b >= 6) return;

        if (a < 4) {
            // Existing process
            c = work.soundWork.drmvol[work.soundWork.getCurrentChip()][b];
            a = ((a << 6) & 0b1100_0000) | (c & 0b0001_1111);
            work.soundWork.drmvol[work.soundWork.getCurrentChip()][b] = a;
            // if (Work.cd.getCurrentPageNo() == Work.pg.getPageNo())
            {
                if (work.soundWork.getCurrentChip() < 2)
                    outPSG((b + 0x18), a);
                else
                    outPCM(1, (b + 0x8), a);
            }
            work.soundWork.drmPanEnable[work.soundWork.getCurrentChip()][b] = 0; // No panning
            return;
        }

        work.soundWork.drmPanEnable[work.soundWork.getCurrentChip()][b] |= 1; // pan is allowed
        work.soundWork.drmPanMode[work.soundWork.getCurrentChip()][b] = a;
        work.soundWork.drmPanCounterWork[work.soundWork.getCurrentChip()][b] = work.pg.mData[work.hl].dat;
        work.soundWork.drmPanCounter[work.soundWork.getCurrentChip()][b] = work.pg.mData[work.hl].dat;
        work.hl++;

        switch (a) {
        case 4:
            work.soundWork.drmPanValue[work.soundWork.getCurrentChip()][b] = a = 2;
            break;
        case 5:
            work.soundWork.drmPanValue[work.soundWork.getCurrentChip()][b] = a = 0;
            break;
        default:
            work.soundWork.drmPanValue[work.soundWork.getCurrentChip()][b] = a = 1;
            break;
        }

        a = autoPanTable[a];
        c = work.soundWork.drmvol[work.soundWork.getCurrentChip()][b];
        a = ((a << 6) & 0b1100_0000) | (c & 0b0001_1111);
        work.soundWork.drmvol[work.soundWork.getCurrentChip()][b] = a;
        if (work.cd.getCurrentPageNo() == work.pg.getPageNo()) {
            if (work.soundWork.getCurrentChip() < 2)
                outPSG(b + 0x18, a);
            else
                outPCM(1, b + 0x8, a);
        }
    }

    private void STEREO_AMD98_ADPCM() {
        outDummy();

        int a = work.pg.mData[work.hl++].dat;

        if (a < 4) {
            // Existing process
            if (work.cd.getCurrentPageNo() == work.pg.getPageNo())
                work.soundWork.getPcmLr()[work.soundWork.getCurrentChip()] = a;
            work.pg.panValue = a;
            work.pg.panEnable = 0; // No panning
            return;
        }

        work.pg.panEnable |= 1; // pan is allowed
        work.pg.panMode = a;
        work.pg.panCounterWork = work.pg.mData[work.hl].dat;
        work.pg.panCounter = work.pg.mData[work.hl].dat;
        work.hl++;

        switch (a) {
        case 4:
            work.pg.panValue = a = 2;
            break;
        case 5:
            work.pg.panValue = a = 0;
            break;
        default:
            work.pg.panValue = a = 1;
            break;
        }

        a = autoPanTable[a];
        work.soundWork.getPcmLr()[work.soundWork.getCurrentChip()] = a;

        if (work.soundWork.getCurrentChip() < 2) {
            if (work.cd.getCurrentPageNo() == work.pg.getPageNo())
                outPCM(0x01, a << 6);
        } else {
            if (work.cd.getCurrentPageNo() == work.pg.getPageNo())
                outPCM(0, 0x11, a << 6);
        }
    }

    private void restoreSTEREO_AMD98() {
        int a, c, d;
        if (work.soundWork.getDrmF1() != 0) {
            restoreSTEREO_AMD98_RHYTHM();
            return;
        }

        if (work.soundWork.getPcmFlg() != 0) {
            restoreSTEREO_AMD98_ADPCM();
            return;
        }

        a = work.pg.panMode;
        if (a < 4) { // goto STE012;

            outDummy();

            // Existing process
            c = ((a >> 2) & 0x3f) | (a << 6); // Rotate right twice (rotate left six times is simpler in C#)
            d = SoundWork.PALDAT[work.soundWork.getFmPort() + work.pg.channelNumber * 10 + work.pg.getPageNo()];
            d = (d & 0b0011_1111) | c;
            SoundWork.PALDAT[work.soundWork.getFmPort() + work.pg.channelNumber * 10 + work.pg.getPageNo()] = d;
            a = 0x0B4 + work.pg.channelNumber;
            outPSG(a, d);
            work.pg.panEnable = 0; // No panning
//            return;
        } else {
//STE012:
            work.pg.panEnable |= 1; // pan is allowed
            work.pg.panCounterWork = work.pg.panCounter;
            switch (a) {
            case 4:
                work.pg.panValue = a = 2; // LEFT index
                break;
            case 5:
                work.pg.panValue = a = 0; // RIGHT index
                break;
            default:
                work.pg.panValue = a = 1; // CENTER index
                break;
            }

            a = autoPanTable[a];

            c = a << 6;
            d = SoundWork.PALDAT[work.soundWork.getFmPort() + work.pg.channelNumber * 10 + work.pg.getPageNo()];
            d = (d & 0b0011_1111) | c;
            SoundWork.PALDAT[work.soundWork.getFmPort() + work.pg.channelNumber * 10 + work.pg.getPageNo()] = d;
            a = 0x0B4 + work.pg.channelNumber;

            outPSG(a, d);
        }
    }

    private void restoreSTEREO_AMD98_RHYTHM() {
        outDummy();

        for (int b = 0; b < 6; b++) {
            int a = work.soundWork.drmvol[work.soundWork.getCurrentChip()][b];
            if (work.soundWork.drmPanMode[work.soundWork.getCurrentChip()][b] < 4) {
                if (work.cd.getCurrentPageNo() == work.pg.getPageNo()) {
                    if (work.soundWork.getCurrentChip() < 2)
                        outPSG((b + 0x18), a);
                    else
                        outPCM(1, (b + 0x8), a);
                }
                work.soundWork.drmPanEnable[work.soundWork.getCurrentChip()][b] = 0; // No panning
                continue;
            }

            work.soundWork.drmPanEnable[work.soundWork.getCurrentChip()][b] |= 1; // pan is allowed
            work.soundWork.drmPanCounterWork[work.soundWork.getCurrentChip()][b] = work.soundWork.drmPanCounter[work.soundWork.getCurrentChip()][b];

            switch (a) {
            case 4:
                work.soundWork.drmPanValue[work.soundWork.getCurrentChip()][b] = a = 2;
                break;
            case 5:
                work.soundWork.drmPanValue[work.soundWork.getCurrentChip()][b] = a = 0;
                break;
            default:
                work.soundWork.drmPanValue[work.soundWork.getCurrentChip()][b] = a = 1;
                break;
            }

            a = autoPanTable[a];
            int c = work.soundWork.drmvol[work.soundWork.getCurrentChip()][b];
            a = ((a << 6) & 0b1100_0000) | (c & 0b0001_1111);
            work.soundWork.drmvol[work.soundWork.getCurrentChip()][b] = a;
            if (work.cd.getCurrentPageNo() == work.pg.getPageNo()) {
                if (work.soundWork.getCurrentChip() < 2)
                    outPSG((b + 0x18), a);
                else
                    outPCM(1, (b + 0x8), a);
            }
        }

        // bit 0~3 rhythmType RTHCSB
        // bit 4~7 Specify pan (1: right, 2: left, 3: center, 4: auto right, 5: auto left, 6: random).
    }

    private void restoreSTEREO_AMD98_ADPCM() {
        outDummy();
        int a = work.pg.panMode;
        if (a < 4) {
            work.pg.panEnable = 0; // No panning
            a = work.pg.panValue;
            work.soundWork.getPcmLr()[work.soundWork.getCurrentChip()] = a;
            if (work.soundWork.getCurrentChip() < 2)
                outPCM(0x01, a << 6);
            else
                outPCM(0, 0x11, a << 6);
            return;
        }

        work.pg.panEnable |= 1; // pan is allowed
        work.pg.panCounterWork = work.pg.panCounter;

        switch (a) {
        case 4:
            work.pg.panValue = a = 2;
            break;
        case 5:
            work.pg.panValue = a = 0;
            break;
        default:
            work.pg.panValue = a = 1;
            break;
        }

        a = autoPanTable[a];
        work.soundWork.getPcmLr()[work.soundWork.getCurrentChip()] = a;
        if (work.soundWork.getCurrentChip() < 2)
            outPCM(0x01, a << 6);
        else
            outPCM(0, 0x11, a << 6);
    }

    private void PANNING() {
        if (work.soundWork.getDrmF1() != 0) {
            PANNING_RHYTHM();
            return;
        }

        if ((work.pg.panEnable & 1) == 0) return;
        if ((--work.pg.panCounterWork) != 0) return;

        work.pg.panCounterWork = work.pg.panCounter; // Counter reset

        if (work.pg.panMode == 4 || work.pg.panMode == 5) {
            // left / right
            int ah = work.pg.panValue;
            ah++;
            if (ah == autoPanTable.length) {
                ah = 0;
            }
            work.pg.panValue = ah; // ah: 0~
        } else {
            // random
            int ax;
            do {
                ax = (int) work.soundWork.getRANDUM();
                ax *= 5;
                ax += 0x1993;
                work.soundWork.setRANDUM(ax);
                ax &= 0x0300;
            } while (ax == 0);
            work.pg.panValue = (ax >> 8); // ah: 1~3
        }

        int a, c, d;
        a = (work.pg.panMode == 4 || work.pg.panMode == 5) ? autoPanTable[work.pg.panValue] : work.pg.panValue;

        List<Object> args = new ArrayList<>();
        args.add(a);
        outDummy(MMLType.Pan, args);

        if (work.soundWork.getCurrentChip() != 4) {
            if (work.soundWork.getSsgF1() != 0) {
                return;
            } else if (work.soundWork.getPcmFlg() == 0) {
                c = ((a >> 2) & 0x3f) | (a << 6); // Rotate right twice (rotate left six times is simpler in C#)
                d = SoundWork.PALDAT[work.soundWork.getFmPort() + work.pg.channelNumber * 10 + work.pg.getPageNo()];
                d = (d & 0b0011_1111) | c;
                SoundWork.PALDAT[work.soundWork.getFmPort() + work.pg.channelNumber * 10 + work.pg.getPageNo()] = d;
                a = 0x0B4 + work.pg.channelNumber;

                if (checkCh3SpecialMode() || work.cd.getCurrentPageNo() == work.pg.getPageNo())
                    outPSG(a, d);

                return;
            }
        } else {
            a = ((a & 1) << 1) | ((a & 2) >> 1);
            c = (a << 6) | (work.pg.feedback << 3) | work.pg.algo;
            a = 0x20 + work.pg.channelNumber;

            if (work.cd.getCurrentPageNo() == work.pg.getPageNo()) {
                outPSG(a, c);
                //logger.log(Level.TRACE, "%x".formatted(c & 0xc0));
            }

            return;
        }

        work.soundWork.getPcmLr()[work.soundWork.getCurrentChip()] = a;
        c = (a << 6) & 0xc0;

        if (work.soundWork.getCurrentChip() < 2) {
            if (work.cd.getCurrentPageNo() == work.pg.getPageNo())
                outPCM(0x01, c);
        } else {
            if (work.cd.getCurrentPageNo() == work.pg.getPageNo())
                outPCM(0, 0x11, c);
        }
    }

    private void PANNING_RHYTHM() {
        for (int n = 0; n < 6; n++) {
            if ((work.soundWork.drmPanEnable[work.soundWork.getCurrentChip()][n] & 1) == 0) continue;
            if ((--work.soundWork.drmPanCounterWork[work.soundWork.getCurrentChip()][n]) != 0) continue;

            work.soundWork.drmPanCounterWork[work.soundWork.getCurrentChip()][n] = work.soundWork.drmPanCounter[work.soundWork.getCurrentChip()][n]; // ; Counter reset

            if (work.soundWork.drmPanMode[work.soundWork.getCurrentChip()][n] == 4 || work.soundWork.drmPanMode[work.soundWork.getCurrentChip()][n] == 5) {
                // left / right
                int ah = work.soundWork.drmPanValue[work.soundWork.getCurrentChip()][n];
                ah++;
                if (ah == autoPanTable.length) {
                    ah = 0;
                }
                work.soundWork.drmPanValue[work.soundWork.getCurrentChip()][n] = ah; // ah : 0～
            } else {
                // random
                int ax;
                do {
                    ax = (int) work.soundWork.getRANDUM();
                    ax *= 5;
                    ax += 0x1993;
                    work.soundWork.setRANDUM(ax);
                    ax &= 0x0300;
                } while (ax == 0);
                work.soundWork.drmPanValue[work.soundWork.getCurrentChip()][n] = (ax >> 8); // ah : 1～3
            }

            int a = (work.soundWork.drmPanMode[work.soundWork.getCurrentChip()][n] == 4 || work.soundWork.drmPanMode[work.soundWork.getCurrentChip()][n] == 5)
                    ? autoPanTable[work.soundWork.drmPanValue[work.soundWork.getCurrentChip()][n]]
                    : work.soundWork.drmPanValue[work.soundWork.getCurrentChip()][n];

            int c = work.soundWork.drmvol[work.soundWork.getCurrentChip()][n];
            a = ((work.soundWork.drmPanValue[work.soundWork.getCurrentChip()][n] << 6) & 0b1100_0000) | (c & 0b0001_1111);
            work.soundWork.drmvol[work.soundWork.getCurrentChip()][n] = a;

            if (work.cd.getCurrentPageNo() == work.pg.getPageNo()) {
                if (work.soundWork.getCurrentChip() < 2)
                    outPSG((n + 0x18), a);
                else
                    outPCM(1, (n + 0x8), a);
            }
        }
    }

    /** Flag setting */
    private void FLGSET() {
        int a = work.pg.mData[work.hl++].dat;
        work.soundWork.setFLGADR(a);
    }

    /** WRITE REG */
    private void W_REG() {
        int d = work.pg.mData[work.hl++].dat;
        int e = work.pg.mData[work.hl++].dat;
        outPSG(d, e);
    }

    private void MW_REG() {
        int c = work.pg.mData[work.hl++].dat;
        int p = work.pg.mData[work.hl++].dat;
        int d = work.pg.mData[work.hl++].dat;
        int e = work.pg.mData[work.hl++].dat;
        outPSG(c, p, d, e);
    }

    private void CH3SP() {
        int c = work.pg.mData[work.hl++].dat;
        if (c == 0x00) {
            int sw = work.pg.mData[work.hl++].dat;
            if (work.soundWork.getCurrentChip() != 4) { // If not OPM, set sound effect mode
                if (sw == 0) TO_NML();
                else TO_EFC();
            }
        } else {
            int slot = work.pg.mData[work.hl++].dat;
            work.pg.useSlot = slot;
        }
    }

    /** volume up & down */
    private void VOLUPF() {
        List<Object> args;
        // LinePos lp;

        if (work.soundWork.getDrmF1() != 0) {
            int n = work.pg.mData[work.hl++].dat & 0xff;

            if (work.isDotNET) {
                if ((n & 0x80) != 0) {
                    VOLUPF_Rhythm(n);
                    return;
                }
            }

            work.pg.volume += (byte) n;
//logger.log(Level.INFO, "work.pg.volume: " + work.pg.volume);
            // For parameter display
            args = new ArrayList<>();
            args.add(work.pg.volume & 0x3f);
            outDummy(MMLType.Volume, args);

            DVOLSET();
            return;
        }

        if (work.cd.getFmVolMode() != 3) {
            work.pg.volume += (byte) work.pg.mData[work.hl++].dat;
//logger.log(Level.INFO, "work.pg.volume: " + work.pg.volume + ", %x".formatted(work.pg.mData[work.hl - 1].dat));
        } else {
            int n = (-((byte) work.pg.mData[work.hl++].dat)) & 0xff;
            for (int i = 0; i < 4; i++) {
                work.pg.getTlDirectTable()[i] += n;
            }
        }

        if (work.soundWork.getPcmFlg() != 0) {
            args = new ArrayList<>();
            args.add(work.pg.volume - 4);
            outDummy(MMLType.Volume, args);
            return;
        }

        STVOL();
    }

    private void VOLUPF_Rhythm(int a) {
        int inst = work.pg.instrumentNumber;
        for (int i = 0; i < 6; i++) {
            if (((inst >> i) & 1) != 0) {
                int b = ((byte) ((a & 0x3f) | ((a & 0x40) != 0 ? 0xc0 : 0) + (work.soundWork.drmvol[work.soundWork.getCurrentChip()][i] & 0x3f))) & 0xff;

                // For parameter display
                List<Object> args = new ArrayList<>();
                args.add(b);
                outDummy(MMLType.Volume, args);

                b = (work.soundWork.drmvol[work.soundWork.getCurrentChip()][i] & 0b1100_0000) | b;
                work.soundWork.drmvol[work.soundWork.getCurrentChip()][i] = b;
                // if (Work.cd.getCurrentPageNo() == Work.pg.getPageNo())
                {
                    if (work.soundWork.getCurrentChip() < 2)
                        outPSG(i + 0x18, b);
                    else
                        outPCM(1, i + 0x8, b);
                }
            }
        }
    }

    /** hard lfo set */
    private void HLFOON() {
        int a = work.pg.mData[work.hl++].dat; // freq cont
        a |= 0b0000_1000;
        outPSG(0x22, a);

        int c = work.pg.mData[work.hl++].dat; // pms
        c = (c & 0xff) | (work.pg.mData[work.hl++].dat << 4); // ams+pms
        int de = work.soundWork.getFmPort() + work.pg.channelNumber * 10 + work.pg.getPageNo(); // paldat
        a = (SoundWork.PALDAT[de] & 0b1100_0000) | c;
        SoundWork.PALDAT[de] = a;
        outPSG(0xb4 + work.pg.channelNumber, a);
    }

    private void TIE() {
        work.pg.keyOffFlag = false;
    }

    /** repeat skip */
    private void RSKIP() {
        int e = work.pg.mData[work.hl++].dat;
        int d = work.pg.mData[work.hl++].dat;

        int hl = work.hl;
        hl -= 2;
        hl += e + d * 0x100;

        int a = work.pg.mData[hl].dat;
        a--; // LOOP counter = 1 ?
        if (a == 0) {
            hl += 4; // HL = JUMP ADR
            work.hl = hl;
        }
    }

    private void SECPRC() {
        int a = work.pg.mData[work.hl++].dat;
        a &= 0xf; // A=COMMAND No.(0-F)

        if (work.soundWork.getSsgF1() == 0 || (work.soundWork.getSsgF1() != 0 && !work.isDotNET))
            fmCom2[a].run();
        else
            psgCom2[a].run(); // kuma: This is a table for DotNET only.
    }

    private void NTMEAN() {
    }

    /** PCM VMODE CHANGE */
    private void PVMCHG() {
        int a = work.pg.mData[work.hl++].dat;
        work.soundWork.setPvMode(a);
    }

    /** Reverb */
    private void REVERVE() {
        int a = work.pg.mData[work.hl++].dat;
        work.pg.reverbVol = a;
        // RV1:
        work.pg.reverbFlg = true;
    }

    private void REVSW() {
        int a = work.pg.mData[work.hl++].dat;
        if (a != 0) {
            // goto RV1;
            work.pg.reverbFlg = true;
            return;
        }
        work.pg.reverbFlg = false;

        // if (Work.idx >= 3 && Work.idx <= 5) return;
        if (work.soundWork.getSsgF1() != 0) return;

        STVOL();
    }

    private void REVMOD() {
        int a = work.pg.mData[work.hl++].dat;
        if (a != 0) {
            work.pg.reverbMode = true;
            return;
        }
// RM2:
        work.pg.reverbMode = false;
    }

    /** set PSG tone */
    private void OTOSSG() {
        outDummy();

        int a = work.pg.mData[work.hl++].dat;

        // OTOCAL
        int ptr = 0; // ssgDat;
        ptr = a * 6;
        // ENVPST();
        System.arraycopy(work.soundWork.SSGDAT, ptr + 0, work.pg.softEnvelopeParam, 0, 6);
        work.pg.volume = work.pg.volume | 0b1001_0000;
    }

    private void OTOSET() {
        int a = work.pg.mData[work.hl++].dat;

        // otocal
        int ptr = 0; // ssgDat
        ptr = a * 6;

        for (int i = 0; i < 6; i++) {
            work.soundWork.SSGDAT[ptr + i] = work.pg.mData[work.hl++].dat;
        }
    }

    /** Envelope parameter setting */
    private void ENVPST() {
        for (int i = 0; i < 6; i++) {
            work.pg.softEnvelopeParam[i] = work.pg.mData[work.hl++].dat;
        }
        work.pg.volume = work.pg.volume | 0b1001_0000; // set envelope flag, attack flag
    }

    private void ENVPSTex() {
        for (int i = 0; i < 6; i++) {
            work.pg.softEnvelopeParam[i] = work.pg.mData[work.hl++].dat;
        }
        work.pg.softEnvelopeFlag = 0b1001_0000; // set envelope flag, attack flag
    }

    /** PSG volume */
    private void PSGVOL() {
        outDummy();
        work.pg.hardEnveFlg = false;
        int e = work.pg.volume & 0b1111_0000;
        int c = work.pg.mData[work.hl].dat;
        PV1(c, e);
    }

    private void PV1(int c, int e) {
        int a = work.soundWork.getTOTALV();
        a += c;
        if (a >= 16) { // goto PV2;
            a = 0;
        }
//PV2:
        a |= e;
        work.hl++;
        work.pg.volume = a;
    }

    /** mix port control */
    private void NOISE() {
        work.pg.setBackupMIXPort(work.pg.mData[work.hl++].dat);
        if (work.pg.getPageNo() != work.cd.getCurrentPageNo()) return;

        tNOISE();
    }

    private void restoreNOISE() {
        tNOISE();
    }

    private void tNOISE() {
        int c = work.pg.getBackupMIXPort();
        int b = work.pg.channelNumber;
        int e = work.soundWork.pregBf[work.soundWork.getCurrentChip()][5];
        b >>= 1;
        b++;
        int d = b;
        int a = 0b0111_1011;
//NOISE1:
        do {
            a = (a << 1) | (a >> 7);
            b--;
        } while (b != 0);
        a &= e;
        e = a;
        a = c;
        b = d;
        a = (a >> 1) | (a << 7);
//NOISE2:
        do {
            a = (a << 1) | (a >> 7);
            b--;
        } while (b != 0);
        a |= e;
        d = 7;
        e = a;
        outPSG(d, e);
        work.soundWork.pregBf[work.soundWork.getCurrentChip()][5] = e;
    }

    /** Noise Frequency */
    private void NOISEW() {
        work.pg.setBackupNoiseFrq(work.pg.mData[work.hl++].dat);
        if (work.pg.getPageNo() != work.cd.getCurrentPageNo()) return;

        tNOISEW();
    }

    private void restoreNOISEW() {
        tNOISEW();
    }

    private void tNOISEW() {
        int e = work.pg.getBackupNoiseFrq();
        outPSG(6, e);
        work.soundWork.pregBf[work.soundWork.getCurrentChip()][4] = e;
    }

    /** ssg volume up & down */
    private void VOLUPS() {
        int d = work.pg.mData[work.hl++].dat;
        if (!work.pg.hardEnveFlg) {
            int a = work.pg.volume;
            int e = a;
            a &= 0b0000_1111;
            a += d;
            if (a >= 16) {
                return;
            }
            d = a;
            a = e;
            a &= 0b1111_0000;
            a |= d;
            work.pg.volume = a;
logger.log(Level.TRACE, "work.pg.volume: " + work.pg.volume);

            List<Object> args = new ArrayList<>();
            args.add(d);
            outDummy(MMLType.Volume, args);
        }
    }

    /** LFO routine */
    private void PLLFO() {
        if (!checkCh3SpecialMode() && work.pg.getPageNo() != work.cd.getCurrentPageNo()) return;

        // FOR FM & SSG LFO
        if (!work.pg.lfoflg) {
            return;
        }
        int hl = work.pg.dataAddressWork;
        hl--;
        int a = work.pg.mData[hl].dat;
        if ((a & 0xff) == 0xf0) {
            return; // If the previous data is '&', RET
        }
        if (!work.pg.lfoContFlg) {
            // LFO INITIARIZE
            LFORST();
            LFORST2();
            work.pg.lfoCounterWork = work.pg.lfoCounter;
            work.pg.lfoContFlg = true; // SET CONTINUE FLAG
        }
// CTLFO:
        if (work.pg.lfoDelayWork == 0) { // If the delay is complete, proceed to the next step.
            CTLFO1();
            return;
        }
        work.pg.lfoDelayWork--; // Delay countdown
    }

    private void CTLFO1() {
        work.pg.lfoCounterWork--; // Counter
        if (work.pg.lfoCounterWork != 0) {
            return;
        }
        work.pg.lfoCounterWork = work.pg.lfoCounter; // Counter reset
        if (work.pg.lfoPeakWork == 0) { //  GET PEAK LEVEL COUNTER(P.L.C)
            work.pg.lfoDeltaWork = -work.pg.lfoDeltaWork; // WAVE Inversion
            work.pg.lfoPeakWork = work.pg.lfoPeak; //  P.L.C reset
        }
        // PLLFO1:
        work.pg.lfoPeakWork--; // P.L.C.-1
        int hl = work.pg.lfoDeltaWork;

        List<Object> args = new ArrayList<>();
        args.add(hl);
        makeDummyCrrentMmlDatum(MMLType.Lfo, args); // TODO LFODelta

        PLS2(hl);
    }

    private void PLS2(int hl) {
        if (work.soundWork.getPcmFlg() == 0) {
            PLSKI2(hl);
            return;
        }

        hl += work.soundWork.getDeltN()[work.soundWork.getCurrentChip()];
        work.soundWork.getDeltN()[work.soundWork.getCurrentChip()] = hl;

        outPCM(0x09, hl & 0xff);
        outPCM(0x0a, (hl & 0xff00) >> 8);
    }

    private void PLSKI2(int hl) {
        if (work.soundWork.getSsgF1() != 0 && work.pg.getSsgTremoloFlg()) {
            work.pg.addSSGTremoloVol((short) (hl & 0xffff));
// logger.log(Level.TRACE, Work.pg.SSGTremoloVol);
            return;
        }

        if (work.soundWork.getSsgF1() == 0) {
            // KUMA: Limit check processing when in FM

            int[] num = new int[1];
            int dlt = (short) (hl & 0xffff);
//logger.log(Level.TRACE, "b:%d num:%x -> +%d".formatted(blk, num, dlt));

            num[0] = work.pg.fnum & 0x7ff;
            int[] blk = {work.pg.fnum >> 11};
            num[0] += dlt;
            getFNum(/* ref */ blk, /* ref */ num);
//logger.log(Level.TRACE, " -> b:%d num:%x".formatted(blk,num));
            hl = (blk[0] << 11) | num[0];
        } else {
            // KUMA: Existing processing for SSG

            int de = work.pg.fnum; // GET FNUM1
            // get b/fNum2
            hl += de; //  HL= NEW F-NUMBER
            hl = hl & 0xffff;
        }

        work.pg.fnum = hl; // SET NEW F-NUM1
        // SET NEW F-NUM2

        if (work.soundWork.getSsgF1() == 0) {
            LFOP5(hl);
            return;
        }

        // FOR SSG LFO
        int a = work.pg.beforeCode; // GET KEY CODE&octave
        a >>= 4;
        if (a != 0) { // octave=1?
            int b = a;
//SNUMGETL:
            do {
                hl >>= 1;
                b--;
            } while (b != 0);
        }
//SSLFO2:
        int e = hl;
        int d = work.pg.channelNumber;
        outPSG(d, e);
        d++;
        e = hl >> 8;
        outPSG(d, e);
    }

    private static void getFNum(/* ref */ int[] blk, /* ref */ int[] num) {
        int NoteC = 0x26a;
        while (num[0] < NoteC) {
            if (blk[0] == 0) {
                break;
            }
            blk[0]--;
            num[0] = NoteC * 2 - (NoteC - num[0]);
        }
        while (num[0] >= NoteC * 2) {
            if (blk[0] == 7) {
                break;
            }
            blk[0]++;
            num[0] = num[0] - NoteC * 2 + NoteC;
        }
        num[0] = Math.clamp(num[0], 0, 0x7ff);
    }

    /** for fm lfo */
    private void LFOP5(int hl) {
        if (work.pg.tlLfoFlag) {
            if (work.soundWork.getCurrentChip() == 4) {
                LFOP6opm(hl);
                return;
            }
            LFOP6(hl);
            return;
        }

        if (work.soundWork.getCurrentChip() == 4) {
            PLLFO2opm(hl);
            return;
        }

        if ((work.soundWork.getCurrentCh() / 10) != 2) { // CH=3?
            PLLFO2(hl); // NOT CH3 THEN PLLFO2
            return;
        }

        if (!work.soundWork.ch3SpMode(work.soundWork.getCurrentChip())) {
            PLLFO2(hl); // NOT SE MODE
            return;
        }

        work.soundWork.setNEWFNM(hl);
//LFOP4:
        hl = 0;
        int iy = 0;
        byte b = 4;
//LFOP3:
        do {
            int[] blk = {(work.soundWork.getNEWFNM() >> 11) & 0x7};
            int[] num = {(work.soundWork.getNEWFNM() & 0x7ff) + work.soundWork.detdat[work.soundWork.getCurrentChip()][hl++]};
            getFNum(/* ref */ blk, /* ref */ num);
            int fnum = (blk[0] << 11) | num[0];

            int d = work.soundWork.opSel[iy++];
            int e = fnum >> 8;
            outPSG(d, e);

            d -= 4;
            e = fnum;
            outPSG(d, e);

            b--;
        } while (b != 0);
    }

    private void PLLFO2(int hl) {
        int d = 0xa4; // PORT A4H
        d += work.pg.channelNumber;
        int e = hl >> 8;
        outPSG(d, e);

        d -= 4;
        e = hl; // F-NUMBER1 DATA
        outPSG(d, e);
    }

    private void PLLFO2opm(int hl) {
        int oct = (hl & 0x3800) >> 11;
        int note = (hl & 0x7ff) >> 6;
        note--;
        if (note == 0xff) {
            oct--;
            note = 11;
        }
        note = (note < 3 ? note : (note < 6 ? (note + 1) : (note < 9 ? (note + 2) : (note + 3))));

        int e = (oct << 4) | (note & 0xff); // oct:bit6-4 note :bit3-0
        int d = 0x28; // KC address
        d += work.pg.channelNumber;
        outPSG(d, e);
//logger.log(Level.TRACE, "PLLFO2opm:d:%02x e:%02x".formatted(d, e));
        d += 8; // KF address
        e = (hl & 0x3f) << 2; // KF (bit:7-2)
        outPSG(d, e);
//logger.log(Level.TRACE, "PLLFO2opm:d:%02x e:%02x".formatted(d, e));
    }

    private void LFOP6(int hl) {
        int c = work.pg.tlLfoSlot;

        int d = 0x40;
        d += work.pg.channelNumber;
        int e = hl;

        if ((c & 0x01) != 0) outPSG(d, e);
        d += 4;
        if ((c & 0x04) != 0) outPSG(d, e);
        d += 4;
        if ((c & 0x02) != 0) outPSG(d, e);
        d += 4;
        if ((c & 0x08) == 0) return;
        outPSG(d, e);
    }

    private void LFOP6opm(int hl) {
        int c = work.pg.tlLfoSlot; //.soundWork.LFOP6_VAL;

        int d = 0x60;
        d += work.pg.channelNumber;
        int e = hl;

        if ((c & 0x01) != 0) outPSG(d, e);
        d += 8;
        if ((c & 0x04) != 0) outPSG(d, e);
        d += 8;
        if ((c & 0x02) != 0) outPSG(d, e);
        d += 8;
        if ((c & 0x08) == 0) return;
        outPSG(d, e);
    }

    private void prcLFO() {
        if (!checkCh3SpecialMode() && work.pg.getPageNo() != work.cd.getCurrentPageNo()) return;

        // for fm & ssg lfo
        if (!work.pg.lfoflg) {
            return;
        }

        int hl = work.pg.dataAddressWork;
        hl--;
        int a = work.pg.mData[hl].dat;
        if (a == 0xf0) {
            return; // If the previous data is '&', RET
        }

        if (!work.pg.lfoContFlg) {
            // lfo initialize
            LFORST();
            LFORST2();
            work.pg.lfoCounterWork = work.pg.lfoCounter;
            work.pg.lfoContFlg = true; // set continue flag
        }

//CTLFO:
        if (work.pg.lfoDelayWork == 0) { // If the delay is complete, proceed to the next step.
            prcCTLFO1();
            return;
        }
        work.pg.lfoDelayWork--; // Delay countdown
    }

    private void prcCTLFO1() {
        work.pg.lfoCounterWork--; // counter
        if (work.pg.lfoCounterWork != 0) {
            return;
        }

        work.pg.lfoCounterWork = work.pg.lfoCounter; // Counter reset
        if (work.pg.lfoPeakWork == 0) { // get peak level counter(p.l.c)
            work.pg.lfoDeltaWork = -work.pg.lfoDeltaWork; // WAVE inversion
            work.pg.lfoPeakWork = work.pg.lfoPeak; // P.L.C reset
        }

//PLLFO1:
        work.pg.lfoPeakWork--; // p.l.c.-1
        int hl = work.pg.lfoDeltaWork;
        prcPLS2(hl);
    }

    private void prcPLS2(int hl) {
        if (work.soundWork.getPcmFlg() == 0) {
            prcPLSKI2(hl);
            return;
        }

        hl += work.soundWork.getDeltN()[work.soundWork.getCurrentChip()];
        work.soundWork.getDeltN()[work.soundWork.getCurrentChip()] = hl;
    }

    private void prcPLSKI2(int hl) {
        if (work.soundWork.getSsgF1() != 0 && work.pg.getSsgTremoloFlg()) {
            work.pg.addSSGTremoloVol((short) (hl & 0xffff));
//logger.log(Level.TRACE, Work.pg.SSGTremoloVol);
            return;
        }

        if (work.soundWork.getSsgF1() == 0) {
            // KUMA: Limit check processing when in FM

            int[] num = new int[1];
            int dlt = (short) (hl & 0xffff);
//logger.log(Level.TRACE, "b:%d num:%x -> +%d".formatted(blk, num, dlt));

            if (work.soundWork.getCurrentChip() != 4) {
                num[0] = work.pg.fnum & 0x7ff;
                int[] blk = {work.pg.fnum >> 11};
                num[0] += dlt;
                getFNum(/*ref*/ blk, /*ref*/ num);
//logger.log(Level.TRACE, " -> b:%d num:%x".formatted(blk, num));
                hl = (blk[0] << 11) | num[0];
            } else {
                num[0] = addDetuneToFNumOpm(work.pg.fnum, dlt);
                hl = num[0];
            }
        } else {
            // KUMA: Existing processing for SSG

            int de = work.pg.fnum; // GET FNUM1
            // get b/fNum2
            hl += de; // HL= NEW F-NUMBER
            hl = (short) hl;
        }

        work.pg.fnum = hl; // SET NEW F-NUM1
        // SET NEW F-NUM2
    }

    private void PORTAON() {
        work.pg.portaFlg = true;
        work.pg.portaContFlg = false;
        work.pg.portaWorkClock = 0;

        work.pg.portaStNote = work.pg.mData[work.hl++].dat;
        work.pg.portaEdNote = work.pg.mData[work.hl++].dat;
        work.pg.portaTotalClock = work.pg.mData[work.hl++].dat;
        work.pg.portaTotalClock += (work.pg.mData[work.hl++].dat) << 8;
    }

    private void prcPortamento() {
        if (!checkCh3SpecialMode() && work.pg.getPageNo() != work.cd.getCurrentPageNo()) return;

        if (!work.pg.portaFlg) {
            return;
        }

        int hl = work.pg.dataAddressWork;
        hl--;
        int a = work.pg.mData[hl].dat;
        if (a == 0xf0) {
            return; // If the previous data is '&', RET
        }

        if (!work.pg.portaContFlg) {
            // portamento initialize
            work.pg.portaWorkClock = 0;
            work.pg.portaContFlg = true;
        }

        prcCTPRO1();
    }

    private void prcCTPRO1() {
        if (work.soundWork.getPcmFlg() != 0) {
            prcCTPRO1_PCM();
            return;
        }

        if (work.soundWork.getSsgF1() != 0) {
            prcCTPRO1_SSG();
            return;
        }

        prcCTPRO1_FM();
    }

    private void prcCTPRO1_FM() {
        if (work.pg.portaTotalClock == 0) return;

        int stOct = work.pg.portaStNote >> 4;
        int stNote = work.pg.portaStNote & 0xf;
        int edOct = work.pg.portaEdNote >> 4;
        int edNote = work.pg.portaEdNote & 0xf;
        boolean isNeg = work.pg.portaEdNote < work.pg.portaStNote;
        int noteDisatance = Math.abs((stOct * 12 + stNote) - (edOct * 12 + edNote));

        // Pitch change range * elapsed clock / total portamento clock = how much the pitch has changed from the starting pitch
        double noteDelta = noteDisatance * work.pg.portaWorkClock / (double) work.pg.portaTotalClock;

        // Separate into integer and decimal parts
        int iNoteDelta = (int) noteDelta;
        iNoteDelta = isNeg ? -iNoteDelta : iNoteDelta;
        noteDelta -= iNoteDelta;

        // Get fNum from pitch
        int a = iNoteDelta + (stNote + stOct * 12);
        int b = (a + 12) % 12;
        int n = (a + (isNeg ? 11 : 1)) % 12;

        int bsOct;
        int nxOct;
        bsOct = a / 12;
        nxOct = (a + (isNeg ? -1 : 1)) / 12;

        int bsFnum = 0, nxFnum = 0;
        if (work.soundWork.getCurrentChip() < 2) {
            bsFnum = work.soundWork.FNUMB[0][b] + bsOct * work.soundWork.FNUMB[0][0];
            nxFnum = work.soundWork.FNUMB[0][n] + nxOct * work.soundWork.FNUMB[0][0];
        } else if (work.soundWork.getCurrentChip() < 4) {
            bsFnum = work.soundWork.FNUMB[1][b] + bsOct * work.soundWork.FNUMB[1][0];
            nxFnum = work.soundWork.FNUMB[1][n] + nxOct * work.soundWork.FNUMB[1][0];
        } else {
            bsFnum = work.soundWork.FNUMBopm[0][b] + bsOct * 0x300;
            nxFnum = work.soundWork.FNUMBopm[0][n] + nxOct * 0x300;
        }

        // Calculate fNum from the decimal point
        double d = isNeg ? ((bsFnum - nxFnum) * (1.0 - (noteDelta - noteDelta))) : ((nxFnum - bsFnum) * noteDelta);
        d += isNeg ? nxFnum : bsFnum;

        if (work.pg.portaWorkClock == 0) work.pg.portaBeforeFNum = isNeg ? bsFnum : (int) d;
        int delta = (int) (d - work.pg.portaBeforeFNum);
        work.pg.portaBeforeFNum = (int) d;

//logger.log(Level.TRACE, "%s %d %s".formatted(isNeg, d, nxFnum));

        int[] num = new int[1];
        int dlt = (short) (delta & 0xffff);

        if (work.soundWork.getCurrentChip() != 4) {
            num[0] = work.pg.fnum & 0x7ff;
            int[] blk = {work.pg.fnum >> 11};
            num[0] += dlt;
            getFNum(/* ref */ blk, /* ref */ num);
            delta = (blk[0] << 11) | num[0];
        } else {
            num[0] = addDetuneToFNumOpm(work.pg.fnum, dlt);
            delta = num[0];
        }

        work.pg.fnum = delta;
        work.pg.portaWorkClock++;
        if (work.pg.portaWorkClock == work.pg.portaTotalClock) {
            work.pg.portaFlg = false;
        }
    }

    private void prcCTPRO1_SSG() {
        if (work.pg.portaTotalClock == 0) return;

        int stOct = work.pg.portaStNote >> 4;
        int stNote = work.pg.portaStNote & 0xf;
        int edOct = work.pg.portaEdNote >> 4;
        int edNote = work.pg.portaEdNote & 0xf;
        boolean isNeg = work.pg.portaEdNote < work.pg.portaStNote;
        int noteDisatance = Math.abs((stOct * 12 + stNote) - (edOct * 12 + edNote));

        // Pitch change range * elapsed clock / total portamento clock = how much the pitch has changed from the starting pitch
        double noteDelta = noteDisatance * work.pg.portaWorkClock / (double) work.pg.portaTotalClock;

        // Separate into integer and decimal parts
        int iNoteDelta = (int) noteDelta;
        iNoteDelta = isNeg ? -iNoteDelta : iNoteDelta;
        noteDelta -= iNoteDelta;

        // Get fNum from pitch
        int a = iNoteDelta + (stNote + stOct * 12);
        int b = (a + 12) % 12;
        int n = (a + (isNeg ? 11 : 1)) % 12;

        int bsOct;
        int nxOct;
        bsOct = a / 12;
        nxOct = (a + (isNeg ? -1 : 1)) / 12;

        int bsFnum = 0, nxFnum = 0;
        if (work.soundWork.getCurrentChip() < 2) {
            bsFnum = work.soundWork.SNUMB[0][b];
            if (nxOct - bsOct >= 0) nxFnum = work.soundWork.SNUMB[0][n] >> (nxOct - bsOct);
            else nxFnum = work.soundWork.SNUMB[0][n] << (bsOct - nxOct);
        } else if (work.soundWork.getCurrentChip() < 4) {
            bsFnum = work.soundWork.SNUMB[1][b];
            if (nxOct - bsOct >= 0) nxFnum = work.soundWork.SNUMB[1][n] >> (nxOct - bsOct);
            else nxFnum = work.soundWork.SNUMB[1][n] << (bsOct - nxOct);
        }

        // Calculate fNum from the decimal point
        double d = isNeg ? ((bsFnum - nxFnum) * (1.0 - (noteDelta - noteDelta))) : ((nxFnum - bsFnum) * noteDelta);
        d += isNeg ? nxFnum : bsFnum;

        if (work.pg.portaWorkClock == 0) work.pg.portaBeforeFNum = isNeg ? bsFnum : (int) d;
        int delta = (int) (d - work.pg.portaBeforeFNum);
        work.pg.portaBeforeFNum = (int) d;

        //logger.log(Level.TRACE, "%s %d %d".formatted(isNeg, d, nxFnum));

        delta += work.pg.fnum;
        work.pg.beforeCode = bsOct << 4;

        work.pg.fnum = delta;
        work.pg.portaWorkClock++;
        if (work.pg.portaWorkClock == work.pg.portaTotalClock) {
            work.pg.portaFlg = false;
        }
    }

    private void prcCTPRO1_PCM() {
        if (work.pg.portaTotalClock == 0) return;

        int stOct = work.pg.portaStNote >> 4;
        int stNote = work.pg.portaStNote & 0xf;
        int edOct = work.pg.portaEdNote >> 4;
        int edNote = work.pg.portaEdNote & 0xf;
        boolean isNeg = edNote < stNote;
        int noteDisatance = Math.abs(stNote - edNote);
        if (stOct != edOct) {
            isNeg = stOct < edOct;
            noteDisatance = isNeg
                    ? (stNote + 12 * (edOct - stOct) - edNote)
                    : (edNote + 12 * (stOct - edOct) - stNote)
            ;
        }

        // Pitch change range * elapsed clock / total portamento clock = how much the pitch has changed from the starting pitch
        double noteDelta = noteDisatance * work.pg.portaWorkClock / (double) work.pg.portaTotalClock;

        // Separate into integer and decimal parts
        int iNoteDelta = (int) noteDelta;
        noteDelta -= iNoteDelta;
        iNoteDelta = isNeg ? -iNoteDelta : iNoteDelta;

        // Get fNum from pitch
        int a = iNoteDelta + stNote;
        int b = a % 12;
        b += b < 0 ? 12 : 0;
        int n = (a + (isNeg ? -1 : 1)) % 12;
        n += n < 0 ? 12 : 0;

        int bsOct;
        int nxOct;
        if (isNeg) {
            bsOct = stOct + ((11 - stNote) - iNoteDelta) / 12;
            nxOct = b < n ? (bsOct + 1) : bsOct;
        } else {
            bsOct = stOct - (stNote + iNoteDelta) / 12;
            nxOct = b > n ? (bsOct - 1) : bsOct;
        }

        int bsFnum = 0, nxFnum = 0;
        if (work.soundWork.getCurrentChip() < 2) {
            bsFnum = work.soundWork.PCMNMB[0][b] >> bsOct;
            nxFnum = work.soundWork.PCMNMB[0][n] >> nxOct;
        } else if (work.soundWork.getCurrentChip() < 4) {
            bsFnum = work.soundWork.PCMNMB[1][b] >> bsOct;
            nxFnum = work.soundWork.PCMNMB[1][n] >> nxOct;
        }

        // Calculate fNum from the decimal point
        double d = isNeg ? ((bsFnum - nxFnum) * (1.0 - noteDelta)) : ((nxFnum - bsFnum) * noteDelta);
        d += isNeg ? nxFnum : bsFnum;

        if (work.pg.portaWorkClock == 0) work.pg.portaBeforeFNum = isNeg ? bsFnum : (int) d;
        int delta = (int) (d - work.pg.portaBeforeFNum);
        work.pg.portaBeforeFNum = (int) d;

        //logger.log(Level.TRACE, "%s %d  %d  %d  %d".formatted(isNeg, d, nxFnum, bsOct, nxOct));

        delta += work.pg.fnum;
        work.pg.beforeCode = bsOct << 4;

        work.pg.fnum = delta;
        work.pg.portaWorkClock++;
        if (work.pg.portaWorkClock == work.pg.portaTotalClock) {
            work.pg.portaFlg = false;
        }
    }

    private void prcWriteFnum() {
        int hl;

        if (work.soundWork.getPcmFlg() != 0) {
            hl = work.soundWork.getDeltN()[work.soundWork.getCurrentChip()] + work.pg.fnum;
            if (work.soundWork.getCurrentChip() < 2) {
                outPCM(0x09, (hl & 0xff));
                outPCM(0x0a, (hl & 0xff00) >> 8);
            } else {
                outPCM(0, 0x19, hl & 0xff);
                outPCM(0, 0x1a, (hl & 0xff00) >> 8);
            }
            return;
        }

        if (work.soundWork.getSsgF1() != 0) {
            // for ssg lfo
            hl = work.pg.fnum;
            int a = work.pg.beforeCode; // get key code&octave
            a >>= 4;
            if (a != 0) { // octave=1?
                int b = a;
                do {
                    hl >>= 1;
                    b--;
                } while (b != 0);
            }
            int e = hl;
            int d = work.pg.channelNumber;
            outPSG(d, e);
            d++;
            e = hl >> 8;

            if (work.SSGExtend) {
                if (work.pg.getSsgWfNum() != 0) {
                    e |= work.pg.getSsgWfNum() << 4;
                }
            }

            outPSG(d, e);
            return;
        }

        LFOP5(work.pg.fnum);
    }

    private void prcSoftEnvelope() {
        if ((work.pg.softEnvelopeFlag & 0x80) == 0) return;
        SOFENVex();
    }

    // SSG:

    /** SSG sound source performance routine */
    private void SSGSUB() {
        // Work.cd = Work.soundWork.chData[Work.idx];
        // Work.pg = Work.cd.pgDat.get(0);
        work.hl = work.pg.dataAddressWork;

        work.pg.lengthCounter = work.pg.lengthCounter - 1;
        if (work.pg.lengthCounter == 0) {
            SSSUB7();
            return;
        }

        if (work.pg.lengthCounter != work.pg.quantize) {
            SSSUB0();
            return;
        }

        if (work.pg.mData[work.pg.dataAddressWork].dat != 0xfd) { // count over? goto SSUB0;
            SSSUBA(); // to release
//            return; // ret
        } else {
//SSUB0:
            work.pg.keyOffFlag = false; // set tie flag (Maybe reset the key off)
            SSSUB0();
        }
    }

    private void SSSUB0() {
        if (work.pg.getPageNo() != work.cd.getCurrentPageNo())
            return;

        if ((work.pg.volume & 0x80) == 0) { // envelope check
            return;
        }

        SOFENV();

        if (work.pg.getSsgTremoloFlg()) {
            work.aReg = Math.clamp(work.aReg + work.pg.getSsgTremoloVol(), 0, 15);
            //logger.log(Level.TRACE, "%d".formatted(Work.pg.SSGTremoloVol));
        }

        int e = work.aReg;
        if (work.soundWork.getReady() == 0) {
            e = 0;
        }
        if (work.soundWork.getKEY_FLAG() != 0xff) {
            int d = work.pg.volReg;
            if (work.SSGExtend) e |= work.pg.panValue << 6;
            if (work.pg.getPageNo() == work.cd.getCurrentPageNo()) outPSG(d, e);
        }
    }

    private void SSSUB7() {
        work.hl = work.pg.dataAddressWork;
        if (work.pg.mData[work.hl].dat == 0xfd) { // count OVER?
//SSUB1:
            work.pg.keyOffFlag = false; // set tie flag(Maybe reset the key off)
            work.hl++;
            SSSUBB();
            return;
        }
//SSUBE:
        work.pg.keyOffFlag = true;
        SSSUBB();
    }

    /** RR processing when KEY OFF */
    private void SSSUBA() {
        // HARD ENV.KEY OFF
        if (work.pg.hardEnveFlg) {
            if (work.pg.getPageNo() == work.cd.getCurrentPageNo())
                outPSG(work.pg.volReg, 0); // SSG KEY OFF
        }

        // SOFT ENV.KEY OFF

        if (work.pg.reverbFlg) {
            work.pg.keyOffFlag = false;
            SSSUB0();
            return;
        }

//SSUBAC:
        if ((work.pg.volume & 0x80) == 0) {
            SSSUB3(0); // if not releasing SSSUB3
            return;
        }
        work.pg.volume &= 0b1000_1111; // STATE 4 (release)
        SOFEV9();
        SSSUB3(work.aReg);
    }

    private void SSSUBB() {
        work.crntMmlDatum = work.pg.mData[work.hl];

        int a;
        boolean nrFlg = false;
        do {
            a = work.pg.mData[work.hl].dat;
            while (a == 0) { // CHECK END MARK
                work.pg.loopEndFlg = true;
                // HL=DATA TOP ADD
                if (work.pg.dataTopAddress == -1 || nrFlg) {
                    if (nrFlg)
                        work.abnormalEnd = true;
                    SSGEND();
                    return;
                }
                work.hl = work.pg.dataTopAddress;
                a = work.pg.mData[work.hl].dat; // GET FLAG & LENGTH
                work.pg.incloopCounter();
                // if (Work.pg.loopCounter > Work.nowLoopCounter) Work.nowLoopCounter = Work.pg.loopCounter;
                nrFlg = true;
            }

            // Evacuate performance information
            work.crntMmlDatum = work.pg.mData[work.hl];

//SSSUB1:
//SSSUB2:
            a = work.pg.mData[work.hl++].dat; // input flag &length
            if ((a & 0xff) < 0xf0) break;
            // COMMAND OF PSG?
            //SSSUB8();
            a &= 0xf; // A=COMMAND No.(0-F)
            psgCom[a].run();
        } while (true);

        nrFlg = false;
        boolean carry = ((a & 0x80) != 0);
        a &= 0x7f; // CY=REST FLAG

        work.pg.lengthCounter = a; // set wait counter
        // if rest then SSSUBA
        if (carry) {
            work.crntMmlDatum = work.pg.mData[work.hl - 1];
            SSSUBA();
            SETPT();
            return;
        }

        // set fine tune & coarse tune
//SSSUB6:
        a = work.pg.mData[work.hl++].dat; // load oct & key code
        int b, c;
        if (!work.pg.keyOffFlag) {
            c = a;
            b = work.pg.beforeCode & 0xff;
            a -= b;
            if (a == 0) {
                SETPT(); // if now code=before code then setpt
                return;
            }
            a = c;
            //goto ssskip0; // non tie
        }

//ssskip0:
        work.pg.beforeCode = a; // store key code & octave

        // Mem.stack.Push(Z80.HL);

        if (work.cd.getKeyOnCh() != -1 && work.cd.getKeyOnCh() != work.pg.getPageNo()) {
            SETPT(); // KUMA: Update playing position
            return;
        }
        work.cd.setKeyOnCh(work.pg.getPageNo());
        if (work.cd.getCurrentPageNo() != work.pg.getPageNo()) {
            work.cd.setCurrentPageNo(work.pg.getPageNo());
            // Restoring
            restoreNOISE();
            restoreNOISEW();
            restoreHRDENV();
            restoreENVPOD();
            //Work.pg.lfoflg = false;
        }

        b = a;
        a &= 0b0000_1111; // get key code
        int e = a;
        int hl = work.soundWork.SNUMB[work.soundWork.getCurrentChip() / 2][e]; // GET FNUM2
        int de = work.pg.detune; // get detune data
        hl += de; // detune plus
        hl = (short) hl;
        work.pg.fnum = hl; // save for lfo
        b >>= 4; // octave=1?
        if (b != 0) {
//SSSUB5:
            do {
                hl >>= 1;
                b--;
            } while (b != 0); // determine octave data
            //  if 1 then SSSUB4
        }

        // KUMA: sets FNUM
// SSSUB4:
        e = hl & 0xff;
        int d = work.pg.channelNumber;
        outPSG(d, e);
        e = (hl >> 8);
        if (work.SSGExtend) {
            if (work.pg.getSsgWfNum() != 0) {
                e |= work.pg.getSsgWfNum() << 4;
            }
        }
        d++;
        outPSG(d, e);

        if (!work.pg.keyOffFlag) { // goto SSSUBF;
            SOFENV();
            //goto SSSUB9;
        } else {
//SSSUBF:
            // Processing when KEYON

            if (work.pg.hardEnveFlg) {
                // HARD ENV. KEY ON
                if (work.soundWork.getKEY_FLAG() != 0xff) {
                    outPSG(work.pg.volReg, 0x10);
                }
                outPSG(0x0d, work.pg.hardEnvelopValue);
            } else {
                // SOFT ENV.KEYON

//SSSUBG:
                a = work.pg.volume;
                a &= 0b0000_1111;
                a |= 0b1001_0000; // TO STATE 1 (ATTACK)
                work.pg.volume = a;

                a = work.pg.softEnvelopeParam[0]; //  ENVE INIT
                work.pg.softEnvelopeCounter = a; // KUMA: AL is used as the initial value of the counter.
                work.pg.lfoContFlg = false; // RESET LFO CONTINUE FLAG
                SOFEV7();

//SSSUBH:
                c = work.pg.lfoPeak;
                c >>= 1;
                work.pg.lfoPeakWork = c; //  LFO PEAK LEVEL reset
                work.pg.lfoDelayWork = work.pg.lfoDelay; //  LFO DELAY reset
            }
        }
//SSSUB9:
        //Z80.HL = Mem.stack.Pop();

        // volume OUT PROCESS

        // 
        // ENTRY A: volume DATA
        // 
        SSSUB3(work.aReg);
    }

    /** SOFT ENVELOPE PROCESS */
    private void SOFENV() {
        if ((work.pg.volume & 0x10) != 0) { // CHECK ATTACK FLAG goto SOFEV2; // KUMA: Go to check the decay flag

            int a = work.pg.softEnvelopeCounter; // KUMA: get counter
            int d = work.pg.softEnvelopeParam[1]; // KUMA: get AR
            boolean carry = ((a & 0xff) + (d & 0xff) > 0xff); // KUMA: Did counter + AR exceed 255?
            a += d;
            if (carry) { // goto SOFEV1;
                a = 0xff; // KUMA: The counter exceeded the upper limit, so the counter was changed to 255.
            }
//SOFEV1:
            // KUMA: Updating counter and flag
            work.pg.softEnvelopeCounter = a; // KUMA: counter = counter + AR (Every clock, the counter increases by the AR.)
            if ((a & 0xff) - 0xff != 0) {
                SOFEV7(); // KUMA: If counter has not reached 255, go to SOFEV7
                return;
            }
            a = work.pg.volume; // KUMA: Get current volume & flags
            a ^= 0b0011_0000; // KUMA: Attack flag: off decay flag: on realized with xor (nice)
            work.pg.volume = a; // TO STATE 2 (DECAY) // KUMA: Update current volume & flags
logger.log(Level.TRACE, "work.pg.volume: " + work.pg.volume);
            SOFEV7();
//            return;
//SOFEV2:
        } else if ((work.pg.volume & 0x20) == 0) { // KUMA: Check decay flag // goto SOFEV4; // KUMA: Check the sustain flag
            int a = work.pg.softEnvelopeCounter; // KUMA:get counter
            int d = work.pg.softEnvelopeParam[2]; // GET DECAY // KUMA:get DR
            int e = work.pg.softEnvelopeParam[3]; // GET SUSTAIN // KUMA:get SR
            boolean carry = ((a - d) < 0); // KUMA: If the result of "counter = counter - DR" is less than 0, go to SOFEV8.
            a -= d;
            if (carry // goto SOFEV8; TODO recheck
                    || (a - e < 0)) { // KUMA: If counter-SR is 0 or more, goto SOFEV3
//SOFEV8:
                a = e; // KUMA: counter = SR
            }
//SOFEV3:
            work.pg.softEnvelopeCounter = a; // KUMA:counter = counter - DR (Every clock, the counter decreases by the DR.)
            if ((a - e) != 0) {
                SOFEV7(); // KUMA: If counter has not reached SR, go to SOFEV7
                return;
            }
            a = work.pg.volume; // KUMA: Get current volume & flags
            a ^= 0b0110_0000; // KUMA: decay flag:off, sustain flag:on
            work.pg.volume = a; // TO STATE 3 (SUSTAIN) // KUMA: Update current volume & flags
logger.log(Level.TRACE, "work.pg.volume: " + work.pg.volume);
            SOFEV7();
//            return;
        } else {
//SOFEV4:
            if ((work.pg.volume & 0x40) == 0) { // KUMA: Check sustain flag
                SOFEV9(); // KUMA: goto Release process
                return;
            }
            int a = work.pg.softEnvelopeCounter; // KUMA: get counter
            int d = work.pg.softEnvelopeParam[4]; // GET SUSTAIN LEVEL // KUMA:get SL
            boolean carry = ((a - d) < 0); // KUMA: If the result of "counter = counter - SL" is 0 or more, go to SOFEV5.
            a -= d;
            if (carry) { // goto SOFEV5;
                a = 0; // KUMA: counter=0
            }
//SOFEV5:
            work.pg.softEnvelopeCounter = a; // KUMA:counter = counter - SL (Every clock, the counter decreases by SL.)
            if (a != 0) {
                SOFEV7();
                return;
            }
            a = work.pg.volume; // KUMA: Get current volume & flags
            a &= 0b1000_1111; // KUMA: Resets progress flags used in envelopes
            work.pg.volume = a; // END OF ENVE // KUMA: If SL is reached during KEYON and the counter reaches 0, envelope processing ends.
logger.log(Level.TRACE, "work.pg.volume: " + work.pg.volume);
            SOFEV7();
        }
    }

    private void SOFEV9() {
        int a = work.pg.softEnvelopeCounter; // KUMA:get counter
        int d = work.pg.softEnvelopeParam[5]; // GET RELEASE // KUMA:get RR
        boolean carry = ((a - d) < 0); // KUMA: Decrement counter with RR
        a -= d;
        if (carry) { // goto SOFEVA;
            a = 0;
        }
//SOFEVA:
        work.pg.softEnvelopeCounter = a; // KUMA: Update counter
        SOFEV7();
    }

    /** volume CALCULATE */
    private void SOFEV7() {
        int e = work.pg.softEnvelopeCounter; // KUMA:get counter
        int hl = 0;
        int a = work.pg.volume; // GET volume
        a &= 0b0000_1111;
        a++;
        int b = a; // Repeat count: volume+1 times
//SOFEV6:
        do {
            hl += e;
            b--;
        } while (b != 0);
        a = hl >> 8; // A will contain the value of counter/256, with VOLUME+1 as the maximum value.
        work.aReg = a;
        if (work.pg.keyOffFlag) {
            return;
        }
        if (!work.pg.reverbFlg) {
            return;
        }
        a += work.pg.reverbVol; // .softEnvelopeParam[5];

        work.carry = ((a & 0x01) != 0);
        a >>= 1;
        work.aReg = a;
    }

    private void SOFENVex() {
        if ((work.pg.softEnvelopeFlag & 0x10) != 0) { // CHECK ATTACK FLAG goto SOFEV2; // KUMA: Go to check the decay flag

            int a = work.pg.softEnvelopeCounter; // KUMA:get counter
            int d = work.pg.softEnvelopeParam[1]; // KUMA:get AR
            boolean carry = ((a & 0xff) + (d & 0xff) > 0xff); // KUMA: Did counter + AR exceed 255?
            a += d;
            if (carry) { // goto SOFEV1;
                a = 0xff; // KUMA: The counter exceeded the upper limit, so the counter was changed to 255.
            }
//SOFEV1:
            // KUMA: Updating counter and flag
            work.pg.softEnvelopeCounter = a; // KUMA: counter = counter + AR (Every clock, the counter increases by the AR.)
            if (((a & 0xff) - 0xff) != 0) {
                SOFEV7ex(); // KUMA: If counter has not reached 255, go to SOFEV7
                return;
            }
            a = work.pg.softEnvelopeFlag; // KUMA: Get current volume & flags
            a ^= 0b0011_0000; // KUMA: Attack flag: off decay flag: on realized with xor (nice)
            work.pg.softEnvelopeFlag = a; // TO STATE 2 (DECAY) // KUMA: Update current volume & flags
            SOFEV7ex();
//            return;
//SOFEV2:
        } else if ((work.pg.softEnvelopeFlag & 0x20) == 0) { // KUMA: Check decay flag goto SOFEV4; // KUMA: Check the sustain flag
            int a = work.pg.softEnvelopeCounter; // KUMA:get counter
            int d = work.pg.softEnvelopeParam[2]; // GET DECAY // KUMA:get DR
            int e = work.pg.softEnvelopeParam[3]; // GET SUSTAIN // KUMA:get SR
            boolean carry = ((a - d) < 0); // KUMA: If the result of "counter = counter - DR" is less than 0, go to SOFEV8.
            a -= d;
            if (carry || // ) { goto SOFEV8;
                    (a - e < 0)) { // KUMA: If counter-SR is 0 or more, goto SOFEV3
//SOFEV8:
                a = e; // KUMA: counter = SR
            }
//SOFEV3:
            work.pg.softEnvelopeCounter = a; // KUMA: counter = counter -DR (Every clock, the counter decreases by the DR.)
            if ((a - e) != 0) {
                SOFEV7ex(); // KUMA: If counter has not reached SR, go to SOFEV7
                return;
            }
            a = work.pg.softEnvelopeFlag; // KUMA:Get current volume & flags
            a ^= 0b0110_0000; // KUMA:dcay flag:off  sustain flag:on
            work.pg.softEnvelopeFlag = a; // TO STATE 3 (SUSTAIN) // KUMA:Update current volume & flags
            SOFEV7ex();
//            return;
//SOFEV4:
        } else {
            if ((work.pg.softEnvelopeFlag & 0x40) == 0) { // KUMA: Check sustain flag
                SOFEV9ex(); // KUMA: to Release process
                return;
            }

            int a = work.pg.softEnvelopeCounter; // KUMA: get counter
            int d = work.pg.softEnvelopeParam[4]; // GET SUSTAIN LEVEL // KUMA: get SL
            boolean carry = ((a - d) < 0); // KUMA: If the result of "counter = counter - SL" is 0 or more, go to SOFEV5.
            a -= d;
            if (carry) { // goto SOFEV5;
                a = 0; // KUMA: counter=0
            }
//SOFEV5:
            work.pg.softEnvelopeCounter = a; // KUMA: counter = counter - SL(Every clock, the counter decreases by SL.)
            if (a != 0) {
                SOFEV7ex();
                return;
            }
            a = work.pg.softEnvelopeFlag; // KUMA:Get current volume & flags
            a &= 0b1000_1111; // KUMA: Resets progress flags used in envelopes
            work.pg.softEnvelopeFlag = a; // END OF ENVE // KUMA: If SL is reached during KEYON and the counter reaches 0, envelope processing ends.
            SOFEV7ex();
        }
    }

    private void SOFEV9ex() {
        int a = work.pg.softEnvelopeCounter; // KUMA: get counter
        int d = work.pg.softEnvelopeParam[5]; // GET RELEASE // KUMA get RR
        boolean carry = ((a - d) < 0); // KUMA: Decrement counter with RR
        a -= d;
        if (carry) { // goto SOFEVA;
            a = 0;
        }
//SOFEVA:
        work.pg.softEnvelopeCounter = a; // KUMA: Update counter
        SOFEV7ex();
    }

    private void SOFEV7ex() {
        int e = work.pg.softEnvelopeCounter & 0xff; // KUMA: get counter
        int a = work.pg.volume & 0xff; // GET volume
        a++;
        a = (e * a) >> 8; // A will contain the value of counter/256, with VOLUME+1 as the maximum value.
        work.aReg = a;
        if (work.pg.keyOffFlag) return;
        if (!work.pg.reverbFlg) return;

        a += work.pg.reverbVol; // .softEnvelopeParam[5];
        work.carry = ((a & 0x01) != 0);
        a >>= 1;
        work.aReg = a;
    }

    /** SET POINTER */
    private void SETPT() {
        work.pg.dataAddressWork = work.hl; // SET NEXT SOUND DATA ADDRES
    }

    private void SSGEND() {
        work.pg.setMusicEnd(true);
        work.pg.dataAddressWork = work.hl;
        SKYOFF();
        work.pg.lfoflg = false; // RESET LFO FLAG
    }

    /** SSG KEY OFF */
    private void SKYOFF() {
        work.pg.volume = 0; // ENVE FLAG RESET
        int e = 0;
        int d = work.pg.volReg;
        outPSG(d, e);
    }

    private void SSSUB3(int a) {
        if (!work.pg.hardEnveFlg) {
            int e = a;
            if (work.soundWork.getReady() == 0) {
                e = 0;
            }
            if (work.soundWork.getKEY_FLAG() != 0xff) {
                int d = work.pg.volReg;
                if (work.SSGExtend) e |= work.pg.panValue << 6;
                if (work.pg.getPageNo() == work.cd.getCurrentPageNo()) outPSG(d, e);
            }
        }
        work.pg.dataAddressWork = work.hl;

//        byte e = a; // added
//        if (Work.soundWork.READY == 0) { // added
//            e = 0; // added
//        }
//SSSUB32:
//        byte d = (byte) Work.pg.volReg;
//        outPSG(d, e);
//        SETPT();
    }

    private void HRDENV() {
        work.pg.setBackupHardEnv(work.pg.mData[work.hl++].dat);
        work.pg.hardEnveFlg = true;
        if (work.pg.getPageNo() != work.cd.getCurrentPageNo()) return;

        tHRDENV();
    }

    private void restoreHRDENV() {
        if (!work.pg.hardEnveFlg) return;
        tHRDENV();
    }

    private void tHRDENV() {
        int e = work.pg.getBackupHardEnv();
        int d = 0x0d;
        outPSG(d, e);
        work.pg.hardEnveFlg = true;
        work.pg.hardEnvelopValue = (e & 0xf);
        work.pg.volume = 16;
    }

    private void ENVPOD() {
        work.pg.setBackupHardEnvFine(work.pg.mData[work.hl++].dat);
        work.pg.setBackupHardEnvCoarse(work.pg.mData[work.hl++].dat);
        if (work.pg.getPageNo() != work.cd.getCurrentPageNo()) return;

        tENVPOD();
    }

    private void restoreENVPOD() {
        tENVPOD();
    }

    private void tENVPOD() {
        int e = work.pg.getBackupHardEnvFine();
        int d = 0x0b;
        outPSG(d, e);
        e = work.pg.getBackupHardEnvCoarse();
        d = 0x0c;
        outPSG(d, e);
    }

    private void SetKeyOnDelay() {
        work.pg.keyOnDelayFlag = false;
        for (int i = 0; i < 4; i++) {
            work.pg.kdWork[i] = work.pg.kd[i] = work.pg.mData[work.hl++].dat;
            if (work.pg.kd[i] != 0) work.pg.keyOnDelayFlag = true;
        }

        work.pg.keyOnSlot = 0x00;
        if (!work.pg.keyOnDelayFlag) work.pg.keyOnSlot = 0xf0;
    }

    private void KeyOnDelaying() {
        if (work.soundWork.getDrmF1() != 0) return;
        if (work.soundWork.getPcmFlg() != 0) return;
        if (!work.pg.keyOnDelayFlag) return;

        int newf = work.pg.keyOnSlot;
        for (int i = 0; i < 4; i++) {
            if (work.pg.kdWork[i] == 0) continue;
            work.pg.kdWork[i]--;
            if (work.pg.kdWork[i] == 0) {
                newf |= (0x10 << i) & 0xff;
            }
        }

        if (newf != work.pg.keyOnSlot) {
            // Key On!!
            KEYON2();
            work.pg.keyOnSlot = newf;
        }
    }

    private void KEYON2() {
        if (work.soundWork.getReady() == 0) return;

        int a = 0x04;
        if (work.soundWork.getFmPort() == 0) {
            a = 0x00;
        }

        if (!work.pg.keyOnDelayFlag) {
            a += work.pg.keyOnSlot;
        } else {
            work.pg.keyOnSlot = 0x00;
            if (work.pg.kdWork[0] == 0) work.pg.keyOnSlot += 0x10;
            if (work.pg.kdWork[1] == 0) work.pg.keyOnSlot += 0x20;
            if (work.pg.kdWork[2] == 0) work.pg.keyOnSlot += 0x40;
            if (work.pg.kdWork[3] == 0) work.pg.keyOnSlot += 0x80;
            a += work.pg.keyOnSlot;
        }

//KEYON2:
        a += work.pg.channelNumber;
        outPSG(0x28, a); // KEY-ON
        work.pg.useKeyOn = true;

        if (work.pg.reverbFlg) {
            STVOL();
        }
    }

    private void FMVolMode() {
        int b = work.pg.mData[work.hl++].dat;

        if (b == 0xff) {
            for (int i = 0; i < 4; i++) {
                work.pg.getTlDirectTable()[i == 0 ? 3 : (i == 1 ? 1 : (i == 2 ? 2 : 0))] = work.pg.mData[work.hl++].dat;
            }
            return;
        }

        work.cd.setFmVolMode(b);
        switch (work.cd.getFmVolMode()) {
        case 0:
            work.cd.setCurrentFMVolTable(SoundWork.FMVDAT);
            break;
        case 1:
            for (int i = 0; i < 20; i++) {
                work.cd.getFmVolUserTable()[19 - i] = work.pg.mData[work.hl++].dat;
            }
            work.cd.setCurrentFMVolTable(work.cd.getFmVolUserTable());
            break;
        case 2:
            break;
        case 3:
            break;
        }
    }

    private void OTOPSTG() {
        if (work.soundWork.getPcmFlg() != 0) return;
        if (work.soundWork.getDrmF1() != 0) return;

        //work.pg.instrumentNumber = work.pg.mData[work.hl++].dat;
        for (int i = 0; i < 2; i++) {
            work.pg.instrumentGradations[i] = work.pg.mData[work.hl++].dat;
        }
        int wait = work.pg.mData[work.hl++].dat;
        int rst = work.pg.mData[work.hl++].dat;

        // KUMA Changes the tone only when the current page is active or when sound effect mode is enabled.
        if (!checkCh3SpecialMode() && work.cd.getCurrentPageNo() != work.pg.getPageNo()) return;

        work.pg.instrumentGradationSwitch = true;
        work.pg.instrumentGradationWait = wait;
        instrumentGradationGetParamsFromVoice(/* ref */ work.pg.instrumentGradationSt ,work.pg.instrumentGradations[0]);
        instrumentGradationGetParamsFromVoice(/* ref */ work.pg.instrumentGradationEd ,work.pg.instrumentGradations[1]);
        work.pg.instrumentGradationReset = rst==1;

        instrumentGradationReset();
    }

    private static final int instrumentGradParam = 42;

    private void instrumentGradationReset() {
        work.pg.instrumentNumber = work.pg.instrumentGradations[0];
        work.pg.instrumentGradationWaitCounter = work.pg.instrumentGradationWait;
        work.pg.instrumentGradationPointer = 0;
        System.arraycopy(work.pg.instrumentGradationSt, 0, work.pg.instrumentGradationWk, 0, instrumentGradParam);

        STENV(); // Standard tone set
        STVOL();
    }

    private void prcInstrumentGradation() {
        if (!checkCh3SpecialMode() && work.pg.getPageNo() != work.cd.getCurrentPageNo()) return;

        if (!work.pg.instrumentGradationSwitch) return;

        work.pg.instrumentGradationWaitCounter--;
        if (work.pg.instrumentGradationWaitCounter != 0) return;
        work.pg.instrumentGradationWaitCounter = work.pg.instrumentGradationWait;

        instrumentGradationUpdate();

        //work.pg.instrumentNumber = work.pg.instrumentGradations[work.pg.instrumentGradationPointer];
        STENVGradation(); // Key off, no release cut tone set

        STVOL();
    }

    private void instrumentGradationUpdate() {
        for (int i = 0; i < instrumentGradParam; i++) {
            if (work.pg.instrumentGradationWk[i] == work.pg.instrumentGradationEd[i]) {
                work.pg.instrumentGradationFlg[i] = false;
                continue;
            }

            work.pg.instrumentGradationFlg[i] = true;
            if (work.pg.instrumentGradationWk[i] < work.pg.instrumentGradationEd[i])
                work.pg.instrumentGradationWk[i]++;
            else
                work.pg.instrumentGradationWk[i]--;
        }
    }

    private void instrumentGradationGetParamsFromVoice(/* ref */ int[] param, int v) {
        int hl = v * 25; // HL =* 25
        hl++; // Since the number of tones is stored, it is shifted

        param[0] = (work.fmVoiceAtMusData[hl + 0 * 4 + 0] & 0x70) >> 4; // op1 Det
        param[1] = work.fmVoiceAtMusData[hl + 0 * 4 + 0] & 0xf;         // op1 Mul
        param[2] = (work.fmVoiceAtMusData[hl + 0 * 4 + 1] & 0x70) >> 4; // op3 Det
        param[3] = work.fmVoiceAtMusData[hl + 0 * 4 + 1] & 0xf;         // op3 Mul
        param[4] = (work.fmVoiceAtMusData[hl + 0 * 4 + 2] & 0x70) >> 4; // op2 Det
        param[5] = work.fmVoiceAtMusData[hl + 0 * 4 + 2] & 0xf;         // op2 Mul
        param[6] = (work.fmVoiceAtMusData[hl + 0 * 4 + 3] & 0x70) >> 4; // op4 Det
        param[7] = work.fmVoiceAtMusData[hl + 0 * 4 + 3] & 0xf;         // op4 Mul

        param[8] = work.fmVoiceAtMusData[hl + 1 * 4 + 0] & 0x7f;  // op1 TL
        param[9] = work.fmVoiceAtMusData[hl + 1 * 4 + 1] & 0x7f;  // op3 TL
        param[10] = work.fmVoiceAtMusData[hl + 1 * 4 + 2] & 0x7f; // op2 TL
        param[11] = work.fmVoiceAtMusData[hl + 1 * 4 + 3] & 0x7f; // op4 TL

        param[12] = (work.fmVoiceAtMusData[hl + 2 * 4 + 0] & 0xc0) >> 6; // op1 KS
        param[13] = work.fmVoiceAtMusData[hl + 2 * 4 + 0] & 0x1f;        // op1 AR
        param[14] = (work.fmVoiceAtMusData[hl + 2 * 4 + 1] & 0xc0) >> 6; // op3 KS
        param[15] = work.fmVoiceAtMusData[hl + 2 * 4 + 1] & 0x1f;        // op3 AR
        param[16] = (work.fmVoiceAtMusData[hl + 2 * 4 + 2] & 0xc0) >> 6; // op2 KS
        param[17] = work.fmVoiceAtMusData[hl + 2 * 4 + 2] & 0x1f;        // op2 AR
        param[18] = (work.fmVoiceAtMusData[hl + 2 * 4 + 3] & 0xc0) >> 6; // op4 KS
        param[19] = work.fmVoiceAtMusData[hl + 2 * 4 + 3] & 0x1f;        // op4 AR

        param[20] = work.fmVoiceAtMusData[hl + 3 * 4 + 0] & 0x1f; // op1 DR
        param[21] = work.fmVoiceAtMusData[hl + 3 * 4 + 1] & 0x1f; // op3 DR
        param[22] = work.fmVoiceAtMusData[hl + 3 * 4 + 2] & 0x1f; // op2 DR
        param[23] = work.fmVoiceAtMusData[hl + 3 * 4 + 3] & 0x1f; // op4 DR

        param[24] = work.fmVoiceAtMusData[hl + 4 * 4 + 0] & 0x1f; // op1 SR
        param[25] = work.fmVoiceAtMusData[hl + 4 * 4 + 1] & 0x1f; // op3 SR
        param[26] = work.fmVoiceAtMusData[hl + 4 * 4 + 2] & 0x1f; // op2 SR
        param[27] = work.fmVoiceAtMusData[hl + 4 * 4 + 3] & 0x1f; // op4 SR

        param[28] = (work.fmVoiceAtMusData[hl + 5 * 4 + 0] & 0xf0) >> 4; // op1 SL
        param[29] = work.fmVoiceAtMusData[hl + 5 * 4 + 0] & 0x0f;        // op1 RR
        param[30] = (work.fmVoiceAtMusData[hl + 5 * 4 + 1] & 0xf0) >> 4; // op3 SL
        param[31] = work.fmVoiceAtMusData[hl + 5 * 4 + 1] & 0x0f;        // op3 RR
        param[32] = (work.fmVoiceAtMusData[hl + 5 * 4 + 2] & 0xf0) >> 4; // op2 SL
        param[33] = work.fmVoiceAtMusData[hl + 5 * 4 + 2] & 0x0f;        // op2 RR
        param[34] = (work.fmVoiceAtMusData[hl + 5 * 4 + 3] & 0xf0) >> 4; // op4 SL
        param[35] = work.fmVoiceAtMusData[hl + 5 * 4 + 3] & 0x0f;        // op4 RR

        param[36] = (work.fmVoiceAtMusData[hl + 4 * 4 + 0] & 0xc0) >> 6; // op1 DT2
        param[37] = (work.fmVoiceAtMusData[hl + 4 * 4 + 1] & 0xc0) >> 6; // op3 DT2
        param[38] = (work.fmVoiceAtMusData[hl + 4 * 4 + 2] & 0xc0) >> 6; // op2 DT2
        param[39] = (work.fmVoiceAtMusData[hl + 4 * 4 + 3] & 0xc0) >> 6; // op4 DT2

        param[40] = (work.fmVoiceAtMusData[hl + 24] & 0x38) >> 3; // FB
        param[41] = work.fmVoiceAtMusData[hl + 24] & 0x07;        // ALG
    }

    private static final byte[] GraSlot = { 1, 4, 2, 8 };

    private void STENVGradation() {
        int d = 0x30; // START=PORT 30H
        d += work.pg.channelNumber;// PLU S CHANNEL No.
        boolean CH3 = checkCh3SpecialMode();
        byte dd = 4;

        if (work.soundWork.getCurrentChip() == 4) {
            d = 0x40;
            CH3 = false;
            dd = 8;
        }

        if (work.pg.instrumentGradationFlg[40] || work.pg.instrumentGradationFlg[41]) {
            work.pg.feedback = work.pg.instrumentGradationWk[40];
            work.pg.algo = work.pg.instrumentGradationWk[41];

            if (dd != 8) { // OPN
                outPSG(0xb0 + work.pg.channelNumber, (work.pg.feedback << 3) | work.pg.algo);
            } else { // OPM
                int  a = ((work.pg.panValue & 1) << 1) | ((work.pg.panValue & 2) >> 1);
                int val = (a << 6) | (work.pg.feedback << 3) | work.pg.algo;
                outPSG(0x20 + work.pg.channelNumber, val);
            }
        }

        // 6 PARAMETER(Det/Mul, Total, KS/AR, DR, SR, SL/RR)

        // Det/Mul
        for (int o = 0; o < 4; o++) {
            if (CH3 && (work.pg.useSlot & GraSlot[o]) == 0) continue;
            if (work.pg.instrumentGradationFlg[o * 2] || work.pg.instrumentGradationFlg[o * 2 + 1])
                outPSG(d, (byte)((work.pg.instrumentGradationWk[o * 2] << 4) | work.pg.instrumentGradationWk[o * 2 + 1]));
            d += dd;
        }

        // TL
        int c = SoundWork.CRYDAT[work.pg.algo];
        for (int o = 0; o < 4; o++) {
            if (CH3 && (work.pg.useSlot & GraSlot[o]) == 0) continue;
            if (work.pg.instrumentGradationFlg[8 + o])//TL
            {
                if ((c & (1 << o)) == 0)
                    outPSG(d, (byte)work.pg.instrumentGradationWk[8 + o]);
                work.pg.vTl[o] = (byte)work.pg.instrumentGradationWk[8 + o];
            }
            d += dd;
        }

        // KS/AR
        for (int o = 0; o < 4; o++) {
            if (CH3 && (work.pg.useSlot & GraSlot[o]) == 0) continue;
            if (work.pg.instrumentGradationFlg[12 + o * 2] || work.pg.instrumentGradationFlg[12 + o * 2 + 1])
                outPSG(d, (byte)((work.pg.instrumentGradationWk[12 + o * 2] << 6) | work.pg.instrumentGradationWk[12 + o * 2 + 1]));
            d += dd;
        }

        // AM/DR
        for (int o = 0; o < 4; o++) {
            if (CH3 && (work.pg.useSlot & GraSlot[o]) == 0) continue;
            if (work.pg.instrumentGradationFlg[20 + o])
                outPSG(d, (byte)(0x80 | work.pg.instrumentGradationWk[20 + o]));
            d += dd;
        }

        // Dt2/SR
        for (int o = 0; o < 4; o++) {
            if (CH3 && (work.pg.useSlot & GraSlot[o]) == 0) continue;
            if (dd == 4) {
                // OPN
                if (work.pg.instrumentGradationFlg[24 + o])
                    outPSG(d, (byte)work.pg.instrumentGradationWk[24 + o]);
            } else {
                // OPM
                if (work.pg.instrumentGradationFlg[36 + o] || work.pg.instrumentGradationFlg[24 + o])
                    outPSG(d, (byte)((work.pg.instrumentGradationWk[36 + o] << 6) | work.pg.instrumentGradationWk[24 + o]));
            }
            d += dd;
        }

        // SL/RR
        for (int o = 0; o < 4; o++) {
            if (CH3 && (work.pg.useSlot & GraSlot[o]) == 0) continue;
            if (work.pg.instrumentGradationFlg[28 + o * 2] || work.pg.instrumentGradationFlg[28 + o * 2 + 1])
                outPSG(d, (byte)((work.pg.instrumentGradationWk[28 + o * 2] << 4) | work.pg.instrumentGradationWk[28 + o * 2 + 1]));
            d += dd;
        }
    }

    private int STENV2() {
        if (work.soundWork.getCurrentChip() == 4) {
            STENV2opm();
            return 0;
        }

        // Get the tone number from the work
//STENV0:
        int hl = work.pg.instrumentNumber * 25; // HL =* 25
        hl++; // Since the number of tones is stored, it is shifted

        // KUMA Save TL
        //if (work.isDotNET && work.header.CarrierCorrection)
        {
            work.pg.vTl[0] = work.fmVoiceAtMusData[hl + 4 + 0];
            work.pg.vTl[1] = work.fmVoiceAtMusData[hl + 4 + 1];
            work.pg.vTl[2] = work.fmVoiceAtMusData[hl + 4 + 2];
            work.pg.vTl[3] = work.fmVoiceAtMusData[hl + 4 + 3];
        }

//STENV1:
        int d = 0x30; // START=PORT 30H
        d += work.pg.channelNumber; // PLUS CHANNEL No.
//STENV2:
        byte c = 6; // 6 PARAMETER(Det/Mul, Total, KS/AR, DR, SR, SL/RR)
        do {
            if (checkCh3SpecialMode()) {
                if ((work.pg.useSlot & 1) != 0) outPSG(d, work.fmVoiceAtMusData[hl]);
                hl++;
                d += 4; // SKIP BLANK PORT
                if ((work.pg.useSlot & 4) != 0) outPSG(d, work.fmVoiceAtMusData[hl]);
                hl++;
                d += 4; // SKIP BLANK PORT
                if ((work.pg.useSlot & 2) != 0) outPSG(d, work.fmVoiceAtMusData[hl]);
                hl++;
                d += 4; // SKIP BLANK PORT
                if ((work.pg.useSlot & 8) != 0) outPSG(d, work.fmVoiceAtMusData[hl]);
                hl++;
                d += 4; // SKIP BLANK PORT
            } else {
                byte b = 4; // 4 OPERATOR
//STENV3:
                do {
                    // GET DATA
                    //outPSG(d, work.mData[hl++].dat);
                    outPSG(d, work.fmVoiceAtMusData[hl++]);
                    d += 4;// SKIP BLANK PORT
                    b--;
                } while (b != 0);
            }

            c--;

        } while (c != 0);

        //e = work.mData[hl].dat; // GET FEEDBACK/ALGORITHM
        byte e = work.fmVoiceAtMusData[hl]; // GET FEEDBACK/ALGORITHM
        // GET ALGORITHM
        work.pg.algo = e & 0x07; // STORE ALGORITHM
        // GET ALGO SET ADDRESS
        d = 0xb0 + work.pg.channelNumber; // CH PLUS
        outPSG(d, e);

        return hl;
    }

    private void STENV2opm() {
        // Get the tone number from the work
//STENV0:
        int hl = work.pg.instrumentNumber * 25;// HL =* 25
        hl++; // Since the number of tones is stored, it is shifted

        // KUMA Save tl
        //if (work.header.CarrierCorrection) {
            work.pg.vTl[0] = work.fmVoiceAtMusData[hl + 4 + 0];
            work.pg.vTl[1] = work.fmVoiceAtMusData[hl + 4 + 1];
            work.pg.vTl[2] = work.fmVoiceAtMusData[hl + 4 + 2];
            work.pg.vTl[3] = work.fmVoiceAtMusData[hl + 4 + 3];
        //}

//STENV1:
        int d = 0x40; // START = Adr:40H
        d += work.pg.channelNumber; // PLUS CHANNEL No.
//STENV2:
        byte c = 6; // 6 PARAMETER(Det/Mul, Total, KS/AR, DR, SR, SL/RR)
        do {
            byte b = 4;// 4 OPERATOR
            do {
                // GET DATA
                outPSG(d, work.fmVoiceAtMusData[hl++]);
                d += 8; // SKIP BLANK PORT
                b--;
            } while (b != 0);
            c--;
        } while (c != 0);

        int e = work.fmVoiceAtMusData[hl] & 0xff; // GET FEEDBACK/ALGORITHM
        int a = (((work.pg.panValue & 1) << 1) | ((work.pg.panValue & 2) >> 1)); // bit order swapping
        e |= a << 6; // pan

        // GET ALGORITHM
        work.pg.algo = e & 0x07; // STORE ALGORITHM
        work.pg.feedback = (e & 0x38) >> 3;
        // GET ALGO SET ADDRESS
        d = 0x20 + work.pg.channelNumber; // CH PLUS
        outPSG(d, e);
    }
}
