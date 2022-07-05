package mucom88.compiler.PCMTool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import dotnet4j.io.Stream;
import dotnet4j.util.compat.StringUtilities;
import mdsound.Common;


public class AdpcmMaker {

    private String[] src;
    private int i = -1;
    private List<String> list = null;
    private Function<String, Stream> appendFileReaderCallback = null;

    public AdpcmMaker(String[] src) {
        this.src = src;
    }

    public AdpcmMaker(int i, List<String> list, Function<String, Stream> appendFileReaderCallback) {
        this.i = i;
        this.list = list;
        this.appendFileReaderCallback = appendFileReaderCallback;
    }

    public byte[] make() {
        Config config;
        PCMFileManager fileManager;
        if (i == -1) {
            config = GetConfig();
            fileManager = GetPCMFiles(config);
            return make(config, fileManager);
        } else {
            config = new Config();
            if (i == 0) config.FormatType = FormatType.mucom88;
            else if (i == 1) config.FormatType = FormatType.mucomDotNET_OPNA_ADPCM;
            else if (i == 2) config.FormatType = FormatType.mucomDotNET_OPNB_ADPCMB;
            else if (i == 3) config.FormatType = FormatType.mucomDotNET_OPNB_ADPCMB;
            else if (i == 4) config.FormatType = FormatType.mucomDotNET_OPNB_ADPCMA;
            else if (i == 5) config.FormatType = FormatType.mucomDotNET_OPNB_ADPCMA;
            fileManager = new PCMFileManager(config, appendFileReaderCallback);
            for (String line : list) {
                String lin = line.trim();
                if (StringUtilities.isNullOrEmpty(lin)) continue;
                lin = CutComment(lin).trim();
                if (StringUtilities.isNullOrEmpty(lin)) continue;

                fileManager.Add(lin);
            }
            return make(config, fileManager);
        }
    }

    private String CutComment(String lin) {
        String ret = "";
        boolean strFlg = false;

        for (int i = 0; i < lin.length(); i++) {
            char ch = lin.charAt(i);
            char chn = i + 1 < lin.length() ? lin.charAt(i + 1) : '\0';

            if (ch == ';' && !strFlg) break;
            if (ch == '"' && (strFlg && chn != '"')) strFlg = !strFlg;
            ret += ch;
        }

        return ret;
    }

    private Config GetConfig() {
        Config config = new Config();

        for (String line : src) {
            String lin = line.trim();
            if (StringUtilities.isNullOrEmpty(lin)) continue;
            if (lin.charAt(0) != '#') continue;

            config.Add(lin);
        }

        return config;
    }

    private PCMFileManager GetPCMFiles(Config config) {
        PCMFileManager filemanager = new PCMFileManager(config, null);

        for (String line : src) {
            String lin = line.trim();
            if (StringUtilities.isNullOrEmpty(lin)) continue;
            if (lin.charAt(0) != '@') continue;
            if (lin.length() > 1) continue;

            filemanager.Add(lin.substring(1));
        }

        return filemanager;
    }

    private byte[] make(Config config, PCMFileManager fileManager) {
        List<Byte> dst = new ArrayList<>();
        dst = MakeHeader(config, fileManager, dst);
        List<Byte> raw = fileManager.GetRawData();
        if (raw != null) dst.addAll(raw);

        return Common.toByteArray(dst);
    }

    private List<Byte> MakeHeader(Config config, PCMFileManager fileManager, List<Byte> dst) {
        switch (config.FormatType) {
        case mucom88:
            dst.addAll(MakeHeader_mucom88(fileManager));
            break;
        case mucomDotNET_OPNA_ADPCM:
            dst.addAll(MakeHeader_mucomDotNET_OPNA_ADPCM(fileManager));
            break;
        case mucomDotNET_OPNB_ADPCMB:
            dst.addAll(MakeHeader_mucomDotNET_OPNB_ADPCMB(fileManager));
            break;
        case mucomDotNET_OPNB_ADPCMA:
            dst.addAll(MakeHeader_mucomDotNET_OPNB_ADPCMA(fileManager));
            break;
        }

        return dst;
    }

