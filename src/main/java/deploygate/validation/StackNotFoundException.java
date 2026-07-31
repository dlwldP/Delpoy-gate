package deploygate.validation;

public class StackNotFoundException extends RuntimeException {

    public StackNotFoundException(String message) {
        super(message);
    }
}
