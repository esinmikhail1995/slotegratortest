package com.slotegrator.qa.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/**
 * Single source of truth for the environment under test.
 *
 * <p>Resolution order for every key, first hit wins:
 * <ol>
 *   <li>system property, e.g. {@code -Dapi.base.url=...}</li>
 *   <li>environment variable, e.g. {@code API_BASE_URL} (key upper-cased, dots to underscores)</li>
 *   <li>{@code src/test/resources/config.properties}</li>
 * </ol>
 */
public final class TestConfig {

    private static final String FILE = "config.properties";
    private static final Properties FILE_PROPERTIES = load();

    private TestConfig() {
    }

    public static String baseUrl() {
        return required("api.base.url");
    }

    public static Transport transport() {
        return Transport.valueOf(get("api.transport").orElse("feign").trim().toUpperCase(Locale.ROOT));
    }

    /** Feign log level: NONE, BASIC, HEADERS, FULL. */
    public static String logLevel() {
        return get("api.log.level").orElse("BASIC").trim().toUpperCase(Locale.ROOT);
    }

    public static int connectTimeoutMs() {
        return Integer.parseInt(get("api.connect.timeout.ms").orElse("10000").trim());
    }

    public static int readTimeoutMs() {
        return Integer.parseInt(get("api.read.timeout.ms").orElse("30000").trim());
    }

    public static String testerEmail() {
        return required("auth.email");
    }

    public static String testerPassword() {
        return required("auth.password");
    }

    /** Optional HTTP Basic credentials for {@code /api/tester/login}; empty when the endpoint needs none. */
    public static Optional<String> basicUsername() {
        return nonBlank("auth.basic.username");
    }

    public static Optional<String> basicPassword() {
        return nonBlank("auth.basic.password");
    }

    public static int playersToCreate() {
        return Integer.parseInt(get("players.count").orElse("12").trim());
    }

    public static String playerCurrencyCode() {
        return get("player.currency.code").orElse("EUR").trim();
    }

    public static String playerPassword() {
        return get("player.password").orElse("Passw0rd!").trim();
    }

    /**
     * Whether the final {@code getAll} check demands a globally empty list (as the task states) or only
     * that the players created by this run are gone. Set to {@code false} on a shared test account.
     */
    public static boolean expectEmptyAfterCleanup() {
        return Boolean.parseBoolean(get("cleanup.expect.empty").orElse("true").trim());
    }

    public static Optional<String> get(String key) {
        String fromSystem = System.getProperty(key);
        if (isPresent(fromSystem)) {
            return Optional.of(fromSystem);
        }
        String fromEnv = System.getenv(toEnvKey(key));
        if (isPresent(fromEnv)) {
            return Optional.of(fromEnv);
        }
        return Optional.ofNullable(FILE_PROPERTIES.getProperty(key)).filter(TestConfig::isPresent);
    }

    private static Optional<String> nonBlank(String key) {
        return get(key).map(String::trim).filter(value -> !value.isEmpty());
    }

    private static String required(String key) {
        return nonBlank(key).orElseThrow(() -> new IllegalStateException(
                "Missing configuration '" + key + "'. Set it in src/test/resources/" + FILE
                        + ", or pass -D" + key + "=<value>, or export " + toEnvKey(key) + "=<value>."));
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static String toEnvKey(String key) {
        return key.toUpperCase(Locale.ROOT).replace('.', '_');
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = TestConfig.class.getClassLoader().getResourceAsStream(FILE)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + FILE, e);
        }
        return properties;
    }

    /** Which HTTP stack actually executes the Feign-declared calls. */
    public enum Transport {
        /** Feign's built-in java.net client. */
        FEIGN,
        /** RestAssured, plugged in as a {@code feign.Client}. */
        RESTASSURED
    }
}
