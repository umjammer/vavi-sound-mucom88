package mucom88.common;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import dotnet4j.util.compat.TriConsumer;
import musicDriverInterface.ChipAction;
import musicDriverInterface.ChipDatum;


public class MucomChipAction implements ChipAction {

    private Consumer<ChipDatum> write;
    private TriConsumer<byte[], Integer, Integer> writePCMData;
    private BiConsumer<Long, Integer> waitSend;

    public MucomChipAction(Consumer<ChipDatum> write, TriConsumer<byte[], Integer, Integer> writePCMData, BiConsumer<Long, Integer> waitSend) {
        this.write = write;
        this.writePCMData = writePCMData;
        this.waitSend = waitSend;
    }

    @Override public String getChipName() {
        throw new UnsupportedOperationException();
    }

    @Override public void waitSend(long t1, int t2) {
//        throw new UnsupportedOperationException();
    }

    @Override public void writePCMData(byte[] data, int startAddress, int endAddress) {
        writePCMData.accept(data, startAddress, endAddress);
    }

    @Override public void writeRegister(ChipDatum cd) {
        write.accept(cd);
    }
}

