package deploygate.validation;

public class ApprovalRequestNotFoundException extends RuntimeException {

    public ApprovalRequestNotFoundException(String message) {
        super(message);
    }
}
