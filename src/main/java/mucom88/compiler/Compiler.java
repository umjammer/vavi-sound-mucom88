package mucom88.compiler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.logging.Level;

import dotnet4j.io.Stream;
import dotnet4j.util.compat.StringUtilities;
import dotnet4j.util.compat.Tuple;
import dotnet4j.util.compat.Tuple3;
import mucom88.common.MUCInfo;
import mucom88.common.MucException;
import mucom88.common.Common;
import mucom88.compiler.pcmTool.AdpcmMaker;
import musicDriverInterface.CompilerInfo;
import musicDriverInterface.GD3Tag;
import musicDriverInterface.MmlDatum;
import musicDriverInterface.Tag;
import musicDriverInterface.ICompiler;
import vavi.util.Debug;


public class Compiler implements ICompiler {

    static final ResourceBundle rb = ResourceBundle.getBundle("lang/message");

    private byte[] srcBuf = null;
    private MUCInfo mucInfo = new MUCInfo();
    private Work work = null;
    private Muc88 muc88 = null;
    private Msub msub = null;
    private Expand expand = null;
    private SMon smon = null;
    private byte[] voice;
    private byte[][] pcmData = new byte[6][];
    private final List<Tuple<Integer, String>> basSrc = new ArrayList<>();
    private final List<MmlDatum> dat = new ArrayList<>();

    private String outFileName;

    public String getOutFileName() {
        return outFileName;
    }

    public void setOutFileName(String value) {
        outFileName = value;
    }

    private Point skipPoint = Common.EmptyPoint;

    private boolean isIDE = false;

    public enum MUCOMFileType {
        unknown,
        MUB,
        MUC
    }

    public void init() {
        // mucInfo = new MUCInfo();
        work = new Work();
        muc88 = new Muc88(work, mucInfo);
        msub = new Msub(work, mucInfo);
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
            srcBuf = mdsound.Common.readAllBytes(sourceMML);
            mucInfo = getMUCInfo(srcBuf);
            mucInfo.setIDE(isIDE);
            mucInfo.setSkipPoint(skipPoint);
            mucInfo.setErrSign(false);
            voice = null;
            for (int i = 0; i < 6; i++) pcmData[i] = null;

            try (Stream vd = appendFileReaderCallback.apply(StringUtilities.isNullOrEmpty(mucInfo.getVoice()) ? "voice.dat" : mucInfo.getVoice())) {
                voice = mdsound.Common.readAllBytes(vd);
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
                    pcmData[i] = getPackedPCM(i, mucInfo.getPcmAt()[i], appendFileReaderCallback);
                }

                if (pcmData[i] == null) {
                    try (Stream pd = appendFileReaderCallback.apply(StringUtilities.isNullOrEmpty(mucInfo.getPcm()[i])
                            ? pcmDefaultFilenames[i]
                            : mucInfo.getPcm()[i])) {
                        pcmData[i] = mdsound.Common.readAllBytes(pd);
                    }
                }
            }

            mucInfo.setLines(storeBasicSource(srcBuf));
            mucInfo.setVoiceData(voice);
            mucInfo.setPcmData(pcmData[0]);
            mucInfo.setBasSrc(basSrc);
            mucInfo.setSrcCPtr(0);
            mucInfo.setSrcLinPtr(-1);
            //work.compilerInfo.jumpRow = -1;
            //work.compilerInfo.jumpCol = -1;

            // MUCOM88 初期化
            int ret = muc88.compile(); // vector 0xeea8

            // コンパイルエラー発生時は 0 以外が返る
            if (ret != 0) {
                int errLine = muc88.getErrorLine();
Debug.println(Level.FINE, "errLine: " + errLine);
                work.compilerInfo.errorList.add(
                        new Tuple3<>(
                                mucInfo.getRow(),
                                mucInfo.getCol(),
                                String.format(rb.getString("E0100"), mucInfo.getRow(), mucInfo.getCol())
                        ));
                Debug.printf(Level.SEVERE, rb.getString("E0100"), mucInfo.getRow(), mucInfo.getCol());
                return null;
            }

