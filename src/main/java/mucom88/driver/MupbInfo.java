package mucom88.driver;

import musicDriverInterface.MmlDatum;


public class MupbInfo {

    private int version;

    public int getversion() {
        return version;
    }

    public void setversion(int value) {
        version = value;
    }

    private int variableLengthCount;

    public int getvariableLengthCount() {
        return variableLengthCount;
    }

    public void setvariableLengthCount(int value) {
        variableLengthCount = value;
    }

    private int useChipCount;

    public int getuseChipCount() {
        return useChipCount;
    }

    public void setuseChipCount(int value) {
        useChipCount = value;
    }

    private int usePartCount;

    public int getusePartCount() {
        return usePartCount;
    }

    public void setusePartCount(int value) {
        usePartCount = value;
    }

    private int usePageCount;

    public int getusePageCount() {
        return usePageCount;
    }

    public void setusePageCount(int value) {
        usePageCount = value;
    }

    private int useInstrumentSetCount;

    public int getuseInstrumentSetCount() {
        return useInstrumentSetCount;
    }

    public void setuseInstrumentSetCount(int value) {
        useInstrumentSetCount = value;
    }

    private int usePCMSetCount;

    public int getusePCMSetCount() {
        return usePCMSetCount;
    }

    public void setusePCMSetCount(int value) {
        usePCMSetCount = value;
    }

    private ChipDefine[] chips;

    public ChipDefine[] getchips() {
        return chips;
    }

    public void setchips(ChipDefine[] value) {
        chips = value;
    }

    private int tagDataOffset;

    public int gettagDataOffset() {
        return tagDataOffset;
    }

    public void settagDataOffset(int value) {
        tagDataOffset = value;
    }

    private int tagDataSize;

    public int gettagDataSize() {
        return tagDataSize;
    }

    public void settagDataSize(int value) {
        tagDataSize = value;
    }

    private int JCLOCK;

    public int getJCLOCK() {
        return JCLOCK;
    }

    public void setJCLOCK(int value) {
        JCLOCK = value;
    }

    private int JPLINE;

    public int getJPLINE() {
        return JPLINE;
    }

    public void setJPLINE(int value) {
        JPLINE = value;
    }

    private PartDefine[] parts;

    public PartDefine[] getparts() {
        return parts;
    }

    public void setparts(PartDefine[] value) {
        parts = value;
    }

    private PageDefine[] pages;

    public PageDefine[] getpages()
    {
        return pages;
    }
    public void setpages(PageDefine[] value)
    {
        pages = value;
    }

    private InstrumentDefine[] instruments;

    public InstrumentDefine[] getinstruments() {
        return instruments;
    }

    public void setinstruments(InstrumentDefine[] value) {
        instruments = value;
    }

    private PCMDefine[] pcms;

    public PCMDefine[] getpcms() {
        return pcms;
    }

    public void setpcms(PCMDefine[] value) {
        pcms = value;
    }

    public static class ChipDefine {
        private int indexNumber;

        public int getindexNumber() {
            return indexNumber;
        }

        public void setindexNumber(int value) {
            indexNumber = value;
        }

        private int identifyNumber;

        public int getidentifyNumber() {
            return identifyNumber;
        }

        public void setidentifyNumber(int value) {
            identifyNumber = value;
        }

        private int masterClock;

        public int getmasterClock() {
            return masterClock;
        }

        public void setmasterClock(int value) {
            masterClock = value;
        }

        private int option;

        public int getoption() {
            return option;
        }

        public void setoption(int value) {
            option = value;
        }

        private int heartBeat;

        public int getheartBeat() {
            return heartBeat;
        }

        public void setheartBeat(int value) {
            heartBeat = value;
        }

        private int heartBeat2;

        public int getheartBeat2() {
            return heartBeat2;
        }

        public void setheartBeat2(int value) {
            heartBeat2 = value;
        }

        private int[] instrumentNumber;

        public int[] getinstrumentNumber() {
            return instrumentNumber;
        }

        public void setinstrumentNumber(int[] value) {
            instrumentNumber = value;
        }

        private int[] pcmNumber;

        public int[] getpcmNumber() {
            return pcmNumber;
        }

        public void setpcmNumber(int[] value) {
            pcmNumber = value;
        }

        private ChipPart[] parts;

        public ChipPart[] getparts() {
            return parts;
        }

        public void setparts(ChipPart[] value) {
            parts = value;
        }

        public static class ChipPart {
            private PageDefine[] pages;

            public PageDefine[] getpages() {
                return pages;
            }

            public void setpages(PageDefine[] value) {
                pages = value;
            }
        }
    }

    public static class PartDefine {
        private int pageCount;

        public int getpageCount() {
            return pageCount;
        }

        public void setpageCount(int value) {
            pageCount = value;
        }
    }

    public static class PageDefine {
        private int length;

        public int getlength() {
            return length;
        }

        public void setlength(int value) {
            length = value;
        }

        private int loopPoint;

        public int getloopPoint() {
            return loopPoint;
        }

        public void setloopPoint(int value) {
            loopPoint = value;
        }

        private MmlDatum[] data;

        public MmlDatum[] getdata() {
            return data;
        }

        public void setdata(MmlDatum[] value) {
            data = value;
        }
    }

    public static class InstrumentDefine {
        private int length;

        public int getlength() {
            return length;
        }

        public void setlength(int value) {
            length = value;
        }

        private byte[] data;

        public byte[] getdata() {
            return data;
        }

        public void setdata(byte[] value) {
            data = value;
        }
    }

    public static class PCMDefine {
        private int length;

        public int getlength() {
            return length;
        }

        public void setlength(int value) {
            length = value;
        }

        private byte[] data;

        public byte[] getdata() {
            return data;
        }

        public void setdata(byte[] value) {
            data = value;
        }
    }
}
