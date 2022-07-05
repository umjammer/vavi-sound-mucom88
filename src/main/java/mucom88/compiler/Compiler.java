package mucom88.compiler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;

import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import dotnet4j.util.compat.StringUtilities;
import dotnet4j.util.compat.Tuple;
import dotnet4j.util.compat.Tuple3;
import mucom88.common.MUCInfo;
import mucom88.common.Message;
import mucom88.common.MucException;
import mucom88.common.iEncoding;
import mucom88.common.MyEncoding;
import mucom88.common.Common;
import mucom88.compiler.PCMTool.AdpcmMaker;
import musicDriverInterface.CompilerInfo;
import musicDriverInterface.GD3Tag;
import musicDriverInterface.MmlDatum;
import musicDriverInterface.enmTag;
import musicDriverInterface.iCompiler;
import vavi.util.Debug;


public class Compiler implements iCompiler {
    private byte[] srcBuf = null;
    private MUCInfo mucInfo = new MUCInfo();
    private Work work = null;
    private Muc88 muc88 = null;
    private Msub msub = null;
    private Expand expand = null;
    private SMon smon = null;
    private byte[] voice;
    private byte[][] pcmdata = new byte[6][];
    private final List<Tuple<Integer, String>> basSrc = new ArrayList<>();
    private final List<MmlDatum> dat = new ArrayList<>();

    private String OutFileName;

    public String getOutFileName() {
        return OutFileName;
    }

    public void setOutFileName(String value) {
        OutFileName = value;
    }

    private Point skipPoint = Common.EmptyPoint;

    private iEncoding enc;
    private boolean isIDE = false;

    public enum EnmMUCOMFileType {
        unknown,
        MUB,
        MUC
    }

    public Compiler(iEncoding enc /*= null*/) {
        this.enc = enc != null ? enc : MyEncoding.Default();
    }

    public void Init() {
        //mucInfo = new MUCInfo();
        work = new Work();
        muc88 = new Muc88(work, mucInfo, enc);
        msub = new Msub(work, mucInfo, enc);
        expand = new Expand(work, mucInfo);
        smon = new SMon(mucInfo);
        muc88.msub = msub;
        muc88.expand = expand;
        msub.muc88 = muc88;
        expand.msub = msub;
        expand.smon = smon;
        expand.muc88 = muc88;
    }

    public MmlDatum[] compile(Stream sourceMML, Function<String, Stream> appendFileReaderCallback) {
        try {
            srcBuf = readAllBytes(sourceMML);
            mucInfo = getMUCInfo(srcBuf);
            mucInfo.setIDE(isIDE);
            mucInfo.setSkipPoint(skipPoint);
            mucInfo.setErrSign(false);
            voice = null;
            for (int i = 0; i < 6; i++) pcmdata[i] = null;

            try (Stream vd = appendFileReaderCallback.apply(StringUtilities.isNullOrEmpty(mucInfo.getVoice()) ? "voice.dat" : mucInfo.getVoice())) {
                voice = readAllBytes(vd);
            }

            String[] pcmDefaultFilenames = new String[] {
                    "mucompcm.bin",
                    "mucompcm_2nd.bin",
                    "mucompcm_3rd_B.bin",
                    "mucompcm_4th_B.bin",
                    "mucompcm_3rd_A.bin",
                    "mucompcm_4th_A.bin"
            };

            for (int i = 0; i < 6; i++) {
                if (mucInfo.getPcmAt()[i].size() > 0) {
                    pcmdata[i] = getPackedPCM(i, mucInfo.getPcmAt()[i], appendFileReaderCallback);
                }

                if (pcmdata[i] == null) {
                    try (Stream pd = appendFileReaderCallback.apply(StringUtilities.isNullOrEmpty(mucInfo.getPcm()[i])
                            ? pcmDefaultFilenames[i]
                            : mucInfo.getPcm()[i])) {
                        pcmdata[i] = readAllBytes(pd);
                    }
                }
            }

            mucInfo.setLines(storeBasicSource(srcBuf));
            mucInfo.setVoiceData(voice);
            mucInfo.setPcmData(pcmdata[0]);
            mucInfo.setBasSrc(basSrc);
            mucInfo.setSrcCPtr(0);
            mucInfo.setsrcLinPtr(-1);
            //Work.compilerInfo.jumpRow = -1;
            //Work.compilerInfo.jumpCol = -1;

            // MUCOM88 初期化
            int ret = muc88.COMPIL(); // vector 0xeea8

            // コンパイルエラー発生時は 0 以外が返る
            if (ret != 0) {
                int errLine = muc88.GetErrorLine();
                work.compilerInfo.errorList.add(
                        new Tuple3<>(
                                mucInfo.getRow()
                                , mucInfo.getCol()
                                , String.format(Message.get("E0100"), mucInfo.getRow(), mucInfo.getCol())
                        ));
                Debug.printf(Level.SEVERE, String.format(Message.get("E0100"), mucInfo.getRow(), mucInfo.getCol()));
                return null;
            }

            ret = saveMub();
            if (ret == 0) {
                return dat.toArray(MmlDatum[]::new);
            }
        } catch (MucException me) {
            if (work.compilerInfo == null) work.compilerInfo = new CompilerInfo();
            work.compilerInfo.errorList.add(new Tuple3<>(-1, -1, me.getMessage()));
            Debug.printf(Level.SEVERE, me.getMessage());
        } catch (Exception e) {
e.printStackTrace();
            if (work.compilerInfo == null) work.compilerInfo = new CompilerInfo();
            work.compilerInfo.errorList.add(new Tuple3<>(-1, -1, e.getMessage()));
            Debug.printf(Level.SEVERE, String.format(
                    Message.get("E0000")
                    , e.getMessage()
                    , Arrays.toString(e.getStackTrace())));
        }

        return null;
    }

    public boolean compile(Stream sourceMML, Stream destCompiledBin, Function<String, Stream> appendFileReaderCallback) {
        var dat = compile(sourceMML, appendFileReaderCallback);
        if (dat == null) {
            return false;
        }
        for (MmlDatum md : dat) {
            if (md == null) {
                destCompiledBin.writeByte((byte) 0);
            } else {
                destCompiledBin.writeByte((byte) md.dat);
            }
        }
        return true;
    }

