package deploygate.policy;

import deploygate.entity.ApprovalLevel;
import deploygate.entity.Deployer;
import deploygate.entity.StackPolicy;
import org.springframework.stereotype.Component;

@Component
public class SingleApproverPolicy extends ApprovalPolicy {

    @Override
    public ApprovalLevel getSupportedLevel() {
        return ApprovalLevel.SINGLE_APPROVER;
    }

    @Override
    public ApprovalResult evaluate(Deployer deployer, StackPolicy stackPolicy) {
        String requiredClaim = stackPolicy.getRequiredClaim();
        if (!deployer.getClaims().contains(requiredClaim)) {
            return ApprovalResult.denied("deployer is missing required claim: " + requiredClaim);
        }
        return ApprovalResult.pending("requires 1 distinct approver");
    }
}
