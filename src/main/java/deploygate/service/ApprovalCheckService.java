package deploygate.service;

import deploygate.dao.DeployerRepository;
import deploygate.dao.StackPolicyRepository;
import deploygate.entity.Deployer;
import deploygate.entity.StackPolicy;
import deploygate.policy.ApprovalPolicy;
import deploygate.policy.ApprovalPolicyResolver;
import deploygate.policy.ApprovalResult;
import org.springframework.stereotype.Service;

@Service
public class ApprovalCheckService {

    private final DeployerRepository deployerRepository;
    private final StackPolicyRepository stackPolicyRepository;
    private final ApprovalPolicyResolver approvalPolicyResolver;

    public ApprovalCheckService(DeployerRepository deployerRepository,
                                StackPolicyRepository stackPolicyRepository,
                                ApprovalPolicyResolver approvalPolicyResolver) {
        this.deployerRepository = deployerRepository;
        this.stackPolicyRepository = stackPolicyRepository;
        this.approvalPolicyResolver = approvalPolicyResolver;
    }

    public ApprovalResult check(String user, String stack) {
        Deployer deployer = deployerRepository.findByName(user).orElse(null);
        if (deployer == null) {
            return ApprovalResult.denied("unknown deployer: " + user);
        }

        StackPolicy stackPolicy = stackPolicyRepository.findByStackName(stack).orElse(null);
        if (stackPolicy == null) {
            return ApprovalResult.denied("no policy configured for stack: " + stack);
        }

        return approvalPolicyResolver.resolve(stackPolicy.getApprovalLevel())
                .map((ApprovalPolicy policy) -> policy.evaluate(deployer, stackPolicy))
                .orElseGet(() -> ApprovalResult.denied(
                        "approval required for stack '" + stack + "' (" + stackPolicy.getApprovalLevel()
                                + "), approval workflow is not yet implemented"));
    }
}
