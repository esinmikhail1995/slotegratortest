package com.slotegrator.qa.tests;

import com.slotegrator.qa.api.PlayersApi;
import com.slotegrator.qa.config.TestConfig;
import com.slotegrator.qa.data.PlayerFactory;
import com.slotegrator.qa.dto.PlayerRequestDto;
import com.slotegrator.qa.dto.PlayerResponseDto;
import com.slotegrator.qa.http.ApiResponse;
import com.slotegrator.qa.http.JsonSchemas;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps 2–6 of the task, as one ordered scenario: create 12 players, read one back, list and sort them all,
 * delete them, then verify the list is empty.
 *
 * <p>The steps share state (ids and payloads of the created players). Order is expressed with
 * {@code dependsOnMethods} rather than a priority number, so it states <em>why</em> each step follows the
 * previous one — and a broken step leaves its dependents reported as skipped instead of failing them all
 * with the same cascading symptom.
 */
@Epic("Players API")
@Feature("Player lifecycle")
@Story("Register 12 players, read them back, then remove them")
@Severity(SeverityLevel.CRITICAL)
public class PlayerLifecycleTest extends BaseApiTest {

    /** Requested payload keyed by the id the API assigned to it. */
    private final Map<String, PlayerRequestDto> created = new LinkedHashMap<>();

    private PlayersApi players;

    @BeforeClass(alwaysRun = true)
    public void initClient() {
        // Runs after BaseApiTest#authenticate, so the token is available.
        players = players();
    }

    @AfterClass(alwaysRun = true)
    public void removeLeftovers() {
        // Safety net: if the scenario broke before step 5, do not leave players behind for the next run.
        created.keySet().forEach(id -> {
            try {
                players.deleteOne(id);
            } catch (RuntimeException e) {
                log.debug("Leftover cleanup of id {} failed: {}", id, e.getMessage());
            }
        });
    }

    @Test(description = "2. POST /create registers 12 players and echoes each one back")
    public void createsTwelvePlayers() {
        int count = TestConfig.playersToCreate();
        List<PlayerRequestDto> requests = PlayerFactory.players(count);

        for (PlayerRequestDto request : requests) {
            ApiResponse<PlayerResponseDto> response = players.create(request);

            assertThat(response.status())
                    .as("create '%s' — %s", request.username(), response.describe())
                    .isEqualTo(PlayersApi.CREATE_STATUS);
            JsonSchemas.assertMatches(response, JsonSchemas.PLAYER_CREATED);

            PlayerResponseDto player = response.requiredBody();
            assertThat(player.id()).as("assigned id").isNotBlank();
            assertThat(player.username()).as("username echoed back").isEqualTo(request.username());
            assertThat(player.email()).as("email echoed back").isEqualTo(request.email());
            assertThat(player.name()).as("name echoed back").isEqualTo(request.name());
            assertThat(player.surname()).as("surname echoed back").isEqualTo(request.surname());
            assertThat(player.currencyCode()).as("currency_code echoed back").isEqualTo(request.currencyCode());

            assertThat(created.put(player.id(), request))
                    .as("id %s was already assigned to another player", player.id())
                    .isNull();
        }

        assertThat(created).as("all %s players were created with distinct ids", count).hasSize(count);
        log.info("Created {} players", created.size());
    }

    @Test(description = "3. POST /getOne returns the created player's profile",
            dependsOnMethods = "createsTwelvePlayers", priority = 1)
    public void returnsProfileOfCreatedPlayer() {
        PlayerRequestDto expected = firstCreatedPlayer();

        ApiResponse<PlayerResponseDto> response = players.getOne(expected.email());

        assertThat(response.status()).as("status — %s", response.describe())
                .isEqualTo(PlayersApi.GET_ONE_STATUS);
        JsonSchemas.assertMatches(response, JsonSchemas.PLAYER_RESPONSE);

        PlayerResponseDto player = response.requiredBody();
        assertThat(player.email()).isEqualTo(expected.email());
        assertThat(player.username()).isEqualTo(expected.username());
        assertThat(player.name()).isEqualTo(expected.name());
        assertThat(player.surname()).isEqualTo(expected.surname());
        assertThat(player.currencyCode()).isEqualTo(expected.currencyCode());
        assertThat(player.id()).as("id matches the one returned by create").isEqualTo(idOf(expected));
    }

