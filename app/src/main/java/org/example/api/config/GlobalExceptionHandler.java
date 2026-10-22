package org.example.api.config;

import org.example.api.dto.ErrorResponseDTO;
import org.example.api.exception.ApiException;
import org.example.api.exception.ExtractionException;
import org.example.api.exception.NoStreamsAvailableException;
import org.example.api.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler for the API.
 * Provides consistent error responses across all endpoints.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    /**
     * Handle validation exceptions (400 Bad Request).
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(
            ValidationException ex,
            WebRequest request
    ) {
        logger.warn("Validation error: {}", ex.getMessage());

        ErrorResponseDTO error = new ErrorResponseDTO(
                ex.getHttpStatus(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                ex.getErrorCode()
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle extraction exceptions (500 Internal Server Error).
     */
    @ExceptionHandler(ExtractionException.class)
    public ResponseEntity<ErrorResponseDTO> handleExtractionException(
            ExtractionException ex,
            WebRequest request
    ) {
        logger.error("Extraction error: {}", ex.getMessage(), ex);

        ErrorResponseDTO error = new ErrorResponseDTO(
                ex.getHttpStatus(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ex.getMessage(),
                ex.getErrorCode()
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handle generic API exceptions.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponseDTO> handleApiException(
            ApiException ex,
            WebRequest request
    ) {
        logger.error("API error: {}", ex.getMessage(), ex);

        HttpStatus status = HttpStatus.valueOf(ex.getHttpStatus());
        ErrorResponseDTO error = new ErrorResponseDTO(
                ex.getHttpStatus(),
                status.getReasonPhrase(),
                ex.getMessage(),
                ex.getErrorCode()
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(status).body(error);
    }

    /**
     * Handle missing request parameters (400 Bad Request).
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingParameter(
            MissingServletRequestParameterException ex,
            WebRequest request
    ) {
        logger.warn("Missing parameter: {}", ex.getParameterName());

        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                String.format("Required parameter '%s' is missing", ex.getParameterName()),
                "MISSING_PARAMETER"
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle No stream available exception (503 Service Unavailable Error)
     */
    @ExceptionHandler(NoStreamsAvailableException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoStreamsAvailable(
            NoStreamsAvailableException ex, WebRequest request) {
        logger.warn("No streams available: {}", ex.getMessage());

        ErrorResponseDTO error = new ErrorResponseDTO(
                ex.getHttpStatus(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                ex.getMessage(),
                ex.getErrorCode());
        error.setPath(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Handle all other exceptions (500 Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(
            Exception ex,
            WebRequest request
    ) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);

        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred",
                "INTERNAL_ERROR"
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
