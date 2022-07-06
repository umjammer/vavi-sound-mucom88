package mucom88.common;

import java.util.ResourceBundle;


public class MubException extends RuntimeException {

    static final ResourceBundle rb = ResourceBundle.getBundle("lang/message");

    public MubException(String message) {
        super(message);
    }

    public MubException(String message, Exception innerException) {
        super(message, innerException);
    }

    public MubException(String message, int row, int col) {
        super(String.format(rb.getString("E0300"), row, col, message));
    }
}

