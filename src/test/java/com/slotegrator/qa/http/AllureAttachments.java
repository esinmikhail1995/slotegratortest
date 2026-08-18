package com.slotegrator.qa.http;

import feign.Request;
import feign.Response;
import io.qameta.allure.Allure;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Attaches the request and the response of every call to the Allure report.
 *
 * <p>Hooked into {@link ResponseMapper}, which is the single point every call passes through — so the report
 * is identical whether the suite ran over the Feign transport or the RestAssured one, and no test has to
 * remember to attach anything.
 *
 * <p>Everything is masked by {@link Sensitive} on the way in: an Allure report is shareable HTML, and these
 * payloads contain the tester's bearer token and, thanks to BUG-001, player passwords.
 */
final class AllureAttachments {

    private static final int BODY_LIMIT = 64 * 1024;

    private AllureAttachments() {
    }

    static void attach(Response response, String rawBody) {
        Request request = response.request();
        if (request != null) {
            Allure.addAttachment(
                    request.httpMethod().name() + " " + pathOf(request.url()) + " — request",
                    "text/plain",
                    renderRequest(request),
                    ".txt");
        }
        Allure.addAttachment(
                "HTTP " + response.status() + " — response",
                "text/plain",
                renderResponse(response, rawBody),
                ".txt");
    }

    private static String renderRequest(Request request) {
        StringBuilder out = new StringBuilder()
                .append(request.httpMethod().name()).append(' ').append(request.url()).append('\n');
        appendHeaders(out, request.headers());
        byte[] body = request.body();
        if (body != null && body.length > 0) {
            out.append('\n').append(truncate(Sensitive.mask(new String(body, StandardCharsets.UTF_8))));
        }
        return out.toString();
    }

    private static String renderResponse(Response response, String rawBody) {
        StringBuilder out = new StringBuilder()
                .append("HTTP ").append(response.status());
        if (response.reason() != null && !response.reason().isBlank()) {
            out.append(' ').append(response.reason());
        }
        out.append('\n');
        appendHeaders(out, response.headers());
        out.append('\n');
        out.append(rawBody == null || rawBody.isEmpty() ? "<empty body>" : truncate(Sensitive.mask(rawBody)));
        return out.toString();
    }

    private static void appendHeaders(StringBuilder out, Map<String, Collection<String>> headers) {
        if (headers == null) {
            return;
        }
        // Sorted so two attachments of the same call are diffable.
        new TreeMap<>(headers).forEach((name, values) ->
                values.forEach(value -> out.append(name).append(": ")
                        .append(Sensitive.maskHeader(name, value)).append('\n')));
    }

    private static String truncate(String body) {
        return body.length() <= BODY_LIMIT
                ? body
                : body.substring(0, BODY_LIMIT) + "\n… truncated at " + BODY_LIMIT + " characters";
    }

    /** Keeps the attachment name short — the full URL is in the body. */
    private static String pathOf(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            return url;
        }
        int pathStart = url.indexOf('/', schemeEnd + 3);
        return pathStart < 0 ? "/" : url.substring(pathStart);
    }
}
