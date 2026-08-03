package deploygate.config;

import deploygate.dao.DeployerRepository;
import deploygate.dao.StackPolicyRepository;
import deploygate.entity.ApprovalLevel;
import deploygate.entity.Deployer;
import deploygate.entity.StackPolicy;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final DeployerRepository deployerRepository;
    private final StackPolicyRepository stackPolicyRepository;

    public DemoDataSeeder(DeployerRepository deployerRepository, StackPolicyRepository stackPolicyRepository) {
        this.deployerRepository = deployerRepository;
        this.stackPolicyRepository = stackPolicyRepository;
    }

    @Override
    public void run(String... args) {
        if (deployerRepository.count() > 0) {
            return;
        }

        deployerRepository.save(Deployer.builder()
                .name("jiye")
                .claims(Set.of(
                        "stack:SmallAppStack:deploy",
                        "stack:SmallAppStack:destroy",
                        "stack:ProdAlbStack:deploy",
                        "stack:StagingApiStack:deploy"))
                .build());

        deployerRepository.save(Deployer.builder()
                .name("alice")
                .claims(Set.of(
                        "stack:ProdAlbStack:approve",
                        "stack:StagingApiStack:approve"))
                .build());

        deployerRepository.save(Deployer.builder()
                .name("bob")
                .claims(Set.of("stack:ProdAlbStack:approve"))
                .build());

        stackPolicyRepository.save(StackPolicy.builder()
                .stackName("SmallAppStack")
                .requiredClaim("stack:SmallAppStack:deploy")
                .approvalLevel(ApprovalLevel.NONE)
                .build());

        stackPolicyRepository.save(StackPolicy.builder()
                .stackName("StagingApiStack")
                .requiredClaim("stack:StagingApiStack:deploy")
                .approvalLevel(ApprovalLevel.SINGLE_APPROVER)
                .build());

        stackPolicyRepository.save(StackPolicy.builder()
                .stackName("ProdAlbStack")
                .requiredClaim("stack:ProdAlbStack:deploy")
                .approvalLevel(ApprovalLevel.DUAL_APPROVER)
                .build());
    }
}
