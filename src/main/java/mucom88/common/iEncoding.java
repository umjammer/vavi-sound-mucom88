package mucom88.common;

public interface iEncoding {
    String getStringFromSjisArray(byte[] sjisArray);

    String getStringFromSjisArray(byte[] sjisArray, int index, int count);

    byte[] GetSjisArrayFromString(String utfString);

    String GetStringFromUtfArray(byte[] utfArray);

    byte[] GetUtfArrayFromString(String utfString);
}
