package org.example.api.exception;

/**
 * A NewPipeExtractor call failed.
 *
 * <p>Pass the exception to {@link NewPipeExceptionMapper#translate(Throwable)}, which
 * picks the code and keeps the library's wording out of the response.</p>
 */
public class ExtractionException extends ApiException {

    /** {@code EXTRACTION_ERROR} with a message the API wrote. */
    public ExtractionException(String message) {
        super(ErrorCode.EXTRACTION_ERROR, message);
    }

    /** {@code EXTRACTION_ERROR} with a message the API wrote. */
    public ExtractionException(String message, Throwable cause) {
        super(ErrorCode.EXTRACTION_ERROR, message, cause);
    }

    /** The given code with its default message. Used by {@link NewPipeExceptionMapper}. */
    public ExtractionException(ErrorCode code, Throwable cause) {
        super(code, cause);
    }
}