    /**
     * ストリームから一括でバイナリを読み込む
     */
    private byte[] readAllBytes(Stream stream) {
        if (stream == null) return null;

        var buf = new byte[8192];
        try (var ms = new MemoryStream()) {
            while (true) {
                var r = stream.read(buf, 0, buf.length);
                if (r < 1) {
                    break;
                }
                ms.write(buf, 0, r);
            }
            return ms.toArray();
        }
    }

    public MUCInfo getMUCInfo(byte[] buf) {
        if (checkFileType(buf) != EnmMUCOMFileType.MUC) {
            throw new UnsupportedOperationException();
        }

        List<Tuple<String, String>> tags = getTagsFromMUC(buf);
        mucInfo.clear();
        for (Tuple<String, String> tag : tags) {
            switch (tag.getItem1()) {
            case "title":
                mucInfo.setTitle(tag.getItem2());
                break;
            case "composer":
                mucInfo.setComposer(tag.getItem2());
                break;
            case "author":
                mucInfo.setAuthor(tag.getItem2());
                break;
            case "comment":
                if (StringUtilities.isNullOrEmpty(mucInfo.getComment()))
                    mucInfo.setComment(tag.getItem2());
                else
                    mucInfo.addComment("\n" + tag.getItem2());
                break;
            case "mucom88":
                mucInfo.setMucom88(tag.getItem2());
                break;
            case "date":
                mucInfo.setDate(tag.getItem2());
                break;
            case "voice":
                mucInfo.setVoice(tag.getItem2());
                break;
            case "pcm":
                mucInfo.getPcm()[0] = tag.getItem2();
                break;
            case "pcm_2nd":
                mucInfo.getPcm()[1] = tag.getItem2();
                mucInfo.setDriverType(MUCInfo.DriverType.DotNet);
                break;
            case "pcm_3rd_b":
                mucInfo.getPcm()[2] = tag.getItem2();
                mucInfo.setDriverType(MUCInfo.DriverType.DotNet);
                break;
            case "pcm_4th_b":
                mucInfo.getPcm()[3] = tag.getItem2();
                mucInfo.setDriverType(MUCInfo.DriverType.DotNet);
                break;
            case "pcm_3rd_a":
                mucInfo.getPcm()[4] = tag.getItem2();
                mucInfo.setDriverType(MUCInfo.DriverType.DotNet);
                break;
            case "pcm_4th_a":
                mucInfo.getPcm()[5] = tag.getItem2();
                mucInfo.setDriverType(MUCInfo.DriverType.DotNet);
                break;
            case "@pcm":
                mucInfo.getPcmAt()[0].add(tag.getItem2());
                break;
            case "@pcm_2nd":
                mucInfo.getPcmAt()[1].add(tag.getItem2());
                mucInfo.setDriverType(MUCInfo.DriverType.DotNet);
                break;
            case "@pcm_3rd_b":
                mucInfo.getPcmAt()[2].add(tag.getItem2());
                mucInfo.setDriverType(MUCInfo.DriverType.DotNet);
                break;
            case "@pcm_4th_b":
                mucInfo.getPcmAt()[3].add(tag.getItem2());
                mucInfo.setDriverType(MUCInfo.DriverType.DotNet);
                break;
            case "@pcm_3rd_a":
                mucInfo.getPcmAt()[4].add(tag.getItem2());
                mucInfo.setDriverType(MUCInfo.DriverType.DotNet);
                break;
            case "@pcm_4th_a":
                mucInfo.getPcmAt()[5].add(tag.getItem2());
                mucInfo.setDriverType(MUCInfo.DriverType.DotNet);
                break;
            case "driver":
                mucInfo.setDriver(tag.getItem2());
                if (mucInfo.getDriver().equalsIgnoreCase("mucomdotnet")) {
                    mucInfo.setDriverType(MUCInfo.DriverType.DotNet);
                } else if (mucInfo.getDriver().equalsIgnoreCase("mucom88e")) {
                    mucInfo.setDriverType(MUCInfo.DriverType.E);
                } else if (mucInfo.getDriver().equalsIgnoreCase("mucom88em")) {
                    mucInfo.setDriverType(MUCInfo.DriverType.em);
                } else
                    mucInfo.setDriverType(MUCInfo.DriverType.normal);
                break;
            case "invert":
                mucInfo.setInvert(tag.getItem2());
                break;
            case "pcminvert":
                mucInfo.setPcmInvert(tag.getItem2());
                break;
            case "carriercorrection":
                String val = tag.getItem2().toLowerCase().trim();

                mucInfo.setCarrierCorrection(false);
                if (val.equals("yes") || val.equals("y") || val.equals("1") || val.equals("true") || val.equals("t")) {
                    mucInfo.setCarrierCorrection(true);
                }
                break;
            case "opmclockmode":
                String ocmval = tag.getItem2().toLowerCase().trim();

                mucInfo.setOpmClockMode(MUCInfo.OpmClockMode.normal);
                if (ocmval.equals("x68000") || ocmval.equals("x68k") || ocmval.equals("x68") || ocmval.equals("x") || ocmval.equals("40000") || ocmval.equals("x680x0")) {
                    mucInfo.setOpmClockMode(MUCInfo.OpmClockMode.X68000);
                }
                break;
            case "ssgextend":
                String ssgextval = tag.getItem2().toLowerCase().trim();

                mucInfo.setSSGExtend(false);
                if (ssgextval.equals("on") || ssgextval.equals("yes") || ssgextval.equals("y") || ssgextval.equals("1") || ssgextval.equals("true") || ssgextval.equals("t")) {
                    mucInfo.setSSGExtend(true);
                }
                break;
            case "opmpanreverse":
                String opmpanval = tag.getItem2().toLowerCase().trim();

                mucInfo.setOpmPanReverse(false);
                if (opmpanval.equals("on") || opmpanval.equals("yes") || opmpanval.equals("y") || opmpanval.equals("1") || opmpanval.equals("true") || opmpanval.equals("t")) {
                    mucInfo.setOpmPanReverse(true);
                }
                break;
            case "opna1rhythmmute":
                String rhythmmute = tag.getItem2().toLowerCase().trim();
                mucInfo.setopna1rhythmmute(0);
                if (rhythmmute.indexOf('b') > -1) mucInfo.oropna1rhythmmute(1);
                if (rhythmmute.indexOf('s') > -1) mucInfo.oropna1rhythmmute(2);
                if (rhythmmute.indexOf('c') > -1) mucInfo.oropna1rhythmmute(4);
                if (rhythmmute.indexOf('h') > -1) mucInfo.oropna1rhythmmute(8);
                if (rhythmmute.indexOf('t') > -1) mucInfo.oropna1rhythmmute(16);
                if (rhythmmute.indexOf('r') > -1) mucInfo.oropna1rhythmmute(32);
                break;
            case "opna2rhythmmute":
                String rhythmmute2 = tag.getItem2().toLowerCase().trim();
                mucInfo.setOpna2RhythmMute(0);
                if (rhythmmute2.indexOf('b') > -1) mucInfo.orOpna2RhythmMute(1);
                if (rhythmmute2.indexOf('s') > -1) mucInfo.orOpna2RhythmMute(2);
                if (rhythmmute2.indexOf('c') > -1) mucInfo.orOpna2RhythmMute(4);
                if (rhythmmute2.indexOf('h') > -1) mucInfo.orOpna2RhythmMute(8);
                if (rhythmmute2.indexOf('t') > -1) mucInfo.orOpna2RhythmMute(16);
                if (rhythmmute2.indexOf('r') > -1) mucInfo.orOpna2RhythmMute(32);
                break;
            case "opnb1adpcmamute":
                String adpcmamute1 = tag.getItem2().toLowerCase().trim();
                mucInfo.setOpnb1AdpcmAMute(0);
                if (adpcmamute1.indexOf('1') > -1) mucInfo.orOpnb1AdpcmAMute(1);
                if (adpcmamute1.indexOf('2') > -1) mucInfo.orOpnb1AdpcmAMute(2);
                if (adpcmamute1.indexOf('3') > -1) mucInfo.orOpnb1AdpcmAMute(4);
                if (adpcmamute1.indexOf('4') > -1) mucInfo.orOpnb1AdpcmAMute(8);
                if (adpcmamute1.indexOf('5') > -1) mucInfo.orOpnb1AdpcmAMute(16);
                if (adpcmamute1.indexOf('6') > -1) mucInfo.orOpnb1AdpcmAMute(32);
                break;
            case "opnb2adpcmamute":
                String adpcmamute2 = tag.getItem2().toLowerCase().trim();
                mucInfo.setOpnb2AdpcmAMute(0);
                if (adpcmamute2.indexOf('1') > -1) mucInfo.orOpnb2AdpcmAMute(1);
                if (adpcmamute2.indexOf('2') > -1) mucInfo.orOpnb2AdpcmAMute(2);
                if (adpcmamute2.indexOf('3') > -1) mucInfo.orOpnb2AdpcmAMute(4);
                if (adpcmamute2.indexOf('4') > -1) mucInfo.orOpnb2AdpcmAMute(8);
                if (adpcmamute2.indexOf('5') > -1) mucInfo.orOpnb2AdpcmAMute(16);
                if (adpcmamute2.indexOf('6') > -1) mucInfo.orOpnb2AdpcmAMute(32);
                break;
            }
        }

        if (mucInfo.getSSGExtend() && mucInfo.getDriverType() != MUCInfo.DriverType.DotNet) mucInfo.setSSGExtend(false);

        return mucInfo;
    }

