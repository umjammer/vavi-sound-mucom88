package mucom88.driver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dotnet4j.util.compat.StringUtilities;
import dotnet4j.util.compat.Tuple;
import mucom88.common.Common;
import mucom88.common.MubException;
import musicDriverInterface.MmlDatum;
import vavi.util.Debug;

import static dotnet4j.util.compat.CollectionUtilities.toByteArray;


public class MubHeader {
    public int magic;
    public int dataOffset = 0;
    public int dataSize = 0;
    public int tagData;
    public int tagSize;
    public int[] pcmDataPtr = new int[] {0, 0, 0, 0, 0, 0};
    public int[] pcmSize = new int[] {0, 0, 0, 0, 0, 0};
    public int jumpCount;
    public int jumpLine;
    public int extFlags = 0;
    public int extSystem = 0;
    public int extTarget = 0;
    public int extChannelNum = 0;
    public int extFmVoiceNum = 0;
    public int extPlayer = 0;
    public int pad1 = 0;
    public byte[] extFmVoice = new byte[32];
    private MmlDatum[] srcBuf;
    public MupbInfo mupb;
    private int mupbDataPtr;
    public boolean carrierCorrection;
    public enmOPMClockMode opmClockMode;
    public boolean SSGExtend = false;
    public int[] rhythmMute = new int[] {0x3f, 0x3f, 0x3f, 0x3f};

    public enum enmOPMClockMode {
        normal, X68000
    }

