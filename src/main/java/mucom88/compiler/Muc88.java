package mucom88.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.logging.Level;

import dotnet4j.util.compat.Tuple;
import dotnet4j.util.compat.Tuple3;
import dotnet4j.util.compat.Tuple4;
import mucom88.common.AutoExtendList;
import mucom88.common.Common;
import mucom88.common.MUCInfo;
import mucom88.common.MucException;
import musicDriverInterface.CompilerInfo;
import musicDriverInterface.LinePos;
import musicDriverInterface.MmlDatum;
import musicDriverInterface.MMLType;
import vavi.util.Debug;

import static dotnet4j.util.compat.CollectionUtilities.toByteArray;


public class Muc88 {

    static final ResourceBundle rb = ResourceBundle.getBundle("lang/message");

    public Msub msub = null;
    public Expand expand = null;

    // private static readonly int MAXCH = 11;
    private final MUCInfo mucInfo;
    private final Supplier<NextAction>[] COMTBL = Arrays.<Supplier<NextAction>>asList(
            this::SETLIZ,
            this::SETOCT,
            this::SETDT,
            this::SETVOL,
            this::SETCOL,
            this::SETOUP,
            this::SETODW,
            this::SETVUP,
            this::SETVDW,
            this::SETTIE,
            this::SETREG,
            this::SETMOD,
            this::SETRST,
            this::SETLPS,
            this::SETLPE,
            this::SETSEorSETHE,
            this::SETJMP,
            this::SETQLG,
            this::SETSEVorSETC3SP,
            this::SETMIXorSETPOR,
            this::SETWAV,
            this::TIMERB,
            this::SETCLK,
            this::COMOVR,
            //                this::SETKST,
            this::SETKS2,
            this::SETRJP,
            this::TOTALV,
            this::SETBEF,
            //                this::SETHE,
            this::SETHEP,
            this::SETKST,
            this::SETKONorSETHE,
            this::SETDCO,
            this::SETLR,
            this::SETHLF,
            this::SETTMP,
            this::SETTAG,
            this::SETMEM,
            this::SETRV,
            this::SETMAC,
            this::STRET, // RETで戻る!
            this::SETTI2, // ret code:fcomp13
            this::SETSYO,
            this::ENDMAC,
            this::SETPTM,
            this::SETFLG,
            this::SETPinPOR
    ).toArray(Supplier[]::new);
    // private readonly int errLin = 0;
    private Work work;

    public Muc88(Work work, MUCInfo mucInfo) {
        this.work = work;
        this.mucInfo = mucInfo;
    }

    private NextAction SETHE() {
        if (CHCHK() != ChannelType.SSG) {
            throw new MucException(rb.getString("E0521"), mucInfo.getRow(), mucInfo.getCol());
        }

        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.ERRT(mucInfo.getLin(), /*ref*/ ptr, rb.getString("E0504"));
        mucInfo.setSrcCPtr(ptr[0]);
        if (mucInfo.getErrSign() || n < 0 || n > 15)
            throw new MucException(rb.getString("E0505"), mucInfo.getRow(), mucInfo.getCol());

        // mucInfo.getDriverType() = MUCInfo.DriverType.DotNet;
        msub.MWRITE(new MmlDatum(0xff), new MmlDatum(0xf1));
        msub.MWRIT2(new MmlDatum(n));

        return NextAction.fcomp1;
    }

    private NextAction SETHEP() {
        if (CHCHK() != ChannelType.SSG) {
            throw new MucException(rb.getString("E0520"), mucInfo.getRow(), mucInfo.getCol());
        }

        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.ERRT(mucInfo.getLin(), /*ref*/ ptr, rb.getString("E0506"));
        mucInfo.setSrcCPtr(ptr[0]);
        if (mucInfo.getErrSign() || n < 0 || n > 65535)
            throw new MucException(rb.getString("E0507"), mucInfo.getRow(), mucInfo.getCol());

        // mucInfo.getDriverType() = MUCInfo.DriverType.DotNet;
        msub.MWRITE(new MmlDatum(0xff), new MmlDatum(0xf2));
        msub.MWRITE(new MmlDatum(n), new MmlDatum(n >> 8));

        return NextAction.fcomp1;
    }

    private NextAction SETSEorSETHE() {
        if (CHCHK() != ChannelType.SSG) return SETSE(); // SSG以外ならスロットディチューンコマンドとして動作

        return SETHE(); // SSGなら
    }

    // *ﾏｸﾛｾｯﾄ*

    public NextAction SETMAC() {
        mucInfo.getAndIncSrcCPtr();
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.readData(mucInfo.getLin(), /*ref*/ ptr);
        if (mucInfo.getDriverType() != MUCInfo.DriverType.DotNet) {
            if (n > 0xff || n < 0) {
                writeWarning(rb.getString("W0400"), mucInfo.getRow(), mucInfo.getCol());
            }
            n &= 0xff;
        }
        mucInfo.setSrcCPtr(ptr[0]);

        // 戻り先を記憶
        mucInfo.getBufMacStack().set(work.adrStc, mucInfo.getSrcCPtr()); // col
        mucInfo.getBufMacStack().set(work.adrStc + 1, mucInfo.getSrcLinPtr() + 1); // line row
        work.adrStc += 2;

        // 飛び先を取得
        mucInfo.setSrcCPtr(mucInfo.getBufMac().get(n * 2 + 0));
        mucInfo.setSrcLinPtr(mucInfo.getBufMac().get(n * 2 + 1));
        if (mucInfo.getSrcLinPtr() == 0) {
            throw new MucException(rb.getString("E0400"), mucInfo.getRow(), mucInfo.getCol());
        }
        mucInfo.decSrcCPtr();
        mucInfo.setLin(mucInfo.getBasSrc().get(mucInfo.getSrcLinPtr()));

        return NextAction.fcomp1;
    }

    private NextAction ENDMAC() {
        work.adrStc -= 2;
        if (work.adrStc < 0) {
            throw new MucException(rb.getString("E0401"), mucInfo.getRow(), mucInfo.getCol());
        }
        mucInfo.setSrcCPtr(mucInfo.getBufMacStack().get(work.adrStc));
        mucInfo.setSrcLinPtr(mucInfo.getBufMacStack().get(work.adrStc + 1));
        mucInfo.getBufMacStack().set(work.adrStc, 0); // clear col
        mucInfo.getBufMacStack().set(work.adrStc + 1, 0); // clear line row
        if (mucInfo.getSrcLinPtr() == 0) {
            throw new MucException(rb.getString("E0402"), mucInfo.getRow(), mucInfo.getCol());
        }
        mucInfo.decSrcCPtr();
        mucInfo.setLin(mucInfo.getBasSrc().get(mucInfo.getSrcLinPtr()));

        return NextAction.fcomp1;
    }

    private void skipSpaceAndTab() {
        char c = getMoji();

        while (c == ' ' || c == 0x9) {
            mucInfo.getAndIncSrcCPtr();
            c = getMoji();
        }
    }

    private char getMoji() {
        return mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length() ?
                mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;
    }

    private NextAction SETPTM() {
        LinePos lp = new LinePos(mucInfo.document, mucInfo.getFnSrc(), mucInfo.getRow(),
                mucInfo.getCol(), mucInfo.getSrcCPtr(), "", "", 0, 0, -1);

        mucInfo.getAndIncSrcCPtr();
        byte beforeNote = msub.STTONE(); // KUMA: オクターブ情報などを含めた音符情報に変換
        if (mucInfo.getCarry()) {
            throw new MucException(rb.getString("E0403"), mucInfo.getRow(), mucInfo.getCol());
        }

        mucInfo.getAndIncSrcCPtr();
        int befCo = FC162p_clock(beforeNote); // SET TONE&LIZ
        int qBefCo = befCo - work.quantize;

        skipSpaceAndTab();

        char c = getMoji();
        if (c == 0) {
            throw new MucException(rb.getString("E0404"), mucInfo.getRow(), mucInfo.getCol());
        }

        if (c == '}') {
            // 到達点を指定することなくポルタメント終了している場合はエラー
            throw new MucException(rb.getString("E0405"), mucInfo.getRow(), mucInfo.getCol());
        }

        boolean pflg = false;
        while (c == '>' || c == '<' || c == ' ' || c == 0x9) {
            if (c == '>') {
                if (pflg && mucInfo.getDriverType() != MUCInfo.DriverType.DotNet)
                    throw new MucException(rb.getString("E9998"), mucInfo.getRow(), mucInfo.getCol());
                pflg = true;
                SOU1();
            } else if (c == '<') {
                if (pflg && mucInfo.getDriverType() != MUCInfo.DriverType.DotNet)
                    throw new MucException(rb.getString("E9998"), mucInfo.getRow(), mucInfo.getCol());
                pflg = true;
                SOD1();
            } else
                mucInfo.getAndIncSrcCPtr();

            c = getMoji();
        }

        byte after_note = msub.STTONE(); // KUMA:オクターブ情報などを含めた音符情報に変換
        if (mucInfo.getCarry()) {
            throw new MucException(rb.getString("E0404"), mucInfo.getRow(), mucInfo.getCol());
        }

        mucInfo.getAndIncSrcCPtr();

        skipSpaceAndTab();

        c = getMoji(); // KUMA:次のコマンドを取得

        if (c != '}') {
            throw new MucException(rb.getString("E0407"), mucInfo.getRow(), mucInfo.getCol());
        }

        mucInfo.getAndIncSrcCPtr();
        lp.length = mucInfo.getSrcCPtr() - lp.length;

        if (mucInfo.getDriverType() != MUCInfo.DriverType.DotNet)
            mainPortament(beforeNote, after_note, befCo, qBefCo);
        else
            PortamentMainEx(beforeNote, after_note, befCo, befCo - qBefCo);

        work.latestNote = 1; // KUMA:チェック用(音符)

        return NextAction.fcomp1;
    }

    private void PortamentMainEx(byte beforeNote, byte afterNote, int clk, int q) {
        msub.MWRIT2(new MmlDatum(0xff)); // PTMDAT;
        msub.MWRIT2(new MmlDatum(0xf9));
        msub.MWRIT2(new MmlDatum(beforeNote & 0xff));
        msub.MWRIT2(new MmlDatum(afterNote & 0xff));
        msub.MWRIT2(new MmlDatum(clk));
        msub.MWRIT2(new MmlDatum(clk >> 8));

        writeFC162p(beforeNote, (byte) clk, (byte) q, false, true);
    }

    private void mainPortament(byte beforeNote, byte afterNote, int befCo, int qBefCo) {
        int beftone = expand.ctone(beforeNote);
        int noteNum = expand.ctone(afterNote) - beftone;
        int sign = (int) Math.signum(noteNum);
        noteNum = Math.abs(noteNum); // +1;

        qBefCo = Math.max(qBefCo, 1);

        // 26を超えたら分割が必要?

        int noteDiv = noteNum / 2 + 1; // 2おんずつ
        // int noteDiv = noteNum; // 1おんずつ
        if (noteDiv > befCo) noteDiv = befCo;
        double noteStep = (double) noteNum / noteDiv * sign;
        // int noteMod = Math.Abs(noteNum % noteDiv);
        int clock = befCo / noteDiv;
        int clockMod = befCo % noteDiv;

        List<Tuple4<Integer, Integer, Integer, Integer>> lstPrt = new ArrayList<>();
        double stNote = 0;
        for (int i = 0; i < noteDiv; i++) {
            // int edNote = stNote + noteStep + ((noteMod > 0 ? 1 : 0)) * sign;
            double edNote = stNote + noteStep;
            if (clock + (clockMod > 0 ? 1 : 0) != 0) {
                int n = clock + (clockMod > 0 ? 1 : 0);
                Tuple4<Integer, Integer, Integer, Integer> p = new Tuple4<>(
                        (int) (stNote + beftone)
                        , (int) (edNote + beftone)
                        , n
                        , Math.min(qBefCo, n)
                );
                lstPrt.add(p);
                qBefCo -= n;
                qBefCo = Math.max(qBefCo, 0);
            } else {
                Tuple4<Integer, Integer, Integer, Integer> p = new Tuple4<>(
                        lstPrt.get(lstPrt.size() - 1).getItem1()
                        , (int) (edNote + beftone)
                        , lstPrt.get(lstPrt.size() - 1).getItem3()
                        , qBefCo > lstPrt.get(lstPrt.size() - 1).getItem3() ? lstPrt.get(lstPrt.size() - 1).getItem3() : qBefCo
                );
                qBefCo -= lstPrt.get(lstPrt.size() - 1).getItem3();
                qBefCo = Math.max(qBefCo, 0);
                lstPrt.set(lstPrt.size() - 1, p);
            }

            stNote = edNote; // + sign;
            // noteMod--;
            clockMod--;
        }

        boolean tie = true;
        work.beforeQuantize = work.quantize;

        for (int i = 0; i < lstPrt.size(); i++) {
            Tuple4<Integer, Integer, Integer, Integer> it = lstPrt.get(i);
            if (i == lstPrt.size() - 1) tie = false;
            byte st = (byte) (((it.getItem1() / 12) << 4) | ((it.getItem1() % 12) & 0xf));
            byte ed = (byte) (((it.getItem2() / 12) << 4) | ((it.getItem2() % 12) & 0xf));
            if (mucInfo.getDriverType() != MUCInfo.DriverType.DotNet) {
                writePortament(st, ed, (byte) (int) it.getItem3(), (byte) (it.getItem3() - it.getItem4()), tie);
            } else {
                writePortamentEx(st, ed, (byte) (int) it.getItem3(), (byte) (it.getItem3() - it.getItem4()), tie);
            }
        }

        endPortament();
    }

    private void writePortament(byte note, byte endNote, byte clk, byte q, boolean tie) {
        int depth;
        if (work.chipIndex != 4) {
            depth = expand.CULPTM(work.chipIndex, note, endNote, clk); // KUMA:DEPTHを計算
        } else {
            // ここでは距離を求めたいだけなのでopmのマスタークロックの違いを考慮する必要は無い
            mucInfo.setCarry(false);
            int s = ((note & 0xf0) >> 4) * 12 + (note & 0xf);
            int e = ((endNote & 0xf0) >> 4) * 12 + (endNote & 0xf);
            depth = (e - s) * 64 / clk;
        }
        if (mucInfo.getCarry()) {
            throw new MucException(
                    rb.getString("E0406")
                    , mucInfo.getRow(), mucInfo.getCol());
        }

        startPortament(depth);
        writeFC162p(note, clk, q, tie, false);
    }

    private void writePortamentEx(byte note, byte endNote, byte clk, byte q, boolean tie) {
        double depth;
        if (work.chipIndex != 4) {
            depth = expand.CULPTMex(work.chipIndex, note, endNote, clk); // KUMA:DEPTHを計算
        } else {
            // ここでは距離を求めたいだけなのでopmのマスタークロックの違いを考慮する必要は無い
            mucInfo.setCarry(false);
            int s = ((note & 0xf0) >> 4) * 12 + (note & 0xf);
            int e = ((endNote & 0xf0) >> 4) * 12 + (endNote & 0xf);
            depth = (e - s) * 64D / clk;
        }
        if (mucInfo.getCarry()) {
            throw new MucException(
                    rb.getString("E0406")
                    , mucInfo.getRow(), mucInfo.getCol());
        }

        int speed = 1;
        while (depth != 0 && Math.abs(depth) < 1.0) {
            speed++;
            depth *= 2.0;
        }

        startPortamentEx((int) depth, speed);
        writeFC162p(note, clk, q, tie, false);
    }

    private void startPortament(int depth) {
        msub.MWRIT2(new MmlDatum(0xf4)); // PTMDAT;
        msub.MWRIT2(new MmlDatum(0x00));
        msub.MWRIT2(new MmlDatum(0x01));
        msub.MWRIT2(new MmlDatum(0x01));
        work.setBefMd(work.mData); // KUMA:DEPTHの書き込み位置を退避
        // Work.mData += 2;
        msub.MWRIT2(new MmlDatum(depth & 0xff));
        msub.MWRIT2(new MmlDatum(depth >> 8));

        msub.MWRIT2(new MmlDatum(0xff)); // KUMA:回数(255回)を書き込む
    }

    private void startPortamentEx(int depth, int speed) {
        msub.MWRIT2(new MmlDatum(0xf4)); // PTMDAT;
        msub.MWRIT2(new MmlDatum(0x00));
        msub.MWRIT2(new MmlDatum(0x01));
        msub.MWRIT2(new MmlDatum(speed)); // speed
        work.setBefMd(work.mData); // KUMA:DEPTHの書き込み位置を退避
        // Work.mData += 2;
        msub.MWRIT2(new MmlDatum(depth & 0xff));
        msub.MWRIT2(new MmlDatum(depth >> 8));

        msub.MWRIT2(new MmlDatum(0xff)); // KUMA:回数(255回)を書き込む
    }

    private void endPortament() {
        msub.MWRIT2(new MmlDatum(0xf4)); // KUMA:2個目のMコマンド作成開始
        byte a = work.lfoData[0]; // KUMA:現在のLFOのスイッチを取得
        a--;
        if (a == 0) { // KUMA:OFF(1)の場合はSTP1で2個めのMコマンドへOFF(1)を書き込む
            msub.MWRIT2(new MmlDatum(0x01));
        } else {
            msub.MWRIT2(new MmlDatum(0x00)); // KUMA:ON(0)の場合は2個めのMコマンドへON(0)を書き込む
            msub.MWRIT2(new MmlDatum(work.lfoData[1] & 0xff)); // KUMA:残りの現在のLFOの設定5byteをそのまま２個目のMコマンドへコピー
            msub.MWRIT2(new MmlDatum(work.lfoData[2] & 0xff));
            msub.MWRIT2(new MmlDatum(work.lfoData[3] & 0xff));
            msub.MWRIT2(new MmlDatum(work.lfoData[4] & 0xff));
            msub.MWRIT2(new MmlDatum(work.lfoData[5] & 0xff));
        }

        if (work.beforeQuantize != work.quantize)
            msub.MWRITE(new MmlDatum(0xf3), new MmlDatum(work.quantize)); // COM OF 'q'

    }

    private void writeFC162p(int note, byte clk, byte q, boolean tie, boolean force/*=false*/) {
        // if (tie) {
        //    msub.MWRIT2(new MmlDatum(0xfd));
        // }
        TCLKSUB(clk); // ﾄｰﾀﾙｸﾛｯｸ ｶｻﾝ

        if (work.beforeQuantize != q) {
            msub.MWRITE(new MmlDatum(0xf3), new MmlDatum(q & 0xff)); // COM OF 'q'
            work.beforeQuantize = q;
        }

        if (q < clk || force) {
            FCOMP17(note, clk); // note
            if (tie) {
                msub.MWRIT2(new MmlDatum(0xfd)); // tie
            }
        } else {
            // rest
            while (clk > 0x6f) {
                clk -= 0x6f;
                msub.MWRIT2(new MmlDatum(0b1110_1111));
            }
            work.befRst = clk;
            msub.MWRIT2(new MmlDatum((clk & 0xff) | 0b1000_0000)); // SET REST FLAG
        }
    }