    public CompilerInfo getCompilerInfo() {
        return work.compilerInfo;
    }


    private EnmMUCOMFileType checkFileType(byte[] buf) {
        if (buf == null || buf.length < 4) {
            return EnmMUCOMFileType.unknown;
        }

        if (buf[0] == 0x4d
                && buf[1] == 0x55
                && buf[2] == 0x43
                && buf[3] == 0x38) {
            return EnmMUCOMFileType.MUB;
        }

        return EnmMUCOMFileType.MUC;
    }

    private List<Tuple<String, String>> tags = new ArrayList<>();

    /**
     * mucからタグのリストを抽出する
     * @param buf muc(バイト配列、実態はsjisのテキスト)
     * @return tupleのリスト。
     * item1がタグ名。トリム、小文字化済み。
     * item2が値。トリム済み。
     */
    private List<Tuple<String, String>> getTagsFromMUC(byte[] buf) {
        var text = Arrays.stream(enc.getStringFromSjisArray(buf).split("\n"))
                .filter(x -> x.indexOf("#") == 0).toArray(String[]::new);
        if (tags != null) tags.clear();
        else tags = new ArrayList<>();

        for (String v : text) {
            try {
                int p = v.indexOf(' ');
                String tag = "";
                String ele = "";
                if (p >= 0) {
                    tag = v.substring(1, 1 + p).trim().toLowerCase();
                    ele = v.substring(p + 1).trim();
                    Tuple<String, String> item = new Tuple<String, String>(tag, ele);
                    tags.add(item);
                }
            } catch (Exception e) {
            }
        }

        return tags;
    }

    private int storeBasicSource(byte[] buf) {
        int line = 0;
        var text = enc.getStringFromSjisArray(buf).split("\n");

        basSrc.clear();
        for (String txt : text) {
            Tuple<Integer, String> d = new Tuple<>(line + 1, txt);
            basSrc.add(d);

            line++;
        }

        return line;
    }

