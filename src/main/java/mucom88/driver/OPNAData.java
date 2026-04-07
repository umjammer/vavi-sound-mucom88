package mucom88.driver;


public class OPNAData {
    public final byte port;
    public final byte address;
    public final byte data;
    public final long time;
    public Object addtionalData = null;

    public OPNAData(byte port, byte address, byte data, long time /* = 0 */, Object addtionalData /* = null */) {
        this.port = port;
        this.address = address;
        this.data = data;
        this.time = time;
        this.addtionalData = addtionalData;
    }
}

