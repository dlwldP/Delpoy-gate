package deploygate.controller;

import deploygate.dto.ApprovalCheckRequest;
import deploygate.dto.ApprovalCheckResponse;
import deploygate.policy.ApprovalResult;
import deploygate.policy.Decision;
import deploygate.service.ApprovalCheckService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/approval")
public class ApprovalController {

    private final ApprovalCheckService approvalCheckService;

    public ApprovalController(ApprovalCheckService approvalCheckService) {
        this.approvalCheckService = approvalCheckService;
    }

    @PostMapping("/check")
    public ResponseEntity<ApprovalCheckResponse> check(@Valid @RequestBody ApprovalCheckRequest request) {
        ApprovalResult result = approvalCheckService.check(request.user(), request.stack());
        HttpStatus status = result.decision() == Decision.ALLOWED ? HttpStatus.OK : HttpStatus.FORBIDDEN;
        ApprovalCheckResponse body = new ApprovalCheckResponse(result.decision().name(), request.stack(), result.reason());
        return ResponseEntity.status(status).body(body);
    }
}
