package mucom88.driver;


public class OPNAData {
    public byte port;
    public byte address;
    public byte data;
    public long time;
    public Object addtionalData = null;

    public OPNAData(byte port, byte address, byte data, long time/* = 0*/, Object addtionalData/* = null*/) {
        this.port = port;
        this.address = address;
        this.data = data;
        this.time = time;
        this.addtionalData = addtionalData;
    }
}

