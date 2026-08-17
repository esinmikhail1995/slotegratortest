package com.slotegrator.qa.http;

import feign.Client;
import feign.Request;
import feign.Response;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.HttpClientConfig.httpClientConfig;

/**
 * Executes Feign-declared calls over RestAssured.
 *
 * <p>This keeps a single declarative API surface (the Feign interfaces) while letting the same suite run on
 * either HTTP stack — switch with {@code -Dapi.transport=restassured}. It also means RestAssured's request /
 * response logging is available for the whole suite without duplicating the client code.
 */
public final class RestAssuredFeignClient implements Client {

    /** RestAssured sets these itself; forwarding Feign's copies produces duplicate or wrong values. */
    private static final List<String> SKIPPED_REQUEST_HEADERS = List.of("content-length", "host");

    private final boolean logTraffic;

    public RestAssuredFeignClient(boolean logTraffic) {
        this.logTraffic = logTraffic;
    }

    @Override
    public Response execute(Request request, Request.Options options) {
        RequestSpecification spec = RestAssured.given()
                .config(config(options))
                // The URL Feign hands over is already encoded.
                .urlEncodingEnabled(false)
                .relaxedHTTPSValidation();

        if (logTraffic) {
            spec.filter(new RequestLoggingFilter()).filter(new ResponseLoggingFilter());
        }

        request.headers().forEach((name, values) -> {
            if (!SKIPPED_REQUEST_HEADERS.contains(name.toLowerCase())) {
                values.forEach(value -> spec.header(name, value));
            }
        });

        if (request.body() != null && request.body().length > 0) {
            spec.body(request.body());
        }

        io.restassured.response.Response restAssured =
                spec.request(request.httpMethod().name(), request.url());

        return Response.builder()
                .request(request)
                .status(restAssured.statusCode())
                .reason(reasonOf(restAssured))
                .headers(toFeignHeaders(restAssured))
                .body(restAssured.asByteArray())
                .build();
    }

    private static RestAssuredConfig config(Request.Options options) {
        return RestAssured.config()
                .encoderConfig(encoderConfig().defaultContentCharset("UTF-8"))
                .httpClient(httpClientConfig()
                        .setParam("http.connection.timeout", (int) options.connectTimeoutUnit()
                                .toMillis(options.connectTimeoutMillis()))
                        .setParam("http.socket.timeout", (int) options.readTimeoutUnit()
                                .toMillis(options.readTimeoutMillis())));
    }

    private static String reasonOf(io.restassured.response.Response response) {
        String statusLine = response.getStatusLine();
        if (statusLine == null) {
            return "";
        }
        // "HTTP/1.1 201 Created" -> "Created"
        String[] parts = statusLine.split(" ", 3);
        return parts.length == 3 ? parts[2] : "";
    }

    private static Map<String, Collection<String>> toFeignHeaders(io.restassured.response.Response response) {
        Map<String, Collection<String>> headers = new LinkedHashMap<>();
        response.getHeaders().forEach(header ->
                headers.computeIfAbsent(header.getName(), key -> new ArrayList<>()).add(header.getValue()));
        return headers;
    }
}
