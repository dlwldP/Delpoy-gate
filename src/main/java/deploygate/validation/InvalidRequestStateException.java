package deploygate.validation;

public class InvalidRequestStateException extends RuntimeException {

    public InvalidRequestStateException(String message) {
        super(message);
    }
}
