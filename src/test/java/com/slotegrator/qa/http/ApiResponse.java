package com.slotegrator.qa.http;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Status + headers + raw body + deserialised body of one call.
 *
 * <p>Tests always get an {@code ApiResponse} — error statuses are values, not exceptions — so a test can
 * assert on {@code 400}/{@code 401} exactly the way it asserts on {@code 200}. The raw body is retained so
 * that JSON-schema assertions and failure messages can show what the server actually sent.
 *
 * @param body deserialised payload, {@code null} when the body was empty or did not match {@code T}
 *             (see {@link #parseError()})
 */
public record ApiResponse<T>(
        int status,
        String reason,
        Map<String, Collection<String>> headers,
        String rawBody,
        T body,
        String parseError) {

    public boolean isSuccessful() {
        return status >= 200 && status < 300;
    }

    public Optional<String> parseErrorMessage() {
        return Optional.ofNullable(parseError);
    }

    public Optional<String> header(String name) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .flatMap(entry -> entry.getValue().stream())
                .findFirst();
    }

    public Optional<String> contentType() {
        return header("Content-Type");
    }

    /** The deserialised body, failing loudly (with the raw payload) when it is not there. */
    public T requiredBody() {
        if (body == null) {
            throw new AssertionError("No deserialised body for HTTP " + status + " " + reason
                    + parseErrorMessage().map(message -> "; parse error: " + message).orElse("")
                    + "; raw body: " + describeRawBody());
        }
        return body;
    }

    /** Short single-line description usable in assertion messages. */
    public String describe() {
        return "HTTP " + status + " " + reason + " body=" + describeRawBody();
    }

    private String describeRawBody() {
        if (rawBody == null) {
            return "<none>";
        }
        String flat = rawBody.replaceAll("\\s+", " ").trim();
        return flat.length() > 2000 ? flat.substring(0, 2000) + "…" : flat;
    }

    static <T> ApiResponse<T> of(int status, String reason, Map<String, Collection<String>> headers,
                                 String rawBody, T body, String parseError) {
        return new ApiResponse<>(status, reason == null ? "" : reason,
                headers == null ? Map.of() : Map.copyOf(headers), rawBody, body, parseError);
    }

    /** Convenience for list endpoints: never {@code null}. */
    public static <E> List<E> orEmpty(List<E> list) {
        return list == null ? List.of() : list;
    }
}
