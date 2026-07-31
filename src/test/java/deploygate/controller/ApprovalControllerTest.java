package deploygate.controller;

import deploygate.dto.ApprovalActionResponse;
import deploygate.dto.ApprovalCheckRequest;
import deploygate.dto.ApprovalCheckResponse;
import deploygate.dto.ApprovalDecisionRequest;
import deploygate.dto.ApprovalHistoryEntry;
import deploygate.dto.ApprovalRequestCreateRequest;
import deploygate.dto.ApprovalRequestResponse;
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
    void checkReturnsPendingWhenApprovalRequired() {
        ApprovalCheckRequest request = new ApprovalCheckRequest("jiye", "ProdAlbStack", "DEPLOY");

        ResponseEntity<ApprovalCheckResponse> response =
                restTemplate.postForEntity("/approval/check", request, ApprovalCheckResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody().result()).isEqualTo("PENDING");
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

    @Test
    void singleApprover_fullFlow_requestApproveRecheckReturns200() {
        ApprovalRequestResponse created = restTemplate.postForEntity("/approval/request",
                new ApprovalRequestCreateRequest("jiye", "StagingApiStack", "DEPLOY_S1"),
                ApprovalRequestResponse.class).getBody();
        assertThat(created.status()).isEqualTo("PENDING");

        ResponseEntity<ApprovalActionResponse> approveResponse = restTemplate.postForEntity(
                "/approval/" + created.id() + "/approve", new ApprovalDecisionRequest("alice"), ApprovalActionResponse.class);
        assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approveResponse.getBody().status()).isEqualTo("APPROVED");

        ResponseEntity<ApprovalCheckResponse> recheck = restTemplate.postForEntity("/approval/check",
                new ApprovalCheckRequest("jiye", "StagingApiStack", "DEPLOY_S1"), ApprovalCheckResponse.class);
        assertThat(recheck.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(recheck.getBody().result()).isEqualTo("ALLOWED");
    }

    @Test
    void dualApprover_staysPendingAfterOneOfTwoApprovals() {
        ApprovalRequestResponse created = restTemplate.postForEntity("/approval/request",
                new ApprovalRequestCreateRequest("jiye", "ProdAlbStack", "DEPLOY_D1"),
                ApprovalRequestResponse.class).getBody();

        ResponseEntity<ApprovalActionResponse> approveResponse = restTemplate.postForEntity(
                "/approval/" + created.id() + "/approve", new ApprovalDecisionRequest("alice"), ApprovalActionResponse.class);
        assertThat(approveResponse.getBody().status()).isEqualTo("PENDING");

        ResponseEntity<ApprovalCheckResponse> recheck = restTemplate.postForEntity("/approval/check",
                new ApprovalCheckRequest("jiye", "ProdAlbStack", "DEPLOY_D1"), ApprovalCheckResponse.class);
        assertThat(recheck.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        ResponseEntity<ApprovalActionResponse> secondApprove = restTemplate.postForEntity(
                "/approval/" + created.id() + "/approve", new ApprovalDecisionRequest("bob"), ApprovalActionResponse.class);
        assertThat(secondApprove.getBody().status()).isEqualTo("APPROVED");

        ResponseEntity<ApprovalCheckResponse> finalCheck = restTemplate.postForEntity("/approval/check",
                new ApprovalCheckRequest("jiye", "ProdAlbStack", "DEPLOY_D1"), ApprovalCheckResponse.class);
        assertThat(finalCheck.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void reject_immediatelyDeniesRecheck() {
        ApprovalRequestResponse created = restTemplate.postForEntity("/approval/request",
                new ApprovalRequestCreateRequest("jiye", "ProdAlbStack", "DEPLOY_D2"),
                ApprovalRequestResponse.class).getBody();

        ResponseEntity<ApprovalActionResponse> rejectResponse = restTemplate.postForEntity(
                "/approval/" + created.id() + "/reject", new ApprovalDecisionRequest("alice"), ApprovalActionResponse.class);
        assertThat(rejectResponse.getBody().status()).isEqualTo("REJECTED");

        ResponseEntity<ApprovalCheckResponse> recheck = restTemplate.postForEntity("/approval/check",
                new ApprovalCheckRequest("jiye", "ProdAlbStack", "DEPLOY_D2"), ApprovalCheckResponse.class);
        assertThat(recheck.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void approve_duplicateVoteReturns409() {
        ApprovalRequestResponse created = restTemplate.postForEntity("/approval/request",
                new ApprovalRequestCreateRequest("jiye", "ProdAlbStack", "DEPLOY_D3"),
                ApprovalRequestResponse.class).getBody();

        restTemplate.postForEntity("/approval/" + created.id() + "/approve",
                new ApprovalDecisionRequest("alice"), ApprovalActionResponse.class);
        ResponseEntity<String> duplicate = restTemplate.postForEntity("/approval/" + created.id() + "/approve",
                new ApprovalDecisionRequest("alice"), String.class);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void approve_unknownRequestIdReturns404() {
        ResponseEntity<String> response = restTemplate.postForEntity("/approval/999999/approve",
                new ApprovalDecisionRequest("alice"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void approve_unauthorizedApproverReturns403() {
        ApprovalRequestResponse created = restTemplate.postForEntity("/approval/request",
                new ApprovalRequestCreateRequest("jiye", "ProdAlbStack", "DEPLOY_D4"),
                ApprovalRequestResponse.class).getBody();

        ResponseEntity<String> response = restTemplate.postForEntity("/approval/" + created.id() + "/approve",
                new ApprovalDecisionRequest("jiye"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void historyEndpointReturnsEntriesFilteredByStack() {
        restTemplate.postForEntity("/approval/check",
                new ApprovalCheckRequest("jiye", "SmallAppStack", "DEPLOY"), ApprovalCheckResponse.class);

        ResponseEntity<ApprovalHistoryEntry[]> response =
                restTemplate.getForEntity("/approval/history?stack=SmallAppStack", ApprovalHistoryEntry[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody()[0].stack()).isEqualTo("SmallAppStack");
    }
}
