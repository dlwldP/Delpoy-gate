package deploygate.service;

import deploygate.dao.ApprovalLogRepository;
import deploygate.entity.ApprovalLog;
import deploygate.policy.Decision;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ApprovalLogService {

    private final ApprovalLogRepository approvalLogRepository;

    public ApprovalLogService(ApprovalLogRepository approvalLogRepository) {
        this.approvalLogRepository = approvalLogRepository;
    }

    public void record(String deployerName, String stackName, String action, Decision result, String decidedBy, Long requestId) {
        approvalLogRepository.save(ApprovalLog.builder()
                .deployerName(deployerName)
                .stackName(stackName)
                .action(action)
                .result(result)
                .decidedBy(decidedBy)
                .decidedAt(Instant.now())
                .requestId(requestId)
                .build());
    }
}
