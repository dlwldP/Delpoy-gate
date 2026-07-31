package deploygate.service;

import deploygate.dao.DeployerRepository;
import deploygate.dao.StackPolicyRepository;
import deploygate.entity.ApprovalLevel;
import deploygate.entity.Deployer;
import deploygate.entity.StackPolicy;
import deploygate.policy.ApprovalPolicyResolver;
import deploygate.policy.ApprovalResult;
import deploygate.policy.Decision;
import deploygate.policy.NoApprovalPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApprovalCheckServiceTest {

    private DeployerRepository deployerRepository;
    private StackPolicyRepository stackPolicyRepository;
    private ApprovalCheckService service;

    @BeforeEach
    void setUp() {
        deployerRepository = mock(DeployerRepository.class);
        stackPolicyRepository = mock(StackPolicyRepository.class);
        ApprovalPolicyResolver resolver = new ApprovalPolicyResolver(List.of(new NoApprovalPolicy()));
        service = new ApprovalCheckService(deployerRepository, stackPolicyRepository, resolver);
    }

    @Test
    void deniesWhenDeployerUnknown() {
        when(deployerRepository.findByName("ghost")).thenReturn(Optional.empty());

        ApprovalResult result = service.check("ghost", "SmallAppStack");

        assertThat(result.decision()).isEqualTo(Decision.DENIED);
        assertThat(result.reason()).contains("unknown deployer");
    }

    @Test
    void deniesWhenStackPolicyMissing() {
        when(deployerRepository.findByName("jiye"))
                .thenReturn(Optional.of(Deployer.builder().name("jiye").claims(Set.of()).build()));
        when(stackPolicyRepository.findByStackName("UnknownStack")).thenReturn(Optional.empty());

        ApprovalResult result = service.check("jiye", "UnknownStack");

        assertThat(result.decision()).isEqualTo(Decision.DENIED);
        assertThat(result.reason()).contains("no policy configured");
    }

    @Test
    void deniesWhenApprovalLevelUnsupported() {
        when(deployerRepository.findByName("jiye"))
                .thenReturn(Optional.of(Deployer.builder().name("jiye")
                        .claims(Set.of("stack:ProdAlbStack:deploy")).build()));
        when(stackPolicyRepository.findByStackName("ProdAlbStack"))
                .thenReturn(Optional.of(StackPolicy.builder()
                        .stackName("ProdAlbStack")
                        .requiredClaim("stack:ProdAlbStack:deploy")
                        .approvalLevel(ApprovalLevel.DUAL_APPROVER)
                        .build()));

        ApprovalResult result = service.check("jiye", "ProdAlbStack");

        assertThat(result.decision()).isEqualTo(Decision.DENIED);
        assertThat(result.reason()).contains("approval required");
    }

    @Test
    void allowsWhenClaimMatchesUnderNoApprovalPolicy() {
        when(deployerRepository.findByName("jiye"))
                .thenReturn(Optional.of(Deployer.builder().name("jiye")
                        .claims(Set.of("stack:SmallAppStack:deploy")).build()));
        when(stackPolicyRepository.findByStackName("SmallAppStack"))
                .thenReturn(Optional.of(StackPolicy.builder()
                        .stackName("SmallAppStack")
                        .requiredClaim("stack:SmallAppStack:deploy")
                        .approvalLevel(ApprovalLevel.NONE)
                        .build()));

        ApprovalResult result = service.check("jiye", "SmallAppStack");

        assertThat(result.decision()).isEqualTo(Decision.ALLOWED);
    }
}
