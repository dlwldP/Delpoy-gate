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
        deployerRepository.save(Deployer.builder()
                .name("jiye")
                .claims(Set.of("stack:SmallAppStack:deploy", "stack:SmallAppStack:destroy"))
                .build());

        stackPolicyRepository.save(StackPolicy.builder()
                .stackName("SmallAppStack")
                .requiredClaim("stack:SmallAppStack:deploy")
                .approvalLevel(ApprovalLevel.NONE)
                .build());

        stackPolicyRepository.save(StackPolicy.builder()
                .stackName("ProdAlbStack")
                .requiredClaim("stack:ProdAlbStack:deploy")
                .approvalLevel(ApprovalLevel.DUAL_APPROVER)
                .build());
    }
}
