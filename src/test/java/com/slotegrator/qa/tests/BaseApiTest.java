package com.slotegrator.qa.tests;

import com.slotegrator.qa.api.AuthApi;
import com.slotegrator.qa.api.PlayersApi;
import com.slotegrator.qa.dto.LoginResponseDto;
import com.slotegrator.qa.http.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Logs in once per JVM and shares the token, so a suite of N test classes costs one login rather than N.
 *
 * <p>TestNG runs a superclass {@code @BeforeClass} before the subclass's own, so a subclass may rely on the
 * token being present in its own {@code @BeforeClass}.
 */
public abstract class BaseApiTest {

    protected static final Logger log = LoggerFactory.getLogger(BaseApiTest.class);

    protected static final AuthApi authApi = new AuthApi();

    private static LoginResponseDto session;

    @BeforeClass(alwaysRun = true)
    public void authenticate() {
        if (session != null) {
            return;
        }
        ApiResponse<LoginResponseDto> response = authApi.loginAsTester();
        assertThat(response.status())
                .as("Login failed, the rest of the suite cannot run — %s", response.describe())
                .isEqualTo(AuthApi.LOGIN_SUCCESS_STATUS);
        session = response.requiredBody();
        assertThat(session.accessToken())
                .as("Login returned no accessToken — %s", response.describe())
                .isNotBlank();
        log.info("Authenticated as {}", session.user() == null ? "<unknown>" : session.user().email());
    }

    protected static LoginResponseDto session() {
        return session;
    }

    /** A players client authorised with the shared token. */
    protected static PlayersApi players() {
        return PlayersApi.withToken(session.authorizationHeader());
    }
}
