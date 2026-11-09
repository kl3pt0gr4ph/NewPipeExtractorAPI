package org.example.api.service;

import org.example.api.cache.StreamInfoCache;
import org.example.api.cache.StreamInfoCacheProperties;
import org.example.api.config.ExtractionProperties;
import org.example.api.exception.ApiException;
import org.example.api.exception.ErrorCode;
import org.example.api.exception.ExtractionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.exceptions.PrivateContentException;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.Description;
import org.schabi.newpipe.extractor.stream.Frameset;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamSegment;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Unit tests for VideoStreamingService.
 */
@DisplayName("VideoStreamingService Tests")
class VideoStreamingServiceTest {

    private static final String TEST_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";

    private VideoStreamingService service;

    @BeforeEach
    void setUp() {
        service = serviceWithAttempts(2);
    }

    private static VideoStreamingService serviceWithAttempts(int attempts) {
        return new VideoStreamingService(
                new StreamInfoCache(cacheProperties()), new ExtractionProperties(attempts));
    }

    private static StreamInfoCacheProperties cacheProperties() {
        return new StreamInfoCacheProperties(
                300,
                Duration.ofMinutes(10),
                Duration.ofMinutes(5),
                Duration.ofSeconds(30),
                Duration.ofHours(6));
    }

    /** A call to one of the service's accessor methods. */
    @FunctionalInterface
    interface ServiceCall {
        Object apply(VideoStreamingService service) throws ExtractionException;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * StreamInfo mock with explicit stream lists and duration — the three
     * inputs to the retry decision.
     */
    private static StreamInfo streamInfo(List<VideoStream> videoOnly,
                                         List<AudioStream> audio,
                                         long duration) {
        StreamInfo info = mock(StreamInfo.class);
        when(info.getVideoOnlyStreams()).thenReturn(videoOnly);
        when(info.getAudioStreams()).thenReturn(audio);
        when(info.getVideoStreams()).thenReturn(Collections.emptyList());
        when(info.getDuration()).thenReturn(duration);
        return info;
    }

    private static StreamInfo completeStreamInfo() {
        return streamInfo(List.of(mock(VideoStream.class)), List.of(mock(AudioStream.class)), 120L);
    }

    /** No adaptive streams but a valid duration — the retry trigger. */
    private static StreamInfo incompleteStreamInfo() {
        return streamInfo(Collections.emptyList(), Collections.emptyList(), 120L);
    }

    // ── Retry logic (the actual business logic in this class) ────────────

    @Nested
    @DisplayName("getStreamInfo retry logic")
    class RetryLogicTests {

        @Test
        @DisplayName("Returns first result when adaptive streams are present")
        void returnsFirstResult_whenAdaptiveStreamsPresent() throws Exception {
            StreamInfo complete = completeStreamInfo();

            try (MockedStatic<StreamInfo> extractor = mockStatic(StreamInfo.class)) {
                extractor.when(() -> StreamInfo.getInfo(TEST_URL)).thenReturn(complete);

                StreamInfo result = service.getStreamInfo(TEST_URL);

                assertThat(result).isSameAs(complete);
                extractor.verify(() -> StreamInfo.getInfo(TEST_URL), times(1));
            }
        }

        @Test
        @DisplayName("Retries once when response is incomplete but valid, returning the retry result")
        void retriesOnce_whenResponseIncompleteButValid() throws Exception {
            StreamInfo incomplete = incompleteStreamInfo();
            StreamInfo complete = completeStreamInfo();

            try (MockedStatic<StreamInfo> extractor = mockStatic(StreamInfo.class)) {
                extractor.when(() -> StreamInfo.getInfo(TEST_URL)).thenReturn(incomplete, complete);

                StreamInfo result = service.getStreamInfo(TEST_URL);

                assertThat(result).isSameAs(complete);
                extractor.verify(() -> StreamInfo.getInfo(TEST_URL), times(2));
            }
        }

        @Test
        @DisplayName("Gives up after one retry, returning the second result even if still incomplete")
        void returnsSecondResult_whenRetryAlsoIncomplete() throws Exception {
            StreamInfo firstIncomplete = incompleteStreamInfo();
            StreamInfo secondIncomplete = incompleteStreamInfo();

            try (MockedStatic<StreamInfo> extractor = mockStatic(StreamInfo.class)) {
                extractor.when(() -> StreamInfo.getInfo(TEST_URL))
                        .thenReturn(firstIncomplete, secondIncomplete);

                StreamInfo result = service.getStreamInfo(TEST_URL);

                assertThat(result).isSameAs(secondIncomplete);
                extractor.verify(() -> StreamInfo.getInfo(TEST_URL), times(2));
            }
        }

