package deploygate.dto;

public record ApprovalRequestResponse(
        Long id,
        String status,
        int requiredApprovals,
        long currentApprovals,
        String reason
) {
}
