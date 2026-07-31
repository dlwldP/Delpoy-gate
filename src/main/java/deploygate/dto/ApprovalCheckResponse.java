package deploygate.dto;

public record ApprovalCheckResponse(
        String result,
        String stack,
        String reason
) {
}
