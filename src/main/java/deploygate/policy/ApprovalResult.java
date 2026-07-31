package deploygate.policy;

public record ApprovalResult(Decision decision, String reason) {

    public static ApprovalResult allowed(String reason) {
        return new ApprovalResult(Decision.ALLOWED, reason);
    }

    public static ApprovalResult denied(String reason) {
        return new ApprovalResult(Decision.DENIED, reason);
    }

    public static ApprovalResult pending(String reason) {
        return new ApprovalResult(Decision.PENDING, reason);
    }
}
