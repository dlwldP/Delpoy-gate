package deploygate.controller;

import deploygate.dto.ApprovalCheckRequest;
import deploygate.dto.ApprovalCheckResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApprovalControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void allowsSmallAppStackDeployForJiye() {
        ApprovalCheckRequest request = new ApprovalCheckRequest("jiye", "SmallAppStack", "DEPLOY");

        ResponseEntity<ApprovalCheckResponse> response =
                restTemplate.postForEntity("/approval/check", request, ApprovalCheckResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().result()).isEqualTo("ALLOWED");
    }

    @Test
    void blocksProdAlbStackDeployForJiye() {
        ApprovalCheckRequest request = new ApprovalCheckRequest("jiye", "ProdAlbStack", "DEPLOY");

        ResponseEntity<ApprovalCheckResponse> response =
                restTemplate.postForEntity("/approval/check", request, ApprovalCheckResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().result()).isEqualTo("DENIED");
    }

    @Test
    void blocksUnknownDeployer() {
        ApprovalCheckRequest request = new ApprovalCheckRequest("ghost", "SmallAppStack", "DEPLOY");

        ResponseEntity<ApprovalCheckResponse> response =
                restTemplate.postForEntity("/approval/check", request, ApprovalCheckResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void blocksUnknownStack() {
        ApprovalCheckRequest request = new ApprovalCheckRequest("jiye", "UnknownStack", "DEPLOY");

        ResponseEntity<ApprovalCheckResponse> response =
                restTemplate.postForEntity("/approval/check", request, ApprovalCheckResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
