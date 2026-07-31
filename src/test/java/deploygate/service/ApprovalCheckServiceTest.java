package deploygate.service;

import deploygate.dao.ApprovalRequestRepository;
import deploygate.dao.ApprovalVoteRepository;
import deploygate.dao.DeployerRepository;
import deploygate.dao.StackPolicyRepository;
import deploygate.entity.ApprovalLevel;
import deploygate.entity.ApprovalRequest;
import deploygate.entity.ApprovalRequestStatus;
import deploygate.entity.Deployer;
import deploygate.entity.StackPolicy;
import deploygate.entity.VoteDecision;
import deploygate.policy.ApprovalPolicyResolver;
import deploygate.policy.ApprovalResult;
import deploygate.policy.Decision;
import deploygate.policy.DualApproverPolicy;
import deploygate.policy.NoApprovalPolicy;
import deploygate.policy.SingleApproverPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApprovalCheckServiceTest {

    private DeployerRepository deployerRepository;
    private StackPolicyRepository stackPolicyRepository;
    private ApprovalRequestRepository approvalRequestRepository;
    private ApprovalVoteRepository approvalVoteRepository;
    private ApprovalCheckService service;

    @BeforeEach
    void setUp() {
        deployerRepository = mock(DeployerRepository.class);
        stackPolicyRepository = mock(StackPolicyRepository.class);
        approvalRequestRepository = mock(ApprovalRequestRepository.class);
        approvalVoteRepository = mock(ApprovalVoteRepository.class);
        ApprovalPolicyResolver resolver = new ApprovalPolicyResolver(
                List.of(new NoApprovalPolicy(), new SingleApproverPolicy(), new DualApproverPolicy()));
        ApprovalLogService approvalLogService = mock(ApprovalLogService.class);
        service = new ApprovalCheckService(deployerRepository, stackPolicyRepository, resolver,
                approvalRequestRepository, approvalVoteRepository, approvalLogService);
    }

    @Test
    void deniesWhenDeployerUnknown() {
        when(deployerRepository.findByName("ghost")).thenReturn(Optional.empty());

        ApprovalResult result = service.check("ghost", "SmallAppStack", "DEPLOY");

        assertThat(result.decision()).isEqualTo(Decision.DENIED);
        assertThat(result.reason()).contains("unknown deployer");
    }

    @Test
    void deniesWhenStackPolicyMissing() {
        when(deployerRepository.findByName("jiye"))
                .thenReturn(Optional.of(Deployer.builder().name("jiye").claims(Set.of()).build()));
        when(stackPolicyRepository.findByStackName("UnknownStack")).thenReturn(Optional.empty());

        ApprovalResult result = service.check("jiye", "UnknownStack", "DEPLOY");

        assertThat(result.decision()).isEqualTo(Decision.DENIED);
        assertThat(result.reason()).contains("no policy configured");
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

        ApprovalResult result = service.check("jiye", "SmallAppStack", "DEPLOY");

        assertThat(result.decision()).isEqualTo(Decision.ALLOWED);
    }

    @Test
    void deniesWhenClaimMissingUnderApprovalRequiredStack() {
        when(deployerRepository.findByName("jiye"))
                .thenReturn(Optional.of(Deployer.builder().name("jiye").claims(Set.of()).build()));
        when(stackPolicyRepository.findByStackName("ProdAlbStack"))
                .thenReturn(Optional.of(dualStackPolicy()));
        when(approvalRequestRepository.findTopByDeployerIdAndStackNameAndActionOrderByCreatedAtDesc(null, "ProdAlbStack", "DEPLOY"))
                .thenReturn(Optional.empty());

        ApprovalResult result = service.check("jiye", "ProdAlbStack", "DEPLOY");

        assertThat(result.decision()).isEqualTo(Decision.DENIED);
    }

    @Test
    void pendingWhenNoRequestExistsYetForApprovalRequiredStack() {
        Deployer jiye = Deployer.builder().id(1L).name("jiye")
                .claims(Set.of("stack:ProdAlbStack:deploy")).build();
        when(deployerRepository.findByName("jiye")).thenReturn(Optional.of(jiye));
        when(stackPolicyRepository.findByStackName("ProdAlbStack")).thenReturn(Optional.of(dualStackPolicy()));
        when(approvalRequestRepository.findTopByDeployerIdAndStackNameAndActionOrderByCreatedAtDesc(1L, "ProdAlbStack", "DEPLOY"))
                .thenReturn(Optional.empty());

        ApprovalResult result = service.check("jiye", "ProdAlbStack", "DEPLOY");

        assertThat(result.decision()).isEqualTo(Decision.PENDING);
        assertThat(result.reason()).contains("/approval/request");
    }

    @Test
    void pendingWithRemainingCountWhenRequestIsPending() {
        Deployer jiye = Deployer.builder().id(1L).name("jiye")
                .claims(Set.of("stack:ProdAlbStack:deploy")).build();
        when(deployerRepository.findByName("jiye")).thenReturn(Optional.of(jiye));
        when(stackPolicyRepository.findByStackName("ProdAlbStack")).thenReturn(Optional.of(dualStackPolicy()));
        ApprovalRequest request = ApprovalRequest.builder()
                .id(10L).deployerId(1L).stackName("ProdAlbStack").action("DEPLOY")
                .status(ApprovalRequestStatus.PENDING).requiredApprovals(2).createdAt(Instant.now()).build();
        when(approvalRequestRepository.findTopByDeployerIdAndStackNameAndActionOrderByCreatedAtDesc(1L, "ProdAlbStack", "DEPLOY"))
                .thenReturn(Optional.of(request));
        when(approvalVoteRepository.countByRequestIdAndDecision(10L, VoteDecision.APPROVE)).thenReturn(1L);

        ApprovalResult result = service.check("jiye", "ProdAlbStack", "DEPLOY");

        assertThat(result.decision()).isEqualTo(Decision.PENDING);
        assertThat(result.reason()).contains("1 more approver");
    }

    @Test
    void allowsWhenRequestIsApproved() {
        Deployer jiye = Deployer.builder().id(1L).name("jiye")
                .claims(Set.of("stack:ProdAlbStack:deploy")).build();
        when(deployerRepository.findByName("jiye")).thenReturn(Optional.of(jiye));
        when(stackPolicyRepository.findByStackName("ProdAlbStack")).thenReturn(Optional.of(dualStackPolicy()));
        ApprovalRequest request = ApprovalRequest.builder()
                .id(10L).deployerId(1L).stackName("ProdAlbStack").action("DEPLOY")
                .status(ApprovalRequestStatus.APPROVED).requiredApprovals(2).createdAt(Instant.now()).build();
        when(approvalRequestRepository.findTopByDeployerIdAndStackNameAndActionOrderByCreatedAtDesc(1L, "ProdAlbStack", "DEPLOY"))
                .thenReturn(Optional.of(request));

        ApprovalResult result = service.check("jiye", "ProdAlbStack", "DEPLOY");

        assertThat(result.decision()).isEqualTo(Decision.ALLOWED);
    }

    @Test
    void deniesWithRetryHintWhenRequestIsRejected() {
        Deployer jiye = Deployer.builder().id(1L).name("jiye")
                .claims(Set.of("stack:ProdAlbStack:deploy")).build();
        when(deployerRepository.findByName("jiye")).thenReturn(Optional.of(jiye));
        when(stackPolicyRepository.findByStackName("ProdAlbStack")).thenReturn(Optional.of(dualStackPolicy()));
        ApprovalRequest request = ApprovalRequest.builder()
                .id(10L).deployerId(1L).stackName("ProdAlbStack").action("DEPLOY")
                .status(ApprovalRequestStatus.REJECTED).requiredApprovals(2).createdAt(Instant.now()).build();
        when(approvalRequestRepository.findTopByDeployerIdAndStackNameAndActionOrderByCreatedAtDesc(1L, "ProdAlbStack", "DEPLOY"))
                .thenReturn(Optional.of(request));

        ApprovalResult result = service.check("jiye", "ProdAlbStack", "DEPLOY");

        assertThat(result.decision()).isEqualTo(Decision.DENIED);
        assertThat(result.reason()).contains("/approval/request");
    }

    private StackPolicy dualStackPolicy() {
        return StackPolicy.builder()
                .stackName("ProdAlbStack")
                .requiredClaim("stack:ProdAlbStack:deploy")
                .approvalLevel(ApprovalLevel.DUAL_APPROVER)
                .build();
    }
}
