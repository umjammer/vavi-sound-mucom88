package mucom88.common;

import java.io.Serializable;


public class MucException extends RuntimeException implements Serializable {
    public MucException() {
    }

    public MucException(String message) {
        super(message);
    }

    public MucException(String message, Exception innerException) {
        super(message, innerException);
    }

    public MucException(String message, int row, int col) {
        super(String.format(Message.get("E0300"), row, col, message));
    }
}