        @Test
        @DisplayName("Does not retry when attempts is 1")
        void doesNotRetry_whenAttemptsIsOne() throws Exception {
            VideoStreamingService noRetry = serviceWithAttempts(1);
            StreamInfo incomplete = incompleteStreamInfo();

            try (MockedStatic<StreamInfo> extractor = mockStatic(StreamInfo.class)) {
                extractor.when(() -> StreamInfo.getInfo(TEST_URL)).thenReturn(incomplete);

                StreamInfo result = noRetry.getStreamInfo(TEST_URL);

                assertThat(result).isSameAs(incomplete);
                extractor.verify(() -> StreamInfo.getInfo(TEST_URL), times(1));
            }
        }

        @Test
        @DisplayName("Retries up to the configured number of attempts")
        void retriesUpToConfiguredAttempts() throws Exception {
            VideoStreamingService threeAttempts = serviceWithAttempts(3);
            // Built before stubbing: creating a mock inside thenReturn() breaks Mockito
            StreamInfo first = incompleteStreamInfo();
            StreamInfo second = incompleteStreamInfo();
            StreamInfo third = incompleteStreamInfo();
            StreamInfo fourth = completeStreamInfo();

            try (MockedStatic<StreamInfo> extractor = mockStatic(StreamInfo.class)) {
                extractor.when(() -> StreamInfo.getInfo(TEST_URL)).thenReturn(first, second, third, fourth);

                StreamInfo result = threeAttempts.getStreamInfo(TEST_URL);

                assertThat(result).isSameAs(third);
                extractor.verify(() -> StreamInfo.getInfo(TEST_URL), times(3));
            }
        }

        @Test
        @DisplayName("Does not retry when duration is invalid (zero)")
        void doesNotRetry_whenDurationInvalid() throws Exception {
            StreamInfo invalid = streamInfo(Collections.emptyList(), Collections.emptyList(), 0L);

            try (MockedStatic<StreamInfo> extractor = mockStatic(StreamInfo.class)) {
                extractor.when(() -> StreamInfo.getInfo(TEST_URL)).thenReturn(invalid);

                StreamInfo result = service.getStreamInfo(TEST_URL);

                assertThat(result).isSameAs(invalid);
                extractor.verify(() -> StreamInfo.getInfo(TEST_URL), times(1));
            }
        }

        @Test
        @DisplayName("Translates a private video to CONTENT_PRIVATE, keeping the cause and its text out of the message")
        void translatesFailure() {
            PrivateContentException thrown = new PrivateContentException("This video is private");

            try (MockedStatic<StreamInfo> extractor = mockStatic(StreamInfo.class)) {
                extractor.when(() -> StreamInfo.getInfo(TEST_URL)).thenThrow(thrown);

                assertThatThrownBy(() -> service.getStreamInfo(TEST_URL))
                        .isInstanceOf(ExtractionException.class)
                        .hasMessage(ErrorCode.CONTENT_PRIVATE.defaultMessage())
                        .hasCause(thrown)
                        .extracting(e -> ((ApiException) e).getCode())
                        .isEqualTo(ErrorCode.CONTENT_PRIVATE);
            }
        }
    }

    // ── Accessor delegation ──────────────────────────────────────────────

    static Stream<Arguments> accessors() {
        List<AudioStream> audio = List.of(mock(AudioStream.class), mock(AudioStream.class));
        List<VideoStream> video = List.of(mock(VideoStream.class), mock(VideoStream.class));
        String dashUrl = "https://youtube.com/dash/manifest.mpd";
        List<Image> thumbnails = List.of(mock(Image.class));
        List<SubtitlesStream> subtitles = List.of(mock(SubtitlesStream.class));
        List<StreamSegment> segments = List.of(mock(StreamSegment.class));
        List<Frameset> frames = List.of(mock(Frameset.class));
        Description description = mock(Description.class);
        List<InfoItem> related = List.of(mock(InfoItem.class), mock(InfoItem.class));

        return Stream.of(
                accessor("getAudioStreams",
                        info -> when(info.getAudioStreams()).thenReturn(audio),
                        s -> s.getAudioStreams(TEST_URL), audio),
                accessor("getVideoStreams",
                        info -> when(info.getVideoOnlyStreams()).thenReturn(video),
                        s -> s.getVideoStreams(TEST_URL), video),
                accessor("getDashMpdUrl",
                        info -> when(info.getDashMpdUrl()).thenReturn(dashUrl),
                        s -> s.getDashMpdUrl(TEST_URL), dashUrl),
                accessor("getStreamThumbnails",
                        info -> when(info.getThumbnails()).thenReturn(thumbnails),
                        s -> s.getStreamThumbnails(TEST_URL), thumbnails),
                accessor("getSubtitleStreams",
                        info -> when(info.getSubtitles()).thenReturn(subtitles),
                        s -> s.getSubtitleStreams(TEST_URL), subtitles),
                accessor("getStreamSegments",
                        info -> when(info.getStreamSegments()).thenReturn(segments),
                        s -> s.getStreamSegments(TEST_URL), segments),
                accessor("getPreviewFrames",
                        info -> when(info.getPreviewFrames()).thenReturn(frames),
                        s -> s.getPreviewFrames(TEST_URL), frames),
                accessor("getStreamDescription",
                        info -> when(info.getDescription()).thenReturn(description),
                        s -> s.getStreamDescription(TEST_URL), description),
                accessor("getRelatedStreams",
                        info -> when(info.getRelatedItems()).thenReturn(related),
                        s -> s.getRelatedStreams(TEST_URL), related)
        );
    }

