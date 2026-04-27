package mucom88.compiler.pcmTool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import dotnet4j.io.Stream;
import dotnet4j.util.compat.StringUtilities;

import static mucom88.common.Common.charset;


public class PCMFileManager {

    private final Map<Integer, PCMFileInfo> dicFile = new HashMap<>();
    private final Config config;
    private final Function<String, Stream> appendFileReaderCallback;

    public PCMFileManager(Config config, Function<String, Stream> appendFileReaderCallback /* = null */) {
        this.config = config;
        this.appendFileReaderCallback = appendFileReaderCallback;
    }

    public void add(String lin) {
        if (StringUtilities.isNullOrEmpty(lin)) return;
        if (lin.length() < 3) return;

        List<String> itemList = analyzeLine(lin);
        PCMFileInfo fi = new PCMFileInfo(itemList, appendFileReaderCallback);
        dicFile.remove(fi.getNumber() - 1);
        dicFile.put(fi.getNumber() - 1, fi);
        if (fi.getLength() > -1) fi.encode(config.FormatType);
    }

    public List<Byte> getRawData() {
        List<Byte> ret = new ArrayList<>();
        int num = 0;
        int cnt = 0;
        while (cnt < dicFile.size() && num < 65536) {
            if (dicFile.containsKey(num)) {
                PCMFileInfo o = dicFile.get(num);
                if (o.getEncData() != null)
                    for (byte d : o.getEncData()) ret.add(d);
                else if (o.getRaw() != null)
                    for (byte d : o.getRaw()) ret.add(d);

                cnt++;
            }
            num++;
        }

        return ret;
    }

    public List<Byte> getName(int i, int v) {
        List<Byte> ret = new ArrayList<>();

        if (!dicFile.containsKey(i) || dicFile.get(i) == null || StringUtilities.isNullOrEmpty(dicFile.get(i).getName())) {
            for (int n = 0; n < v; n++) ret.add((byte) 0);
            return ret;
        }

        byte[] data = dicFile.get(i).getName().getBytes(charset);
        for (int n = 0; n < v; n++) {
            if (n < data.length)
                ret.add(data[n]);
            else
                ret.add((byte) 0x20);
        }

        return ret;
    }

    public int getVolume(int i) {
        if (!dicFile.containsKey(i) || dicFile.get(i) == null) {
            return 0;
        }
        return dicFile.get(i).getVolume();
    }

    public int getLengthAddress(int i) {
        if (!dicFile.containsKey(i) || dicFile.get(i) == null) {
            return 0;
        }
        return dicFile.get(i).getLength();
    }

    public List<Byte> getName(int i) {
        List<Byte> ret = new ArrayList<>();

        if (!dicFile.containsKey(i) || dicFile.get(i) == null || StringUtilities.isNullOrEmpty(dicFile.get(i).getName())) {
            ret.add((byte) 0);
            return ret;
        }

        byte[] data = dicFile.get(i).getName().getBytes(charset);
        for (byte datum : data) {
            ret.add(datum);
        }
        ret.add((byte) 0);

        return ret;
    }

    public int getCount() {
        int i = 0;
        for (PCMFileInfo o : dicFile.values()) {
            i = Math.max(i, o.getNumber());
        }

        return i;
    }

    private static List<String> analyzeLine(String lin) {
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
