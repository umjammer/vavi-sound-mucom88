package mucom88.driver;


class OPNAData {
    private final byte port;
    private final byte address;
    private final byte data;
    private final long time;
    private Object addtionalData = null;

    public OPNAData(byte port, byte address, byte data, long time /* = 0 */, Object addtionalData /* = null */) {
        this.port = port;
        this.address = address;
        this.data = data;
        this.time = time;
        this.addtionalData = addtionalData;
    }
}

