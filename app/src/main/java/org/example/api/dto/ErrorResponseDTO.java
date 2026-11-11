package org.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Standardised error response DTO.
 * Provides consistent error structure across all API endpoints.
 */
@Schema(name = "ErrorResponse", description = "Body of every error response from `/api/v1`")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDTO(
        @Schema(description = "When the error occurred, ISO-8601 UTC", example = "2026-10-29T10:30:00.123Z")
        String timestamp,

        @Schema(description = "HTTP status code, repeated from the response", example = "400")
        int status,

        @Schema(description = "HTTP reason phrase", example = "Bad Request")
        String error,

        @Schema(
                description = "What went wrong, written by the API and safe to show to end users. "
                        + "Wording may change between releases, so don't parse it: branch on `errorCode`.",
                example = "This content is private.")
        String message,

        @Schema(
                description = """
                Machine-readable cause. Branch on this rather than on `message` or the status.

                | `errorCode` | Status | Meaning |
                | --- | --- | --- |
                | `MISSING_PARAMETER` | 400 | A required parameter is absent |
                | `VALIDATION_ERROR` | 400 | A parameter is empty, malformed, of the wrong type, or names something the target doesn't have |
                | `NOT_FOUND` | 404 | No such endpoint or kiosk |
                | `METHOD_NOT_ALLOWED` | 405 | The path doesn't accept this HTTP method |
                | `CONTENT_UNAVAILABLE` | 404 | The content was removed or never existed |
                | `CONTENT_PRIVATE` | 403 | The content is private |
                | `AGE_RESTRICTED` | 403 | The content needs a signed-in account |
                | `PAID_CONTENT` | 403 | Members-only, paid, or YouTube Music Premium content |
                | `GEO_RESTRICTED` | 451 | Not available in the country the server is in |
                | `ACCOUNT_TERMINATED` | 410 | The account the content belonged to was terminated |
                | `UPSTREAM_BLOCKED` | 503 | YouTube is rate-limiting or bot-checking the server |
                | `UPSTREAM_CHANGED` | 502 | YouTube's response couldn't be parsed; NewPipeExtractor may need an update |
                | `UPSTREAM_UNREACHABLE` | 502 | YouTube couldn't be reached |
                | `NO_STREAMS_AVAILABLE` | 503 | `/streams/dash` found nothing playable |
                | `EXTRACTION_ERROR` | 500 | Extraction failed for a reason with no more specific code |
                | `INTERNAL_ERROR` | 500 | A bug in the API |

                Adding a code is a minor change; renaming or removing one is a breaking change.""",
                allowableValues = {
                    "MISSING_PARAMETER",
                    "VALIDATION_ERROR",
                    "NOT_FOUND",
                    "METHOD_NOT_ALLOWED",
                    "CONTENT_UNAVAILABLE",
                    "CONTENT_PRIVATE",
                    "AGE_RESTRICTED",
                    "PAID_CONTENT",
                    "GEO_RESTRICTED",
                    "ACCOUNT_TERMINATED",
                    "UPSTREAM_BLOCKED",
                    "UPSTREAM_CHANGED",
                    "UPSTREAM_UNREACHABLE",
                    "NO_STREAMS_AVAILABLE",
                    "EXTRACTION_ERROR",
                    "INTERNAL_ERROR"
                },
                example = "CONTENT_PRIVATE")
        String errorCode,

        @Schema(description = "Request path that failed", example = "/api/v1/streams/details")
        String path,

        @Schema(
                description = "The underlying exception chain, for debugging. Only present when the "
                        + "server runs with `OPENTUBE_ERRORS_INCLUDEDETAIL=true`; never show it to users.",
                example = "PrivateContentException: This video is private")
        String detail) {

    /**
     * Creates an error stamped with the current time.
     */
    public static ErrorResponseDTO of(int status, String error, String message, String errorCode, String path) {
        return new ErrorResponseDTO(Instant.now().toString(), status, error, message, errorCode, path, null);
    }

    /**
     * Copy with the debug detail attached; used only when detail output is enabled.
     */
    public ErrorResponseDTO withDetail(String detail) {
        return new ErrorResponseDTO(timestamp, status, error, message, errorCode, path, detail);
    }
}
