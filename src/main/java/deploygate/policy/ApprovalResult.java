package deploygate.policy;

public record ApprovalResult(Decision decision, String reason) {

    public static ApprovalResult allowed(String reason) {
        return new ApprovalResult(Decision.ALLOWED, reason);
    }

    public static ApprovalResult denied(String reason) {
        return new ApprovalResult(Decision.DENIED, reason);
    }
}
