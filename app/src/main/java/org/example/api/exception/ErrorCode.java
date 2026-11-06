package org.example.api.exception;

/**
 * Every {@code errorCode} the API can return, with its HTTP status and a default message
 * to show end users.
 */
public enum ErrorCode {

    // ── The request ─────────────────────────────────────────────────────────

    MISSING_PARAMETER(400, "A required parameter is missing.", false),
    VALIDATION_ERROR(400, "A parameter is invalid.", false),
    NOT_FOUND(404, "Nothing exists at this path.", false),
    METHOD_NOT_ALLOWED(405, "This HTTP method isn't supported at this path.", false),

    // ── The content ─────────────────────────────────────────────────────────

    CONTENT_UNAVAILABLE(404, "This content isn't available. It may have been removed, or never existed.", false),
    CONTENT_PRIVATE(403, "This content is private.", false),
    AGE_RESTRICTED(403, "This content is age-restricted and can't be played without signing in.", false),
    PAID_CONTENT(403, "This content is only available to paying members or subscribers.", false),
    GEO_RESTRICTED(451, "This content isn't available in the country this server is in.", false),
    ACCOUNT_TERMINATED(410, "The account this content belonged to has been terminated.", false),

    // ── YouTube ─────────────────────────────────────────────────────────────

    UPSTREAM_BLOCKED(503, "YouTube is blocking requests from this server. Try again later.", false),
    UPSTREAM_CHANGED(502, "YouTube sent a response the server couldn't read. YouTube may have changed.", true),
    UPSTREAM_UNREACHABLE(502, "The server couldn't reach YouTube. Try again later.", false),
    NO_STREAMS_AVAILABLE(503, "No playable streams were found for this video.", false),

    // ── The server ──────────────────────────────────────────────────────────

    EXTRACTION_ERROR(500, "The content couldn't be extracted.", true),
    INTERNAL_ERROR(500, "An unexpected error occurred", true);

    private final int httpStatus;
    private final String defaultMessage;
    private final boolean unexpected;

    ErrorCode(int httpStatus, String defaultMessage, boolean unexpected) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
        this.unexpected = unexpected;
    }

    public int httpStatus() {
        return httpStatus;
    }

    /** User-facing text used when the thrower doesn't supply its own message. */
    public String defaultMessage() {
        return defaultMessage;
    }

    /**
     * Whether this code points at a bug or an upstream change someone should look at.
     * These are logged at {@code ERROR} with a stack trace; the rest are routine outcomes.
     */
    public boolean unexpected() {
        return unexpected;
    }
}