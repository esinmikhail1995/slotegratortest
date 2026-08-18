package com.slotegrator.qa.tests;

import com.slotegrator.qa.api.PlayersApi;
import com.slotegrator.qa.data.PlayerFactory;
import com.slotegrator.qa.dto.PlayerResponseDto;
import com.slotegrator.qa.http.ApiResponse;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The player endpoints are declared with BearerAuth — an anonymous caller must be turned away. */
@Epic("Players API")
@Feature("Authorisation")
@Severity(SeverityLevel.CRITICAL)
public class PlayersSecurityTest {

    private final PlayersApi anonymous = PlayersApi.anonymous();

    @Test(description = "GET /getAll without a token is rejected")
    public void getAllRequiresToken() {
        ApiResponse<List<PlayerResponseDto>> response = anonymous.getAll();
        assertThat(response.status()).as("status — %s", response.describe()).isIn(401, 403);
    }

    @Test(description = "POST /create without a token is rejected")
    public void createRequiresToken() {
        ApiResponse<PlayerResponseDto> response = anonymous.create(PlayerFactory.player());
        assertThat(response.status()).as("status — %s", response.describe()).isIn(401, 403);
    }
}
