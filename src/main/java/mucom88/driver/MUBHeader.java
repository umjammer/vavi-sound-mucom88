package mucom88.driver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dotnet4j.util.compat.StringUtilities;
import dotnet4j.util.compat.Tuple;
import mucom88.common.Common;
import mucom88.common.MubException;
import mucom88.common.iEncoding;
import musicDriverInterface.MmlDatum;


public class MUBHeader {
    public int magic = 0;
    public int dataoffset = 0;
    public int datasize = 0;
    public int tagdata = 0;
    public int tagsize = 0;
    public int[] pcmdataPtr = new int[] {0, 0, 0, 0, 0, 0};
    public int[] pcmsize = new int[] {0, 0, 0, 0, 0, 0};
    public int jumpcount = 0;
    public int jumpline = 0;
    public int ext_flags = 0;
    public int ext_system = 0;
    public int ext_target = 0;
    public int ext_channel_num = 0;
    public int ext_fmvoice_num = 0;
    public int ext_player = 0;
    public int pad1 = 0;
    public byte[] ext_fmvoice = new byte[32];
    private MmlDatum[] srcBuf = null;
    private iEncoding enc = null;
    public MupbInfo mupb;
    private int mupbDataPtr;
    public boolean CarrierCorrection = false;
    public enmOPMClockMode OPMClockMode = enmOPMClockMode.normal;
    public boolean SSGExtend = false;
    public int[] RhythmMute = new int[] {0x3f, 0x3f, 0x3f, 0x3f};

    public enum enmOPMClockMode {
        normal, X68000
    }

