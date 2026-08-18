package com.slotegrator.qa.http;

import java.util.regex.Pattern;

/**
 * Masks secrets before they reach a report, a log or an assertion message.
 *
 * <p>Necessary rather than decorative here: the tester's bearer token appears in every login response, and
 * the API echoes the submitted password back from {@code create} and {@code deleteOne} (BUG-001). An Allure
 * report is an HTML directory that gets zipped and shared, so anything left unmasked travels with it.
 *
 * <p>Field <em>names</em> survive masking — {@code "password_change":"***"} still proves the API echoed the
 * field, which is what BUG-001 is about, without carrying the value.
 */
public final class Sensitive {

    private static final String MASK = "\"***\"";

    /** JSON string values whose content is a secret. */
    private static final Pattern SECRET_JSON_FIELD = Pattern.compile(
            "(\"(?:password|password_change|password_repeat|passwordChange|passwordRepeat"
                    + "|accessToken|access_token|refreshToken|refresh_token|token)\"\\s*:\\s*)\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE);

    /** {@code Authorization: Bearer eyJ…} in a rendered header block — keeps the scheme, drops the token. */
    private static final Pattern AUTH_HEADER_WITH_SCHEME = Pattern.compile(
            "(?i)^(\\s*Authorization\\s*:\\s*)(\\S+)\\s+\\S.*$", Pattern.MULTILINE);

    /** {@code Authorization: eyJ…} — no scheme to keep, so mask the whole value. */
    private static final Pattern AUTH_HEADER_BARE = Pattern.compile(
            "(?i)^(\\s*Authorization\\s*:\\s*)\\S+\\s*$", Pattern.MULTILINE);

    private Sensitive() {
    }

    /** Masks secret JSON values and Authorization headers. Safe on {@code null}. */
    public static String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String masked = SECRET_JSON_FIELD.matcher(text).replaceAll("$1" + MASK);
        masked = AUTH_HEADER_WITH_SCHEME.matcher(masked).replaceAll("$1$2 ***");
        return AUTH_HEADER_BARE.matcher(masked).replaceAll("$1***");
    }

    /** Masks a single header value, keeping an auth scheme like {@code Bearer} visible. */
    public static String maskHeader(String name, String value) {
        if (!"authorization".equalsIgnoreCase(name) || value == null || value.isBlank()) {
            return value;
        }
        int space = value.indexOf(' ');
        return space > 0 ? value.substring(0, space) + " ***" : "***";
    }
}
