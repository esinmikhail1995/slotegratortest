package com.slotegrator.qa.tests;

import com.slotegrator.qa.api.AuthApi;
import com.slotegrator.qa.config.TestConfig;
import com.slotegrator.qa.dto.CredentialsDto;
import com.slotegrator.qa.dto.LoginResponseDto;
import com.slotegrator.qa.http.ApiResponse;
import com.slotegrator.qa.http.JsonSchemas;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Step 1: obtain a tester token. {@code POST /api/tester/login}. */
@Epic("Players API")
@Feature("Authentication")
@Severity(SeverityLevel.BLOCKER)
public class AuthTest {

    private final AuthApi authApi = new AuthApi();

    @Test(description = "login returns a usable access token for valid credentials")
    public void loginReturnsToken() {
        ApiResponse<LoginResponseDto> response = authApi.loginAsTester();

        // The contract documents 200; the service answers 201. Tracked in ContractConformanceTest.
        assertThat(response.status()).as("status — %s", response.describe())
                .isEqualTo(AuthApi.LOGIN_SUCCESS_STATUS);
        JsonSchemas.assertMatches(response, JsonSchemas.LOGIN_RESPONSE);

        LoginResponseDto login = response.requiredBody();
        assertThat(login.accessToken()).as("accessToken").isNotBlank();
        assertThat(login.user()).as("user").isNotNull();
        assertThat(login.user().email())
                .as("token is issued for the account that logged in")
                .isEqualToIgnoringCase(TestConfig.testerEmail());
        assertThat(login.authorizationHeader()).startsWith("Bearer ");
    }

    @Test(description = "login rejects a wrong password with 401")
    public void loginWithWrongPasswordIsRejected() {
        ApiResponse<LoginResponseDto> response =
                authApi.login(new CredentialsDto(TestConfig.testerEmail(), "definitely-not-the-password"));

        assertThat(response.status()).as("status — %s", response.describe()).isEqualTo(401);
        // An error payload can still deserialise into the DTO full of nulls, so check the field itself.
        String accessToken = response.body() == null ? null : response.body().accessToken();
        assertThat(accessToken).as("no token must be issued — %s", response.describe()).isNull();
    }

    @Test(description = "login rejects an unknown account with 401")
    public void loginWithUnknownEmailIsRejected() {
        ApiResponse<LoginResponseDto> response =
                authApi.login(new CredentialsDto("no-such-tester@qa-automation.test", "whatever"));

        assertThat(response.status()).as("status — %s", response.describe()).isEqualTo(401);
    }
}