    @Test(description = "3a. POST /getOne rejects an unknown email",
            dependsOnMethods = "createsTwelvePlayers", priority = 2)
    public void getOneRejectsUnknownEmail() {
        ApiResponse<PlayerResponseDto> response = players.getOne("no-such-player@qa-automation.test");

        assertThat(response.status()).as("status — %s", response.describe()).isEqualTo(400);
    }

    @Test(description = "4. GET /getAll lists every created player and sorts them by name",
            dependsOnMethods = "createsTwelvePlayers", priority = 3)
    public void listsAllPlayersSortedByName() {
        ApiResponse<List<PlayerResponseDto>> response = players.getAll();

        assertThat(response.status()).as("status — %s", response.describe()).isEqualTo(200);
        JsonSchemas.assertMatches(response, JsonSchemas.PLAYER_RESPONSE_LIST);

        List<PlayerResponseDto> all = response.requiredBody();
        assertThat(all).extracting(PlayerResponseDto::id)
                .as("every created player is present in getAll")
                .containsAll(created.keySet());

        Comparator<String> byText = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
        List<PlayerResponseDto> sorted = new ArrayList<>(all);
        sorted.sort(Comparator.comparing(PlayerResponseDto::name, byText)
                .thenComparing(PlayerResponseDto::surname, byText)
                .thenComparing(PlayerResponseDto::id, byText));

        assertThat(sorted)
                .as("sorting must keep the same players, only reorder them")
                .containsExactlyInAnyOrderElementsOf(all);
        assertThat(sorted).extracting(PlayerResponseDto::name)
                .as("names are in ascending order after sorting")
                .isSortedAccordingTo(byText);

        log.info("getAll returned {} players, sorted by name: {}",
                sorted.size(), sorted.stream().map(PlayerResponseDto::name).toList());
    }

    @Test(description = "5. DELETE /deleteOne/{id} removes every player created by this run",
            dependsOnMethods = {"returnsProfileOfCreatedPlayer", "getOneRejectsUnknownEmail",
                    "listsAllPlayersSortedByName"})
    public void deletesAllCreatedPlayers() {
        assertThat(created).as("nothing to delete — step 2 must run first").isNotEmpty();

        for (Map.Entry<String, PlayerRequestDto> entry : created.entrySet()) {
            ApiResponse<PlayerResponseDto> response = players.deleteOne(entry.getKey());

            assertThat(response.status())
                    .as("delete id %s — %s", entry.getKey(), response.describe())
                    .isEqualTo(200);
            assertThat(response.requiredBody().email())
                    .as("the deleted player is returned")
                    .isEqualTo(entry.getValue().email());
        }
        log.info("Deleted {} players", created.size());
    }

    @Test(description = "6. GET /getAll returns an empty list after the cleanup",
            dependsOnMethods = "deletesAllCreatedPlayers")
    public void listIsEmptyAfterCleanup() {
        ApiResponse<List<PlayerResponseDto>> response = players.getAll();

        assertThat(response.status()).as("status — %s", response.describe()).isEqualTo(200);

        List<PlayerResponseDto> remaining = ApiResponse.orEmpty(response.body());
        if (TestConfig.expectEmptyAfterCleanup()) {
            assertThat(remaining).as("getAll must be empty — %s", response.describe()).isEmpty();
        } else {
            // Shared account: only assert that this run left nothing behind.
            assertThat(remaining).extracting(PlayerResponseDto::id)
                    .as("players created by this run must be gone — %s", response.describe())
                    .doesNotContainAnyElementsOf(created.keySet());
        }
    }

    private PlayerRequestDto firstCreatedPlayer() {
        assertThat(created).as("no player was created — step 2 must run first").isNotEmpty();
        return created.values().iterator().next();
    }

    private String idOf(PlayerRequestDto request) {
        return created.entrySet().stream()
                .filter(entry -> entry.getValue().equals(request))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
    }
}
