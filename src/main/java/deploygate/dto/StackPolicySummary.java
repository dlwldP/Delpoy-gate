package deploygate.dto;

public record StackPolicySummary(
        Long id,
        String stackName,
        String requiredClaim,
        String approvalLevel
) {
}
