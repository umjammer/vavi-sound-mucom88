package mucom88.driver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import dotnet4j.Tuple6;
import mdsound.Log;
import mdsound.LogLevel;
import mucom88.common.Common;
import musicDriverInterface.ChipDatum;
import musicDriverInterface.LinePos;
import musicDriverInterface.MmlDatum;
import musicDriverInterface.enmMMLType;


public class Music2 {

    public final int MAXCH = 11;
    // **FM CONTROL COMMAND(s)   **
    public Runnable[] FMCOM = null;
    public Runnable[] FMCOM2 = null;
    public Runnable[] LFOTBL = null;
    // **   PSG COMMAND TABLE**
    public Runnable[] PSGCOM = null;
    public Runnable[] PSGCOM2 = null;//kuma:DotNET専用テーブル

    private Work work;
    private Consumer<ChipDatum> WriteOPNAPRegister;
    private Consumer<ChipDatum> WriteOPNASRegister;
    private Consumer<ChipDatum> WriteOPNBPRegister;
    private Consumer<ChipDatum> WriteOPNBSRegister;
    private Consumer<ChipDatum> WriteOPMPRegister;

    private byte[] autoPantable = new byte[] {2, 3, 1, 3};


    public Music2(Work work
            , Consumer<ChipDatum> WriteOPNAPRegister, Consumer<ChipDatum> WriteOPNASRegister
            , Consumer<ChipDatum> WriteOPNBPRegister, Consumer<ChipDatum> WriteOPNBSRegister
            , Consumer<ChipDatum> WriteOPMPRegister
    ) {
        this.work = work;
        this.WriteOPNAPRegister = WriteOPNAPRegister;
        this.WriteOPNASRegister = WriteOPNASRegister;
        this.WriteOPNBPRegister = WriteOPNBPRegister;
        this.WriteOPNBSRegister = WriteOPNBSRegister;
        this.WriteOPMPRegister = WriteOPMPRegister;
        initMusic2();
        initAryDRIVE();
    }

    boolean notSoundBoard2;