    private NextAction SETPTM2() {
        msub.MWRIT2(new MmlDatum(0xf4)); // PTMDAT;
        msub.MWRIT2(new MmlDatum(0x00));
        msub.MWRIT2(new MmlDatum(0x01));
        msub.MWRIT2(new MmlDatum(0x01));
        work.setBefMd(work.mData); // KUMA:DEPTHの書き込み位置を退避
        work.mData += 2;
        msub.MWRIT2(new MmlDatum(0xff)); // KUMA:回数(255回)を書き込む

        mucInfo.getAndIncSrcCPtr();

        byte note = msub.STTONE(); // KUMA:オクターブ情報などを含めた音符情報に変換
        if (mucInfo.getCarry()) {
            throw new MucException(rb.getString("E0403"), mucInfo.getRow(), mucInfo.getCol());
        }

        mucInfo.getAndIncSrcCPtr();
        FC162p(note); // SET TONE&LIZ

        msub.MWRIT2(new MmlDatum(0xf4)); // KUMA:2個目のMコマンド作成開始
        byte a = work.lfoData[0]; // KUMA:現在のLFOのスイッチを取得
        a--;
        if (a == 0) { // KUMA:OFF(1)の場合はSTP1で2個めのMコマンドへOFF(1)を書き込む
            msub.MWRIT2(new MmlDatum(0x01));
        } else {
            msub.MWRIT2(new MmlDatum(0x00)); // KUMA:ON(0)の場合は2個めのMコマンドへON(0)を書き込む
            msub.MWRIT2(new MmlDatum(work.lfoData[1] & 0xff)); // KUMA:残りの現在のLFOの設定5byteをそのまま２個目のMコマンドへコピー
            msub.MWRIT2(new MmlDatum(work.lfoData[2] & 0xff));
            msub.MWRIT2(new MmlDatum(work.lfoData[3] & 0xff));
            msub.MWRIT2(new MmlDatum(work.lfoData[4] & 0xff));
            msub.MWRIT2(new MmlDatum(work.lfoData[5] & 0xff));
        }

        char c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length() ?
                mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;

        if (c == 0) {
            throw new MucException(
                    rb.getString("E0404")
                    , mucInfo.getRow(), mucInfo.getCol());
        }
        if (c == '}') // 0x7d
        {
            throw new MucException(
                    rb.getString("E0405")
                    , mucInfo.getRow(), mucInfo.getCol());
        }

        boolean pflg = false;
        while (c == '>' || c == '<' || c == ' ' || c == 0x9) {
            if (c == '>') // 0x3e
            {
                if (pflg && mucInfo.getDriverType() != MUCInfo.DriverType.DotNet)
                    throw new MucException(rb.getString("E9998"), mucInfo.getRow(), mucInfo.getCol());
                pflg = true;
                SOU1();
            } else if (c == '<') // 0x3c
            {
                if (pflg && mucInfo.getDriverType() != MUCInfo.DriverType.DotNet)
                    throw new MucException(rb.getString("E9998"), mucInfo.getRow(), mucInfo.getCol());
                pflg = true;
                SOD1();
            } else
                mucInfo.getAndIncSrcCPtr();

            c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length() ?
                    mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                    : (char) 0;
        }

        // int depth = Expand.CULPTM(); // KUMA:DEPTHを計算
        int depth;
        if (work.chipIndex != 4) {
            depth = expand.CULPTM(work.chipIndex); // KUMA:DEPTHを計算
        } else {
            // ここでは距離を求めたいだけなのでopmのマスタークロックの違いを考慮する必要は無い
            int s = ((note & 0xf0) >> 4) * 12 + (note & 0xf);
            int e = ((work.getBeforeTone()[0] & 0xf0) >> 4) * 12 + (work.getBeforeTone()[0] & 0xf);
            depth = (e - s) * 64 / work.getBefCo();
        }

        if (mucInfo.getCarry()) {
            throw new MucException(
                    rb.getString("E0406")
                    , mucInfo.getRow(), mucInfo.getCol());
        }

        mucInfo.getBufDst().set(work.getBefMd(), new MmlDatum(depth)); // KUMA:DE(DEPTH)を書き込む
        mucInfo.getBufDst().set(work.getBefMd() + 1, new MmlDatum(depth >> 8));

        mucInfo.getAndIncSrcCPtr();
        c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length() ?
                mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0; // KUMA:次のコマンドを取得

        if (c != '}') // 0x7d
        {
            throw new MucException(
                    rb.getString("E0407")
                    , mucInfo.getRow(), mucInfo.getCol());
        }

        mucInfo.getAndIncSrcCPtr();
        return NextAction.fcomp1;
    }

    // *ﾘﾊﾞｰﾌﾞ*

    private NextAction SETRV() {
        ChannelType tp = CHCHK();
        if (tp != ChannelType.FM && tp != ChannelType.SSG) {
            throw new MucException(
                    rb.getString("E0408")
                    , mucInfo.getRow(), mucInfo.getCol());
        }

        mucInfo.getAndIncSrcCPtr();
        char c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length() ?
                mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;

        byte dat = (byte) 0xf3;
        if (c == 'm') // 0x6d
        {
            dat = (byte) 0xf4;
        } else if (c == 'F') // 0x46
        {
            dat = (byte) 0xf5;
        } else {
            mucInfo.decSrcCPtr();
        }

        msub.MWRITE(new MmlDatum(0xff), new MmlDatum(dat & 0xff));
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.ERRT(mucInfo.getLin(), /*ref*/ ptr, rb.getString("E0409"));
        mucInfo.setSrcCPtr(ptr[0]);
        msub.MWRIT2(new MmlDatum(n));

        return NextAction.fcomp1;
    }

    private NextAction SETTMP() {
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.ERRT(mucInfo.getLin(), /*ref*/ ptr, rb.getString("E0410")); // T
        mucInfo.setSrcCPtr(ptr[0]);
        if ((byte) n == 0) {
            throw new MucException(
                    rb.getString("E0411")
                    , mucInfo.getRow(), mucInfo.getCol());
        }

        byte HL;
        if (work.chipIndex < 2) {
            // OPNA : 7987200 Hz
            HL = (byte) ((25600 - 346 * (byte) (60000 / (work.clock / 4 * (byte) n) + 1)) / 100);
        } else if (work.chipIndex < 4) {
            // OPNB : 8000000 Hz
            HL = (byte) ((25600 - 347 * (byte) (60000 / (work.clock / 4 * (byte) n) + 1)) / 100);
        } else {
            // OPM

            if (mucInfo.getOpmClockMode() == MUCInfo.OpmClockMode.normal) {
                // Normal : 3579545 Hz
                HL = (byte) ((25600 - 350 * (byte) (60000 / (work.clock / 4 * (byte) n) + 1)) / 100);
            } else {
                // X68k : 400000 Hz
                HL = (byte) ((25600 - 391 * (byte) (60000 / (work.clock / 4 * (byte) n) + 1)) / 100);
            }
        }

        // if (HL < 1) {
        //    HL = 1;
        // }

        return TIMEB2(HL);
    }

    /** FLAGDATA SET */
    private NextAction SETFLG() {
        mucInfo.getAndIncSrcCPtr();
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.readData(mucInfo.getLin(), /*ref*/ ptr);
        mucInfo.setSrcCPtr(ptr[0]);
        if (mucInfo.getCarry()) {
            n = 0xFF;
        }
        msub.MWRITE(new MmlDatum(0xf9), new MmlDatum(n));

        return NextAction.fcomp1;
    }

    /** ｼｮｳｾﾂﾏｰｸ */
    private NextAction SETSYO() {
        mucInfo.getAndIncSrcCPtr();

        if (work.getPartReplaceSw()) {
            return setPartReplaceEnd();
        }

        return NextAction.fcomp1;
    }

    /** ﾁｭｳﾔｸ */
    private NextAction SETMEM() {
        mucInfo.setSrcCPtr(mucInfo.getLin().getItem2().length());
        return NextAction.fcomp1; // KUMA:NextLine
    }

    /** SET TAG & JUMP TO TAG */
    private NextAction SETTAG() {

        work.jClock = work.tCnt[work.chipIndex][work.chipCh][work.pageNow];
        work.setJChCom(new ArrayList<>());
        work.getJChCom().add(work.chipCh);
        if (work.pointC >= 0) {
            int p = work.pointC;
            while (p >= 0) {
                work.jClock += mucInfo.getBufLoopStack().get(p + 6) + mucInfo.getBufLoopStack().get(p + 7) * 0x100;
                p -= 10;
            }
        }
        work.setJpLine(mucInfo.getRow());
        work.setJpCol(mucInfo.getCol());

        mucInfo.getAndIncSrcCPtr();

        return NextAction.fcomp1;
    }

    /** *HARD LFO* */
    private NextAction SETHLF() {
        ChannelType tp = CHCHK();
        if (tp != ChannelType.FM) {
            throw new MucException(rb.getString("E0412"), mucInfo.getRow(), mucInfo.getCol());
        }

        List<Object> args = new ArrayList<>();
        LinePos lp = new LinePos(
                mucInfo.document,
                mucInfo.getFnSrcOnlyFile()
                , mucInfo.getRow(), mucInfo.getCol()
                , mucInfo.getSrcCPtr() - mucInfo.getCol() + 1
                , work.currentPartType
                , work.currentChipName
                , 0, work.chipIndex % 2, work.chipCh * Work.MAXPG + work.pageNow);

        msub.MWRIT2(new MmlDatum(MMLType.HardLFO, args, lp, 0xfc));

        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.ERRT(mucInfo.getLin(), /*ref*/ ptr, rb.getString("E0413"));
        msub.MWRIT2(new MmlDatum(n));
        args.add(n);

        n = msub.ERRT(mucInfo.getLin(), /*ref*/ ptr, rb.getString("E0413"));
        msub.MWRIT2(new MmlDatum(n));
        args.add(n);

        n = msub.ERRT(mucInfo.getLin(), /*ref*/ ptr, rb.getString("E0413"));
        mucInfo.setSrcCPtr(ptr[0]);
        msub.MWRIT2(new MmlDatum(n));
        args.add(n);

        return NextAction.fcomp1;
    }

    /** *STEREO PAN* */
    private NextAction SETLR() {
        ChannelType tp = CHCHK();
        if (tp == ChannelType.SSG && !mucInfo.getSSGExtend()) {
            writeWarning(rb.getString("W0407"), mucInfo.getRow(), mucInfo.getCol());
        }

        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        // Ryhthm の内訳
        // bit0～3 rythmType R:5 T:4 H:3 C:2 S:1 B:0
        // bit4～7 パン 1:右, 2:左, 3:中央 4:右オート 5:左オート 6:ランダム
        int v;

        // Rhythmの場合はpmコマンドか判定
        if (tp == ChannelType.RHYTHM) {
            char mode = mucInfo.getLin().getItem2().length() > ptr[0] + 1 ? mucInfo.getLin().getItem2().charAt(ptr[0] + 1) : (char) 0;
            if (mode == 'm') {
                ptr[0] = mucInfo.incAndGetSrcCPtr();
                v = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0414"));
                work.setRhythmPanMode(v != 0);
                mucInfo.setSrcCPtr(ptr[0]);
                return NextAction.fcomp1;
            }
        }

        v = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0414"));
        int n = (byte) v;
        int rn = (byte) ((tp == ChannelType.RHYTHM && !work.getRhythmPanMode()) ? (n >> 4) : n);

        // 1-6の範囲外の時 -> エラー
        if (rn < 1 || rn > 6) {
            // 使用可能な値の範囲外
            throw new MucException(String.format(rb.getString("E0524"), v), mucInfo.getRow(), mucInfo.getCol());
        }

        skipSpaceAndTab();
        mucInfo.setSrcCPtr(ptr[0]);
        char c = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr() ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr()) : (char) 0;

        // 1-3なのに,がある時 -> 警告扱い
        if (c == ',' && rn > 0 && rn < 4) {
            writeWarning(rb.getString("W0415"), mucInfo.getRow(), mucInfo.getCol());
        }

        if (tp != ChannelType.RHYTHM || !work.getRhythmPanMode()) {

            if (work.chipIndex == 4 && mucInfo.getOpmPanReverse()) {
                if ((n & 0x3) != 0) {
                    n = ((n & 0xfc) | ((n & 1) << 1) | ((n & 2) >> 1));
                }
            }

            // 
            List<Object> args = new ArrayList<>();
            args.add(n);
            LinePos lp = new LinePos(
                    mucInfo.document,
                    mucInfo.getFnSrcOnlyFile()
                    , mucInfo.getRow(), mucInfo.getCol()
                    , mucInfo.getSrcCPtr() - mucInfo.getCol() + 1
                    , work.currentPartType
                    , work.currentChipName
                    , 0, work.chipIndex % 2, work.chipCh * Work.MAXPG + work.pageNow);

            if (tp == ChannelType.SSG && mucInfo.getSSGExtend()) { // kuma: SSGでパンが使用できるのは拡張モードが有効な場合のみ
                msub.MWRITE(
                        new MmlDatum(MMLType.Pan, args, lp, 0xff),
                        new MmlDatum(0xf0)); // kuma: SSGパートの場合は 0xff 0xf0 n を書き込む
                msub.MWRIT2(new MmlDatum(n));
            } else {
                msub.MWRITE(
                        new MmlDatum(MMLType.Pan, args, lp, 0xf8),
                        new MmlDatum(n)); // COM OF 'p' // kuma: SSGパート以外の場合は 0xf8 n を書き込む
            }

            // AMD98

            // 1-6の範囲内で且つwait値が指定されている場合はそれを使用する.省略時はlコマンドの値を使用する
            int n2 = work.count;
            if (c == ',') { // 0x2c
                // 1-6の範囲内ならwait値を取得する
                if (mucInfo.getDriverType() != MUCInfo.DriverType.DotNet)
                    throw new MucException(rb.getString("E9998"), mucInfo.getRow(), mucInfo.getCol());
                mucInfo.getAndIncSrcCPtr();
                ptr[0] = mucInfo.getSrcCPtr();
                n2 = msub.readData(mucInfo.getLin(), /*ref*/ptr);
                mucInfo.setSrcCPtr(ptr[0]);
            }

            // 4-6の範囲内の場合はwait値(レングス)を出力する
            if (rn > 3 && rn < 7) {
                // wait値が範囲外の時 ->エラー
                if (n2 < 1 || n2 > 255 || (byte) n2 < 1) {
                    throw new MucException(String.format(rb.getString("E0526"), 255, v), mucInfo.getRow(), mucInfo.getCol());
                }
                if (tp == ChannelType.SSG && !mucInfo.getSSGExtend())
                    return NextAction.fcomp1; // KUMA:互換の為。。。(SSGパートではnoise周波数設定コマンドとして動作)

                msub.MWRIT2(new MmlDatum(n2)); // ２こめ
            }

            // 

            return NextAction.fcomp1;
        } else {
            int n2 = -1;
            if (c == ',') // 0x2c
            {
                if (mucInfo.getDriverType() != MUCInfo.DriverType.DotNet)
                    throw new MucException(rb.getString("E9998"), mucInfo.getRow(), mucInfo.getCol());
                mucInfo.getAndIncSrcCPtr();
                ptr[0] = mucInfo.getSrcCPtr();
                n2 = msub.readData(mucInfo.getLin(), /*ref*/ ptr);
                mucInfo.setSrcCPtr(ptr[0]);
            }

            for (int i = 0; i < 6; i++) {
                if ((work.getRhythmInstNum() & (1 << i)) == 0) continue;
                n = (rn << 4) | i;
                List<Object> args = new ArrayList<>();
                args.add(n);
                LinePos lp = new LinePos(
                        mucInfo.document,
                        mucInfo.getFnSrcOnlyFile()
                        , mucInfo.getRow(), mucInfo.getCol()
                        , mucInfo.getSrcCPtr() - mucInfo.getCol() + 1
                        , work.chipIndex / 2 == 0 ? "RHYTHM" : "ADPCM-A" // YM2151はここに入ってこないので判定を略する
                        , work.chipIndex / 2 == 0 ? "YM2608" : "YM2610B"
                        , 0, work.chipIndex % 2, work.chipCh * Work.MAXPG + work.pageNow);

                msub.MWRITE(
                        new MmlDatum(MMLType.Pan, args, lp, 0xf8)
                        , new MmlDatum(n)); // COM OF 'p'

            }

            if (n2 != -1) { // 0x2c
                // 4-6の範囲内の場合のみwait値を出力する
                if (rn > 3 && rn < 7) {
                    // wait値が範囲外の時 ->エラー
                    if (n2 < 1 || n2 > 255 || (byte) n2 < 1) {
                        throw new MucException(String.format(rb.getString("E0526"), 255, v), mucInfo.getRow(), mucInfo.getCol());
                    }
                    msub.MWRIT2(new MmlDatum(n2)); // ２こめ
                }
            }

            return NextAction.fcomp1;
        }
    }

    /** DIRECT count */
    private NextAction SETDCO() {
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0415"));
        mucInfo.setSrcCPtr(ptr[0]);
        work.count = (byte) n;

        return NextAction.fcomp12;
    }

//    /** SET HARD ENVE TYPE/FLAG */
//    private enmFCOMPNextRtn SETHE() {
//        mucInfo.incSrcCPtr();
//        msub.MWRITE(new MmlDatum(0xff,0xf1); // 2nd COM
//
//        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
//        int n = msub.REDATA(mucInfo.getLin(), /*ref*/ptr);
//        mucInfo.setSrcCPtr(ptr);
//        msub.MWRIT2(new MmlDatum((byte)n);
//
//        return enmFCOMPNextRtn.fcomp1;
//    }

