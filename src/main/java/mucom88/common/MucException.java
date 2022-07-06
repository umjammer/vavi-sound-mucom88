package mucom88.common;

import java.io.Serializable;
import java.util.ResourceBundle;


public class MucException extends RuntimeException implements Serializable {

    static final ResourceBundle rb = ResourceBundle.getBundle("lang/message");

    public MucException(String message) {
        super(message);
    }

    public MucException(String message, Exception innerException) {
        super(message, innerException);
    }

    public MucException(String message, int row, int col) {
        super(String.format(rb.getString("E0300"), row, col, message));
    }
}
