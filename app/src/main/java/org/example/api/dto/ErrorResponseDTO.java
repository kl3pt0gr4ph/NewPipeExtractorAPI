package org.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Standardised error response DTO.
 * Provides consistent error structure across all API endpoints.
 */
@Schema(name = "ErrorResponse", description = "Body of every error response from `/api/v1`")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {

    @Schema(description = "When the error occurred, ISO-8601 UTC", example = "2026-10-29T10:30:00.123Z")
    @JsonProperty("timestamp")
    private String timestamp;

    @Schema(description = "HTTP status code, repeated from the response", example = "400")
    @JsonProperty("status")
    private int status;

    @Schema(description = "HTTP reason phrase", example = "Bad Request")
    @JsonProperty("error")
    private String error;

    @Schema(description = "Human-readable detail. Not stable; don't parse it.",
            example = "Required parameter 'id' is missing")
    @JsonProperty("message")
    private String message;

    @Schema(description = "Machine-readable cause. Branch on this rather than on `message`.",
            allowableValues = {"MISSING_PARAMETER", "VALIDATION_ERROR", "EXTRACTION_ERROR",
                    "NO_STREAMS_AVAILABLE", "INTERNAL_ERROR"},
            example = "MISSING_PARAMETER")
    @JsonProperty("errorCode")
    private String errorCode;

    @Schema(description = "Request path that failed", example = "/api/v1/streams/details")
    @JsonProperty("path")
    private String path;

    public ErrorResponseDTO() {
        this.timestamp = Instant.now().toString();
    }

    public ErrorResponseDTO(int status, String error, String message, String errorCode) {
        this();
        this.status = status;
        this.error = error;
        this.message = message;
        this.errorCode = errorCode;
    }

    // Getters and Setters
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}