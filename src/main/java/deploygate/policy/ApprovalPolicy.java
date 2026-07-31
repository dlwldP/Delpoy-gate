package deploygate.policy;

import deploygate.entity.ApprovalLevel;
import deploygate.entity.Deployer;
import deploygate.entity.StackPolicy;

public abstract class ApprovalPolicy {

    public abstract ApprovalLevel getSupportedLevel();

    public abstract ApprovalResult evaluate(Deployer deployer, StackPolicy stackPolicy);
}
