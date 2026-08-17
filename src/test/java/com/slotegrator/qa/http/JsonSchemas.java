package com.slotegrator.qa.http;

import io.restassured.module.jsv.JsonSchemaValidator;
import org.hamcrest.MatcherAssert;

/**
 * Contract assertions: the response body is validated against a JSON schema, so "the response matches the
 * documentation" is checked structurally rather than field by field.
 *
 * <p>Two sets of schemas exist. {@code schemas/*} describe what the API actually returns and back the regular
 * suite; {@code schemas/documented/*} are transcribed straight from the OpenAPI document and back
 * {@code ContractConformanceTest}, which reports where the two disagree.
 *
 * <p>Backed by RestAssured's {@code json-schema-validator}, applied to the raw body captured in
 * {@link ApiResponse}.
 */
public final class JsonSchemas {

    public static final String LOGIN_RESPONSE = "schemas/login-response.json";
    /** Shape returned by create and deleteOne: {@code _id} plus the echoed password fields. */
    public static final String PLAYER_CREATED = "schemas/player-created.json";
    /** Shape returned by getOne. */
    public static final String PLAYER_RESPONSE = "schemas/player-response.json";
    public static final String PLAYER_RESPONSE_LIST = "schemas/player-response-list.json";

    public static final String DOCUMENTED_TOKEN = "schemas/documented/token.json";
    public static final String DOCUMENTED_PLAYER_RESPONSE = "schemas/documented/player-response.json";

    private JsonSchemas() {
    }

    public static void assertMatches(ApiResponse<?> response, String schemaOnClasspath) {
        MatcherAssert.assertThat(
                "Response does not match " + schemaOnClasspath + " — " + response.describe(),
                response.rawBody(),
                JsonSchemaValidator.matchesJsonSchemaInClasspath(schemaOnClasspath));
    }
}
