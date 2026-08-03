package deploygate.service;

import deploygate.dao.ApprovalRequestRepository;
import deploygate.dao.ApprovalVoteRepository;
import deploygate.dao.DeployerRepository;
import deploygate.dao.StackPolicyRepository;
import deploygate.entity.ApprovalLevel;
import deploygate.entity.ApprovalRequest;
import deploygate.entity.Deployer;
import deploygate.entity.StackPolicy;
import deploygate.entity.VoteDecision;
import deploygate.policy.ApprovalPolicy;
import deploygate.policy.ApprovalPolicyResolver;
import deploygate.policy.ApprovalResult;
import deploygate.policy.Decision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApprovalCheckService {

    private final DeployerRepository deployerRepository;
    private final StackPolicyRepository stackPolicyRepository;
    private final ApprovalPolicyResolver approvalPolicyResolver;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalVoteRepository approvalVoteRepository;
    private final ApprovalLogService approvalLogService;

    public ApprovalCheckService(DeployerRepository deployerRepository,
                                StackPolicyRepository stackPolicyRepository,
                                ApprovalPolicyResolver approvalPolicyResolver,
                                ApprovalRequestRepository approvalRequestRepository,
                                ApprovalVoteRepository approvalVoteRepository,
                                ApprovalLogService approvalLogService) {
        this.deployerRepository = deployerRepository;
        this.stackPolicyRepository = stackPolicyRepository;
        this.approvalPolicyResolver = approvalPolicyResolver;
        this.approvalRequestRepository = approvalRequestRepository;
        this.approvalVoteRepository = approvalVoteRepository;
        this.approvalLogService = approvalLogService;
    }

    public ApprovalResult check(String user, String stack, String action) {
        Deployer deployer = deployerRepository.findByName(user).orElse(null);
        if (deployer == null) {
            return ApprovalResult.denied("unknown deployer: " + user);
        }

        StackPolicy stackPolicy = stackPolicyRepository.findByStackName(stack).orElse(null);
        if (stackPolicy == null) {
            return ApprovalResult.denied("no policy configured for stack: " + stack);
        }

        if (stackPolicy.getApprovalLevel() == ApprovalLevel.NONE) {
            ApprovalResult result = evaluate(deployer, stackPolicy);
            approvalLogService.record(user, stack, action, result.decision(), "SYSTEM", null);
            return result;
        }

        return checkWithApprovalFlow(deployer, stackPolicy, action);
    }

    private ApprovalResult checkWithApprovalFlow(Deployer deployer, StackPolicy stackPolicy, String action) {
        String stack = stackPolicy.getStackName();
        ApprovalRequest request = approvalRequestRepository
                .findTopByDeployerIdAndStackNameAndActionOrderByCreatedAtDesc(deployer.getId(), stack, action)
                .orElse(null);

        if (request != null) {
            return switch (request.getStatus()) {
                case APPROVED -> ApprovalResult.allowed("approved (request #" + request.getId() + ")");
                case REJECTED -> ApprovalResult.denied(
                        "request #" + request.getId() + " was rejected; submit a new /approval/request to retry");
                case PENDING -> {
                    long approved = approvalVoteRepository.countByRequestIdAndDecision(request.getId(), VoteDecision.APPROVE);
                    long remaining = request.getRequiredApprovals() - approved;
                    yield ApprovalResult.pending(
                            "awaiting " + remaining + " more approver(s) (request #" + request.getId() + ")");
                }
            };
        }

        ApprovalResult result = evaluate(deployer, stackPolicy);
        if (result.decision() == Decision.DENIED) {
            approvalLogService.record(deployer.getName(), stack, action, result.decision(), "SYSTEM", null);
            return result;
        }
        return ApprovalResult.pending("approval required; call POST /approval/request to start the approval flow");
    }

    private ApprovalResult evaluate(Deployer deployer, StackPolicy stackPolicy) {
        return approvalPolicyResolver.resolve(stackPolicy.getApprovalLevel())
                .map((ApprovalPolicy policy) -> policy.evaluate(deployer, stackPolicy))
                .orElseGet(() -> ApprovalResult.denied(
                        "no approval policy registered for level: " + stackPolicy.getApprovalLevel()));
    }
}
