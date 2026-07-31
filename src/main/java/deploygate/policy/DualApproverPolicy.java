package deploygate.policy;

import deploygate.entity.ApprovalLevel;
import deploygate.entity.Deployer;
import deploygate.entity.StackPolicy;
import org.springframework.stereotype.Component;

@Component
public class DualApproverPolicy extends ApprovalPolicy {

    @Override
    public ApprovalLevel getSupportedLevel() {
        return ApprovalLevel.DUAL_APPROVER;
    }

    @Override
    public ApprovalResult evaluate(Deployer deployer, StackPolicy stackPolicy) {
        String requiredClaim = stackPolicy.getRequiredClaim();
        if (!deployer.getClaims().contains(requiredClaim)) {
            return ApprovalResult.denied("deployer is missing required claim: " + requiredClaim);
        }
        return ApprovalResult.pending("requires 2 distinct approvers");
    }
}
