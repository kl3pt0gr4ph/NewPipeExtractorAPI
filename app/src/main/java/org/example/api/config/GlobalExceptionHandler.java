package org.example.api.config;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
 *
 * <p>The {@link ApiResponse} annotations here are picked up by springdoc and added to
 * every operation in the OpenAPI spec, so controllers don't repeat them. Responses that
 * only some endpoints can produce (e.g. {@code 503}) are declared on those endpoints.</p>
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String BAD_REQUEST_DESCRIPTION =
            "A required parameter is missing (`MISSING_PARAMETER`) or invalid (`VALIDATION_ERROR`)";
    private static final String SERVER_ERROR_DESCRIPTION =
            "Extraction failed (`EXTRACTION_ERROR`), e.g. the video is unavailable or YouTube changed, "
                    + "or something unexpected went wrong (`INTERNAL_ERROR`)";

    /**
     * Handle validation exceptions (400 Bad Request).
     */
    @ApiResponse(responseCode = "400", description = BAD_REQUEST_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
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
    @ApiResponse(responseCode = "500", description = SERVER_ERROR_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
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
    @ApiResponse(responseCode = "400", description = BAD_REQUEST_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
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
    @ApiResponse(responseCode = "500", description = SERVER_ERROR_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
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