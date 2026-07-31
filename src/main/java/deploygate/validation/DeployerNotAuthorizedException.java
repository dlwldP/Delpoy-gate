package deploygate.validation;

public class DeployerNotAuthorizedException extends RuntimeException {

    public DeployerNotAuthorizedException(String message) {
        super(message);
    }
}
