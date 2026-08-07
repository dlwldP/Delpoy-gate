package deploygate.controller;

import deploygate.dto.DeployerSummary;
import deploygate.dto.StackPolicySummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void listsSeededDeployersWithClaims() {
        ResponseEntity<DeployerSummary[]> response = restTemplate.getForEntity("/admin/deployers", DeployerSummary[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Arrays.stream(response.getBody()).map(DeployerSummary::name)).contains("jiye", "alice", "bob");
        DeployerSummary jiye = Arrays.stream(response.getBody())
                .filter(deployer -> deployer.name().equals("jiye"))
                .findFirst().orElseThrow();
        assertThat(jiye.claims()).contains("stack:SmallAppStack:deploy");
    }

    @Test
    void listsSeededStackPolicies() {
        ResponseEntity<StackPolicySummary[]> response = restTemplate.getForEntity("/admin/stack-policies", StackPolicySummary[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Arrays.stream(response.getBody()).map(StackPolicySummary::stackName))
                .contains("SmallAppStack", "StagingApiStack", "ProdAlbStack");
        StackPolicySummary prodAlb = Arrays.stream(response.getBody())
                .filter(policy -> policy.stackName().equals("ProdAlbStack"))
                .findFirst().orElseThrow();
        assertThat(prodAlb.approvalLevel()).isEqualTo("DUAL_APPROVER");
    }
}
