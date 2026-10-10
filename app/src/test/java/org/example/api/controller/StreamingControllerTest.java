package org.example.api.controller;

import org.example.api.component.DashPlayabilityFilter;
import org.example.api.component.DashPlayabilityFilter.PlayableStreams;
import org.example.api.config.GlobalExceptionHandler;
import org.example.api.dto.dash.DashManifestConfigDTO;
import org.example.api.exception.ExtractionException;
import org.example.api.service.DashManifestGeneratorService;
import org.example.api.service.VideoStreamingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.Description;
import org.schabi.newpipe.extractor.stream.Frameset;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamSegment;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for StreamingController.
 */
@DisplayName("StreamingController Tests")
class StreamingControllerTest {

    private static final String TEST_VIDEO_ID = "dQw4w9WgXcQ";
    private static final String VIDEO_URL = "https://www.youtube.com/watch?v=" + TEST_VIDEO_ID;

    private static final MediaType DASH_MPD =
            new MediaType("application", "dash+xml", StandardCharsets.UTF_8);
    private static final MediaType URI_LIST =
            new MediaType("text", "uri-list", StandardCharsets.UTF_8);

    private MockMvc mockMvc;

    @Mock
    private VideoStreamingService videoStreamingService;

    @Mock
    private DashManifestGeneratorService dashManifestGeneratorService;

