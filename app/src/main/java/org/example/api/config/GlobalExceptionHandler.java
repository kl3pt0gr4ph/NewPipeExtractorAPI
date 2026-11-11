package org.example.api.config;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.Set;
import org.example.api.dto.ErrorResponseDTO;
import org.example.api.exception.ApiException;
import org.example.api.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every exception that leaves a controller into an {@link ErrorResponseDTO}.
 *
 * <p>Errors the API raises on purpose are {@link ApiException}s and all go through
 * {@link #handleApiException}, which takes the status and {@code errorCode} from the
 * exception's {@link ErrorCode}. A few Spring exceptions get their own handlers so they
 * keep their 4xx status. Anything else is a bug and becomes {@code 500 INTERNAL_ERROR}.</p>
 *
 * <p>{@code message} is always text the API wrote. The underlying exception's message is
 * logged, and also returned as {@code detail} when {@code opentube.errors.include-detail}
 * is on.</p>
 *
 * <p>The {@link ApiResponse} annotations here are picked up by springdoc and added to
 * every operation in the OpenAPI spec, so controllers don't repeat them. Responses that
 * only some endpoints can produce (e.g. {@code 503 NO_STREAMS_AVAILABLE}) are declared on
 * those endpoints.</p>
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final int DETAIL_DEPTH = 5;

    private static final String ERROR_JSON = "application/json";

    static final String BAD_REQUEST_DESCRIPTION = "`MISSING_PARAMETER`: a required parameter is absent. "
            + "`VALIDATION_ERROR`: a parameter is empty, malformed, of the wrong type, "
            + "or names something the target doesn't have (e.g. a channel tab)";
    static final String FORBIDDEN_DESCRIPTION =
            "The content exists but can't be played: `CONTENT_PRIVATE`, `AGE_RESTRICTED` "
                    + "(needs a signed-in account) or `PAID_CONTENT` (members, YouTube Music Premium)";
    static final String NOT_FOUND_DESCRIPTION = "`CONTENT_UNAVAILABLE`: the content was removed or never existed. "
            + "`NOT_FOUND`: no such endpoint or kiosk";
    static final String GONE_DESCRIPTION = "`ACCOUNT_TERMINATED`: the account the content belonged to was terminated";
    static final String UNAVAILABLE_FOR_LEGAL_REASONS_DESCRIPTION =
            "`GEO_RESTRICTED`: not available in the country the server is in";
    static final String SERVER_ERROR_DESCRIPTION =
            "`EXTRACTION_ERROR`: extraction failed for a reason with no more specific code. "
                    + "`INTERNAL_ERROR`: a bug in the API";
    static final String BAD_GATEWAY_DESCRIPTION =
            "`UPSTREAM_CHANGED`: YouTube's response couldn't be parsed, usually because YouTube "
                    + "changed and NewPipeExtractor needs an update. "
                    + "`UPSTREAM_UNREACHABLE`: YouTube couldn't be reached";
    static final String SERVICE_UNAVAILABLE_DESCRIPTION =
            "`UPSTREAM_BLOCKED`: YouTube is rate-limiting or bot-checking the server's IP";

    private final boolean includeDetail;

    /** Without detail. For tests and standalone {@code MockMvc} setups. */
    public GlobalExceptionHandler() {
        this(new ApiErrorProperties(false));
    }

    @Autowired
    public GlobalExceptionHandler(ApiErrorProperties properties) {
        this.includeDetail = properties.includeDetail();
    }

    /**
     * Every error the API raises on purpose: validation, extraction, no streams.
     */
    @ApiResponse(
            responseCode = "400",
            description = BAD_REQUEST_DESCRIPTION,
            content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponseDTO.class)))
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN_DESCRIPTION,
            content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponseDTO.class)))
    @ApiResponse(
            responseCode = "404",
            description = NOT_FOUND_DESCRIPTION,
            content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponseDTO.class)))
    @ApiResponse(
            responseCode = "410",
            description = GONE_DESCRIPTION,
            content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponseDTO.class)))
    @ApiResponse(
            responseCode = "451",
            description = UNAVAILABLE_FOR_LEGAL_REASONS_DESCRIPTION,
            content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponseDTO.class)))
    @ApiResponse(
            responseCode = "502",
            description = BAD_GATEWAY_DESCRIPTION,
            content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponseDTO.class)))
    @ApiResponse(
            responseCode = "503",
            description = SERVICE_UNAVAILABLE_DESCRIPTION,
            content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponseDTO.class)))
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponseDTO> handleApiException(ApiException ex, WebRequest request) {
        return respond(ex.getCode(), ex.getMessage(), ex.getCause(), ex, request);
    }

    /**
     * A required {@code @RequestParam} is absent.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingParameter(
            MissingServletRequestParameterException ex, WebRequest request) {
        return respond(
                ErrorCode.MISSING_PARAMETER,
                "Required parameter '%s' is missing".formatted(ex.getParameterName()),
                null,
                null,
                request);
    }

    /**
     * A parameter can't be converted to its declared type, e.g. {@code ?count=abc} for an int.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        Class<?> requiredType = ex.getRequiredType();
        String message = requiredType != null
                ? "Parameter '%s' must be of type %s".formatted(ex.getName(), requiredType.getSimpleName())
                : "Parameter '%s' has an invalid value".formatted(ex.getName());
        return respond(ErrorCode.VALIDATION_ERROR, message, ex, null, request);
    }

    /**
     * No controller or static resource matches the path. Spring Boot raises
     * {@link NoResourceFoundException}; a dispatcher without a resource handler, such as a
     * standalone {@code MockMvc}, raises {@link NoHandlerFoundException}.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponseDTO> handleNoResource(Exception ex, WebRequest request) {
        return respond(ErrorCode.NOT_FOUND, null, null, null, request);
    }

    /**
     * The path exists but not for this HTTP method. Lists the methods it does accept in
     * {@code Allow}, as RFC 9110 requires.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {
        ResponseEntity<ErrorResponseDTO> response = respond(
                ErrorCode.METHOD_NOT_ALLOWED,
                "Method '%s' isn't supported at this path".formatted(ex.getMethod()),
                null,
                null,
                request);

        Set<HttpMethod> allowed = ex.getSupportedHttpMethods();
        if (allowed == null || allowed.isEmpty()) {
            return response;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setAllow(allowed);
        return ResponseEntity.status(response.getStatusCode()).headers(headers).body(response.getBody());
    }

    /**
     * Anything else is a bug. The client gets a generic message; the stack trace goes to the log.
     */
    @ApiResponse(
            responseCode = "500",
            description = SERVER_ERROR_DESCRIPTION,
            content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponseDTO.class)))
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex, WebRequest request) {
        return respond(ErrorCode.INTERNAL_ERROR, null, ex, ex, request);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Logs the error once and builds the response.
     *
     * @param message text the API wrote; {@code null} for the code's default message
     * @param cause   the underlying exception, for the log line and {@code detail}; may be {@code null}
     * @param thrown  what to print the stack trace of when the code is unexpected; may be {@code null}
     */
    private ResponseEntity<ErrorResponseDTO> respond(
            ErrorCode code, String message, Throwable cause, Throwable thrown, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(code.httpStatus());
        String path = path(request);
        String safeMessage = message != null ? message : code.defaultMessage();

        log(code, status, path, safeMessage, cause, thrown);

        ErrorResponseDTO error =
                ErrorResponseDTO.of(status.value(), status.getReasonPhrase(), safeMessage, code.name(), path);
        if (includeDetail && cause != null) {
            error = error.withDetail(describe(cause));
        }
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Bugs and upstream changes at {@code ERROR} with the stack trace; other server-side
     * failures at {@code WARN}; client errors and unplayable content, which are routine,
     * at {@code INFO}. The underlying message is logged, never sent.
     */
    private void log(
            ErrorCode code, HttpStatus status, String path, String message, Throwable cause, Throwable thrown) {
        String reason = cause != null ? describe(cause) : message;
        if (code.unexpected()) {
            logger.error("{} {} on {}: {}", status.value(), code, path, reason, thrown);
        } else if (status.is5xxServerError()) {
            logger.warn("{} {} on {}: {}", status.value(), code, path, reason);
        } else {
            logger.info("{} {} on {}: {}", status.value(), code, path, reason);
        }
    }

    /**
     * {@code "ParsingException: Could not get … <- IOException: …"}: each exception in the
     * cause chain, outermost first.
     */
    static String describe(Throwable cause) {
        StringBuilder out = new StringBuilder();
        Throwable current = cause;
        for (int depth = 0; current != null && depth < DETAIL_DEPTH; depth++) {
            if (!out.isEmpty()) {
                out.append(" <- ");
            }
            out.append(current.getClass().getSimpleName());
            if (current.getMessage() != null) {
                out.append(": ").append(current.getMessage());
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        return out.toString();
    }

    private static String path(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
