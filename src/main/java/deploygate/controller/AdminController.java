package deploygate.controller;

import deploygate.dto.DeployerSummary;
import deploygate.dto.StackPolicySummary;
import deploygate.service.AdminQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only endpoints backing the admin screen: lets operators see who holds which
 * claims and how each stack is policed, without touching the database directly.
 * Intentionally read-only for now — no create/update/delete here.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminQueryService adminQueryService;

    public AdminController(AdminQueryService adminQueryService) {
        this.adminQueryService = adminQueryService;
    }

    @GetMapping("/deployers")
    public ResponseEntity<List<DeployerSummary>> deployers() {
        return ResponseEntity.ok(adminQueryService.listDeployers());
    }

    @GetMapping("/stack-policies")
    public ResponseEntity<List<StackPolicySummary>> stackPolicies() {
        return ResponseEntity.ok(adminQueryService.listStackPolicies());
    }
}
