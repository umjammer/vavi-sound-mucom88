package mucom88.common;

import java.io.Serializable;
import java.util.ResourceBundle;


public class MucException extends RuntimeException implements Serializable {

    private static final ResourceBundle rb = ResourceBundle.getBundle("mucom88/message");

    public MucException(String message) {
        super(message);
    }

    public MucException(String message, Exception innerException) {
        super(message, innerException);
    }

    public MucException(String message, int row, int col) {
        super(rb.getString("E0300").formatted(row, col, message));
    }
}
