package org.example.api.downloader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.OkHttpClient;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

/**
 * Tests for DownloaderImpl, using MockWebServer for hermetic HTTP.
 */
@DisplayName("DownloaderImpl Tests")
class DownloaderImplTest {

    private static final String USER_AGENT = "NewPipeExtractorAPI-test/1.0";

    private MockWebServer mockWebServer;
    private DownloaderImpl downloader;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        baseUrl = mockWebServer.url("/").toString();

        // NOTE: init() mutates a static singleton
        downloader = DownloaderImpl.init(null, USER_AGENT);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.close();
    }

    private static MockResponse okBody(String body) {
        return new MockResponse.Builder().body(body).build();
    }

    // ── Initialization ───────────────────────────────────────────────────

    @Test
    @DisplayName("init registers the singleton for null and custom builders")
    void init_registersSingleton() {
        DownloaderImpl fromNull = DownloaderImpl.init(null, USER_AGENT);
        assertNotNull(fromNull);
        assertSame(fromNull, DownloaderImpl.getInstance());

        OkHttpClient.Builder customBuilder = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS);
        DownloaderImpl fromCustom = DownloaderImpl.init(customBuilder, USER_AGENT);
        assertNotNull(fromCustom);
        assertSame(fromCustom, DownloaderImpl.getInstance());
    }

    // ── Cookies ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Cookie Management Tests")
    class CookieManagementTests {

        @Test
        @DisplayName("Cookies start null and round-trip through set/get")
        void cookies_startNullAndRoundTrip() {
            assertNull(downloader.getCookies());

            String testCookies = "session=abc123; token=xyz789";
            downloader.setCookies(testCookies);
            assertEquals(testCookies, downloader.getCookies());
        }

        @Test
        @DisplayName("stream() sends the Cookie header when cookies are set")
        void stream_sendsCookieHeader() throws Exception {
            mockWebServer.enqueue(okBody("OK"));
            downloader.setCookies("session=test123");

            downloader.stream(baseUrl + "test");

            RecordedRequest request = mockWebServer.takeRequest();
            assertEquals("session=test123", request.getHeaders().get("Cookie"));
        }

        @Test
        @DisplayName("stream() omits the Cookie header when cookies are empty")
        void stream_omitsCookieHeaderWhenEmpty() throws Exception {
            mockWebServer.enqueue(okBody("OK"));
            downloader.setCookies("");

            downloader.stream(baseUrl + "test");

            RecordedRequest request = mockWebServer.takeRequest();
            assertNull(request.getHeaders().get("Cookie"));
        }

        @Test
        @DisplayName("execute() sends the Cookie header when cookies are set")
        void execute_sendsCookieHeader() throws Exception {
            mockWebServer.enqueue(okBody("OK"));
            downloader.setCookies("session=abc123");

            downloader.execute(new Request.Builder().get(baseUrl + "api/data").build());

            RecordedRequest request = mockWebServer.takeRequest();
            assertEquals("session=abc123", request.getHeaders().get("Cookie"));
        }
    }

    // ── Content length ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Content Length Tests")
    class ContentLengthTests {

        @Test
        @DisplayName("Reads Content-Length via a HEAD request")
        void getContentLength_readsHeaderViaHead() throws Exception {
            mockWebServer.enqueue(okBody("x".repeat(12345)));

            long contentLength = downloader.getContentLength(baseUrl + "file");

            assertEquals(12345L, contentLength);
            RecordedRequest request = mockWebServer.takeRequest();
            assertEquals("HEAD", request.getMethod());
        }

        @Test
        @DisplayName("Parses values beyond Integer range")
        void getContentLength_parsesLargeValues() throws Exception {
            long largeSize = 5_000_000_000L;
            // body() sets a Content-Length header; setHeader overrides it.
            mockWebServer.enqueue(new MockResponse.Builder()
                    .body("x")
                    .setHeader("Content-Length", String.valueOf(largeSize))
                    .build());

            assertEquals(largeSize, downloader.getContentLength(baseUrl + "large"));
        }

        @Test
        @DisplayName("Throws IOException when the header is missing")
        void getContentLength_missingHeader_throwsIOException() {
            mockWebServer.enqueue(new MockResponse.Builder()
                    .chunkedBody(new Buffer().writeUtf8("test content"), 4)
                    .build());

            assertThrows(IOException.class, () -> downloader.getContentLength(baseUrl + "file"));
        }

        @Test
        @DisplayName("Throws IOException when the header is not a number")
        void getContentLength_invalidNumber_throwsIOException() {
            mockWebServer.enqueue(new MockResponse.Builder()
                    .code(200)
                    .chunkedBody(new Buffer().writeUtf8(""), 1)
                    .setHeader("Content-Length", "not-a-number")
                    .build());

            IOException exception =
                    assertThrows(IOException.class, () -> downloader.getContentLength(baseUrl + "file"));
            assertTrue(exception.getMessage().contains("Invalid content length"));
        }
    }

    // ── stream() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Stream Tests")
    class StreamTests {

        @Test
        @DisplayName("Streams body content via GET with the configured User-Agent")
        void stream_returnsContent_viaGetWithUserAgent() throws Exception {
            // Content round-trip, method, and UA in one request — these were
            // three tests each firing an identical request.
            String testContent = "Test content data";
            mockWebServer.enqueue(okBody(testContent));

            try (InputStream stream = downloader.stream(baseUrl + "data")) {
                assertNotNull(stream);
                assertEquals(testContent, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }

            RecordedRequest request = mockWebServer.takeRequest();
            assertEquals("GET", request.getMethod());
            assertEquals(USER_AGENT, request.getHeaders().get("User-Agent"));
        }

        @Test
        @DisplayName("Streams binary data intact")
        void stream_handlesBinaryData() throws Exception {
            byte[] binaryData = {0x00, 0x01, 0x02, (byte) 0xFF};
            mockWebServer.enqueue(new MockResponse.Builder()
                    .body(new Buffer().write(binaryData))
                    .build());

            try (InputStream stream = downloader.stream(baseUrl + "binary")) {
                assertArrayEquals(binaryData, stream.readAllBytes());
            }
        }

        @Test
        @DisplayName("Returns a non-null empty stream for an empty body")
        void stream_returnsEmptyStreamForEmptyBody() throws Exception {
            mockWebServer.enqueue(new MockResponse.Builder().code(204).body("").build());

            InputStream stream = downloader.stream(baseUrl + "empty");

            assertNotNull(stream);
            stream.close();
        }

        @Test
        @DisplayName("Throws IOException with reCaptcha message on 429")
        void stream_throwsOn429() {
            mockWebServer.enqueue(new MockResponse.Builder().code(429).build());

            IOException exception = assertThrows(IOException.class, () -> downloader.stream(baseUrl + "blocked"));
            assertTrue(exception.getMessage().contains("reCaptcha"));
        }
    }

    // ── execute() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Execute Request Tests")
    class ExecuteRequestTests {

        @Test
        @DisplayName("Executes GET returning code and body, with the configured User-Agent")
        void execute_getRequest_returnsCodeAndBody() throws Exception {
            String responseBody = "Success response";
            mockWebServer.enqueue(
                    new MockResponse.Builder().code(200).body(responseBody).build());

            Response response = downloader.execute(
                    new Request.Builder().get(baseUrl + "api/data").build());

            assertEquals(200, response.responseCode());
            assertEquals(responseBody, response.responseBody());

            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertEquals("GET", recordedRequest.getMethod());
            assertEquals(USER_AGENT, recordedRequest.getHeaders().get("User-Agent"));
        }

        @Test
        @DisplayName("Executes POST sending the request data")
        void execute_postRequest_sendsData() throws Exception {
            mockWebServer.enqueue(new MockResponse.Builder().code(201).build());

            byte[] postData = "key=value".getBytes(StandardCharsets.UTF_8);
            Response response = downloader.execute(
                    new Request.Builder().post(baseUrl + "api/create", postData).build());

            assertEquals(201, response.responseCode());

            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertEquals("POST", recordedRequest.getMethod());
            // RecordedRequest.body is a ByteString in mockwebserver3
            assertNotNull(recordedRequest.getBody());
            assertEquals("key=value", recordedRequest.getBody().utf8());
        }

        @Test
        @DisplayName("Executes HEAD requests")
        void execute_headRequest() throws Exception {
            mockWebServer.enqueue(new MockResponse.Builder()
                    .code(200)
                    .setHeader("Content-Length", "1234")
                    .build());

            Response response = downloader.execute(
                    new Request.Builder().head(baseUrl + "api/check").build());

            assertEquals(200, response.responseCode());
            assertEquals("HEAD", mockWebServer.takeRequest().getMethod());
        }

        @Test
        @DisplayName("Forwards custom headers, including multi-valued ones")
        void execute_forwardsCustomHeaders() throws Exception {
            mockWebServer.enqueue(okBody("OK"));

            Map<String, List<String>> headers = new HashMap<>();
            headers.put("X-Custom-Header", List.of("CustomValue"));
            headers.put("Accept", List.of("application/json", "text/html"));

            downloader.execute(new Request.Builder()
                    .get(baseUrl + "api/data")
                    .headers(headers)
                    .build());

            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertEquals("CustomValue", recordedRequest.getHeaders().get("X-Custom-Header"));
            List<String> acceptHeaders = recordedRequest.getHeaders().values("Accept");
            assertTrue(acceptHeaders.contains("application/json"));
            assertTrue(acceptHeaders.contains("text/html"));
        }

        @Test
        @DisplayName("Lets a request's own User-Agent replace the configured one")
        void execute_requestUserAgentReplacesDefault() throws Exception {
            // NewPipe sets its own User-Agent for the mobile InnerTube clients
            mockWebServer.enqueue(okBody("OK"));
            String clientUserAgent = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)";

            downloader.execute(new Request.Builder()
                    .get(baseUrl + "api/data")
                    .headers(Map.of("User-Agent", List.of(clientUserAgent)))
                    .build());

            assertEquals(
                    List.of(clientUserAgent),
                    mockWebServer.takeRequest().getHeaders().values("User-Agent"));
        }

        @ParameterizedTest(name = "Passes through HTTP {0} with its body")
        @CsvSource({"404, Not Found", "500, Internal Server Error"})
        @DisplayName("Passes error statuses and bodies through unchanged")
        void execute_passesThroughErrorStatuses(int status, String body) throws Exception {
            mockWebServer.enqueue(
                    new MockResponse.Builder().code(status).body(body).build());

            Response response = downloader.execute(
                    new Request.Builder().get(baseUrl + "api/err").build());

            assertEquals(status, response.responseCode());
            assertEquals(body, response.responseBody());
        }

        @Test
        @DisplayName("Returns an empty (non-null) body for 204 No Content")
        void execute_returnsEmptyBodyFor204() throws Exception {
            mockWebServer.enqueue(new MockResponse.Builder().code(204).build());

            Response response = downloader.execute(
                    new Request.Builder().get(baseUrl + "api/empty").build());

            assertEquals(204, response.responseCode());
            assertNotNull(response.responseBody());
        }

        @Test
        @DisplayName("Captures response headers")
        void execute_capturesResponseHeaders() throws Exception {
            mockWebServer.enqueue(new MockResponse.Builder()
                    .setHeader("Content-Type", "application/json")
                    .setHeader("X-Custom", "value123")
                    .body("{\"status\":\"ok\"}")
                    .build());

            Response response = downloader.execute(
                    new Request.Builder().get(baseUrl + "api/data").build());

            Map<String, List<String>> responseHeaders = response.responseHeaders();
            assertTrue(responseHeaders.containsKey("Content-Type"));
            assertTrue(responseHeaders.containsKey("X-Custom"));
        }

        @Test
        @DisplayName("Throws ReCaptchaException on 429")
        void execute_throwsReCaptchaOn429() {
            mockWebServer.enqueue(new MockResponse.Builder().code(429).build());

            ReCaptchaException exception = assertThrows(
                    ReCaptchaException.class,
                    () -> downloader.execute(
                            new Request.Builder().get(baseUrl + "api/data").build()));
            assertTrue(exception.getMessage().contains("reCaptcha"));
        }
    }

    // ── Error handling ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Wraps invalid URLs in IOException with the cause preserved")
        void execute_invalidUrl_throwsIOException() {
            Request request = new Request.Builder().get("not-a-valid-url").build();

            IOException exception = assertThrows(IOException.class, () -> downloader.execute(request));

            assertTrue(exception.getMessage().contains("Invalid URL"));
            assertTrue(exception.getCause() instanceof IllegalArgumentException);
        }

        @Test
        @DisplayName("Throws IOException when the connection is refused")
        void execute_connectionRefused_throwsIOException() throws Exception {
            mockWebServer.close(); // simulate connection refused

            Request request = new Request.Builder().get(baseUrl + "api/data").build();

            assertThrows(IOException.class, () -> downloader.execute(request));
        }
    }

    // ── Timeouts ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Timeout Tests")
    class TimeoutTests {

        @Test
        @DisplayName("Uses the builder's read timeout instead of overriding it")
        void execute_usesBuilderReadTimeout() {
            mockWebServer.enqueue(new MockResponse.Builder()
                    .headersDelay(2, TimeUnit.SECONDS)
                    .body("late")
                    .build());
            DownloaderImpl shortTimeout =
                    DownloaderImpl.init(new OkHttpClient.Builder().readTimeout(200, TimeUnit.MILLISECONDS), USER_AGENT);

            Request request = new Request.Builder().get(baseUrl + "slow").build();

            assertThrows(IOException.class, () -> shortTimeout.execute(request));
        }
    }
}
