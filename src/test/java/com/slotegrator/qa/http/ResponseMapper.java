package com.slotegrator.qa.http;

import com.fasterxml.jackson.core.type.TypeReference;
import feign.Response;
import feign.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Turns a raw {@link Response} into a typed {@link ApiResponse}.
 *
 * <p>The Feign interfaces deliberately return {@code feign.Response} rather than a DTO: that is the one
 * return type Feign hands back for every status code, so error responses reach the tests as data instead of
 * being converted into exceptions by an {@code ErrorDecoder}. Deserialisation happens here instead.
 */
public final class ResponseMapper {

    private static final Logger log = LoggerFactory.getLogger(ResponseMapper.class);

    private ResponseMapper() {
    }

    public static <T> ApiResponse<T> map(Response response, TypeReference<T> type) {
        try (Response open = response) {
            String rawBody = readBody(open);
            T body = null;
            String parseError = null;
            if (rawBody != null && !rawBody.isBlank()) {
                try {
                    body = Json.mapper().readValue(rawBody, type);
                } catch (Exception e) {
                    // Expected for error payloads, which do not follow the success schema.
                    parseError = e.getMessage();
                    log.debug("Body of HTTP {} does not match {}: {}", open.status(), type.getType(), e.getMessage());
                }
            }
            return ApiResponse.of(open.status(), open.reason(), open.headers(), rawBody, body, parseError);
        }
    }

    /** For endpoints whose payload is irrelevant to the assertion. */
    public static ApiResponse<Void> mapWithoutBody(Response response) {
        try (Response open = response) {
            return ApiResponse.of(open.status(), open.reason(), open.headers(), readBody(open), null, null);
        }
    }

    private static String readBody(Response response) {
        if (response.body() == null) {
            return null;
        }
        try (Reader reader = response.body().asReader(StandardCharsets.UTF_8)) {
            return Util.toString(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read response body of HTTP " + response.status(), e);
        }
    }
}