    public MUBHeader(MmlDatum[] buf, iEncoding enc) {
        CarrierCorrection = false;
        OPMClockMode = enmOPMClockMode.normal;
        SSGExtend = false;

        this.enc = enc;

        magic = Common.getLE32(buf, 0x0000);

        if (magic == 0x3843554d) //'MUC8'
        {
            dataoffset = Common.getLE32(buf, 0x0004);
            datasize = Common.getLE32(buf, 0x0008);
            tagdata = Common.getLE32(buf, 0x000c);
            tagsize = Common.getLE32(buf, 0x0010);
            pcmdataPtr[0] = Common.getLE32(buf, 0x0014);
            pcmsize[0] = Common.getLE32(buf, 0x0018);
            jumpcount = Common.getLE16(buf, 0x001c);
            jumpline = Common.getLE16(buf, 0x001e);
        } else if (magic == 0x3842554d) //'MUB8'
        {
            dataoffset = Common.getLE32(buf, 0x0004);
            datasize = Common.getLE32(buf, 0x0008);
            tagdata = Common.getLE32(buf, 0x000c);
            tagsize = Common.getLE32(buf, 0x0010);
            pcmdataPtr[0] = Common.getLE32(buf, 0x0014);
            pcmsize[0] = Common.getLE32(buf, 0x0018);
            jumpcount = Common.getLE16(buf, 0x001c);
            jumpline = Common.getLE16(buf, 0x001e);

            ext_flags = Common.getLE16(buf, 0x0020);
            ext_system = buf[0x0022].dat;
            ext_target = buf[0x0023].dat;
            ext_channel_num = Common.getLE16(buf, 0x0024);
            ext_fmvoice_num = Common.getLE16(buf, 0x0026);
            ext_player = Common.getLE32(buf, 0x0028);
            pad1 = Common.getLE32(buf, 0x002c);
            for (int i = 0; i < 32; i++) {
                ext_fmvoice[i] = (byte) buf[0x0030 + i].dat;
            }
        } else if (magic == 0x6250756d) {
            mupb = new MupbInfo();
            mupb.setversion(Common.getLE32(buf, 0x0004));
            mupb.setvariableLengthCount(buf[0x0008].dat);
            mupb.setuseChipCount(buf[0x0009].dat);
            mupb.setusePartCount(Common.getLE16(buf, 0x000a));
            mupb.setusePageCount(Common.getLE16(buf, 0x000c));
            mupb.setuseInstrumentSetCount(Common.getLE16(buf, 0x000e));
            mupb.setusePCMSetCount(Common.getLE16(buf, 0x0010));
            tagdata = Common.getLE32(buf, 0x0012);
            tagsize = Common.getLE32(buf, 0x0016);
            jumpcount = Common.getLE32(buf, 0x001a);
            jumpline = Common.getLE32(buf, 0x001e);

            int ptr = 0x0022;

            //Chip Define division.
            mupb.setchips(new MupbInfo.ChipDefine[mupb.getuseChipCount()]);
            for (int i = 0; i < mupb.getuseChipCount(); i++) {
                mupb.getchips()[i] = new MupbInfo.ChipDefine();
                MupbInfo.ChipDefine cd = mupb.getchips()[i];
                cd.setindexNumber(Common.getLE16(buf, ptr));
                ptr += 2;
                cd.setidentifyNumber(Common.getLE32(buf, ptr));
                ptr += 4;
                cd.setmasterClock(Common.getLE32(buf, ptr));
                ptr += 4;
                cd.setoption(Common.getLE32(buf, ptr));
                ptr += 4;
                cd.setheartBeat(Common.getLE32(buf, ptr));
                ptr += 4;
                cd.setheartBeat2(Common.getLE32(buf, ptr));
                ptr += 4;

                cd.setparts(new MupbInfo.ChipDefine.ChipPart[buf[ptr].dat]);
                ptr++;
                for (int j = 0; j < cd.getparts().length; j++) {
                    cd.getparts()[j] = new MupbInfo.ChipDefine.ChipPart();
                }

                cd.setinstrumentNumber(new int[buf[ptr].dat]);
                ptr++;
                for (int j = 0; j < cd.getinstrumentNumber().length; j++) {
                    cd.getinstrumentNumber()[j] = Common.getLE16(buf, ptr);
                    ptr += 2;
                }

                cd.setpcmNumber(new int[buf[ptr].dat]);
                ptr++;
                for (int j = 0; j < cd.getpcmNumber().length; j++) {
                    cd.getpcmNumber()[j] = Common.getLE16(buf, ptr);
                    ptr += 2;
                }
            }

            //Part division.
            mupb.setparts(new MupbInfo.PartDefine[mupb.getusePartCount()]);
            for (int i = 0; i < mupb.getusePartCount(); i++) {
                mupb.getparts()[i] = new MupbInfo.PartDefine();
                MupbInfo.PartDefine pd = mupb.getparts()[i];
                pd.setpageCount(buf[ptr].dat);
                ptr++;
            }

            //Page division.
            mupb.setpages(new MupbInfo.PageDefine[mupb.getusePageCount()]);
            for (int i = 0; i < mupb.getusePageCount(); i++) {
                mupb.getpages()[i] = new MupbInfo.PageDefine();
                MupbInfo.PageDefine pd = mupb.getpages()[i];
                pd.setlength(Common.getLE32(buf, ptr));
                ptr += 4;
                pd.setloopPoint(Common.getLE32(buf, ptr));
                ptr += 4;
            }

            //Instrument set division.
            mupb.setinstruments(new MupbInfo.InstrumentDefine[mupb.getuseInstrumentSetCount()]);
            for (int i = 0; i < mupb.getuseInstrumentSetCount(); i++) {
                mupb.getinstruments()[i] = new MupbInfo.InstrumentDefine();
                MupbInfo.InstrumentDefine id = mupb.getinstruments()[i];
                id.setlength(Common.getLE32(buf, ptr));
                ptr += 4;
            }

            //PCM set division.
            mupb.setpcms(new MupbInfo.PCMDefine[mupb.getusePCMSetCount()]);
            for (int i = 0; i < mupb.getusePCMSetCount(); i++) {
                mupb.getpcms()[i] = new MupbInfo.PCMDefine();
                MupbInfo.PCMDefine pd = mupb.getpcms()[i];
                pd.setlength(Common.getLE32(buf, ptr));
                ptr += 4;
            }

            //データ部開始位置
            mupbDataPtr = ptr;

            //Chip毎のページ情報割り当てとページデータの取り込み
            int pp = 0;
            int pr = 0;
            for (int i = 0; i < mupb.getuseChipCount(); i++) {
                MupbInfo.ChipDefine cd = mupb.getchips()[i];
                for (int j = 0; j < cd.getparts().length; j++) {
                    cd.getparts()[j].setpages(new MupbInfo.PageDefine[mupb.getparts()[pr++].getpageCount()]);
                    for (int k = 0; k < cd.getparts()[j].getpages().length; k++) {
                        cd.getparts()[j].getpages()[k] = mupb.getpages()[pp++];
                        cd.getparts()[j].getpages()[k].setdata(new MmlDatum[cd.getparts()[j].getpages()[k].getlength()]);
                        for (int l = 0; l < cd.getparts()[j].getpages()[k].getlength(); l++) {
                            cd.getparts()[j].getpages()[k].getdata()[l] = buf[ptr++];
                        }
                    }
                }
            }

            //instrumentデータの取り込み
            for (int i = 0; i < mupb.getuseInstrumentSetCount(); i++) {
                MupbInfo.InstrumentDefine id = mupb.getinstruments()[i];
                id.setdata(new byte[id.getlength()]);
                for (int j = 0; j < id.getlength(); j++) {
                    id.getdata()[j] = (byte) buf[ptr++].dat;
                }
            }

            //pcmデータの取り込み
            for (int i = 0; i < mupb.getusePCMSetCount(); i++) {
                MupbInfo.PCMDefine pd = mupb.getpcms()[i];
                pd.setdata(new byte[pd.getlength()]);
                for (int j = 0; j < pd.getlength(); j++) {
                    pd.getdata()[j] = (byte) buf[ptr++].dat;
                }
            }

        } else {
            throw new MubException("This data is not mucom88 data !");
        }
        srcBuf = buf;
    }

