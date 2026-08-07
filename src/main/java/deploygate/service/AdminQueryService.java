package deploygate.service;

import deploygate.dao.DeployerRepository;
import deploygate.dao.StackPolicyRepository;
import deploygate.dto.DeployerSummary;
import deploygate.dto.StackPolicySummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminQueryService {

    private final DeployerRepository deployerRepository;
    private final StackPolicyRepository stackPolicyRepository;

    public AdminQueryService(DeployerRepository deployerRepository, StackPolicyRepository stackPolicyRepository) {
        this.deployerRepository = deployerRepository;
        this.stackPolicyRepository = stackPolicyRepository;
    }

    public List<DeployerSummary> listDeployers() {
        return deployerRepository.findAll().stream()
                .sorted(Comparator.comparing(deployer -> deployer.getName().toLowerCase()))
                .map(deployer -> new DeployerSummary(deployer.getId(), deployer.getName(), deployer.getClaims()))
                .toList();
    }

    public List<StackPolicySummary> listStackPolicies() {
        return stackPolicyRepository.findAll().stream()
                .sorted(Comparator.comparing(policy -> policy.getStackName().toLowerCase()))
                .map(policy -> new StackPolicySummary(policy.getId(), policy.getStackName(),
                        policy.getRequiredClaim(), policy.getApprovalLevel().name()))
                .toList();
    }
}