    public void MSTART(int musicNumber) {
        synchronized (work.SystemInterrupt) {

            work.soundWork.setMUSNUM(musicNumber);
            AKYOFF();
            SSGOFF();
            WORKINIT();

            CHK();//added
            INT57();
            ENBL();

            for (int c = 0; c < 5; c++) {
                work.soundWork.setcurrentChip(c);
                work.currentTimer = c;
                TO_NML();
            }
            work.soundWork.setcurrentChip(0);

            work.resetPlaySync = true;
            work.Status = 1;
            work.currentTimer = 0;

            //一度だけタイマ割り込みをさせる(初めの発音が不安定になる現象対策)
            while ((work.timerOPNA1.StatReg & 3) == 0) {
                synchronized (work.SystemInterrupt) {
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
        synchronized (work.SystemInterrupt) {

            AKYOFF();
            SSGOFF();

            if (work.Status > 0) work.Status = 0;

        }
    }

    public void FDO() {
        synchronized (work.SystemInterrupt) {


        }
    }

    public Object RETW() {
        synchronized (work.SystemInterrupt) {


        }
        return null;
    }

    public void EFC() {
        synchronized (work.SystemInterrupt) {
            //Work.SystemInterrupt = true;
            //Work.SystemInterrupt = false;
        }
    }

    public void Rendering() {
        if (work.Status == 0) return;

        synchronized (work.SystemInterrupt) {
            //Work.SystemInterrupt = true;

            if (work.resetPlaySync) {
                work.resetPlaySync = false;
                ChipDatum dat = new ChipDatum(-1, -1, -1, 0, new MmlDatum(enmMMLType.ResetPlaySync, null, null, 0));
                WriteRegister(0, dat);
            }

            work.timerOPNA1.timer();
            work.timerOPNA2.timer();
            work.timerOPNB1.timer();
            work.timerOPNB2.timer();
            work.timerOPM.timer();

            //Console.WriteLine("CurrentTimer:{0}", Work.currentTimer);

            work.timeCounter++;
            boolean flg = false;
            switch (work.currentTimer) {
            case 0:
                flg = (work.timerOPNA1.StatReg & 3) != 0;
                break;
            case 1:
                flg = (work.timerOPNA2.StatReg & 3) != 0;
                break;
            case 2:
                flg = (work.timerOPNB1.StatReg & 3) != 0;
                break;
            case 3:
                flg = (work.timerOPNB2.StatReg & 3) != 0;
                break;
            case 4:
                flg = (work.timerOPM.StatReg & 3) != 0;
                break;
            }
            if (flg) {
                PL_SND();
            }

            //Work.SystemInterrupt = false;
        }
    }

    public void SkipCount(int count) {
        synchronized (work.SystemInterrupt) {
            //Work.SystemInterrupt = true;
            for (int c = 0; c < 4; c++) {
                for (int i = 0; i < work.soundWork.chData.get(c).size(); i++) {
                    for (int j = 0; j < work.soundWork.chData.get(c).get(i).PGDAT.size(); j++)
                        work.soundWork.chData.get(c).get(i).PGDAT.get(j).muteFlg = true;
                }
            }

            while (count > 0) {
                PL_SND();
                count--;
            }

            for (int c = 0; c < 4; c++) {
                for (int i = 0; i < work.soundWork.chData.get(c).size(); i++)
                    for (int j = 0; j < work.soundWork.chData.get(c).get(i).PGDAT.size(); j++)
                        work.soundWork.chData.get(c).get(i).PGDAT.get(j).muteFlg = false;
            }
        }
    }

    public void SetMuteFlg(int chip, int ch, int page, boolean flg) {
        if (chip < 0 || chip >= work.soundWork.chData.size()) return;
        if (ch < 0 || ch >= work.soundWork.chData.get(chip).size()) return;
        if (page < 0 || page >= work.soundWork.chData.get(chip).get(ch).PGDAT.size()) return;
        work.soundWork.chData.get(chip).get(ch).PGDAT.get(page).silentFlg = flg;
    }

    public void SetAllMuteFlg(boolean flg) {
        for (int c = 0; c < 5; c++) {
            for (int i = 0; i < work.soundWork.chData.get(c).size(); i++) {
                if (work.soundWork.chData.get(c).get(i) == null) continue;
                for (int j = 0; j < work.soundWork.chData.get(c).get(i).PGDAT.size(); j++)
                    work.soundWork.chData.get(c).get(i).PGDAT.get(j).silentFlg = flg;
            }
        }
    }

    public void initMusic2() {
        SetFMCOMTable();
        SetLFOTBL();
        SetPSGCOM();
        SetSoundWork();

    }

    public void SetFMCOMTable() {
        FMCOM = new Runnable[] {
                this::OTOPST        // 0xF0 - ｵﾝｼｮｸ ｾｯﾄ    '@'
                , this::VOLPST       // 0xF1 - VOLUME SET   'v'
                , this::FRQ_DF       // 0xF2 - DETUNE(ｼｭｳﾊｽｳ ｽﾞﾗｼ) 'D'
                , this::SETQ         // 0xF3 - SET COMMAND 'q'
                , this::LFOON        // 0xF4 - LFO SET
                , this::REPSTF       // 0xF5 - REPEAT START SET  '['
                , this::REPENF       // 0xF6 - REPEAT END SET    ']'
                , this::MDSET        // 0xF7 - FMｵﾝｹﾞﾝ ﾓｰﾄﾞｾｯﾄ  KUMA:'S'スロットディチューンコマンド
                // ,STEREO    // 0xF8 - STEREO MODE
                , this::STEREO_AMD98 // 0xF8 - STEREO MODE  'p'
                , this::FLGSET       // 0xF9 - FLAG SET
                , this::W_REG        // 0xFA - COMMAND OF   'y'
                , this::VOLUPF       // 0xFB - VOLUME UP    ')'
                , this::HLFOON       // 0xFC - HARD LFO
                , this::TIE          // (CANT USE)
                , this::RSKIP        // 0xFE - REPEAT JUMP'/'
                , this::SECPRC       // 0xFF - to second com
        };

        FMCOM2 = new Runnable[] {
                this::PVMCHG         // 0xFF 0xF0 - PCM VOLUME MODE
                , this::HRDENV           // 0xFF 0xF1 - HARD ENVE SET 's'  -> 'S'(kuma)
                , this::ENVPOD        // 0xFF 0xF2 - HARD ENVE PERIOD 'm'
                , this::REVERVE       // 0xFF 0xF3 - ﾘﾊﾞｰﾌﾞ
                , this::REVMOD           // 0xFF 0xF4 - ﾘﾊﾞｰﾌﾞﾓｰﾄﾞ
                , this::REVSW           // 0xFF 0xF5 - ﾘﾊﾞｰﾌﾞ ｽｲｯﾁ
                , this::SetKeyOnDelay // 0xFF 0xF6 - キーオンディレイ 'KD' n1,n2,n3,n4
                , this::MW_REG        // 0xFF 0xF7 - multi Write Register n1,n2,n3,n4
                , this::CH3SP         // 0xFF 0xF8 - 効果音モード系制御コマンド
                , this::PORTAON       // 0xFF 0xF9 - ポルタメント n1,n2,n3  (st ed totalclock)
                , this::ENVPSTex      // 0xFF 0xFA - ソフトエンベロープ 'E' n1,n2,n3,n4,n5,n6
                , this::FMVolMode     // 0xFF 0xFB - FMボリュームモード切替
                , this::NTMEAN        // 0xFF 0xFC
                , this::NTMEAN        // 0xFF 0xFD
                , this::NTMEAN        // 0xFF 0xFE
                , this::NOP           // 0xFF 0xFF
        };
    }

    private void NOP() {
        DummyOUT();
    }

    public void SetLFOTBL() {
        LFOTBL = new Runnable[] {
                this::LFOOFF
                , this::LFOON2
                , this::SETDEL
                , this::SETCO
                , this::SETVC2
                , this::SETPEK
                , this::TLLFOorSSGTremolo
        };
    }

    public void SetPSGCOM() {
        PSGCOM = new Runnable[] {
                this::OTOSSG// 0xF0 - ｵﾝｼｮｸ ｾｯﾄ         '@'
                , this::PSGVOL// 0xF1 - VOLUME SET
                , this::FRQ_DF// 0xF2 - DETUNE
                , this::SETQ  // 0xF3 - COMMAND OF        'q'
                , this::LFOON // 0xF4 - LFO
                , this::REPSTF// 0xF5 - REPEAT START SET  '['
                , this::REPENF// 0xF6 - REPEAT END SET    ']'
                , this::NOISE // 0xF7 - MIX PORT          'P'
                , this::NOISEW// 0xF8 - NOIZE PARAMATER   'w'
                , this::FLGSET// 0xF9 - FLAG SET
                , this::ENVPST// 0xFA - SOFT ENVELOPE     'E'
                , this::VOLUPS// 0xFB - VOLUME UP    ')'
                , this::OTOSET// 0xFC - ｵﾝｼｮｸﾃｲｷﾞ   '@='
                , this::TIE   // 0x
                , this::RSKIP // 0x
                , this::SECPRC// 0xFF - to sec com
        };

        PSGCOM2 = new Runnable[] {
                this::STEREO_AMD98    // 0xFF 0xF0 - 'p' パン
                , this::HRDENV            // 0xFF 0xF1 - HARD ENVE SET 's'  -> 'S'(kuma)
                , this::ENVPOD         // 0xFF 0xF2 - HARD ENVE PERIOD 'm'
                , this::REVERVE        // 0xFF 0xF3 - ﾘﾊﾞｰﾌﾞ
                , this::REVMOD            // 0xFF 0xF4 - ﾘﾊﾞｰﾌﾞﾓｰﾄﾞ
                , this::REVSW            // 0xFF 0xF5 - ﾘﾊﾞｰﾌﾞ ｽｲｯﾁ
                , this::SelectWaveForm // 0xFF 0xF6
                , this::MW_REG         // 0xFF 0xF7 - multi Write Register n1,n2,n3,n4
                , this::CH3SP          // 0xFF 0xF8 - 効果音モード系制御コマンド
                , this::PORTAON        // 0xFF 0xF9 - ポルタメント n1,n2,n3  (st ed totalclock)
                , this::ENVPSTex       // 0xFF 0xFA - ソフトエンベロープ 'E' n1,n2,n3,n4,n5,n6
                , this::NOP            // 0xFF 0xFB
                , this::NTMEAN         // 0xFF 0xFC
                , this::NTMEAN         // 0xFF 0xFD
                , this::NTMEAN         // 0xFF 0xFE
                , this::NOP            // 0xFF 0xFF
        };

    }

    private void SelectWaveForm() {
        byte a = (byte) work.pg.mData[work.hl++].dat;
        if (a != 0xff) {
            //波形プリセット選択
            work.pg.setSSGWfNum(a);
        } else {
            //ユーザー波形選択
            work.pg.setSSGWfNum(10 + (work.pg.channelNumber >> 1)); // >>1の理由は、SSGのchannelNumberは0,2,4となっている為
            a = (byte) work.pg.mData[work.hl++].dat;
        }

        //SSG拡張モードでは無いときは送信しない
        if (!work.SSGExtend) return;

        //dutycycle更新

        //WaveForm送信
        if (work.pg.getSSGWfNum() > 9) {
            SendSSGWf(a);
        }
    }

    private void SendSSGWf(byte wfNum) {
        if (work.ssgVoiceAtMusData == null) return;
        if (!work.ssgVoiceAtMusData.containsKey(wfNum)) return;
        byte[] dat = work.ssgVoiceAtMusData.get(wfNum);
        byte vch = (byte) (work.pg.channelNumber >> 1);
        PSGOUT((byte) 0x0d, (byte) (0x80 | ((vch & 3) << 4) | (work.pg.hardEnvelopValue & 0xf)));
        for (int n = 0; n < dat.length; n++)
            PSGOUT((byte) 0x0e, (byte) (dat[n] + 0x80));
    }

    public void SetSoundWork() {
        work.Init();
    }

    /**
     * AllKeYOFF(fm only)
     */
    private void AKYOFF() {
        for (int i = 0; i < 5; i++) {
            for (int e = 0; e < (i != 4 ? 7 : 8); e++) {
                ChipDatum dat = new ChipDatum(0, (i != 4 ? 0x28 : 0x08), (byte) e);
                WriteRegister(i, dat);
            }
        }
    }

    // **SSG ALL SOUND OFF**

    private void SSGOFF() {
        ChipDatum dat;
        for (int i = 0; i < 4; i++) {
            for (int b = 0; b < 3; b++) {
                dat = new ChipDatum(0, (byte) (0x8 + b), 0x0);
                WriteRegister(i, dat);
            }
            //dat = new ChipDatum(0, (byte)0x7, 0x0);
            //WriteRegister(i, dat);
        }
    }

    /** VOLUME OR FADEOUT etc RESET */
    public void WORKINIT() {
        work.soundWork.setC2NUM(0);
        work.soundWork.setCHNUM(0);
        work.soundWork.setPVMODE(0);

        work.soundWork.setKEY_FLAG(0);
        work.soundWork.setRANDUM((short) System.currentTimeMillis());

        if (work.getHeader().mupb != null) {
            WORKINITExtendFormat();
            return;
        }

        int num = work.soundWork.getMUSNUM();
        work.mDataAdr = work.soundWork.getMU_TOP();


        for (int i = 0; i < num; i++) {
            work.mDataAdr += 1 + MAXCH * 4;
            work.mDataAdr = work.soundWork.getMU_TOP() + Common.getLE16(work.mData, (int) work.mDataAdr);
        }

        work.soundWork.setTIMER_B((work.mData[work.mDataAdr] != null) ? ((byte) work.mData[work.mDataAdr].dat) : (byte) 200);
        work.soundWork.setTB_TOP(++work.mDataAdr);

        int ch = 0;// (CH1DATのこと)
        for (ch = 0; ch < 6; ch++) {
            FMINIT(0, ch);
            //ch++;//オリジナルは　ix+=WKLENG だが、配列化しているので。
        }

        work.soundWork.setCHNUM(0);
        ch = 6;//DRAMDAT
        FMINIT(0, ch);

        work.soundWork.setCHNUM(0);
        //ix = 7;//CHADAT
        for (ch = 7; ch < 7 + 4; ch++) {
            FMINIT(0, ch);
            //オリジナルは　ix+=WKLENG だが、配列化しているので。
        }

        work.fmVoiceAtMusData = GetVoiceDataAtMusData();

        work.mData = null;
    }

    public void WORKINITExtendFormat() {
        work.mDataAdr = 0;
        work.soundWork.setTIMER_B((byte) 200);
        work.soundWork.setTB_TOP(0);

        int ch;// (CH1DATのこと)
        for (int c = 0; c < 5; c++) {
            work.soundWork.setCHNUM(0);
            for (ch = 0; ch < (c != 4 ? 6 : 8); ch++) FMINITex(c, ch);
        }

        for (int c = 0; c < 4; c++) {
            work.soundWork.setCHNUM(0);
            ch = 6;//DRAMDAT
            FMINITex(c, ch);
        }

        for (int c = 0; c < 4; c++) {
            work.soundWork.setCHNUM(0);
            for (ch = 7; ch < 7 + 4; ch++) FMINITex(c, ch);
        }

        if (work.getHeader().mupb.getinstruments() != null && work.getHeader().mupb.getinstruments().length > 0 && work.getHeader().mupb.getinstruments()[0].getdata() != null) {
            work.fmVoiceAtMusData = work.getHeader().mupb.getinstruments()[0].getdata();

            //SSG波形データの読み込み
            work.ssgVoiceAtMusData = null;
            if (work.getHeader().mupb.getinstruments().length == 2) {
                work.ssgVoiceAtMusData = new HashMap<>();
                byte[] buf = work.getHeader().mupb.getinstruments()[1].getdata();
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

    private byte[] GetVoiceDataAtMusData() {
        int otodat = work.mData[1].dat + work.mData[2].dat * 0x100 + work.weight;
        int voiCnt = work.mData[otodat].dat;
        List<Byte> buf = new ArrayList<>();
        buf.add((byte) work.mData[otodat++].dat);
        for (int i = 0; i < voiCnt * 25; i++) {
            buf.add((byte) work.mData[otodat + i].dat);
        }
        return mdsound.Common.toByteArray(buf);
    }

    private void FMINIT(int chipIndex, int ch) {
        work.soundWork.chData.get(chipIndex).set(ch, new SoundWork.CHDAT());
        work.soundWork.chData.get(chipIndex).get(ch).PGDAT = new ArrayList<>();
        work.soundWork.chData.get(chipIndex).get(ch).PGDAT.add(new SoundWork.CHDAT.PGDAT());
        work.soundWork.chData.get(chipIndex).get(ch).PGDAT.get(0).lengthCounter = 1;
        work.soundWork.chData.get(chipIndex).get(ch).PGDAT.get(0).volume = 0;
        work.soundWork.chData.get(chipIndex).get(ch).PGDAT.get(0).setmusicEnd(false);
        work.soundWork.chData.get(chipIndex).get(ch).setFMVolMode((byte) 0);
        work.soundWork.chData.get(chipIndex).get(ch).setcurrentFMVolTable(work.soundWork.FMVDAT);

        // ---POINTER ﾉ ｻｲｾｯﾃｲ---
        int stPtr = Common.getLE16(work.mData, work.soundWork.getTB_TOP());
        int lpPtr = (int) Common.getLE16(work.mData, work.soundWork.getTB_TOP() + 2);
        if (lpPtr == 0) lpPtr = -1;

        //次のチャンネル
        int nCPtr = (int) Common.getLE16(work.mData, work.soundWork.getTB_TOP() + 4);

        List<MmlDatum> bf = new ArrayList<>();
        int length = (int) (nCPtr - stPtr + (nCPtr < stPtr ? 0x10000 : 0));
        for (int i = 0; i < length; i++) {
            bf.add(work.mData[work.soundWork.getMU_TOP() + work.weight + stPtr + i]);
        }
        work.soundWork.chData.get(chipIndex).get(ch).PGDAT.get(0).mData = bf.toArray(new MmlDatum[0]);

        if (nCPtr < stPtr) {
            work.weight += 0x1_0000;
        }

        work.soundWork.chData.get(chipIndex).get(ch).PGDAT.get(0).dataAddressWork
                = 0; //ix 2,3
        work.soundWork.chData.get(chipIndex).get(ch).PGDAT.get(0).dataTopAddress
                = lpPtr != -1 ? (lpPtr - stPtr) : -1; // ix 4,5


        work.soundWork.incC2NUM();
        work.soundWork.addTB_TOP(4);
        if (work.soundWork.getCHNUM() > 2) {
            // ---   FOR SSG   ---
            work.soundWork.chData.get(chipIndex).get(ch).PGDAT.get(0).volReg = work.soundWork.getCHNUM() + 5;//ix 7
            work.soundWork.chData.get(chipIndex).get(ch).PGDAT.get(0).channelNumber = (work.soundWork.getCHNUM() - 3) * 2;//ix 8
            work.soundWork.incCHNUM();
            return;
        }
        work.soundWork.chData.get(chipIndex).get(ch).PGDAT.get(0).channelNumber = work.soundWork.getCHNUM();//ix 8
        work.soundWork.incCHNUM();
    }

    private void FMINITex(int chipIndex, int ch) {
        work.soundWork.chData.get(chipIndex).set(ch, new SoundWork.CHDAT());
        work.soundWork.chData.get(chipIndex).get(ch).PGDAT = new ArrayList<SoundWork.CHDAT.PGDAT>();
        work.soundWork.chData.get(chipIndex).get(ch).setkeyOnCh(-1); // KUMA:初期化不要だが念のため
        work.soundWork.chData.get(chipIndex).get(ch).setcurrentPageNo(0); // KUMA:初期カレントは0ページ
        work.soundWork.chData.get(chipIndex).get(ch).setFMVolMode((byte) 0);
        work.soundWork.chData.get(chipIndex).get(ch).setcurrentFMVolTable(SoundWork.FMVDAT);

        MupbInfo.ChipDefine.ChipPart partInfo = work.getHeader().mupb.getchips()[chipIndex].getparts()[ch];

        for (int i = 0; i < partInfo.getpages().length; i++) {
            MupbInfo.PageDefine pageInfo = partInfo.getpages()[i];

            SoundWork.CHDAT.PGDAT pg = new SoundWork.CHDAT.PGDAT();
            pg.setpageNo(i);
            work.soundWork.chData.get(chipIndex).get(ch).PGDAT.add(pg);
            pg.lengthCounter = 1;
            pg.volume = 0;
            pg.keyoffflg = true;
            pg.setmusicEnd(false);
            pg.dataAddressWork = 0;//ix 2,3
            pg.dataTopAddress = pageInfo.getloopPoint();
            pg.mData = pageInfo.getdata();
            pg.channelNumber = work.soundWork.getCHNUM();//ix 8
            pg.setTLDirectTable(new byte[] {
                    (byte) 255, (byte) 255, (byte) 255, (byte) 255
            });

            if (chipIndex != 4) {
                if (work.soundWork.getCHNUM() > 2) {
                    // ---   FOR SSG   ---
                    pg.volReg = work.soundWork.getCHNUM() + 5;//ix 7
                    pg.channelNumber = (work.soundWork.getCHNUM() - 3) * 2;//ix 8
                }
                //リズムＣｈの場合は音色番号を0x3fにセットして全てのドラム音を有効にする
                if (ch == 6) {
                    pg.instrumentNumber = 0x3f;
                }
            }

            pg.panMode = 3;
        }

        work.soundWork.incC2NUM();
        work.soundWork.addTB_TOP(4);
        work.soundWork.incCHNUM();
    }

    /**
     * サウンドボード2のチェックと割り込みベクタ、ポートの設定
     * (割り込みベクタ、ポートの設定は不要)
     */
    private void CHK() {
        work.soundWork.setNOTSB2(notSoundBoard2 ? 1 : 0);
    }

    // **ﾜﾘｺﾐ ﾉ ﾚﾍﾞﾙ ｿﾉﾀ ｼｮｷｾｯﾃｲ ｦ ｵｺﾅｳ**

    private void INT57() {

        //割り込み系の設定は不要

        for (int c = 0; c < 5; c++) {
            work.soundWork.setcurrentChip(c);
            TO_NML();
        }
        work.soundWork.setcurrentChip(0);

        MONO();
        AKYOFF();// ALL KEY OFF
        SSGOFF();

        for (int c = 0; c < 5; c++) {
            ChipDatum dat;
            if (c != 4) {
                if (c < 2) {
                    dat = new ChipDatum(0, 0x29, 0x83);// CH 4-6 ENABLE
                    WriteRegister(c, dat);
                }

                for (int b = 0; b < 6; b++) {
                    dat = new ChipDatum(0, (byte) b, 0x00);// SSG registers($00～$05) 0 clear
                    WriteRegister(c, dat);
                }

                dat = new ChipDatum(0, 7, 0b0011_1000);//SSG tone mixer initialize
                WriteRegister(c, dat);

                if (c < 2) dat = new ChipDatum(0, 0x11, 63);//Rhythm volume initialize(max:63)
                else dat = new ChipDatum(1, 0x01, 63);//ADPCM-A volume initialize(max:63)
                WriteRegister(c, dat);

                dat = new ChipDatum(1, 0x06, 0xf0);
                WriteRegister(c, dat);
                dat = new ChipDatum(1, 0x07, 0x01);
                WriteRegister(c, dat);
            } else {
                //OPM どうしようかな
                dat = new ChipDatum(0, 1, 0x02);//LFO reset
                WriteRegister(c, dat);
            }

            if (c == 4) continue;

            // PSGﾊﾞｯﾌｧ ｲﾆｼｬﾗｲｽﾞ
            for (int i = 0; i < work.soundWork.INITPM.length; i++) {
                work.soundWork.PREGBF[c][i] = work.soundWork.INITPM[i];
            }
        }
    }

    //private void TO_NML()
    //{
    //    Work.soundWork.PLSET1_VAL = 0x38;
    //    Work.soundWork.PLSET2_VAL = 0x3a;

    //    OPNAData dat = new OPNAData(0, 0x27, 0x3a);
    //    WriteOPNARegister(dat);
    //}

    // **ALL MONORAL / H.LFO OFF***

    private void MONO() {
        ChipDatum dat;
        work.soundWork.setFMPORT(0);
        for (int b = 0; b < 3; b++) {
            dat = new ChipDatum(0, (byte) (0xb4 + b), 0xc0);//fm 1-3
            WriteRegister(work.soundWork.getcurrentChip(), dat);
        }

        for (int b = 0; b < 6; b++) {
            dat = new ChipDatum(0, (byte) (0x18 + b), 0xc0);//rhythm
            WriteRegister(work.soundWork.getcurrentChip(), dat);
        }

        work.soundWork.setFMPORT(4);
        for (int b = 0; b < 3; b++) {
            dat = new ChipDatum(1, (byte) (0xb4 + b), 0xc0);//fm 4-6
            WriteRegister(work.soundWork.getcurrentChip(), dat);
        }

        work.soundWork.setFMPORT(0);
        dat = new ChipDatum(0, 0x22, 0x00);//lfo freq control
        WriteRegister(work.soundWork.getcurrentChip(), dat);
        dat = new ChipDatum(0, 0x12, 0x00);//rhythm test data
        WriteRegister(work.soundWork.getcurrentChip(), dat);


        for (int b = 0; b < 7; b++) {
            for (int c = 0; c < 10; c++)
                work.soundWork.PALDAT[b * 10 + c] = (byte) 0xc0;
        }

        for (int c = 0; c < 4; c++)
            work.soundWork.getPCMLR()[c] = 3;
    }

    private void WriteRegister(int id, ChipDatum dat) {
        switch (id) {
        case 0:
            WriteOPNAPRegister.accept(dat);
            break;
        case 1:
            WriteOPNASRegister.accept(dat);
            break;
        case 2:
            WriteOPNBPRegister.accept(dat);
            break;
        case 3:
            WriteOPNBSRegister.accept(dat);
            break;
        case 4:
            WriteOPMPRegister.accept(dat);
            break;
        }
    }

    // **ﾐｭｰｼﾞｯｸ ﾜﾘｺﾐ ENABLE**

    public void ENBL() {
        STTMB(work.soundWork.getTIMER_B());// SET Timer-B

        //割り込みベクタリセット不要
        //Z80.A = M_VECTR;
        //Z80.C = Z80.A;
        //Z80.A = PC88.IN(Z80.C);
        //Z80.A &= 0x7F;
        //PC88.OUT(Z80.C, Z80.A);
    }

    // **Timer-B ｶｳﾝﾀ･ｾｯﾄ ﾙｰﾁﾝ   **
    // IN: E<= TIMER_B COUNTER

    private void STTMB(byte e) {
        ChipDatum dat;

        for (int c = 0; c < 4; c++) {
            dat = new ChipDatum(0, 0x26, e);
            WriteRegister(c, dat);

            dat = new ChipDatum(0, 0x27, 0x78);
            WriteRegister(c, dat);

            dat = new ChipDatum(0, 0x27, 0x7a);
            WriteRegister(c, dat);
        }

        dat = new ChipDatum(0, 0x12, e);
        WriteRegister(4, dat);

        dat = new ChipDatum(0, 0x14, 0x78);
        WriteRegister(4, dat);

        dat = new ChipDatum(0, 0x14, 0x7a);
        WriteRegister(4, dat);

        //割り込みレベルリセット不要
        //Z80.A = 5;
        //PC88.OUT(0xe4, Z80.A);
    }


    // **MUSIC MAIN**

    public void PL_SND() {
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
                for (int j = 0; j < work.soundWork.chData.get(c).get(i).PGDAT.size(); j++) {
                    if (work.soundWork.chData.get(c).get(i).PGDAT.get(j).getmusicEnd()) p++;
                }
                if (p == work.soundWork.chData.get(c).get(i).PGDAT.size()) n++;
            }
        }
        if (n == 11 * 4 + 8)
            work.Status = 0;
    }

    private void updateTimer() {
        ChipDatum dat;
        if (work.currentTimer != 4) {
            dat = new ChipDatum(0, 0x27, work.soundWork.PLSET1_VAL[work.currentTimer]); // TIMER-OFF DATA
            WriteRegister(work.currentTimer, dat);
            dat = new ChipDatum(0, 0x27, work.soundWork.PLSET2_VAL[work.currentTimer]); // TIMER-ON DATA
            WriteRegister(work.currentTimer, dat);
        } else {
            dat = new ChipDatum(0, 0x14, work.soundWork.PLSET1_VAL[work.currentTimer]); // TIMER-OFF DATA
            WriteRegister(work.currentTimer, dat);
            dat = new ChipDatum(0, 0x14, work.soundWork.PLSET2_VAL[work.currentTimer]); // TIMER-ON DATA
            WriteRegister(work.currentTimer, dat);
        }
    }

    // **CALL FM**

    private Tuple6<String, Integer, Integer, Integer, Integer, Runnable>[] aryDRIVE;
    private Tuple6<String, Integer, Integer, Integer, Integer, Runnable>[] aryMDRIVE;

    private void initAryDRIVE() {
        aryDRIVE = Arrays.<Tuple6<String, Integer, Integer, Integer, Integer, Runnable>>asList(
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
        ).toArray(new Tuple6[0]);
        aryMDRIVE = Arrays.<Tuple6<String, Integer, Integer, Integer, Integer, Runnable>>asList(
                new Tuple6<>("----- FM 1  ", 0, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 2  ", 0, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 3  ", 0, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 4  ", 0, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 5  ", 0, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 6  ", 0, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 7  ", 0, 0x00, 0, 0x00, this::FMENT),
                new Tuple6<>("----- FM 8  ", 0, 0x00, 0, 0x00, this::FMENT)
        ).toArray(new Tuple6[0]);
    }

    public void DRIVE() {
        int n = 0;

        for (int c = 0; c < 5; c++) {
            work.soundWork.setcurrentChip(c);
            int nowLoopCounter = Integer.MAX_VALUE;
            Tuple6<String, Integer, Integer, Integer, Integer, Runnable>[] aryDrv = (c != 4 ? aryDRIVE : aryMDRIVE);

            for (int i = 0; i < aryDrv.length; i++) {
                //Log.WriteLine(LogLevel.TRACE, aryDRIVE[i].getItem1());

                //KUMA:フラグ系パラメータのセット
                work.soundWork.setFMPORT(aryDrv[i].getItem2());
                work.soundWork.setSSGF1(aryDrv[i].getItem3());
                work.soundWork.setDRMF1(aryDrv[i].getItem4());
                work.soundWork.setPCMFLG(aryDrv[i].getItem5());

                work.cd = work.soundWork.chData.get(c).get(i); // KUMA:カレントのパートワーク切り替え
                work.cd.setkeyOnCh(-1); // KUMA:発音ページ情報をリセット
                if (c != 4) {
                    work.rhythmOR[c] = 0;
                    work.rhythmORKeyOff[c] = 0;
                }

                int m = 0;
                for (int j = 0; j < work.cd.PGDAT.size(); j++) {
                    work.soundWork.setcurrentChip(i * 10 + j);
                    work.pg = work.cd.PGDAT.get(j); // KUMA:カレントのページワーク切り替え
                    if (!work.pg.getmusicEnd()) {
                        if (work.pg.muteFlg || work.pg.silentFlg)
                            work.soundWork.setREADY((byte) 0x00); //KUMA: 0x08(bit3)=MUTE FLAG 又は外部からmuteの指定がある場合

                        aryDrv[i].getItem6().run(); // KUMA:パートごとの処理をコール

                        if (work.pg.muteFlg || work.pg.silentFlg)
                            work.soundWork.setREADY(0xff); // KUMA: 0x08(bit3)=MUTE FLAG
                    } else {
                        if (work.isDotNET)
                            AddEffect();
                    }
                    //KUMA:終了パートのカウント
                    if ((work.pg.dataTopAddress == -1 && work.pg.loopEndFlg)
                            || work.pg.getloopCounter() >= work.maxLoopCount) m++;

                    //
                    if ((work.pg.dataTopAddress != -1 || !work.pg.getmusicEnd())
                            //&& Work.pg.loopEndFlg
                            && work.pg.getloopCounter() < nowLoopCounter) {
                        nowLoopCounter = work.pg.getloopCounter();
                    }
                }
                if (m == work.cd.PGDAT.size())
                    n++;

                //リズム音源のキーオンオフ制御
                if (c < 2) {
                    if (work.rhythmORKeyOff[c] != 0)
                        PSGOUT((byte) 0x10, (byte) ((work.rhythmORKeyOff[c] & 0b0011_1111) | 0x80));// KEY OFF
                    if (work.rhythmOR[c] != 0)
                        PSGOUT((byte) 0x10, (byte) (work.rhythmOR[c] & 0b0011_1111));// KEY ON
                } else if (c < 4) {
                    if (work.rhythmORKeyOff[c] != 0)
                        PCMOUT((byte) 1, (byte) 0x0, (byte) ((work.rhythmORKeyOff[c] & 0b0011_1111) | 0x80));// KEY OFF
                    if (work.rhythmOR[c] != 0)
                        PCMOUT((byte) 1, (byte) 0x0, (byte) (work.rhythmOR[c] & 0b0011_1111));// KEY ON
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


    public void FMENT() {
        PANNING(); // AMD98
        KeyOnDelaying();

        FMSUB();
        if (work.isDotNET) {
            AddEffect();
            return;
        }
        PLLFO();
    }

    public void SSGENT() {
        if (work.SSGExtend) PANNING();
        SSGSUB();
        if (work.isDotNET) {
            AddEffect();
            return;
        }
        PLLFO();
    }

    public void AddEffect() {


        // !! ここにくる以前に最新のfnumが送信されている前提になっています !!
        int fnum = (int) work.pg.fnum;
        int deltn = 0;
        if (work.soundWork.getPCMFLG() != 0)
            deltn = work.soundWork.getDELT_N()[work.soundWork.getcurrentChip()];

        prcLFO();
        prcPortament();

        if ((work.soundWork.getPCMFLG() == 0 && fnum != work.pg.fnum)
                || (work.soundWork.getPCMFLG() != 0 && fnum + deltn != work.pg.fnum + work.soundWork.getDELT_N()[work.soundWork.getcurrentChip()])) {

            List<Object> args = new ArrayList<>();
            args.add(work.pg.lfoDeltaWork);
            MakeDummyCrntMmlDatum(enmMMLType.Lfo, args); // TODO LFODelta

            prcWriteFnum();
        }


        if (work.soundWork.getPCMFLG() != 0) {
            prcSoftEnvelope();
            //Console.WriteLine("{0}", Work.A_Reg);
            //send volume

            if ((work.pg.softEnvelopeFlag & 0x80) != 0) {
                if (work.soundWork.getcurrentChip() < 2)
                    PCMOUT((byte) 0xb, work.A_Reg);
                else
                    PCMOUT((byte) 0, (byte) 0x1b, work.A_Reg);
            }
        }
    }

    //**FM ｵﾝｹﾞﾝ ﾆ ﾀｲｽﾙ ｴﾝｿｳ ﾙｰﾁﾝ**

    public void FMSUB() {
        //Work.carry = false;
        work.pg.lengthCounter--;
        work.pg.lengthCounter = (byte) work.pg.lengthCounter;
        if (work.pg.lengthCounter == 0) {
            FMSUB1();
            return;
        }

        if ((byte) work.pg.lengthCounter > (byte) work.pg.quantize) {
            //if(!Work.carry)
            return;
        }

        //FMSUB0

        if (work.pg.mData[work.pg.dataAddressWork].dat == 0xfd) return;// COUNT OVER ?

        //    BIT5,(IX+33)
        if (work.pg.reverbFlg)//KUMA: 0x20(0b0010_0000)(bit5) = REVERVE FLAG  
        {
            FS2();
            return;
        }

        if (CheckCh3SpecialMode()
                || work.soundWork.getDRMF1() != 0
                || work.cd.getcurrentPageNo() == work.pg.getpageNo())
            KEYOFF(false);
    }

    public void FS2() {
        STV2((byte) ((byte) (work.pg.volume + work.pg.reverbVol) >> 1));
        work.pg.keyoffflg = true;
    }

    public void STV2(byte c) {
        if (work.soundWork.getcurrentChip() == 4) {
            STV2opm(c);
            return;
        }

        byte e;
        if (work.isDotNET) {
            if (work.cd.getFMVolMode() == 2)
                e = (byte) (127 - Math.min(Math.max(work.pg.volume, 0), 127));
            else if (work.cd.getFMVolMode() == 3)
                e = (byte) 255;
            else {
                //if (Work.cd.currentFMVolTable == null)
                //    Work.cd.currentFMVolTable = Work.soundWork.FMVDAT;
                e = work.cd.getcurrentFMVolTable()[c];// GET VOLUME DATA
            }
        } else
            e = work.soundWork.FMVDAT[c];// GET VOLUME DATA

        byte d = (byte) (0x40 + work.pg.channelNumber);// GET PORT No.

        if (work.pg.algo >= 8) return;//KUMA: オリジナルはチェック無し

        c = work.soundWork.CRYDAT[work.pg.algo];

        if (CheckCh3SpecialMode()) {
            if ((work.pg.useSlot & 1) != 0) {
                if ((c & (1 << 0)) != 0)//slot1
                {
                    byte v = e;
                    if (work.isDotNET) v = (byte) Math.min(Math.max(e + work.pg.v_tl[0], 0), 127);
                    PSGOUT((byte) (d + 0 * 4), v);// ｷｬﾘｱ ﾅﾗ PSGOUT ﾍ
                }
            }

            if ((work.pg.useSlot & 4) != 0) {
                if ((c & (1 << 1)) != 0)//slot3
                {
                    byte v = e;
                    if (work.isDotNET) v = (byte) Math.min(Math.max(e + work.pg.v_tl[1], 0), 127);
                    PSGOUT((byte) (d + 1 * 4), v);// ｷｬﾘｱ ﾅﾗ PSGOUT ﾍ
                }
            }

            if ((work.pg.useSlot & 2) != 0) {
                if ((c & (1 << 2)) != 0)//slot2
                {
                    byte v = e;
                    if (work.isDotNET) v = (byte) Math.min(Math.max(e + work.pg.v_tl[2], 0), 127);
                    PSGOUT((byte) (d + 2 * 4), v);// ｷｬﾘｱ ﾅﾗ PSGOUT ﾍ
                }
            }

            if ((work.pg.useSlot & 8) != 0) {
                if ((c & (1 << 3)) != 0)//slot4
                {
                    byte v = e;
                    if (work.isDotNET) v = (byte) Math.min(Math.max(e + work.pg.v_tl[3], 0), 127);
                    PSGOUT((byte) (d + 3 * 4), v);// ｷｬﾘｱ ﾅﾗ PSGOUT ﾍ
                }
            }

        } else {
            for (int b = 0; b < 4; b++) {
                if ((c & (1 << b)) != 0) {
                    byte v = e;
                    if (work.isDotNET) {
                        if (e == 255) {
                            v = (byte) Math.min(Math.max(work.pg.getTLDirectTable()[b] + work.pg.v_tl[b], 0), 127);
                        } else {
                            v = (byte) Math.min(Math.max(e + work.pg.v_tl[b], 0), 127);
                        }
                    }
                    PSGOUT((byte) (d + b * 4), v);// ｷｬﾘｱ ﾅﾗ PSGOUT ﾍ
                }
            }
        }

        //パラメータ表示向け
        List<Object> args = new ArrayList<>();
        if (work.isDotNET && (work.cd.getFMVolMode() == 2 || work.cd.getFMVolMode() == 3))
            args.add(work.pg.volume);
        else
            args.add(work.pg.volume - 4);
        args.add(work.cd.getFMVolMode());

        DummyOUT(enmMMLType.Volume, args);
    }

    public void STV2opm(byte c) {

        byte e;
        if (work.cd.getFMVolMode() == 2)
            e = (byte) (127 - Math.min(Math.max(work.pg.volume, 0), 127));
        else if (work.cd.getFMVolMode() == 3)
            e = (byte) 255;
        else {
            //if (Work.cd.currentFMVolTable == null) Work.cd.currentFMVolTable = Work.soundWork.FMVDAT;
            e = work.cd.getcurrentFMVolTable()[c];// GET VOLUME DATA
        }

        byte d = (byte) (0x60 + work.pg.channelNumber);// GET PORT No.

        if (work.pg.algo >= 8) return;//KUMA: オリジナルはチェック無し

        c = work.soundWork.CRYDAT[work.pg.algo];

        for (int b = 0; b < 4; b++) {
            if ((c & (1 << b)) != 0) {
                byte v = e;
                if (work.isDotNET) {
                    if (e == 255) {
                        v = (byte) Math.min(Math.max(work.pg.getTLDirectTable()[b] + work.pg.v_tl[b], 0), 127);
                    } else {
                        v = (byte) Math.min(Math.max(e + work.pg.v_tl[b], 0), 127);
                    }
                }
                PSGOUT((byte) (d + b * 8), v);// ｷｬﾘｱ ﾅﾗ PSGOUT ﾍ
            }
        }

        //パラメータ表示向け
        List<Object> args = new ArrayList<>();
        if (work.isDotNET && (work.cd.getFMVolMode() == 2 || work.cd.getFMVolMode() == 3))
            args.add(work.pg.volume);
        else
            args.add(work.pg.volume - 4);
        args.add(work.cd.getFMVolMode());

        DummyOUT(enmMMLType.Volume, args);
    }

    public void PSGOUT(byte d, byte e) {
        byte port = 0;
        if (d >= 0x30) {
            if (work.soundWork.getFMPORT() != 0) {
                port = 1;
            }
        }

        //if (d == 0x27)// && d <= 0x1d) {
        //    Console.WriteLine("{0:x} {1:x}", d, e);
        //}

        ChipDatum dat = new ChipDatum(port, d, e, 0, work.crntMmlDatum);
        WriteRegister(work.soundWork.getcurrentChip(), dat);
    }

    public void PSGOUT(byte c, byte p, byte d, byte e) {
        ChipDatum dat = new ChipDatum(p, d, e, 0, work.crntMmlDatum);
        WriteRegister(c, dat);
    }

    public void DummyOUT() {
        ChipDatum dat = new ChipDatum(-1, -1, -1, 0, work.crntMmlDatum);
        WriteRegister(work.soundWork.getcurrentChip(), dat);
    }

    public void DummyOUT(enmMMLType type, List<Object> args) {
        MakeDummyCrntMmlDatum(type, args);
        DummyOUT();
    }

    public void MakeDummyCrntMmlDatum(enmMMLType type, List<Object> args) {
        LinePos lp;
        if (work.soundWork.getcurrentChip() != 4) {
            lp = new LinePos(null, "", -1, -1, -1
                    , work.soundWork.getcurrentChip() < 2
                    ? (work.soundWork.getPCMFLG() != 0 ? "ADPCM" : (work.soundWork.getDRMF1() != 0 ? "RHYTHM" : (work.soundWork.getSSGF1() != 0 ? "SSG" : "FM")))
                    : (work.soundWork.getPCMFLG() != 0 ? "ADPCM-B" : (work.soundWork.getDRMF1() != 0 ? "ADPCM-A" : (work.soundWork.getSSGF1() != 0 ? "SSG" : "FM")))
                    , Common.GetChipName(work.soundWork.getcurrentChip())
                    , 0
                    , Common.GetChipNumber(work.soundWork.getcurrentChip())
                    , work.soundWork.getcurrentCh(), "", "", 0
            );
        } else {
            lp = new LinePos(null, "", -1, -1, -1
                    , "FM"
                    , "YM2151"
                    , 0
                    , work.soundWork.getcurrentChip() % 2
                    , work.soundWork.getcurrentCh(),
                    "", "", 0
            );
        }
        work.crntMmlDatum = new MmlDatum(type, args, lp, 0);
    }

    /** KEY-OFF ROUTINE */
    public void KEYOFF(boolean force /*=false*/) {
        if (work.isDotNET && !work.pg.enblKeyOff && !force) return;

        if (work.soundWork.getcurrentChip() == 4) {
            work.pg.KDWork[0] = 0;
            work.pg.KDWork[1] = 0;
            work.pg.KDWork[2] = 0;
            work.pg.KDWork[3] = 0;
            PSGOUT((byte) 0x08, (byte) work.pg.channelNumber); // KEY-OFF
            return;
        }

        if (work.soundWork.getPCMFLG() != 0) {
            PCMEND();
            return;
        }

        if (work.soundWork.getDRMF1() != 0) {
            if (work.getHeader().mupb == null) {
                // --ﾘｽﾞﾑ ｵﾝｹﾞﾝ ﾉ ｷｰｵﾌ--
                PSGOUT((byte) 0x10, (byte) ((work.soundWork.getRHYTHM() & 0b0011_1111) | 0x80)); // GET RETHM PARAMETER
            } else {
                work.rhythmORKeyOff[work.soundWork.getcurrentChip()] |= (work.pg.instrumentNumber & 0b0011_1111);
            }
            return;
        }

        work.pg.KDWork[0] = 0;
        work.pg.KDWork[1] = 0;
        work.pg.KDWork[2] = 0;
        work.pg.KDWork[3] = 0;

        if (CheckCh3SpecialMode()) {
            work.cd.ch3KeyOn &= (byte) ~(work.pg.useSlot << 4);
            byte a = (byte) (work.cd.ch3KeyOn | 0x2);
            PSGOUT((byte) 0x28, a);//KEY-OFF
            //Console.WriteLine("KEYOFF : {0:x02}", a);
        } else {
            PSGOUT((byte) 0x28, (byte) (work.soundWork.getFMPORT() + work.pg.channelNumber));//  KEY-OFF
        }

    }

    public void PCMEND() {
        if (work.soundWork.getcurrentChip() > 1) {
            PCMEND2610();
            return;
        }

        if (work.cd.getcurrentPageNo() != work.pg.getpageNo()) return;

        if ((work.pg.softEnvelopeFlag & 0x80) == 0) {
            PCMOUT((byte) 0x0b, (byte) 0x00);
            PCMOUT((byte) 0x01, (byte) 0x00);
            PCMOUT((byte) 0x00, (byte) 0x21);
            return;
        }

        work.pg.softEnvelopeFlag &= 0b1000_1111;// STATE 4 (ﾘﾘｰｽ)

    }

    public void PCMEND2610() {
        if (work.cd.getcurrentPageNo() != work.pg.getpageNo()) return;

        if ((work.pg.softEnvelopeFlag & 0x80) == 0) {
            PCMOUT((byte) 0, (byte) 0x1b, (byte) 0x00);
            PCMOUT((byte) 0, (byte) 0x11, (byte) 0x00);
            PCMOUT((byte) 0, (byte) 0x10, (byte) 0x21);
            return;
        }

        work.pg.softEnvelopeFlag &= 0b1000_1111;// STATE 4 (ﾘﾘｰｽ)

    }

    /** ADPCM OUT */
    public void PCMOUT(byte d, byte e) {
        ChipDatum dat = new ChipDatum(1, d, e, 0, work.crntMmlDatum);
        WriteRegister(work.soundWork.getcurrentChip(), dat);
    }

    public void PCMOUT(byte p, byte d, byte e) {
        ChipDatum dat = new ChipDatum(p, d, e, 0, work.crntMmlDatum);
        WriteRegister(work.soundWork.getcurrentChip(), dat);
    }

    /** SET NEW SOUND */
    public void FMSUB1() {
        work.pg.keyoffflg = true;
        if (work.pg.mData[work.pg.dataAddressWork].dat != 0x0FD) // COUNT OVER?
        {
            FMSUBC(work.pg.dataAddressWork);
            return;
        }

        work.pg.keyoffflg = false; // RES KEYOFF FLAG
        FMSUBC(work.pg.dataAddressWork + 1);
    }

    public void FMSUBC(int hl) {

        byte a;
        boolean nrFlg = false;
        do {
            Log.writeLine(LogLevel.TRACE, String.format("%x", hl + 0xc200));
            a = (byte) work.pg.mData[hl].dat;
            //* 00H as end
            while (a == 0)// ﾃﾞｰﾀ ｼｭｳﾘｮｳ ｦ ｼﾗﾍﾞﾙ
            {
                work.pg.loopEndFlg = true;

                if (work.pg.dataTopAddress == -1 || nrFlg) {
                    if (nrFlg)
                        work.abnormalEnd = true;
                    FMEND(hl);//* DATA TOP ADRESS ｶﾞ 0000H ﾃﾞ BGM
                    return; // ﾉ ｼｭｳﾘｮｳ ｦ ｹｯﾃｲ ｿﾚ ｲｶﾞｲﾊ ｸﾘｶｴｼ
                }
                hl = (int) work.pg.dataTopAddress;
                a = (byte) work.pg.mData[hl].dat;// GET FLAG & LENGTH
                work.pg.incloopCounter();
                //if (Work.pg.loopCounter > Work.nowLoopCounter) Work.nowLoopCounter = Work.pg.loopCounter;
                nrFlg = true;
            }

            //演奏情報退避
            work.crntMmlDatum = work.pg.mData[hl];

            // **SET LENGTH**
            hl++;
            if (a < 0xf0) break;

            // DATA ｶﾞ ｺﾏﾝﾄﾞ ﾅﾗ FMSUBA ﾍ
            // **ｻﾌﾞ･ｺﾏﾝﾄﾞ ﾉ ｹｯﾃｲ**
            //FMSUBA
            a &= 0xf;// A=COMMAND No.(0-F)
            work.hl = hl;
            FMCOM[a].run();
            hl = work.hl;
        } while (true);

        nrFlg = false;
        work.pg.lengthCounter = a & 0x7f;// SET WAIT COUNTER


        if ((a & 0x80) != 0) //BIT7(ｷｭｳﾌ ﾌﾗｸﾞ)
        {
            work.crntMmlDatum = work.pg.mData[hl - 1];
            // **SET F-NUMBER**
            work.pg.dataAddressWork = hl;// SET NEXT SOUND DATA ADD

            if (work.pg.reverbMode) {
                if (
                        CheckCh3SpecialMode()
                                || work.cd.getcurrentPageNo() == work.pg.getpageNo()
                                || work.soundWork.getDRMF1() != 0)
                    KEYOFF(false);
                return;
            }
            if (work.pg.reverbFlg) {
                FS2();
                return;
            }
            if (
                    CheckCh3SpecialMode()
                            || work.cd.getcurrentPageNo() == work.pg.getpageNo()
                            || work.soundWork.getDRMF1() != 0)
                KEYOFF(false);

            DummyOUT();

            return;
        }

        if (work.cd.getkeyOnCh() != -1 && work.cd.getkeyOnCh() != work.pg.getpageNo())// !Work.pg.keyoffflg)
        {
            work.pg.dataAddressWork = hl + 1;// SET NEXT SOUND DATA ADD
            return;
        }

        if (!CheckCh3SpecialMode() && work.cd.getcurrentPageNo() != work.pg.getpageNo()) {
            //切り替え処理
            RestoreOTOPST();
            restoreSTEREO_AMD98();
        }

        //カレントページ情報セット
        work.cd.setcurrentPageNo(work.pg.getpageNo());

        // ｵﾝﾌﾟ ﾅﾗ FMSUB5 ﾍ
        if (work.pg.keyoffflg) {
            KEYOFF(false);
        }

        if (!work.soundWork.Ch3SpMode(work.soundWork.getcurrentChip()))//効果音モードでは無い場合
        {
            FMSUB4(hl);
            return;
        }

        if (work.soundWork.getFMPORT() != 0) {
            FMSUB4(hl);
            return;
        }

        if (work.pg.channelNumber == 2)//CH=3?
        {
            EXMODE(hl);
            return;
        }

        FMSUB4(hl);
    }

    // **ｴﾝｿｳ ｵﾜﾘ**

    public void FMEND(int hl) {
        work.pg.setmusicEnd(true);
        work.pg.dataAddressWork = hl;

        if (work.soundWork.getPCMFLG() != 0) {
            PCMEND();
            return;
        }
        if (CheckCh3SpecialMode()
                || work.cd.getcurrentPageNo() == work.pg.getpageNo())
            KEYOFF(false);
    }

    public void FMSUB4(int hl) {
        byte a, b;
        work.carry = false;

        a = (byte) work.pg.mData[hl].dat;// A=Bsynchronized(OCTAVE-1 ) & KEY CODE DATA
        work.pg.dataAddressWork = hl + 1;// SET NEXT SOUND DATA ADD
        if (!work.pg.keyoffflg // CHECK KEYOFF FLAG
                && work.pg.beforeCode == a)// GET BEFORE CODE DATA
        {
            work.carry = true;
            return;
        }

        work.pg.beforeCode = a;

        if (work.soundWork.getPCMFLG() != 0) {
            //PCMGFQ:
            hl = (int) (work.soundWork.PCMNMB[work.soundWork.getcurrentChip() / 2][a & 0b0000_1111] + work.pg.detune);
            a >>= 4;
            b = a;
            //ASUB7:
            while (b != 0) {
                hl >>= 1;
                b--;
            }
            //ASUB72:
            work.soundWork.getDELT_N()[work.soundWork.getcurrentChip()] = hl;
            work.pg.fnum = 0;
            if (!work.pg.keyoffflg) {
                LFORST();
            }
            LFORST2();
            PLAY();
            return;//戻り値がcarry
        }

        if (work.soundWork.getDRMF1() == 0) {
            //FMGFQ:
            if (work.soundWork.getcurrentChip() != 4) {
                hl = work.soundWork.FNUMB[work.soundWork.getcurrentChip() / 2][a & 0xf];// GET KEY CODE(C, C+, D...B)
                hl |= (short) ((a & 0x70) << 7);// GET BLOCK DATA
                // A4-A6 ﾎﾟｰﾄ ｼｭﾂﾘｮｸﾖｳ ﾆ ｱﾜｾﾙ
                // GET FNUM2
                // A= KEY CODE & FNUM HI

                hl = (int) (hl + work.pg.detune);// GET DETUNE DATA
                // DETUNE PLUS
            } else {
                //OPM専用処理
                short val = work.soundWork.FNUMBopm[work.getHeader().OPMClockMode == MUBHeader.enmOPMClockMode.normal ? 0 : 1][a & 0xf];// GET KEY CODE(C, C+, D...B)
                int oct = (a & 0x70) >> 4;
                if (val < 0) {
                    oct--;
                    val += 0x300;
                    if (oct < 0) {
                        oct = 0;
                        val = 0;
                    }
                }

                //detune加算
                hl = AddDetuneToFNumopm((short) (val | ((oct & 0x7) << 11)), (short) work.pg.detune);
            }

            if (!work.pg.tlLfoflg) {
                work.pg.fnum = hl;// FOR LFO
                // FOR LFO
                work.soundWork.setFNUM(hl);
            }
            if (work.pg.keyoffflg) {
                LFORST();
            }
            LFORST2();
            //FMSUB8:
            FMSUB6(hl, work.soundWork.getFMSUB8_VAL());//戻り値がcarry
            return;
        }

        //DRMFQ:
        if (!work.pg.keyoffflg) {
            return;
        }
        DKEYON();//戻り値がcarry
    }

    /**
     * 効果音モード専用のFMSUB4
     */
    public void FMSUB4ex(int hl) {
        byte a;
        work.carry = false;

        a = (byte) work.pg.mData[hl].dat;// A=Bsynchronized(OCTAVE-1 ) & KEY CODE DATA
        work.pg.dataAddressWork = hl + 1;// SET NEXT SOUND DATA ADD
        if (!work.pg.keyoffflg // CHECK KEYOFF FLAG
                && work.pg.beforeCode == a)// GET BEFORE CODE DATA
        {
            work.carry = true;
            return;
        }

        work.pg.beforeCode = a;

        hl = work.soundWork.FNUMB[work.soundWork.getcurrentChip() / 2][a & 0xf];// GET KEY CODE(C, C+, D...B)
        hl |= (short) ((a & 0x70) << 7);// GET BLOCK DATA
        // A4-A6 ﾎﾟｰﾄ ｼｭﾂﾘｮｸﾖｳ ﾆ ｱﾜｾﾙ
        // GET FNUM2
        // A= KEY CODE & FNUM HI

        hl = hl + work.pg.detune;// GET DETUNE DATA
        // DETUNE PLUS
        if (!work.pg.tlLfoflg) {
            work.pg.fnum = hl;// FOR LFO
            // FOR LFO
            work.soundWork.setFNUM(hl);
        }
        if (work.pg.keyoffflg) {
            LFORST();
        }
        LFORST2();
    }

    public void FMSUB6(int hl, int bc) {
        if (work.soundWork.getcurrentChip() == 4) {
            FMSUB6opm(hl, bc);
            return;
        }

        if (work.isDotNET) {
            hl = AddDetuneToFNum((short) hl, (short) bc);
        } else {
            hl += bc;// BLOCK/FNUM1&2 DETUNE PLUS(for SE MODE)
        }

        byte e = (byte) (hl >> 8);// BLOCK/F-NUMBER2 DATA
        //FPORT:
        byte d = work.soundWork.getFPORT_VAL();// 0x0A4;// PORT A4H
        d += (byte) work.pg.channelNumber;
        PSGOUT(d, e);

        d -= 4;
        e = (byte) hl;// F-NUMBER1 DATA
        //FMSUB7:
        PSGOUT(d, e);

        KEYON();
        work.carry = false;
    }

    public void FMSUB6ex(int hl, int bc) {
        if (work.isDotNET) {
            hl = AddDetuneToFNum((short) hl, (short) bc);
        } else {
            hl += bc;// BLOCK/FNUM1&2 DETUNE PLUS(for SE MODE)
        }

        byte e = (byte) (hl >> 8);// BLOCK/F-NUMBER2 DATA
        //FPORT:
        byte d = work.soundWork.getFPORT_VAL();// 0x0A4;// PORT A4H
        d += (byte) work.pg.channelNumber;
        PSGOUT(d, e);

        d -= 4;
        e = (byte) hl;// F-NUMBER1 DATA
        //FMSUB7:
        PSGOUT(d, e);

    }

    public void FMSUB6opm(int hl, int bc) {
        hl = AddDetuneToFNumopm((short) hl, (short) (short) bc);

        byte oct = (byte) (((hl & 0x3800) >> 11));
        byte note = (byte) (((hl & 0x7ff) >> 6));
        note--;
        if (note == 0xff) {
            oct--;
            note = 11;
        }
        note = (byte) (note < 3 ? note : (note < 6 ? (note + 1) : (note < 9 ? (note + 2) : (note + 3))));

        byte e = (byte) ((oct << 4) | note);// oct:bit6-4 note :bit3-0
        byte d = 0x28;// KC のアドレス
        d += (byte) work.pg.channelNumber;
        PSGOUT(d, e);

        d += 8;//KF のアドレス
        e = (byte) ((hl & 0x3f) << 2);// KF (bit:7-2)
        PSGOUT(d, e);

        KEYONopm();
        work.carry = false;
    }

    private short AddDetuneToFNum(short fnum, short detune) {
        int block = (byte) ((fnum >> 11) & 7);
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

        return (short) (((block & 7) << 11) | (fnum11b & 0x7ff));
    }

    private short AddDetuneToFNumopm(short fnum, short detune) {
        int block = (byte) ((fnum >> 11) & 7);
        int fnum11b = fnum & 0x7ff;

        fnum11b += detune;
        if (detune < 0) {
            while (fnum11b < 0) // 0より小さい
            {
                if (block == 0) {
                    if (fnum11b < 0) fnum11b = 0;//limit
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

        return (short) (((block & 7) << 11) | (fnum11b & 0x7ff));
    }

    /** SE MODE ﾉ DETUNE ｾｯﾃｲ */
    public void EXMODE(int hl) {
        //fnum算出

        FMSUB4ex(hl);// SET OP1
        if (work.carry) {
            return;
        }

        //Ch3 のスロット毎にfnumをセット
        if ((work.pg.useSlot & 8) != 0) //slot4
            FMSUB6ex(work.soundWork.getFNUM(), work.soundWork.DETDAT[work.soundWork.getcurrentChip()][0]);

        work.soundWork.setFPORT_VAL((byte) 0xaa);
        if ((work.pg.useSlot & 4) != 0) //slot3
            FMSUB6ex(work.soundWork.getFNUM(), work.soundWork.DETDAT[work.soundWork.getcurrentChip()][1]);

        work.soundWork.setFPORT_VAL((byte) 0xab);
        if ((work.pg.useSlot & 1) != 0) //slot1
            FMSUB6ex(work.soundWork.getFNUM(), work.soundWork.DETDAT[work.soundWork.getcurrentChip()][2]);

        work.soundWork.setFPORT_VAL((byte) 0xac);
        if ((work.pg.useSlot & 2) != 0) //slot2
            FMSUB6ex(work.soundWork.getFNUM(), work.soundWork.DETDAT[work.soundWork.getcurrentChip()][3]);

        work.soundWork.setFPORT_VAL((byte) 0xa4);

        KEYONex();
        work.carry = false;

    }

    /** RESET PEAK L.&DELAY */
    public void LFORST() {
        work.pg.lfoDelayWork = work.pg.lfoDelay;// LFO DELAY ﾉ ｻｲｾｯﾃｲ
        work.pg.lfoContFlg = false;            // RESET LFO CONTINE FLAG
    }

    public void LFORST2() {
        work.pg.lfoPeakWork = work.pg.lfoPeak >> 1;// LFO PEAK LEVEL ｻｲ ｾｯﾃｲ
        work.pg.lfoDeltaWork = work.pg.lfoDelta;// ﾍﾝｶﾘｮｳ ｻｲｾｯﾃｲ
        work.pg.setSSGTremoloVol(0);
        if (!work.pg.tlLfoflg) {
            return;
        }
        work.pg.fnum = work.pg.TLlfo;
        work.pg.bfnum2 = 0;
    }

    /**
     * ADPCM PLAY
     * IN:(STTADR)<=ｻｲｾｲ ｽﾀｰﾄ ｱﾄﾞﾚｽ
     * (ENDADR)  <=ｻｲｾｲ ｴﾝﾄﾞ ｱﾄﾞﾚｽ
     * (DELT_N)<=ｻｲｾｲ ﾚｰﾄ
     */
    public void PLAY() {
        if (work.soundWork.getcurrentChip() > 1) {
            PLAY2610();
            return;
        }

        if (work.cd.getkeyOnCh() != -1)
            return; // KUMA:既に他のページが発音中の場合は処理しない
        work.cd.setkeyOnCh(work.pg.getpageNo());

        if (work.soundWork.getREADY() == 0) return;

        PCMOUT((byte) 0x0b, (byte) 0x00);
        PCMOUT((byte) 0x01, (byte) 0x00);
        PCMOUT((byte) 0x00, (byte) 0x21);
        PCMOUT((byte) 0x10, (byte) 0x08);
        PCMOUT((byte) 0x10, (byte) 0x80);// INIT
        PCMOUT((byte) 0x02, (byte) work.soundWork.getSTTADR()[work.soundWork.getcurrentChip()]);// START ADR
        PCMOUT((byte) 0x03, (byte) (work.soundWork.getSTTADR()[work.soundWork.getcurrentChip()] >> 8));
        int eAdr = work.soundWork.getENDADR()[work.soundWork.getcurrentChip()];
        PCMOUT((byte) 0x04, (byte) eAdr);// END ADR
        PCMOUT((byte) 0x05, (byte) (eAdr >> 8));

        if (work.isDotNET && work.pg.keyoffflg) {
            //if (Work.soundWork.getcurrentChip() == 0)
            //    eAdr -= Work.soundWork.STTADR[Work.soundWork.getcurrentChip()];
            work.pg.lfoContFlg = false;// RESET LFO CONTINE FLAG
            if ((work.pg.softEnvelopeFlag & 0x80) != 0) {
                work.pg.softEnvelopeFlag = 0x90;
                work.pg.softEnvelopeCounter = (byte) work.pg.softEnvelopeParam[0];//KUMA:ALがcounterの初期値として使用される
            }
        }

        PCMOUT((byte) 0x09, (byte) work.soundWork.getDELT_N()[work.soundWork.getcurrentChip()]);// ｻｲｾｲ ﾚｰﾄ ｶｲ
        PCMOUT((byte) 0x0a, (byte) (work.soundWork.getDELT_N()[work.soundWork.getcurrentChip()] >> 8));// ｻｲｾｲ ﾚｰﾄ ｼﾞｮｳｲ
        PCMOUT((byte) 0x00, (byte) 0xa0);

        byte e = (byte) (work.soundWork.getTOTALV() * 4 + work.pg.volume);
        if (e >= 250) {
            e = 0;
        }
        //PL1:
        if (work.soundWork.getPVMODE() != 0) {
            e += (byte) work.pg.volReg;
        }
        //PL2:
        if ((work.pg.softEnvelopeFlag & 0x80) == 0)
            PCMOUT((byte) 0xb, e);// VOLUME

        e = (byte) ((work.soundWork.getPCMLR()[work.soundWork.getcurrentChip()] & 3) << 6);
        PCMOUT((byte) 0x01, e);// 1 bit TYPE, L&R OUT

        // ｼﾝｺﾞｳﾀﾞｽ
        work.soundWork.setP_OUT(work.soundWork.getPCMNUM());
    }

    public void PLAY2610() {
        if (work.cd.getkeyOnCh() != -1)
            return; // KUMA:既に他のページが発音中の場合は処理しない
        work.cd.setkeyOnCh(work.pg.getpageNo());

        if (work.soundWork.getREADY() == 0) return;

        if (work.pg.keyoffflg) {
            PCMOUT((byte) 0, (byte) 0x1b, (byte) 0x00);
            PCMOUT((byte) 0, (byte) 0x11, (byte) 0x00);
            PCMOUT((byte) 0, (byte) 0x10, (byte) 0x21);
            PCMOUT((byte) 0, (byte) 0x1c, (byte) 0x08);
            PCMOUT((byte) 0, (byte) 0x1c, (byte) 0x80);// INIT
            PCMOUT((byte) 0, (byte) 0x12, (byte) (work.soundWork.getSTTADR()[work.soundWork.getcurrentChip()] >> 0));// START ADR
            PCMOUT((byte) 0, (byte) 0x13, (byte) (work.soundWork.getSTTADR()[work.soundWork.getcurrentChip()] >> 8));
            PCMOUT((byte) 0, (byte) 0x14, (byte) (work.soundWork.getENDADR()[work.soundWork.getcurrentChip()] >> 0));// END ADR
            PCMOUT((byte) 0, (byte) 0x15, (byte) (work.soundWork.getENDADR()[work.soundWork.getcurrentChip()] >> 8));
            work.pg.lfoContFlg = false;// RESET LFO CONTINE FLAG
            if ((work.pg.softEnvelopeFlag & 0x80) != 0) {
                work.pg.softEnvelopeFlag = 0x90;
                work.pg.softEnvelopeCounter = (byte) work.pg.softEnvelopeParam[0];//KUMA:ALがcounterの初期値として使用される
            }
        }

        PCMOUT((byte) 0, (byte) 0x19, (byte) (work.soundWork.getDELT_N()[work.soundWork.getcurrentChip()] >> 0));// ｻｲｾｲ ﾚｰﾄ ｶｲ
        PCMOUT((byte) 0, (byte) 0x1a, (byte) (work.soundWork.getDELT_N()[work.soundWork.getcurrentChip()] >> 8));// ｻｲｾｲ ﾚｰﾄ ｼﾞｮｳｲ
        PCMOUT((byte) 0, (byte) 0x10, (byte) 0xa0);

        byte e = (byte) (work.soundWork.getTOTALV() * 4 + work.pg.volume);
        if ((e & 0xff) >= 250) {
            e = 0;
        }
        //PL1:
        if (work.soundWork.getPVMODE() != 0) {
            e += (byte) work.pg.volReg;
        }
        //PL2:
        if ((work.pg.softEnvelopeFlag & 0x80) == 0)
            PCMOUT((byte) 0, (byte) 0x1b, e);// VOLUME

        e = (byte) ((work.soundWork.getPCMLR()[work.soundWork.getcurrentChip()] & 3) << 6);
        PCMOUT((byte) 0, (byte) 0x11, e);// 1 bit TYPE, L&R OUT

        // ｼﾝｺﾞｳﾀﾞｽ
        work.soundWork.setP_OUT(work.soundWork.getPCMNUM());
    }

    public void SetADPCM_AAddress(int ach) {

        PCMOUT((byte) 1, (byte) (0x10 + ach), (byte) (work.soundWork.getPCMaSTTADR()[work.soundWork.getcurrentChip() - 2][ach] >> 0));// START ADR
        PCMOUT((byte) 1, (byte) (0x18 + ach), (byte) (work.soundWork.getPCMaSTTADR()[work.soundWork.getcurrentChip() - 2][ach] >> 8));
        PCMOUT((byte) 1, (byte) (0x20 + ach), (byte) (work.soundWork.PCMaENDADR[work.soundWork.getcurrentChip() - 2][ach] >> 0));// END ADR
        PCMOUT((byte) 1, (byte) (0x28 + ach), (byte) (work.soundWork.PCMaENDADR[work.soundWork.getcurrentChip() - 2][ach] >> 8));

    }

    public void SetADPCM_A_InstrumentAddress(int ach, int i) {

        if (work.pcmTables[work.soundWork.getcurrentChip() + 2] == null) return;
        if (work.pcmTables[work.soundWork.getcurrentChip() + 2].length < 1) return;

        if (i >= work.pcmTables[work.soundWork.getcurrentChip() + 2].length) return;
        work.soundWork.getPCMaSTTADR()[work.soundWork.getcurrentChip() - 2][ach] = work.pcmTables[work.soundWork.getcurrentChip() + 2][i].getItem2()[0];//start address
        work.soundWork.PCMaENDADR[work.soundWork.getcurrentChip() - 2][ach] = work.pcmTables[work.soundWork.getcurrentChip() + 2][i].getItem2()[1];//end address

    }

    /** ﾘｽﾞﾑ ｵﾝｹﾞﾝ ﾉ ｷｰｵﾝ */
    public void DKEYON() {
        if (work.soundWork.getREADY() == 0) return;
        if (work.getHeader().mupb == null) {
            PSGOUT((byte) 0x10, (byte) (work.soundWork.getRHYTHM() & work.getHeader().RhythmMute[0]));// KEY ON
        } else {
            work.rhythmOR[work.soundWork.getcurrentChip()] |= (work.pg.instrumentNumber &
                    work.getHeader().RhythmMute[work.soundWork.getcurrentChip()]);

            //アドレス送信
            if (work.soundWork.getcurrentChip() > 1) {
                for (int i = 0; i < 6; i++) {
                    if ((work.pg.instrumentNumber & (1 << i)) != 0) {
                        SetADPCM_A_InstrumentAddress(i, work.pg.beforeCode);
                        SetADPCM_AAddress(i);
                    }
                }
            }
        }
    }

    /** KEY-ON ROUTINE */
    public void KEYON() {
        if (work.soundWork.getREADY() == 0) return;
        if (work.cd.getkeyOnCh() != -1) return;//KUMA:既に他のページが発音中の場合は処理しない

        byte a = 0x04;
        if (work.soundWork.getFMPORT() == 0) {
            a = 0x00;
        }

        if (!work.pg.KeyOnDelayFlag) {
            a += work.pg.keyOnSlot;
        } else {
            work.pg.keyOnSlot = 0x00;
            if (work.pg.KD[0] == 0) work.pg.keyOnSlot += 0x10;
            if (work.pg.KD[1] == 0) work.pg.keyOnSlot += 0x20;
            if (work.pg.KD[2] == 0) work.pg.keyOnSlot += 0x40;
            if (work.pg.KD[3] == 0) work.pg.keyOnSlot += 0x80;
            a += work.pg.keyOnSlot;

            work.pg.KDWork[0] = work.pg.KD[0];
            work.pg.KDWork[1] = work.pg.KD[1];
            work.pg.KDWork[2] = work.pg.KD[2];
            work.pg.KDWork[3] = work.pg.KD[3];
        }

        //発音ページ情報セット
        work.cd.setkeyOnCh(work.pg.getpageNo());

        //KEYON2:
        a += (byte) work.pg.channelNumber;
        PSGOUT((byte) 0x28, a); // KEY-ON

        if (work.pg.reverbFlg) {
            STVOL();
        }
    }

    public void KEYONex() {
        if (work.soundWork.getREADY() == 0) return;
        //if (Work.cd.keyOnCh != -1) return;//KUMA:既に他のページが発音中の場合は処理しない

        byte a = 0x02;
        //if (Work.soundWork.FMPORT == 0) {
        //    a = 0x00;
        //}

        if (!work.pg.KeyOnDelayFlag) {
            a += work.pg.keyOnSlot;
        } else {
            work.pg.keyOnSlot = 0x00;
            if (work.pg.KD[0] == 0) work.pg.keyOnSlot += 0x10;
            if (work.pg.KD[1] == 0) work.pg.keyOnSlot += 0x20;
            if (work.pg.KD[2] == 0) work.pg.keyOnSlot += 0x40;
            if (work.pg.KD[3] == 0) work.pg.keyOnSlot += 0x80;
            a += work.pg.keyOnSlot;

            work.pg.KDWork[0] = work.pg.KD[0];
            work.pg.KDWork[1] = work.pg.KD[1];
            work.pg.KDWork[2] = work.pg.KD[2];
            work.pg.KDWork[3] = work.pg.KD[3];
        }

        // 発音ページ情報セット
        //Work.cd.keyOnCh = Work.pg.getpageNo();

        work.cd.ch3KeyOn |= (byte) (work.pg.useSlot << 4);
        a &= (byte) (work.cd.ch3KeyOn | 0xf);
        PSGOUT((byte) 0x28, a);//KEY-ON

        if (work.pg.reverbFlg) {
            STVOL();
        }
    }

    public void KEYONopm() {
        if (work.soundWork.getREADY() == 0) return;
        if (work.cd.getkeyOnCh() != -1) return;//KUMA:既に他のページが発音中の場合は処理しない

        byte a = 0x00;
        if (!work.pg.KeyOnDelayFlag) {
            a += (byte) (work.pg.keyOnSlot >> 1);
        } else {
            work.pg.keyOnSlot = 0x00;
            if (work.pg.KD[0] == 0) work.pg.keyOnSlot += 0x10;
            if (work.pg.KD[1] == 0) work.pg.keyOnSlot += 0x20;
            if (work.pg.KD[2] == 0) work.pg.keyOnSlot += 0x40;
            if (work.pg.KD[3] == 0) work.pg.keyOnSlot += 0x80;
            a += (byte) (work.pg.keyOnSlot >> 1);

            work.pg.KDWork[0] = work.pg.KD[0];
            work.pg.KDWork[1] = work.pg.KD[1];
            work.pg.KDWork[2] = work.pg.KD[2];
            work.pg.KDWork[3] = work.pg.KD[3];
        }

        // 発音ページ情報セット
        work.cd.setkeyOnCh(work.pg.getpageNo());

        // KEYON2:
        a += (byte) work.pg.channelNumber;
        PSGOUT((byte) 0x08, a);//KEY-ON

        if (work.pg.reverbFlg) {
            STVOL();
        }
    }

    /** ﾎﾞﾘｭｰﾑ */
    public void STVOL() {
        byte c;

        //STV1
        c = (byte) (work.soundWork.getTOTALV() + work.pg.volume);// INPUT VOLUME
        if (c >= 20) {
            c = 0;
        }
        //STV12:
        STV2(c);
    }


    public void RestoreOTOPST() {
        if (work.soundWork.getPCMFLG() != 0) {
            restoreOTOPCM();
            return;
        }

        if (work.soundWork.getDRMF1() != 0) {
            restoreOTODRM();
            return;
        }

        STENV();
        STVOL();
    }

    /** ｵﾝｼｮｸ ｾｯﾄ ﾒｲﾝ */
    public void OTOPST() {
        if (work.soundWork.getPCMFLG() != 0) {
            OTOPCM();
            return;
        }

        if (work.soundWork.getDRMF1() != 0) {
            OTODRM();
            return;
        }

        work.pg.instrumentNumber = work.pg.mData[work.hl++].dat;

        // KUMA:カレントページの場合、または効果音モード有効時のみ音色を変更する
        if (!CheckCh3SpecialMode() && work.cd.getcurrentPageNo() != work.pg.getpageNo()) return;

        STENV();
        STVOL();
    }

    public void OTODRM() {
        DummyOUT();
        work.soundWork.setRHYTHM(work.pg.mData[work.hl++].dat); // SET RETHM PARA
        work.pg.instrumentNumber = work.soundWork.getRHYTHM();
    }

    public void restoreOTODRM() {
        DummyOUT();
        work.soundWork.setRHYTHM(work.pg.instrumentNumber);
    }

    public void OTOPCM() {
        if (work.cd.getcurrentPageNo() != work.pg.getpageNo()) {
            work.pg.instrumentNumber = (byte) work.pg.mData[work.hl++].dat - 1;
            return;
        }
        DummyOUT();
        byte a = (byte) work.pg.mData[work.hl++].dat;
        work.soundWork.setPCMNUM(a);
        a--;
        work.pg.instrumentNumber = a;

        if (work.pcmTables != null && work.pcmTables[work.soundWork.getcurrentChip()] != null && work.pcmTables[work.soundWork.getcurrentChip()].length > a) {
            work.soundWork.getSTTADR()[work.soundWork.getcurrentChip()] = work.pcmTables[work.soundWork.getcurrentChip()][a].getItem2()[0];//start address
            work.soundWork.getENDADR()[work.soundWork.getcurrentChip()] = work.pcmTables[work.soundWork.getcurrentChip()][a].getItem2()[1];//end address
        }

        if (work.soundWork.getPVMODE() == 0) return;

        work.pg.volume = (byte) work.pcmTables[work.soundWork.getcurrentChip()][a].getItem2()[3];
    }

    public void restoreOTOPCM() {
        DummyOUT();
        work.soundWork.setPCMNUM((byte) (work.pg.instrumentNumber + 1));
        byte a = (byte) work.pg.instrumentNumber;

        if (work.pcmTables != null
                && work.pcmTables[work.soundWork.getcurrentChip()] != null
                && work.pcmTables[work.soundWork.getcurrentChip()].length > a) {
            work.soundWork.getSTTADR()[work.soundWork.getcurrentChip()] = work.pcmTables[work.soundWork.getcurrentChip()][a].getItem2()[0];//start address
            work.soundWork.getENDADR()[work.soundWork.getcurrentChip()] = work.pcmTables[work.soundWork.getcurrentChip()][a].getItem2()[1];//end address
        }

        if (work.soundWork.getPVMODE() == 0) return;

        work.pg.volume = (byte) work.pcmTables[work.soundWork.getcurrentChip()][a].getItem2()[3];
    }

    // **ｵﾝｼｮｸ ｾｯﾄ ｻﾌﾞﾙｰﾁﾝ(FM)  **

    public void STENV() {
        if (work.soundWork.getcurrentChip() == 4) {
            STENVopm();
            return;
        }

        KEYOFF(false);

        byte a = (byte) (0x80 + work.pg.channelNumber);
        byte e = 0xf;
        byte b = 4;
        //ENVLP:

        if (CheckCh3SpecialMode()) {
            if ((work.pg.useSlot & 1) != 0) PSGOUT(a, e);
            a += 4;
            if ((work.pg.useSlot & 4) != 0) PSGOUT(a, e);
            a += 4;
            if ((work.pg.useSlot & 2) != 0) PSGOUT(a, e);
            a += 4;
            if ((work.pg.useSlot & 8) != 0) PSGOUT(a, e);
        } else {
            do {
                PSGOUT(a, e);// ﾘﾘｰｽ(RR) ｶｯﾄ ﾉ ｼｮﾘ
                a += 4;
                b--;
            } while (b != 0);
        }

        // ﾜｰｸ ｶﾗ ｵﾝｼｮｸ ﾅﾝﾊﾞｰ ｦ ｴﾙ
        //STENV0:
        int hl = work.pg.instrumentNumber * 25;// HL=*25
        //hl += Work.mData[Work.soundWork.OTODAT].dat + Work.mData[Work.soundWork.OTODAT + 1].dat * 0x100 + 1;// HL ﾊ ｵﾝｼｮｸﾃﾞｰﾀ ｶｸﾉｳ ｱﾄﾞﾚｽ
        //hl += Work.soundWork.MUSNUM;
        hl++;//音色数を格納している為いっこずらす


        //KUMA:tlの保存
        if (work.isDotNET && work.getHeader().CarrierCorrection) {
            work.pg.v_tl[0] = work.fmVoiceAtMusData[hl + 4 + 0];
            work.pg.v_tl[1] = work.fmVoiceAtMusData[hl + 4 + 1];
            work.pg.v_tl[2] = work.fmVoiceAtMusData[hl + 4 + 2];
            work.pg.v_tl[3] = work.fmVoiceAtMusData[hl + 4 + 3];
        }


        //STENV1:
        byte d = 0x30;// START=PORT 30H
        d += (byte) work.pg.channelNumber;// PLUS CHANNEL No.
        //STENV2:
        byte c = 6;// 6 PARAMATER(Det/Mul, Total, KS/AR, DR, SR, SL/RR)
        do {
            if (CheckCh3SpecialMode()) {
                if ((work.pg.useSlot & 1) != 0) PSGOUT(d, work.fmVoiceAtMusData[hl]);
                hl++;
                d += 4;// SKIP BLANK PORT
                if ((work.pg.useSlot & 4) != 0) PSGOUT(d, work.fmVoiceAtMusData[hl]);
                hl++;
                d += 4;// SKIP BLANK PORT
                if ((work.pg.useSlot & 2) != 0) PSGOUT(d, work.fmVoiceAtMusData[hl]);
                hl++;
                d += 4;// SKIP BLANK PORT
                if ((work.pg.useSlot & 8) != 0) PSGOUT(d, work.fmVoiceAtMusData[hl]);
                hl++;
                d += 4;// SKIP BLANK PORT
            } else {
                b = 4;// 4 OPERATER
                //STENV3:
                do {
                    // GET DATA
                    //PSGOUT(d, Work.mData[hl++].dat);
                    PSGOUT(d, work.fmVoiceAtMusData[hl++]);
                    d += 4;// SKIP BLANK PORT
                    b--;
                } while (b != 0);
            }

            c--;

        } while (c != 0);

        //e = Work.mData[hl].dat;// GET FEEDBACK/ALGORIZM
        e = work.fmVoiceAtMusData[hl];// GET FEEDBACK/ALGORIZM
        // GET ALGORIZM
        work.pg.algo = e & 0x07;// STORE ALGORIZM
        // GET ALGO SET ADDRES
        d = (byte) (0xb0 + work.pg.channelNumber);// CH PLUS
        PSGOUT(d, e);
    }

    private boolean CheckCh3SpecialMode() {
        return (work.soundWork.getFMPORT() == 0
                && work.pg.channelNumber == 2
                && work.soundWork.Ch3SpMode(work.soundWork.getcurrentChip()));
    }

    public void STENVopm() {
        KEYOFF(false);

        byte a = (byte) (0xe0 + work.pg.channelNumber);
        byte e = 0xf;
        byte b = 4;

        do {
            PSGOUT(a, e);// ﾘﾘｰｽ(RR) ｶｯﾄ ﾉ ｼｮﾘ
            a += 8;
            b--;
        } while (b != 0);

        // ﾜｰｸ ｶﾗ ｵﾝｼｮｸ ﾅﾝﾊﾞｰ ｦ ｴﾙ
        //STENV0:
        int hl = work.pg.instrumentNumber * 25;// HL=*25
        hl++;//音色数を格納している為いっこずらす


        //KUMA:tlの保存
        if (work.getHeader().CarrierCorrection) {
            work.pg.v_tl[0] = work.fmVoiceAtMusData[hl + 4 + 0];
            work.pg.v_tl[1] = work.fmVoiceAtMusData[hl + 4 + 1];
            work.pg.v_tl[2] = work.fmVoiceAtMusData[hl + 4 + 2];
            work.pg.v_tl[3] = work.fmVoiceAtMusData[hl + 4 + 3];
        }


        //STENV1:
        byte d = 0x40;// START =Adr:40H
        d += (byte) work.pg.channelNumber;// PLUS CHANNEL No.
        //STENV2:
        byte c = 6;// 6 PARAMATER(Det/Mul, Total, KS/AR, DR, SR, SL/RR)
        do {
            b = 4;// 4 OPERATER
            do {
                // GET DATA
                PSGOUT(d, work.fmVoiceAtMusData[hl++]);
                d += 8;// SKIP BLANK PORT
                b--;
            } while (b != 0);
            c--;
        } while (c != 0);

        e = work.fmVoiceAtMusData[hl];// GET FEEDBACK/ALGORIZM
        a = (byte) (((work.pg.panValue & 1) << 1)
                | ((work.pg.panValue & 2) >> 1));//bit並び入れ替え
        e |= (byte) (a << 6);// pan

        // GET ALGORIZM
        work.pg.algo = e & 0x07;// STORE ALGORIZM
        work.pg.feedback = (e & 0x38) >> 3;
        // GET ALGO SET ADDRES
        d = (byte) (0x20 + work.pg.channelNumber);// CH PLUS
        PSGOUT(d, e);
    }

    // **ﾎﾞﾘｭｰﾑ ｾｯﾄ**

    public void VOLPST() {
        DummyOUT();
        if (work.soundWork.getPCMFLG() != 0) {
            PCMVOL();
            return;
        }

        if (work.soundWork.getDRMF1() != 0) {
            VOLDRM();
            return;
        }

        work.pg.volume = work.pg.mData[work.hl++].dat;
        STVOL();
    }

    public void PCMVOL() {
        byte e = (byte) work.pg.mData[work.hl++].dat;
        if (work.soundWork.getPVMODE() != 0) {
            work.pg.volReg = e;
            return;
        }
        work.pg.volume = e;
    }

    public void VOLDRM() {
        byte a = (byte) work.pg.mData[work.hl++].dat;

        if (work.isDotNET) {
            if ((a & 0x80) != 0) {
                VOLDRMn((byte) (a & 0x1f));
                return;
            }
        }

        work.pg.volume = a;
        DVOLSET();
        //VOLDR1:
        byte b = 6;
        int de = 0;// Work.soundWork.DRMVOL;
        //VOLDR2:
        do {
            a = (byte) (work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][de] & 0b1100_0000);
            a |= (byte) work.pg.mData[work.hl++].dat;
            work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][de++] = a;
            if (work.soundWork.getcurrentChip() < 2)
                PSGOUT((byte) (0x18 - b + 6), a);
            else
                PCMOUT((byte) 1, (byte) (0x8 - b + 6), a);
            b--;
        } while (b != 0);
    }

    public void VOLDRMn(byte a) {
        int inst = work.pg.instrumentNumber;
        for (int i = 0; i < 6; i++) {
            if (((inst >> i) & 1) != 0) {
                byte b = (byte) ((work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][i] & 0b1100_0000) | a);
                work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][i] = b;
                if (work.soundWork.getcurrentChip() < 2)
                    PSGOUT((byte) (0x18 + i), b);
                else
                    PCMOUT((byte) 1, (byte) (0x8 + i), b);
            }
        }
    }

    /** SET TOTAL RHYTHM VOL */
    public void DVOLSET() {
        byte d = 0x11;
        byte a = (byte) work.pg.volume;
        a &= 0b0011_1111;
        a = (byte) (work.soundWork.getTOTALV() * 5 + a);
        if (a >= 64) {
            a = 0;
        }
        //DV2:
        if (work.soundWork.getcurrentChip() < 2)
            PSGOUT(d, a);
        else
            PCMOUT((byte) 1, (byte) 0x1, a);
    }

    /** ﾃﾞﾁｭｰﾝ ｾｯﾄ */
    public void FRQ_DF() {

        DummyOUT();
        work.pg.beforeCode = 0; // DETUNE ﾉ ﾊﾞｱｲﾊ BEFORE CODE ｦ CLEAR
        int de = (short) (work.pg.mData[work.hl].dat + work.pg.mData[work.hl + 1].dat * 0x100);
        work.hl += 2;
        byte a = (byte) work.pg.mData[work.hl++].dat;
        if (a != 0) {
            de += work.pg.detune;
        }
        // FD2:
        work.pg.detune = de;
        if (work.soundWork.getPCMFLG() == 0) {
            return;
        }

        if (work.cd.getcurrentPageNo() != work.pg.getpageNo()) return;

        short hl = (short) work.soundWork.getDELT_N()[work.soundWork.getcurrentChip()];
        hl += (short) de;
        if (work.soundWork.getcurrentChip() < 2) {
            PCMOUT((byte) 0x09, (byte) hl);
            PCMOUT((byte) 0x0a, (byte) (hl >> 8));
        } else {
            PCMOUT((byte) 0, (byte) 0x19, (byte) hl);
            PCMOUT((byte) 0, (byte) 0x1a, (byte) (hl >> 8));
        }
    }

    /** SET Q COMMAND */
    public void SETQ() {
        work.pg.quantize = work.pg.mData[work.hl++].dat;
        work.pg.enblKeyOff = (work.pg.quantize != 255);

        List<Object> args = new ArrayList<>();
        args.add(work.pg.quantize);
        DummyOUT(enmMMLType.Gatetime, args);

    }

    /** SOFT LFO SET(RESET) */
    public void LFOON() {
        byte a = (byte) work.pg.mData[work.hl++].dat; // GET SUB COMMAND
        if (a != 0) {
            a--; // LFOTBL;
            LFOTBL[a].run();
            LFODummySend();
            return;
        }
        SETDEL();
        SETCO();
        SETVCT();
        SETPEK();
        work.pg.lfoflg = true; // SET LFO FLAG
        LFODummySend();
    }

    private void LFODummySend() {
        List<Object> args = new ArrayList<>();
        args.add(work.pg.lfoflg);
        DummyOUT(enmMMLType.LfoSwitch, args);

        args = new ArrayList<>();
        args.add(work.pg.lfoDelay); // 8bit unsigned
        args.add(work.pg.lfoCounter); // 8bit unsigned
        args.add(work.pg.lfoDelta); // 16bit signed
        args.add(work.pg.lfoPeak); // 8bit unsigned
        DummyOUT(enmMMLType.Lfo, args);
    }

    public void SETDEL() {
        byte a = (byte) work.pg.mData[work.hl++].dat;
        work.pg.lfoDelay = a;
        work.pg.lfoDelayWork = a;
    }

    public void SETCO() {
        byte a = (byte) work.pg.mData[work.hl++].dat;
        work.pg.lfoCounter = a;
        work.pg.lfoCounterWork = a;
    }

    public void SETVCT() {
        byte e = (byte) work.pg.mData[work.hl++].dat;
        byte d = (byte) work.pg.mData[work.hl++].dat;

        work.pg.lfoDelta = e + d * 0x100;
        work.pg.lfoDeltaWork = e + d * 0x100;
    }

    public void SETPEK() {
        byte a = (byte) work.pg.mData[work.hl++].dat;

        work.pg.lfoPeak = a; // SET PEAK LEVEL
        a >>= 1;
        work.pg.lfoPeakWork = a;
    }

    public void LFOOFF() {
        work.pg.lfoflg = false;// RESET LFO
    }

    public void LFOON2() {
        work.pg.lfoflg = true;// LFOON
    }

    public void SETVC2() {
        SETVCT();
        LFORST();
    }

    public void TLLFOorSSGTremolo() {
        if (work.soundWork.getSSGF1() == 0) {
            TLLFO();
            return;
        }

        SSGTremolo();
    }

    public void TLLFO() {

        byte a = (byte) work.pg.mData[work.hl++].dat;
        if (a == 0) {
            work.pg.tlLfoflg = false;
            return;
        }

        //TLL2:
        work.pg.TLlfoSlot = a;
        work.pg.tlLfoflg = true;
        a = (byte) work.pg.mData[work.hl++].dat;
        work.pg.fnum = a;
        work.pg.bfnum2 = 0;
        work.pg.TLlfo = a;
    }

    public void SSGTremolo() {
        byte a = (byte) work.pg.mData[work.hl++].dat;
        if (a == 0) {
            work.pg.setSSGTremoloFlg(false);
            work.pg.setSSGTremoloVol(0);
            return;
        }

        work.pg.setSSGTremoloFlg(true);
        work.pg.setSSGTremoloVol(0);
    }

    // **ﾘﾋﾟｰﾄ ｽﾀｰﾄ ｾｯﾄ**

    public void REPSTF() {
        byte e = (byte) work.pg.mData[work.hl++].dat;
        byte d = (byte) work.pg.mData[work.hl++].dat;//DE as REWRITE ADR OFFSET +1

        int hl = (int) work.hl;
        hl -= 2;
        hl += e + d * 0x100;
        byte a = (byte) work.pg.mData[hl--].dat;
        work.pg.mData[hl].dat = a;
    }

    // **ﾘﾋﾟｰﾄ ｴﾝﾄﾞ ｾｯﾄ(FM) **

    public void REPENF() {
        byte a = (byte) (work.pg.mData[work.hl].dat - (byte) 1);// DEC REPEAT Co.
        work.pg.mData[work.hl].dat--;

        if (a == 0) {
            //REPENF2();
            work.pg.mData[work.hl].dat = work.pg.mData[work.hl + 1].dat;
            work.hl += 4;
            return;
        }

        work.hl += 2;

        byte e = (byte) work.pg.mData[work.hl++].dat;
        byte d = (byte) work.pg.mData[work.hl--].dat;

        //a &= a;
        work.hl -= (int) (e + d * 0x100);
    }

    // **SE DETUNE SET SUB ROUTINE**

    public void MDSET() {
        TO_EFC();

        if (work.isDotNET) {
            for (int bc = 0; bc < 4; bc++) {
                byte l = (byte) work.pg.mData[work.hl++].dat;
                byte m = (byte) work.pg.mData[work.hl++].dat;
                work.soundWork.DETDAT[work.soundWork.getcurrentChip()][bc] = (short) (l + m * 0x100);
            }
        } else {
            for (int bc = 0; bc < 4; bc++) {
                byte a = (byte) work.pg.mData[work.hl++].dat;
                work.soundWork.DETDAT[work.soundWork.getcurrentChip()][bc] = a;
            }
        }
    }

    // **CHANGE SE MODE**

    public void TO_NML() {
        int timer = work.currentTimer;
        work.soundWork.PLSET1_VAL[work.soundWork.getcurrentChip()] = 0x38;
        TNML2((byte) 0x3a);
        work.currentTimer = timer;
    }

    public void TO_EFC() {
        int timer = work.currentTimer;
        work.soundWork.PLSET1_VAL[work.soundWork.getcurrentChip()] = 0x78;
        TNML2((byte) 0x7a);
        work.currentTimer = timer;
    }

    public void TNML2(byte a) {
        work.soundWork.PLSET2_VAL[work.soundWork.getcurrentChip()] = a;
        if (work.soundWork.getcurrentChip() != 4) PSGOUT((byte) 0x27, a);
        else PSGOUT((byte) 0x14, a);
    }

    // **STEREO**

    public void STEREO() {
        if (work.soundWork.getDRMF1() != 0) { // goto STE2;
            if (work.soundWork.getPCMFLG() != 0) {
                work.soundWork.getPCMLR()[work.soundWork.getcurrentChip()] = work.pg.mData[work.hl++].dat;
//                return;
            } else {
//STER2:
                byte a = (byte) work.pg.mData[work.hl++].dat;
                byte c = (byte) (((a >> 2) & 0x3f) | (a << 6));
                byte d = work.soundWork.PALDAT[work.soundWork.getFMPORT() + work.pg.channelNumber * 10 + work.pg.getpageNo()];
                d = (byte) ((d & 0b0011_1111) | c);
                work.soundWork.PALDAT[work.soundWork.getFMPORT() + work.pg.channelNumber * 10 + work.pg.getpageNo()] = d;
                a = (byte) (0x0B4 + work.pg.channelNumber);

                if (CheckCh3SpecialMode() || work.cd.getcurrentPageNo() == work.pg.getpageNo())
                    PSGOUT(a, d);
            }
//            return;
        } else {
//STE2:
            byte dat = (byte) work.pg.mData[work.hl++].dat;
            byte c = dat;
            dat &= 0b0000_1111;
            byte a = work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][dat];
            a = (byte) (((c << 2) & 0b1100_0000) | (a & 0b0001_1111));
            work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][dat] = a;

            if (work.cd.getcurrentPageNo() == work.pg.getpageNo()) {
                if (work.soundWork.getcurrentChip() < 2)
                    PSGOUT((byte) (dat + 0x18), a);
                else
                    PCMOUT((byte) 1, (byte) (dat + 0x8), a);
            }
        }
    }

    public void STEREO_AMD98() {
        byte a, c, d;
        if (work.soundWork.getDRMF1() != 0) {
            STEREO_AMD98_RHYTHM();
            return;
        }

        if (work.soundWork.getPCMFLG() != 0) {
            STEREO_AMD98_ADPCM();
            return;
        }

        a = (byte) work.pg.mData[work.hl++].dat;
        work.pg.panValue = a;
        work.pg.panMode = a;

        if (a < 4) { // goto STE012;

            DummyOUT();

            if (work.soundWork.getcurrentChip() != 4 && work.soundWork.getSSGF1() == 0) {
                //既存処理
                c = (byte) (((a >> 2) & 0x3f) | (a << 6));//右ローテート2回(左6回のほうがC#的にはシンプル)
                d = work.soundWork.PALDAT[work.soundWork.getFMPORT() + work.pg.channelNumber * 10 + work.pg.getpageNo()];
                d = (byte) ((d & 0b0011_1111) | c);
                work.soundWork.PALDAT[work.soundWork.getFMPORT() + work.pg.channelNumber * 10 + work.pg.getpageNo()] = d;
                a = (byte) (0x0B4 + work.pg.channelNumber);

                if (CheckCh3SpecialMode() || work.cd.getcurrentPageNo() == work.pg.getpageNo())
                    PSGOUT(a, d);
            } else if (work.soundWork.getSSGF1() != 0) {
                //pan & phrst は volume出力時に共に更新されるのでここで音源に送信する必要は無い
            } else {
                work.pg.panValue = (byte) a;
                a = (byte) (((a & 1) << 1) | ((a & 2) >> 1));
                c = (byte) ((a << 6) | (work.pg.feedback << 3) | work.pg.algo);
                a = (byte) (0x20 + work.pg.channelNumber);

                if (work.cd.getcurrentPageNo() == work.pg.getpageNo())
                    PSGOUT(a, c);
            }

            work.pg.panEnable = 0;//パーン禁止
//            return;
        } else {
//STE012:
            work.pg.panEnable |= 1;//パーン許可
            //Work.pg.panMode = a;
            work.pg.panCounterWork = (byte) work.pg.mData[work.hl].dat;
            work.pg.panCounter = (byte) work.pg.mData[work.hl].dat;
            work.hl++;
            switch (a) {
            case 4:
                work.pg.panValue = a = 2; //LEFT index
                break;
            case 5:
                work.pg.panValue = a = 0; //RIGHT index
                break;
            default:
                work.pg.panValue = a = 1; //CENTER index
                break;
            }

            a = autoPantable[a];

            if (work.soundWork.getcurrentChip() != 4) {
                if (work.soundWork.getSSGF1() != 0) {
                    //pan & phrst は volume出力時に共に更新されるのでここで音源に送信する必要は無い
                } else {
                    c = (byte) (a << 6);
                    d = SoundWork.PALDAT[work.soundWork.getFMPORT() + work.pg.channelNumber * 10 + work.pg.getpageNo()];
                    d = (byte) ((d & 0b0011_1111) | c);
                    SoundWork.PALDAT[work.soundWork.getFMPORT() + work.pg.channelNumber * 10 + work.pg.getpageNo()] = d;
                    a = (byte) (0x0B4 + work.pg.channelNumber);

                    if (CheckCh3SpecialMode() || work.cd.getcurrentPageNo() == work.pg.getpageNo())
                        PSGOUT(a, d);
                }
            } else {
                //Work.pg.panValue = (byte)a;
                a = (byte) (((a & 1) << 1) | ((a & 2) >> 1));
                c = (byte) ((a << 6) | (work.pg.feedback << 3) | work.pg.algo);
                a = (byte) (0x20 + work.pg.channelNumber);

                if (work.cd.getcurrentPageNo() == work.pg.getpageNo())
                    PSGOUT(a, c);
            }
        }
    }

    public void STEREO_AMD98_RHYTHM() {
        DummyOUT();

        // bit0～3 rythmType RTHCSB
        // bit4～7 パン(1:右, 2:左, 3:中央 4:右オート 5:左オート 6:ランダム)を指定する。
        byte a = (byte) (work.pg.mData[work.hl].dat >> 4);
        byte b = (byte) (work.pg.mData[work.hl].dat & 0xf);
        work.hl++;
        byte c;
        if (b >= 6) return;

        if (a < 4) {
            //既存処理
            c = work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][b];
            a = (byte) (((a << 6) & 0b1100_0000) | (c & 0b0001_1111));
            work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][b] = a;
            //if (Work.cd.getcurrentPageNo() == Work.pg.getpageNo())
            {
                if (work.soundWork.getcurrentChip() < 2)
                    PSGOUT((byte) (b + 0x18), a);
                else
                    PCMOUT((byte) 1, (byte) (b + 0x8), a);
            }
            work.soundWork.DrmPanEnable[work.soundWork.getcurrentChip()][b] = 0;//パーン禁止
            return;
        }

        work.soundWork.DrmPanEnable[work.soundWork.getcurrentChip()][b] |= 1;//パーン許可
        work.soundWork.DrmPanMode[work.soundWork.getcurrentChip()][b] = a;
        work.soundWork.DrmPanCounterWork[work.soundWork.getcurrentChip()][b] = (byte) work.pg.mData[work.hl].dat;
        work.soundWork.DrmPanCounter[work.soundWork.getcurrentChip()][b] = (byte) work.pg.mData[work.hl].dat;
        work.hl++;

        switch (a) {
        case 4:
            work.soundWork.DrmPanValue[work.soundWork.getcurrentChip()][b] = a = 2;
            break;
        case 5:
            work.soundWork.DrmPanValue[work.soundWork.getcurrentChip()][b] = a = 0;
            break;
        default:
            work.soundWork.DrmPanValue[work.soundWork.getcurrentChip()][b] = a = 1;
            break;
        }

        a = (byte) autoPantable[a];
        c = work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][b];
        a = (byte) (((a << 6) & 0b1100_0000) | (c & 0b0001_1111));
        work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][b] = a;
        //if (Work.cd.getcurrentPageNo() == Work.pg.getpageNo())
        if (work.cd.getcurrentPageNo() == work.pg.getpageNo()) {
            if (work.soundWork.getcurrentChip() < 2)
                PSGOUT((byte) (b + 0x18), a);
            else
                PCMOUT((byte) 1, (byte) (b + 0x8), a);
        }
    }

    public void STEREO_AMD98_ADPCM() {
        DummyOUT();

        byte a = (byte) work.pg.mData[work.hl++].dat;

        if (a < 4) {
            //既存処理
            if (work.cd.getcurrentPageNo() == work.pg.getpageNo())
                work.soundWork.getPCMLR()[work.soundWork.getcurrentChip()] = a;
            work.pg.panValue = a;
            work.pg.panEnable = 0;//パーン禁止
            return;
        }

        work.pg.panEnable |= 1;//パーン許可
        work.pg.panMode = a;
        work.pg.panCounterWork = (byte) work.pg.mData[work.hl].dat;
        work.pg.panCounter = (byte) work.pg.mData[work.hl].dat;
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

        a = (byte) autoPantable[a];
        work.soundWork.getPCMLR()[work.soundWork.getcurrentChip()] = a;

        if (work.soundWork.getcurrentChip() < 2) {
            if (work.cd.getcurrentPageNo() == work.pg.getpageNo())
                PCMOUT((byte) 0x01, (byte) (a << 6));
        } else {
            if (work.cd.getcurrentPageNo() == work.pg.getpageNo())
                PCMOUT((byte) 0, (byte) 0x11, (byte) (a << 6));
        }
    }

    public void restoreSTEREO_AMD98() {
        byte a, c, d;
        if (work.soundWork.getDRMF1() != 0) {
            restoreSTEREO_AMD98_RHYTHM();
            return;
        }

        if (work.soundWork.getPCMFLG() != 0) {
            restoreSTEREO_AMD98_ADPCM();
            return;
        }

        a = work.pg.panMode;
        if (a < 4) { // goto STE012;

            DummyOUT();

            // 既存処理
            c = (byte) (((a >> 2) & 0x3f) | (a << 6)); // 右ローテート2回(左6回のほうがC#的にはシンプル)
            d = SoundWork.PALDAT[work.soundWork.getFMPORT() + work.pg.channelNumber * 10 + work.pg.getpageNo()];
            d = (byte) ((d & 0b0011_1111) | c);
            SoundWork.PALDAT[work.soundWork.getFMPORT() + work.pg.channelNumber * 10 + work.pg.getpageNo()] = d;
            a = (byte) (0x0B4 + work.pg.channelNumber);
            PSGOUT(a, d);
            work.pg.panEnable = 0; // パーン禁止
//            return;
        } else {
//STE012:
            work.pg.panEnable |= 1; // パーン許可
            work.pg.panCounterWork = work.pg.panCounter;
            switch (a) {
            case 4:
                work.pg.panValue = a = 2; //LEFT index
                break;
            case 5:
                work.pg.panValue = a = 0; //RIGHT index
                break;
            default:
                work.pg.panValue = a = 1; //CENTER index
                break;
            }

            a = (byte) autoPantable[a];

            c = (byte) (a << 6);
            d = SoundWork.PALDAT[work.soundWork.getFMPORT() + work.pg.channelNumber * 10 + work.pg.getpageNo()];
            d = (byte) ((d & 0b0011_1111) | c);
            SoundWork.PALDAT[work.soundWork.getFMPORT() + work.pg.channelNumber * 10 + work.pg.getpageNo()] = d;
            a = (byte) (0x0B4 + work.pg.channelNumber);

            PSGOUT(a, d);
        }
    }

    public void restoreSTEREO_AMD98_RHYTHM() {
        DummyOUT();

        for (byte b = 0; b < 6; b++) {
            byte a = work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][b];
            if (work.soundWork.DrmPanMode[work.soundWork.getcurrentChip()][b] < 4) {
                if (work.cd.getcurrentPageNo() == work.pg.getpageNo()) {
                    if (work.soundWork.getcurrentChip() < 2)
                        PSGOUT((byte) (b + 0x18), a);
                    else
                        PCMOUT((byte) 1, (byte) (b + 0x8), a);
                }
                work.soundWork.DrmPanEnable[work.soundWork.getcurrentChip()][b] = 0;//パーン禁止
                continue;
            }

            work.soundWork.DrmPanEnable[work.soundWork.getcurrentChip()][b] |= 1;//パーン許可
            work.soundWork.DrmPanCounterWork[work.soundWork.getcurrentChip()][b] = work.soundWork.DrmPanCounter[work.soundWork.getcurrentChip()][b];

            switch (a) {
            case 4:
                work.soundWork.DrmPanValue[work.soundWork.getcurrentChip()][b] = a = 2;
                break;
            case 5:
                work.soundWork.DrmPanValue[work.soundWork.getcurrentChip()][b] = a = 0;
                break;
            default:
                work.soundWork.DrmPanValue[work.soundWork.getcurrentChip()][b] = a = 1;
                break;
            }

            a = (byte) autoPantable[a];
            byte c = work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][b];
            a = (byte) (((a << 6) & 0b1100_0000) | (c & 0b0001_1111));
            work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][b] = a;
            if (work.cd.getcurrentPageNo() == work.pg.getpageNo()) {
                if (work.soundWork.getcurrentChip() < 2)
                    PSGOUT((byte) (b + 0x18), a);
                else
                    PCMOUT((byte) 1, (byte) (b + 0x8), a);
            }
        }

        // bit0～3 rythmType RTHCSB
        // bit4～7 パン(1:右, 2:左, 3:中央 4:右オート 5:左オート 6:ランダム)を指定する。
    }

    public void restoreSTEREO_AMD98_ADPCM() {
        DummyOUT();
        byte a = (byte) work.pg.panMode;
        if (a < 4) {
            work.pg.panEnable = 0;//パーン禁止
            a = work.pg.panValue;
            work.soundWork.getPCMLR()[work.soundWork.getcurrentChip()] = a;
            if (work.soundWork.getcurrentChip() < 2)
                PCMOUT((byte) 0x01, (byte) (a << 6));
            else
                PCMOUT((byte) 0, (byte) 0x11, (byte) (a << 6));
            return;
        }

        work.pg.panEnable |= 1;//パーン許可
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

        a = (byte) autoPantable[a];
        work.soundWork.getPCMLR()[work.soundWork.getcurrentChip()] = a;
        if (work.soundWork.getcurrentChip() < 2)
            PCMOUT((byte) 0x01, (byte) (a << 6));
        else
            PCMOUT((byte) 0, (byte) 0x11, (byte) (a << 6));
    }

    public void PANNING() {
        if (work.soundWork.getDRMF1() != 0) {
            PANNING_RHYTHM();
            return;
        }

        if ((work.pg.panEnable & 1) == 0) return;
        if ((--work.pg.panCounterWork) != 0) return;

        work.pg.panCounterWork = work.pg.panCounter;//; カウンター再設定

        if (work.pg.panMode == 4 || work.pg.panMode == 5) {
            //LEFT / RIGHT
            byte ah = work.pg.panValue;
            ah++;
            if (ah == autoPantable.length) {
                ah = 0;
            }
            work.pg.panValue = ah;//ah : 0～
        } else {
            //RANDOM
            short ax;
            do {
                ax = work.soundWork.getRANDUM();
                ax *= 5;
                ax += 0x1993;
                work.soundWork.setRANDUM(ax);
                ax &= 0x0300;
            } while (ax == 0);
            work.pg.panValue = (byte) (ax >> 8);//ah : 1～3
        }

        byte a, c, d;
        a = (work.pg.panMode == 4 || work.pg.panMode == 5) ? autoPantable[work.pg.panValue] : work.pg.panValue;

        List<Object> args = new ArrayList<>();
        args.add((int) a);
        DummyOUT(enmMMLType.Pan, args);

        if (work.soundWork.getcurrentChip() != 4) {
            if (work.soundWork.getSSGF1() != 0) {
                return;
            } else if (work.soundWork.getPCMFLG() == 0) {
                c = (byte) (((a >> 2) & 0x3f) | (a << 6)); // 右ローテート2回(左6回のほうがC#的にはシンプル)
                d = work.soundWork.PALDAT[work.soundWork.getFMPORT() + work.pg.channelNumber * 10 + work.pg.getpageNo()];
                d = (byte) ((d & 0b0011_1111) | c);
                work.soundWork.PALDAT[work.soundWork.getFMPORT() + work.pg.channelNumber * 10 + work.pg.getpageNo()] = d;
                a = (byte) (0x0B4 + work.pg.channelNumber);

                if (CheckCh3SpecialMode() || work.cd.getcurrentPageNo() == work.pg.getpageNo())
                    PSGOUT(a, d);

                return;
            }
        } else {
            a = (byte) (((a & 1) << 1) | ((a & 2) >> 1));
            c = (byte) ((a << 6) | (work.pg.feedback << 3) | work.pg.algo);
            a = (byte) (0x20 + work.pg.channelNumber);

            if (work.cd.getcurrentPageNo() == work.pg.getpageNo()) {
                PSGOUT(a, c);
                //Console.WriteLine("{0:x}", c&0xc0);
            }

            return;
        }

        work.soundWork.getPCMLR()[work.soundWork.getcurrentChip()] = a;
        c = (byte) ((a << 6) & 0xc0);

        if (work.soundWork.getcurrentChip() < 2) {
            if (work.cd.getcurrentPageNo() == work.pg.getpageNo())
                PCMOUT((byte) 0x01, c);
        } else {
            if (work.cd.getcurrentPageNo() == work.pg.getpageNo())
                PCMOUT((byte) 0, (byte) 0x11, c);
        }
    }

    public void PANNING_RHYTHM() {
        for (int n = 0; n < 6; n++) {
            if ((work.soundWork.DrmPanEnable[work.soundWork.getcurrentChip()][n] & 1) == 0) continue;
            if ((--work.soundWork.DrmPanCounterWork[work.soundWork.getcurrentChip()][n]) != 0) continue;

            work.soundWork.DrmPanCounterWork[work.soundWork.getcurrentChip()][n] = work.soundWork.DrmPanCounter[work.soundWork.getcurrentChip()][n];//; カウンター再設定

            if (work.soundWork.DrmPanMode[work.soundWork.getcurrentChip()][n] == 4 || work.soundWork.DrmPanMode[work.soundWork.getcurrentChip()][n] == 5) {
                //LEFT / RIGHT
                byte ah = work.soundWork.DrmPanValue[work.soundWork.getcurrentChip()][n];
                ah++;
                if (ah == autoPantable.length) {
                    ah = 0;
                }
                work.soundWork.DrmPanValue[work.soundWork.getcurrentChip()][n] = ah;//ah : 0～
            } else {
                //RANDOM
                short ax;
                do {
                    ax = work.soundWork.getRANDUM();
                    ax *= 5;
                    ax += 0x1993;
                    work.soundWork.setRANDUM(ax);
                    ax &= 0x0300;
                } while (ax == 0);
                work.soundWork.DrmPanValue[work.soundWork.getcurrentChip()][n] = (byte) (ax >> 8);//ah : 1～3
            }

            byte a = (work.soundWork.DrmPanMode[work.soundWork.getcurrentChip()][n] == 4 || work.soundWork.DrmPanMode[work.soundWork.getcurrentChip()][n] == 5)
                    ? autoPantable[work.soundWork.DrmPanValue[work.soundWork.getcurrentChip()][n]]
                    : work.soundWork.DrmPanValue[work.soundWork.getcurrentChip()][n];

            byte c = work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][n];
            a = (byte) (((work.soundWork.DrmPanValue[work.soundWork.getcurrentChip()][n] << 6) & 0b1100_0000) | (c & 0b0001_1111));
            work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][n] = a;

            if (work.cd.getcurrentPageNo() == work.pg.getpageNo())
                if (work.cd.getcurrentPageNo() == work.pg.getpageNo()) {
                    if (work.soundWork.getcurrentChip() < 2)
                        PSGOUT((byte) (n + 0x18), a);
                    else
                        PCMOUT((byte) 1, (byte) (n + 0x8), a);
                }

        }
    }

    // **ﾌﾗｸﾞｾｯﾄ**

    public void FLGSET() {
        byte a = (byte) work.pg.mData[work.hl++].dat;
        work.soundWork.setFLGADR(a);
    }

    // **   WRITE REG   **

    public void W_REG() {
        byte d = (byte) work.pg.mData[work.hl++].dat;
        byte e = (byte) work.pg.mData[work.hl++].dat;
        PSGOUT(d, e);
    }

    public void MW_REG() {
        byte c = (byte) work.pg.mData[work.hl++].dat;
        byte p = (byte) work.pg.mData[work.hl++].dat;
        byte d = (byte) work.pg.mData[work.hl++].dat;
        byte e = (byte) work.pg.mData[work.hl++].dat;
        PSGOUT(c, p, d, e);
    }

    public void CH3SP() {
        byte c = (byte) work.pg.mData[work.hl++].dat;
        if (c == 0x00) {
            byte sw = (byte) work.pg.mData[work.hl++].dat;
            if (work.soundWork.getcurrentChip() != 4) // OPM以外であれば効果音モードを設定
            {
                if (sw == 0) TO_NML();
                else TO_EFC();
            }
        } else {
            byte slot = (byte) work.pg.mData[work.hl++].dat;
            work.pg.useSlot = slot;
        }
    }

    // **VOLUME UP & DOWN**

    public void VOLUPF() {
        List<Object> args;
        //LinePos lp;

        if (work.soundWork.getDRMF1() != 0) {
            byte n = (byte) work.pg.mData[work.hl++].dat;

            if (work.isDotNET) {
                if ((n & 0x80) != 0) {
                    VOLUPF_Rhythm(n);
                    return;
                }
            }

            work.pg.volume += n;
            //パラメータ表示向け
            args = new ArrayList<>();
            args.add(work.pg.volume);
            DummyOUT(enmMMLType.Volume, args);

            DVOLSET();
            return;
        }

        if (work.cd.getFMVolMode() != 3) {
            work.pg.volume += work.pg.mData[work.hl++].dat;
        } else {
            byte n = (byte) (-(byte) work.pg.mData[work.hl++].dat);
            for (int i = 0; i < 4; i++) {
                work.pg.getTLDirectTable()[i] += n;
            }
        }

        if (work.soundWork.getPCMFLG() != 0) {
            args = new ArrayList<>();
            args.add(work.pg.volume - 4);
            DummyOUT(enmMMLType.Volume, args);
            return;
        }

        STVOL();
    }

    public void VOLUPF_Rhythm(byte a) {
        int inst = work.pg.instrumentNumber;
        for (int i = 0; i < 6; i++) {
            if (((inst >> i) & 1) != 0) {
                byte b = (byte) ((byte) ((a & 0x3f) | ((a & 0x40) != 0 ? 0xc0 : 0)) + (work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][i] & 0x3f));

                //パラメータ表示向け
                List<Object> args = new ArrayList<>();
                args.add((int) b);
                DummyOUT(enmMMLType.Volume, args);

                b = (byte) ((work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][i] & 0b1100_0000) | b);
                work.soundWork.DRMVOL[work.soundWork.getcurrentChip()][i] = b;
                //if (Work.cd.getcurrentPageNo() == Work.pg.getpageNo())
                {
                    if (work.soundWork.getcurrentChip() < 2)
                        PSGOUT((byte) (i + 0x18), b);
                    else
                        PCMOUT((byte) 1, (byte) (i + 0x8), b);
                }
            }
        }
    }

    // **HARD LFO SET**

    public void HLFOON() {
        byte a = (byte) work.pg.mData[work.hl++].dat;// FREQ CONT
        a |= 0b0000_1000;
        PSGOUT((byte) 0x22, a);

        byte c = (byte) work.pg.mData[work.hl++].dat;// PMS
        c = (byte) (c | (work.pg.mData[work.hl++].dat << 4));// AMS+PMS
        int de = work.soundWork.getFMPORT() + work.pg.channelNumber * 10 + work.pg.getpageNo(); // PALDAT;
        a = (byte) ((SoundWork.PALDAT[de] & 0b1100_0000) | c);
        SoundWork.PALDAT[de] = a;
        PSGOUT((byte) (0xb4 + work.pg.channelNumber), a);
    }

    public void TIE() {
        work.pg.keyoffflg = false;
    }

    // **ﾘﾋﾟｰﾄ ｽｷｯﾌﾟ**

    public void RSKIP() {
        byte e = (byte) work.pg.mData[work.hl++].dat;
        byte d = (byte) work.pg.mData[work.hl++].dat;

        int hl = work.hl;
        hl -= 2;
        hl += e + d * 0x100;

        byte a = (byte) work.pg.mData[hl].dat;
        a--;// LOOP ｶｳﾝﾀ = 1 ?
        if (a == 0) {
            hl += 4;// HL = JUMP ADR
            work.hl = hl;
            return;
        }

    }

    public void SECPRC() {
        byte a = (byte) work.pg.mData[work.hl++].dat;
        a &= 0xf; // A=COMMAND No.(0-F)

        if (work.soundWork.getSSGF1() == 0 || (work.soundWork.getSSGF1() != 0 && !work.isDotNET))
            FMCOM2[a].run();
        else
            PSGCOM2[a].run(); // kuma: DotNET専用テーブルです
    }

    public void NTMEAN() {
    }

    // **PCM VMODE CHANGE**

    public void PVMCHG() {
        byte a = (byte) work.pg.mData[work.hl++].dat;
        work.soundWork.setPVMODE(a);
    }

    // **ﾘﾊﾞｰﾌﾞ**

    public void REVERVE() {
        byte a = (byte) work.pg.mData[work.hl++].dat;
        work.pg.reverbVol = a;
        //RV1:
        work.pg.reverbFlg = true;
    }

    public void REVSW() {
        byte a = (byte) work.pg.mData[work.hl++].dat;
        if (a != 0) {
            //goto RV1;
            work.pg.reverbFlg = true;
            return;
        }
        work.pg.reverbFlg = false;

        //if (Work.idx >= 3 && Work.idx <= 5) return;
        if (work.soundWork.getSSGF1() != 0) return;

        STVOL();
    }

    public void REVMOD() {
        byte a = (byte) work.pg.mData[work.hl++].dat;
        if (a != 0) {
            work.pg.reverbMode = true;
            return;
        }
        //RM2:
        work.pg.reverbMode = false;
    }

    /** PSG ｵﾝｼｮｸｾｯﾄ */
    public void OTOSSG() {
        DummyOUT();

        byte a = (byte) work.pg.mData[work.hl++].dat;

        // OTOCAL
        int ptr = 0; // SSGDAT;
        ptr = a * 6;
        //ENVPST();
        for (int i = 0; i < 6; i++) {
            work.pg.softEnvelopeParam[i] = work.soundWork.SSGDAT[ptr + i];
        }
        work.pg.volume = work.pg.volume | 0b1001_0000;
    }

    public void OTOSET() {
        byte a = (byte) work.pg.mData[work.hl++].dat;

        // OTOCAL
        int ptr = 0; // SSGDAT;
        ptr = a * 6;

        for (int i = 0; i < 6; i++) {
            work.soundWork.SSGDAT[ptr + i] = (byte) work.pg.mData[work.hl++].dat;
        }
    }

    /** ｴﾝﾍﾞﾛｰﾌﾟ ﾊﾟﾗﾒｰﾀ ｾｯﾄ */
    public void ENVPST() {
        for (int i = 0; i < 6; i++) {
            work.pg.softEnvelopeParam[i] = work.pg.mData[work.hl++].dat;
        }
        work.pg.volume = work.pg.volume | 0b1001_0000; // ｴﾝﾍﾞﾌﾗｸﾞ ｱﾀｯｸﾌﾗｸﾞ ｾｯﾄ
    }

    public void ENVPSTex() {
        for (int i = 0; i < 6; i++) {
            work.pg.softEnvelopeParam[i] = work.pg.mData[work.hl++].dat;
        }
        work.pg.softEnvelopeFlag = 0b1001_0000; // ｴﾝﾍﾞﾌﾗｸﾞ ｱﾀｯｸﾌﾗｸﾞ ｾｯﾄ
    }

    // **PSG VOLUME**

    public void PSGVOL() {
        DummyOUT();
        work.pg.hardEnveFlg = false;
        byte e = (byte) (work.pg.volume & 0b1111_0000);
        byte c = (byte) work.pg.mData[work.hl].dat;
        PV1(c, e);
    }

    public void PV1(byte c, byte e) {
        byte a = work.soundWork.getTOTALV();
        a += c;
        if (a >= 16) { // goto PV2;
            a = 0;
        }
//PV2:
        a |= e;
        work.hl++;
        work.pg.volume = a;
    }

    /** MIX PORT CONTROL */
    public void NOISE() {
        work.pg.setbackupMIXPort((byte) work.pg.mData[work.hl++].dat);
        if (work.pg.getpageNo() != work.cd.getcurrentPageNo()) return;

        tNOISE();
    }

    public void restoreNOISE() {
        tNOISE();
    }

    public void tNOISE() {
        byte c = work.pg.getbackupMIXPort();
        byte b = (byte) work.pg.channelNumber;
        byte e = work.soundWork.PREGBF[work.soundWork.getcurrentChip()][5];
        b >>= 1;
        b++;
        byte d = b;
        byte a = 0b0111_1011;
        //NOISE1:
        do {
            a = (byte) ((a << 1) | (a >> 7));
            b--;
        } while (b != 0);
        a &= e;
        e = a;
        a = c;
        b = d;
        a = (byte) ((a >> 1) | (a << 7));
        //NOISE2:
        do {
            a = (byte) ((a << 1) | (a >> 7));
            b--;
        } while (b != 0);
        a |= e;
        d = 7;
        e = a;
        PSGOUT(d, e);
        work.soundWork.PREGBF[work.soundWork.getcurrentChip()][5] = e;
    }


    // **   ﾉｲｽﾞ ｼｭｳﾊｽｳ   **

    public void NOISEW() {
        work.pg.setbackupNoiseFrq((byte) work.pg.mData[work.hl++].dat);
        if (work.pg.getpageNo() != work.cd.getcurrentPageNo()) return;

        tNOISEW();

    }

    public void restoreNOISEW() {
        tNOISEW();
    }

    public void tNOISEW() {
        byte e = work.pg.getbackupNoiseFrq();
        PSGOUT((byte) 6, e);
        work.soundWork.PREGBF[work.soundWork.getcurrentChip()][4] = e;
    }

    // **SSG VOLUME UP & DOWN**

    public void VOLUPS() {
        byte d = (byte) work.pg.mData[work.hl++].dat;
        if (!work.pg.hardEnveFlg) {
            byte a = (byte) work.pg.volume;
            byte e = a;
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

            List<Object> args = new ArrayList<>();
            args.add((int) d);
            DummyOUT(enmMMLType.Volume, args);
        }
    }

    //** LFO ﾙｰﾁﾝ */
    public void PLLFO() {
        if (!CheckCh3SpecialMode() && work.pg.getpageNo() != work.cd.getcurrentPageNo()) return;

        // FOR FM & SSG LFO
        if (!work.pg.lfoflg) {
            return;
        }
        int hl = work.pg.dataAddressWork;
        hl--;
        byte a = (byte) work.pg.mData[hl].dat;
        if ((a & 0xff) == 0xf0) {
            return; // ｲｾﾞﾝ ﾉ ﾃﾞｰﾀ ｶﾞ '&' ﾅﾗ RET
        }
        if (!work.pg.lfoContFlg) {
            // **LFO INITIARIZE   **
            LFORST();
            LFORST2();
            work.pg.lfoCounterWork = work.pg.lfoCounter;
            work.pg.lfoContFlg = true;// SET CONTINUE FLAG
        }
        //CTLFO:
        if (work.pg.lfoDelayWork == 0)//delayが完了していたら次の処理へ
        {
            CTLFO1();
            return;
        }
        work.pg.lfoDelayWork--;//delayのカウントダウン
    }

    public void CTLFO1() {
        work.pg.lfoCounterWork--;// ｶｳﾝﾀ
        if (work.pg.lfoCounterWork != 0) {
            return;
        }
        work.pg.lfoCounterWork = work.pg.lfoCounter;//ｶｳﾝﾀ ｻｲ ｾｯﾃｲ
        if (work.pg.lfoPeakWork == 0)//  GET PEAK LEVEL COUNTER(P.L.C)
        {
            work.pg.lfoDeltaWork = -work.pg.lfoDeltaWork;// WAVE ﾊﾝﾃﾝ
            work.pg.lfoPeakWork = work.pg.lfoPeak;//  P.L.C ｻｲ ｾｯﾃｲ
        }
        //PLLFO1:
        work.pg.lfoPeakWork--;// P.L.C.-1
        int hl = work.pg.lfoDeltaWork;

        List<Object> args = new ArrayList<>();
        args.add(hl);
        MakeDummyCrntMmlDatum(enmMMLType.Lfo, args); // TODO LFODelta

        PLS2(hl);
    }

    public void PLS2(int hl) {
        if (work.soundWork.getPCMFLG() == 0) {
            PLSKI2(hl);
            return;
        }

        hl += (int) work.soundWork.getDELT_N()[work.soundWork.getcurrentChip()];
        work.soundWork.getDELT_N()[work.soundWork.getcurrentChip()] = hl;

        PCMOUT((byte) 0x09, (byte) hl);
        PCMOUT((byte) 0x0a, (byte) (hl >> 8));
    }

    public void PLSKI2(int hl) {
        if (work.soundWork.getSSGF1() != 0 && work.pg.getSSGTremoloFlg()) {
            work.pg.addSSGTremoloVol(hl);
            //Console.WriteLine(Work.pg.SSGTremoloVol);
            return;
        }

        if (work.soundWork.getSSGF1() == 0) {
            //KUMA:FMの時はリミットチェック処理

            int[] num = new int[1];
            short dlt = (short) (short) hl;
            //Console.Write("b:{0} num:{1:x} -> +{2}", blk, num, dlt);

            num[0] = work.pg.fnum & 0x7ff;
            int[] blk = new int[] {work.pg.fnum >> 11};
            num[0] += dlt;
            GetFnum(/*ref*/ blk, /*ref*/ num);
            //Console.WriteLine(" -> b:{0} num:{1:x}",blk,num);
            hl = (blk[0] << 11) | num[0];
        } else {
            //KUMA:SSGの時は既存の処理

            int de = work.pg.fnum;// GET FNUM1
            // GET B/FNUM2
            hl += de;//  HL= NEW F-NUMBER
            hl = (short) hl;
        }

        work.pg.fnum = hl;// SET NEW F-NUM1
        // SET NEW F-NUM2

        if (work.soundWork.getSSGF1() == 0) {
            LFOP5(hl);
            return;
        }

        // ---FOR SSG LFO---
        byte a = (byte) work.pg.beforeCode;// GET KEY CODE&OCTAVE
        a >>= 4;
        if (a != 0)//  OCTAVE=1?
        {
            byte b = a;
            //SNUMGETL:
            do {
                hl >>= 1;
                b--;
            } while (b != 0);
        }
        //SSLFO2:
        byte e = (byte) hl;
        byte d = (byte) work.pg.channelNumber;
        PSGOUT(d, e);
        d++;
        e = (byte) (hl >> 8);
        PSGOUT(d, e);
    }

    private void GetFnum(/*ref*/ int[] blk, /*ref*/ int[] num) {
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
        num[0] = Math.min(Math.max(num[0], 0), 0x7ff);

    }

    // ---FOR FM LFO---

    public void LFOP5(int hl) {
        if (work.pg.tlLfoflg) {
            LFOP6(hl);
            return;
        }

        if (work.soundWork.getcurrentChip() == 4) {
            PLLFO2opm(hl);
            return;
        }

        //if ((Work.pg.channelNumber & 0x02) == 0)//  CH=3?
        if ((work.soundWork.getcurrentCh() / 10) != 2)//  CH=3?
        {
            PLLFO2(hl);// NOT CH3 THEN PLLFO2
            return;
        }

        if (!work.soundWork.Ch3SpMode(work.soundWork.getcurrentChip())) {
            PLLFO2(hl);// NOT SE MODE
            return;
        }

        work.soundWork.setNEWFNM(hl);
        //LFOP4:
        hl = 0;
        int iy = 0;
        byte b = 4;
        //LFOP3:
        do {
            //int fnum = Work.soundWork.NEWFNM + Work.soundWork.DETDAT[Work.soundWork.getcurrentChip()][hl++];
            int[] blk = new int[] {(work.soundWork.getNEWFNM() >> 11) & 0x7};
            int[] num = new int[] {(work.soundWork.getNEWFNM() & 0x7ff) + work.soundWork.DETDAT[work.soundWork.getcurrentChip()][hl++]};
            GetFnum(/*ref*/ blk, /*ref*/ num);
            int fnum = (blk[0] << 11) | num[0];

            byte d = work.soundWork.OP_SEL[iy++];
            byte e = (byte) (fnum >> 8);
            PSGOUT(d, e);

            d -= 4;
            e = (byte) fnum;
            PSGOUT(d, e);

            b--;
        } while (b != 0);
    }

    public void PLLFO2(int hl) {
        byte d = (byte) 0xa4;//  PORT A4H
        d += (byte) work.pg.channelNumber;
        byte e = (byte) (hl >> 8);
        PSGOUT(d, e);

        d -= 4;
        e = (byte) hl;// F-NUMBER1 DATA
        PSGOUT(d, e);
    }

    public void PLLFO2opm(int hl) {
        byte oct = (byte) (((hl & 0x3800) >> 11));
        byte note = (byte) (((hl & 0x7ff) >> 6));
        note--;
        if (note == 0xff) {
            oct--;
            note = 11;
        }
        note = (byte) (note < 3 ? note : (note < 6 ? (note + 1) : (note < 9 ? (note + 2) : (note + 3))));

        byte e = (byte) ((oct << 4) | note);// oct:bit6-4 note :bit3-0
        byte d = 0x28;// KC のアドレス
        d += (byte) work.pg.channelNumber;
        PSGOUT(d, e);
        //Log.writeLine(LogLevel.TRACE, String.Format("PLLFO2opm:d:{0:x02} e:{1:x02}", d, e));
        d += 8;//KF のアドレス
        e = (byte) ((hl & 0x3f) << 2);// KF (bit:7-2)
        PSGOUT(d, e);
        //Log.writeLine(LogLevel.TRACE, String.Format("PLLFO2opm:d:{0:x02} e:{1:x02}", d, e));

    }

    public void LFOP6(int hl) {
        byte c = work.pg.TLlfoSlot;//.soundWork.LFOP6_VAL;

        byte d = 0x40;
        d += (byte) work.pg.channelNumber;
        byte e = (byte) hl;

        if ((c & 0x01) != 0) {
            PSGOUT(d, e);
            d += 4;
        }
        if ((c & 0x04) != 0) {
            PSGOUT(d, e);
            d += 4;
        }
        if ((c & 0x02) != 0) {
            PSGOUT(d, e);
            d += 4;
        }
        if ((c & 0x08) == 0) {
            return;
        }
        PSGOUT(d, e);
        d += 4;
    }

    public void prcLFO() {
        if (!CheckCh3SpecialMode() && work.pg.getpageNo() != work.cd.getcurrentPageNo()) return;

        // for fm & ssg lfo
        if (!work.pg.lfoflg) {
            return;
        }

        int hl = work.pg.dataAddressWork;
        hl--;
        byte a = (byte) work.pg.mData[hl].dat;
        if ((a & 0xff) == 0xf0) {
            return; // ｲｾﾞﾝ ﾉ ﾃﾞｰﾀ ｶﾞ '&' ﾅﾗ ret
        }

        if (!work.pg.lfoContFlg) {
            // lfo initialize
            LFORST();
            LFORST2();
            work.pg.lfoCounterWork = work.pg.lfoCounter;
            work.pg.lfoContFlg = true;// set continue flag
        }

        // CTLFO:
        if (work.pg.lfoDelayWork == 0) // delay が完了していたら次の処理へ
        {
            prcCTLFO1();
            return;
        }
        work.pg.lfoDelayWork--; // delay のカウントダウン
    }

    public void prcCTLFO1() {
        work.pg.lfoCounterWork--; // ｶｳﾝﾀ
        if (work.pg.lfoCounterWork != 0) {
            return;
        }

        work.pg.lfoCounterWork = work.pg.lfoCounter; // ｶｳﾝﾀ ｻｲ ｾｯﾃｲ
        if (work.pg.lfoPeakWork == 0) // GET PEAK LEVEL COUNTER(P.L.C)
        {
            work.pg.lfoDeltaWork = -work.pg.lfoDeltaWork; // WAVE ﾊﾝﾃﾝ
            work.pg.lfoPeakWork = work.pg.lfoPeak; // P.L.C ｻｲ ｾｯﾃｲ
        }

        //PLLFO1:
        work.pg.lfoPeakWork--;// P.L.C.-1
        int hl = work.pg.lfoDeltaWork;
        prcPLS2(hl);
    }

    public void prcPLS2(int hl) {
        if (work.soundWork.getPCMFLG() == 0) {
            prcPLSKI2(hl);
            return;
        }

        hl += (int) work.soundWork.getDELT_N()[work.soundWork.getcurrentChip()];
        work.soundWork.getDELT_N()[work.soundWork.getcurrentChip()] = hl;

    }

    public void prcPLSKI2(int hl) {
        if (work.soundWork.getSSGF1() != 0 && work.pg.getSSGTremoloFlg()) {
            work.pg.addSSGTremoloVol(hl);
            //Console.WriteLine(Work.pg.SSGTremoloVol);
            return;
        }

        if (work.soundWork.getSSGF1() == 0) {
            //KUMA:FMの時はリミットチェック処理

            int[] num = new int[1];
            short dlt = (short) (short) hl;
            //Console.Write("b:{0} num:{1:x} -> +{2}", blk, num, dlt);

            if (work.soundWork.getcurrentChip() != 4) {
                num[0] = work.pg.fnum & 0x7ff;
                int[] blk = new int[] {work.pg.fnum >> 11};
                num[0] += dlt;
                GetFnum(/*ref*/ blk, /*ref*/ num);
                //Console.WriteLine(" -> b:{0} num:{1:x}",blk,num);
                hl = (blk[0] << 11) | num[0];
            } else {
                num[0] = AddDetuneToFNumopm((short) work.pg.fnum, dlt);
                hl = num[0];
            }
        } else {
            // KUMA: SSGの時は既存の処理

            int de = work.pg.fnum; // GET FNUM1
            // GET B/FNUM2
            hl += de; //  HL= NEW F-NUMBER
            hl = (short) hl;
        }

        work.pg.fnum = hl; // SET NEW F-NUM1
        // SET NEW F-NUM2
    }

    private void PORTAON() {
        work.pg.portaFlg = true;
        work.pg.portaContFlg = false;
        work.pg.portaWorkClock = 0;

        work.pg.portaStNote = (byte) work.pg.mData[work.hl++].dat;
        work.pg.portaEdNote = (byte) work.pg.mData[work.hl++].dat;
        work.pg.portaTotalClock = (byte) work.pg.mData[work.hl++].dat;
        work.pg.portaTotalClock += ((byte) work.pg.mData[work.hl++].dat) << 8;
    }

    public void prcPortament() {
        if (!CheckCh3SpecialMode() && work.pg.getpageNo() != work.cd.getcurrentPageNo()) return;

        if (!work.pg.portaFlg) {
            return;
        }

        int hl = work.pg.dataAddressWork;
        hl--;
        byte a = (byte) work.pg.mData[hl].dat;
        if ((a & 0xff) == 0xf0) {
            return; // ｲｾﾞﾝ ﾉ ﾃﾞｰﾀ ｶﾞ '&' ﾅﾗ RET
        }

        if (!work.pg.portaContFlg) {
            //portament initialize
            work.pg.portaWorkClock = 0;
            work.pg.portaContFlg = true;
        }

        prcCTPRO1();
    }

    public void prcCTPRO1() {
        if (work.soundWork.getPCMFLG() != 0) {
            prcCTPRO1_PCM();
            return;
        }

        if (work.soundWork.getSSGF1() != 0) {
            prcCTPRO1_SSG();
            return;
        }

        prcCTPRO1_FM();
    }

    public void prcCTPRO1_FM() {
        if (work.pg.portaTotalClock == 0) return;

        int stOct = work.pg.portaStNote >> 4;
        int stNote = work.pg.portaStNote & 0xf;
        int edOct = work.pg.portaEdNote >> 4;
        int edNote = work.pg.portaEdNote & 0xf;
        boolean isNeg = work.pg.portaEdNote < work.pg.portaStNote;
        int noteDisatance = Math.abs((stOct * 12 + stNote) - (edOct * 12 + edNote));

        //音程変化範囲 * 経過クロック / ポルタメント総クロック = 開始音程からどの程度音程が変化したか
        double noteDelta = noteDisatance * work.pg.portaWorkClock / (double) work.pg.portaTotalClock;

        //整数部と小数部に分離
        int iNoteDelta = (int) noteDelta;
        iNoteDelta = isNeg ? -iNoteDelta : iNoteDelta;
        noteDelta -= iNoteDelta;

        //音程からfnumを取得
        int a = iNoteDelta + (stNote + stOct * 12);
        int b = (a + 12) % 12;
        int n = (a + (isNeg ? 11 : 1)) % 12;

        int bsOct;
        int nxOct;
        bsOct = a / 12;
        nxOct = (a + (isNeg ? -1 : 1)) / 12;

        int bsFnum = 0, nxFnum = 0;
        if (work.soundWork.getcurrentChip() < 2) {
            bsFnum = work.soundWork.FNUMB[0][b] + bsOct * work.soundWork.FNUMB[0][0];
            nxFnum = work.soundWork.FNUMB[0][n] + nxOct * work.soundWork.FNUMB[0][0];
        } else if (work.soundWork.getcurrentChip() < 4) {
            bsFnum = work.soundWork.FNUMB[1][b] + bsOct * work.soundWork.FNUMB[1][0];
            nxFnum = work.soundWork.FNUMB[1][n] + nxOct * work.soundWork.FNUMB[1][0];
        } else {
            bsFnum = work.soundWork.FNUMBopm[0][b] + bsOct * 0x300;
            nxFnum = work.soundWork.FNUMBopm[0][n] + nxOct * 0x300;
        }

        //小数部からfnumを算出
        double d = (double) (isNeg ? ((bsFnum - nxFnum) * (1.0 - (noteDelta - (int) noteDelta))) : ((nxFnum - bsFnum) * noteDelta));
        //d = isNeg ? -d : d;
        d += isNeg ? nxFnum : bsFnum;

        if (work.pg.portaWorkClock == 0) work.pg.portaBeforeFNum = isNeg ? (int) bsFnum : (int) d;
        int delta = (int) d - (int) work.pg.portaBeforeFNum;
        work.pg.portaBeforeFNum = (int) d;

        //Console.WriteLine("{0} {1} {2}", isNeg, d, nxFnum);

        int[] num = new int[1];
        short dlt = (short) delta;

        if (work.soundWork.getcurrentChip() != 4) {
            num[0] = work.pg.fnum & 0x7ff;
            int[] blk = new int[] {work.pg.fnum >> 11};
            num[0] += dlt;
            GetFnum(/*ref*/ blk, /*ref*/ num);
            delta = (blk[0] << 11) | num[0];
        } else {
            num[0] = AddDetuneToFNumopm((short) work.pg.fnum, dlt);
            delta = num[0];
        }

        work.pg.fnum = delta;
        work.pg.portaWorkClock++;
        if (work.pg.portaWorkClock == work.pg.portaTotalClock) {
            work.pg.portaFlg = false;
        }
    }

    public void prcCTPRO1_SSG() {
        if (work.pg.portaTotalClock == 0) return;

        int stOct = work.pg.portaStNote >> 4;
        int stNote = work.pg.portaStNote & 0xf;
        int edOct = work.pg.portaEdNote >> 4;
        int edNote = work.pg.portaEdNote & 0xf;
        boolean isNeg = work.pg.portaEdNote < work.pg.portaStNote;
        int noteDisatance = Math.abs((stOct * 12 + stNote) - (edOct * 12 + edNote));

        //音程変化範囲 * 経過クロック / ポルタメント総クロック = 開始音程からどの程度音程が変化したか
        double noteDelta = noteDisatance * work.pg.portaWorkClock / (double) work.pg.portaTotalClock;

        //整数部と小数部に分離
        int iNoteDelta = (int) noteDelta;
        iNoteDelta = isNeg ? -iNoteDelta : iNoteDelta;
        noteDelta -= iNoteDelta;

        //音程からfnumを取得
        int a = iNoteDelta + (stNote + stOct * 12);
        int b = (a + 12) % 12;
        int n = (a + (isNeg ? 11 : 1)) % 12;

        int bsOct;
        int nxOct;
        bsOct = a / 12;
        nxOct = (a + (isNeg ? -1 : 1)) / 12;

        int bsFnum = 0, nxFnum = 0;
        if (work.soundWork.getcurrentChip() < 2) {
            bsFnum = work.soundWork.SNUMB[0][b];
            if (nxOct - bsOct >= 0) nxFnum = work.soundWork.SNUMB[0][n] >> (nxOct - bsOct);
            else nxFnum = work.soundWork.SNUMB[0][n] << (bsOct - nxOct);
        } else if (work.soundWork.getcurrentChip() < 4) {
            bsFnum = work.soundWork.SNUMB[1][b];
            if (nxOct - bsOct >= 0) nxFnum = work.soundWork.SNUMB[1][n] >> (nxOct - bsOct);
            else nxFnum = work.soundWork.SNUMB[1][n] << (bsOct - nxOct);
        }

        //小数部からfnumを算出
        double d = (double) (isNeg ? ((bsFnum - nxFnum) * (1.0 - (noteDelta - (int) noteDelta))) : ((nxFnum - bsFnum) * noteDelta));
        //d = isNeg ? -d : d;
        d += isNeg ? nxFnum : bsFnum;

        if (work.pg.portaWorkClock == 0) work.pg.portaBeforeFNum = isNeg ? (int) bsFnum : (int) d;
        int delta = (int) d - (int) work.pg.portaBeforeFNum;
        work.pg.portaBeforeFNum = (int) d;

        //Console.WriteLine("{0} {1} {2}", isNeg, d, nxFnum);

        delta += work.pg.fnum;
        work.pg.beforeCode = bsOct << 4;

        work.pg.fnum = delta;
        work.pg.portaWorkClock++;
        if (work.pg.portaWorkClock == work.pg.portaTotalClock) {
            work.pg.portaFlg = false;
        }

    }

    public void prcCTPRO1_PCM() {
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

        //音程変化範囲 * 経過クロック / ポルタメント総クロック = 開始音程からどの程度音程が変化したか
        double noteDelta = noteDisatance * work.pg.portaWorkClock / (double) work.pg.portaTotalClock;

        //整数部と小数部に分離
        int iNoteDelta = (int) noteDelta;
        noteDelta -= iNoteDelta;
        iNoteDelta = isNeg ? -iNoteDelta : iNoteDelta;

        //音程からfnumを取得
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
        if (work.soundWork.getcurrentChip() < 2) {
            bsFnum = work.soundWork.PCMNMB[0][b] >> bsOct;
            nxFnum = work.soundWork.PCMNMB[0][n] >> nxOct;
        } else if (work.soundWork.getcurrentChip() < 4) {
            bsFnum = work.soundWork.PCMNMB[1][b] >> bsOct;
            nxFnum = work.soundWork.PCMNMB[1][n] >> nxOct;
        }

        //小数部からfnumを算出
        double d = (double) (isNeg ? ((bsFnum - nxFnum) * (1.0 - noteDelta)) : ((nxFnum - bsFnum) * noteDelta));
        //d = isNeg ? -d : d;
        d += isNeg ? nxFnum : bsFnum;

        if (work.pg.portaWorkClock == 0) work.pg.portaBeforeFNum = isNeg ? (int) bsFnum : (int) d;
        int delta = (int) d - (int) work.pg.portaBeforeFNum;
        work.pg.portaBeforeFNum = (int) d;

        //Console.WriteLine("{0} {1} {2} {3} {4}", isNeg, d, nxFnum,bsOct,nxOct);

        delta += work.pg.fnum;
        work.pg.beforeCode = bsOct << 4;

        work.pg.fnum = delta;
        work.pg.portaWorkClock++;
        if (work.pg.portaWorkClock == work.pg.portaTotalClock) {
            work.pg.portaFlg = false;
        }

    }

    public void prcWriteFnum() {
        int hl;

        if (work.soundWork.getPCMFLG() != 0) {
            hl = work.soundWork.getDELT_N()[work.soundWork.getcurrentChip()] + work.pg.fnum;
            if (work.soundWork.getcurrentChip() < 2) {
                PCMOUT((byte) 0x09, (byte) hl);
                PCMOUT((byte) 0x0a, (byte) (hl >> 8));
            } else {
                PCMOUT((byte) 0, (byte) 0x19, (byte) hl);
                PCMOUT((byte) 0, (byte) 0x1a, (byte) (hl >> 8));
            }
            return;
        }

        if (work.soundWork.getSSGF1() != 0) {
            // ---FOR SSG LFO---
            hl = work.pg.fnum;
            byte a = (byte) work.pg.beforeCode;// GET KEY CODE&OCTAVE
            a >>= 4;
            if (a != 0)//  OCTAVE=1?
            {
                byte b = a;
                do {
                    hl >>= 1;
                    b--;
                } while (b != 0);
            }
            byte e = (byte) hl;
            byte d = (byte) work.pg.channelNumber;
            PSGOUT(d, e);
            d++;
            e = (byte) (hl >> 8);

            if (work.SSGExtend) {
                if (work.pg.getSSGWfNum() != 0) {
                    e |= (byte) (work.pg.getSSGWfNum() << 4);
                }
            }

            PSGOUT(d, e);
            return;
        }

        LFOP5(work.pg.fnum);
    }


    public void prcSoftEnvelope() {
        if ((work.pg.softEnvelopeFlag & 0x80) == 0) return;
        SOFENVex();
    }


    //SSG:
    // **SSG ｵﾝｹﾞﾝｴﾝｿｳ ﾙｰﾁﾝ**

    public void SSGSUB() {
        //Work.cd = Work.soundWork.chData[Work.idx];
        //Work.pg = Work.cd.PGDAT.get(0);
        work.hl = work.pg.dataAddressWork;

        work.pg.lengthCounter = (byte) (work.pg.lengthCounter - 1);
        if (work.pg.lengthCounter == 0) {
            SSSUB7();
            return;
        }

        if (work.pg.lengthCounter != work.pg.quantize) {
            SSSUB0();
            return;
        }

        if (work.pg.mData[work.pg.dataAddressWork].dat != 0xfd) { // COUNT OVER? goto SSUB0;
            SSSUBA(); // to release
//            return; // ret
        } else {
//SSUB0:
            work.pg.keyoffflg = false; // set tie flag (たぶんキーオフをリセット)
            SSSUB0();
        }
    }

    public void SSSUB0() {
        if (work.pg.getpageNo() != work.cd.getcurrentPageNo())
            return;

        if ((work.pg.volume & 0x80) == 0) { // envelope check
            return;
        }

        SOFENV();

        if (work.pg.getSSGTremoloFlg()) {
            work.A_Reg = (byte) Math.max(Math.min((work.A_Reg + work.pg.getSSGTremoloVol()), 15), 0);
            //Console.WriteLine("{0}",Work.pg.SSGTremoloVol);
        }

        byte e = work.A_Reg;
        if (work.soundWork.getREADY() == 0) {
            e = 0;
        }
        if (work.soundWork.getKEY_FLAG() != 0xff) {
            byte d = (byte) work.pg.volReg;
            if (work.SSGExtend) e |= (byte) (work.pg.panValue << 6);
            if (work.pg.getpageNo() == work.cd.getcurrentPageNo()) PSGOUT(d, e);
        }
    }

    public void SSSUB7() {
        work.hl = work.pg.dataAddressWork;
        if (work.pg.mData[work.hl].dat == 0xfd) // COUNT OVER?
        {
            //SSUB1:
            work.pg.keyoffflg = false; // SET TIE FLAG(たぶんキーオフをリセット)
            work.hl++;
            SSSUBB();
            return;
        }
        //SSSUBE:
        work.pg.keyoffflg = true;
        SSSUBB();
    }

    /** KEY OFF ｼﾞ ﾉ RR ｼｮﾘ */
    public void SSSUBA() {
        // HARD ENV.KEY OFF
        if (work.pg.hardEnveFlg) {
            if (work.pg.getpageNo() == work.cd.getcurrentPageNo())
                PSGOUT((byte) work.pg.volReg, (byte) 0); // SSG KEY OFF
        }

        // SOFT ENV.KEY OFF

        if (work.pg.reverbFlg) {
            work.pg.keyoffflg = false;
            SSSUB0();
            return;
        }

        //SSUBAC:
        if ((work.pg.volume & 0x80) == 0) {
            SSSUB3((byte) 0); // ﾘﾘｰｽ ｼﾞｬﾅｹﾚﾊﾞ SSSUB3
            return;
        }
        work.pg.volume &= 0b1000_1111;// STATE 4 (ﾘﾘｰｽ)
        SOFEV9();
        SSSUB3(work.A_Reg);
    }

    public void SSSUBB() {
        work.crntMmlDatum = work.pg.mData[work.hl];

        byte a;
        boolean nrFlg = false;
        do {
            a = (byte) work.pg.mData[work.hl].dat;
            while (a == 0) { // CHECK END MARK
                work.pg.loopEndFlg = true;
                // HL=DATA TOP ADD
                if (work.pg.dataTopAddress == -1 || nrFlg) {
                    if (nrFlg)
                        work.abnormalEnd = true;
                    SSGEND();
                    return;
                }
                work.hl = (int) work.pg.dataTopAddress;
                a = (byte) work.pg.mData[work.hl].dat;// GET FLAG & LENGTH
                work.pg.incloopCounter();
                //if (Work.pg.loopCounter > Work.nowLoopCounter) Work.nowLoopCounter = Work.pg.loopCounter;
                nrFlg = true;
            }

            //演奏情報退避
            work.crntMmlDatum = work.pg.mData[work.hl];

            //SSSUB1:
            //SSSUB2:
            a = (byte) work.pg.mData[work.hl++].dat;// INPUT FLAG &LENGTH
            if (a < 0xf0) break;
            // COMMAND OF PSG?
            //SSSUB8();
            a &= 0xf; // A=COMMAND No.(0-F)
            PSGCOM[a].run();
        } while (true);

        nrFlg = false;
        boolean carry = ((a & 0x80) != 0);
        a &= 0x7f; // CY=REST FLAG

        work.pg.lengthCounter = a;//  SET WAIT COUNTER
        //  ｷｭｳﾌ ﾅﾗ SSSUBA
        if (carry) {
            work.crntMmlDatum = work.pg.mData[work.hl - 1];
            SSSUBA();
            SETPT();
            return;
        }

        // **SET FINE TUNE & COARSE TUNE**

        //SSSUB6:
        a = (byte) work.pg.mData[work.hl++].dat;// LOAD OCT & KEY CODE
        byte b, c;
        if (!work.pg.keyoffflg) {
            c = a;
            b = (byte) work.pg.beforeCode;
            a -= b;
            if (a == 0) {
                SETPT();// IF NOW CODE=BEFORE CODE THEN SETPT
                return;
            }
            a = c;
            //goto SSSKIP0;// NON TIE
        }

        //SSSKIP0:
        work.pg.beforeCode = a;// STORE KEY CODE & OCTAVE

        //Mem.stack.Push(Z80.HL);


        if (work.cd.getkeyOnCh() != -1 && work.cd.getkeyOnCh() != work.pg.getpageNo()) { // || !Work.pg.keyoffflg) // KUMA:既に他のページが発音中の場合は処理しない
            SETPT(); // KUMA:演奏位置の更新
            return;
        }
        work.cd.setkeyOnCh(work.pg.getpageNo());
        if (work.cd.getcurrentPageNo() != work.pg.getpageNo()) {
            work.cd.setcurrentPageNo(work.pg.getpageNo());
            // 復帰処理
            restoreNOISE();
            restoreNOISEW();
            restoreHRDENV();
            restoreENVPOD();
            //Work.pg.lfoflg = false;
        }

        b = a;
        a &= 0b0000_1111;//  GET KEY CODE
        byte e = a;
        int hl = work.soundWork.SNUMB[work.soundWork.getcurrentChip() / 2][e];// GET FNUM2
        int de = work.pg.detune;// GET DETUNE DATA
        hl += de;//  DETUNE PLUS
        hl = (short) hl;
        work.pg.fnum = hl;// SAVE FOR LFO
        b >>= 4;//  OCTAVE=1?
        if (b != 0) {
            //SSSUB5:
            do {
                hl >>= 1;
                b--;
            } while (b != 0);// OCTAVE DATA ﾉ ｹｯﾃｲ
            //  1 ﾅﾗ SSSUB4 ﾍ
        }

        //KUMA:FNUMのセット
        //SSSUB4:
        e = (byte) hl;
        byte d = (byte) work.pg.channelNumber;
        PSGOUT(d, e);
        e = (byte) (hl >> 8);
        if (work.SSGExtend) {
            if (work.pg.getSSGWfNum() != 0) {
                e |= (byte) (work.pg.getSSGWfNum() << 4);
            }
        }
        d++;
        PSGOUT(d, e);

        if (!work.pg.keyoffflg) { // goto SSSUBF;
            SOFENV();
//            goto SSSUB9;
        } else {
//SSSUBF:
            // KEYON ｻﾚﾀﾄｷ ﾉ ｼｮﾘ

            if (work.pg.hardEnveFlg) {
                // HARD ENV. KEY ON
                if (work.soundWork.getKEY_FLAG() != 0xff) {
                    PSGOUT((byte) work.pg.volReg, (byte) 0x10);
                }
                PSGOUT((byte) 0x0d, (byte) work.pg.hardEnvelopValue);
            } else {
                // SOFT ENV.KEYON

                //SSSUBG:
                a = (byte) work.pg.volume;
                a &= 0b0000_1111;
                a |= 0b1001_0000;//  TO STATE 1 (ATTACK)
                work.pg.volume = a;

                a = (byte) work.pg.softEnvelopeParam[0];//  ENVE INIT
                work.pg.softEnvelopeCounter = a;//KUMA:ALがcounterの初期値として使用される
                work.pg.lfoContFlg = false;// RESET LFO CONTINE FLAG
                SOFEV7();

                //SSSUBH:
                c = (byte) work.pg.lfoPeak;
                c >>= 1;
                work.pg.lfoPeakWork = c;//  LFO PEAK LEVEL ｻｲ ｾｯﾃｲ
                work.pg.lfoDelayWork = work.pg.lfoDelay;//  LFO DELAY ﾉ ｻｲｾｯﾃｲ
            }
        }
//SSSUB9:
        //Z80.HL = Mem.stack.Pop();

        // VOLUME OUT PROCESS

        //
        //  ENTRY A: VOLUME DATA
        //
        SSSUB3(work.A_Reg);
    }

    /** SOFT ENVEROPE PROCESS */
    public void SOFENV() {
        if ((work.pg.volume & 0x10) != 0) { // CHECK ATTACK FLAG goto SOFEV2; // KUMA:decay flagのチェックへゴー

            byte a = (byte) work.pg.softEnvelopeCounter;  //KUMA:get counter
            byte d = (byte) work.pg.softEnvelopeParam[1];  //KUMA:get AR
            boolean carry = ((a & 0xff) + (d & 0xff) > 0xff); //KUMA:counter + AR が255を超えたか？
            a += d;
            if (carry) { // goto SOFEV1;
                a = (byte) 0xff; // KUMA: counterが 上限を突破したので, counter を 255 に修正
            }
//SOFEV1:
            //KUMA:counterとflagの更新
            work.pg.softEnvelopeCounter = a; //KUMA: counter = counter + AR(毎クロック,AR分だけcounterが増える)
            if ((a & 0xff) - 0xff != 0) {
                SOFEV7(); //KUMA:counterが255に達していないならSOFEV7へ
                return;
            }
            a = (byte) work.pg.volume;//KUMA:current volume & flagsを取得
            a ^= 0b0011_0000;//KUMA:attack flag:off  decay flag:on をxorで実現(上手い)
            work.pg.volume = a;// TO STATE 2 (DECAY) //KUMA:current volume & flagsを更新
            SOFEV7();
//            return;
//SOFEV2:
        } else if ((work.pg.volume & 0x20) == 0) { //KUMA: Check decay flag //goto SOFEV4;//KUMA:sustain flagのチェックへ
            byte a = (byte) work.pg.softEnvelopeCounter;// KUMA:get counter
            byte d = (byte) work.pg.softEnvelopeParam[2];// GET DECAY //KUMA:get DR
            byte e = (byte) work.pg.softEnvelopeParam[3];// GET SUSTAIN //KUMA:get SR
            boolean carry = ((a - d) < 0); //KUMA:counter = counter - DR 結果、counterが0未満の場合はSOFEV8へ
            a -= d;
            if (carry // goto SOFEV8; TODO recheck
                    || (a - e < 0)) { // KUMA:counter-SR は0以上の場合はSOFEV3へ goto SOFEV3;
//SOFEV8:
                a = e; //KUMA: counter = SR
            }
//SOFEV3:
            work.pg.softEnvelopeCounter = a; // KUMA:counter=counter-DR(毎クロック,DR分だけcounterが減る)
            if ((a - e) != 0) {
                SOFEV7(); // KUMA: counterがSRに到達していないならSOFEV7へ
                return;
            }
            a = (byte) work.pg.volume; // KUMA:current volume & flagsを取得
            a ^= 0b0110_0000; // KUMA:dcay flag:off  sustain flag:on
            work.pg.volume = a; // TO STATE 3 (SUSTAIN) //KUMA:current volume & flagsを更新
            SOFEV7();
//            return;
        } else {
//SOFEV4:
            if ((work.pg.volume & 0x40) == 0)//KUMA: Check sustain flag
            {
                SOFEV9();//KUMA:release 処理へ
                return;
            }
            byte a = (byte) work.pg.softEnvelopeCounter;// KUMA:get counter
            byte d = (byte) work.pg.softEnvelopeParam[4];// GET SUSTAIN LEVEL// KUMA:get SL
            boolean carry = ((a - d) < 0);//KUMA:counter = counter - SL 結果、counterが0以上の場合はSOFEV5へ
            a -= d;
            if (carry) { // goto SOFEV5;
                a = 0; // KUMA: counter=0
            }
//SOFEV5:
            work.pg.softEnvelopeCounter = a;//KUMA:counter=counter-SL(毎クロック,SL分だけcounterが減る)
            if (a != 0) {
                SOFEV7();
                return;
            }
            a = (byte) work.pg.volume;//KUMA:current volume & flagsを取得
            a &= 0b1000_1111;//KUMA:エンベロープで使用した進捗に関わるフラグをリセット
            work.pg.volume = a;// END OF ENVE //KUMA:KEYON中にSLにきて更にcounterが0になったらエンベロープ処理は終了する
            SOFEV7();
        }
    }

    public void SOFEV9() {
        byte a = (byte) work.pg.softEnvelopeCounter;//KUMA:get counter
        byte d = (byte) work.pg.softEnvelopeParam[5];// GET REREASE//KUMA:get RR
        boolean carry = ((a - d) < 0);//KUMA:RRでcounterを減算
        a -= d;
        if (carry) { // goto SOFEVA;
            a = 0;
        }
//SOFEVA:
        work.pg.softEnvelopeCounter = a; // KUMA:counterを更新
        SOFEV7();
    }

    /** VOLUME CALCURATE */
    public void SOFEV7() {
        byte e = (byte) work.pg.softEnvelopeCounter; // KUMA:get counter
        int hl = 0;
        byte a = (byte) work.pg.volume; // GET VOLUME
        a &= 0b0000_1111;
        a++;
        byte b = a; // 繰り返す回数 VOLUME+1回
//SOFEV6:
        do {
            hl += e;
            b--;
        } while (b != 0);
        a = (byte) (hl >> 8);//AにはVOLUME+1を最大値としたcounter/256の割合分の値が入る
        work.A_Reg = a;
        if (work.pg.keyoffflg) {
            return;
        }
        if (!work.pg.reverbFlg) {
            return;
        }
        a += (byte) work.pg.reverbVol;//.softEnvelopeParam[5];

        work.carry = ((a & 0x01) != 0);
        a >>= 1;
        work.A_Reg = a;
    }

    public void SOFENVex() {
        if ((work.pg.softEnvelopeFlag & 0x10) != 0) { // CHECK ATTACK FLAG goto SOFEV2; //KUMA:decay flagのチェックへゴー

            byte a = (byte) work.pg.softEnvelopeCounter;  //KUMA:get counter
            byte d = (byte) work.pg.softEnvelopeParam[1];  //KUMA:get AR
            boolean carry = ((a + d) > 0xff); //KUMA:counter + AR が255を超えたか？
            a += d;
            if (carry) { // goto SOFEV1;
                a = (byte) 0xff; //KUMA:counterが上限を突破したので,counterを255に修正
            }
//SOFEV1:
            //KUMA:counterとflagの更新
            work.pg.softEnvelopeCounter = a; //KUMA: counter = counter + AR(毎クロック,AR分だけcounterが増える)
            if ((a - 0xff) != 0) {
                SOFEV7ex(); //KUMA:counterが255に達していないならSOFEV7へ
                return;
            }
            a = (byte) work.pg.softEnvelopeFlag;//KUMA:current volume & flagsを取得
            a ^= 0b0011_0000;//KUMA:attack flag:off  decay flag:on をxorで実現(上手い)
            work.pg.softEnvelopeFlag = a;// TO STATE 2 (DECAY) //KUMA:current volume & flagsを更新
            SOFEV7ex();
//            return;
//SOFEV2:
        } else if ((work.pg.softEnvelopeFlag & 0x20) == 0) { //KUMA: Check decay flag goto SOFEV4;//KUMA:sustain flagのチェックへ
            byte a = (byte) work.pg.softEnvelopeCounter;// KUMA:get counter
            byte d = (byte) work.pg.softEnvelopeParam[2];// GET DECAY //KUMA:get DR
            byte e = (byte) work.pg.softEnvelopeParam[3];// GET SUSTAIN //KUMA:get SR
            boolean carry = ((a - d) < 0); //KUMA:counter = counter - DR 結果、counterが0未満の場合はSOFEV8へ
            a -= d;
            if (carry || // ) { goto SOFEV8;
                    (a - e < 0)) { //KUMA:counter-SR は0以上の場合はSOFEV3へ goto SOFEV3;
//SOFEV8:
                a = e;//KUMA: counter = SR
            }
//SOFEV3:
            work.pg.softEnvelopeCounter = a;//KUMA:counter=counter-DR(毎クロック,DR分だけcounterが減る)
            if ((a - e) != 0) {
                SOFEV7ex();//KUMA: counterがSRに到達していないならSOFEV7へ
                return;
            }
            a = (byte) work.pg.softEnvelopeFlag;//KUMA:current volume & flagsを取得
            a ^= 0b0110_0000;//KUMA:dcay flag:off  sustain flag:on
            work.pg.softEnvelopeFlag = a;// TO STATE 3 (SUSTAIN) //KUMA:current volume & flagsを更新
            SOFEV7ex();
//            return;
//SOFEV4:
        } else {
            if ((work.pg.softEnvelopeFlag & 0x40) == 0) { //KUMA: Check sustain flag
                SOFEV9ex();//KUMA:release 処理へ
                return;
            }

            byte a = (byte) work.pg.softEnvelopeCounter;// KUMA:get counter
            byte d = (byte) work.pg.softEnvelopeParam[4];// GET SUSTAIN LEVEL// KUMA:get SL
            boolean carry = ((a - d) < 0);//KUMA:counter = counter - SL 結果、counterが0以上の場合はSOFEV5へ
            a -= d;
            if (carry) { // goto SOFEV5;
                a = 0;//KUMA: counter=0
            }
//SOFEV5:
            work.pg.softEnvelopeCounter = a;//KUMA:counter=counter-SL(毎クロック,SL分だけcounterが減る)
            if (a != 0) {
                SOFEV7ex();
                return;
            }
            a = (byte) work.pg.softEnvelopeFlag;//KUMA:current volume & flagsを取得
            a &= 0b1000_1111;//KUMA:エンベロープで使用した進捗に関わるフラグをリセット
            work.pg.softEnvelopeFlag = a;// END OF ENVE //KUMA:KEYON中にSLにきて更にcounterが0になったらエンベロープ処理は終了する
            SOFEV7ex();
        }
    }

    public void SOFEV9ex() {
        byte a = (byte) work.pg.softEnvelopeCounter;//KUMA:get counter
        byte d = (byte) work.pg.softEnvelopeParam[5];// GET REREASE//KUMA:get RR
        boolean carry = ((a - d) < 0);//KUMA:RRでcounterを減算
        a -= d;
        if (carry) { // goto SOFEVA;
            a = 0;
        }
//SOFEVA:
        work.pg.softEnvelopeCounter = a;//KUMA:counterを更新
        SOFEV7ex();
    }

    public void SOFEV7ex() {
        byte e = (byte) work.pg.softEnvelopeCounter;//KUMA:get counter
        int a = (byte) work.pg.volume;// GET VOLUME
        a++;
        a = (byte) ((e * a) >> 8);//AにはVOLUME+1を最大値としたcounter/256の割合分の値が入る
        work.A_Reg = (byte) a;
        if (work.pg.keyoffflg) return;
        if (!work.pg.reverbFlg) return;

        a += (byte) work.pg.reverbVol;//.softEnvelopeParam[5];
        work.carry = ((a & 0x01) != 0);
        a >>= 1;
        work.A_Reg = (byte) a;
    }

    /** SET POINTER */
    public void SETPT() {
        work.pg.dataAddressWork = work.hl; // SET NEXT SOUND DATA ADDRES
    }

    public void SSGEND() {
        work.pg.setmusicEnd(true);
        work.pg.dataAddressWork = work.hl;
        SKYOFF();
        work.pg.lfoflg = false; // RESET LFO FLAG
    }

    /** SSG KEY OFF */
    public void SKYOFF() {
        work.pg.volume = 0; // ENVE FLAG RESET
        byte e = 0;
        byte d = (byte) work.pg.volReg;
        PSGOUT(d, e);
    }

    public void SSSUB3(byte a) {
        if (!work.pg.hardEnveFlg) {
            byte e = a;
            if (work.soundWork.getREADY() == 0) {
                e = 0;
            }
            if (work.soundWork.getKEY_FLAG() != 0xff) {
                byte d = (byte) work.pg.volReg;
                if (work.SSGExtend) e |= (byte) (work.pg.panValue << 6);
                if (work.pg.getpageNo() == work.cd.getcurrentPageNo()) PSGOUT(d, e);
            }
        }
        work.pg.dataAddressWork = work.hl;

        //byte e = a;//added
        //if (Work.soundWork.READY == 0) { //added
        //    e = 0;//added
        //}
        //SSSUB32:
        //byte d = (byte)Work.pg.volReg;
        //PSGOUT(d,e);
        //SETPT();
    }

    public void HRDENV() {
        work.pg.setbackupHardEnv((byte) work.pg.mData[work.hl++].dat);
        work.pg.hardEnveFlg = true;
        if (work.pg.getpageNo() != work.cd.getcurrentPageNo()) return;

        tHRDENV();
    }

    public void restoreHRDENV() {
        if (!work.pg.hardEnveFlg) return;
        tHRDENV();
    }

    public void tHRDENV() {
        byte e = work.pg.getbackupHardEnv();
        byte d = 0x0d;
        PSGOUT(d, e);
        work.pg.hardEnveFlg = true;
        work.pg.hardEnvelopValue = (byte) (e & 0xf);
        work.pg.volume = 16;
    }


    public void ENVPOD() {
        work.pg.setbackupHardEnvFine((byte) work.pg.mData[work.hl++].dat);
        work.pg.setbackupHardEnvCoarse((byte) work.pg.mData[work.hl++].dat);
        if (work.pg.getpageNo() != work.cd.getcurrentPageNo()) return;

        tENVPOD();
    }

    public void restoreENVPOD() {
        tENVPOD();
    }

    public void tENVPOD() {

        byte e = (byte) work.pg.getbackupHardEnvFine();
        byte d = 0x0b;
        PSGOUT(d, e);
        e = (byte) work.pg.getbackupHardEnvCoarse();
        d = 0x0c;
        PSGOUT(d, e);

    }

    private void SetKeyOnDelay() {
        work.pg.KeyOnDelayFlag = false;
        for (int i = 0; i < 4; i++) {
            work.pg.KDWork[i] = work.pg.KD[i] = (byte) work.pg.mData[work.hl++].dat;
            if (work.pg.KD[i] != 0) work.pg.KeyOnDelayFlag = true;
        }

        work.pg.keyOnSlot = 0x00;
        if (!work.pg.KeyOnDelayFlag) work.pg.keyOnSlot = (byte) 0xf0;
    }

    private void KeyOnDelaying() {
        if (work.soundWork.getDRMF1() != 0) return;
        if (work.soundWork.getPCMFLG() != 0) return;
        if (!work.pg.KeyOnDelayFlag) return;

        byte newf = work.pg.keyOnSlot;
        for (int i = 0; i < 4; i++) {
            if (work.pg.KDWork[i] == 0) continue;
            work.pg.KDWork[i]--;
            if (work.pg.KDWork[i] == 0) {
                newf |= (byte) (0x10 << i);
            }
        }

        if (newf != work.pg.keyOnSlot) {
            //Key On!!
            KEYON2();
            work.pg.keyOnSlot = newf;
        }
    }

    public void KEYON2() {
        if (work.soundWork.getREADY() == 0) return;

        byte a = 0x04;
        if (work.soundWork.getFMPORT() == 0) {
            a = 0x00;
        }

        if (!work.pg.KeyOnDelayFlag) {
            a += work.pg.keyOnSlot;
        } else {
            work.pg.keyOnSlot = 0x00;
            if (work.pg.KDWork[0] == 0) work.pg.keyOnSlot += 0x10;
            if (work.pg.KDWork[1] == 0) work.pg.keyOnSlot += 0x20;
            if (work.pg.KDWork[2] == 0) work.pg.keyOnSlot += 0x40;
            if (work.pg.KDWork[3] == 0) work.pg.keyOnSlot += 0x80;
            a += work.pg.keyOnSlot;
        }

        //KEYON2:
        a += (byte) work.pg.channelNumber;
        PSGOUT((byte) 0x28, a);//KEY-ON

        if (work.pg.reverbFlg) {
            STVOL();
        }
    }

    private void FMVolMode() {
        byte b = (byte) work.pg.mData[work.hl++].dat;

        if ((b & 0xff) == 0xff) {
            for (int i = 0; i < 4; i++) {
                work.pg.getTLDirectTable()[i == 0 ? 3 : (i == 1 ? 1 : (i == 2 ? 2 : 0))] = (byte) work.pg.mData[work.hl++].dat;
            }
            return;
        }

        work.cd.setFMVolMode(b);
        switch (work.cd.getFMVolMode()) {
        case 0:
            work.cd.setcurrentFMVolTable(SoundWork.FMVDAT);
            break;
        case 1:
            for (int i = 0; i < 20; i++) {
                work.cd.getFMVolUserTable()[19 - i] = (byte) work.pg.mData[work.hl++].dat;
            }
            work.cd.setcurrentFMVolTable(work.cd.getFMVolUserTable());
            break;
        case 2:
            break;
        case 3:
            break;
        }
    }
}