    public MmlDatum[] GetDATA() {
        try {
            if (dataoffset == 0) return null;
            if (srcBuf == null) return null;

            List<MmlDatum> lb = new ArrayList<MmlDatum>();
            for (int i = 0; i < datasize; i++) {
                lb.add(srcBuf[dataoffset + i]);
            }

            return lb.toArray(MmlDatum[]::new);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Tuple<String, String>> GetTags() {
        try {
            if (tagdata == 0) return null;
            if (srcBuf == null) return null;

            List<Byte> lb = new ArrayList<>();
            for (int i = 0; i < tagsize; i++) {
                lb.add((byte) srcBuf[tagdata + i].dat);
            }

            return GetTagsByteArray(mdsound.Common.toByteArray(lb));
        } catch (Exception e) {
            return null;
        }
    }

    public byte[] GetPCM(int id) {
        try {
            if (pcmdataPtr[id] == 0) return null;
            if (srcBuf == null) return null;

            List<Byte> lb = new ArrayList<>();
            for (int i = 0; i < pcmsize[id]; i++) {
                lb.add((byte) srcBuf[pcmdataPtr[id] + i].dat);
            }

            return mdsound.Common.toByteArray(lb);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Tuple<String, String>> GetTagsByteArray(byte[] buf) {
        var text = Arrays.stream(enc.getStringFromSjisArray(buf).split("\n"))
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
                    Tuple<String, String> item = new Tuple<String, String>(tag, ele);
                    tags.add(item);
                }
            } catch (Exception e) {
            }
        }

        SetDriverOptionFromTags(tags);

        return tags;
    }

    public void SetDriverOptionFromTags(List<Tuple<String, String>> tags) {
        if (tags == null) return;
        if (tags.size() < 1) return;

        for (var tag : tags) {
            if (tag == null) continue;
            if (StringUtilities.isNullOrEmpty(tag.getItem1())) continue;

            if (tag.getItem1().toLowerCase().trim().equals("carriercorrection")) {
                if (!StringUtilities.isNullOrEmpty(tag.getItem2())) {
                    String val = tag.getItem2().toLowerCase().trim();

                    CarrierCorrection = false;
                    if (val.equals("yes") || val.equals("y") || val.equals("1") || val.equals("true") || val.equals("t")) {
                        CarrierCorrection = true;
                    }
                }
            } else if (tag.getItem1().toLowerCase().trim().equals("opmclockmode")) {
                if (!StringUtilities.isNullOrEmpty(tag.getItem2())) {
                    String val = tag.getItem2().toLowerCase().trim();

                    OPMClockMode = enmOPMClockMode.normal;
                    if (val.equals("x68000") || val.equals("x68k") || val.equals("x68") || val.equals("x") || val.equals("4000000") || val.equals("x680x0")) {
                        OPMClockMode = enmOPMClockMode.X68000;
                    }
                }
            } else if (tag.getItem1().toLowerCase().trim().equals("ssgextend")) {
                if (!StringUtilities.isNullOrEmpty(tag.getItem2())) {
                    String val = tag.getItem2().toLowerCase().trim();

                    SSGExtend = false;
                    if (val.equals("on") || val.equals("yes") || val.equals("y") || val.equals("1") || val.equals("true") || val.equals("t")) {
                        SSGExtend = true;
                    }
                }
            } else if (tag.getItem1().toLowerCase().trim().equals("opna1rhythmmute")) {
                if (!StringUtilities.isNullOrEmpty(tag.getItem2())) {
                    String val = tag.getItem2().toLowerCase().trim();
                    RhythmMute[0] = 0;
                    if (val.indexOf('b') > -1) RhythmMute[0] |= 1;
                    if (val.indexOf('s') > -1) RhythmMute[0] |= 2;
                    if (val.indexOf('c') > -1) RhythmMute[0] |= 4;
                    if (val.indexOf('h') > -1) RhythmMute[0] |= 8;
                    if (val.indexOf('t') > -1) RhythmMute[0] |= 16;
                    if (val.indexOf('r') > -1) RhythmMute[0] |= 32;
                    RhythmMute[0] = (~RhythmMute[0]) & 0b0011_1111;
                }
            } else if (tag.getItem1().toLowerCase().trim().equals("opna2rhythmmute")) {
                if (!StringUtilities.isNullOrEmpty(tag.getItem2())) {
                    String val = tag.getItem2().toLowerCase().trim();

                    RhythmMute[1] = 0;
                    if (val.indexOf('b') > -1) RhythmMute[1] |= 1;
                    if (val.indexOf('s') > -1) RhythmMute[1] |= 2;
                    if (val.indexOf('c') > -1) RhythmMute[1] |= 4;
                    if (val.indexOf('h') > -1) RhythmMute[1] |= 8;
                    if (val.indexOf('t') > -1) RhythmMute[1] |= 16;
                    if (val.indexOf('r') > -1) RhythmMute[1] |= 32;
                    RhythmMute[1] = (~RhythmMute[1]) & 0b0011_1111;
                }
            } else if (tag.getItem1().toLowerCase().trim().equals("opnb1adpcmamute")) {
                if (!StringUtilities.isNullOrEmpty(tag.getItem2())) {
                    String val = tag.getItem2().toLowerCase().trim();

                    RhythmMute[2] = 0;
                    if (val.indexOf('1') > -1) RhythmMute[2] |= 1;
                    if (val.indexOf('2') > -1) RhythmMute[2] |= 2;
                    if (val.indexOf('3') > -1) RhythmMute[2] |= 4;
                    if (val.indexOf('4') > -1) RhythmMute[2] |= 8;
                    if (val.indexOf('5') > -1) RhythmMute[2] |= 16;
                    if (val.indexOf('6') > -1) RhythmMute[2] |= 32;
                    RhythmMute[2] = (~RhythmMute[2]) & 0b0011_1111;
                }
            } else if (tag.getItem1().toLowerCase().trim().equals("opnb2adpcmamute")) {
                if (!StringUtilities.isNullOrEmpty(tag.getItem2())) {
                    String val = tag.getItem2().toLowerCase().trim();

                    RhythmMute[3] = 0;
                    if (val.indexOf('1') > -1) RhythmMute[3] |= 1;
                    if (val.indexOf('2') > -1) RhythmMute[3] |= 2;
                    if (val.indexOf('3') > -1) RhythmMute[3] |= 4;
                    if (val.indexOf('4') > -1) RhythmMute[3] |= 8;
                    if (val.indexOf('5') > -1) RhythmMute[3] |= 16;
                    if (val.indexOf('6') > -1) RhythmMute[3] |= 32;
                    RhythmMute[3] = (~RhythmMute[3]) & 0b0011_1111;
                }
            }
        }
    }
}
