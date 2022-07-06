package mucom88.driver;

import musicDriverInterface.MmlDatum;


public class MupbInfo {

    private int version;

    public int getVersion() {
        return version;
    }

    public void setVersion(int value) {
        version = value;
    }

    private int variableLengthCount;

    public int getVariableLengthCount() {
        return variableLengthCount;
    }

    public void setVariableLengthCount(int value) {
        variableLengthCount = value;
    }

    private int useChipCount;

    public int getUseChipCount() {
        return useChipCount;
    }

    public void setUseChipCount(int value) {
        useChipCount = value;
    }

    private int usePartCount;

    public int getPsePartCount() {
        return usePartCount;
    }

    public void setUsePartCount(int value) {
        usePartCount = value;
    }

    private int usePageCount;

    public int getUsePageCount() {
        return usePageCount;
    }

    public void setUsePageCount(int value) {
        usePageCount = value;
    }

    private int useInstrumentSetCount;

    public int getUseInstrumentSetCount() {
        return useInstrumentSetCount;
    }

    public void setUseInstrumentSetCount(int value) {
        useInstrumentSetCount = value;
    }

    private int usePCMSetCount;

    public int getUsePCMSetCount() {
        return usePCMSetCount;
    }

    public void setUsePCMSetCount(int value) {
        usePCMSetCount = value;
    }

    private ChipDefine[] chips;

    public ChipDefine[] getChips() {
        return chips;
    }

    public void setChips(ChipDefine[] value) {
        chips = value;
    }

    private int tagDataOffset;

    public int getTagDataOffset() {
        return tagDataOffset;
    }

    public void setTagDataOffset(int value) {
        tagDataOffset = value;
    }

    private int tagDataSize;

    public int getTagDataSize() {
        return tagDataSize;
    }

    public void setTagDataSize(int value) {
        tagDataSize = value;
    }

    private int jClock;

    public int getJClock() {
        return jClock;
    }

    public void setJClock(int value) {
        jClock = value;
    }

    private int jpLine;

    public int getJpLine() {
        return jpLine;
    }

    public void setJpLine(int value) {
        jpLine = value;
    }

    private PartDefine[] parts;

    public PartDefine[] getParts() {
        return parts;
    }

    public void setParts(PartDefine[] value) {
        parts = value;
    }

    private PageDefine[] pages;

    public PageDefine[] getPages()
    {
        return pages;
    }
    public void setPages(PageDefine[] value)
    {
        pages = value;
    }

    private InstrumentDefine[] instruments;

    public InstrumentDefine[] getInstruments() {
        return instruments;
    }

    public void setInstruments(InstrumentDefine[] value) {
        instruments = value;
    }

    private PCMDefine[] pcms;

    public PCMDefine[] getPcms() {
        return pcms;
    }

    public void setPcms(PCMDefine[] value) {
        pcms = value;
    }

    public static class ChipDefine {
        private int indexNumber;

        public int getIndexNumber() {
            return indexNumber;
        }

        public void setIndexNumber(int value) {
            indexNumber = value;
        }

        private int identifyNumber;

        public int getIdentifyNumber() {
            return identifyNumber;
        }

        public void setIdentifyNumber(int value) {
            identifyNumber = value;
        }

        private int masterClock;

        public int getMasterClock() {
            return masterClock;
        }

        public void setMasterClock(int value) {
            masterClock = value;
        }

        private int option;

        public int getOption() {
            return option;
        }

        public void setOption(int value) {
            option = value;
        }

        private int heartBeat;

        public int getHeartBeat() {
            return heartBeat;
        }

        public void setHeartBeat(int value) {
            heartBeat = value;
        }

        private int heartBeat2;

        public int getHeartBeat2() {
            return heartBeat2;
        }

        public void setHeartBeat2(int value) {
            heartBeat2 = value;
        }

        private int[] instrumentNumber;

        public int[] getInstrumentNumber() {
            return instrumentNumber;
        }

        public void setInstrumentNumber(int[] value) {
            instrumentNumber = value;
        }

        private int[] pcmNumber;

        public int[] getPcmNumber() {
            return pcmNumber;
        }

        public void setPcmNumber(int[] value) {
            pcmNumber = value;
        }

        private ChipPart[] parts;

        public ChipPart[] getParts() {
            return parts;
        }

        public void setParts(ChipPart[] value) {
            parts = value;
        }

        public static class ChipPart {
            private PageDefine[] pages;

            public PageDefine[] getPages() {
                return pages;
            }

            public void setPages(PageDefine[] value) {
                pages = value;
            }
        }
    }

    public static class PartDefine {
        private int pageCount;

        public int getPageCount() {
            return pageCount;
        }

        public void setPageCount(int value) {
            pageCount = value;
        }
    }

    public static class PageDefine {
        private int length;

        public int getLength() {
            return length;
        }

        public void setLength(int value) {
            length = value;
        }

        private int loopPoint;

        public int getLoopPoint() {
            return loopPoint;
        }

        public void setLoopPoint(int value) {
            loopPoint = value;
        }

        private MmlDatum[] data;

        public MmlDatum[] getData() {
            return data;
        }

        public void setData(MmlDatum[] value) {
            data = value;
        }
    }

    public static class InstrumentDefine {
        private int length;

        public int getLength() {
            return length;
        }

        public void setLength(int value) {
            length = value;
        }

        private byte[] data;

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] value) {
            data = value;
        }
    }

    public static class PCMDefine {
        private int length;

        public int getLength() {
            return length;
        }

        public void setLength(int value) {
            length = value;
        }

        private byte[] data;

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] value) {
            data = value;
        }
    }
}
