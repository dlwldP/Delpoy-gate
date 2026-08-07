package deploygate.controller;

import deploygate.config.DemoDataSeeder;
import deploygate.dto.ApprovalActionResponse;
import deploygate.dto.ApprovalCheckRequest;
import deploygate.dto.ApprovalCheckResponse;
import deploygate.dto.ApprovalHistoryEntry;
import deploygate.dto.ApprovalRequestCreateRequest;
import deploygate.dto.ApprovalRequestResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApprovalControllerTest {

    private static final String CI_TOKEN = "test-ci-service-token";

    @Autowired
    private TestRestTemplate restTemplate;

    private static HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private <T> ResponseEntity<T> post(String path, String token, Object body, Class<T> responseType) {
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers(token)), responseType);
    }

    private <T> ResponseEntity<T> postNoBody(String path, String token, Class<T> responseType) {
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(headers(token)), responseType);
    }

    private <T> ResponseEntity<T> get(String path, String token, Class<T> responseType) {
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers(token)), responseType);
    }

    /** Creates a fresh pending request as jiye via the CI token. */
    private ApprovalRequestResponse createRequest(String stack, String action) {
        return post("/approval/request", CI_TOKEN,
                new ApprovalRequestCreateRequest("jiye", stack, action), ApprovalRequestResponse.class).getBody();
    }

    // --- authentication / authorization ---------------------------------------

    @Test
    void rejectsRequestWithoutToken() {
        ResponseEntity<String> response = restTemplate.postForEntity("/approval/check",
                new ApprovalCheckRequest("jiye", "SmallAppStack", "DEPLOY"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsUnknownToken() {
        ResponseEntity<String> response = post("/approval/check", "dgt_not_a_real_token",
                new ApprovalCheckRequest("jiye", "SmallAppStack", "DEPLOY"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void ciServiceTokenCannotCastApprovalVote() {
        ApprovalRequestResponse created = createRequest("ProdAlbStack", "DEPLOY_CI_VOTE");

        ResponseEntity<String> response = postNoBody("/approval/" + created.id() + "/approve", CI_TOKEN, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void personalTokenCannotActOnBehalfOfAnotherDeployer() {
        ResponseEntity<String> response = post("/approval/check", DemoDataSeeder.ALICE_TOKEN,
                new ApprovalCheckRequest("jiye", "SmallAppStack", "DEPLOY"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deployerCannotApproveTheirOwnRequest() {
        // jiye holds ProdAlbStack:deploy but must not be able to wave through their own deploy.
        ApprovalRequestResponse created = createRequest("ProdAlbStack", "DEPLOY_SELF");

        ResponseEntity<String> response =
                postNoBody("/approval/" + created.id() + "/approve", DemoDataSeeder.JIYE_TOKEN, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void historyRequiresAdminReadClaim() {
        // bob authenticates fine but holds no admin:read claim.
        ResponseEntity<String> response = get("/approval/history", DemoDataSeeder.BOB_TOKEN, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // --- decision flow ---------------------------------------------------------

    @Test
    void allowsSmallAppStackDeployForJiye() {
        ResponseEntity<ApprovalCheckResponse> response = post("/approval/check", CI_TOKEN,
                new ApprovalCheckRequest("jiye", "SmallAppStack", "DEPLOY"), ApprovalCheckResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().result()).isEqualTo("ALLOWED");
    }

    @Test
    void checkReturnsPendingWhenApprovalRequired() {
        ResponseEntity<ApprovalCheckResponse> response = post("/approval/check", CI_TOKEN,
                new ApprovalCheckRequest("jiye", "ProdAlbStack", "DEPLOY"), ApprovalCheckResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody().result()).isEqualTo("PENDING");
    }

    @Test
    void blocksUnknownDeployer() {
        ResponseEntity<ApprovalCheckResponse> response = post("/approval/check", CI_TOKEN,
                new ApprovalCheckRequest("ghost", "SmallAppStack", "DEPLOY"), ApprovalCheckResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void blocksUnknownStack() {
        ResponseEntity<ApprovalCheckResponse> response = post("/approval/check", CI_TOKEN,
                new ApprovalCheckRequest("jiye", "UnknownStack", "DEPLOY"), ApprovalCheckResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void singleApprover_fullFlow_requestApproveRecheckReturns200() {
        ApprovalRequestResponse created = createRequest("StagingApiStack", "DEPLOY_S1");
        assertThat(created.status()).isEqualTo("PENDING");

        ResponseEntity<ApprovalActionResponse> approveResponse =
                postNoBody("/approval/" + created.id() + "/approve", DemoDataSeeder.ALICE_TOKEN, ApprovalActionResponse.class);
        assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approveResponse.getBody().status()).isEqualTo("APPROVED");

        ResponseEntity<ApprovalCheckResponse> recheck = post("/approval/check", CI_TOKEN,
                new ApprovalCheckRequest("jiye", "StagingApiStack", "DEPLOY_S1"), ApprovalCheckResponse.class);
        assertThat(recheck.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(recheck.getBody().result()).isEqualTo("ALLOWED");
    }

    @Test
    void dualApprover_staysPendingAfterOneOfTwoApprovals() {
        ApprovalRequestResponse created = createRequest("ProdAlbStack", "DEPLOY_D1");

        ResponseEntity<ApprovalActionResponse> firstApproval =
                postNoBody("/approval/" + created.id() + "/approve", DemoDataSeeder.ALICE_TOKEN, ApprovalActionResponse.class);
        assertThat(firstApproval.getBody().status()).isEqualTo("PENDING");

        ResponseEntity<ApprovalCheckResponse> recheck = post("/approval/check", CI_TOKEN,
                new ApprovalCheckRequest("jiye", "ProdAlbStack", "DEPLOY_D1"), ApprovalCheckResponse.class);
        assertThat(recheck.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        ResponseEntity<ApprovalActionResponse> secondApproval =
                postNoBody("/approval/" + created.id() + "/approve", DemoDataSeeder.BOB_TOKEN, ApprovalActionResponse.class);
        assertThat(secondApproval.getBody().status()).isEqualTo("APPROVED");

        ResponseEntity<ApprovalCheckResponse> finalCheck = post("/approval/check", CI_TOKEN,
                new ApprovalCheckRequest("jiye", "ProdAlbStack", "DEPLOY_D1"), ApprovalCheckResponse.class);
        assertThat(finalCheck.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void reject_immediatelyDeniesRecheck() {
        ApprovalRequestResponse created = createRequest("ProdAlbStack", "DEPLOY_D2");

        ResponseEntity<ApprovalActionResponse> rejectResponse =
                postNoBody("/approval/" + created.id() + "/reject", DemoDataSeeder.ALICE_TOKEN, ApprovalActionResponse.class);
        assertThat(rejectResponse.getBody().status()).isEqualTo("REJECTED");

        ResponseEntity<ApprovalCheckResponse> recheck = post("/approval/check", CI_TOKEN,
                new ApprovalCheckRequest("jiye", "ProdAlbStack", "DEPLOY_D2"), ApprovalCheckResponse.class);
        assertThat(recheck.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void approve_duplicateVoteReturns409() {
        ApprovalRequestResponse created = createRequest("ProdAlbStack", "DEPLOY_D3");

        postNoBody("/approval/" + created.id() + "/approve", DemoDataSeeder.ALICE_TOKEN, ApprovalActionResponse.class);
        ResponseEntity<String> duplicate =
                postNoBody("/approval/" + created.id() + "/approve", DemoDataSeeder.ALICE_TOKEN, String.class);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void approve_unknownRequestIdReturns404() {
        ResponseEntity<String> response = postNoBody("/approval/999999/approve", DemoDataSeeder.ALICE_TOKEN, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void approve_approverWithoutStackApproveClaimReturns403() {
        // bob may approve ProdAlbStack but holds no StagingApiStack:approve claim.
        ApprovalRequestResponse created = createRequest("StagingApiStack", "DEPLOY_S2");

        ResponseEntity<String> response =
                postNoBody("/approval/" + created.id() + "/approve", DemoDataSeeder.BOB_TOKEN, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void historyEndpointReturnsEntriesFilteredByStack() {
        post("/approval/check", CI_TOKEN,
                new ApprovalCheckRequest("jiye", "SmallAppStack", "DEPLOY"), ApprovalCheckResponse.class);

        ResponseEntity<ApprovalHistoryEntry[]> response =
                get("/approval/history?stack=SmallAppStack", DemoDataSeeder.JIYE_TOKEN, ApprovalHistoryEntry[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody()[0].stack()).isEqualTo("SmallAppStack");
    }
}
