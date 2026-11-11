package org.example.api.config;

import org.example.api.dto.ErrorResponseDTO;
import org.example.api.exception.ApiException;
import org.example.api.exception.ErrorCode;
import org.example.api.exception.ExtractionException;
import org.example.api.exception.NewPipeExceptionMapper;
import org.example.api.exception.NoStreamsAvailableException;
import org.example.api.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.schabi.newpipe.extractor.exceptions.AccountTerminatedException;
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.GeographicRestrictionException;
import org.schabi.newpipe.extractor.exceptions.PaidContentException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.exceptions.PrivateContentException;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.exceptions.SignInConfirmNotBotException;
import org.schabi.newpipe.extractor.exceptions.SoundCloudGoPlusContentException;
import org.schabi.newpipe.extractor.exceptions.UnsupportedContentInCountryException;
import org.schabi.newpipe.extractor.exceptions.YoutubeMusicPremiumContentException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    /** Text no response may contain unless detail is turned on. */
    private static final String RAW = "raw NewPipe wording 7f3c";

    private GlobalExceptionHandler handler;
    private WebRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/v1/test");
    }

    /** What a service does with a NewPipe failure, then what the advice does with the result. */
    private ResponseEntity<ErrorResponseDTO> handleThrown(GlobalExceptionHandler h, Throwable thrown) {
        RuntimeException translated = NewPipeExceptionMapper.translate(thrown);
        if (translated instanceof ApiException api) {
            return h.handleApiException(api, request);
        }
        return h.handleGenericException(translated, request);
    }

    private ResponseEntity<ErrorResponseDTO> handleThrown(Throwable thrown) {
        return handleThrown(handler, thrown);
    }

    private static void assertError(ResponseEntity<ErrorResponseDTO> response, int status, String errorCode) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        ErrorResponseDTO body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(status);
        assertThat(body.error()).isEqualTo(HttpStatus.valueOf(status).getReasonPhrase());
        assertThat(body.errorCode()).isEqualTo(errorCode);
        assertThat(body.path()).isEqualTo("/api/v1/test");
        assertThat(body.timestamp()).isNotBlank();
    }

    // ── NewPipe exception mapping ───────────────────────────────────────────

    static Stream<Arguments> newPipeExceptions() {
        return Stream.of(
                Arguments.of(new ContentNotAvailableException(RAW), 404, "CONTENT_UNAVAILABLE"),
                Arguments.of(new PrivateContentException(RAW), 403, "CONTENT_PRIVATE"),
                Arguments.of(new AgeRestrictedContentException(RAW), 403, "AGE_RESTRICTED"),
                Arguments.of(new GeographicRestrictionException(RAW), 451, "GEO_RESTRICTED"),
                Arguments.of(new UnsupportedContentInCountryException(RAW), 451, "GEO_RESTRICTED"),
                Arguments.of(new PaidContentException(RAW), 403, "PAID_CONTENT"),
                Arguments.of(new YoutubeMusicPremiumContentException(), 403, "PAID_CONTENT"),
                Arguments.of(new SoundCloudGoPlusContentException(), 403, "PAID_CONTENT"),
                Arguments.of(new AccountTerminatedException(RAW), 410, "ACCOUNT_TERMINATED"),
                Arguments.of(new SignInConfirmNotBotException(RAW), 503, "UPSTREAM_BLOCKED"),
                Arguments.of(new ReCaptchaException(RAW, "https://www.youtube.com/watch?v=x"), 503, "UPSTREAM_BLOCKED"),
                Arguments.of(new ParsingException(RAW), 502, "UPSTREAM_CHANGED"),
                Arguments.of(new IOException(RAW), 502, "UPSTREAM_UNREACHABLE"),
                Arguments.of(new SocketTimeoutException(RAW), 502, "UPSTREAM_UNREACHABLE"),
                Arguments.of(new org.schabi.newpipe.extractor.exceptions.ExtractionException(RAW),
                        500, "EXTRACTION_ERROR"));
    }

    @ParameterizedTest(name = "{0} -> {1} {2}")
    @MethodSource("newPipeExceptions")
    @DisplayName("Each NewPipe exception gets its own status and errorCode, with the API's message")
    void mapsNewPipeException(Throwable thrown, int status, String errorCode) {
        ResponseEntity<ErrorResponseDTO> response = handleThrown(thrown);

        assertError(response, status, errorCode);
        ErrorResponseDTO body = response.getBody();
        assertThat(body.message()).isEqualTo(ErrorCode.valueOf(errorCode).defaultMessage());
        assertThat(body.message()).doesNotContain(RAW);
        assertThat(body.detail()).isNull();
    }

    @ParameterizedTest(name = "{0} -> {1} {2}")
    @MethodSource("newPipeExceptions")
    @DisplayName("The mapping also applies when the NewPipe exception is wrapped")
    void mapsWrappedNewPipeException(Throwable thrown, int status, String errorCode) {
        assertError(handleThrown(new CompletionException(thrown)), status, errorCode);
    }

    @Test
    @DisplayName("A restriction wins over the generic ParsingException wrapped around it")
    void mostSpecificExceptionInChainWins() {
        Throwable thrown = new ParsingException("Could not get stream info",
                new AgeRestrictedContentException(RAW));

        assertError(handleThrown(thrown), 403, "AGE_RESTRICTED");
    }

    @Test
    @DisplayName("Of two generic exceptions in a chain, the outermost decides")
    void outermostGenericExceptionWins() {
        Throwable thrown = new ParsingException(RAW, new IOException("connection reset"));

        assertError(handleThrown(thrown), 502, "UPSTREAM_CHANGED");
    }

    @Test
    @DisplayName("The translated exception keeps the original as its cause")
    void translationKeepsCause() {
        PrivateContentException original = new PrivateContentException(RAW);

        RuntimeException translated = NewPipeExceptionMapper.translate(original);

        assertThat(translated).isInstanceOf(ExtractionException.class).hasCause(original);
    }

    @Test
    @DisplayName("An ApiException is passed through translation unchanged")
    void apiExceptionPassesThrough() {
        ValidationException own = new ValidationException("tab", "this channel has no 'x' tab");

        assertThat(NewPipeExceptionMapper.translate(own)).isSameAs(own);
        assertThat(NewPipeExceptionMapper.translate(new CompletionException(own))).isSameAs(own);
    }

    @Test
    @DisplayName("An unrelated RuntimeException passes through and becomes 500 INTERNAL_ERROR")
    void unrelatedRuntimeExceptionIsInternalError() {
        NullPointerException bug = new NullPointerException(RAW);

        assertThat(NewPipeExceptionMapper.translate(bug)).isSameAs(bug);

        ResponseEntity<ErrorResponseDTO> response = handleThrown(bug);
        assertError(response, 500, "INTERNAL_ERROR");
        assertThat(response.getBody().message()).doesNotContain(RAW);
    }

    @Test
    @DisplayName("An unrelated checked exception becomes 500 INTERNAL_ERROR")
    void unrelatedCheckedExceptionIsInternalError() {
        Exception checked = new Exception(RAW);

        RuntimeException translated = NewPipeExceptionMapper.translate(checked);

        assertThat(translated).isInstanceOf(ApiException.class).hasCause(checked);
        assertError(handleThrown(checked), 500, "INTERNAL_ERROR");
    }

    // ── ApiException ────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0}")
    @EnumSource(ErrorCode.class)
    @DisplayName("Every ErrorCode is returned with its own status, reason phrase and default message")
    void everyCodeRoundTrips(ErrorCode code) {
        ResponseEntity<ErrorResponseDTO> response = handler.handleApiException(new ApiException(code), request);

        assertError(response, code.httpStatus(), code.name());
        assertThat(response.getBody().message()).isEqualTo(code.defaultMessage());
    }

    @Test
    @DisplayName("A message the API wrote is passed through as message")
    void apiWrittenMessageIsKept() {
        ResponseEntity<ErrorResponseDTO> response = handler.handleApiException(
                new ValidationException("URL", "must start with http:// or https://"), request);

        assertError(response, 400, "VALIDATION_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Invalid URL: must start with http:// or https://");
    }

    @Test
    @DisplayName("NoStreamsAvailableException is 503 NO_STREAMS_AVAILABLE")
    void noStreamsAvailable() {
        ResponseEntity<ErrorResponseDTO> response = handler.handleApiException(
                new NoStreamsAvailableException("No streams available"), request);

        assertError(response, 503, "NO_STREAMS_AVAILABLE");
        assertThat(response.getBody().message()).isEqualTo("No streams available");
    }

    // ── detail ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("detail")
    class DetailTests {

        private final GlobalExceptionHandler withDetail =
                new GlobalExceptionHandler(new ApiErrorProperties(true));

        @Test
        @DisplayName("Holds the underlying exception chain when include-detail is on")
        void includesCauseChainWhenEnabled() {
            Throwable thrown = new ParsingException("Could not get stream info",
                    new PrivateContentException(RAW));

            ErrorResponseDTO body = handleThrown(withDetail, thrown).getBody();

            assertThat(body.errorCode()).isEqualTo("CONTENT_PRIVATE");
            assertThat(body.message()).doesNotContain(RAW);
            assertThat(body.detail())
                    .isEqualTo("ParsingException: Could not get stream info <- PrivateContentException: " + RAW);
        }

        @Test
        @DisplayName("Holds the exception for unexpected errors when include-detail is on")
        void includesUnexpectedExceptionWhenEnabled() {
            ErrorResponseDTO body = withDetail.handleGenericException(
                    new IllegalStateException(RAW), request).getBody();

            assertThat(body.message()).isEqualTo("An unexpected error occurred");
            assertThat(body.detail()).isEqualTo("IllegalStateException: " + RAW);
        }

        @Test
        @DisplayName("Is absent when include-detail is off (the default)")
        void absentByDefault() {
            ErrorResponseDTO body = handleThrown(new PrivateContentException(RAW)).getBody();

            assertThat(body.detail()).isNull();
        }

        @Test
        @DisplayName("Is absent when the error has no underlying exception")
        void absentWithoutCause() {
            ErrorResponseDTO body = withDetail.handleApiException(
                    new ValidationException("id", "must not be empty"), request).getBody();

            assertThat(body.detail()).isNull();
        }
    }

    // ── Spring MVC exceptions ───────────────────────────────────────────────

    @Nested
    @DisplayName("Spring MVC exceptions")
    class SpringExceptionTests {

        @Test
        @DisplayName("A missing parameter is 400 MISSING_PARAMETER naming the parameter")
        void missingParameter() {
            ResponseEntity<ErrorResponseDTO> response = handler.handleMissingParameter(
                    new MissingServletRequestParameterException("id", "String"), request);

            assertError(response, 400, "MISSING_PARAMETER");
            assertThat(response.getBody().message()).isEqualTo("Required parameter 'id' is missing");
        }

        @Test
        @DisplayName("A parameter of the wrong type is 400 VALIDATION_ERROR naming the parameter and type")
        void typeMismatch() {
            MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                    "abc", Integer.class, "count", mock(MethodParameter.class),
                    new NumberFormatException("For input string: \"abc\""));

            ResponseEntity<ErrorResponseDTO> response = handler.handleTypeMismatch(ex, request);

            assertError(response, 400, "VALIDATION_ERROR");
            assertThat(response.getBody().message()).isEqualTo("Parameter 'count' must be of type Integer");
        }

        @Test
        @DisplayName("An unknown path is 404 NOT_FOUND, not 500 (Spring Boot's NoResourceFoundException)")
        void noResourceFound() {
            ResponseEntity<ErrorResponseDTO> response = handler.handleNoResource(
                    new NoResourceFoundException(HttpMethod.GET, "/api/v1/nope", "api/v1/nope"), request);

            assertError(response, 404, "NOT_FOUND");
        }

        @Test
        @DisplayName("An unknown path is 404 NOT_FOUND (NoHandlerFoundException)")
        void noHandlerFound() {
            ResponseEntity<ErrorResponseDTO> response = handler.handleNoResource(
                    new NoHandlerFoundException("GET", "/api/v1/nope", new HttpHeaders()), request);

            assertError(response, 404, "NOT_FOUND");
        }

        @Test
        @DisplayName("An unsupported method is 405 METHOD_NOT_ALLOWED with an Allow header")
        void methodNotSupported() {
            ResponseEntity<ErrorResponseDTO> response = handler.handleMethodNotSupported(
                    new HttpRequestMethodNotSupportedException("POST", List.of("GET")), request);

            assertError(response, 405, "METHOD_NOT_ALLOWED");
            assertThat(response.getBody().message()).isEqualTo("Method 'POST' isn't supported at this path");
            assertThat(response.getHeaders().getAllow()).containsExactly(HttpMethod.GET);
        }
    }

    // ── Catch-all ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Catch-all")
    class CatchAllTests {

        @Test
        @DisplayName("Anything unexpected is 500 INTERNAL_ERROR with a generic message")
        void unexpectedIsInternalError() {
            ResponseEntity<ErrorResponseDTO> response = handler.handleGenericException(
                    new IllegalStateException("Internal database error: connection failed"), request);

            assertError(response, 500, "INTERNAL_ERROR");
            assertThat(response.getBody().message())
                    .isEqualTo("An unexpected error occurred")
                    .doesNotContain("database", "connection");
        }

        @Test
        @DisplayName("Keeps the query string in path")
        void pathKeepsQueryString() {
            when(request.getDescription(false)).thenReturn("uri=/api/v1/search?q=test&page=2");

            ResponseEntity<ErrorResponseDTO> response = handler.handleGenericException(
                    new RuntimeException(), request);

            assertThat(response.getBody().path()).isEqualTo("/api/v1/search?q=test&page=2");
        }
    }
}