//    private enmFCOMPNextRtn SETHEP() {
//        mucInfo.incSrcCPtr();
//        msub.MWRITE(new MmlDatum(0xff, 0xf2);
//
//        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
//        int n = msub.REDATA(mucInfo.getLin(), /*ref*/ptr);
//        mucInfo.setSrcCPtr(ptr);
//        msub.MWRITE(new MmlDatum((byte)n, (byte)(n >> 8)); // 2ﾊﾞｲﾄﾃﾞｰﾀ ｶｸ
//
//        return enmFCOMPNextRtn.fcomp1;
//    }

    // BEFORE CODE
    private NextAction SETBEF() {
        int[] ptr = new int[1];
        int n;

        mucInfo.getAndIncSrcCPtr();
        char c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length() ?
                mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;
        if (c != '=') { // 0x3d
            msub.MWRITE(new MmlDatum(0xfb), new MmlDatum(-work.getVdDat() & 0xff));
            msub.MWRIT2(new MmlDatum(work.getBefCo()));
            TCLKSUB(work.getBefCo());
            msub.MWRIT2(new MmlDatum(work.getBeforeTone()[work.getBfDat() & 0xff] & 0xff));
            msub.MWRITE(new MmlDatum(0xfb), new MmlDatum(work.getVdDat() & 0xff));

            return NextAction.fcomp1;
        }

        ptr[0] = mucInfo.getSrcCPtr();
        n = (byte) msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0416"));
        mucInfo.setSrcCPtr(ptr[0]);

        if (n >= 10) {
            throw new MucException(
                    rb.getString("E0417")
                    , mucInfo.getRow(), mucInfo.getCol());
        }

        if (n == 0) {
            n++;
        }
        n--;

        work.setBfDat((byte) n);

        c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length() ?
                mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;
        if (c != ',') // 0x2c
        {
            return NextAction.fcomp1;
        }

        ptr[0] = mucInfo.getSrcCPtr();
        n = (byte) msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0416"));
        mucInfo.setSrcCPtr(ptr[0]);

        work.setVdDat((byte) n);
        return NextAction.fcomp1;
    }

    /** *TOTAL volume* */
    private NextAction TOTALV() {
        int[] ptr = new int[1];
        ptr[0] = mucInfo.getSrcCPtr();
        int n = (byte) msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0418"));
        mucInfo.setSrcCPtr(ptr[0]);

        work.tvOfs = (byte) n;

        return NextAction.fcomp1;
    }

    /** **REPEAT JUMP** */
    private NextAction SETRJP() {
        mucInfo.getAndIncSrcCPtr();

        msub.MWRIT2(new MmlDatum(0xfe));

        int HL = work.pointC + 4;
        if (HL < 0) {
            throw new MucException(rb.getString("E0523"), mucInfo.getRow(), mucInfo.getCol());
        }

        if ((mucInfo.getBufLoopStack().get(HL) | mucInfo.getBufLoopStack().get(HL + 1)) != 0) {
            throw new MucException(rb.getString("E0419"), mucInfo.getRow(), mucInfo.getCol());
        }

        mucInfo.getBufLoopStack().set(HL, (byte) work.mData);
        mucInfo.getBufLoopStack().set(HL + 1, (byte) (work.mData >> 8));
        HL += 4;
        work.mData += 2;

        mucInfo.getBufLoopStack().set(HL, (byte) work.tCnt[work.chipIndex][work.chipCh][work.pageNow]);
        mucInfo.getBufLoopStack().set(HL + 1, (byte) (work.tCnt[work.chipIndex][work.chipCh][work.pageNow] >> 8));

        return NextAction.fcomp1;
    }

    public NextAction SETKONorSETHE() {
        if (mucInfo.getDriverType() != MUCInfo.DriverType.E) {
            return SETKON();
        } else {
            return SETHE();
        }
    }

    //** KEY ON REVISE * added */
    public NextAction SETKON() {
        int[] ptr = new int[1];
        ptr[0] = mucInfo.getSrcCPtr();
        int n = (byte) msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0420"));
        mucInfo.setSrcCPtr(ptr[0]);
        work.setKeyOnR((byte) n);

        return NextAction.fcomp1;
    }

    /** *KEY SHIFT(k)* */
    private NextAction SETKST() {
        int[] ptr = new int[1];
        ptr[0] = mucInfo.getSrcCPtr();
        int n = (byte) msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0421"));
        mucInfo.setSrcCPtr(ptr[0]);
        work.siftDat = (byte) n;

        return NextAction.fcomp1;
    }

    /** *KEY SHIFT(K)* */
    public NextAction SETKS2() {
        char ch = mucInfo.getLin().getItem2().length() > (mucInfo.getSrcCPtr() + 1) ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr() + 1) : (char) 0;
        if (ch == 'D') {
            return SETKeyOnDelay();
        }

        int[] ptr = new int[1];
        ptr[0] = mucInfo.getSrcCPtr();
        int n = (byte) msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0422"));
        mucInfo.setSrcCPtr(ptr[0]);
        work.siftDa2 = (byte) n;

        return NextAction.fcomp1;
    }

    private NextAction SETKeyOnDelay() {
        if (CHCHK() != ChannelType.FM) {
            throw new MucException(rb.getString("E0518"), mucInfo.getRow(), mucInfo.getCol());
        }

        int[] ptr = new int[] {mucInfo.getSrcCPtr() + 1};
        int[] n = new int[4];

        for (int i = 0; i < 4; i++) {
            n[i] = (byte) msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0516"));
            if (n[i] > work.clock)
                throw new MucException(String.format(rb.getString("E0517"), n[i]), mucInfo.getLin().getItem1(), ptr[0]); // ERRORIF
            if (n[i] < 0)
                throw new MucException(String.format(rb.getString("E0518"), n[i]), mucInfo.getLin().getItem1(), ptr[0]); // ERRORIF
        }

        mucInfo.setSrcCPtr(ptr[0]);
        if (mucInfo.getDriverType() != MUCInfo.DriverType.DotNet)
            throw new MucException(rb.getString("E9998"), mucInfo.getRow(), mucInfo.getCol());
        msub.MWRITE(new MmlDatum(0xff), new MmlDatum(0xf6));
        msub.MWRITE(new MmlDatum(n[0]), new MmlDatum(n[1]));
        msub.MWRITE(new MmlDatum(n[2]), new MmlDatum(n[3]));

        return NextAction.fcomp1;
    }


    // *!*
    private NextAction COMOVR() {
        work.setCompEndCmdFlag(true);
        return NextAction.comovr;
    }

    private NextAction STRET() {
        return NextAction.comovr;
    }

    // **TEMPO(TIMER_B) SET**

    private NextAction TIMERB() {
        int[] ptr = new int[1];
        ptr[0] = mucInfo.getSrcCPtr();
        int n = (byte) msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0423"));
        mucInfo.setSrcCPtr(ptr[0]);

        return TIMEB2((byte) n);
    }

    private NextAction TIMEB2(byte n) {
        if (!mucInfo.isExtendFormat()) {
            mucInfo.getBufDst().set(work.datTbl - 1, new MmlDatum(n & 0xff)); // TIMER_B ﾆ ｱﾜｾﾙ
        }

        if (mucInfo.getDriverType() == MUCInfo.DriverType.DotNet) {
            return TIMEB2ex(n);
        }

        if (work.chipCh >= 3 && work.chipCh < 6) {
            writeWarning(rb.getString("W0412"), mucInfo.getRow(), mucInfo.getCol());
            return NextAction.fcomp1;
        }
        if ((n & 0xff) >= 253 && (n & 0xff) <= 255) {
            writeWarning(rb.getString("W0413"), mucInfo.getRow(), mucInfo.getCol());
        }

        if (work.chipIndex == 0) {
            msub.MWRITE(new MmlDatum(0xfa), new MmlDatum(0x26));
            msub.MWRIT2(new MmlDatum(n & 0xff));
        } else {
            msub.MWRITE(new MmlDatum(0xff), new MmlDatum(0xf7));
            msub.MWRITE(new MmlDatum(0x00), new MmlDatum(0x00)); // chips:0  port:0
            if (work.chipIndex != 4)
                msub.MWRITE(new MmlDatum(0x26), new MmlDatum(n & 0xff)); // adr:0x26  dat:n
            else
                msub.MWRITE(new MmlDatum(0x12), new MmlDatum(n & 0xff)); // adr:0x12  dat:n
        }
        return NextAction.fcomp1;
    }

    private NextAction TIMEB2ex(byte n) {
        //if (Work.chipIndex == 0) {
        //    if (Work.chipCh < 3 || Work.chipCh >= 6) {
        //        msub.MWRITE(new MmlDatum(0xfa), new MmlDatum(0x26));
        //        msub.MWRIT2(new MmlDatum(n));

        //        return NextAction.fcomp1;
        //    }
        //}

        msub.MWRITE(new MmlDatum(0xff), new MmlDatum(0xf7));
        msub.MWRITE(new MmlDatum(work.chipIndex), new MmlDatum(0x00)); // chips:0  port:0
        if (work.chipIndex != 4)
            msub.MWRITE(new MmlDatum(0x26), new MmlDatum(n & 0xff)); // adr:0x26  dat:n
        else
            msub.MWRITE(new MmlDatum(0x12), new MmlDatum(n & 0xff)); // adr:0x12  dat:n

        return NextAction.fcomp1;
    }

    // **NOIZE WAVE**

    private NextAction SETWAV() {

        ChannelType tp = CHCHK();
        if (tp != ChannelType.SSG) {
            throw new MucException(
                    rb.getString("E0424")
                    , mucInfo.getRow(), mucInfo.getCol());
        }

        int[] ptr = new int[1];
        ptr[0] = mucInfo.getSrcCPtr();
        int n = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0425"));
        mucInfo.setSrcCPtr(ptr[0]);

        msub.MWRITE(new MmlDatum(0xf8), new MmlDatum(n)); // COM OF 'w'

        return NextAction.fcomp1;
    }

    private NextAction SETSEVorSETC3SP() {
        char ch = mucInfo.getLin().getItem2().length() > (mucInfo.getSrcCPtr() + 1) ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr() + 1) : (char) 0;
        if (ch == 'X') {
            return SETCh3SpecialMode();
        }

        return SETSEV();
    }

    private NextAction SETMIXorSETPOR() {
        char ch = mucInfo.getLin().getItem2().length() > (mucInfo.getSrcCPtr() + 1) ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr() + 1) : (char) 0;
        if (ch == 'O') {
            return SETPOR();
        }

        return SETMIX();
    }

    // **MIX PORT**

    private NextAction SETMIX() {

        ChannelType tp = CHCHK();
        if (tp != ChannelType.SSG) {
            throw new MucException(
                    rb.getString("E0426")
                    , mucInfo.getRow(), mucInfo.getCol());
        }

        int[] ptr = new int[1];
        ptr[0] = mucInfo.getSrcCPtr();
        int n = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0427"));
        mucInfo.setSrcCPtr(ptr[0]);

        if (n == 1) {
            n = 8;
        } else if (n == 2) {
            n = 1;
        } else if (n == 3) {
            n = 0;
        } else if (n == 0) {
            n = 9;
        } else {
            throw new MucException(
                    rb.getString("E0428")
                    , mucInfo.getRow(), mucInfo.getCol());
        }

        msub.MWRITE(new MmlDatum(0xf7), new MmlDatum(n)); // COM OF 'P'
        return NextAction.fcomp1;
    }

    private NextAction SETCh3SpecialMode() {
        mucInfo.getAndIncSrcCPtr();
        char ch = mucInfo.getLin().getItem2().length() > (mucInfo.getSrcCPtr() + 1) ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr() + 1) : (char) 0;
        if (ch == 'O') {
            // EXON/EXOF
            return SETCh3SpecialMode_SW();
        }

        // EXnコマンド

        skipSpaceAndTab();

        // 数値の取得
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0534"));
        mucInfo.setSrcCPtr(ptr[0]);

        // 簡易チェック
        if (n < 1 || n > 4321) {
            // error
            throw new MucException(String.format(rb.getString("E0535"), n)
                    , mucInfo.getRow(), mucInfo.getCol());
        }

        String s = String.valueOf(n);
        int sw = 0;
        // 重複指定できるけどまあ良しｗ
        for (char c : s.toCharArray()) {
            if (c < '1' || c > '4') {
                // error
                throw new MucException(String.format(rb.getString("E0536"), c)
                        , mucInfo.getRow(), mucInfo.getCol());
            }
            int d = Integer.parseInt(String.valueOf(c));
            sw |= (1 << (d - 1));
        }

        // EXコマンドの発行
        msub.MWRITE(new MmlDatum(0xff), new MmlDatum(0xf8));
        msub.MWRITE(new MmlDatum(0x01), new MmlDatum(sw));

        return NextAction.fcomp1;
    }

    private NextAction SETCh3SpecialMode_SW() {
        mucInfo.getAndIncSrcCPtr();
        char ch = mucInfo.getLin().getItem2().length() > (mucInfo.getSrcCPtr() + 1) ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr() + 1) : (char) 0;
        int sw = -1;
        if (ch == 'N') {
            sw = 1;
        } else if (ch == 'F') {
            sw = 0;
        }
        if (sw == -1) {
            // error
            throw new MucException(rb.getString("E0537")
                    , mucInfo.getRow(), mucInfo.getCol());
        }
        mucInfo.getAndIncSrcCPtr();
        mucInfo.getAndIncSrcCPtr();

        // EXON/EXOFコマンドの発行
        msub.MWRITE(new MmlDatum(0xff), new MmlDatum(0xf8));
        msub.MWRITE(new MmlDatum(0x00), new MmlDatum(sw));

        return NextAction.fcomp1;
    }

    private NextAction SETPOR() {
        mucInfo.getAndIncSrcCPtr();
        char ch = mucInfo.getLin().getItem2().length() > (mucInfo.getSrcCPtr() + 1) ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr() + 1) : (char) 0;
        if (ch == 'S') {
            return SETPOR_SW();
        } else if (ch == 'R') {
            return SETPOR_RST();
        } else if (ch == 'L') {
            return SETPOR_TIM();
        }

        // スイッチ

        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = (byte) msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0530"));
        mucInfo.setSrcCPtr(ptr[0]);
        work.porSW = (byte) n;

        skipSpaceAndTab();
        ch = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr() ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr()) : (char) 0;
        if (ch != ',') {
            // error
            throw new MucException(String.format(rb.getString("E0529"))
                    , mucInfo.getRow(), mucInfo.getCol());
        }


        // デルタ

        ptr[0] = mucInfo.getSrcCPtr();
        n = (byte) msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0530"));
        mucInfo.setSrcCPtr(ptr[0]);
        work.porDelta = (byte) n;
        work.porOldNote = -1;

        skipSpaceAndTab();
        ch = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr() ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr()) : (char) 0;
        if (ch != ',') {
            // error
            throw new MucException(String.format(rb.getString("E0529"))
                    , mucInfo.getRow(), mucInfo.getCol());
        }


        // タイム

        ptr[0] = mucInfo.getSrcCPtr();
        n = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0530"));
        mucInfo.setSrcCPtr(ptr[0]);
        n = Math.max(n, 0);
        work.porTime = n;

        return NextAction.fcomp1;
    }

    private NextAction SETPOR_SW() {
        mucInfo.getAndIncSrcCPtr();

        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        byte n = (byte) msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0531"));
        mucInfo.setSrcCPtr(ptr[0]);
        work.porSW = n;

        return NextAction.fcomp1;
    }

    private NextAction SETPOR_RST() {
        mucInfo.getAndIncSrcCPtr();

        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        byte n = (byte) msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0532"));
        mucInfo.setSrcCPtr(ptr[0]);
        work.porDelta = n;
        work.porOldNote = -1;

        return NextAction.fcomp1;
    }

    private NextAction SETPOR_TIM() {
        mucInfo.getAndIncSrcCPtr();

        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0533"));
        mucInfo.setSrcCPtr(ptr[0]);
        n = Math.max(n, 0);
        work.porTime = n;

        return NextAction.fcomp1;
    }

    private NextAction SETPinPOR() {
        mucInfo.getAndIncSrcCPtr();
        work.porPin = 1;
        char ch = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr() ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr()) : (char) 0;
        if (ch == '_') {
            mucInfo.getAndIncSrcCPtr();
            work.porPin = 2;
        }

        return NextAction.fcomp1;
    }

    // **SOFT ENVELOPE**

    private NextAction SETSEV() {

        int[] ptr = new int[1];
        int n;
        ChannelType tp = CHCHK();

        if (mucInfo.getDriverType() == MUCInfo.DriverType.DotNet && tp == ChannelType.ADPCM) {
            ptr[0] = mucInfo.getSrcCPtr();
            n = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0430"));
            mucInfo.setSrcCPtr(ptr[0]);

            msub.MWRITE(new MmlDatum(0xff), new MmlDatum(0xfa)); // COM OF 'E'
            msub.MWRIT2(new MmlDatum(n)); // SET DATA ONLY

            return SETSE1(5, "E0512", "E0513"); // ﾉｺﾘ 5 PARAMETER
        }

        if (tp != ChannelType.SSG) {
            throw new MucException(
                    rb.getString("E0429")
                    , mucInfo.getRow(), mucInfo.getCol());
        }

        ptr[0] = mucInfo.getSrcCPtr();
        n = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0430"));
        mucInfo.setSrcCPtr(ptr[0]);

        msub.MWRITE(new MmlDatum(0xfa), new MmlDatum(n)); // COM OF 'E'

        return SETSE1(5, "E0512", "E0513"); // ﾉｺﾘ 5 PARAMETER
    }

    // **Q**

    private NextAction SETQLG() {
        int[] ptr = new int[1];
        ptr[0] = mucInfo.getSrcCPtr();
        int n = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0431"));
        mucInfo.setSrcCPtr(ptr[0]);
        work.quantize = n;

        msub.MWRITE(new MmlDatum(0xf3), new MmlDatum(n)); // COM OF 'q'

        return NextAction.fcomp1;
    }

    /** JUMP ADDRESS SET */
    private NextAction SETJMP() {
        mucInfo.getAndIncSrcCPtr();

        int hl = work.mData;
        int de = hl - work.muTop;
        int c = work.chipCh;

        if (!mucInfo.isExtendFormat()) {
            // Jump先となるアドレスをDATTBLに書き込む
            hl = work.datTbl + c * 4 + 2;
            mucInfo.getBufDst().set(hl, new MmlDatum(de));
            hl++;
            mucInfo.getBufDst().set(hl, new MmlDatum(de >> 8));
        } else {
            work.loopPoint[work.chipIndex][c][work.pageNow] = hl;
        }

        work.lCnt[work.chipIndex][c][work.pageNow] = work.tCnt[work.chipIndex][c][work.pageNow] + 1; // +1('L'ﾌﾗｸﾞﾉ ｶﾜﾘ)

        return NextAction.fcomp1;
    }

    // **SE DETUNE ﾉ ｾｯﾃｲ**

    private NextAction SETSE() {

        if (work.chipCh != 2) {
            // 3 Ch ｲｶﾞｲﾅﾗ ERROR
            throw new MucException(
                    rb.getString("E0432")
                    , mucInfo.getRow(), mucInfo.getCol());
        }

        int[] ptr = new int[1];
        String errCode_Fmt = "E0434";
        String errCode_Val = "E0435";
        int[] n = new int[4];

        ptr[0] = mucInfo.getSrcCPtr();
        n[0] = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0433"));
        mucInfo.setSrcCPtr(ptr[0]);
        if (mucInfo.getCarry()) throw new MucException(rb.getString(errCode_Fmt), mucInfo.getRow(), mucInfo.getCol());
        if (mucInfo.getErrSign()) throw new MucException(rb.getString(errCode_Val), mucInfo.getRow(), mucInfo.getCol());

        for (int i = 1; i < 4; i++) {
            char c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length() ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr()) : (char) 0;
            if (c != ',') throw new MucException(rb.getString(errCode_Fmt), mucInfo.getRow(), mucInfo.getCol());
            mucInfo.getAndIncSrcCPtr();
            ptr[0] = mucInfo.getSrcCPtr();
            n[i] = msub.readData(mucInfo.getLin(), /*ref*/ptr);
            mucInfo.setSrcCPtr(ptr[0]);
            if (mucInfo.getCarry())
                throw new MucException(rb.getString(errCode_Fmt), mucInfo.getRow(), mucInfo.getCol());
            if (mucInfo.getErrSign())
                throw new MucException(rb.getString(errCode_Val), mucInfo.getRow(), mucInfo.getCol());
        }

        // 値の範囲をチェック
        boolean is16bit = false;
        for (int i = 0; i < 4; i++) {
            if (n[i] == (byte) n[i]) continue;
            is16bit = true;
            break;
        }

        if (mucInfo.getDriverType() != MUCInfo.DriverType.DotNet && !is16bit) {
            msub.MWRIT2(new MmlDatum(0xf7)); // COM OF 'S'
            for (int i = 0; i < 4; i++)
                msub.MWRIT2(new MmlDatum(n[i]));
            // mucInfo.needNormalMucom = true; // 既存のmucomであることを求めるフラグ
        } else {
            // 既存のフォーマットを既に使っている時はエラーとする
            if (mucInfo.getDriverType() == MUCInfo.DriverType.normal) // && mucInfo.needNormalMucom)
                throw new MucException(rb.getString("E0527"), mucInfo.getRow(), mucInfo.getCol());

            msub.MWRIT2(new MmlDatum(0xf7)); // COM OF 'S'
            for (int i = 0; i < 4; i++) {
                msub.MWRIT2(new MmlDatum(n[i] & 0xff));
                msub.MWRIT2(new MmlDatum((n[i] >> 8) & 0xff));
            }
            // mucInfo.needNormalMucom = true;
        }

        return NextAction.fcomp1;
    }

    private NextAction SETSE1(int b, String errCodeFmt, String errCodeVal) {
        do {
            char c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length() ?
                    mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                    : (char) 0;
            if (c != ',') { // 0x2c
                throw new MucException(rb.getString(errCodeFmt), mucInfo.getRow(), mucInfo.getCol());
            }

            mucInfo.getAndIncSrcCPtr();
            int[] ptr = new int[] {mucInfo.getSrcCPtr()};
            int n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
            mucInfo.setSrcCPtr(ptr[0]);

            if (mucInfo.getCarry()) {
                // nondata ﾅﾗ error
                throw new MucException(rb.getString(errCodeFmt), mucInfo.getRow(), mucInfo.getCol());
            }

            if (mucInfo.getErrSign()) {
                throw new MucException(rb.getString(errCodeVal), mucInfo.getRow(), mucInfo.getCol());
            }

            msub.MWRIT2(new MmlDatum(n)); // set data only
            b--;
        } while (b != 0);

        return NextAction.fcomp1;
    }

    private NextAction SETLPE() {
        int[] ptr = new int[1];

        ptr[0] = mucInfo.getSrcCPtr();
        int rep = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0436"));
        mucInfo.setSrcCPtr(ptr[0]);
        if (rep < 0 || rep > 255) {
            writeWarning(String.format(rb.getString("W0416"), rep), mucInfo.getRow(), mucInfo.getCol());
        }

        msub.MWRIT2(new MmlDatum(0xf6)); // write com of loop
        msub.MWRIT2(new MmlDatum(rep)); // write loop co.
        int adr = work.mData;
        msub.MWRIT2(new MmlDatum(rep)); // write loop co. (spear)

        if (work.pointC < work.loopSp) {
            throw new MucException(rb.getString("E0437"), mucInfo.getRow(), mucInfo.getCol());
        }

        int loopStackPtr = work.pointC;
        work.pointC -= 10;

        int n = mucInfo.getBufLoopStack().get(loopStackPtr)
                + mucInfo.getBufLoopStack().get(loopStackPtr + 1) * 0x100;
        adr -= n;

        mucInfo.getBufDst().set(n, new MmlDatum(adr)); // RSKIP JP ADR
        mucInfo.getBufDst().set(n + 1, new MmlDatum(adr >> 8));

        int m = mucInfo.getBufLoopStack().get(loopStackPtr + 2)
                + mucInfo.getBufLoopStack().get(loopStackPtr + 3) * 0x100; // HL ﾊ LOOP ｦ ｶｲｼｼﾀ ｱﾄﾞﾚｽ

        int loopRetOfs = work.mData - m; // LOOP RET ADR OFFSET
        mucInfo.getBufDst().set(work.mData, new MmlDatum(loopRetOfs));
        mucInfo.getBufDst().set(work.mData + 1, new MmlDatum(loopRetOfs >> 8)); // WRITE RET ADR OFFSET
        work.mData += 2;

        m = mucInfo.getBufLoopStack().get(loopStackPtr + 4);
        m += mucInfo.getBufLoopStack().get(loopStackPtr + 5) * 0x100;
        if (m != 0) {
            int de = work.mData - 4;
            de -= m; // loopStackPtr + 4; // de as OFFSET
            mucInfo.getBufDst().set(m, new MmlDatum(de)); // loopStackPtr + 4, (byte)de);
            mucInfo.getBufDst().set(m + 1, new MmlDatum(de >> 8)); // loopStackPtr + 5, (byte)(de >> 8));
        }

        work.repCount--;

        int backupAdr = mucInfo.getBufLoopStack().get(loopStackPtr + 6)
                + mucInfo.getBufLoopStack().get(loopStackPtr + 7) * 0x100;
        backupAdr += work.tCnt[work.chipIndex][work.chipCh][work.pageNow] * (rep - 1);

        int ofs = mucInfo.getBufLoopStack().get(loopStackPtr + 8)
                + mucInfo.getBufLoopStack().get(loopStackPtr + 9) * 0x100;
        if (ofs == 0) {
            // '/'ﾊ ｶｯｺﾅｲﾆ ﾂｶﾜﾚﾃﾅｲ
            ofs = work.tCnt[work.chipIndex][work.chipCh][work.pageNow];
        }

        backupAdr += ofs;
        work.tCnt[work.chipIndex][work.chipCh][work.pageNow] = backupAdr;

        return NextAction.fcomp1;
    }

    /** LOOP START */
    private NextAction SETLPS() {

        mucInfo.getAndIncSrcCPtr();

        char c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length() ?
                mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr()) : (char) 0;
        if (c == '|') { // 0x2c
            return setPartReplaceStart();
        }

        msub.MWRIT2(new MmlDatum(0xf5)); // com of loopstart
        work.pointC += 10;

        mucInfo.getBufLoopStack().set(work.pointC + 0, (byte) work.mData); // SAVE REWRITE ADR
        mucInfo.getBufLoopStack().set(work.pointC + 1, (byte) (work.mData >> 8));

        work.mData += 2;

        mucInfo.getBufLoopStack().set(work.pointC + 2, (byte) work.mData); // SAVE LOOP START ADR
        mucInfo.getBufLoopStack().set(work.pointC + 3, (byte) (work.mData >> 8));
        mucInfo.getBufLoopStack().set(work.pointC + 4, (byte) 0);
        mucInfo.getBufLoopStack().set(work.pointC + 5, (byte) 0);
        mucInfo.getBufLoopStack().set(work.pointC + 6, (byte) work.tCnt[work.chipIndex][work.chipCh][work.pageNow]);
        mucInfo.getBufLoopStack().set(work.pointC + 7, (byte) (work.tCnt[work.chipIndex][work.chipCh][work.pageNow] >> 8));
        mucInfo.getBufLoopStack().set(work.pointC + 8, (byte) 0);
        mucInfo.getBufLoopStack().set(work.pointC + 9, (byte) 0);

        work.tCnt[work.chipIndex][work.chipCh][work.pageNow] = 0; // ﾄｰﾀﾙ ｸﾛｯｸ ｸﾘｱ

        work.repCount++;
        if (work.repCount > 16) {
            throw new MucException(rb.getString("E0438"), mucInfo.getRow(), mucInfo.getCol());
        }

        return NextAction.fcomp1;
    }

    // **REST**

    private NextAction SETRST() {

        if (mucInfo.getDriverType() == MUCInfo.DriverType.DotNet) {
            return SETRST_DotNET();
        }

        int[] ptr = new int[1];
        int kotae;

        mucInfo.getAndIncSrcCPtr();
        char c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;

        if (c == '%') // 0x25
        {
            mucInfo.getAndIncSrcCPtr();
            ptr[0] = mucInfo.getSrcCPtr();
            kotae = msub.readData(mucInfo.getLin(), /*ref*/ptr);
            if (kotae < 0 || kotae > 255) {
                writeWarning(String.format(rb.getString("W0403"), kotae), mucInfo.getRow(), mucInfo.getCol());
            }
            kotae = (byte) kotae;
            mucInfo.setSrcCPtr(ptr[0]);
            if (mucInfo.getCarry()) {
                kotae = (byte) work.count;
                mucInfo.decSrcCPtr();
                c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                        ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                        : (char) 0;
                if (c == '.') { // 0x2e
                    // 厳密には挙動が違いますが、この文法は使用できないことを再現させるため
                    throw new MucException(rb.getString("E0439"), mucInfo.getRow(), mucInfo.getCol());
                    //mucInfo.incSrcCPtr();
                    //kotae += (byte)(kotae >> 1); // /2
                }
            }
            if (mucInfo.getErrSign()) {
                throw new MucException(rb.getString("E0439"), mucInfo.getRow(), mucInfo.getCol());
            }
        } else {
            ptr[0] = mucInfo.getSrcCPtr();
            kotae = msub.readData(mucInfo.getLin(), /*ref*/ptr);
            if (kotae < 0 || kotae > 255) {
                writeWarning(String.format(rb.getString("W0403"), kotae), mucInfo.getRow(), mucInfo.getCol());
            }
            kotae = (byte) kotae;
            if (kotae != 0) kotae = (byte) (work.clock / kotae);
            mucInfo.setSrcCPtr(ptr[0]);
            if (mucInfo.getCarry()) {
                if (c == '^' || c == '&') {
                    writeWarning(rb.getString("W0401"), mucInfo.getRow(), mucInfo.getCol());
                }
                kotae = (byte) work.count;
                mucInfo.decSrcCPtr();
            }
            if (mucInfo.getErrSign()) {
                throw new MucException(rb.getString("E0439"), mucInfo.getRow(), mucInfo.getCol());
            }

            c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                    ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                    : (char) 0;
            if (c == '.') { // 0x2e
                mucInfo.getAndIncSrcCPtr();
                kotae += (kotae >> 1); // /2
                if (kotae < 0 || kotae > 255) {
                    writeWarning(String.format(rb.getString("W0403"), kotae), mucInfo.getRow(), mucInfo.getCol());
                }
                kotae = (byte) kotae;

                c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                        ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                        : (char) 0;
                if (c == '.') {
                    writeWarning(rb.getString("W0402"), mucInfo.getRow(), mucInfo.getCol());
                }
            }
        }

        work.tCnt[work.chipIndex][work.chipCh][work.pageNow] += kotae;

        if (work.befRst != 0) { // ｾﾞﾝｶｲｶｳﾝﾀ ﾜｰｸ(ﾌﾗｸﾞ)
            kotae += work.befRst;
            if (kotae < 0 || kotae > 255) {
                writeWarning(String.format(rb.getString("W0403"), kotae), mucInfo.getRow(), mucInfo.getCol());
            }
            kotae = (byte) kotae;
            work.mData--;
        }

        List<Object> args = new ArrayList<>();
        args.add(kotae);
        LinePos lp;

        while (kotae > 0x6f) {
            kotae -= 0x6f;
            lp = new LinePos(
                    mucInfo.document,
                    mucInfo.getFnSrcOnlyFile(),
                    mucInfo.getRow(),
                    mucInfo.getCol(),
                    mucInfo.getSrcCPtr() - mucInfo.getCol() + 1,
                    "",
                    Common.getChipName(work.chipIndex),
                    0,
                    Common.getChipNumber(work.chipIndex),
                    work.chipCh * Work.MAXPG + work.pageNow);
            msub.MWRIT2(new MmlDatum(MMLType.Rest, args, lp, 0b1110_1111));
        }
        work.befRst = kotae;
        lp = new LinePos(
                mucInfo.document,
                mucInfo.getFnSrcOnlyFile(),
                mucInfo.getRow(),
                mucInfo.getCol(),
                mucInfo.getSrcCPtr() - mucInfo.getCol() + 1,
                "",
                Common.getChipName(work.chipIndex),
                0,
                Common.getChipNumber(work.chipIndex),
                work.chipCh * Work.MAXPG + work.pageNow);
        msub.MWRIT2(new MmlDatum(MMLType.Rest, args, lp, kotae | 0b1000_0000)); // set rest flag

        work.latestNote = 2; // KUMA:チェック用(休符)

        return NextAction.fcomp12;
    }

    private NextAction SETRST_DotNET() {
        int[] ptr = new int[1];
        int kotae;

        mucInfo.getAndIncSrcCPtr();
        char c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;

        if (c == '%') { // 0x25
            mucInfo.getAndIncSrcCPtr();
            ptr[0] = mucInfo.getSrcCPtr();
            kotae = msub.readData(mucInfo.getLin(), /*ref*/ptr);
            mucInfo.setSrcCPtr(ptr[0]);
            if (mucInfo.getCarry()) {
                kotae = work.count;
                mucInfo.decSrcCPtr();

                //c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                //    ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                //    : (char)0;
                //if (c == '.') { // 0x2e
                //    // 厳密には挙動が違いますが、この文法は使用できないことを再現させるため
                //    throw new MucException(rb.getString("E0439"), mucInfo.getRow(), mucInfo.getCol());
                //}

                c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                        ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                        : (char) 0;
                int mod = kotae;
                while (c == '.') { // 0x2e
                    mucInfo.getAndIncSrcCPtr();
                    mod >>= 1;
                    kotae += mod; // /2
                    c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                            ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                            : (char) 0;
                }
            }
            if (mucInfo.getErrSign()) {
                throw new MucException(rb.getString("E0439"), mucInfo.getRow(), mucInfo.getCol());
            }
        } else {
            ptr[0] = mucInfo.getSrcCPtr();
            kotae = msub.readData(mucInfo.getLin(), /*ref*/ptr);
            if (kotae != 0) kotae = work.clock / kotae;
            mucInfo.setSrcCPtr(ptr[0]);
            if (mucInfo.getCarry()) {
                if (c == '^' || c == '&') {
                    writeWarning(rb.getString("W0401"), mucInfo.getRow(), mucInfo.getCol());
                }
                kotae = work.count;
                mucInfo.decSrcCPtr();
            }
            if (mucInfo.getErrSign()) {
                throw new MucException(rb.getString("E0439"), mucInfo.getRow(), mucInfo.getCol());
            }

            c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                    ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                    : (char) 0;
            int mod = kotae;
            while (c == '.') { // 0x2e
                mucInfo.getAndIncSrcCPtr();
                mod >>= 1;
                kotae += mod; // /2
                c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                        ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                        : (char) 0;
                //if (c == '.') {
                //    WriteWarning(rb.getString("W0402"), mucInfo.getRow(), mucInfo.getCol());
                //}
            }
        }

        work.tCnt[work.chipIndex][work.chipCh][work.pageNow] += kotae;

        if (work.befRst != 0) { // ｾﾞﾝｶｲｶｳﾝﾀ ﾜｰｸ(ﾌﾗｸﾞ)
            kotae += work.befRst;
            work.mData--;
        }

        List<Object> args = new ArrayList<>();
        args.add(kotae);
        LinePos lp;

        while (kotae > 0x6f) {
            kotae -= 0x6f;
            lp = new LinePos(
                    mucInfo.document,
                    mucInfo.getFnSrcOnlyFile(),
                    mucInfo.getRow(),
                    mucInfo.getCol(),
                    mucInfo.getSrcCPtr() - mucInfo.getCol() + 1,
                    "",
                    Common.getChipName(work.chipIndex),
                    0,
                    Common.getChipNumber(work.chipIndex),
                    work.chipCh * Work.MAXPG + work.pageNow);
            msub.MWRIT2(new MmlDatum(MMLType.Rest, args, lp, 0b1110_1111));
        }
        work.befRst = kotae;
        lp = new LinePos(
                mucInfo.document,
                mucInfo.getFnSrcOnlyFile(),
                mucInfo.getRow(),
                mucInfo.getCol(),
                mucInfo.getSrcCPtr() - mucInfo.getCol() + 1,
                "",
                Common.getChipName(work.chipIndex),
                0,
                Common.getChipNumber(work.chipIndex),
                work.chipCh * Work.MAXPG + work.pageNow);
        msub.MWRIT2(new MmlDatum(MMLType.Rest, args, lp, kotae | 0b1000_0000)); // set rest flag

        work.latestNote = 2; // KUMA:チェック用(休符)

        return NextAction.fcomp12;
    }

    private NextAction SETMOD() {
        int ix = 0;

        mucInfo.getAndIncSrcCPtr();
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
        mucInfo.setSrcCPtr(ptr[0]);
        if (mucInfo.getCarry()) {
            return SETMO2(); // nondata ﾅﾗ 2nd com prc
        }
        if (mucInfo.getErrSign()) {
            throw new MucException(rb.getString("E0440"), mucInfo.getRow(), mucInfo.getCol());
        }

        work.lfoData[ix] = 0;
        msub.MWRIT2(new MmlDatum(0xf4)); // COM OF 'M'
        msub.MWRIT2(new MmlDatum(0x00)); // 2nd COM
        work.lfoData[ix + 1] = (byte) (n & 0xff);
        msub.MWRIT2(new MmlDatum(n)); // SET DELAY

        // --ｶｳﾝﾀ ｾｯﾄ--
        // SETMO1:
        char c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;
        if (c != ',') { // 0x2c
            throw new MucException(rb.getString("E0441"), mucInfo.getRow(), mucInfo.getCol());
        }

        mucInfo.getAndIncSrcCPtr();
        ptr[0] = mucInfo.getSrcCPtr();
        n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
        mucInfo.setSrcCPtr(ptr[0]);

        if (mucInfo.getCarry()) {
            throw new MucException(rb.getString("E0441"), mucInfo.getRow(), mucInfo.getCol());
        }
        if (mucInfo.getErrSign()) {
            throw new MucException(rb.getString("E0440"), mucInfo.getRow(), mucInfo.getCol());
        }

        work.lfoData[ix + 2] = (byte) n;
        msub.MWRIT2(new MmlDatum(n)); // set data only
        // SET VECTOR
        c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;
        if (c != ',') { // 0x2c
            throw new MucException(rb.getString("E0441"), mucInfo.getRow(), mucInfo.getCol());
        }

        mucInfo.getAndIncSrcCPtr();
        ptr[0] = mucInfo.getSrcCPtr();
        n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
        mucInfo.setSrcCPtr(ptr[0]);

        if (mucInfo.getCarry()) {
            throw new MucException(rb.getString("E0441"), mucInfo.getRow(), mucInfo.getCol());
        }
        if (mucInfo.getErrSign()) {
            throw new MucException(rb.getString("E0440"), mucInfo.getRow(), mucInfo.getCol());
        }

        ChannelType tp = CHCHK();
        if (tp == ChannelType.SSG) {
            n = -n; // ssgの場合はdeの符号を反転
        }

        msub.MWRIT2(new MmlDatum(n & 0xff));
        work.lfoData[ix + 3] = (byte) n;
        msub.MWRIT2(new MmlDatum((n >> 8) & 0xff));
        work.lfoData[ix + 4] = (byte) (n >> 8);

        // SET DEPTH
        c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;
        if (c != ',') { // 0x2c
            throw new MucException(rb.getString("E0441"), mucInfo.getRow(), mucInfo.getCol());
        }

        ptr[0] = mucInfo.getSrcCPtr();
        n = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0442"));
        mucInfo.setSrcCPtr(ptr[0]);
        work.lfoData[ix + 5] = (byte) (n & 0xff);
        msub.MWRIT2(new MmlDatum(n)); // set data only

        c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;
        if (c != ',') { // 0x2c
            return NextAction.fcomp1;
        }

        ptr[0] = mucInfo.getSrcCPtr();
        msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0442"));
        mucInfo.setSrcCPtr(ptr[0]);

        return NextAction.fcomp1;
    }

    private NextAction SETMO2() {
        int iy;
        int[] ptr = new int[1];
        int n;

        //mucInfo.incSrcCPtr();
        // COM OF 'M'
        msub.MWRIT2(new MmlDatum(0xf4)); // COM ONLY
        if (work.getSecCom() == 'F') { // 0x46
            ptr[0] = mucInfo.getSrcCPtr();
            n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
            mucInfo.setSrcCPtr(ptr[0]);
            if (mucInfo.getCarry()) {
                throw new MucException(rb.getString("E0443"), mucInfo.getRow(), mucInfo.getCol());
            }
            if (mucInfo.getErrSign()) {
                throw new MucException(rb.getString("E0444"), mucInfo.getRow(), mucInfo.getCol());
            }

            if (n != 0 && n != 1) {
                throw new MucException(rb.getString("E0445"), mucInfo.getRow(), mucInfo.getCol());
            }
            n++; // SECOND COM
            work.lfoData[0] = (byte) (n & 0xff);
            msub.MWRIT2(new MmlDatum(n)); // 'MF0' or 'MF1'
            return NextAction.fcomp1;
        }

        // MO4:
        iy = 1;
        if (work.getSecCom() == 0x57) { // 'W'
            return MODP2(iy, (byte) 3, rb.getString("E0446")); // COM OF 'MW'
        }

        // M05:
        iy++;
        if (work.getSecCom() == 0x43) { // 'C'
            return MODP2(iy, (byte) 4, rb.getString("E0447")); // 'MC'
        }

        // M06:
        iy++;
        if (work.getSecCom() == 0x4c) { // 'L'
            // 'ML'
            work.lfoData[0] = 5;
            msub.MWRIT2(new MmlDatum(0x5));

            ptr[0] = mucInfo.getSrcCPtr();
            n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
            mucInfo.setSrcCPtr(ptr[0]);
            if (mucInfo.getCarry()) {
                throw new MucException(rb.getString("E0448"), mucInfo.getRow(), mucInfo.getCol());
            }
            if (mucInfo.getErrSign()) {
                throw new MucException(rb.getString("E0449"), mucInfo.getRow(), mucInfo.getCol());
            }
            work.lfoData[iy] = (byte) n;
            work.lfoData[iy + 1] = (byte) (n >> 8);
            work.lfoData[iy + 2] = 0;
            msub.MWRITE(new MmlDatum(n), new MmlDatum(n >> 8));

            return NextAction.fcomp1;
        }
        // M07:
        iy += 3;
        if (work.getSecCom() == 0x44) { // 'D'
            return MODP2(iy, (byte) 6, rb.getString("E0450")); // 'D'
        }
        // M08:
        if (work.getSecCom() != 0x54) { // 'T'
            throw new MucException(rb.getString("E0451"), mucInfo.getRow(), mucInfo.getCol());
        }

        ChannelType tp = CHCHK();

        // MT はFMではTL LFO , SSGでは音量LFOスイッチ

        // if (tp == ChannelType.SSG)
        // {
        //    throw new MucException(
        //        rb.getString("E0452")
        //        , mucInfo.getRow(), mucInfo.getCol());
        // }

        if (mucInfo.getDriverType() != MUCInfo.DriverType.DotNet)
            throw new MucException(rb.getString("E9998"), mucInfo.getRow(), mucInfo.getCol());

        ptr[0] = mucInfo.getSrcCPtr();
        n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
        mucInfo.setSrcCPtr(ptr[0]);
        if (mucInfo.getCarry()) {
            throw new MucException(rb.getString("E0453"), mucInfo.getRow(), mucInfo.getCol());
        }
        if (mucInfo.getErrSign()) {
            throw new MucException(rb.getString("E0454"), mucInfo.getRow(), mucInfo.getCol());
        }

        msub.MWRITE(new MmlDatum(7), new MmlDatum(n));
        if (n == 0 || tp == ChannelType.SSG) {
            return NextAction.fcomp1;
        }

        // M083:
        mucInfo.getAndIncSrcCPtr();
        ptr[0] = mucInfo.getSrcCPtr();
        n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
        mucInfo.setSrcCPtr(ptr[0]);
        if (mucInfo.getCarry()) {
            throw new MucException(rb.getString("E0455"), mucInfo.getRow(), mucInfo.getCol());
        }
        if (mucInfo.getErrSign()) {
            throw new MucException(rb.getString("E0456"), mucInfo.getRow(), mucInfo.getCol());
        }

        msub.MWRIT2(new MmlDatum(n));

        return NextAction.fcomp1;
    }

    // **PARAMETER SET**

    // IN: A<= COM No.

    public NextAction MODP2(int iy, byte a, String typ) {
        work.lfoData[0] = a;
        msub.MWRIT2(new MmlDatum(a & 0xff));

        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
        mucInfo.setSrcCPtr(ptr[0]);
        if (mucInfo.getCarry()) {
            throw new MucException(String.format(rb.getString("E0457"), typ), mucInfo.getRow(), mucInfo.getCol());
        }
        if (mucInfo.getErrSign()) {
            throw new MucException(String.format(rb.getString("E0458"), typ), mucInfo.getRow(), mucInfo.getCol());
        }

        work.lfoData[iy] = (byte) (n & 0xff);
        msub.MWRIT2(new MmlDatum(n));

        return NextAction.fcomp1;
    }

    private NextAction SETREG() {
        ChannelType tp = CHCHK();
        if (tp == ChannelType.SSG) {
            writeWarning(rb.getString("W0406"), mucInfo.getRow(), mucInfo.getCol());
        }

        mucInfo.getAndIncSrcCPtr();
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
        mucInfo.setSrcCPtr(ptr[0]);

        if (mucInfo.getCarry()) {
            mucInfo.decSrcCPtr();
            return SETR2();
        }
        if (mucInfo.getErrSign()) {
            throw new MucException(rb.getString("E0459"), mucInfo.getRow(), mucInfo.getCol());
        }

        if (mucInfo.getDriverType() == MUCInfo.DriverType.DotNet) {
            return SETR1ex(n);
        }

        if (0xb6 < n) {
            throw new MucException(rb.getString("E0460"), mucInfo.getRow(), mucInfo.getCol());
        }

        return SETR1(n);
    }

    private NextAction SETR1(int n) {
        msub.MWRITE(new MmlDatum(0xfa), new MmlDatum(n)); // COM OF 'y'

        char c = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr() ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr()) : (char) 0;
        if (c != ',') // 0x2c
        {
            throw new MucException(rb.getString("E0461"), mucInfo.getRow(), mucInfo.getCol());
        }

        mucInfo.getAndIncSrcCPtr();
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
        mucInfo.setSrcCPtr(ptr[0]);

        if (mucInfo.getCarry()) {
            throw new MucException(rb.getString("E0462"), mucInfo.getRow(), mucInfo.getCol());
        }
        if (mucInfo.getErrSign()) {
            throw new MucException(rb.getString("E0463"), mucInfo.getRow(), mucInfo.getCol());
        }

        msub.MWRIT2(new MmlDatum(n)); // SET DATA ONLY

        return NextAction.fcomp1;
    }

    private NextAction SETR1ex(int n) {
        int n1 = n, n2, n3, n4;

        // n2
        char c = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr() ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr()) : (char) 0;
        if (c != ',') { // 0x2c
            throw new MucException(rb.getString("E0461"), mucInfo.getRow(), mucInfo.getCol());
        }
        mucInfo.getAndIncSrcCPtr();
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        n2 = msub.readData(mucInfo.getLin(), /*ref*/ptr);
        mucInfo.setSrcCPtr(ptr[0]);
        if (mucInfo.getCarry()) {
            throw new MucException(rb.getString("E0462"), mucInfo.getRow(), mucInfo.getCol());
        }
        if (mucInfo.getErrSign()) {
            throw new MucException(rb.getString("E0463"), mucInfo.getRow(), mucInfo.getCol());
        }

        // n3
        c = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr() ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr()) : (char) 0;
        if (c != ',') { // 0x2c
            msub.MWRITE(new MmlDatum(0xfa), new MmlDatum(n1));
            msub.MWRIT2(new MmlDatum(n2));
            return NextAction.fcomp1;
        }
        mucInfo.getAndIncSrcCPtr();
        ptr[0] = mucInfo.getSrcCPtr();
        n3 = msub.readData(mucInfo.getLin(), /*ref*/ptr);
        mucInfo.setSrcCPtr(ptr[0]);
        if (mucInfo.getCarry()) {
            throw new MucException(rb.getString("E0462"), mucInfo.getRow(), mucInfo.getCol());
        }
        if (mucInfo.getErrSign()) {
            throw new MucException(rb.getString("E0463"), mucInfo.getRow(), mucInfo.getCol());
        }

        // n4
        c = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr() ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr()) : (char) 0;
        if (c != ',') // 0x2c
        {
            throw new MucException(rb.getString("E0461"), mucInfo.getRow(), mucInfo.getCol());
        }
        mucInfo.getAndIncSrcCPtr();
        ptr[0] = mucInfo.getSrcCPtr();
        n4 = msub.readData(mucInfo.getLin(), /*ref*/ptr);
        mucInfo.setSrcCPtr(ptr[0]);
        if (mucInfo.getCarry()) {
            throw new MucException(rb.getString("E0462"), mucInfo.getRow(), mucInfo.getCol());
        }
        if (mucInfo.getErrSign()) {
            throw new MucException(rb.getString("E0463"), mucInfo.getRow(), mucInfo.getCol());
        }

        msub.MWRITE(new MmlDatum(0xff), new MmlDatum(0xf7));
        msub.MWRITE(new MmlDatum(n1), new MmlDatum(n2));
        msub.MWRITE(new MmlDatum(n3), new MmlDatum(n4));

        return NextAction.fcomp1;
    }

    private NextAction SETR2() {
        // --yXX(ﾓｼﾞﾚﾂ),OpNo.,DATA--

        int i = 0;
        int[] ptr = new int[1];

        do {
            ptr[0] = mucInfo.getSrcCPtr();
            mucInfo.setCarry(msub.MCMP_DE(DMDAT[i], mucInfo.getLin(), /*ref*/ptr));
            if (mucInfo.getCarry()) {
                mucInfo.setSrcCPtr(ptr[0]);
                break;
            }
            i++;
        } while (i < 7);
        if (i == 7) {
            throw new MucException(rb.getString("E0464"), mucInfo.getRow(), mucInfo.getCol());
        }
        if (work.chipIndex == 4 && i == 6) {
            throw new MucException(rb.getString("E0464"), mucInfo.getRow(), mucInfo.getCol());
        }

        byte SETR9_VAL;
        if (work.chipIndex != 4) {
            SETR9_VAL = (byte) (i * 16);
        } else {
            SETR9_VAL = (byte) ((i * 0x20) + 0x40);
        }

        char c = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr() ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr()) : (char) 0;
        if (c != ',') // 0x2c
        {
            throw new MucException(rb.getString("E0461"), mucInfo.getRow(), mucInfo.getCol());
        }

        ptr[0] = mucInfo.getSrcCPtr();
        int n = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0465")); // ｵﾍﾟﾚｰﾀｰ No.
        mucInfo.setSrcCPtr(ptr[0]);

        if (n == 0 || n > 4) {
            throw new MucException(rb.getString("E0466"), mucInfo.getRow(), mucInfo.getCol());
        }

        // op2<->op3
        if (n == 3) {
            n = 2;
        } else if (n == 2) {
            n = 3;
        }

        n--;
        if (work.chipIndex != 4)
            n *= 4;
        else
            n *= 8;

        byte ch = (byte) work.chipCh;
        if (work.chipIndex != 4) {
            if (ch < 0 || (ch >= 3 && ch < 7) || ch >= 10) {
                throw new MucException(String.format(rb.getString("E0467"), (char) ('A' + ch))
                        , mucInfo.getRow(), mucInfo.getCol());
            }
            if (ch > 6) ch -= 7;

            n += ch; // Op*4+CH No.
            n += 0x30 + SETR9_VAL;
            return SETR1(n);
        }

        if (ch < 0 || ch > 7) {
            throw new MucException(String.format(rb.getString("E0467"), (char) ('A' + ch))
                    , mucInfo.getRow(), mucInfo.getCol());
        }

        n += ch; // Op*4+CH No.
        n += SETR9_VAL;
        return SETR1(n);
    }

    public String[] DMDAT = new String[] {
            "DM\0"
            , "TL\0"
            , "KA\0"
            , "DR\0"
            , "SR\0"
            , "SL\0"
            , "SE\0"
    };

    private NextAction SETTIE() {
        SETTI1();
        return NextAction.fcomp12;
    }

    public void SETTI1() {
        mucInfo.getAndIncSrcCPtr();
        work.tieFg = 0xfd;
        msub.MWRIT2(new MmlDatum(0xfd));
    }

    public NextAction SETTI2() {

        if (work.latestNote != 1) {
            throw new MucException(rb.getString("E0528"), mucInfo.getRow(), mucInfo.getCol());
        }

        SETTI1();
        byte a = work.getBeforeTone()[0];

        return FCOMP13(a);
    }


    private NextAction SETVUP() {
        if (work.volumeUDFlag != 0) {
            return SVD2();
        }
        return SVU2();
    }

    public NextAction SETVDW() {
        if (work.volumeUDFlag != 0) {
            return SVU2();
        }
        return SVD2();
    }

    public NextAction SVU2() {

        mucInfo.getAndIncSrcCPtr();
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
        if (mucInfo.getErrSign()) {
            throw new MucException(rb.getString("E0468"), mucInfo.getRow(), mucInfo.getCol());
        }

        mucInfo.setSrcCPtr(ptr[0]);

        if (mucInfo.getCarry()) {
            mucInfo.decSrcCPtr();
            n = 1; // ﾍﾝｶ 1
        }

        return SetRelativeVolume((byte) n);
    }

    public NextAction SVD2() {

        mucInfo.getAndIncSrcCPtr();
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
        if (mucInfo.getErrSign()) {
            throw new MucException(rb.getString("E0470"), mucInfo.getRow(), mucInfo.getCol());
        }
        mucInfo.setSrcCPtr(ptr[0]);

        if (mucInfo.getCarry()) {
            mucInfo.decSrcCPtr();
            n = 1; // ﾍﾝｶ 1
        }

        n = -n; // ')' ﾉ ﾊﾝﾀｲ ﾊ '('
        return SetRelativeVolume((byte) n);
    }

    private NextAction SetRelativeVolume(byte n) {
        work.volume += n;

        if (mucInfo.getDriverType() == MUCInfo.DriverType.DotNet) {
            if (work.chipIndex != 4 && work.chipCh == 6) { // KUMA:Rhythmの場合のみ特殊処理
                n = (byte) Math.min(Math.max(n, -63), 63);
                byte m = n;
                m &= 0x7f;
                if (work.getRhythmRelMode()) { // KUMA:とりあえず。
                    m |= 0x80;
                }

                msub.MWRITE(new MmlDatum(0xfb), new MmlDatum(m & 0xff));
                return NextAction.fcomp1;
            }
        }

        msub.MWRITE(new MmlDatum(0xfb), new MmlDatum(n & 0xff));
        return NextAction.fcomp1;
    }


    private NextAction SETODW() {
        SOD1();
        if (mucInfo.getCarry()) {
            throw new MucException(rb.getString("E0469"), mucInfo.getRow(), mucInfo.getCol());
        }

        return NextAction.fcomp1;
    }

    private NextAction SETOUP() {

        SOU1();
        if (mucInfo.getCarry()) {
            throw new MucException(rb.getString("E0471"), mucInfo.getRow(), mucInfo.getCol());
        }

        return NextAction.fcomp1;
    }

    public void SOU1() {
        mucInfo.getAndIncSrcCPtr();

        if (work.octaveUDFlag != 0 || (work.pcmInvert && work.octaveUDFlag == 0)) {
            SOD2();
            return;
        }
        SOU2();
    }

    public void SOU2() {
        if (work.octave == 7) {
            mucInfo.setCarry(true);
            return;
        }

        work.octave++;
        mucInfo.setCarry(false);
    }

    public void SOD1() {
        mucInfo.getAndIncSrcCPtr();

        if (work.octaveUDFlag != 0 || (work.pcmInvert && work.octaveUDFlag == 0)) {
            SOU2();
            return;
        }
        SOD2();
    }

    public void SOD2() {
        if (work.octave == 0) {
            mucInfo.setCarry(true);
            return;
        }
        work.octave--;
        mucInfo.setCarry(false);
    }


    private NextAction SETVOL() {
        ChannelType tp;
        char c;

        if (work.chipIndex != 4 && work.chipCh == 10) {
            return SETVOL_ADPCM();
        }

        mucInfo.getAndIncSrcCPtr();
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
        if (mucInfo.getErrSign()) {
            throw new MucException(rb.getString("E0472"), mucInfo.getRow(), mucInfo.getCol());
        }
        mucInfo.setSrcCPtr(ptr[0]);

        if (mucInfo.getCarry()) {
            if (mucInfo.getDriverType() == MUCInfo.DriverType.DotNet) {
                c = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr() - 1 ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr() - 1) : (char) 0;
                if (c == 'm') {
                    if (work.chipIndex != 4 && work.chipCh == 6) {
                        ptr[0] = mucInfo.getSrcCPtr();
                        n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
                        if (mucInfo.getErrSign()) {
                            throw new MucException(rb.getString("E0472"), mucInfo.getRow(), mucInfo.getCol());
                        }
                        mucInfo.setSrcCPtr(ptr[0]);
                        work.setRhythmRelMode(false);
                        if (n != 0) work.setRhythmRelMode(true);
                        return NextAction.fcomp1;
                    }

                    tp = CHCHK();
                    if (tp == ChannelType.FM) {
                        ptr[0] = mucInfo.getSrcCPtr();
                        n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
                        if (mucInfo.getErrSign() || n < 0 || n > 3) {
                            throw new MucException(rb.getString("E0472"), mucInfo.getRow(), mucInfo.getCol());
                        }
                        mucInfo.setSrcCPtr(ptr[0]);

                        // 実際の処理はドライバ任せだが、判定用に値を保持する
                        work.setFmVolMode(n);
                        msub.MWRITE(new MmlDatum(0xff), new MmlDatum(0xfb)); // COM OF 'vm'
                        msub.MWRIT2(new MmlDatum(n));
                        if (n == 1) // vm1の場合は更に20個データを読み込む
                        {
                            for (int i = 0; i < 20; i++) {
                                skipSpaceAndTab();
                                c = getMoji();
                                if (c != ',')
                                    throw new MucException(rb.getString("E0472"), mucInfo.getRow(), mucInfo.getCol());
                                ptr[0] = mucInfo.incAndGetSrcCPtr();
                                n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
                                n = Math.min(Math.max(n, 0), 127);
                                mucInfo.setSrcCPtr(ptr[0]);
                                if (mucInfo.getCarry())
                                    throw new MucException(rb.getString("E0472"), mucInfo.getRow(), mucInfo.getCol());
                                if (mucInfo.getErrSign())
                                    throw new MucException(rb.getString("E0472"), mucInfo.getRow(), mucInfo.getCol());

                                msub.MWRIT2(new MmlDatum(n));
                            }
                        }

                        return NextAction.fcomp1;
                    }
                }
            }

            n = work.volInt;
        }

        n = (byte) n;
        work.volume = n;
        work.volInt = n;

        skipSpaceAndTab();


        // initialize
        byte[] d = new byte[] {(byte) (127 - n), (byte) 255, (byte) 255, (byte) 255};
        int ind = 0;

        if (getMoji() == ',') {
            if (work.chipIndex == 4 || (work.chipIndex != 4 && work.chipCh != 6)) {
                ind++;
                // analyze
                while (getMoji() == ',') {
                    ptr[0] = mucInfo.incAndGetSrcCPtr();
                    n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
                    if (mucInfo.getCarry()) {
                        n = -1;
                        mucInfo.setSrcCPtr(--ptr[0]);
                        if (getMoji() != ',') {
                            break;
                        }
                    } else {
                        n = 127 - Math.min(Math.max(n, 0), 127);
                        mucInfo.setSrcCPtr(ptr[0]);
                        skipSpaceAndTab();
                        ptr[0] = mucInfo.getSrcCPtr();
                    }
                    if (ind < d.length) d[ind++] = (byte) n;
                    mucInfo.setSrcCPtr(ptr[0]);
                }
            }
        }

        tp = CHCHK();
        List<Object> args = new ArrayList<>();
        args.add(n);
        LinePos lp = new LinePos(
                mucInfo.document,
                mucInfo.getFnSrcOnlyFile(),
                mucInfo.getRow(), mucInfo.getCol(),
                mucInfo.getSrcCPtr() - mucInfo.getCol() + 1,
                work.currentPartType,
                work.currentChipName,
                0, work.chipIndex % 2, work.chipCh * Work.MAXPG + work.pageNow);

        if (work.chipIndex != 4) {
            if (work.chipCh != 6) {
                if (mucInfo.getDriverType() != MUCInfo.DriverType.DotNet
                        || (mucInfo.getDriverType() == MUCInfo.DriverType.DotNet && work.getFmVolMode() != 2)) {
                    n += work.tvOfs;
                    if (work.chipCh < 3 || work.chipCh > 6) {
                        n += 4;
                    }
                }

                if (ind == 0) {
                    msub.MWRITE(new MmlDatum(MMLType.Volume, args, lp, 0xf1), new MmlDatum(n)); // COM OF 'v'
                } else {
                    msub.MWRITE(new MmlDatum(MMLType.Volume, args, lp, 0xff), new MmlDatum(0xfb));
                    msub.MWRITE(new MmlDatum(0xff), new MmlDatum((d[0] & 0xff) + work.tvOfs));
                    msub.MWRITE(new MmlDatum((d[1] & 0xff) + work.tvOfs), new MmlDatum((d[2] & 0xff) + work.tvOfs));
                    msub.MWRIT2(new MmlDatum((d[3] & 0xff) + work.tvOfs));
                }
                return NextAction.fcomp1;
            }

            return SETVOL_Rhythm(n);
        } else {
            if (work.getFmVolMode() != 2) {
                n += work.tvOfs;
                n += 4;
            }

            if (ind == 0) {
                msub.MWRITE(new MmlDatum(MMLType.Volume, args, lp, 0xf1), new MmlDatum(n)); // COM OF 'v'
            } else {
                msub.MWRITE(new MmlDatum(MMLType.Volume, args, lp, 0xff), new MmlDatum(0xfb));
                msub.MWRITE(new MmlDatum(0xff), new MmlDatum((d[0] & 0xff) + work.tvOfs));
                msub.MWRITE(new MmlDatum((d[1] & 0xff) + work.tvOfs), new MmlDatum((d[2] & 0xff) + work.tvOfs));
                msub.MWRIT2(new MmlDatum((d[3] & 0xff) + work.tvOfs));
            }

            return NextAction.fcomp1;

        }
    }

    private NextAction SETVOL_Rhythm(int n) {
        List<Object> args = new ArrayList<>();
        args.add(n);
        LinePos lp = new LinePos(
                mucInfo.document,
                mucInfo.getFnSrcOnlyFile(),
                mucInfo.getRow(), mucInfo.getCol(),
                mucInfo.getSrcCPtr() - mucInfo.getCol() + 1,
                work.chipIndex / 2 == 0 ? "RHYTHM" : "ADPCM-A",
                Common.getChipName(work.chipIndex),
                0,
                Common.getChipNumber(work.chipIndex),
                work.chipCh * Work.MAXPG + work.pageNow);

        // -DRAM V. -
        n += work.tvOfs;

        if (mucInfo.getDriverType() == MUCInfo.DriverType.DotNet)
            n = Math.min(Math.max(n, 0), 63);

        msub.MWRITE(new MmlDatum(MMLType.Volume, args, lp, 0xf1), new MmlDatum(n));

        for (int i = 0; i < 6; i++) {
            char c = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr() ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr()) : (char) 0;
            if (c != ',') { // ','
                if (mucInfo.getDriverType() == MUCInfo.DriverType.DotNet) {
                    // KUMA:もしパラメータが一つだけの場合は個別指定したものとする
                    if (i == 0) {
                        MmlDatum m = mucInfo.getBufDst().get(work.mData - 1);
                        m.dat = Math.min(Math.max(m.dat, 0), 31);
                        m.dat |= 0x80; // KUMA:個別指定を意味するフラグをbit7にたてる
                        mucInfo.getBufDst().set(work.mData - 1, m);

                        MmlDatum m2 = mucInfo.getBufDst().get(work.mData - 2);
                        m2.args.set(0, m.dat & 0x7f); // KUMA:引数もクリップしたものに入れ替える
                        mucInfo.getBufDst().set(work.mData - 2, m2);

                        return NextAction.fcomp1;
                    }
                }

                throw new MucException(rb.getString("E0473"), mucInfo.getRow(), mucInfo.getCol());
            }

            int[] ptr = new int[] {mucInfo.getSrcCPtr()};
            n = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0474"));
            if (mucInfo.getErrSign())
                throw new MucException(rb.getString("E0475"), mucInfo.getRow(), mucInfo.getCol());
            mucInfo.setSrcCPtr(ptr[0]);

            msub.MWRIT2(new MmlDatum(n));
        }

        return NextAction.fcomp1;
    }

    private NextAction SETVOL_ADPCM() {

        // -PCMVOL-
        mucInfo.getAndIncSrcCPtr();
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
        if (mucInfo.getErrSign())
            throw new MucException(rb.getString("E0476"), mucInfo.getRow(), mucInfo.getCol());
        mucInfo.setSrcCPtr(ptr[0]);

        if (!mucInfo.getCarry()) {
            if (mucInfo.getErrSign()) {
                throw new MucException(rb.getString("E0476"), mucInfo.getRow(), mucInfo.getCol());
            }

            n = (byte) n;
            work.volume = n;
            work.volInt = n;
            n += work.tvOfs + 4;
            if ((n & 0xff) >= 246 && (n & 0xff) <= 251 && mucInfo.getVM() == 0) {
                writeWarning(rb.getString("W0411"), mucInfo.getRow(), mucInfo.getCol());
            }

            List<Object> args = new ArrayList<>();
            args.add(work.volume);
            LinePos lp = new LinePos(
                    mucInfo.document,
                    mucInfo.getFnSrcOnlyFile(),
                    mucInfo.getRow(), mucInfo.getCol(),
                    mucInfo.getSrcCPtr() - mucInfo.getCol() + 1,
                    work.currentPartType,
                    work.currentChipName,
                    0, work.chipIndex % 2, work.chipCh * 10 + work.pageNow);

            msub.MWRITE(new MmlDatum(MMLType.Volume, args, lp, 0xf1), new MmlDatum(n));
            return NextAction.fcomp1;
        }

        msub.MWRITE(new MmlDatum(0xff), new MmlDatum(0xf0));
        mucInfo.decSrcCPtr();
        char c = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr() ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr()) : (char) 0;
        if (c != 'm') { // vm command
            throw new MucException(String.format(rb.getString("E0477"), c), mucInfo.getRow(), mucInfo.getCol());
        }

        mucInfo.getAndIncSrcCPtr();
        ptr[0] = mucInfo.getSrcCPtr();
        n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
        if (mucInfo.getErrSign())
            throw new MucException(rb.getString("E0478"), mucInfo.getRow(), mucInfo.getCol());
        mucInfo.setSrcCPtr(ptr[0]);

        if (mucInfo.getCarry()) {
            throw new MucException(rb.getString("E0479"), mucInfo.getRow(), mucInfo.getCol());
        }

        if (mucInfo.getErrSign()) {
            throw new MucException(rb.getString("E0478"), mucInfo.getRow(), mucInfo.getCol());
        }

        mucInfo.setVM(n);
        msub.MWRIT2(new MmlDatum(n)); // SET DATA ONLY

        return NextAction.fcomp1;
    }

    private NextAction SETDT() {
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0480"));
        if (mucInfo.getErrSign())
            throw new MucException(rb.getString("E0481"), mucInfo.getRow(), mucInfo.getCol());
        mucInfo.setSrcCPtr(ptr[0]);

        List<Object> args = new ArrayList<>();
        args.add(n);

        ChannelType tp = CHCHK();
        if (tp == ChannelType.SSG) {
            n = -n;
        }

        LinePos lp = new LinePos(
                mucInfo.document,
                mucInfo.getFnSrcOnlyFile(),
                mucInfo.getRow(), mucInfo.getCol(),
                mucInfo.getSrcCPtr() - mucInfo.getCol() + 1,
                work.currentPartType,
                work.currentChipName,
                0, work.chipIndex % 2, work.chipCh * Work.MAXPG + work.pageNow);
        msub.MWRITE(new MmlDatum(MMLType.Detune, args, lp, 0xf2), new MmlDatum(n & 0xff)); // COM OF 'D'
        msub.MWRIT2(new MmlDatum((n >> 8) & 0xff));

        char c = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr()
                ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;
        mucInfo.getAndIncSrcCPtr();
        if (c != 0x2b) { // '+'
            c = (char) 0;
            mucInfo.decSrcCPtr();
        }
        msub.MWRIT2(new MmlDatum(c));

        return NextAction.fcomp1;
    }

    private NextAction SETLIZ() {
        char c = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr() + 1 ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr() + 1) : (char) 0;
        if (c == '%') { // 0x2c
            mucInfo.getAndIncSrcCPtr();
            return SETDCO();
        }

        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0482"));
        if (mucInfo.getErrSign())
            throw new MucException(rb.getString("E0483"));
        mucInfo.setSrcCPtr(ptr[0]);

        if (work.clock < n) {
            throw new MucException(String.format(rb.getString("E0484"), work.clock, n), mucInfo.getRow(), mucInfo.getCol());
        }

        work.count = work.clock / n;
        int cnt = work.count;
        int bf = 0;

        do {
            c = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr()
                    ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                    : (char) 0;
            if (c != 0x2e) // '.'
                break;

            mucInfo.getAndIncSrcCPtr();
            cnt >>= 1;
            bf += cnt;
        } while (true);

        work.count += bf;

        return NextAction.fcomp12;
    }

    private NextAction SETOCT() {

        mucInfo.getAndIncSrcCPtr();
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.readData(mucInfo.getLin(), /*ref*/ptr);
        if (mucInfo.getErrSign())
            throw new MucException(rb.getString("E0485"), mucInfo.getRow(), mucInfo.getCol());
        mucInfo.setSrcCPtr(ptr[0]);

        if (mucInfo.getCarry()) {
            n = work.octInt;
            n++;
        }

        n--;
        if (n >= 8) {
            // octave > 8?
            throw new MucException(rb.getString("E0486"), mucInfo.getRow(), mucInfo.getCol());
        }

        work.octave = n;
        work.octInt = n;

        return NextAction.fcomp1;
    }

    private NextAction SETCOL() {

        // 音色名による指定か
        mucInfo.getAndIncSrcCPtr();
        char c = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr()
                ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                : (char) 0;
        char w = '\0';
        if (c == '\"') {
            SETVN(); // 文字列による指定
            return NextAction.fcomp1;
        }
        if (c == 'I' || c == 'W') {
            w = c;
            mucInfo.getAndIncSrcCPtr();
        }

        //mucInfo.incSrcCPtr();
        //c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length() ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr()) : (char)0;
        //if (c == '=') {
        //    throw new MucException(rb.getString("E0487"), mucInfo.getRow(), mucInfo.getCol());
        //}
        //mucInfo.getSrcCPtr() -= 2;
        mucInfo.decSrcCPtr();

        // 数値を取得する
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0488"));
        if (mucInfo.getErrSign())
            throw new MucException(rb.getString("E0489"), mucInfo.getRow(), mucInfo.getCol());
        mucInfo.setSrcCPtr(ptr[0]);

        ChannelType tp = CHCHK();
        if (tp == ChannelType.SSG) {
            if (mucInfo.getSSGExtend() && w == 'I') {
                // 波形プリセット番号チェック
                if (n < 0 || n > 9) {
                    throw new MucException(String.format(rb.getString("Exxxx"), n), mucInfo.getRow(), mucInfo.getCol());
                }
            } else if (mucInfo.getSSGExtend() && w == 'W') {
                // ユーザー定義波形番号チェック
                if (n < 0 || n > 255) {
                    throw new MucException(String.format(rb.getString("Exxxx"), n), mucInfo.getRow(), mucInfo.getCol());
                }
                work.getUseSSGVoice().add(n);
            } else {
                // 音色番号チェック
                if (n < 0 || n > 15) {
                    throw new MucException(String.format(rb.getString("E0508"), n), mucInfo.getRow(), mucInfo.getCol());
                }
            }
        }

        c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length() ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr()) : (char) 0;
        if (c == '=') {
            return SETSSGPreset(n);
        }

        // 通常の音色指定

        if (tp == ChannelType.SSG) {
            return STCL5(n, w); // SSG
        }
        if (tp == ChannelType.FM) {
            // 音色番号チェック
            if (mucInfo.getDriverType() != MUCInfo.DriverType.DotNet) {
                if (n == 0 || n == 1) {
                    writeWarning(rb.getString("W0410"), mucInfo.getRow(), mucInfo.getCol());
                }
            }

            STCL2(n); // FM
            return NextAction.fcomp1;
        }

        return STCL72(n); // RHY&PCM
    }

    private NextAction SETSSGPreset(int n) {
        msub.MWRITE(new MmlDatum(0xfc), new MmlDatum(n)); // @= command

        int[] ptr = new int[1];
        ptr[0] = mucInfo.getSrcCPtr();
        n = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0509"));
        mucInfo.setSrcCPtr(ptr[0]);
        if (mucInfo.getErrSign()) {
            throw new MucException(rb.getString("E0510"), mucInfo.getRow(), mucInfo.getCol());
        }

        if (mucInfo.getDriverType() != MUCInfo.DriverType.DotNet)
            throw new MucException(rb.getString("E9998"), mucInfo.getRow(), mucInfo.getCol());

        msub.MWRIT2(new MmlDatum(n)); // 一つ目のパラメータをセット

        return SETSE1(5, "E0510", "E0511");
    }

    public void SETVN() {
        ChannelType tp = CHCHK();
        if (tp != ChannelType.FM) {
            throw new MucException(rb.getString("E0490"), mucInfo.getRow(), mucInfo.getCol());
        }

        mucInfo.getAndIncSrcCPtr();
        int voiBPtr = 0x20 + 26; // 0x6020 + 26;
        int num = 1;
        var sjis = mucInfo.getLin().getItem2().getBytes(Common.fileEncoding);

        do {
            int srcPtr = mucInfo.getSrcCPtr();
            int voiPtr = voiBPtr;

            int chcnt = 6;
            do {
                byte v = (mucInfo.getVoiceData() != null && mucInfo.getVoiceData().length > voiPtr) ? mucInfo.getVoiceData()[voiPtr] : (byte) 0;
                byte s = sjis[srcPtr];
                if (v != s) {
                    //Debug.printf("%06X v:%d s:%d", voiPtr, (char)mucInfo.voiceData[voiPtr], (char)mucInfo.getLin().getItem2().charAt(srcPtr));
                    break;
                }

                srcPtr++;
                voiPtr++;
                if (sjis[srcPtr] == 0x22) { // '"'
                    if (chcnt != 1 && mucInfo.getVoiceData()[voiPtr] != 0x20) {
                        throw new MucException(rb.getString("E0491"), mucInfo.getRow(), mucInfo.getCol());
                    }

                    mucInfo.setSrcCPtr(srcPtr + 1);
                    STCL2(num);
                    return;
                }

                chcnt--;
                if (chcnt == 0) {
                    throw new MucException(rb.getString("E0491"), mucInfo.getRow(), mucInfo.getCol());
                }
            } while (chcnt != 0);

            voiBPtr += 32;
            num++;

        } while (num != 256);

        throw new MucException(rb.getString("E0491"), mucInfo.getRow(), mucInfo.getCol());
    }

    public void STCL2(int num) { // FM
        num++;

        List<Object> args = new ArrayList<>();
        args.add(0); // dummy
        args.add(num - 1);
        LinePos lp = new LinePos(
                mucInfo.document,
                mucInfo.getFnSrcOnlyFile(),
                mucInfo.getRow(), mucInfo.getCol(),
                mucInfo.getSrcCPtr() - mucInfo.getCol() + 1,
                work.currentPartType,
                work.currentChipName,
                0, work.chipIndex % 2, work.chipCh * Work.MAXPG + work.pageNow);

        int voiceIndex = CCVC(num, mucInfo.getBufDefVoice()); // VOICE ｶﾞ ﾄｳﾛｸｽﾞﾐｶ?
        if (voiceIndex != -1) {
            msub.MWRITE(new MmlDatum(MMLType.Instrument, args, lp, 0xf0), new MmlDatum(voiceIndex - 1));
            return;
        }

        voiceIndex = CWVC(num, mucInfo.getBufDefVoice()); // WORK ﾆ ｱｷ ｶﾞ ｱﾙｶ?
        if (voiceIndex != -1) {
            msub.MWRITE(new MmlDatum(MMLType.Instrument, args, lp, 0xf0), new MmlDatum(voiceIndex - 1));
            return;
        }

        throw new MucException(rb.getString("E0492"), mucInfo.getRow(), mucInfo.getCol());
    }

    public NextAction STCL5(int num, char wav) {

        List<Object> args = new ArrayList<>();
        args.add(0); // dummy
        args.add(num);
        LinePos lp = new LinePos(
                mucInfo.document,
                mucInfo.getFnSrcOnlyFile(),
                mucInfo.getRow(), mucInfo.getCol(),
                mucInfo.getSrcCPtr() - mucInfo.getCol() + 1,
                "SSG",
                Common.getChipName(work.chipIndex),
                0,
                Common.getChipNumber(work.chipIndex),
                work.chipCh * Work.MAXPG + work.pageNow);

        if (mucInfo.getSSGExtend() && wav != '\0') {
            msub.MWRITE(new MmlDatum(MMLType.Instrument, args, lp, 0xff), new MmlDatum(0xf6));
            if (wav == 'I')
                msub.MWRIT2(new MmlDatum(num));
            else if (wav == 'W') {
                msub.MWRIT2(new MmlDatum(0xff));
                msub.MWRIT2(new MmlDatum(num));
                //if (!mucInfo.useSSGWavNum.Contains((byte)num))
                //    mucInfo.useSSGWavNum.Add((byte)num);
            }
            return NextAction.fcomp1;
        }

        msub.MWRITE(new MmlDatum(MMLType.Instrument, args, lp, 0xf0), new MmlDatum(num));

        if (work.getPsgMd() != 0) {
            return NextAction.fcomp1;
        }

        if (mucInfo.getSSGExtend() && wav != '\0') {
            return NextAction.fcomp1;
        }

        int HL = num * 16;
        msub.MWRIT2(new MmlDatum(0xf7));
        msub.MWRIT2(new MmlDatum(SSGLIB[HL] & 0xff));
        HL += 8;
        if (SSGLIB[HL] == 1) {
            return NextAction.fcomp1;
        }

        int DE = work.mData;
        mucInfo.getBufDst().set(DE, new MmlDatum(0xf4));
        DE++;
        for (int b = 0; b < 6; b++) {
            mucInfo.getBufDst().set(DE++, new MmlDatum(SSGLIB[HL + b] & 0xff));
            work.lfoData[b] = SSGLIB[HL + b];
        }
        work.mData = DE;

        return NextAction.fcomp1;
    }

    public NextAction STCL72(int num) {
        ChannelType tp = CHCHK();

        List<Object> args = new ArrayList<>();
        args.add(0); // dummy
        args.add(num);
        LinePos lp = new LinePos(
                mucInfo.document,
                mucInfo.getFnSrcOnlyFile(),
                mucInfo.getRow(), mucInfo.getCol(),
                mucInfo.getSrcCPtr() - mucInfo.getCol() + 1,
                work.chipIndex / 2 == 0
                        ? (tp == ChannelType.ADPCM ? "ADPCM" : "RHYTHM")
                        : (tp == ChannelType.ADPCM ? "ADPCM-B" : "ADPCM-A"),
                Common.getChipName(work.chipIndex),
                0,
                Common.getChipNumber(work.chipIndex),
                work.chipCh * Work.MAXPG + work.pageNow);

        msub.MWRITE(new MmlDatum(MMLType.Instrument, args, lp, 0xf0), new MmlDatum(num));

        if (tp == ChannelType.RHYTHM) work.setRhythmInstNum(num);
        if (tp == ChannelType.ADPCM) work.pcmFlag = 1;
        if (work.chipIndex > 1 && tp == ChannelType.RHYTHM) work.pcmFlag = 1;

        return NextAction.fcomp1;
    }

    public static final byte[] SSGLIB = new byte[] {
            8, 0, // ﾉｰﾏﾙ
            (byte) 255, (byte) 255, (byte) 255, (byte) 255, 0, (byte) 255, // E
            1, 0, 0, 0, 0, 0, 0, 0,

            8, 0, // ｺﾅﾐ(1)
            (byte) 255, (byte) 255, (byte) 255, (byte) 200, 0, 10,
            1, 0, 0, 0, 0, 0, 0, 0,

            8, 0, // ｺﾅﾐ(2)
            (byte) 255, (byte) 255, (byte) 255, (byte) 200, 1, 10,
            1, 0, 0, 0, 0, 0, 0, 0,

            8, 0, // ｺﾅﾐ+LFO(1)
            (byte) 255, (byte) 255, (byte) 255, (byte) 190, 0, 10,
            0, 16, 1, 25, 0, 4, 0, 0,

            8, 0, // ｺﾅﾐ+LFO(2)
            (byte) 255, (byte) 255, (byte) 255, (byte) 190, 1, 10,
            0, 16, 1, 25, 0, 4, 0, 0,

            8, 0, // ｺﾅﾐ(3)
            (byte) 255, (byte) 255, (byte) 255, (byte) 170, 0, 10,
            1, 0, 0, 0, 0, 0, 0, 0,

            // 5
            8, 0, // ｾｶﾞ ﾀｲﾌﾟ
            40, 70, 14, (byte) 190, 0, 15,
            0, 16, 1, 24, 0, 5, 0, 0,

            8, 0, // ｽﾄﾘﾝｸﾞ ﾀｲﾌﾟ
            120, 30, (byte) 255, (byte) 255, 0, 10,
            0, 16, 1, 25, 0, 4, 0, 0,

            8, 0, // ﾋﾟｱﾉ･ﾊｰﾌﾟ ﾀｲﾌﾟ
            (byte) 255, (byte) 255, (byte) 255, (byte) 225, 8, 15,
            1, 0, 0, 0, 0, 0, 0, 0,

            1, 0, // ｸﾛｰｽﾞ ﾊｲﾊｯﾄ
            (byte) 255, (byte) 255, (byte) 255, 1, (byte) 255, (byte) 255,
            1, 0, 0, 0, 0, 0, 0, 0,

            1, 0, // ｵｰﾌﾟﾝ ﾊｲﾊｯﾄ
            (byte) 255, (byte) 255, (byte) 255, (byte) 200, 8, (byte) 255,
            1, 0, 0, 0, 0, 0, 0, 0,

            // 10
            8, 0, // ｼﾝｾﾀﾑ･ｼﾝｾｷｯｸ
            (byte) 255, (byte) 255, (byte) 255, (byte) 220, 20, 8,
            0, 1, 1, 0x2C, 1, (byte) 0x0FF, 0, 0,

            8, 0, // UFO
            (byte) 255, (byte) 255, (byte) 255, (byte) 255, 0, 10,
            0, 1, 1, (byte) 0x70, (byte) 0x0FE, 4, 0, 0,

            8, 0, // FALLING
            (byte) 255, (byte) 255, (byte) 255, (byte) 255, 0, 10,
            0, 1, 1, 0x50, 0, (byte) 255, 0, 0,

            8, 0, // ﾎｲｯｽﾙ
            120, 80, (byte) 255, (byte) 255, 0, (byte) 255,
            0, 1, 1, 6, (byte) 0x0FF, 1, 0, 0,

            8, 0, // BOM!
            (byte) 255, (byte) 255, (byte) 255, (byte) 220, 0, (byte) 255,
            0, 1, 1, (byte) 0xB8, 0x0B, (byte) 255, 0, 0
    };


    /** VOICE ｶﾞ ﾄｳﾛｸｽﾞﾐｶ? */
    public int CCVC(int num, List<Integer> buf) {
        for (int b = 0; b < 256; b++) {
            if (num == buf.get(b)) {
                return b + 1; // VOICE ﾊ ｽﾃﾞﾆ ﾄｳﾛｸｽﾞﾐ
            }
        }

        return -1;
    }

    /** WORK ﾆ ｱｷ ｶﾞ ｱﾙｶ? */
    public int CWVC(int num, List<Integer> buf) {
        for (int b = 0; b < 256; b++) {
            if (0 == buf.get(b)) {
                // WORK ﾆ ｱｷ ｱﾘ
                buf.set(b, num);
                return b + 1;
            }
        }

        return -1; // 空き無し
    }

    public ChannelType CHCHK() {
        if (work.chipIndex == 4)
            return ChannelType.FM;

        if (work.chipCh >= 3 && work.chipCh < 6) {
            return ChannelType.SSG;
        }

        if (work.chipCh == 6) return ChannelType.RHYTHM;

        if (work.chipCh < 10) {
            return ChannelType.FM;
        }

        return ChannelType.ADPCM;
    }

    public enum ChannelType {
        FM, SSG, RHYTHM, ADPCM
    }

    private NextAction SETCLK() {
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.ERRT(mucInfo.getLin(), /*ref*/ptr, rb.getString("E0493"));
        if (mucInfo.getErrSign())
            throw new MucException(rb.getString("E0494"), mucInfo.getRow(), mucInfo.getCol());

        int len = ptr[0] - mucInfo.getSrcCPtr();
        mucInfo.setSrcCPtr(ptr[0]);

        if (mucInfo.isIDE()) {
            if (mucInfo.getDriverType() != MUCInfo.DriverType.DotNet)
                throw new MucException(rb.getString("E9998"), mucInfo.getRow(), mucInfo.getCol());

            List<Object> args = new ArrayList<>();
            args.add(n);
            LinePos lp = new LinePos(
                    mucInfo.document,
                    mucInfo.getFnSrcOnlyFile(),
                    mucInfo.getRow(),
                    mucInfo.getCol(),
                    len,
                    "",
                    Common.getChipName(work.chipIndex),
                    0,
                    Common.getChipNumber(work.chipIndex),
                    work.chipCh * Work.MAXPG + work.pageNow);
            msub.MWRITE(new MmlDatum(MMLType.ClockCounter, args, lp, 0xff), new MmlDatum(0xff));
        }

        work.clock = n;
        return NextAction.fcomp1;
    }

    int getErrorLine() {
        return mucInfo.getRow(); // errLin;
    }

    int compile() {
        work.chipIndex = 0;
        work.chipCh = 0;
        //work.MAXCH = 11;
        work.adrStc = 0;
        work.vpco = 0;

        work.setJpLine(-1);
        work.setJpCol(-1);

        mucInfo.setExtendFormat(checkExtendFormat());

        work.currentChipName = "YM2608";
        work.currentPartType = "FM";

        init();
        if (work.linCfg != 0) {
            return COMPI3();
        }
        for (int i = 0; i < Work.MAXChips; i++)
            for (int j = 0; j < Work.MAXCH; j++) {
                for (int pg = 0; pg < 10; pg++) {
                    work.tCnt[i][j][pg] = 0;
                    work.lCnt[i][j][pg] = 0;
                    work.loopPoint[i][j][pg] = -1;
                }
            }
        work.pcmFlag = 0;
        work.jClock = 0;
        work.setJChCom(null);

        return COMPI3();
    }

    public int COMPI3() {
        work.datTbl = work.muTop + 1;
        work.mData = work.muTop + 0x2f; // 11ch * 4byte + 3byte (DATTBLの大きさ?)

        // KUMA: ページ機能を使う場合は 0 番地からデータを配置する
        if (mucInfo.isExtendFormat()) work.mData = 0;

        work.backupMData = work.mData;
        work.repCount = 0;
        work.title = Work.titleFmt;
        work.latestNote = 0;

        if (!mucInfo.isExtendFormat()) {
            mucInfo.getBufDst().set(work.datTbl + 4 * work.chipCh + 0, new MmlDatum(work.mData - work.datTbl + 1));
            mucInfo.getBufDst().set(work.datTbl + 4 * work.chipCh + 1, new MmlDatum((work.mData - work.datTbl + 1) >> 8));
        }

        work.pointC = work.loopSp - 10; // LOOP ﾖｳ ｽﾀｯｸ

        // Z80.HL = 1; // TEXT START ADR
        CSTART();

        return mucInfo.getErrSign() ? -1 : 0;
    }

    public void CSTART() {
        Debug.printf(Level.FINEST, rb.getString("I0400"));
        work.maCfg = (byte) 0xff;
        COMPST(); // KUMA:先ずマクロの解析

        if (!mucInfo.isExtendFormat()) {
            mucInfo.getBufDst().set(work.datTbl + 4 * work.chipCh + 0, new MmlDatum(work.mData - work.datTbl + 1));
            mucInfo.getBufDst().set(work.datTbl + 4 * work.chipCh + 1, new MmlDatum((work.mData - work.datTbl + 1) >> 8));
        }

        mucInfo.setSrcCPtr(0);
        mucInfo.setSrcLinPtr(-1);

        CSTART2();
    }

    public void CSTART2() {
        do {
            work.maCfg = 0x00;

            // KUMA:ページ機能を使う場合は0番地からデータを配置する
            if (mucInfo.isExtendFormat()) work.mData = 0;

            work.setBufStartPtr(work.mData);

            COMPST();
            if (mucInfo.getErrSign()) return;

            CMPEND(); // ﾘﾝｸ ﾎﾟｲﾝﾀ = 0->BASIC END
            if (mucInfo.getErrSign()) return;

        } while (mucInfo.isExtendFormat() ? (work.chipIndex != Work.MAXChips) : (!work.isEnd()));
    }

    public void COMPST() {
        do {
            mucInfo.incSrcLinPtr();
            if (mucInfo.getBasSrc().size() <= mucInfo.getSrcLinPtr()) return;
            mucInfo.setLin(mucInfo.getBasSrc().get(mucInfo.getSrcLinPtr()));
            if (mucInfo.getLin().getItem2().length() < 1) continue;
            mucInfo.setSrcCPtr(0);

            Debug.printf(Level.FINEST, rb.getString("I0401"), mucInfo.getLin().getItem1());
            work.errLine = mucInfo.getLin().getItem1();

            if (work.maCfg == (byte) 0xff) {
                MACPRC(); // マクロの解析
                continue;
            }

            int checkPos = mucInfo.getSrcCPtr();

            boolean isFirst = true;

            // 先ずパート文字を探す
            boolean found = false;
            char c;
errCase:
            do {
                do {
                    c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                            ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                            : (char) 0;
                    if (c == 0 || c == ' ' || c == '\t') break errCase;

                    if (c >= '0' && c <= '9') {
                        if (isFirst) break errCase; // 最初に数字があった場合はパート表記と認めない
                        isFirst = false;
                        mucInfo.getAndIncSrcCPtr();
                        continue;
                    }
                    isFirst = false;

                    if ((c < (char) 0x41 || c > (char) 0x5a) // 大文字の範囲外
                            && (c < (char) 0x61 || c > (char) 0x7a) // 小文字の範囲外
                    ) break errCase; // アルファベット文字以外はパート表記と認めない

                    int trackNum = Work.getTrackNo(c);
                    if (trackNum < 0) break errCase; // パート文字以外のアルファベットは認めない

                    // 探しているパートかどうかチェック
                    if (trackNum == work.chipIndex * Work.MAXCH + work.chipCh) {
                        found = true;
                        mucInfo.getAndIncSrcCPtr();
                        break;
                    }

                    // 違う場合は次の文字を検索
                    mucInfo.getAndIncSrcCPtr();
                } while (true);

                // パート文字が見つからない場合は次の文字へ
                if (!found) continue;

                // ページ文字を探す
                found = false;
                boolean isPageFirst = true;
                do {
                    c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                            ? mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr())
                            : (char) 0;
                    if (c == 0) break;
                    if (c == ' ' || c == '\t') {
                        if (work.pageNow == 0 && isPageFirst) {
                            found = true;
                            c = (char) 0;
                            break;
                        }
                        break;
                    }

                    // もし、次のパート文字が並んでいた場合はページ0とする
                    int trackNum = Work.getTrackNo(c);
                    if (trackNum >= 0) {
                        if (isPageFirst) {
                            if (work.pageNow == 0) {
                                found = true;
                                c = (char) 0;
                            }
                        }
                        break;
                    }
                    isPageFirst = false;

                    // パート文字でもページ文字でもない場合はエラー
                    if (c < '0' || c > '9') {
                        break errCase;
                    }

                    // 目的のページ文字か
                    if (work.pageNow == c - '0') {
                        found = true;
                        c = (char) 0;
                        break;
                    }

                    // 違う場合は次の文字を検索
                    mucInfo.getAndIncSrcCPtr();
                } while (true);

                if (c == 0 || c == ' ' || c == '\t') break;
                // ページ文字が見つからない場合は次の文字へ
                if (!found) continue;

            } while (!found);

            if (!found) continue;

            Debug.printf(Level.FINEST, rb.getString("I0402"), work.chipIndex * Work.MAXCH + work.chipCh, work.pageNow);

            // パートの定義位置を調べる(単一パートの場合は常に 0 )
            work.setPartPos(CheckPartPos(checkPos));
            work.setPartReplaceSw(false);

            EnmFMCOMPrtn ret = FMCOMP(); // TO FM COMPILE
            if (mucInfo.getErrSign()) break;

            if (ret == EnmFMCOMPrtn.nextPart) {
                break;
            }

            // RECOM:
            // ﾘﾝｸﾎﾟｲﾝﾀ ｻｲｾｯﾄ
            // ﾂｷﾞ ﾉ ｷﾞｮｳﾍ
        } while (true);
    }

    private int CheckPartPos(int mpos) {
        char c;
        int partNum = 0;
        int pageNum;
        List<Tuple<Integer, Integer>> partList = new ArrayList<>();
        boolean partMode = true;
        boolean pageFirst = true;

        do {
            c = mpos < mucInfo.getLin().getItem2().length()
                    ? mucInfo.getLin().getItem2().charAt(mpos)
                    : (char) 0;

            if (partMode) {
                // パート文字解析

                partNum = Work.getTrackNo(c);
                if (partNum < 0) break;
                partMode = false;
                mpos++;
            } else {
                // ページ番号解析

                if (c < '0' || c > '9') {
                    if ((c == 0 || c == ' ' || c == '\t') && pageFirst) {
                        partList.add(new Tuple<>(partNum, 0));
                        break;
                    } else {
                        int part = Work.getTrackNo(c);
                        if (part < 0) break;

                        if (pageFirst)
                            partList.add(new Tuple<>(partNum, 0));
                        partMode = true;
                        pageFirst = true;
                    }
                    continue;
                }

                pageNum = c - '0';
                partList.add(new Tuple<>(partNum, pageNum));
                pageFirst = false;
                mpos++;
            }

        } while (true);

        int cnt = 0;
        for (Tuple<Integer, Integer> part : partList) {
            if (part.getItem1() == work.chipIndex * Work.MAXCH + work.chipCh && part.getItem2() == work.pageNow)
                return cnt;
            cnt++;
        }
        return -1;
    }

    public boolean checkExtendFormat() {
        if (mucInfo.getSSGExtend()) {
Debug.println("ssg extended");
            return true;
        }

        int srcLinPtr = -1;
        int srcCPtr;
        Tuple<Integer, String> lin;
        int f = 0x800;

        do {
            srcLinPtr++;
            if (mucInfo.getBasSrc().size() == srcLinPtr) break;
            lin = mucInfo.getBasSrc().get(srcLinPtr);
            if (lin.getItem2().length() < 1) continue;
            srcCPtr = 0;

            char c;

            do {
                c = srcCPtr < lin.getItem2().length()
                        ? lin.getItem2().charAt(srcCPtr++)
                        : (char) 0;
                if (c == 0 || c == ' ' || c == '\t') break;
                int trkNo = Work.getTrackNo(c);
                if (trkNo < 0) break;
                if (trkNo > 10)
                    return true; // L以降のパート名を使用している時はここでチェック完了

                // ここまできてパート文字発見
                // page番号を得る
                int pgNo = 0;
                c = srcCPtr < lin.getItem2().length()
                        ? lin.getItem2().charAt(srcCPtr++)
                        : (char) 0;
                if (c < '0' || c > '9') {
                    if (c == 0 || c == ' ' || c == '\t' || Work.getTrackNo(c) < 0) {
                        f |= 1 << pgNo;
                        break;
                    }
                }

                trkNo = Work.getTrackNo(c);
                if (c >= '0' && c <= '9') pgNo = c - '0';
                if (trkNo >= 0) srcCPtr--;
                f |= 1 << pgNo; // 見つけたページ番号のビットを立てる
                f &= 0x7ff;
            } while (true);
        } while (true);

        return ((f & 0x800) == 0 && (f & 0x7ff) != 0); // 全く使用していない 場合は ページ未使用 と判定する
    }

    public void MACPRC() {
        mucInfo.setRow(mucInfo.getLin().getItem1());
        mucInfo.setCol(mucInfo.getSrcCPtr() + 1);

        //z80.c = 0x2a; // '*'
        //z80.de = 0x0C000; // VRAMSTAC
        work.tst2Val = 0x0c000;
        MPC1();
    }

    public void MPC1() {
        mucInfo.setCarry(false);

        char ch = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr() ? mucInfo.getLin().getItem2().charAt(mucInfo.getAndIncSrcCPtr()) : (char) 0;
        if (ch != '#') {
            mucInfo.setCarry(true);
            return;
        }

        do {
            ch = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr() ? mucInfo.getLin().getItem2().charAt(mucInfo.getAndIncSrcCPtr()) : (char) 0;
        } while (ch == ' ' || ch == '\t');

        if (ch == 0 || ch == ';' || ch == '{') {
            mucInfo.setCarry(true);
            return;
        }
        if (ch != '*') {
            mucInfo.setCarry(true);
            return;
        }

        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        int n = msub.readData(mucInfo.getLin(), /*ref*/ptr); // MAC NO.
        mucInfo.setSrcCPtr(ptr[0]);

        if (mucInfo.getCarry()) {
            throw new MucException(rb.getString("E0495"), mucInfo.getRow(), mucInfo.getCol());
        }

        if (mucInfo.getErrSign()) {
            throw new MucException(rb.getString("E0496"), mucInfo.getRow(), mucInfo.getCol());
        }

        ch = mucInfo.getLin().getItem2().length() > mucInfo.getSrcCPtr() ? mucInfo.getLin().getItem2().charAt(mucInfo.getAndIncSrcCPtr()) : (char) 0;
        if (ch != '{') {
            throw new MucException(rb.getString("E0497"), mucInfo.getRow(), mucInfo.getCol());
        }

        if (work.maCfg == (byte) 0xff) {
            TOSTAC(n);
        }

        mucInfo.setCarry(false);
    }

    public void writeWarning(String wmsg, int row, int col) {
        work.compilerInfo.warningList.add(new Tuple3<>(row, col, wmsg));
        System.out.printf(rb.getString("E0300"), row, col, wmsg);
        System.out.println();
    }

    public void writeWarning(String wmsg) {
        work.compilerInfo.warningList.add(new Tuple3<>(-1, -1, wmsg));
        System.out.printf(rb.getString("E0300"), "-", "-", wmsg);
        System.out.println();
    }

    /** MACRO STAC */
    public void TOSTAC(int n) {
        if (mucInfo.getDriverType() != MUCInfo.DriverType.DotNet) {
            if (n > 0xff || n < 0) {
                writeWarning(rb.getString("W0400"), mucInfo.getRow(), mucInfo.getCol());
            }
            n &= 0xff;
        }

        n *= 2;
        if (work.tst2Val == 0xc000) {
            mucInfo.getBufMac().set(n, mucInfo.getSrcCPtr());
            mucInfo.getBufMac().set(n + 1, mucInfo.getSrcLinPtr() + 1);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public void CMPEND() {
        mucInfo.setErrSign(false);

        if (work.tCnt[work.chipIndex][work.chipCh][work.pageNow] == 0 && mucInfo.getBufDst().get(work.datTbl + 4 * work.chipCh + 2) != null) { // goto CMPE2;

            if (!mucInfo.isExtendFormat()) {
                mucInfo.getBufDst().set(work.datTbl + 4 * work.chipCh + 2, new MmlDatum(0));
                mucInfo.getBufDst().set(work.datTbl + 4 * work.chipCh + 3, new MmlDatum(0));
            }

        }
//CMPE2:
        mucInfo.getBufDst().set(work.mData++, new MmlDatum(0)); // set end mark = 0

        if (work.getCompEndCmdFlag()) {
            if (!mucInfo.isExtendFormat()) {
                // Jump先となるアドレスを0クリアする
                int hl = work.datTbl + work.chipCh * 4 + 2;
                mucInfo.getBufDst().set(hl, new MmlDatum(0));
                hl++;
                mucInfo.getBufDst().set(hl, new MmlDatum(0));
            } else {
                work.loopPoint[work.chipIndex][work.chipCh][work.pageNow] = -1;
            }
        }

        // KUMA: Result 表示用の bufCount をセット
        if (!mucInfo.isExtendFormat()) {
            work.getBufCount()[0][0][0] = work.mData - work.getBufStartPtr();
        } else {
            work.getBufCount()[work.chipIndex][work.chipCh][work.pageNow] = work.mData - work.getBufStartPtr();
        }

        // KUMA: ページ数を +1 する。10 ページ作成したら次の Ch にする。ヘッダには mml データの大きさを書き込む
        if (!mucInfo.isExtendFormat()) work.pageNow = 10; // ページ機能を利用しないなら一気に 10 ページ進める
        else work.pageNow++;

        if (work.pageNow == 10) {
            work.chipCh++; // Ch.=Ch.+ 1
            work.pageNow = 0;
        }

        // KUMA:
        //mucInfo.bufSize[Work.COMNOW][Work.pageNow] = (Work.mData - Work.datTbl + 1);

        if (work.pageNow != 0) {
            if (work.pageNow == 1) work.lastMData = work.mData;
            work.mData = work.backupMData; // KUMA: 次の Ch へ移る場合以外は、MDATA の位置を元に戻す
        } else {
            if (mucInfo.isExtendFormat()) work.mData = work.lastMData;

            if (!mucInfo.isExtendFormat()) {
                // ↓ TBLSET(); 相当
                mucInfo.getBufPage()[work.chipIndex][0][0].set(work.datTbl + 4 * work.chipCh + 0, new MmlDatum(work.mData - work.datTbl + 1));
                mucInfo.getBufPage()[work.chipIndex][0][0].set(work.datTbl + 4 * work.chipCh + 1, new MmlDatum((work.mData - work.datTbl + 1) >> 8));
            }

            if (work.chipCh == Work.MAXCH) {

                mucInfo.setBufDst(mucInfo.getBufPage()[mucInfo.isExtendFormat() ? work.chipIndex : 0][0][0]); // KUMA:最初のバッファへ切り替え

                CMPEN1();

                work.chipIndex++;
                work.chipCh = 0;
            }

            switch (work.chipIndex) {
            case 0:
            case 1:
                work.currentChipName = "YM2608";
                if (work.chipCh >= 0 && work.chipCh < 3) work.currentPartType = "FM";
                else if (work.chipCh >= 3 && work.chipCh < 6) work.currentPartType = "SSG";
                else if (work.chipCh == 6) work.currentPartType = "RHYTHM";
                else if (work.chipCh >= 7 && work.chipCh < 10) work.currentPartType = "FM";
                else if (work.chipCh == 10) work.currentPartType = "ADPCM";
                break;
            case 2:
            case 3:
                work.currentChipName = "YM2610B";
                if (work.chipCh >= 0 && work.chipCh < 3) work.currentPartType = "FM";
                else if (work.chipCh >= 3 && work.chipCh < 6) work.currentPartType = "SSG";
                else if (work.chipCh == 6) work.currentPartType = "ADPCM-A";
                else if (work.chipCh >= 7 && work.chipCh < 10) work.currentPartType = "FM";
                else if (work.chipCh == 10) work.currentPartType = "ADPCM-B";
                break;
            case 4:
                work.currentChipName = "YM2151";
                work.currentPartType = "FM";
                break;
            }

            // KUMA: 最後の Ch、ページまでコンパイルしたか
            if (work.chipIndex == (mucInfo.isExtendFormat() ? Work.MAXChips : 1)) {
                return;
            }

        }

        if (mucInfo.isExtendFormat()) {
            mucInfo.setBufDst(mucInfo.getBufPage()[work.chipIndex][work.chipCh][work.pageNow]); // KUMA:バッファの切り替え
        }

        work.backupMData = work.mData;

        // text start adr
        mucInfo.setSrcCPtr(0);
        mucInfo.setSrcLinPtr(-1);

        init();

        if (work.repCount != 0) {
            throw new MucException(rb.getString("E0498"), mucInfo.getRow(), mucInfo.getCol());
        }

        work.repCount = 0;
        work.tvOfs = 0;
        work.siftDat = 0;
        work.siftDa2 = 0;
        work.setKeyOnR(0);
        work.befRst = 0;
        work.tieFg = 0;
    }

    /**
     * パートごとに呼ばれる初期化処理
     */
    public void init() {
        work.lfoData[0] = 1;
        work.octave = 5;
        work.count = 24;
        work.clock = 128;
        work.volume = 0;
        work.octInt = 0; // 検証結果による値。おそらく実機では不定
        work.compilerInfo = new CompilerInfo();
        work.compilerInfo.jumpRow = -1;
        work.compilerInfo.jumpCol = -1;

        work.octaveUDFlag = 0;
        work.volumeUDFlag = 0;
        if (mucInfo.getInvert().equalsIgnoreCase("on")) {
            work.octaveUDFlag = 1;
            work.volumeUDFlag = 1;
        }
        work.pcmInvert = false;
        if (mucInfo.getPcmInvert().equalsIgnoreCase("on")) {
            work.pcmInvert = true;
        }
        work.quantize = 0; // KUMA: ポルタメントむけq値保存
        work.setRhythmRelMode(false);

        work.porSW = 0;
        work.porPin = 0;
        work.porDelta = 0;
        work.porOldNote = -1;
        work.porTime = 0;

        work.setFmVolMode(0);
        work.setCompEndCmdFlag(false);
    }

    public void CMPEN1() {
        if (work.repCount != 0) {
            throw new MucException(rb.getString("E0498"), mucInfo.getRow(), mucInfo.getCol());
        }

        work.endAdr = work.mData;
        work.otoDat = work.mData - work.muNum;
        work.setEnd(true);
        // START ADRは0固定なのでわざわざ表示を更新する必要なし
        //String h = Convert.ToString(0, 16); // START ADRは0固定

        VOICECONV1();
        expand.SSGTEXT();
    }

    public void VOICECONV1() {
        // --   25 BYTE VOICE DATA ﾉ ｾｲｾｲ   --
        if (!mucInfo.isExtendFormat()) {
            mucInfo.setUseOtoAdr(work.endAdr);
            work.endAdr++;
            work.vicAdr = work.endAdr;
        } else {
            work.endAdr = 0;
            mucInfo.setUseOtoAdr(work.endAdr);
            work.endAdr++;
            work.vicAdr = work.endAdr;
            mucInfo.setBufUseVoice(new AutoExtendList<>(MmlDatum.class));
            mucInfo.setBufDst(mucInfo.getBufUseVoice());
        }

        int dvPtr = 0; // Work.DEFVOICE;
        int B = 256; // FM:256色
        int useOto = 0;

        do {
            int vn = mucInfo.getBufDefVoice().get(dvPtr); // get voice number

            // DEBUG
            //vn = 1;

            if (vn == 0) {
                break;
            }

            vn += work.vpco;
            dvPtr++;
            vn--;
            expand.FVTEXT(vn); // KUMA: mml 中で音色定義されているナンバーかどうか探す

            byte[] bufVoi = mucInfo.getVoiceData();
            int vAdr;

            if (mucInfo.getCarry()) {
                // vAdr = GETADR(vn);
                vAdr = vn * 32 + work.fmlib;
                vAdr++; // HL= VOICE INDEX
                // KUMA:見つからなかった
            } else {
                vAdr = work.fmlib + 1;
                bufVoi = toByteArray(mucInfo.getMmlVoiceDataWork());
            }

            // mucomDotNET 独自
            if (bufVoi == null) {
                throw new MucException(rb.getString("E0519"), -1, -1);
            }

            work.getUsedFMVoiceNumber().add(vn);

            // 
            // TLチェック処理
            // 

            // 24:FB/CON
            // 7:CON bit0-2
            int wAlg = bufVoi[vAdr + 12 + 4 + 8] & 7;
            // 4-7:TL ope1-4
            int[] wTL = new int[] {
                    bufVoi[vAdr + 4] & 0x7f, // op1
                    bufVoi[vAdr + 6] & 0x7f, // op2 6がop2
                    bufVoi[vAdr + 5] & 0x7f, // op3
                    bufVoi[vAdr + 7] & 0x7f // op4
            };
            int[] wCar = new int[] {
                    8, 8, 8, 8, 10, 14, 14, 15
            };

            if (!mucInfo.getCarrierCorrection()) {
                for (int i = 0; i < 4; i++) {
                    if ((wCar[wAlg] & (1 << i)) == 0) continue;
                    if (wTL[i] != 0) {
                        writeWarning(String.format(rb.getString("W0408"), vn, i + 1, wAlg, wTL[0], wTL[1], wTL[2], wTL[3]));
                        break;
                    }
                }
            }

            // KUMA: 最初の 12byte 分の音色データをコピー

            int adr = work.endAdr;

            for (int i = 0; i < 12; i++) {
                mucInfo.getBufDst().set(adr, new MmlDatum(bufVoi[vAdr] & 0xff));
                adr++;
                vAdr++;
            }

            // KUMA: 次の 4byte 分の音色データを bit7(AMON) を立ててコピー
            for (int i = 0; i < 4; i++) {
                mucInfo.getBufDst().set(adr, new MmlDatum((bufVoi[vAdr] & 0xff) | 0b1000_0000)); // set amon flag
                adr++;
                vAdr++;
            }

            for (int i = 0; i < 9; i++) {
                mucInfo.getBufDst().set(adr, new MmlDatum(bufVoi[vAdr] & 0xff));
                adr++;
                vAdr++;
            }

            work.endAdr = adr;

            useOto++;
            B--;
        } while (B != 0);

        work.otoNum[work.chipIndex] = useOto; // ﾂｶﾜﾚﾃﾙ ｵﾝｼｮｸ ﾉ ｶｽﾞ
        mucInfo.getBufDst().set(mucInfo.getUseOtoAdr(), new MmlDatum(useOto));

        work.ssgDat = work.endAdr - work.muNum;

        int n = work.endAdr;
        int pos = 36;
        DispHex4(n, pos);

        n = work.endAdr - work.muNum;
        pos = 41;
        DispHex4(n, pos);

        work.escape = 1;
        //mucInfo.bufDefVoice.clear();
    }

    public void DispHex4(int n, int pos) {
        String h = "000" + Integer.toHexString(n); // pr end adr
        h = h.substring(h.length() - 4, h.length() - 4 + 4);
        byte[] data = h.getBytes(Common.fileEncoding);
        for (int i = 0; i < data.length; i++) mucInfo.getBufTitle().set(pos + i, data[i] & 0xff);
    }

    public enum EnmFMCOMPrtn {
        normal,
        error,
        nextPart
    }

    public EnmFMCOMPrtn FMCOMP() {
        char c;

        // Channel表記の後は必ず空白1文字が必要。
        // 以下のような表記はOK
        // Akumajho cde
        // A cde と同じ意
        do {
            c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                    ? mucInfo.getLin().getItem2().charAt(mucInfo.getAndIncSrcCPtr())
                    : (char) 0;
            if (c == 0) return EnmFMCOMPrtn.normal;
        } while (c != ' ' && c != '\t'); // ONE SPACE?

        mucInfo.setErrSign(false);
        NextAction ret = NextAction.fcomp1;

        do {
            switch (ret) {
            case comprc:
                ret = COMPRC();
                break;
            case fcomp1:
                ret = FCOMP1();
                break;
            case fcomp12:
                ret = FCOMP12();
                break;
//            case enmFCOMPNextRtn.fcomp13:
//                ret = FCOMP13();
//                break;
            }

            if (ret == NextAction.occuredERROR) {
                mucInfo.setErrSign(true);
                return EnmFMCOMPrtn.error;
            }
            if (ret == NextAction.comovr) {
                return EnmFMCOMPrtn.nextPart;
            }

        } while (ret != NextAction.NextLine);

        return EnmFMCOMPrtn.normal;
    }

    public NextAction FCOMP1() {
        work.befRst = 0;
        work.tieFg = 0;
        return NextAction.fcomp12;
    }

    public NextAction FCOMP12() {
        char c;
        do {
            c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length()
                    ? mucInfo.getLin().getItem2().charAt(mucInfo.getAndIncSrcCPtr())
                    : (char) 0;
            if (c == 0) // DATA END?
                return NextAction.NextLine; // ﾂｷﾞ ﾉ ｷﾞｮｳﾍ
        } while (c == ' ' || c == '\t'); // CHECK SPACE

        mucInfo.decSrcCPtr();
        work.setCom(msub.FMCOMC(c)); // COM CHECK

        mucInfo.setRow(mucInfo.getLin().getItem1());
        mucInfo.setCol(mucInfo.getSrcCPtr() + 1);

        if (mucInfo.getSkipPoint() != Common.EmptyPoint) {
            if (mucInfo.getSkipPoint().y == mucInfo.getRow()) {
                mucInfo.setSkipChannel(work.chipCh);

                if (mucInfo.getSkipPoint().x <= mucInfo.getCol()) {
                    SETTAG();
                    mucInfo.decSrcCPtr();
                    mucInfo.setSkipPoint(Common.EmptyPoint);
                    mucInfo.setSkipChannel(-1);
                }
            }

            if (mucInfo.getSkipPoint().y < mucInfo.getRow() && mucInfo.getSkipChannel() == work.chipCh) {
                SETTAG();
                mucInfo.decSrcCPtr();
                mucInfo.setSkipPoint(Common.EmptyPoint);
                mucInfo.setSkipChannel(-1);
            }
        }

        if (work.getCom() != 0) {
            return NextAction.comprc;
        }

        // 音符の解析
        byte note = msub.STTONE();
        if (mucInfo.getCarry()) {
            return NextAction.occuredERROR;
        }

        // ポルタメント制御
        if ((work.porSW != 0 && work.porPin == 0) || (work.porSW == 0 && work.porPin != 0)) {
            if (work.porOldNote != note) {
                return analyzePor(note);
            }
        }

        // ポルタメント無効時或いは、ポルタメントモード中の_,__コマンドも音程の引継ぎは行う
        work.porOldNote = note;
        work.porPin = 0; // _,__の効果は一度だけなのでここでリセット

        work.latestNote = 1; // KUMA: チェック用(音符を出力)

        // 音符が直前と同じで、タイフラグがたっているか
        if (note == work.getBeforeTone()[0] && work.tieFg != 0) {
            mucInfo.getAndIncSrcCPtr();
            return FCOMP13(note);
        } else {
            mucInfo.getAndIncSrcCPtr();
            FC162(note);
        }

        return NextAction.fcomp1;
    }

    private NextAction analyzePor(byte note) {
        mucInfo.getAndIncSrcCPtr();

        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        byte[] clk = new byte[1];
        int ret = msub.STLIZM(mucInfo.getLin(), /*ref*/ptr, /*out*/clk);
        if (ret < 0) {
            writeWarning(rb.getString("W0405"), mucInfo.getRow(), mucInfo.getCol());
        }
        mucInfo.setSrcCPtr(ptr[0]);
        clk[0] = FCOMP1X(clk[0]);

        // ポルタメントスイッチオフの状態で__によるピンポイントオンの場合は必ず初期化させる
        if (work.porSW == 0 && work.porPin == 2) {
            work.porOldNote = -1;
        }

        // 初期化
        if (work.porOldNote < 0) {
            int n = expand.ctone(note);
            n += work.porDelta;
            work.porOldNote = ((n / 12) << 4) | (n % 12);
        }

        int time = Math.min(work.porTime, clk[0]);

        // 本家の動作に準拠
        // int qtime = time - Work.quantize;

        // ポルタメント部に関わる場合のみ影響
        int qtime = time;
        if (clk[0] - work.quantize < work.porTime) {
            qtime = clk[0] - work.quantize;
        }

        qtime = Math.max(qtime, 1);

        // ポルタメント作成
        if (time > 0) {
            PortamentMainEx((byte) work.porOldNote, note, time, time - qtime);

            clk[0] = (byte) (clk[0] - time);

            // タイでつなげる
            if (clk[0] > 0) {
                work.tieFg = 0xfd;
                msub.MWRIT2(new MmlDatum(0xfd));
            }
        }

        // ポルタメント後、クロックが残っている場合は
        if (clk[0] > 0) {
            // 通常ノート作成
            FCOMP17(note, clk[0]);
            TCLKSUB(clk[0]);
        }

        // 直前ノート情報更新
        work.porOldNote = note;
        work.porPin = 0;

        return NextAction.fcomp1;
    }

    private void FC162(int note) {
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        byte[] clk = new byte[1];
        int ret = msub.STLIZM(mucInfo.getLin(), /*ref*/ptr, /*out*/clk); // LIZM SET
        if (ret < 0) {
            writeWarning(rb.getString("W0405"), mucInfo.getRow(), mucInfo.getCol());
        }
        mucInfo.setSrcCPtr(ptr[0]);
        clk[0] = FCOMP1X(clk[0]);
        TCLKSUB(clk[0]); // ﾄｰﾀﾙｸﾛｯｸ ｶｻﾝ

        FCOMP17(note, clk[0] & 0xff);

    }

    private void FC162p(int note) {
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        byte[] clk = new byte[1];
        int ret = msub.STLIZM(mucInfo.getLin(), /*ref*/ptr, /*out*/clk); // LIZM SET
        if (ret < 0) {
            writeWarning(rb.getString("W0405"), mucInfo.getRow(), mucInfo.getCol());
        }
        if ((clk[0] & 0xff) > 128 && mucInfo.getDriverType() != MUCInfo.DriverType.DotNet) {
            writeWarning(String.format(rb.getString("W0414"), clk[0]), mucInfo.getRow(), mucInfo.getCol());
        }
        mucInfo.setSrcCPtr(ptr[0]);
        clk[0] = FCOMP1X(clk[0]);
        TCLKSUB(clk[0]); // ﾄｰﾀﾙｸﾛｯｸ ｶｻﾝ
        FCOMP17(note, clk[0]);

    }

    private byte FC162p_clock(int note) {
        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        byte[] clk = new byte[1];
        int ret = msub.STLIZM(mucInfo.getLin(), /*ref*/ptr, /*out*/clk); // LIZM SET
        if (ret < 0) {
            writeWarning(rb.getString("W0405"), mucInfo.getRow(), mucInfo.getCol());
        }
        if ((clk[0] & 0xff) > 128 && mucInfo.getDriverType() != MUCInfo.DriverType.DotNet) {
            writeWarning(String.format(rb.getString("W0414"), (Object) clk), mucInfo.getRow(), mucInfo.getCol());
        }
        mucInfo.setSrcCPtr(ptr[0]);
        clk[0] = FCOMP1X(clk[0]);
        //TCLKSUB(clk[0]); // ﾄｰﾀﾙｸﾛｯｸ ｶｻﾝ
        //FCOMP17(note, clk[0]);
        return clk[0];
    }


    private NextAction FCOMP13(int note) {
        work.mData -= 3;

        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        byte[] clk = new byte[1];
        int ret = msub.STLIZM(mucInfo.getLin(), /*ref*/ptr, /*out*/clk);
        if (ret < 0) {
            writeWarning(rb.getString("W0405"), mucInfo.getRow(), mucInfo.getCol());
        }
        mucInfo.setSrcCPtr(ptr[0]);
        clk[0] = FCOMP1X(clk[0]);
        TCLKSUB(clk[0]);
        int n = clk[0];
        n += work.getBefCo();
        if (n > 0xff) {
            n -= 127;
            msub.MWRIT2(new MmlDatum(127));
            msub.MWRIT2(new MmlDatum(note));
            msub.MWRIT2(new MmlDatum(0xfd));
        }
        clk[0] = (byte) n;
        FCOMP17(note, clk[0]);

        return NextAction.fcomp1;
    }

    public byte FCOMP1X(byte clk) {
        int n = clk & 0xff;
        n += work.getKeyOnR();
        work.setKeyOnR((byte) -work.getKeyOnR());
        if (n < 0 || n > 255) {
            writeWarning(String.format(rb.getString("W0404"), n), mucInfo.getRow(), mucInfo.getCol());
        }
        clk = (byte) n;
        return clk;
    }


    private void TCLKSUB(int clk) {
        work.tCnt[work.chipIndex][work.chipCh][work.pageNow] += clk;
    }

    public void FCOMP17(int note, int clk) {
        // Mem.LD_8(BEFCO + 1, Z80.A); // ?

        if (clk < 128) {
            FCOMP2(note, clk);
            return;
        }

        List<Object> args = new ArrayList<>();
        args.add(note);
        args.add(clk);
        ChannelType tp = CHCHK();

        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        if (mucInfo.getLin().getItem2().length() > ptr[0] - 1)
            while (ptr[0] > 1 && (mucInfo.getLin().getItem2().charAt(ptr[0] - 1) == ' ' || mucInfo.getLin().getItem2().charAt(ptr[0] - 1) == '\t'))
                ptr[0]--;

        LinePos lp = new LinePos(
                mucInfo.document,
                mucInfo.getFnSrcOnlyFile(),
                mucInfo.getRow(),
                mucInfo.getCol(),
                ptr[0] - mucInfo.getCol() + 1,
                work.currentPartType,
                work.currentChipName,
                0, work.chipIndex % 2, work.chipCh * Work.MAXPG + work.pageNow);

        // --ｶｳﾝﾄ ｵｰﾊﾞｰ ｼｮﾘ--
        clk -= 127;
        msub.MWRIT2(new MmlDatum(MMLType.Note, args, lp, 127)); // first count
        msub.MWRIT2(new MmlDatum(note)); // tone data
        msub.MWRIT2(new MmlDatum(0xfd)); // com of count over(sound)
        work.setBeforeCo(clk); // restore second count
        msub.MWRIT2(new MmlDatum(clk));

        for (int i = 0; i < 8; i++)
            work.getBeforeTone()[8 - i] = work.getBeforeTone()[7 - i];

        work.getBeforeTone()[0] = (byte) note;
        msub.MWRIT2(new MmlDatum(note));
    }

    // --ﾉｰﾏﾙ ｼｮﾘ--
    public void FCOMP2(int note, int clk) {
        List<Object> args = new ArrayList<>();
        args.add(note);
        args.add(clk);
        ChannelType tp = CHCHK();

        int[] ptr = new int[] {mucInfo.getSrcCPtr()};
        if (mucInfo.getLin().getItem2().length() > ptr[0] - 1)
            while (ptr[0] > 1 && (mucInfo.getLin().getItem2().charAt(ptr[0] - 1) == ' ' || mucInfo.getLin().getItem2().charAt(ptr[0] - 1) == '\t'))
                ptr[0]--;

        LinePos lp = new LinePos(
                mucInfo.document,
                mucInfo.getFnSrcOnlyFile(),
                mucInfo.getRow(),
                mucInfo.getCol(),
                ptr[0] - mucInfo.getCol() + 1,
                work.currentPartType,
                work.currentChipName,
                0, work.chipIndex % 2, work.chipCh * Work.MAXPG + work.pageNow);

        mucInfo.getBufDst().set(work.mData++, new MmlDatum(MMLType.Note, args, lp, clk)); // SAVE LIZM
        work.setBeforeCo(clk);

        mucInfo.getBufDst().set(work.mData++, new MmlDatum(note)); // save tone

        for (int i = 0; i < 8; i++)
            work.getBeforeTone()[8 - i] = work.getBeforeTone()[7 - i];
        work.getBeforeTone()[0] = (byte) (note & 0xff);
    }

    public NextAction COMPRC() {
        Supplier<NextAction> act = COMTBL[work.getCom() - 1];
        if (act == null) {
Debug.println(Level.FINE, "no NextAction: " + (work.getCom() - 1));
            return NextAction.occuredERROR;
        }

        Debug.printf(Level.FINEST, act.toString());

        return act.get();
    }

    private NextAction setPartReplaceStart() {
        mucInfo.getAndIncSrcCPtr();
        work.setPartReplaceSw(true);

        int p = work.getPartPos();
        while (p > 0) {
            char c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length() ?
                    mucInfo.getLin().getItem2().charAt(mucInfo.getAndIncSrcCPtr()) : (char) 0;

            if (c == 0) {
                mucInfo.getAndIncSrcCPtr();
                work.setPartReplaceSw(false);
                return NextAction.fcomp1;
            }
            if (c == '|') { // 0x2c
                c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length() ?
                        mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr()) : (char) 0;
                if (c == ']') {
                    mucInfo.getAndIncSrcCPtr();
                    work.setPartReplaceSw(false);
                    return NextAction.fcomp1;
                }
                p--;
            }
        }

        return NextAction.fcomp1;
    }

    private NextAction setPartReplaceEnd() {
        // もし|の場合は最後の|]または行端までスキップ
        // ただし行端の場合はパートリプレイス処理続行
        while (true) {
            char c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length() ?
                    mucInfo.getLin().getItem2().charAt(mucInfo.getAndIncSrcCPtr()) : (char) 0;

            if (c == 0) {
                mucInfo.getAndIncSrcCPtr();
                work.setPartReplaceSw(false);
                break;
            }
            if (c == ']') {
                mucInfo.getAndIncSrcCPtr();
                work.setPartReplaceSw(false);
                break;
            }
            if (c == '|') { // 0x2c
                c = mucInfo.getSrcCPtr() < mucInfo.getLin().getItem2().length() ?
                        mucInfo.getLin().getItem2().charAt(mucInfo.getSrcCPtr()) : (char) 0;
                if (c == ']') {
                    mucInfo.getAndIncSrcCPtr();
                    work.setPartReplaceSw(false);
                    break;
                }
            }
        }

        return NextAction.fcomp1;
    }

    enum NextAction {
        Unknown,
        NextLine,
        comprc,
        occuredERROR,
        fcomp1,
        fcomp12,
        fcomp13,
        comovr
    }
}
