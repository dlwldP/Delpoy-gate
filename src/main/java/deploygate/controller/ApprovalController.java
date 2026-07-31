package deploygate.controller;

import deploygate.dto.ApprovalActionResponse;
import deploygate.dto.ApprovalCheckRequest;
import deploygate.dto.ApprovalCheckResponse;
import deploygate.dto.ApprovalDecisionRequest;
import deploygate.dto.ApprovalHistoryEntry;
import deploygate.dto.ApprovalRequestCreateRequest;
import deploygate.dto.ApprovalRequestResponse;
import deploygate.policy.ApprovalResult;
import deploygate.policy.Decision;
import deploygate.service.ApprovalCheckService;
import deploygate.service.ApprovalRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/approval")
public class ApprovalController {

    private final ApprovalCheckService approvalCheckService;
    private final ApprovalRequestService approvalRequestService;

    public ApprovalController(ApprovalCheckService approvalCheckService, ApprovalRequestService approvalRequestService) {
        this.approvalCheckService = approvalCheckService;
        this.approvalRequestService = approvalRequestService;
    }

    @PostMapping("/check")
    public ResponseEntity<ApprovalCheckResponse> check(@Valid @RequestBody ApprovalCheckRequest request) {
        ApprovalResult result = approvalCheckService.check(request.user(), request.stack(), request.action());
        HttpStatus status = switch (result.decision()) {
            case ALLOWED -> HttpStatus.OK;
            case PENDING -> HttpStatus.ACCEPTED;
            case DENIED -> HttpStatus.FORBIDDEN;
        };
        ApprovalCheckResponse body = new ApprovalCheckResponse(result.decision().name(), request.stack(), result.reason());
        return ResponseEntity.status(status).body(body);
    }

    @PostMapping("/request")
    public ResponseEntity<ApprovalRequestResponse> request(@Valid @RequestBody ApprovalRequestCreateRequest request) {
        ApprovalRequestResponse response = approvalRequestService.createRequest(request.user(), request.stack(), request.action());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApprovalActionResponse> approve(@PathVariable Long id, @Valid @RequestBody ApprovalDecisionRequest request) {
        return ResponseEntity.ok(approvalRequestService.approve(id, request.approver()));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApprovalActionResponse> reject(@PathVariable Long id, @Valid @RequestBody ApprovalDecisionRequest request) {
        return ResponseEntity.ok(approvalRequestService.reject(id, request.approver()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ApprovalHistoryEntry>> history(
            @RequestParam(required = false) String stack,
            @RequestParam(required = false) String user,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(approvalRequestService.history(Optional.ofNullable(stack), Optional.ofNullable(user), limit));
    }
}
