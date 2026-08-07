package deploygate.dto;

public record ApprovalActionResponse(
        Long id,
        String status,
        String reason
) {
}
