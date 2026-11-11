package org.example.api.exception;

/**
 * The client sent a bad request: {@code 400 VALIDATION_ERROR}.
 *
 * <p>Only for problems with what the client sent. A broken invariant inside the API is a
 * bug, not a bad request: throw {@link IllegalStateException} for those, which the catch-all
 * reports as {@code 500 INTERNAL_ERROR}.</p>
 */
public class ValidationException extends ApiException {
    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }

    public ValidationException(String field, String reason) {
        super(ErrorCode.VALIDATION_ERROR, String.format("Invalid %s: %s", field, reason));
    }
}
