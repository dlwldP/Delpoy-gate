package deploygate.validation;

public class DeployerNotFoundException extends RuntimeException {

    public DeployerNotFoundException(String message) {
        super(message);
    }
}
