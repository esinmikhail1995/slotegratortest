package com.slotegrator.qa.http;

import com.slotegrator.qa.config.TestConfig;
import feign.Client;
import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/** Builds the Feign clients: transport, timeouts, logging and auth headers in one place. */
public final class FeignClients {

    private FeignClients() {
    }

    public static <T> T create(Class<T> api) {
        return builder().target(api, TestConfig.baseUrl());
    }

    /** Client whose every request carries {@code Authorization: Bearer <token>}. */
    public static <T> T createAuthorized(Class<T> api, String authorizationHeaderValue) {
        return builder()
                .requestInterceptor(bearer(authorizationHeaderValue))
                .target(api, TestConfig.baseUrl());
    }

    private static Feign.Builder builder() {
        return Feign.builder()
                .client(transport())
                // Only an encoder is configured: responses are decoded by ResponseMapper so that non-2xx
                // statuses stay values instead of turning into exceptions.
                .encoder(new JacksonEncoder(Json.mapper()))
                .logger(new Slf4jLogger("com.slotegrator.qa.http"))
                .logLevel(Logger.Level.valueOf(TestConfig.logLevel()))
                .options(new Request.Options(
                        TestConfig.connectTimeoutMs(), TimeUnit.MILLISECONDS,
                        TestConfig.readTimeoutMs(), TimeUnit.MILLISECONDS,
                        true))
                // Not retrying keeps a failing test's cause unambiguous.
                .retryer(feign.Retryer.NEVER_RETRY);
    }

    private static Client transport() {
        return switch (TestConfig.transport()) {
            case FEIGN -> new Client.Default(null, null);
            case RESTASSURED -> new RestAssuredFeignClient(!"NONE".equals(TestConfig.logLevel()));
        };
    }

    private static RequestInterceptor bearer(String authorizationHeaderValue) {
        return template -> template.header("Authorization", authorizationHeaderValue);
    }

    /** {@code Authorization: Basic ...} value, present only when Basic credentials are configured. */
    public static Optional<String> basicAuthHeader() {
        return TestConfig.basicUsername().map(username -> {
            String password = TestConfig.basicPassword().orElse("");
            String encoded = Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "Basic " + encoded;
        });
    }
}
