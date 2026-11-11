package org.example.api.exception;

/**
 * Extraction succeeded but nothing in the result is playable: {@code 503 NO_STREAMS_AVAILABLE}.
 */
public class NoStreamsAvailableException extends ApiException {
    public NoStreamsAvailableException(String message) {
        super(ErrorCode.NO_STREAMS_AVAILABLE, message);
    }
}
