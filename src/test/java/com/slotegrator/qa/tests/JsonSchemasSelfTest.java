package com.slotegrator.qa.tests;

import com.slotegrator.qa.http.ApiResponse;
import com.slotegrator.qa.http.JsonSchemas;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Framework self-test: proves the contract assertions can fail. A schema that accepts everything would make
 * every "response matches the documentation" check meaningless, so each one is exercised against a known-bad
 * payload too.
 *
 * <p>Needs no environment — it runs offline.
 */
public class JsonSchemasSelfTest {

    private static final String VALID_PLAYER = """
            {"id":"6a82deb2e091227194d4d9bf","currency_code":"EUR","email":"john@doe.test",\
            "name":"John","surname":"Doe","username":"johndoe"}""";

    @Test(description = "accept a documented player and reject one with a missing or mistyped field")
    public void playerSchemaDiscriminates() {
        String missingSurname = """
                {"id":"6a82deb2e091227194d4d9bf","currency_code":"EUR","email":"john@doe.test",\
                "name":"John","username":"johndoe"}""";
        String numericId = """
                {"id":1,"currency_code":"EUR","email":"john@doe.test",\
                "name":"John","surname":"Doe","username":"johndoe"}""";

        assertThatCode(() -> JsonSchemas.assertMatches(response(VALID_PLAYER), JsonSchemas.PLAYER_RESPONSE))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> JsonSchemas.assertMatches(response(missingSurname), JsonSchemas.PLAYER_RESPONSE))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> JsonSchemas.assertMatches(response(numericId), JsonSchemas.PLAYER_RESPONSE))
                .isInstanceOf(AssertionError.class);
    }

    @Test(description = "the create shape requires _id, the read shape requires id — not interchangeable")
    public void createdAndReadShapesAreDistinct() {
        String created = """
                {"_id":"6a82deb2e091227194d4d9bf","currency_code":"EUR","email":"john@doe.test",\
                "name":"John","surname":"Doe","username":"johndoe","password_change":"x","password_repeat":"x"}""";

        assertThatCode(() -> JsonSchemas.assertMatches(response(created), JsonSchemas.PLAYER_CREATED))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> JsonSchemas.assertMatches(response(created), JsonSchemas.PLAYER_RESPONSE))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> JsonSchemas.assertMatches(response(VALID_PLAYER), JsonSchemas.PLAYER_CREATED))
                .isInstanceOf(AssertionError.class);
    }

    @Test(description = "accept a login response and reject one without an accessToken")
    public void loginSchemaDiscriminates() {
        String valid = """
                {"user":{"id":"6a7c80ace091227194d4d8ff","email":"tester@example.com"},"accessToken":"abc"}""";
        String noAccessToken = """
                {"user":{"id":"6a7c80ace091227194d4d8ff","email":"tester@example.com"}}""";

        assertThatCode(() -> JsonSchemas.assertMatches(response(valid), JsonSchemas.LOGIN_RESPONSE))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> JsonSchemas.assertMatches(response(noAccessToken), JsonSchemas.LOGIN_RESPONSE))
                .isInstanceOf(AssertionError.class);
    }

    @Test(description = "an empty player list is a valid list response")
    public void emptyListIsValid() {
        assertThatCode(() -> JsonSchemas.assertMatches(response("[]"), JsonSchemas.PLAYER_RESPONSE_LIST))
                .doesNotThrowAnyException();
        assertThatCode(() -> JsonSchemas.assertMatches(
                response("[" + VALID_PLAYER + "]"), JsonSchemas.PLAYER_RESPONSE_LIST))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> JsonSchemas.assertMatches(
                response("[{\"id\":\"1\"}]"), JsonSchemas.PLAYER_RESPONSE_LIST))
                .isInstanceOf(AssertionError.class);
    }

    @Test(description = "the documented schemas reject the payloads the service actually sends")
    public void documentedSchemasRejectTheActualPayloads() {
        // Guards ContractConformanceTest: if these ever start passing, the deviations have been fixed
        // and that suite can be promoted into the regular run.
        assertThatThrownBy(() -> JsonSchemas.assertMatches(
                response(VALID_PLAYER), JsonSchemas.DOCUMENTED_PLAYER_RESPONSE))
                .as("documented schema requires an integer id")
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> JsonSchemas.assertMatches(
                response("""
                        {"user":{"id":"x","email":"t@e.st"},"accessToken":"abc"}"""),
                JsonSchemas.DOCUMENTED_TOKEN))
                .as("documented schema requires access_token/token_type/expires_in/scope")
                .isInstanceOf(AssertionError.class);
    }

    @Test(description = "requiredBody fails with the raw payload in the message")
    public void requiredBodyReportsRawPayload() {
        ApiResponse<String> response = new ApiResponse<>(
                400, "Bad Request", Map.of(), "{\"message\":\"bad email\"}", null, "no suitable constructor");

        assertThatThrownBy(response::requiredBody)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("bad email")
                .hasMessageContaining("no suitable constructor");
        assertThat(response.isSuccessful()).isFalse();
    }

    private static ApiResponse<Void> response(String rawBody) {
        return new ApiResponse<>(200, "OK", Map.of(), rawBody, null, null);
    }
}
