package org.example.api.config;

import org.example.api.dto.ErrorResponseDTO;
import org.example.api.exception.ApiException;
import org.example.api.exception.ExtractionException;
import org.example.api.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for GlobalExceptionHandler.
 * Tests exception handling, error response generation, and logging.
 */
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private WebRequest mockRequest;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        mockRequest = mock(WebRequest.class);
        when(mockRequest.getDescription(false)).thenReturn("uri=/api/v1/test");
    }

    @Nested
    @DisplayName("ValidationException Handling Tests")
    class ValidationExceptionTests {

        @Test
        @DisplayName("Should handle ValidationException with all fields")
        void testHandleValidationException_AllFields() {
            // Arrange
            // Note: ValidationException prepends "Invalid " to message and uses fixed error code
            ValidationException exception = new ValidationException(
                    "Video ID must not be empty",
                    "INVALID_VIDEO_ID"
            );

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidationException(
                    exception,
                    mockRequest
            );

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

            ErrorResponseDTO error = response.getBody();
            assertNotNull(error);
            assertEquals(400, error.getStatus());
            assertEquals("Bad Request", error.getError());
            // ValidationException prepends "Invalid " to the message
            assertTrue(error.getMessage().contains("Video ID must not be empty"));
            assertEquals("VALIDATION_ERROR", error.getErrorCode());  // Fixed error code
            assertEquals("/api/v1/test", error.getPath());
            assertNotNull(error.getTimestamp());
        }

        @Test
        @DisplayName("Should handle ValidationException with minimal message")
        void testHandleValidationException_MinimalMessage() {
            // Arrange
            ValidationException exception = new ValidationException(
                    "Invalid input",
                    "VALIDATION_ERROR"
            );

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidationException(
                    exception,
                    mockRequest
            );

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            // ValidationException prepends "Invalid " to message
            assertTrue(response.getBody().getMessage().contains("Invalid input"));
        }

        @Test
        @DisplayName("Should extract path from WebRequest")
        void testHandleValidationException_PathExtraction() {
            // Arrange
            when(mockRequest.getDescription(false)).thenReturn("uri=/api/v1/streams/details");
            ValidationException exception = new ValidationException("Error", "CODE");

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidationException(
                    exception,
                    mockRequest
            );

            // Assert
            assertEquals("/api/v1/streams/details", response.getBody().getPath());
        }

        @Test
        @DisplayName("Should return 400 status code")
        void testHandleValidationException_StatusCode() {
            // Arrange
            ValidationException exception = new ValidationException("Error", "CODE");

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidationException(
                    exception,
                    mockRequest
            );

            // Assert
            assertEquals(400, response.getStatusCode().value());
            assertEquals(400, response.getBody().getStatus());
        }
    }

    @Nested
    @DisplayName("ExtractionException Handling Tests")
    class ExtractionExceptionTests {

        @Test
        @DisplayName("Should handle ExtractionException with all fields")
        void testHandleExtractionException_AllFields() {
            // Arrange
            ExtractionException exception = new ExtractionException(
                    "Failed to extract video information"
            );

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleExtractionException(
                    exception,
                    mockRequest
            );

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

            ErrorResponseDTO error = response.getBody();
            assertNotNull(error);
            assertEquals(500, error.getStatus());
            assertEquals("Internal Server Error", error.getError());
            assertEquals("Failed to extract video information", error.getMessage());
            assertEquals("EXTRACTION_ERROR", error.getErrorCode());
            assertEquals("/api/v1/test", error.getPath());
            assertNotNull(error.getTimestamp());
        }

        @Test
        @DisplayName("Should handle ExtractionException with cause")
        void testHandleExtractionException_WithCause() {
            // Arrange
            Exception cause = new RuntimeException("NewPipe error");
            ExtractionException exception = new ExtractionException(
                    "Extraction failed",
                    cause
            );

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleExtractionException(
                    exception,
                    mockRequest
            );

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Extraction failed", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should return 500 status code")
        void testHandleExtractionException_StatusCode() {
            // Arrange
            ExtractionException exception = new ExtractionException("Error");

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleExtractionException(
                    exception,
                    mockRequest
            );

            // Assert
            assertEquals(500, response.getStatusCode().value());
            assertEquals(500, response.getBody().getStatus());
        }
    }

    @Nested
    @DisplayName("ApiException Handling Tests")
    class ApiExceptionTests {

        @Test
        @DisplayName("Should handle ApiException with 404 status")
        void testHandleApiException_NotFound() {
            // Arrange
            ApiException exception = new ApiException(
                    "Video not found",
                    "VIDEO_NOT_FOUND",
                    404
            );

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleApiException(
                    exception,
                    mockRequest
            );

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

            ErrorResponseDTO error = response.getBody();
            assertNotNull(error);
            assertEquals(404, error.getStatus());
            assertEquals("Not Found", error.getError());
            assertEquals("Video not found", error.getMessage());
            assertEquals("VIDEO_NOT_FOUND", error.getErrorCode());
        }

        @Test
        @DisplayName("Should handle ApiException with 429 rate limit")
        void testHandleApiException_RateLimit() {
            // Arrange
            ApiException exception = new ApiException(
                    "Rate limit exceeded",
                    "RATE_LIMIT_EXCEEDED",
                    429
            );

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleApiException(
                    exception,
                    mockRequest
            );

            // Assert
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
            assertEquals(429, response.getBody().getStatus());
            assertEquals("Too Many Requests", response.getBody().getError());
        }

        @Test
        @DisplayName("Should handle ApiException with 503 service unavailable")
        void testHandleApiException_ServiceUnavailable() {
            // Arrange
            ApiException exception = new ApiException(
                    "Service temporarily unavailable",
                    "SERVICE_UNAVAILABLE",
                    503
            );

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleApiException(
                    exception,
                    mockRequest
            );

            // Assert
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertEquals(503, response.getBody().getStatus());
        }

        @Test
        @DisplayName("Should handle ApiException with custom status")
        void testHandleApiException_CustomStatus() {
            // Arrange
            ApiException exception = new ApiException(
                    "I'm a teapot",  // lowercase 'teapot' to match actual output
                    "TEAPOT",
                    418
            );

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleApiException(
                    exception,
                    mockRequest
            );

            // Assert
            assertEquals(418, response.getStatusCode().value());
            // Note: Spring's HttpStatus enum has "I'm a teapot" with lowercase 't'
            assertEquals("I'm a teapot", response.getBody().getError());
        }
    }

    @Nested
    @DisplayName("MissingServletRequestParameterException Handling Tests")
    class MissingParameterTests {

        @Test
        @DisplayName("Should handle missing required parameter")
        void testHandleMissingParameter_Required() {
            // Arrange
            MissingServletRequestParameterException exception =
                    new MissingServletRequestParameterException("videoId", "String");

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleMissingParameter(
                    exception,
                    mockRequest
            );

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

            ErrorResponseDTO error = response.getBody();
            assertNotNull(error);
            assertEquals(400, error.getStatus());
            assertEquals("Bad Request", error.getError());
            assertEquals("Required parameter 'videoId' is missing", error.getMessage());
            assertEquals("MISSING_PARAMETER", error.getErrorCode());
        }

        @Test
        @DisplayName("Should handle missing parameter with different name")
        void testHandleMissingParameter_DifferentName() {
            // Arrange
            MissingServletRequestParameterException exception =
                    new MissingServletRequestParameterException("query", "String");

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleMissingParameter(
                    exception,
                    mockRequest
            );

            // Assert
            assertEquals("Required parameter 'query' is missing", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should return 400 status for missing parameter")
        void testHandleMissingParameter_StatusCode() {
            // Arrange
            MissingServletRequestParameterException exception =
                    new MissingServletRequestParameterException("id", "String");

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleMissingParameter(
                    exception,
                    mockRequest
            );

            // Assert
            assertEquals(400, response.getStatusCode().value());
        }
    }

    @Nested
    @DisplayName("Generic Exception Handling Tests")
    class GenericExceptionTests {

        @Test
        @DisplayName("Should handle generic RuntimeException")
        void testHandleGenericException_RuntimeException() {
            // Arrange
            RuntimeException exception = new RuntimeException("Unexpected error occurred");

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleGenericException(
                    exception,
                    mockRequest
            );

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

            ErrorResponseDTO error = response.getBody();
            assertNotNull(error);
            assertEquals(500, error.getStatus());
            assertEquals("Internal Server Error", error.getError());
            assertEquals("An unexpected error occurred", error.getMessage());
            assertEquals("INTERNAL_ERROR", error.getErrorCode());
        }

        @Test
        @DisplayName("Should handle NullPointerException")
        void testHandleGenericException_NullPointerException() {
            // Arrange
            NullPointerException exception = new NullPointerException("Null value encountered");

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleGenericException(
                    exception,
                    mockRequest
            );

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("An unexpected error occurred", response.getBody().getMessage());
            assertEquals("INTERNAL_ERROR", response.getBody().getErrorCode());
        }

        @Test
        @DisplayName("Should handle IllegalArgumentException")
        void testHandleGenericException_IllegalArgumentException() {
            // Arrange
            IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleGenericException(
                    exception,
                    mockRequest
            );

            // Assert
            assertEquals(500, response.getStatusCode().value());
            assertEquals("INTERNAL_ERROR", response.getBody().getErrorCode());
        }

        @Test
        @DisplayName("Should not expose internal error details")
        void testHandleGenericException_NoDetailsExposed() {
            // Arrange
            Exception exception = new Exception("Internal database error: connection failed");

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleGenericException(
                    exception,
                    mockRequest
            );

            // Assert
            // Should use generic message, not expose internal details
            assertEquals("An unexpected error occurred", response.getBody().getMessage());
            assertFalse(response.getBody().getMessage().contains("database"));
            assertFalse(response.getBody().getMessage().contains("connection"));
        }
    }

    @Nested
    @DisplayName("Error Response Structure Tests")
    class ErrorResponseStructureTests {

        @Test
        @DisplayName("Should include timestamp in all error responses")
        void testErrorResponse_HasTimestamp() {
            // Arrange
            ValidationException exception = new ValidationException("Error", "CODE");

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidationException(
                    exception,
                    mockRequest
            );

            // Assert
            assertNotNull(response.getBody().getTimestamp());
        }

        @Test
        @DisplayName("Should include path in all error responses")
        void testErrorResponse_HasPath() {
            // Arrange
            ValidationException exception = new ValidationException("Error", "CODE");

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidationException(
                    exception,
                    mockRequest
            );

            // Assert
            assertNotNull(response.getBody().getPath());
            assertTrue(response.getBody().getPath().startsWith("/"));
        }

        @Test
        @DisplayName("Should include error code in all error responses")
        void testErrorResponse_HasErrorCode() {
            // Arrange
            // ExtractionException has fixed error code
            ExtractionException exception = new ExtractionException("Error");

            // Act
            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleExtractionException(
                    exception,
                    mockRequest
            );

            // Assert
            assertNotNull(response.getBody().getErrorCode());
            assertEquals("EXTRACTION_ERROR", response.getBody().getErrorCode());  // Fixed code
        }

        @Test
        @DisplayName("Should have consistent response structure")
        void testErrorResponse_ConsistentStructure() {
            // Arrange
            ValidationException validationEx = new ValidationException("Validation failed", "VAL_ERR");
            ExtractionException extractionEx = new ExtractionException("Extraction failed");

            // Act
            ResponseEntity<ErrorResponseDTO> response1 = exceptionHandler.handleValidationException(validationEx, mockRequest);
            ResponseEntity<ErrorResponseDTO> response2 = exceptionHandler.handleExtractionException(extractionEx, mockRequest);

            // Assert - Both should have same fields populated
            assertNotNull(response1.getBody().getTimestamp());
            assertNotNull(response2.getBody().getTimestamp());
            assertNotNull(response1.getBody().getStatus());
            assertNotNull(response2.getBody().getStatus());
            assertNotNull(response1.getBody().getError());
            assertNotNull(response2.getBody().getError());
            assertNotNull(response1.getBody().getMessage());
            assertNotNull(response2.getBody().getMessage());
            assertNotNull(response1.getBody().getErrorCode());
            assertNotNull(response2.getBody().getErrorCode());
            assertNotNull(response1.getBody().getPath());
            assertNotNull(response2.getBody().getPath());
        }
    }

    @Nested
    @DisplayName("Path Extraction Tests")
    class PathExtractionTests {

        @Test
        @DisplayName("Should extract simple path")
        void testPathExtraction_SimplePath() {
            when(mockRequest.getDescription(false)).thenReturn("uri=/api/v1/test");
            ValidationException exception = new ValidationException("Error", "CODE");

            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidationException(
                    exception,
                    mockRequest
            );

            assertEquals("/api/v1/test", response.getBody().getPath());
        }

        @Test
        @DisplayName("Should extract path with query parameters")
        void testPathExtraction_WithQueryParams() {
            when(mockRequest.getDescription(false)).thenReturn("uri=/api/v1/search?q=test&page=2");
            ValidationException exception = new ValidationException("Error", "CODE");

            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidationException(
                    exception,
                    mockRequest
            );

            assertEquals("/api/v1/search?q=test&page=2", response.getBody().getPath());
        }

        @Test
        @DisplayName("Should extract long path")
        void testPathExtraction_LongPath() {
            when(mockRequest.getDescription(false)).thenReturn("uri=/api/v1/streams/details/video/dQw4w9WgXcQ");
            ValidationException exception = new ValidationException("Error", "CODE");

            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidationException(
                    exception,
                    mockRequest
            );

            assertEquals("/api/v1/streams/details/video/dQw4w9WgXcQ", response.getBody().getPath());
        }
    }

    @Nested
    @DisplayName("HTTP Status Mapping Tests")
    class HttpStatusMappingTests {

        @Test
        @DisplayName("Should map ValidationException to 400")
        void testStatusMapping_ValidationTo400() {
            ValidationException exception = new ValidationException("Error", "CODE");

            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidationException(
                    exception,
                    mockRequest
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(400, response.getBody().getStatus());
            assertEquals("Bad Request", response.getBody().getError());
        }

        @Test
        @DisplayName("Should map ExtractionException to 500")
        void testStatusMapping_ExtractionTo500() {
            ExtractionException exception = new ExtractionException("Error");

            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleExtractionException(
                    exception,
                    mockRequest
            );

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals(500, response.getBody().getStatus());
            assertEquals("Internal Server Error", response.getBody().getError());
        }

        @Test
        @DisplayName("Should map MissingParameter to 400")
        void testStatusMapping_MissingParamTo400() {
            MissingServletRequestParameterException exception =
                    new MissingServletRequestParameterException("id", "String");

            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleMissingParameter(
                    exception,
                    mockRequest
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(400, response.getBody().getStatus());
        }

        @Test
        @DisplayName("Should map generic Exception to 500")
        void testStatusMapping_GenericTo500() {
            Exception exception = new Exception("Error");

            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleGenericException(
                    exception,
                    mockRequest
            );

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals(500, response.getBody().getStatus());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null exception message")
        void testEdgeCase_NullMessage() {
            ValidationException exception = new ValidationException(null, "CODE");

            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidationException(
                    exception,
                    mockRequest
            );

            assertNotNull(response.getBody().getMessage());
            assertTrue(response.getBody().getMessage().contains("Invalid"));
        }

        @Test
        @DisplayName("Should handle empty error code")
        void testEdgeCase_EmptyErrorCode() {
            ValidationException exception = new ValidationException("Error", "");

            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidationException(
                    exception,
                    mockRequest
            );

            assertEquals("VALIDATION_ERROR", response.getBody().getErrorCode());
        }

        @Test
        @DisplayName("Should handle very long error message")
        void testEdgeCase_LongMessage() {
            String longMessage = "A".repeat(1000);
            ValidationException exception = new ValidationException(longMessage, "CODE");

            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidationException(
                    exception,
                    mockRequest
            );

            assertTrue(response.getBody().getMessage().length() >= 1000);
            assertTrue(response.getBody().getMessage().contains("AAA"));  // Contains the A's
        }

        @Test
        @DisplayName("Should handle special characters in error message")
        void testEdgeCase_SpecialCharacters() {
            String specialMessage = "Error: \"Invalid\" <parameter> & value='test'";
            ValidationException exception = new ValidationException(specialMessage, "CODE");

            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidationException(
                    exception,
                    mockRequest
            );

            assertTrue(response.getBody().getMessage().contains(specialMessage));
        }
    }

    @Nested
    @DisplayName("Multiple Exception Types Tests")
    class MultipleExceptionTypesTests {

        @Test
        @DisplayName("Should handle different exception types consistently")
        void testMultipleTypes_Consistency() {
            ValidationException validationEx = new ValidationException("Validation error", "VAL_ERR");
            ExtractionException extractionEx = new ExtractionException("Extraction error");
            ApiException apiEx = new ApiException("Not found", "NOT_FOUND", 404);

            ResponseEntity<ErrorResponseDTO> response1 = exceptionHandler.handleValidationException(validationEx, mockRequest);
            ResponseEntity<ErrorResponseDTO> response2 = exceptionHandler.handleExtractionException(extractionEx, mockRequest);
            ResponseEntity<ErrorResponseDTO> response3 = exceptionHandler.handleApiException(apiEx, mockRequest);

            assertNotNull(response1.getBody());
            assertNotNull(response2.getBody());
            assertNotNull(response3.getBody());

            assertNotNull(response1.getBody().getErrorCode());
            assertNotNull(response2.getBody().getErrorCode());
            assertNotNull(response3.getBody().getErrorCode());
        }
    }
}