    public MubHeader(MmlDatum[] buf) {
        carrierCorrection = false;
        opmClockMode = enmOPMClockMode.normal;
        SSGExtend = false;

        magic = Common.getLE32(buf, 0x0000);

        if (magic == 0x3843554d) { // 'MUC8'
            dataOffset = Common.getLE32(buf, 0x0004);
            dataSize = Common.getLE32(buf, 0x0008);
            tagData = Common.getLE32(buf, 0x000c);
            tagSize = Common.getLE32(buf, 0x0010);
            pcmDataPtr[0] = Common.getLE32(buf, 0x0014);
            pcmSize[0] = Common.getLE32(buf, 0x0018);
            jumpCount = Common.getLE16(buf, 0x001c);
            jumpLine = Common.getLE16(buf, 0x001e);
        } else if (magic == 0x3842554d) { // 'MUB8'
            dataOffset = Common.getLE32(buf, 0x0004);
            dataSize = Common.getLE32(buf, 0x0008);
            tagData = Common.getLE32(buf, 0x000c);
            tagSize = Common.getLE32(buf, 0x0010);
            pcmDataPtr[0] = Common.getLE32(buf, 0x0014);
            pcmSize[0] = Common.getLE32(buf, 0x0018);
            jumpCount = Common.getLE16(buf, 0x001c);
            jumpLine = Common.getLE16(buf, 0x001e);

            extFlags = Common.getLE16(buf, 0x0020);
            extSystem = buf[0x0022].dat;
            extTarget = buf[0x0023].dat;
            extChannelNum = Common.getLE16(buf, 0x0024);
            extFmVoiceNum = Common.getLE16(buf, 0x0026);
            extPlayer = Common.getLE32(buf, 0x0028);
            pad1 = Common.getLE32(buf, 0x002c);
            for (int i = 0; i < 32; i++) {
                extFmVoice[i] = (byte) (buf[0x0030 + i].dat & 0xff);
            }
        } else if (magic == 0x6250756d) {
            mupb = new MupbInfo();
            mupb.setVersion(Common.getLE32(buf, 0x0004));
            mupb.setVariableLengthCount(buf[0x0008].dat);
            mupb.setUseChipCount(buf[0x0009].dat);
            mupb.setUsePartCount(Common.getLE16(buf, 0x000a));
            mupb.setUsePageCount(Common.getLE16(buf, 0x000c));
            mupb.setUseInstrumentSetCount(Common.getLE16(buf, 0x000e));
            mupb.setUsePCMSetCount(Common.getLE16(buf, 0x0010));
            tagData = Common.getLE32(buf, 0x0012);
            tagSize = Common.getLE32(buf, 0x0016);
            jumpCount = Common.getLE32(buf, 0x001a);
            jumpLine = Common.getLE32(buf, 0x001e);

            int p = 0x0022;

            //Chip Define division.
            mupb.setChips(new MupbInfo.ChipDefine[mupb.getUseChipCount()]);
            for (int i = 0; i < mupb.getUseChipCount(); i++) {
                mupb.getChips()[i] = new MupbInfo.ChipDefine();
                MupbInfo.ChipDefine cd = mupb.getChips()[i];
                cd.setIndexNumber(Common.getLE16(buf, p));
                p += 2;
                cd.setIdentifyNumber(Common.getLE32(buf, p));
                p += 4;
                cd.setMasterClock(Common.getLE32(buf, p));
                p += 4;
                cd.setOption(Common.getLE32(buf, p));
                p += 4;
                cd.setHeartBeat(Common.getLE32(buf, p));
                p += 4;
                cd.setHeartBeat2(Common.getLE32(buf, p));
                p += 4;

                cd.setParts(new MupbInfo.ChipDefine.ChipPart[buf[p].dat]);
                p++;
                for (int j = 0; j < cd.getParts().length; j++) {
                    cd.getParts()[j] = new MupbInfo.ChipDefine.ChipPart();
                }

                cd.setInstrumentNumber(new int[buf[p].dat]);
                p++;
                for (int j = 0; j < cd.getInstrumentNumber().length; j++) {
                    cd.getInstrumentNumber()[j] = Common.getLE16(buf, p);
                    p += 2;
                }

                cd.setPcmNumber(new int[buf[p].dat]);
                p++;
                for (int j = 0; j < cd.getPcmNumber().length; j++) {
                    cd.getPcmNumber()[j] = Common.getLE16(buf, p);
                    p += 2;
                }
            }

            // Part division.
            mupb.setParts(new MupbInfo.PartDefine[mupb.getPsePartCount()]);
            for (int i = 0; i < mupb.getPsePartCount(); i++) {
                mupb.getParts()[i] = new MupbInfo.PartDefine();
                MupbInfo.PartDefine pd = mupb.getParts()[i];
                pd.setPageCount(buf[p].dat);
                p++;
            }

            // Page division.
            mupb.setPages(new MupbInfo.PageDefine[mupb.getUsePageCount()]);
            for (int i = 0; i < mupb.getUsePageCount(); i++) {
                mupb.getPages()[i] = new MupbInfo.PageDefine();
                MupbInfo.PageDefine pd = mupb.getPages()[i];
                pd.setLength(Common.getLE32(buf, p));
                p += 4;
                pd.setLoopPoint(Common.getLE32(buf, p));
                p += 4;
            }

            // Instrument set division.
            mupb.setInstruments(new MupbInfo.InstrumentDefine[mupb.getUseInstrumentSetCount()]);
            for (int i = 0; i < mupb.getUseInstrumentSetCount(); i++) {
                mupb.getInstruments()[i] = new MupbInfo.InstrumentDefine();
                MupbInfo.InstrumentDefine id = mupb.getInstruments()[i];
                id.setLength(Common.getLE32(buf, p));
                p += 4;
            }

            // PCM set division.
            mupb.setPcms(new MupbInfo.PCMDefine[mupb.getUsePCMSetCount()]);
            for (int i = 0; i < mupb.getUsePCMSetCount(); i++) {
                mupb.getPcms()[i] = new MupbInfo.PCMDefine();
                MupbInfo.PCMDefine pd = mupb.getPcms()[i];
                pd.setLength(Common.getLE32(buf, p));
                p += 4;
            }

            // データ部開始位置
            mupbDataPtr = p;

            // Chip 毎のページ情報割り当てとページデータの取り込み
            int pp = 0;
            int pr = 0;
            for (int i = 0; i < mupb.getUseChipCount(); i++) {
                MupbInfo.ChipDefine cd = mupb.getChips()[i];
                for (int j = 0; j < cd.getParts().length; j++) {
                    cd.getParts()[j].setPages(new MupbInfo.PageDefine[mupb.getParts()[pr++].getPageCount()]);
                    for (int k = 0; k < cd.getParts()[j].getPages().length; k++) {
                        cd.getParts()[j].getPages()[k] = mupb.getPages()[pp++];
                        cd.getParts()[j].getPages()[k].setData(new MmlDatum[cd.getParts()[j].getPages()[k].getLength()]);
                        for (int l = 0; l < cd.getParts()[j].getPages()[k].getLength(); l++) {
                            cd.getParts()[j].getPages()[k].getData()[l] = buf[p++];
                        }
                    }
                }
            }

            // instrument データの取り込み
            for (int i = 0; i < mupb.getUseInstrumentSetCount(); i++) {
                MupbInfo.InstrumentDefine id = mupb.getInstruments()[i];
                id.setData(new byte[id.getLength()]);
                for (int j = 0; j < id.getLength(); j++) {
                    id.getData()[j] = (byte) (buf[p++].dat & 0xff);
                }
            }

            // pcm データの取り込み
            for (int i = 0; i < mupb.getUsePCMSetCount(); i++) {
                MupbInfo.PCMDefine pd = mupb.getPcms()[i];
                pd.setData(new byte[pd.getLength()]);
                for (int j = 0; j < pd.getLength(); j++) {
                    pd.getData()[j] = (byte) (buf[p++].dat & 0xff);
                }
            }

        } else {
            throw new MubException("This data is not mucom88 data !");
        }
        srcBuf = buf;
    }

