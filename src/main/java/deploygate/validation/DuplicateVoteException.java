package deploygate.validation;

public class DuplicateVoteException extends RuntimeException {

    public DuplicateVoteException(String message) {
        super(message);
    }
}