            ret = saveMub();
            if (ret == 0) {
                return dat.toArray(MmlDatum[]::new);
            }
        } catch (MucException me) {
me.printStackTrace();
            if (work.compilerInfo == null) work.compilerInfo = new CompilerInfo();
            work.compilerInfo.errorList.add(new Tuple3<>(-1, -1, me.getMessage()));
            Debug.printf(Level.SEVERE, me.getMessage());
        } catch (Exception e) {
e.printStackTrace();
            if (work.compilerInfo == null) work.compilerInfo = new CompilerInfo();
            work.compilerInfo.errorList.add(new Tuple3<>(-1, -1, e.getMessage()));
            Debug.printf(Level.SEVERE, String.format(rb.getString("E0000"), e.getMessage(), Arrays.toString(e.getStackTrace())));
        }

        return null;
    }

    public boolean compile(Stream sourceMML, Stream destCompiledBin, Function<String, Stream> appendFileReaderCallback) {
        var data = compile(sourceMML, appendFileReaderCallback);
        if (data == null) {
            return false;
        }
        for (MmlDatum datum : data) {
            if (datum == null) {
                destCompiledBin.writeByte((byte) 0);
            } else {
                destCompiledBin.writeByte((byte) (datum.dat & 0xff));
            }
        }
        return true;
    }

    public MUCInfo getMUCInfo(byte[] buf) {
        if (checkFileType(buf) != MUCOMFileType.MUC) {
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
                mucInfo.setOpna1rhythmmute(0);
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


    private MUCOMFileType checkFileType(byte[] buf) {
        if (buf == null || buf.length < 4) {
            return MUCOMFileType.unknown;
        }

        if (buf[0] == 0x4d
                && buf[1] == 0x55
                && buf[2] == 0x43
                && buf[3] == 0x38) {
            return MUCOMFileType.MUB;
        }

        return MUCOMFileType.MUC;
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
        var text = Arrays.stream(new String(buf, Common.fileEncoding).split("\r\n"))
                .filter(x -> x.indexOf("#") == 0).toArray(String[]::new);
        if (tags != null) tags.clear();
        else tags = new ArrayList<>();

        for (String v : text) {
            try {
                int p = v.indexOf(' ');
                String tag;
                String ele;
                if (p >= 0) {
                    tag = v.substring(1, 1 + p).trim().toLowerCase();
                    ele = v.substring(p + 1).trim();
                    Tuple<String, String> item = new Tuple<>(tag, ele);
                    tags.add(item);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return tags;
    }

    private int storeBasicSource(byte[] buf) {
        int line = 0;
        var text = new String(buf, Common.fileEncoding).split("\r\n");

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

            int pcmFlag = 0;
            int maxCount = 0;
            int mubSize;
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
                        if (work.lCnt[i][j][0] != 0) {
                            work.lCnt[i][j][0] = work.tCnt[i][j][0] - (work.lCnt[i][j][0] - 1);
                        }
                        if (work.tCnt[i][j][0] > maxCount) maxCount = work.tCnt[i][j][0];
                        tCount.append(String.format("%s:%05d ", Work.getTrackCharacterFromChipValue(i, j), work.tCnt[i][j][0]));
                        work.compilerInfo.totalCount.add(work.tCnt[i][j][0]);
                        lCount.append(String.format("%s:%05d ", Work.getTrackCharacterFromChipValue(i, j), work.lCnt[i][j][0]));
                        work.compilerInfo.loopCount.add(work.lCnt[i][j][0]);
                        bCount.append(String.format("%s:$%04x ", Work.getTrackCharacterFromChipValue(i, j), work.getBufCount()[i][j][0]));
                        work.compilerInfo.bufferCount.add(work.getBufCount()[i][j][0]);
                        if (work.getBufCount()[i][j][0] > 0xffff) {
                            throw new MucException(String.format(rb.getString("E0700"), Work.getTrackCharacterFromChipValue(i, j), Arrays.deepToString(work.getBufCount()[i])));
                        }
                    } else {
                        work.compilerInfo.formatType = "mupb";
                        int n = 0;
                        for (int pg = 0; pg < 10; pg++) {
                            if (work.lCnt[i][j][pg] != 0) {
                                work.lCnt[i][j][pg] = work.tCnt[i][j][pg] - (work.lCnt[i][j][pg] - 1);
                            }
                            if (work.tCnt[i][j][pg] > maxCount) maxCount = work.tCnt[i][j][pg];
                            work.compilerInfo.totalCount.add(work.tCnt[i][j][pg]);
                            work.compilerInfo.loopCount.add(work.lCnt[i][j][pg]);
                            if (work.getBufCount()[i][j][pg] > 1) {
                                tc.append(String.format("%s%s:%05d ", Work.getTrackCharacterFromChipValue(i, j), pg, work.tCnt[i][j][pg]));
                                lc.append(String.format("%s%s:%05d ", Work.getTrackCharacterFromChipValue(i, j), pg, work.lCnt[i][j][pg]));
                                bc.append(String.format("%s%s:$%04x ", Work.getTrackCharacterFromChipValue(i, j), pg, work.getBufCount()[i][j][pg]));
                                bufferLength = work.getBufCount()[i][j][pg];
                                n++;
                                if (n == 10) {
                                    n = 0;
                                    tc.append("\n");
                                    lc.append("\n");
                                    bc.append("\n");
                                }
                                // if (j != 0) usePageFunction = true;
                            }
                        }
                        for (int pg = 0; pg < 10; pg++) {
                            work.compilerInfo.bufferCount.add(work.getBufCount()[i][j][pg]);
                            if (work.getBufCount()[i][j][pg] > 0xffff) {
                                throw new MucException(String.format(rb.getString("E0700")
                                        , Work.getTrackCharacterFromChipValue(i, j) + String.valueOf(pg)
                                        , Arrays.toString(work.getBufCount()[i][j])));
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

            work.compilerInfo.jumpClock = work.jClock;
            work.compilerInfo.jumpChannel = work.getJChCom();

            if (work.pcmFlag == 0) pcmFlag = 2;
            msg = new String(textLineBuf, 31, 4, Common.fileEncoding);
            int start = Integer.parseInt(msg, 16);
            msg = new String(textLineBuf, 41, 4, Common.fileEncoding);
            int length = mucInfo.getBufDst().size();
            if (isExtendFormat) length = bufferLength;
            mubSize = length;

            System.out.println("- mucom.NET -");
            System.out.print("[ Total count ]\n" + tCount);
            System.out.print("[ Loop count  ]\n" + lCount);
            if (isExtendFormat)
                System.out.print("[ Buffer count  ]\n" + bCount);
            System.out.println();
            System.out.printf("#mucom type    : %s%n", mucInfo.getDriverType());
            System.out.printf("#MUB Format    : %s%n", isExtendFormat ? "Extend" : "Normal");
            System.out.println("#Used FM voice : ");
            System.out.printf("#      @ count : %s%n", work.getUsedFMVoiceNumber().size());
            List<Integer> usedFMVoiceNumberList = new ArrayList<>(work.getUsedFMVoiceNumber());
            Collections.sort(usedFMVoiceNumberList);
            System.out.printf("#      @ list  : %s%n", String.join(" ", usedFMVoiceNumberList.stream().map(String::valueOf).toArray(String[]::new)));
            System.out.printf("#Data Buffer   : $%05x - $%05x ($%05x)%n", start, start + length - 1, length);
            System.out.printf("#Max Count     : %s%n", maxCount);
            System.out.printf("#MML Lines     : %s%n", mucInfo.getLines());
            System.out.printf("#Data          : %s%n", mubSize);

            return saveMusic(length, pcmFlag, isExtendFormat);
        } catch (MucException me) {
            work.compilerInfo.errorList.add(new Tuple3<>(-1, -1, me.getMessage()));
            Debug.printf(Level.SEVERE, me.getMessage());
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 音楽データファイルを出力(コンパイルが必要)
     * @param option 1: #タグによるvoice設定を無視
     *               2: PCM埋め込みをスキップ
     * @return 戻り値が 0 以外の場合はエラー
     * usePageFunc : ページ機能を使用しているか
     */
    private int saveMusic(int length, int option, boolean isExtendFormat) {

Debug.println("isExtendFormat: " + isExtendFormat);
        if (isExtendFormat) {
            return saveMusicExtendFormat(length, option);
        }

        int footSize = 1; // かならず1以上

        int pcmSize = (pcmData[0] == null) ? 0 : pcmData[0].length;
        boolean pcmUse = ((option & 2) == 0);
        pcmData[0] = (!pcmUse ? null : pcmData[0]);
        int pcmPtr = (!pcmUse ? 0 : (32 + length + footSize));
        pcmSize = (!pcmUse ? 0 : pcmSize);
        if (pcmUse) {
            if (pcmData[0] == null || pcmSize == 0) {
                pcmUse = false;
                pcmData[0] = null;
                pcmPtr = 0;
                pcmSize = 0;
            }
        }

        int dataOffset = 0x50;
        int dataSize = length;
        int tagOffset = length + 0x50;

        dat.clear();

        dat.add(new MmlDatum(0x4d)); // M
        dat.add(new MmlDatum(0x55)); // U
        dat.add(new MmlDatum(0x42)); // B
        dat.add(new MmlDatum(0x38)); // 8

        dat.add(new MmlDatum(dataOffset & 0xff));
        dat.add(new MmlDatum((dataOffset >> 8) & 0xff));
        dat.add(new MmlDatum((dataOffset >> 16) & 0xff));
        dat.add(new MmlDatum((dataOffset >> 24) & 0xff));

        dat.add(new MmlDatum(dataSize & 0xff));
        dat.add(new MmlDatum((dataSize >> 8) & 0xff));
        dat.add(new MmlDatum((dataSize >> 16) & 0xff));
        dat.add(new MmlDatum((dataSize >> 24) & 0xff));

        dat.add(new MmlDatum(tagOffset & 0xff));
        dat.add(new MmlDatum((tagOffset >> 8) & 0xff));
        dat.add(new MmlDatum((tagOffset >> 16) & 0xff));
        dat.add(new MmlDatum((tagOffset >> 24) & 0xff));

        dat.add(new MmlDatum(0)); // tagData size(dummy)
        dat.add(new MmlDatum(0));
        dat.add(new MmlDatum(0));
        dat.add(new MmlDatum(0));

        dat.add(new MmlDatum(pcmPtr & 0xff)); // pcmData ptr(32bit)
        dat.add(new MmlDatum((pcmPtr >> 8) & 0xff));
        dat.add(new MmlDatum((pcmPtr >> 16) & 0xff));
        dat.add(new MmlDatum((pcmPtr >> 24) & 0xff));

        dat.add(new MmlDatum(pcmSize & 0xff)); // pcmData size(32bit)
        dat.add(new MmlDatum((pcmSize >> 8) & 0xff));
        dat.add(new MmlDatum((pcmSize >> 16) & 0xff));
        dat.add(new MmlDatum((pcmSize >> 24) & 0xff));

        dat.add(new MmlDatum(work.jClock & 0xff)); // JCLOCKの値(Jコマンドのタグ位置)
        dat.add(new MmlDatum((work.jClock >> 8) & 0xff));

        dat.add(new MmlDatum(work.getJpLine() & 0xff)); // jump line number
        dat.add(new MmlDatum((work.getJpLine() >> 8) & 0xff));

        dat.add(new MmlDatum(0)); // extFlags(?)
        dat.add(new MmlDatum(0));

        dat.add(new MmlDatum(1)); // extSystem(?)

        dat.add(new MmlDatum(2)); // extTarget(?)

        dat.add(new MmlDatum(11)); // extChannelNum
        dat.add(new MmlDatum(0));

        dat.add(new MmlDatum(work.otoNum[0] & 0xff)); // extFmVoiceNum
        dat.add(new MmlDatum((work.otoNum[0] >> 8) & 0xff));

        dat.add(new MmlDatum(0)); // extPlayer(?)
        dat.add(new MmlDatum(0));
        dat.add(new MmlDatum(0));
        dat.add(new MmlDatum(0));

        dat.add(new MmlDatum(0)); // pad1
        dat.add(new MmlDatum(0));
        dat.add(new MmlDatum(0));
        dat.add(new MmlDatum(0));

        for (int i = 0; i < 32; i++) {
            dat.add(new MmlDatum(mucInfo.getBufDefVoice().get(i)));
        }

        work.compilerInfo.jumpRow = -1;
        work.compilerInfo.jumpCol = -1;
        if (work.getJpLine() >= 0) {
            Debug.printf("#Jump count [%s]. channelNumber[%s]", work.jClock, work.getJChCom().get(0));
            Debug.printf("#Jump line [row:%s col:%s].", work.getJpLine(), work.getJpCol());
            work.compilerInfo.jumpRow = work.getJpLine();
            work.compilerInfo.jumpCol = work.getJpCol();
        }

        for (int i = 0; i < length; i++) dat.add(mucInfo.getBufDst().get(i));

        dat.set(dataOffset + 0, new MmlDatum(0)); // バイナリに含まれる曲データ数-1
        dat.set(dataOffset + 1, new MmlDatum(work.otoDat & 0xff));
        dat.set(dataOffset + 2, new MmlDatum((work.otoDat >> 8) & 0xff));
        dat.set(dataOffset + 3, new MmlDatum(work.endAdr & 0xff));
        dat.set(dataOffset + 4, new MmlDatum((work.endAdr >> 8) & 0xff));
        if (dat.get(dataOffset + 5) == null) {
            dat.set(dataOffset + 5, new MmlDatum(0)); // テンポコマンド(タイマーB)を未設定時nullのままになってしまうので、とりあえず値をセット
        }

        footSize = 0;

        boolean useDriverTAG = false;
        if (tags != null) {
            for (Tuple<String, String> tag : tags) {
                if (tag.getItem1().equals("driver")) useDriverTAG = true;
            }
        }

        // データサイズが64k超えていたらdotnet確定
        if (work.endAdr - work.muNum > 0xffff) {
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
                byte[] b = String.format("#%s %s\n", tag.getItem1(), tag.getItem2()).getBytes(Common.fileEncoding);
                footSize += b.length;
                for (byte bd : b) dat.add(new MmlDatum(bd & 0xff));
            }
        }

        if (footSize > 0) {
            dat.add(new MmlDatum(0));
            dat.add(new MmlDatum(0));
            dat.add(new MmlDatum(0));
            dat.add(new MmlDatum(0));
            footSize += 4;

            dat.set(16, new MmlDatum(footSize & 0xff)); // tagData size(32bit)
            dat.set(17, new MmlDatum((footSize >> 8) & 0xff));
            dat.set(18, new MmlDatum((footSize >> 16) & 0xff));
            dat.set(19, new MmlDatum((footSize >> 24) & 0xff));
        } else {
            tags = null;
        }

        if (tags == null) {
            // クリア
            for (int i = 0; i < 8; i++) {
                dat.set(12 + i, new MmlDatum(0));
            }
        }

        if (pcmUse) {
            for (int i = 0; i < pcmSize; i++) dat.add(new MmlDatum(pcmData[0][i]));
            if (pcmSize > 0) {
                pcmPtr = 16 * 3 + 32 + length + footSize;
                dat.set(20, new MmlDatum(pcmPtr & 0xff)); // pcmData size(32bit)
                dat.set(21, new MmlDatum((pcmPtr >> 8) & 0xff));
                dat.set(22, new MmlDatum((pcmPtr >> 16) & 0xff));
                dat.set(23, new MmlDatum((pcmPtr >> 24) & 0xff));
            }
        }

        return 0;
    }

    private int saveMusicExtendFormat(int length, int option) {

        dat.clear();

        // 固定長ヘッダー情報　作成

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
                    if (work.getBufCount()[i][j][k] > 1) n++;
                }
            }
        }

        dat.add(new MmlDatum(n)); // 使用するページの総数(0～)
        dat.add(new MmlDatum(0x00));

        int instSets = 0;
        for (int i = 0; i < Work.MAXChips; i++) instSets += work.otoNum[i];
        if (mucInfo.getSsgVoice().size() > 0) instSets = 2;
        else instSets = instSets > 0 ? 1 : 0;

        dat.add(new MmlDatum(instSets)); // 使用するInstrumentセットの総数(0～)
        dat.add(new MmlDatum(0x00));

        boolean pcmuse = ((option & 2) == 0);
        int[] pcmsize = new int[6];
        int m = 0;
        for (int k = 0; k < 6; k++) {
            pcmsize[k] = (pcmData[k] == null) ? 0 : pcmData[k].length;
            pcmData[k] = (!pcmuse ? null : pcmData[k]);
            pcmsize[k] = (!pcmuse ? 0 : pcmsize[k]);

            if (pcmData[k] == null || pcmsize[k] == 0) {
                pcmData[k] = null;
                pcmsize[k] = 0;
                m++;
            }
        }
        if (m == 6) pcmuse = false;

        dat.add(new MmlDatum(pcmuse ? 6 : 0)); // 使用するPCMセットの総数(0～)
        dat.add(new MmlDatum(0x00));

        dat.add(new MmlDatum(0x00)); // 曲情報への絶対アドレス
        dat.add(new MmlDatum(0x00)); // 
        dat.add(new MmlDatum(0x00)); // 
        dat.add(new MmlDatum(0x00)); // 

        dat.add(new MmlDatum(0x00)); // 曲情報のサイズ
        dat.add(new MmlDatum(0x00)); // 
        dat.add(new MmlDatum(0x00)); // 
        dat.add(new MmlDatum(0x00)); // 

        dat.add(new MmlDatum(work.jClock)); // JCLOCKの値(Jコマンドのタグ位置)
        dat.add(new MmlDatum(work.jClock >> 8));
        dat.add(new MmlDatum(work.jClock >> 16));
        dat.add(new MmlDatum(work.jClock >> 24));

        dat.add(new MmlDatum(work.getJpLine() & 0xff)); // jump line number
        dat.add(new MmlDatum((work.getJpLine() >> 8) & 0xff));
        dat.add(new MmlDatum((work.getJpLine() >> 16) & 0xff));
        dat.add(new MmlDatum((work.getJpLine() >> 24) & 0xff));

        work.compilerInfo.jumpRow = -1;
        work.compilerInfo.jumpCol = -1;
        if (work.getJpLine() >= 0) {
            Debug.printf("#Jump count [%s]. channelNumber[%s]", work.jClock, work.getJChCom().get(0));
            Debug.printf("#Jump line [row:%s col:%s].", work.getJpLine(), work.getJpCol());
            work.compilerInfo.jumpRow = work.getJpLine();
            work.compilerInfo.jumpCol = work.getJpCol();
        }


        // 可変長ヘッダー情報


        // Chip Define division.

        int pcmI = 0;
        for (int chipI = 0; chipI < Work.MAXChips; chipI++) {
            dat.add(new MmlDatum(chipI >> 0)); // Chip Index
            dat.add(new MmlDatum(chipI >> 8)); // 

            int opmIdentifyNumber = 0x0000_0030;
            int opnaIdentifyNumber = 0x0000_0048;
            int opnbIdentifyNumber = 0x0000_004c;
            int opmMasterClock = 3579545;
            int opnaMasterClock = 7987200;
            int opnbMasterClock = 8000000;

            if (chipI < 2) {
                dat.add(new MmlDatum((opnaIdentifyNumber >> 0) & 0xff)); // Chip Identify number
                dat.add(new MmlDatum((opnaIdentifyNumber >> 8) & 0xff)); //
                dat.add(new MmlDatum((opnaIdentifyNumber >> 16) & 0xff)); //
                dat.add(new MmlDatum((opnaIdentifyNumber >> 24) & 0xff)); //

                dat.add(new MmlDatum(opnaMasterClock & 0xff)); // Chip Clock
                dat.add(new MmlDatum((opnaMasterClock >> 8) & 0xff));
                dat.add(new MmlDatum((opnaMasterClock >> 16) & 0xff));
                dat.add(new MmlDatum((opnaMasterClock >> 24) & 0xff));
            } else if (chipI < 4) {
                dat.add(new MmlDatum(opnbIdentifyNumber >> 0 & 0xff)); // Chip Identify number
                dat.add(new MmlDatum((opnbIdentifyNumber >> 8) & 0xff)); //
                dat.add(new MmlDatum((opnbIdentifyNumber >> 16) & 0xff)); //
                dat.add(new MmlDatum((opnbIdentifyNumber >> 24) & 0xff)); //

                dat.add(new MmlDatum(opnbMasterClock & 0xff)); // Chip Clock
                dat.add(new MmlDatum((opnbMasterClock >> 8) & 0xff));
                dat.add(new MmlDatum((opnbMasterClock >> 16) & 0xff));
                dat.add(new MmlDatum((opnbMasterClock >> 24) & 0xff));
            } else {
                dat.add(new MmlDatum((opmIdentifyNumber >> 0) & 0xff)); // Chip Identify number
                dat.add(new MmlDatum((opmIdentifyNumber >> 8) & 0xff)); //
                dat.add(new MmlDatum((opmIdentifyNumber >> 16) & 0xff)); //
                dat.add(new MmlDatum((opmIdentifyNumber >> 24) & 0xff)); //

                dat.add(new MmlDatum(opmMasterClock & 0xff)); // Chip Clock
                dat.add(new MmlDatum((opmMasterClock >> 8) & 0xff));
                dat.add(new MmlDatum((opmMasterClock >> 16) & 0xff));
                dat.add(new MmlDatum((opmMasterClock >> 24) & 0xff));
            }

            dat.add(new MmlDatum(0x00)); // Chip Option
            dat.add(new MmlDatum(0x00)); // 
            dat.add(new MmlDatum(0x00)); // 
            dat.add(new MmlDatum(0x00)); // 

            dat.add(new MmlDatum(0x01)); // Heart Beat (1:OPNA Timer)
            dat.add(new MmlDatum(0x00)); // 
            dat.add(new MmlDatum(0x00)); // 
            dat.add(new MmlDatum(0x00)); // 

            dat.add(new MmlDatum(0x00)); // Heart Beat2 (0:Unuse)
            dat.add(new MmlDatum(0x00)); // 
            dat.add(new MmlDatum(0x00)); // 
            dat.add(new MmlDatum(0x00)); // 

            dat.add(new MmlDatum(Work.MAXCH)); // part count

            n = work.otoNum[chipI] > 0 ? 1 : 0;
            dat.add(new MmlDatum(n)); // 使用するInstrumentセットの総数(0～)

            for (int i = 0; i < n; i++) {
                dat.add(new MmlDatum(0x00)); // この音源Chipで使用するInstrumentセットの番号。上記パラメータの個数だけ繰り返す。
                dat.add(new MmlDatum(0x00));
            }

            n = pcmuse ? (chipI < 2 ? 1 : (chipI < 4 ? 2 : 0)) : 0;
            dat.add(new MmlDatum(n)); // この音源Chipで使用するPCMセットの個数
            for (int i = 0; i < n; i++) {
                dat.add(new MmlDatum(pcmI)); // この音源Chipで使用するPCMセットの番号。上記パラメータの個数だけ繰り返す。
                dat.add(new MmlDatum(pcmI >> 8));
                pcmI++;
            }
        }

        // Part division.

        for (int i = 0; i < Work.MAXChips; i++) {
            for (int j = 0; j < Work.MAXCH; j++) {
                n = 0;
                for (int pg = 0; pg < Work.MAXPG; pg++) if (work.getBufCount()[i][j][pg] > 1) n++;
                dat.add(new MmlDatum(n)); // ページの数(0～)
            }
        }

        // Page division.

        for (int i = 0; i < Work.MAXChips; i++)
            for (int j = 0; j < Work.MAXCH; j++)
                for (int pg = 0; pg < Work.MAXPG; pg++) {
                    if (work.getBufCount()[i][j][pg] < 2) continue;

                    n = work.getBufCount()[i][j][pg];
                    dat.add(new MmlDatum(n & 0xff)); // ページの大きさ(0～)
                    dat.add(new MmlDatum((n >> 8) & 0xff));
                    dat.add(new MmlDatum((n >> 16) & 0xff));
                    dat.add(new MmlDatum((n >> 24) & 0xff));
                    n = work.loopPoint[i][j][pg];
                    dat.add(new MmlDatum(n & 0xff)); // ページのループポイント(0～)
                    dat.add(new MmlDatum((n >> 8) & 0xff));
                    dat.add(new MmlDatum((n >> 16) & 0xff));
                    dat.add(new MmlDatum((n >> 24) & 0xff));
                }

        // Instrument set division.

        // 使用するInstrumentセットの総数(0～)
        if (instSets > 0) { // FM の音色を使用する場合は1(但しSSG波形を使用している場合は、FMを使用していなくとも定義する)
            dat.add(new MmlDatum(mucInfo.getBufUseVoice().size()));
            dat.add(new MmlDatum(mucInfo.getBufUseVoice().size() >> 8));
            dat.add(new MmlDatum(mucInfo.getBufUseVoice().size() >> 16));
            dat.add(new MmlDatum(mucInfo.getBufUseVoice().size() >> 24));
        }
        if (instSets == 2) { // SSG の波形を使用する場合は2
            int ssgVoiceSize = mucInfo.getSsgVoice().size() * 65; // 65 : 64(dataSize) + 1(音色番号)
            dat.add(new MmlDatum(ssgVoiceSize & 0xff));
            dat.add(new MmlDatum((ssgVoiceSize >> 8) & 0xff));
            dat.add(new MmlDatum((ssgVoiceSize >> 16) & 0xff));
            dat.add(new MmlDatum((ssgVoiceSize >> 24) & 0xff));
        }

        // PCM set division.

        if (pcmuse) {
            for (int i = 0; i < pcmI; i++) {
                dat.add(new MmlDatum(pcmsize[i] & 0xff));
                dat.add(new MmlDatum((pcmsize[i] >> 8) & 0xff));
                dat.add(new MmlDatum((pcmsize[i] >> 16) & 0xff));
                dat.add(new MmlDatum((pcmsize[i] >> 24) & 0xff));
            }
        }

        // ページデータ出力

        for (int i = 0; i < Work.MAXChips; i++)
            for (int j = 0; j < Work.MAXCH; j++)
                for (int pg = 0; pg < Work.MAXPG; pg++) {
                    if (work.getBufCount()[i][j][pg] < 2) continue;
                    for (int p = 0; p < work.getBufCount()[i][j][pg]; p++) {
                        dat.add(mucInfo.getBufPage()[i][j][pg].get(p));
                    }
                }

        // Instrumentデータ出力

        if (instSets > 0) {
            dat.addAll(mucInfo.getBufUseVoice());
        }
        if (instSets == 2) {
            for (int key : mucInfo.getSsgVoice().keySet()) {
                dat.add(new MmlDatum(key));
                for (byte d : mucInfo.getSsgVoice().get(key)) {
                    dat.add(new MmlDatum(d & 0xff));
                }
            }
        }

        // PCMデータ出力

        if (pcmuse) {
            for (int i = 0; i < pcmI; i++)
                for (int j = 0; j < pcmsize[i]; j++)
                    dat.add(new MmlDatum(pcmData[i][j] & 0xff));
        }

        // 曲情報出力

        int infoAdr = dat.size();
        dat.set(0x12, new MmlDatum(infoAdr & 0xff));
        dat.set(0x13, new MmlDatum((infoAdr >> 8) & 0xff));
        dat.set(0x14, new MmlDatum((infoAdr >> 16) & 0xff));
        dat.set(0x15, new MmlDatum((infoAdr >> 24) & 0xff));

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
            int tagSize = 0;
            for (Tuple<String, String> tag : tags) {
                if (tag.getItem1() != null && tag.getItem1().length() > 0 && tag.getItem1().charAt(0) == '*') continue;
                byte[] b = String.format("#%s %s\n", tag.getItem1(), tag.getItem2()).getBytes(Common.fileEncoding);
                tagSize += b.length;
                for (byte bd : b) dat.add(new MmlDatum(bd & 0xff));
            }

            dat.set(0x16, new MmlDatum(tagSize & 0xff));
            dat.set(0x17, new MmlDatum((tagSize >> 8) & 0xff));
            dat.set(0x18, new MmlDatum((tagSize >> 16) & 0xff));
            dat.set(0x19, new MmlDatum((tagSize >> 24) & 0xff));
        }

        return 0;
    }

    public void setCompileSwitch(Object... param) {
        this.isIDE = false;
        this.skipPoint = Common.EmptyPoint;

        if (param == null) return;

        for (Object prm : param) {
            if (!(prm instanceof String)) continue;

            // IDEフラグオン
            if (prm.equals("IDE")) {
                this.isIDE = true;
            }

            // スキップ再生指定
            if (((String) prm).indexOf("SkipPoint=") == 0) {
                try {
                    String[] p = ((String) prm).split("=")[1].split(":");
                    //if (p.length != 2) continue;
                    //if (p[0].length < 2 || p[1].length < 2) continue;
                    //if (p[0][0] != 'R' || p[1][0] != 'C') continue;
                    int r = Integer.parseInt(p[0].substring(1));
                    int c = Integer.parseInt(p[1].substring(1));
                    this.skipPoint = new Point(c, r);
                } catch (Exception e) {
                    e.printStackTrace();
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
                addItemAry(gt, Tag.Title, tag.getItem2());
                addItemAry(gt, Tag.TitleJ, tag.getItem2());
                break;
            case "composer":
                addItemAry(gt, Tag.Composer, tag.getItem2());
                addItemAry(gt, Tag.ComposerJ, tag.getItem2());
                break;
            case "author":
                addItemAry(gt, Tag.Artist, tag.getItem2());
                addItemAry(gt, Tag.ArtistJ, tag.getItem2());
                break;
            case "comment":
                addItemAry(gt, Tag.Note, tag.getItem2());
                break;
            case "mucom88":
                addItemAry(gt, Tag.RequestDriverVersion, tag.getItem2());
                break;
            case "date":
                addItemAry(gt, Tag.ReleaseDate, tag.getItem2());
                break;
            case "driver":
                addItemAry(gt, Tag.DriverName, tag.getItem2());
                break;
            }
        }

        return gt;
    }

    private static void addItemAry(GD3Tag gt, Tag tag, String item) {
        if (!gt.items.containsKey(tag))
            gt.items.put(tag, new String[] {item});
        else {
            String[] temp = gt.items.get(tag);
            temp = new String[temp.length + 1];
            temp[temp.length - 1] = item;
            gt.items.put(tag, temp);
        }
    }

    private byte[] getPackedPCM(int i, java.util.List<String> list, Function<String, Stream> appendFileReaderCallback) {
        AdpcmMaker adpcmMaker = new AdpcmMaker(i, list, appendFileReaderCallback);
        return adpcmMaker.make();
    }
}
