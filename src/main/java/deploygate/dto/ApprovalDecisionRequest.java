package deploygate.dto;

import jakarta.validation.constraints.NotBlank;

public record ApprovalDecisionRequest(
        @NotBlank(message = "approver must not be blank") String approver
) {
}