    private int saveMub() {
        try {
            String msg;
            byte[] textLineBuf = new byte[80];
            for (int i = 0; i < work.title.length(); i++) textLineBuf[i] = (byte) work.title.charAt(i);

            int pcmflag = 0;
            int maxcount = 0;
            int mubsize = 0;
            StringBuilder tCount = new StringBuilder();
            StringBuilder lCount = new StringBuilder();
            StringBuilder bCount = new StringBuilder();

            work.compilerInfo.totalCount = new ArrayList<>();
            work.compilerInfo.loopCount = new ArrayList<>();
            work.compilerInfo.bufferCount = new ArrayList<>();

            boolean isExtendFormat = mucInfo.isExtendFormat(); // false;
            int bufferLength = 0;
            for (int i = 0; i < (isExtendFormat ? Work.MAXChips : 1); i++) {
                StringBuilder tc = new StringBuilder();
                StringBuilder lc = new StringBuilder();
                StringBuilder bc = new StringBuilder();
                for (int j = 0; j < Work.MAXCH; j++) {
                    if (!isExtendFormat) {
                        work.compilerInfo.formatType = "mub";
                        if (work.lcnt[i][j][0] != 0) {
                            work.lcnt[i][j][0] = work.tcnt[i][j][0] - (work.lcnt[i][j][0] - 1);
                        }
                        if (work.tcnt[i][j][0] > maxcount) maxcount = work.tcnt[i][j][0];
                        tCount.append(String.format("%s:%05d ", work.getTrackCharacterFromChipValue(i, j), work.tcnt[i][j][0]));
                        work.compilerInfo.totalCount.add(work.tcnt[i][j][0]);
                        lCount.append(String.format("%s:%05d ", work.getTrackCharacterFromChipValue(i, j), work.lcnt[i][j][0]));
                        work.compilerInfo.loopCount.add(work.lcnt[i][j][0]);
                        bCount.append(String.format("%s:$%04x ", work.getTrackCharacterFromChipValue(i, j), work.getbufCount()[i][j][0]));
                        work.compilerInfo.bufferCount.add(work.getbufCount()[i][j][0]);
                        if (work.getbufCount()[i][j][0] > 0xffff) {
                            throw new MucException(String.format(Message.get("E0700"), work.getTrackCharacterFromChipValue(i, j), Arrays.deepToString(work.getbufCount()[i])));
                        }
                    } else {
                        work.compilerInfo.formatType = "mupb";
                        int n = 0;
                        for (int pg = 0; pg < 10; pg++) {
                            if (work.lcnt[i][j][pg] != 0) {
                                work.lcnt[i][j][pg] = work.tcnt[i][j][pg] - (work.lcnt[i][j][pg] - 1);
                            }
                            if (work.tcnt[i][j][pg] > maxcount) maxcount = work.tcnt[i][j][pg];
                            work.compilerInfo.totalCount.add(work.tcnt[i][j][pg]);
                            work.compilerInfo.loopCount.add(work.lcnt[i][j][pg]);
                            if (work.getbufCount()[i][j][pg] > 1) {
                                tc.append(String.format("%s%s:%05d ", work.getTrackCharacterFromChipValue(i, j), pg, work.tcnt[i][j][pg]));
                                lc.append(String.format("%s%s:%05d ", work.getTrackCharacterFromChipValue(i, j), pg, work.lcnt[i][j][pg]));
                                bc.append(String.format("%s%s:$%04x ", work.getTrackCharacterFromChipValue(i, j), pg, work.getbufCount()[i][j][pg]));
                                bufferLength = work.getbufCount()[i][j][pg];
                                n++;
                                if (n == 10) {
                                    n = 0;
                                    tc.append("\n");
                                    lc.append("\n");
                                    bc.append("\n");
                                }
                                //if (j != 0) usePageFunction = true;
                            }
                        }
                        for (int pg = 0; pg < 10; pg++) {
                            work.compilerInfo.bufferCount.add(work.getbufCount()[i][j][pg]);
                            if (work.getbufCount()[i][j][pg] > 0xffff) {
                                throw new MucException(String.format(Message.get("E0700")
                                        , work.getTrackCharacterFromChipValue(i, j) + String.valueOf(pg)
                                        , Arrays.toString(work.getbufCount()[i][j])));
                            }
                        }
                    }
                }

                if (!isExtendFormat) {
                    if (tCount.length() > 2) tCount.append("\n");
                    if (lCount.length() > 2) lCount.append("\n");
                    if (bCount.length() > 2) bCount.append("\n");
                } else {
                    tCount.append(tc);
                    if (tc.length() > 2) tCount.append("\n");
                    lCount.append(lc);
                    if (lc.length() > 2) lCount.append("\n");
                    bCount.append(bc);
                    if (bc.length() > 2) bCount.append("\n");
                }
            }

            work.compilerInfo.jumpClock = work.JCLOCK;
            work.compilerInfo.jumpChannel = work.getJCHCOM();

            if (work.pcmFlag == 0) pcmflag = 2;
            msg = enc.getStringFromSjisArray(textLineBuf, 31, 4);
            int start = Integer.parseInt(msg, 16);
            msg = enc.getStringFromSjisArray(textLineBuf, 41, 4);
            int length = mucInfo.getBufDst().size();
            if (isExtendFormat) length = bufferLength;
            mubsize = length;

            Debug.printf("- mucom.NET -");
            Debug.printf("[ Total count ]\n" + tCount);
            Debug.printf("[ Loop count  ]\n" + lCount);
            if (isExtendFormat)
                Debug.printf("[ Buffer count  ]\n" + bCount);
            Debug.printf("");
            Debug.printf("#mucom type    : %s", mucInfo.getDriverType());
            Debug.printf("#MUB Format    : %s", isExtendFormat ? "Extend" : "Normal");
            Debug.printf("#Used FM voice : ");
            Debug.printf("#      @ count : %s ", work.getusedFMVoiceNumber().size());//, Work.OTONUM[0], Work.OTONUM[1], Work.OTONUM[2], Work.OTONUM[3], Work.OTONUM[4]);
            List<Integer> usedFMVoiceNumberList = new ArrayList<>(work.getusedFMVoiceNumber());
            Collections.sort(usedFMVoiceNumberList);
            Debug.printf("#      @ list  : %s ", String.join(" ", usedFMVoiceNumberList.stream().map(String::valueOf).toArray(String[]::new)));
            Debug.printf("#Data Buffer   : $%05x - $%05x ($%05x)", start, start + length - 1, length);
            Debug.printf("#Max Count     : %s", maxcount);
            Debug.printf("#MML Lines     : %s", mucInfo.getLines());
            Debug.printf("#Data          : %s", mubsize);

            return saveMusic(length, pcmflag, isExtendFormat);
        } catch (MucException me) {
            work.compilerInfo.errorList.add(new Tuple3<Integer, Integer, String>(-1, -1, me.getMessage()));
            Debug.printf(Level.SEVERE, me.getMessage());
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private int saveMusic(int length, int option, boolean isExtendFormat) {
        // 音楽データファイルを出力(コンパイルが必要)
        // option : 1   = #タグによるvoice設定を無視
        //          2   = PCM埋め込みをスキップ
        // (戻り値が0以外の場合はエラー)
        //       usePageFunc : ページ機能を使用しているか

        if (isExtendFormat) {
            return saveMusicExtendFormat(length, option);
        }

        int footsize;
        footsize = 1;//かならず1以上

        int pcmsize = (pcmdata[0] == null) ? 0 : pcmdata[0].length;
        boolean pcmuse = ((option & 2) == 0);
        pcmdata[0] = (!pcmuse ? null : pcmdata[0]);
        int pcmptr = (!pcmuse ? 0 : (32 + length + footsize));
        pcmsize = (!pcmuse ? 0 : pcmsize);
        if (pcmuse) {
            if (pcmdata[0] == null || pcmsize == 0) {
                pcmuse = false;
                pcmdata[0] = null;
                pcmptr = 0;
                pcmsize = 0;
            }
        }

        int dataOffset = 0x50;
        int dataSize = length;
        int tagOffset = length + 0x50;

        dat.clear();

        dat.add(new MmlDatum(0x4d));// M
        dat.add(new MmlDatum(0x55));// U
        dat.add(new MmlDatum(0x42));// B
        dat.add(new MmlDatum(0x38));// 8

        dat.add(new MmlDatum((byte) dataOffset));
        dat.add(new MmlDatum((byte) (dataOffset >> 8)));
        dat.add(new MmlDatum((byte) (dataOffset >> 16)));
        dat.add(new MmlDatum((byte) (dataOffset >> 24)));

        dat.add(new MmlDatum((byte) dataSize));
        dat.add(new MmlDatum((byte) (dataSize >> 8)));
        dat.add(new MmlDatum((byte) (dataSize >> 16)));
        dat.add(new MmlDatum((byte) (dataSize >> 24)));

        dat.add(new MmlDatum((byte) tagOffset));
        dat.add(new MmlDatum((byte) (tagOffset >> 8)));
        dat.add(new MmlDatum((byte) (tagOffset >> 16)));
        dat.add(new MmlDatum((byte) (tagOffset >> 24)));

        dat.add(new MmlDatum(0)); //tagdata size(dummy)
        dat.add(new MmlDatum(0));
        dat.add(new MmlDatum(0));
        dat.add(new MmlDatum(0));

        dat.add(new MmlDatum((byte) pcmptr)); // pcmdata ptr(32bit)
        dat.add(new MmlDatum((byte) (pcmptr >> 8)));
        dat.add(new MmlDatum((byte) (pcmptr >> 16)));
        dat.add(new MmlDatum((byte) (pcmptr >> 24)));

        dat.add(new MmlDatum((byte) pcmsize)); // pcmdata size(32bit)
        dat.add(new MmlDatum((byte) (pcmsize >> 8)));
        dat.add(new MmlDatum((byte) (pcmsize >> 16)));
        dat.add(new MmlDatum((byte) (pcmsize >> 24)));

        dat.add(new MmlDatum((byte) work.JCLOCK)); // JCLOCKの値(Jコマンドのタグ位置)
        dat.add(new MmlDatum((byte) (work.JCLOCK >> 8)));

        dat.add(new MmlDatum((byte) work.getJPLINE())); // jump line number
        dat.add(new MmlDatum((byte) (work.getJPLINE() >> 8)));

        dat.add(new MmlDatum(0)); // ext_flags(?)
        dat.add(new MmlDatum(0));

        dat.add(new MmlDatum(1)); // ext_system(?)

        dat.add(new MmlDatum(2)); // ext_target(?)

        dat.add(new MmlDatum(11)); // ext_channel_num
        dat.add(new MmlDatum(0));

        dat.add(new MmlDatum((byte) work.OTONUM[0])); // ext_fmvoice_num
        dat.add(new MmlDatum((byte) (work.OTONUM[0] >> 8)));

        dat.add(new MmlDatum(0)); // ext_player(?)
        dat.add(new MmlDatum(0));
        dat.add(new MmlDatum(0));
        dat.add(new MmlDatum(0));

        dat.add(new MmlDatum(0)); // pad1
        dat.add(new MmlDatum(0));
        dat.add(new MmlDatum(0));
        dat.add(new MmlDatum(0));

        for (int i = 0; i < 32; i++) {
            dat.add(new MmlDatum((byte) (int) mucInfo.getBufDefVoice().get(i)));
        }

        work.compilerInfo.jumpRow = -1;
        work.compilerInfo.jumpCol = -1;
        if (work.getJPLINE() >= 0) {
            Debug.printf("#Jump count [%s]. channelNumber[%s]", work.JCLOCK, work.getJCHCOM().get(0));
            Debug.printf("#Jump line [row:%s col:%s].", work.getJPLINE(), work.getJPCOL());
            work.compilerInfo.jumpRow = work.getJPLINE();
            work.compilerInfo.jumpCol = work.getJPCOL();
        }

        for (int i = 0; i < length; i++) dat.add(mucInfo.getBufDst().get(i));

        dat.set(dataOffset + 0, new MmlDatum(0)); // バイナリに含まれる曲データ数-1
        dat.set(dataOffset + 1, new MmlDatum((byte) work.OTODAT));
        dat.set(dataOffset + 2, new MmlDatum((byte) (work.OTODAT >> 8)));
        dat.set(dataOffset + 3, new MmlDatum((byte) work.ENDADR));
        dat.set(dataOffset + 4, new MmlDatum((byte) (work.ENDADR >> 8)));
        if (dat.get(dataOffset + 5) == null) {
            dat.set(dataOffset + 5, new MmlDatum(0)); // テンポコマンド(タイマーB)を未設定時nullのままになってしまうので、とりあえず値をセット
        }

        footsize = 0;

        boolean useDriverTAG = false;
        if (tags != null) {
            for (Tuple<String, String> tag : tags) {
                if (tag.getItem1().equals("driver")) useDriverTAG = true;
            }
        }

        // データサイズが64k超えていたらdotnet確定
        if (work.ENDADR - work.MU_NUM > 0xffff) {
            if (mucInfo.getDriverType() != MUCInfo.DriverType.DotNet) {
                // TODO
                return 1;
            }
        }

        if (!useDriverTAG && mucInfo.getDriverType() == MUCInfo.DriverType.DotNet) {
            if (tags == null) tags = new ArrayList<>();
            tags.add(new Tuple<>("driver", MUCInfo.dotNET));
        }

        if (tags != null) {
            for (Tuple<String, String> tag : tags) {
                if (tag.getItem1() != null && tag.getItem1().length() > 0 && tag.getItem1().charAt(0) == '*') continue;
                if (StringUtilities.isNullOrEmpty(tag.getItem1()) && !StringUtilities.isNullOrEmpty(tag.getItem2()) && tag.getItem2().trim().charAt(0) == '*')
                    continue;
                byte[] b = enc.GetSjisArrayFromString(String.format("#%s %s\n", tag.getItem1(), tag.getItem2()));
                footsize += b.length;
                for (byte bd : b) dat.add(new MmlDatum(bd));
            }
        }

        if (footsize > 0) {
            dat.add(new MmlDatum(0));
            dat.add(new MmlDatum(0));
            dat.add(new MmlDatum(0));
            dat.add(new MmlDatum(0));
            footsize += 4;

            dat.set(16, new MmlDatum((byte) footsize)); // tagdata size(32bit)
            dat.set(17, new MmlDatum((byte) (footsize >> 8)));
            dat.set(18, new MmlDatum((byte) (footsize >> 16)));
            dat.set(19, new MmlDatum((byte) (footsize >> 24)));
        } else {
            tags = null;
        }

        if (tags == null) {
            //クリア
            for (int i = 0; i < 8; i++) {
                dat.set(12 + i, new MmlDatum(0));
            }
        }

        if (pcmuse) {
            for (int i = 0; i < pcmsize; i++) dat.add(new MmlDatum(pcmdata[0][i]));
            if (pcmsize > 0) {
                pcmptr = 16 * 3 + 32 + length + footsize;
                dat.set(20, new MmlDatum((byte) pcmptr)); // pcmdata size(32bit)
                dat.set(21, new MmlDatum((byte) (pcmptr >> 8)));
                dat.set(22, new MmlDatum((byte) (pcmptr >> 16)));
                dat.set(23, new MmlDatum((byte) (pcmptr >> 24)));
            }
        }

        return 0;
    }

    private int saveMusicExtendFormat(int length, int option) {

        dat.clear();

        //固定長ヘッダー情報　作成

        dat.add(new MmlDatum(0x6d)); // m
        dat.add(new MmlDatum(0x75)); // u
        dat.add(new MmlDatum(0x50)); // P
        dat.add(new MmlDatum(0x62)); // b

        dat.add(new MmlDatum(0x30)); // 0
        dat.add(new MmlDatum(0x31)); // 1
        dat.add(new MmlDatum(0x30)); // 0
        dat.add(new MmlDatum(0x30)); // 0

        dat.add(new MmlDatum(0x05)); // 可変長ヘッダー情報の数。
        dat.add(new MmlDatum(Work.MAXChips)); // 使用する音源の数(0～)

        dat.add(new MmlDatum(Work.MAXCH * Work.MAXChips)); // 使用するパートの総数(0～)
        dat.add(new MmlDatum(0x00));

        int n = 0;
        for (int i = 0; i < Work.MAXChips; i++) {
            for (int j = 0; j < Work.MAXCH; j++) {
                for (int k = 0; k < Work.MAXPG; k++) {
                    if (work.getbufCount()[i][j][k] > 1) n++;
                }
            }
        }

        dat.add(new MmlDatum(n)); // 使用するページの総数(0～)
        dat.add(new MmlDatum(0x00));

        int instSets = 0;
        for (int i = 0; i < Work.MAXChips; i++) instSets += work.OTONUM[i];
        if (mucInfo.getSsgVoice().size() > 0) instSets = 2;
        else instSets = instSets > 0 ? 1 : 0;

        dat.add(new MmlDatum(instSets)); // 使用するInstrumentセットの総数(0～)
        dat.add(new MmlDatum(0x00));

        boolean pcmuse = ((option & 2) == 0);
        int[] pcmsize = new int[6];
        int m = 0;
        for (int k = 0; k < 6; k++) {
            pcmsize[k] = (pcmdata[k] == null) ? 0 : pcmdata[k].length;
            pcmdata[k] = (!pcmuse ? null : pcmdata[k]);
            pcmsize[k] = (!pcmuse ? 0 : pcmsize[k]);

            if (pcmdata[k] == null || pcmsize[k] == 0) {
                pcmdata[k] = null;
                pcmsize[k] = 0;
                m++;
            }
        }
        if (m == 6) pcmuse = false;

        dat.add(new MmlDatum(pcmuse ? 6 : 0));// 使用するPCMセットの総数(0～)
        dat.add(new MmlDatum(0x00));

        dat.add(new MmlDatum(0x00));// 曲情報への絶対アドレス
        dat.add(new MmlDatum(0x00));//
        dat.add(new MmlDatum(0x00));//
        dat.add(new MmlDatum(0x00));//

        dat.add(new MmlDatum(0x00));// 曲情報のサイズ
        dat.add(new MmlDatum(0x00));//
        dat.add(new MmlDatum(0x00));//
        dat.add(new MmlDatum(0x00));//

        dat.add(new MmlDatum((byte) work.JCLOCK));// JCLOCKの値(Jコマンドのタグ位置)
        dat.add(new MmlDatum((byte) (work.JCLOCK >> 8)));
        dat.add(new MmlDatum((byte) (work.JCLOCK >> 16)));
        dat.add(new MmlDatum((byte) (work.JCLOCK >> 24)));

        dat.add(new MmlDatum((byte) work.getJPLINE()));//jump line number
        dat.add(new MmlDatum((byte) (work.getJPLINE() >> 8)));
        dat.add(new MmlDatum((byte) (work.getJPLINE() >> 16)));
        dat.add(new MmlDatum((byte) (work.getJPLINE() >> 24)));

        work.compilerInfo.jumpRow = -1;
        work.compilerInfo.jumpCol = -1;
        if (work.getJPLINE() >= 0) {
            Debug.printf("#Jump count [%s]. channelNumber[%s]", work.JCLOCK, work.getJCHCOM().get(0));
            Debug.printf("#Jump line [row:%s col:%s].", work.getJPLINE(), work.getJPCOL());
            work.compilerInfo.jumpRow = work.getJPLINE();
            work.compilerInfo.jumpCol = work.getJPCOL();
        }


        //可変長ヘッダー情報


        //Chip Define division.

        int pcmI = 0;
        for (int chipI = 0; chipI < work.MAXChips; chipI++) {
            dat.add(new MmlDatum((byte) (chipI >> 0)));// Chip Index
            dat.add(new MmlDatum((byte) (chipI >> 8)));//

            int opmIdentifyNumber = 0x0000_0030;
            int opnaIdentifyNumber = 0x0000_0048;
            int opnbIdentifyNumber = 0x0000_004c;
            int opmMasterClock = 3579545;
            int opnaMasterClock = 7987200;
            int opnbMasterClock = 8000000;

            if (chipI < 2) {
                dat.add(new MmlDatum((byte) (opnaIdentifyNumber >> 0)));// Chip Identify number
                dat.add(new MmlDatum((byte) (opnaIdentifyNumber >> 8)));//
                dat.add(new MmlDatum((byte) (opnaIdentifyNumber >> 16)));//
                dat.add(new MmlDatum((byte) (opnaIdentifyNumber >> 24)));//

                dat.add(new MmlDatum((byte) opnaMasterClock));// Chip Clock
                dat.add(new MmlDatum((byte) (opnaMasterClock >> 8)));
                dat.add(new MmlDatum((byte) (opnaMasterClock >> 16)));
                dat.add(new MmlDatum((byte) (opnaMasterClock >> 24)));
            } else if (chipI < 4) {
                dat.add(new MmlDatum((byte) (opnbIdentifyNumber >> 0)));// Chip Identify number
                dat.add(new MmlDatum((byte) (opnbIdentifyNumber >> 8)));//
                dat.add(new MmlDatum((byte) (opnbIdentifyNumber >> 16)));//
                dat.add(new MmlDatum((byte) (opnbIdentifyNumber >> 24)));//

                dat.add(new MmlDatum((byte) opnbMasterClock));// Chip Clock
                dat.add(new MmlDatum((byte) (opnbMasterClock >> 8)));
                dat.add(new MmlDatum((byte) (opnbMasterClock >> 16)));
                dat.add(new MmlDatum((byte) (opnbMasterClock >> 24)));
            } else {
                dat.add(new MmlDatum((byte) (opmIdentifyNumber >> 0)));// Chip Identify number
                dat.add(new MmlDatum((byte) (opmIdentifyNumber >> 8)));//
                dat.add(new MmlDatum((byte) (opmIdentifyNumber >> 16)));//
                dat.add(new MmlDatum((byte) (opmIdentifyNumber >> 24)));//

                dat.add(new MmlDatum((byte) opmMasterClock));// Chip Clock
                dat.add(new MmlDatum((byte) (opmMasterClock >> 8)));
                dat.add(new MmlDatum((byte) (opmMasterClock >> 16)));
                dat.add(new MmlDatum((byte) (opmMasterClock >> 24)));
            }

            dat.add(new MmlDatum(0x00));// Chip Option
            dat.add(new MmlDatum(0x00));//
            dat.add(new MmlDatum(0x00));//
            dat.add(new MmlDatum(0x00));//

            dat.add(new MmlDatum(0x01));// Heart Beat (1:OPNA Timer)
            dat.add(new MmlDatum(0x00));//
            dat.add(new MmlDatum(0x00));//
            dat.add(new MmlDatum(0x00));//

            dat.add(new MmlDatum(0x00));// Heart Beat2 (0:Unuse)
            dat.add(new MmlDatum(0x00));//
            dat.add(new MmlDatum(0x00));//
            dat.add(new MmlDatum(0x00));//

            dat.add(new MmlDatum(work.MAXCH));//part count

            n = work.OTONUM[chipI] > 0 ? 1 : 0;
            dat.add(new MmlDatum(n));// 使用するInstrumentセットの総数(0～)

            for (int i = 0; i < n; i++) {
                dat.add(new MmlDatum(0x00));// この音源Chipで使用するInstrumentセットの番号。上記パラメータの個数だけ繰り返す。
                dat.add(new MmlDatum(0x00));
            }

            n = pcmuse ? (chipI < 2 ? 1 : (chipI < 4 ? 2 : 0)) : 0;
            dat.add(new MmlDatum(n));// この音源Chipで使用するPCMセットの個数
            for (int i = 0; i < n; i++) {
                dat.add(new MmlDatum((byte) pcmI));// この音源Chipで使用するPCMセットの番号。上記パラメータの個数だけ繰り返す。
                dat.add(new MmlDatum((byte) (pcmI >> 8)));
                pcmI++;
            }
        }

        //Part division.

        for (int i = 0; i < Work.MAXChips; i++) {
            for (int j = 0; j < Work.MAXCH; j++) {
                n = 0;
                for (int pg = 0; pg < Work.MAXPG; pg++) if (work.getbufCount()[i][j][pg] > 1) n++;
                dat.add(new MmlDatum(n));//ページの数(0～)
            }
        }

        //Page division.

        for (int i = 0; i < Work.MAXChips; i++)
            for (int j = 0; j < Work.MAXCH; j++)
                for (int pg = 0; pg < Work.MAXPG; pg++) {
                    if (work.getbufCount()[i][j][pg] < 2) continue;

                    n = work.getbufCount()[i][j][pg];
                    dat.add(new MmlDatum((byte) n));// ページの大きさ(0～)
                    dat.add(new MmlDatum((byte) (n >> 8)));
                    dat.add(new MmlDatum((byte) (n >> 16)));
                    dat.add(new MmlDatum((byte) (n >> 24)));
                    n = work.loopPoint[i][j][pg];
                    dat.add(new MmlDatum((byte) n));// ページのループポイント(0～)
                    dat.add(new MmlDatum((byte) (n >> 8)));
                    dat.add(new MmlDatum((byte) (n >> 16)));
                    dat.add(new MmlDatum((byte) (n >> 24)));
                }

        //Instrument set division.

        // 使用するInstrumentセットの総数(0～)
        if (instSets > 0) { // FM の音色を使用する場合は1(但しSSG波形を使用している場合は、FMを使用していなくとも定義する)
            dat.add(new MmlDatum((byte) mucInfo.getBufUseVoice().size()));
            dat.add(new MmlDatum((byte) (mucInfo.getBufUseVoice().size() >> 8)));
            dat.add(new MmlDatum((byte) (mucInfo.getBufUseVoice().size() >> 16)));
            dat.add(new MmlDatum((byte) (mucInfo.getBufUseVoice().size() >> 24)));
        }
        if (instSets == 2) { // SSG の波形を使用する場合は2
            int ssgVoiceSize = mucInfo.getSsgVoice().size() * 65;//65 : 64(dataSize) + 1(音色番号)
            dat.add(new MmlDatum((byte) ssgVoiceSize));
            dat.add(new MmlDatum((byte) (ssgVoiceSize >> 8)));
            dat.add(new MmlDatum((byte) (ssgVoiceSize >> 16)));
            dat.add(new MmlDatum((byte) (ssgVoiceSize >> 24)));
        }

        // PCM set division.

        if (pcmuse) {
            for (int i = 0; i < pcmI; i++) {
                dat.add(new MmlDatum((byte) pcmsize[i]));
                dat.add(new MmlDatum((byte) (pcmsize[i] >> 8)));
                dat.add(new MmlDatum((byte) (pcmsize[i] >> 16)));
                dat.add(new MmlDatum((byte) (pcmsize[i] >> 24)));
            }
        }

        // ページデータ出力

        for (int i = 0; i < Work.MAXChips; i++)
            for (int j = 0; j < Work.MAXCH; j++)
                for (int pg = 0; pg < Work.MAXPG; pg++) {
                    if (work.getbufCount()[i][j][pg] < 2) continue;
                    for (int p = 0; p < work.getbufCount()[i][j][pg]; p++) {
                        dat.add(mucInfo.getbufPage()[i][j][pg].get(p));
                    }
                }

        // Instrumentデータ出力

        if (instSets > 0) {
            dat.addAll(mucInfo.getBufUseVoice());
        }
        if (instSets == 2) {
            for (int key : mucInfo.getSsgVoice().keySet()) {
                dat.add(new MmlDatum((byte) key));
                for (byte d : mucInfo.getSsgVoice().get(key)) {
                    dat.add(new MmlDatum((byte) d));
                }
            }
        }

        // PCMデータ出力

        if (pcmuse) {
            for (int i = 0; i < pcmI; i++)
                for (int j = 0; j < pcmsize[i]; j++) dat.add(new MmlDatum(pcmdata[i][j]));
        }

        // 曲情報出力

        int infoAdr = dat.size();
        dat.set(0x12, new MmlDatum((byte) infoAdr));
        dat.set(0x13, new MmlDatum((byte) (infoAdr >> 8)));
        dat.set(0x14, new MmlDatum((byte) (infoAdr >> 16)));
        dat.set(0x15, new MmlDatum((byte) (infoAdr >> 24)));

        boolean useDriverTAG = false;
        if (tags != null) {
            for (Tuple<String, String> tag : tags) {
                if (tag.getItem1().equals("driver")) useDriverTAG = true;
            }
        }

        if (!useDriverTAG && mucInfo.getDriverType() == MUCInfo.DriverType.DotNet) {
            if (tags == null) tags = new ArrayList<>();
            tags.add(new Tuple<>("driver", MUCInfo.dotNET));
        }

        if (tags != null) {
            int tagsize = 0;
            for (Tuple<String, String> tag : tags) {
                if (tag.getItem1() != null && tag.getItem1().length() > 0 && tag.getItem1().charAt(0) == '*') continue;
                byte[] b = enc.GetSjisArrayFromString(String.format("#%s %s\n", tag.getItem1(), tag.getItem2()));
                tagsize += b.length;
                for (byte bd : b) dat.add(new MmlDatum(bd));
            }

            dat.set(0x16, new MmlDatum((byte) tagsize));
            dat.set(0x17, new MmlDatum((byte) (tagsize >> 8)));
            dat.set(0x18, new MmlDatum((byte) (tagsize >> 16)));
            dat.set(0x19, new MmlDatum((byte) (tagsize >> 24)));
        }

        return 0;
    }

    public void setCompileSwitch(Object... param) {
        this.isIDE = false;
        this.skipPoint = Common.EmptyPoint;

        if (param == null) return;

        for (Object prm : param) {
            if (!(prm instanceof String)) continue;

            //IDEフラグオン
            if (prm.equals("IDE")) {
                this.isIDE = true;
            }

            //スキップ再生指定
            if (((String) prm).indexOf("SkipPoint=") == 0) {
                try {
                    String[] p = ((String) prm).split("=")[1].split(":");
                    //if (p.Length != 2) continue;
                    //if (p[0].Length < 2 || p[1].Length < 2) continue;
                    //if (p[0][0] != 'R' || p[1][0] != 'C') continue;
                    int r = Integer.parseInt(p[0].substring(1));
                    int c = Integer.parseInt(p[1].substring(1));
                    this.skipPoint = new Point(c, r);
                } catch (Exception e) {
                }
            }
        }
    }

    public GD3Tag getGD3TagInfo(byte[] srcBuf) {
        List<Tuple<String, String>> tags = getTagsFromMUC(srcBuf);

        GD3Tag gt = new GD3Tag();

        for (Tuple<String, String> tag : tags) {
            switch (tag.getItem1()) {
            case "title":
                addItemAry(gt, enmTag.Title, tag.getItem2());
                addItemAry(gt, enmTag.TitleJ, tag.getItem2());
                break;
            case "composer":
                addItemAry(gt, enmTag.Composer, tag.getItem2());
                addItemAry(gt, enmTag.ComposerJ, tag.getItem2());
                break;
            case "author":
                addItemAry(gt, enmTag.Artist, tag.getItem2());
                addItemAry(gt, enmTag.ArtistJ, tag.getItem2());
                break;
            case "comment":
                addItemAry(gt, enmTag.Note, tag.getItem2());
                break;
            case "mucom88":
                addItemAry(gt, enmTag.RequestDriverVersion, tag.getItem2());
                break;
            case "date":
                addItemAry(gt, enmTag.ReleaseDate, tag.getItem2());
                break;
            case "driver":
                addItemAry(gt, enmTag.DriverName, tag.getItem2());
                break;
            }
        }

        return gt;
    }

    private static void addItemAry(GD3Tag gt, enmTag tag, String item) {
        if (!gt.dicItem.containsKey(tag))
            gt.dicItem.put(tag, new String[] {item});
        else {
            String[] dmy = gt.dicItem.get(tag);
            dmy = new String[dmy.length + 1];
            dmy[dmy.length - 1] = item;
            gt.dicItem.put(tag, dmy);
        }
    }

    private byte[] getPackedPCM(int i, java.util.List<String> list, Function<String, Stream> appendFileReaderCallback) {
        AdpcmMaker adpcmMaker = new AdpcmMaker(i, list, appendFileReaderCallback);
        return adpcmMaker.make();
    }
}