    @Mock
    private DashPlayabilityFilter dashPlayabilityFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        StreamingController streamingController = new StreamingController(
                videoStreamingService, dashManifestGeneratorService, dashPlayabilityFilter);
        mockMvc = MockMvcBuilders.standaloneSetup(streamingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /** Stubbing or verification against the streaming service. */
    @FunctionalInterface
    interface ServiceInteraction {
        void accept(VideoStreamingService service) throws Exception;
    }

    // ── Accessor endpoints (one table, one test) ─────────────────────────

    static Stream<Arguments> accessorEndpoints() {
        StreamInfo info = mock(StreamInfo.class);
        StreamInfo detailsInfo = mock(StreamInfo.class);
        List<AudioStream> audio = List.of(mock(AudioStream.class));
        List<VideoStream> video = List.of(mock(VideoStream.class));
        String dashUrl = "https://youtube.com/dash/manifest.mpd";
        List<Image> thumbnails = List.of(mock(Image.class));
        List<SubtitlesStream> subtitles = List.of(mock(SubtitlesStream.class));
        List<StreamSegment> segments = List.of(mock(StreamSegment.class));
        List<Frameset> frames = List.of(mock(Frameset.class));
        Description description = mock(Description.class);
        List<InfoItem> related = List.of(mock(StreamInfoItem.class));

        return Stream.of(
                Arguments.of("", (ServiceInteraction)
                                s -> {
                                    when(info.getName()).thenReturn("Test Video");
                                    when(s.getStreamInfo(VIDEO_URL)).thenReturn(info);
                                },
                        (ServiceInteraction) s -> verify(s).getStreamInfo(VIDEO_URL)),
                Arguments.of("/audio", (ServiceInteraction)
                                s -> when(s.getAudioStreams(VIDEO_URL)).thenReturn(audio),
                        (ServiceInteraction) s -> verify(s).getAudioStreams(VIDEO_URL)),
                Arguments.of("/video", (ServiceInteraction)
                                s -> when(s.getVideoStreams(VIDEO_URL)).thenReturn(video),
                        (ServiceInteraction) s -> verify(s).getVideoStreams(VIDEO_URL)),
                Arguments.of("/video/dash", (ServiceInteraction)
                                s -> when(s.getDashMpdUrl(VIDEO_URL)).thenReturn(dashUrl),
                        (ServiceInteraction) s -> verify(s).getDashMpdUrl(VIDEO_URL)),
                Arguments.of("/thumbnails", (ServiceInteraction)
                                s -> when(s.getStreamThumbnails(VIDEO_URL)).thenReturn(thumbnails),
                        (ServiceInteraction) s -> verify(s).getStreamThumbnails(VIDEO_URL)),
                Arguments.of("/subtitles", (ServiceInteraction)
                                s -> when(s.getSubtitleStreams(VIDEO_URL)).thenReturn(subtitles),
                        (ServiceInteraction) s -> verify(s).getSubtitleStreams(VIDEO_URL)),
                Arguments.of("/segments", (ServiceInteraction)
                                s -> when(s.getStreamSegments(VIDEO_URL)).thenReturn(segments),
                        (ServiceInteraction) s -> verify(s).getStreamSegments(VIDEO_URL)),
                Arguments.of("/preview-frames", (ServiceInteraction)
                                s -> when(s.getPreviewFrames(VIDEO_URL)).thenReturn(frames),
                        (ServiceInteraction) s -> verify(s).getPreviewFrames(VIDEO_URL)),
                Arguments.of("/description", (ServiceInteraction)
                                s -> when(s.getStreamDescription(VIDEO_URL)).thenReturn(description),
                        (ServiceInteraction) s -> verify(s).getStreamDescription(VIDEO_URL)),
                Arguments.of("/details", (ServiceInteraction)
                                s -> {
                                    when(detailsInfo.getName()).thenReturn("Test Video");
                                    when(detailsInfo.getUrl()).thenReturn(VIDEO_URL);
                                    when(s.getStreamInfo(VIDEO_URL)).thenReturn(detailsInfo);
                                },
                        (ServiceInteraction) s -> verify(s).getStreamInfo(VIDEO_URL)),
                Arguments.of("/related", (ServiceInteraction)
                                s -> when(s.getRelatedStreams(VIDEO_URL)).thenReturn(related),
                        (ServiceInteraction) s -> verify(s).getRelatedStreams(VIDEO_URL)));
    }

    @ParameterizedTest(name = "GET /api/v1/streams{0} delegates with the constructed URL")
    @MethodSource("accessorEndpoints")
    @DisplayName("Accessor endpoints return 200 and delegate with the exact constructed watch URL")
    void accessorEndpoint_delegatesWithConstructedUrl(
            String path, ServiceInteraction stub, ServiceInteraction verification) throws Exception {
        stub.accept(videoStreamingService);

        mockMvc.perform(get("/api/v1/streams" + path)
                        .param("id", TEST_VIDEO_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verification.accept(videoStreamingService);
    }

    // ── Parameter binding (all endpoints, missing and empty id) ──────────

    static Stream<String> allEndpointPaths() {
        return Stream.of("", "/audio", "/video", "/video/dash", "/dash",
                "/thumbnails", "/subtitles", "/segments", "/preview-frames",
                "/description", "/details", "/related");
    }

    @ParameterizedTest(name = "GET /api/v1/streams{0} without id returns 400")
    @MethodSource("allEndpointPaths")
    @DisplayName("A missing id parameter returns 400 on every endpoint")
    void missingId_returnsBadRequest(String path) throws Exception {
        mockMvc.perform(get("/api/v1/streams" + path)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "GET /api/v1/streams{0} with empty id returns 400")
    @MethodSource("allEndpointPaths")
    @DisplayName("An empty id parameter returns 400 on every endpoint")
    void emptyId_returnsBadRequest(String path) throws Exception {
        mockMvc.perform(get("/api/v1/streams" + path)
                        .param("id", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(videoStreamingService);
    }

    // ── GET /api/v1/streams/dash ─────────────────────────────────────────

    /** Filter results that leave no valid on-demand manifest possible. */
    static Stream<Arguments> unplayableResults() {
        return Stream.of(
                Arguments.of("no playable video",
                        new PlayableStreams(List.of(), List.of(mock(AudioStream.class)), List.of())),
                Arguments.of("no playable audio",
                        new PlayableStreams(List.of(mock(VideoStream.class)), List.of(), List.of())),
                Arguments.of("nothing playable",
                        new PlayableStreams(List.of(), List.of(), List.of())));
    }

    @Nested
    @DisplayName("GET /api/v1/streams/dash")
    class DashManifestTests {

        private static final String DIRECT_URL =
                "https://rr3---sn-example.googlevideo.com/videoplayback?muxed";

        private StreamInfo streamInfo;

        @BeforeEach
        void stubStreamInfo() {
            streamInfo = mock(StreamInfo.class);
            when(streamInfo.getDuration()).thenReturn(120L);
            when(videoStreamingService.getStreamInfo(VIDEO_URL)).thenReturn(streamInfo);
        }

        @Test
        @DisplayName("Builds the manifest from the filter's playable streams and serves it as application/dash+xml")
        void returnsGeneratedManifest() throws Exception {
            PlayableStreams playable = new PlayableStreams(
                    List.of(mock(VideoStream.class)), List.of(mock(AudioStream.class)), List.of());
            when(dashPlayabilityFilter.filter(streamInfo)).thenReturn(playable);
            when(dashManifestGeneratorService.generateManifestXml(any())).thenReturn("<MPD/>");

            mockMvc.perform(get("/api/v1/streams/dash").param("id", TEST_VIDEO_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(DASH_MPD))
                    .andExpect(content().string("<MPD/>"));

            // The config is built from exactly what the filter returned
            ArgumentCaptor<DashManifestConfigDTO> config = ArgumentCaptor.forClass(DashManifestConfigDTO.class);
            verify(dashManifestGeneratorService).generateManifestXml(config.capture());
            assertThat(config.getValue().getVideoStreams()).hasSize(1);
            assertThat(config.getValue().getAudioStreams()).hasSize(1);
            assertThat(config.getValue().getSubtitleStreams()).isEmpty();

            verify(dashPlayabilityFilter, never()).muxedFallback(any());
        }

        @ParameterizedTest(name = "Falls back when the filter leaves {0}")
        @MethodSource("org.example.api.controller.StreamingControllerTest#unplayableResults")
        @DisplayName("Serves the muxed stream URL as text/uri-list when no DASH manifest is possible")
        void fallsBackToMuxedStream(String name, PlayableStreams playable) throws Exception {
            VideoStream muxed = mock(VideoStream.class);
            when(muxed.getContent()).thenReturn(DIRECT_URL);
            when(dashPlayabilityFilter.filter(streamInfo)).thenReturn(playable);
            when(dashPlayabilityFilter.muxedFallback(streamInfo)).thenReturn(Optional.of(muxed));

            mockMvc.perform(get("/api/v1/streams/dash").param("id", TEST_VIDEO_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(URI_LIST))
                    // Kept for one release while the frontend switches to Content-Type
                    .andExpect(header().string("X-Stream-Type", "muxed-progressive"))
                    .andExpect(content().string(DIRECT_URL + "\r\n"));

            // The fallback path must not build a manifest
            verifyNoInteractions(dashManifestGeneratorService);
        }

        @Test
        @DisplayName("Returns 503 when nothing is playable and there is no muxed stream either")
        void returns503_whenNoStreamsAtAll() throws Exception {
            when(dashPlayabilityFilter.filter(streamInfo))
                    .thenReturn(new PlayableStreams(List.of(), List.of(), List.of()));
            when(dashPlayabilityFilter.muxedFallback(streamInfo)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/streams/dash").param("id", TEST_VIDEO_ID))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().string(containsString("No streams available")));

            verifyNoInteractions(dashManifestGeneratorService);
        }
    }

    // ── Exception propagation (single advice-wiring test) ────────────────

    @Test
    @DisplayName("Propagates service failures to the exception handler")
    void propagatesFailuresToAdvice() throws Exception {
        when(videoStreamingService.getStreamInfo(anyString()))
                .thenThrow(new ExtractionException("Extraction failed"));

        mockMvc.perform(get("/api/v1/streams")
                        .param("id", TEST_VIDEO_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("EXTRACTION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("Extraction failed")));
    }
}