    private List<Byte> MakeHeader_mucom88(PCMFileManager fileManager) {
        List<Byte> head = new ArrayList<>();
        int ptr = 0;
        for (int i = 0; i < 32; i++) {
            head.addAll(fileManager.GetName(i, 16)); // instrument name 16byte
            head.addAll(Arrays.asList((byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0));//dummy 10byte(limit/sample rate/volume etc.)
            head.add((byte) (fileManager.GetVolume(i)));
            head.add((byte) (fileManager.GetVolume(i) >> 8));
            int length = fileManager.GetLengthAddress(i);
            if (length < 1) {
                head.add((byte) 0);
                head.add((byte) 0);
                head.add((byte) 0);
                head.add((byte) 0);
            } else {
                short stAdr = (short) (ptr >> 2);
                ptr += length - 1;
                short edAdr = (short) length; // (short) ptr;// (ptr >> 2);
                ptr++;
                head.add((byte) (stAdr));
                head.add((byte) (stAdr >> 8));
                head.add((byte) (edAdr));
                head.add((byte) (edAdr >> 8));
            }
        }
        return head;
    }

    private List<Byte> MakeHeader_mucomDotNET_OPNA_ADPCM(PCMFileManager fileManager) {
        List<Byte> head = new ArrayList<>();
        int ptr = 0;

        head.add((byte) 'm');
        head.add((byte) 'd');
        head.add((byte) 'a');
        head.add((byte) ' ');

        int num = fileManager.GetCount();
        head.add((byte) num);
        head.add((byte) (num >> 8));

        for (int i = 0; i <= num; i++) {
            head.addAll(fileManager.GetName(i)); // instrument name 16byte
            head.add((byte) 3);
            head.add((byte) (fileManager.GetVolume(i)));
            head.add((byte) (fileManager.GetVolume(i) >> 8));
            int length = fileManager.GetLengthAddress(i);
            if (length < 1) {
                head.add((byte) 0);
                head.add((byte) 0);
                head.add((byte) 0);
                head.add((byte) 0);
            } else {
                short stAdr = (short) (ptr >> 2);
                ptr += length - 1;
                short edAdr = (short) (ptr >> 2);
                ptr++;
                head.add((byte) (stAdr));
                head.add((byte) (stAdr >> 8));
                head.add((byte) (edAdr));
                head.add((byte) (edAdr >> 8));
            }
        }
        return head;
    }

    private List<Byte> MakeHeader_mucomDotNET_OPNB_ADPCMB(PCMFileManager fileManager) {
        List<Byte> head = new ArrayList<>();
        int ptr = 0;

        head.add((byte) 'm');
        head.add((byte) 'd');
        head.add((byte) 'b');
        head.add((byte) 'b');

        int num = fileManager.GetCount();
        head.add((byte) num);
        head.add((byte) (num >> 8));

        for (int i = 0; i <= num; i++) {
            head.addAll(fileManager.GetName(i)); // instrument name 16byte
            head.add((byte) 3);
            head.add((byte) (fileManager.GetVolume(i)));
            head.add((byte) (fileManager.GetVolume(i) >> 8));
            int length = fileManager.GetLengthAddress(i);
            if (length < 1) {
                head.add((byte) 0);
                head.add((byte) 0);
                head.add((byte) 0);
                head.add((byte) 0);
            } else {
                short stAdr = (short) (ptr >> 8);
                ptr += length - 1;
                short edAdr = (short) (ptr >> 8);
                ptr++;
                head.add((byte) (stAdr));
                head.add((byte) (stAdr >> 8));
                head.add((byte) (edAdr));
                head.add((byte) (edAdr >> 8));
            }
        }
        return head;
    }

    private List<Byte> MakeHeader_mucomDotNET_OPNB_ADPCMA(PCMFileManager fileManager) {
        List<Byte> head = new ArrayList<>();
        int ptr = 0;

        head.add((byte) 'm');
        head.add((byte) 'd');
        head.add((byte) 'b');
        head.add((byte) 'a');

        int num = fileManager.GetCount();
        head.add((byte) num);
        head.add((byte) (num >> 8));

        for (int i = 0; i <= num; i++) {
            head.addAll(fileManager.GetName(i)); // instrument name 16byte
            head.add((byte) 3);
            head.add((byte) (fileManager.GetVolume(i)));
            head.add((byte) (fileManager.GetVolume(i) >> 8));
            int length = fileManager.GetLengthAddress(i);
            if (length < 1) {
                head.add((byte) 0);
                head.add((byte) 0);
                head.add((byte) 0);
                head.add((byte) 0);
            } else {
                short stAdr = (short) (ptr >> 8);
                ptr += length - 1;
                short edAdr = (short) (ptr >> 8);
                ptr++;
                head.add((byte) (stAdr));
                head.add((byte) (stAdr >> 8));
                head.add((byte) (edAdr));
                head.add((byte) (edAdr >> 8));
            }
        }
        return head;
    }
}