    private static Arguments accessor(String name, Consumer<StreamInfo> stub,
                                      ServiceCall call, Object expected) {
        return Arguments.of(name, stub, call, expected);
    }

    @ParameterizedTest(name = "{0} returns the value from StreamInfo")
    @MethodSource("accessors")
    @DisplayName("Accessors delegate to the corresponding StreamInfo getter")
    void accessor_delegatesToStreamInfo(String name, Consumer<StreamInfo> stub,
                                        ServiceCall call, Object expected) throws Exception {
        // Complete info so the retry path stays inert; the stub then wires
        // the sentinel into the getter this accessor is supposed to read.
        StreamInfo info = completeStreamInfo();
        stub.accept(info);

        try (MockedStatic<StreamInfo> extractor = mockStatic(StreamInfo.class)) {
            extractor.when(() -> StreamInfo.getInfo(TEST_URL)).thenReturn(info);

            assertThat(call.apply(service)).isSameAs(expected);
        }
    }

    @ParameterizedTest(name = "{0} translates failures")
    @MethodSource("accessors")
    @DisplayName("Accessors translate extraction failures")
    void accessor_translatesExtractionFailure(String name, Consumer<StreamInfo> stub,
                                              ServiceCall call, Object expected) {
        ParsingException thrown = new ParsingException("Could not get video id");

        try (MockedStatic<StreamInfo> extractor = mockStatic(StreamInfo.class)) {
            extractor.when(() -> StreamInfo.getInfo(TEST_URL)).thenThrow(thrown);

            assertThatThrownBy(() -> call.apply(service))
                    .isInstanceOf(ExtractionException.class)
                    .hasMessage(ErrorCode.UPSTREAM_CHANGED.defaultMessage())
                    .hasCause(thrown);
        }
    }

    @Nested
    @DisplayName("caching")
    class CachingTests {

        @Test
        @DisplayName("Several accessors for one URL share a single extraction")
        void reusesExtractionAcrossAccessors() throws Exception {
            StreamInfo info = completeStreamInfo();

            try (MockedStatic<StreamInfo> extractor = mockStatic(StreamInfo.class)) {
                extractor.when(() -> StreamInfo.getInfo(TEST_URL)).thenReturn(info);

                service.getAudioStreams(TEST_URL);
                service.getSubtitleStreams(TEST_URL);
                service.getRelatedStreams(TEST_URL);

                extractor.verify(() -> StreamInfo.getInfo(TEST_URL), times(1));
            }
        }

        // ── Accessor delegation ──────────────────────────────────────────

        @Test
        @DisplayName("Different URLs are extracted separately")
        void differentUrlsExtractSeparately() throws Exception {
            String other = "https://www.youtube.com/watch?v=oHg5SJYRHA0";
            StreamInfo info = completeStreamInfo();

            try (MockedStatic<StreamInfo> extractor = mockStatic(StreamInfo.class)) {
                extractor.when(() -> StreamInfo.getInfo(TEST_URL)).thenReturn(info);
                extractor.when(() -> StreamInfo.getInfo(other)).thenReturn(info);

                service.getStreamInfo(TEST_URL);
                service.getStreamInfo(other);

                extractor.verify(() -> StreamInfo.getInfo(TEST_URL), times(1));
                extractor.verify(() -> StreamInfo.getInfo(other), times(1));
            }
        }

        @Test
        @DisplayName("A failed extraction is not cached")
        void failuresAreNotCached() throws Exception {
            StreamInfo info = completeStreamInfo();

            try (MockedStatic<StreamInfo> extractor = mockStatic(StreamInfo.class)) {
                extractor.when(() -> StreamInfo.getInfo(TEST_URL))
                        .thenThrow(new IOException("transient"))
                        .thenReturn(info);

                assertThatThrownBy(() -> service.getStreamInfo(TEST_URL))
                        .isInstanceOf(ExtractionException.class);

                assertThat(service.getStreamInfo(TEST_URL)).isSameAs(info);
                extractor.verify(() -> StreamInfo.getInfo(TEST_URL), times(2));
            }
        }
    }
}