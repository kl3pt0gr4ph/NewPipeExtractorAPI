package org.example.api.exception;

import java.util.Objects;

/**
 * Base exception for every error the API reports deliberately.
 *
 * <p>The {@link ErrorCode} decides the HTTP status and the {@code errorCode} in the response.
 * The message is sent to the client as {@code message}, so it must be text the API wrote,
 * never a library's exception message. Keep the original exception as the {@code cause}:
 * it is logged, and shown in {@code detail} when {@code opentube.errors.include-detail}
 * is on.</p>
 */
public class ApiException extends RuntimeException {
    private final ErrorCode code;

    public ApiException(ErrorCode code) {
        this(code, null, null);
    }

    public ApiException(ErrorCode code, String message) {
        this(code, message, null);
    }

    public ApiException(ErrorCode code, Throwable cause) {
        this(code, null, cause);
    }

    public ApiException(ErrorCode code, String message, Throwable cause) {
        super(message != null ? message : Objects.requireNonNull(code, "code").defaultMessage(), cause);
        this.code = Objects.requireNonNull(code, "code");
    }

    public ErrorCode getCode() {
        return code;
    }

    /** The {@code errorCode} string sent to the client. */
    public String getErrorCode() {
        return code.name();
    }

    public int getHttpStatus() {
        return code.httpStatus();
    }
}