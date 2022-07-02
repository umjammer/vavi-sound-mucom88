package mucom88.common;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import dotnet4j.TriConsumer;
import musicDriverInterface.ChipAction;
import musicDriverInterface.ChipDatum;


public class MucomChipAction implements ChipAction {

    private Consumer<ChipDatum> _Write;
    private TriConsumer<byte[], Integer, Integer> _WritePCMData;
    private BiConsumer<Long, Integer> _WaitSend;

    public MucomChipAction(Consumer<ChipDatum> Write, TriConsumer<byte[], Integer, Integer> WritePCMData, BiConsumer<Long, Integer> WaitSend) {
        _Write = Write;
        _WritePCMData = WritePCMData;
        _WaitSend = WaitSend;
    }

    @Override public String getChipName() {
        throw new UnsupportedOperationException();
    }

    @Override public void waitSend(long t1, int t2) {
        //throw new NotImplementedException();
    }

    public @Override void writePCMData(byte[] data, int startAddress, int endAddress) {
        _WritePCMData.accept(data, startAddress, endAddress);
    }

    public @Override void writeRegister(ChipDatum cd) {
        _Write.accept(cd);
    }
}

