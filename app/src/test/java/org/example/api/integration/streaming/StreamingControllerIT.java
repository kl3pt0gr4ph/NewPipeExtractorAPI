package org.example.api.integration.streaming;

import org.example.api.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for StreamingController
 */
@AutoConfigureTestRestTemplate
class StreamingControllerIT extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String TEST_VIDEO_ID = "mImFz8mkaHo";

    private String url(String path) {
        return "http://localhost:%d/api/v1/streams%s?id=%s".formatted(port, path, TEST_VIDEO_ID);
    }

    // ── Accessor endpoints ───────────────────────────────────────────────

    static Stream<Arguments> accessorEndpoints() {
        return Stream.of(
                Arguments.of("", (Consumer<String>) body -> assertThat(body)
                        .contains("\"id\"", "\"name\"", "\"url\"", "\"duration\"", "\"viewCount\"")),
                Arguments.of("/audio", (Consumer<String>) body -> assertThat(body)
                        .startsWith("[").endsWith("]").contains("averageBitrate", "url")),
                Arguments.of("/video", (Consumer<String>) body -> assertThat(body)
                        .startsWith("[").endsWith("]").contains("resolution", "url")),
                Arguments.of("/video/dash", (Consumer<String>) body -> {
                    if (body != null && !body.isBlank()) {
                        assertThat(body).startsWith("http");
                    }
                }),
                Arguments.of("/thumbnails", (Consumer<String>) body -> assertThat(body)
                        .startsWith("[").endsWith("]").contains("url", "height", "width")),
                Arguments.of("/subtitles", (Consumer<String>) body -> assertThat(body)
                        .startsWith("[").endsWith("]")),
                Arguments.of("/segments", (Consumer<String>) body -> assertThat(body)
                        .startsWith("[").endsWith("]")),
                Arguments.of("/preview-frames", (Consumer<String>) body -> assertThat(body)
                        // Also previously untested happy path.
                        .startsWith("[").endsWith("]")),
                Arguments.of("/description", (Consumer<String>) body -> assertThat(body)
                        .contains("content")),
                Arguments.of("/details", (Consumer<String>) body -> assertThat(body)
                        .contains("\"videoTitle\"", "\"url\"", "\"viewCount\"", "\"channelName\"", "\"uploaderAvatars\"")),
                Arguments.of("/related", (Consumer<String>) body -> assertThat(body)
                        .startsWith("[").endsWith("]").contains("name", "url")));
    }

    @ParameterizedTest(name = "GET /api/v1/streams{0} returns a well-formed body")
    @MethodSource("accessorEndpoints")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @DisplayName("Each accessor endpoint returns 200 with the expected shape for a real video")
    void accessorEndpoint_returnsWellFormedBody(String path, Consumer<String> bodyValidator) {
        ResponseEntity<String> response = restTemplate.getForEntity(url(path), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        bodyValidator.accept(response.getBody());
    }

    // ── Missing/empty id ─────────────────────────────────────────────────

    static Stream<String> allPaths() {
        return Stream.of("", "/audio", "/video", "/video/dash", "/dash",
                "/thumbnails", "/subtitles", "/segments", "/preview-frames",
                "/description", "/details", "/related");
    }

    @ParameterizedTest(name = "{0} without id returns 400")
    @MethodSource("allPaths")
    @DisplayName("Every endpoint returns 400 when id is missing")
    void missingId_returnsBadRequest(String path) {
        String urlWithoutId = "http://localhost:%d/api/v1/streams%s".formatted(port, path);
        ResponseEntity<String> response = restTemplate.getForEntity(urlWithoutId, String.class);

        assertThat(response.getStatusCode())
                .as("Endpoint %s should reject a missing id", path)
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── DASH manifest ───────

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    @DisplayName("DASH manifest is either well-formed XML with video+audio adaptation sets, or a muxed fallback URL")
    void dashManifest_isValidXmlOrMuxedFallback() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/dash"), String.class);
        String body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).isNotNull().isNotBlank();

        boolean isDash = body.contains("<?xml") && body.contains("<MPD");
        boolean isMuxed = body.startsWith("https://") && body.contains("googlevideo.com");
        assertThat(isDash || isMuxed).as("Expected DASH manifest or muxed URL, got: %s", body).isTrue();

        if (isDash) {
            assertThat(body).contains("<BaseURL>", "mimeType=\"video/", "mimeType=\"audio/");
            assertThatCode(() -> DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(body.getBytes())))
                    .as("Manifest should be well-formed XML")
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("DASH endpoint sets an XML or text/plain content type matching its body (muxed fallback vs manifest)")
    void dashManifest_contentTypeMatchesBodyShape() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/dash"), String.class);
        String body = response.getBody();
        boolean isDash = body != null && body.contains("<?xml") && body.contains("<MPD");

        MediaType contentType = response.getHeaders().getContentType();
        assertThat(contentType).isNotNull();
        if (isDash) {
            assertThat(contentType.toString()).containsIgnoringCase("xml");
        } else {
            assertThat(contentType.toString()).containsIgnoringCase("text/plain");
        }
    }

    // ── Error path ───────────────────────────────────────────────────────

    @Test
    @DisplayName("An invalid video id returns a client or extraction error, not a 200")
    void invalidVideoId_returnsError() {
        String urlWithBadId = "http://localhost:%d/api/v1/streams?id=invalid_video_id".formatted(port);
        ResponseEntity<String> response = restTemplate.getForEntity(urlWithBadId, String.class);

        assertThat(response.getStatusCode())
                .isIn(HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ── Concurrency smoke test ───────────────────────────────────────────

    @Test
    @DisplayName("Concurrent requests for the same video are all handled successfully")
    void concurrentRequests_areAllHandledSuccessfully() throws InterruptedException {
        int threadCount = 3;
        AtomicInteger successCount = new AtomicInteger(0);
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                ResponseEntity<String> response = restTemplate.getForEntity(url(""), String.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    successCount.incrementAndGet();
                }
            });
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join(20_000);
        }

        assertThat(successCount.get()).isEqualTo(threadCount);
    }
}