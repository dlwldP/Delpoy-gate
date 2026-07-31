package deploygate.validation;

public class ApproverNotAuthorizedException extends RuntimeException {

    public ApproverNotAuthorizedException(String message) {
        super(message);
    }
}
