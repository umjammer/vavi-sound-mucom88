package mucom88.common;

public class MubException extends RuntimeException {
    public MubException() {
    }

    public MubException(String message) {
        super(message);
    }

    public MubException(String message, Exception innerException) {
        super(message, innerException);
    }

    public MubException(String message, int row, int col) {
            super(String.format(Message.get("E0300"),row,col,message));
    }
}

