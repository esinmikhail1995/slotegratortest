package com.slotegrator.qa.tests;

import com.slotegrator.qa.config.TestConfig;
import com.slotegrator.qa.data.PlayerFactory;
import com.slotegrator.qa.dto.CredentialsDto;
import com.slotegrator.qa.dto.LoginResponseDto;
import com.slotegrator.qa.dto.PlayerRequestDto;
import com.slotegrator.qa.dto.PlayerResponseDto;
import com.slotegrator.qa.http.ApiResponse;
import com.slotegrator.qa.http.JsonSchemas;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts the API against the OpenAPI document literally, rather than against how it actually behaves.
 *
 * <p>These tests <strong>currently fail</strong> — that is their purpose: each failure is one place where the
 * service and its documentation disagree, reported with the real payload. They are in the TestNG group
 * {@code contract} and excluded from the default run so the regular suite stays a clean regression signal.
 *
 * <pre>mvn test -Pcontract</pre>
 *
 * <p>When a deviation is fixed, the corresponding test starts passing and can be folded into the main suite.
 * Each finding is written up in {@code docs/bugs/}.
 */
@Epic("Players API")
@Feature("Conformance with the OpenAPI document")
@Severity(SeverityLevel.CRITICAL)
public class ContractConformanceTest extends BaseApiTest {

    private static final String CONTRACT = "contract";

    private final List<String> createdIds = new ArrayList<>();

    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        // These tests fail by design, so cleanup must not depend on them passing.
        createdIds.forEach(id -> players().deleteOne(id));
        createdIds.clear();
    }

    @Test(groups = CONTRACT, description = "BUG-002: login answers 200 with a TokenDTO")
    @Issue("BUG-002")
    public void loginMatchesDocumentedContract() {
        ApiResponse<LoginResponseDto> response = authApi.loginAsTester();

        assertThat(response.status()).as("documented status — %s", response.describe()).isEqualTo(200);
        JsonSchemas.assertMatches(response, JsonSchemas.DOCUMENTED_TOKEN);
    }

    @Test(groups = CONTRACT, description = "BUG-006: login enforces the BasicAuth layer it declares")
    @Issue("BUG-006")
    public void loginRequiresBasicAuth() {
        ApiResponse<LoginResponseDto> response = authApi.loginWithoutBasicAuth(
                new CredentialsDto(TestConfig.testerEmail(), TestConfig.testerPassword()));

        assertThat(response.status())
                .as("login without an Authorization: Basic header — %s", response.describe())
                .isEqualTo(401);
    }

    @Test(groups = CONTRACT,
            description = "BUG-001/003/004: create answers with a PlayerResponseDTO, no password echoed back")
    @Issue("BUG-001")
    public void createMatchesDocumentedContract() {
        ApiResponse<PlayerResponseDto> response = create();

        assertThat(response.status()).isEqualTo(201);
        // Fails twice over: the id is a hex string under the key _id, and the request's password fields
        // are echoed back in the response.
        JsonSchemas.assertMatches(response, JsonSchemas.DOCUMENTED_PLAYER_RESPONSE);
    }

    @Test(groups = CONTRACT, description = "BUG-003: getOne answers with a PlayerResponseDTO, integer id")
    @Issue("BUG-003")
    public void getOneMatchesDocumentedContract() {
        PlayerRequestDto request = PlayerFactory.player();
        rememberId(players().create(request));

        ApiResponse<PlayerResponseDto> response = players().getOne(request.email());

        JsonSchemas.assertMatches(response, JsonSchemas.DOCUMENTED_PLAYER_RESPONSE);
    }

    @Test(groups = CONTRACT, description = "BUG-007: getAll answers with a single PlayerResponseDTO object")
    @Issue("BUG-007")
    public void getAllMatchesDocumentedContract() {
        ApiResponse<List<PlayerResponseDto>> response = players().getAll();

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.rawBody())
                .as("documented as a PlayerResponseDTO object, not an array — %s", response.describe())
                .startsWith("{");
    }

    @Test(groups = CONTRACT, description = "BUG-005: deleting an unknown id is not reported as success")
    @Issue("BUG-005")
    public void deletingUnknownIdIsRejected() {
        // The service answers 200 with an empty body for an id that does not exist, so a client cannot tell
        // a real deletion from a no-op.
        ApiResponse<PlayerResponseDto> response = players().deleteOne("6a82deb2e091227194d4d9bf");

        assertThat(response.status())
                .as("deleting a non-existent player — %s", response.describe())
                .isIn(400, 404);
    }

    private ApiResponse<PlayerResponseDto> create() {
        return rememberId(players().create(PlayerFactory.player()));
    }

    private ApiResponse<PlayerResponseDto> rememberId(ApiResponse<PlayerResponseDto> response) {
        if (response.body() != null && response.body().id() != null) {
            createdIds.add(response.body().id());
        }
        return response;
    }
}
