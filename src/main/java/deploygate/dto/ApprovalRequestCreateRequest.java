package deploygate.dto;

import jakarta.validation.constraints.NotBlank;

public record ApprovalRequestCreateRequest(
        @NotBlank(message = "user must not be blank") String user,
        @NotBlank(message = "stack must not be blank") String stack,
        @NotBlank(message = "action must not be blank") String action
) {
}
