package mucom88.compiler.PCMTool;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import dotnet4j.io.Stream;
import dotnet4j.util.compat.StringUtilities;


public class PCMFileManager {

    private Map<Integer, PCMFileInfo> dicFile = new HashMap<>();
    private Config config;
    private Function<String, Stream> appendFileReaderCallback;

    public PCMFileManager(Config config, Function<String, Stream> appendFileReaderCallback /*=null*/) {
        this.config = config;
        this.appendFileReaderCallback = appendFileReaderCallback;
    }

    public void Add(String lin) {
        if (StringUtilities.isNullOrEmpty(lin)) return;
        if (lin.length() < 3) return;

        List<String> itemList = AnalyzeLine(lin);
        PCMFileInfo fi = new PCMFileInfo(itemList, appendFileReaderCallback);
        if (dicFile.containsKey(fi.getnumber() - 1)) dicFile.remove(fi.getnumber() - 1);
        dicFile.put(fi.getnumber() - 1, fi);
        if (fi.getlength() > -1) fi.Encode(config.FormatType);
    }

    public List<Byte> GetRawData() {
        List<Byte> ret = new ArrayList<>();
        int num = 0;
        int cnt = 0;
        while (cnt < dicFile.size() && num < 65536) {
            if (dicFile.containsKey(num)) {
                PCMFileInfo o = dicFile.get(num);
                if (o.getencData() != null)
                    for (byte d : o.getencData()) ret.add(d);
                else if (o.getraw() != null)
                    for (byte d : o.getraw()) ret.add(d);

                cnt++;
            }
            num++;
        }

        return ret;
    }

    public List<Byte> GetName(int i, int v) {
        List<Byte> ret = new ArrayList<>();

        if (!dicFile.containsKey(i) || dicFile.get(i) == null || StringUtilities.isNullOrEmpty(dicFile.get(i).getname())) {
            for (int n = 0; n < v; n++) ret.add((byte) 0);
            return ret;
        }

        byte[] data = dicFile.get(i).getname().getBytes(Charset.forName("MS932"));
        for (int n = 0; n < v; n++) {
            if (n < data.length)
                ret.add(data[n]);
            else
                ret.add((byte) 0x20);
        }

        return ret;
    }

    public short GetVolume(int i) {
        if (!dicFile.containsKey(i) || dicFile.get(i) == null) {
            return 0;
        }
        return (short) dicFile.get(i).getvolume();
    }

    public int GetLengthAddress(int i) {
        if (!dicFile.containsKey(i) || dicFile.get(i) == null) {
            return 0;
        }
        return dicFile.get(i).getlength();
    }

    public List<Byte> GetName(int i) {
        List<Byte> ret = new ArrayList<>();

        if (!dicFile.containsKey(i) || dicFile.get(i) == null || StringUtilities.isNullOrEmpty(dicFile.get(i).getname())) {
            ret.add((byte) 0);
            return ret;
        }

        byte[] data = dicFile.get(i).getname().getBytes(Charset.forName("MS932"));
        for (byte datum : data) {
            ret.add(datum);
        }
        ret.add((byte) 0);

        return ret;
    }

    public int GetCount() {
        int i = 0;
        for (PCMFileInfo o : dicFile.values()) {
            i = Math.max(i, o.getnumber());
        }

        return i;
    }


    private List<String> AnalyzeLine(String lin) {
        List<String> itemList = new ArrayList<>();
        int pos = 0;
        StringBuilder item = new StringBuilder();
        boolean str = false;
        while (pos < lin.length()) {
            if (lin.charAt(pos) == '"') {
                if (pos + 1 < lin.length() && lin.charAt(pos + 1) == '"' && str) {
                    pos++;
                } else {
                    str = !str;
                    pos++;
                    continue;
                }
            }

            if (lin.charAt(pos) == ',' && !str) {
                itemList.add(item.toString().trim());
                pos++;
                item = new StringBuilder();
                continue;
            }

            item.append(lin.charAt(pos++));
        }

        if (!StringUtilities.isNullOrEmpty(item.toString())) {
            itemList.add(item.toString().trim());
        }

        return itemList;
    }
}