    public MmlDatum[] getDATA() {
        try {
            if (dataOffset == 0) return null;
            if (srcBuf == null) return null;

            List<MmlDatum> lb = new ArrayList<>();
            for (int i = 0; i < dataSize; i++) {
                lb.add(srcBuf[dataOffset + i]);
            }
Debug.println(srcBuf.length + ", " + dataOffset + ", " + dataSize + ", " + lb.size());

            return lb.toArray(MmlDatum[]::new);
        } catch (Exception e) {
e.printStackTrace();
            return null;
        }
    }

    public List<Tuple<String, String>> getTags() {
        try {
            if (tagData == 0) return null;
            if (srcBuf == null) return null;

            List<Byte> lb = new ArrayList<>();
            for (int i = 0; i < tagSize; i++) {
                lb.add((byte) (srcBuf[tagData + i].dat & 0xff));
            }
//Debug.println(srcBuf.length + ", " + dataOffset + ", " + dataSize + ", " + lb.size());

            return getTagsByteArray(toByteArray(lb));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] getPCM(int id) {
        try {
            if (pcmDataPtr[id] == 0) return null;
            if (srcBuf == null) return null;

            List<Byte> lb = new ArrayList<>();
            for (int i = 0; i < pcmSize[id]; i++) {
                lb.add((byte) (srcBuf[pcmDataPtr[id] + i].dat & 0xff));
            }

            return toByteArray(lb);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<Tuple<String, String>> getTagsByteArray(byte[] buf) {
        var text = Arrays.stream(new String(buf, Common.fileEncoding).split("\r\n"))
                .filter(x -> x.indexOf("#") == 0).toArray(String[]::new);

        List<Tuple<String, String>> tags = new ArrayList<>();
        for (String v : text) {
            try {
                int p = v.indexOf(' ');
                String tag = "";
                String ele = "";
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

        setDriverOptionFromTags(tags);

        return tags;
    }

    public void setDriverOptionFromTags(List<Tuple<String, String>> tags) {
        if (tags == null) return;
        if (tags.size() < 1) return;

        for (var tag : tags) {
            if (tag == null) continue;
            if (StringUtilities.isNullOrEmpty(tag.getItem1())) continue;

            if (tag.getItem1().toLowerCase().trim().equals("carriercorrection")) {
                if (!StringUtilities.isNullOrEmpty(tag.getItem2())) {
                    String val = tag.getItem2().toLowerCase().trim();
                    carrierCorrection = val.equals("yes") || val.equals("y") || val.equals("1") || val.equals("true") || val.equals("t");
                }
            } else if (tag.getItem1().toLowerCase().trim().equals("opmclockmode")) {
                if (!StringUtilities.isNullOrEmpty(tag.getItem2())) {
                    String val = tag.getItem2().toLowerCase().trim();

                    opmClockMode = enmOPMClockMode.normal;
                    if (val.equals("x68000") || val.equals("x68k") || val.equals("x68") || val.equals("x") || val.equals("4000000") || val.equals("x680x0")) {
                        opmClockMode = enmOPMClockMode.X68000;
                    }
                }
            } else if (tag.getItem1().toLowerCase().trim().equals("ssgextend")) {
                if (!StringUtilities.isNullOrEmpty(tag.getItem2())) {
                    String val = tag.getItem2().toLowerCase().trim();
                    SSGExtend = val.equals("on") || val.equals("yes") || val.equals("y") || val.equals("1") || val.equals("true") || val.equals("t");
                }
            } else if (tag.getItem1().toLowerCase().trim().equals("opna1rhythmmute")) {
                if (!StringUtilities.isNullOrEmpty(tag.getItem2())) {
                    String val = tag.getItem2().toLowerCase().trim();
                    rhythmMute[0] = 0;
                    if (val.indexOf('b') > -1) rhythmMute[0] |= 1;
                    if (val.indexOf('s') > -1) rhythmMute[0] |= 2;
                    if (val.indexOf('c') > -1) rhythmMute[0] |= 4;
                    if (val.indexOf('h') > -1) rhythmMute[0] |= 8;
                    if (val.indexOf('t') > -1) rhythmMute[0] |= 16;
                    if (val.indexOf('r') > -1) rhythmMute[0] |= 32;
                    rhythmMute[0] = (~rhythmMute[0]) & 0b0011_1111;
                }
            } else if (tag.getItem1().toLowerCase().trim().equals("opna2rhythmmute")) {
                if (!StringUtilities.isNullOrEmpty(tag.getItem2())) {
                    String val = tag.getItem2().toLowerCase().trim();

                    rhythmMute[1] = 0;
                    if (val.indexOf('b') > -1) rhythmMute[1] |= 1;
                    if (val.indexOf('s') > -1) rhythmMute[1] |= 2;
                    if (val.indexOf('c') > -1) rhythmMute[1] |= 4;
                    if (val.indexOf('h') > -1) rhythmMute[1] |= 8;
                    if (val.indexOf('t') > -1) rhythmMute[1] |= 16;
                    if (val.indexOf('r') > -1) rhythmMute[1] |= 32;
                    rhythmMute[1] = (~rhythmMute[1]) & 0b0011_1111;
                }
            } else if (tag.getItem1().toLowerCase().trim().equals("opnb1adpcmamute")) {
                if (!StringUtilities.isNullOrEmpty(tag.getItem2())) {
                    String val = tag.getItem2().toLowerCase().trim();

                    rhythmMute[2] = 0;
                    if (val.indexOf('1') > -1) rhythmMute[2] |= 1;
                    if (val.indexOf('2') > -1) rhythmMute[2] |= 2;
                    if (val.indexOf('3') > -1) rhythmMute[2] |= 4;
                    if (val.indexOf('4') > -1) rhythmMute[2] |= 8;
                    if (val.indexOf('5') > -1) rhythmMute[2] |= 16;
                    if (val.indexOf('6') > -1) rhythmMute[2] |= 32;
                    rhythmMute[2] = (~rhythmMute[2]) & 0b0011_1111;
                }
            } else if (tag.getItem1().toLowerCase().trim().equals("opnb2adpcmamute")) {
                if (!StringUtilities.isNullOrEmpty(tag.getItem2())) {
                    String val = tag.getItem2().toLowerCase().trim();

                    rhythmMute[3] = 0;
                    if (val.indexOf('1') > -1) rhythmMute[3] |= 1;
                    if (val.indexOf('2') > -1) rhythmMute[3] |= 2;
                    if (val.indexOf('3') > -1) rhythmMute[3] |= 4;
                    if (val.indexOf('4') > -1) rhythmMute[3] |= 8;
                    if (val.indexOf('5') > -1) rhythmMute[3] |= 16;
                    if (val.indexOf('6') > -1) rhythmMute[3] |= 32;
                    rhythmMute[3] = (~rhythmMute[3]) & 0b0011_1111;
                }
            }
        }
    }
}
