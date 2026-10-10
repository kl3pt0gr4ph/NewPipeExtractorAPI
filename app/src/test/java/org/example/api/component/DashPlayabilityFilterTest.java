package org.example.api.component;

import org.example.api.component.DashPlayabilityFilter.PlayableStreams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DashPlayabilityFilter.
 *
 * <p>Every rule is about playability, so each test pairs a broken stream with a playable
 * one and asserts that only the broken one is dropped.</p>
 */
@DisplayName("DashPlayabilityFilter Tests")
class DashPlayabilityFilterTest {

    private static final String YT_SUBTITLE_BASE = "https://www.youtube.com/api/timedtext?v=abc&lang=";

    private DashPlayabilityFilter filter;

    @BeforeEach
    void setUp() {
        filter = new DashPlayabilityFilter();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** A video-only stream that passes every rule; tests break one thing at a time. */
    private static VideoStream video(String name) {
        VideoStream stream = mock(VideoStream.class);
        when(stream.getDeliveryMethod()).thenReturn(DeliveryMethod.PROGRESSIVE_HTTP);
        when(stream.isUrl()).thenReturn(true);
        when(stream.getContent()).thenReturn("https://example.test/" + name);
        when(stream.getFormat()).thenReturn(MediaFormat.MPEG_4);
        when(stream.getCodec()).thenReturn("avc1.640028");
        when(stream.getInitStart()).thenReturn(0);
        when(stream.getInitEnd()).thenReturn(740);
        when(stream.getIndexStart()).thenReturn(741);
        when(stream.getIndexEnd()).thenReturn(1048);
        return stream;
    }

    /** An audio stream that passes every rule; tests break one thing at a time. */
    private static AudioStream audio(String name) {
        AudioStream stream = mock(AudioStream.class);
        when(stream.getDeliveryMethod()).thenReturn(DeliveryMethod.PROGRESSIVE_HTTP);
        when(stream.isUrl()).thenReturn(true);
        when(stream.getContent()).thenReturn("https://example.test/" + name);
        when(stream.getFormat()).thenReturn(MediaFormat.M4A);
        when(stream.getCodec()).thenReturn("mp4a.40.2");
        when(stream.getInitStart()).thenReturn(0);
        when(stream.getInitEnd()).thenReturn(600);
        when(stream.getIndexStart()).thenReturn(601);
        when(stream.getIndexEnd()).thenReturn(900);
        return stream;
    }

    /** A subtitle served the way NewPipe builds YouTube URLs: base URL plus &fmt=<suffix>. */
    private static SubtitlesStream subtitle(String trackBaseUrl, MediaFormat format) {
        SubtitlesStream stream = mock(SubtitlesStream.class);
        when(stream.getDeliveryMethod()).thenReturn(DeliveryMethod.PROGRESSIVE_HTTP);
        when(stream.isUrl()).thenReturn(true);
        String suffix = format != null ? format.getSuffix() : "unknown";
        when(stream.getContent()).thenReturn(trackBaseUrl + "&fmt=" + suffix);
        when(stream.getFormat()).thenReturn(format);
        return stream;
    }

    /** A muxed (audio+video) stream; it needs only a progressive URL, no codec or ranges. */
    private static VideoStream muxed(String name) {
        VideoStream stream = mock(VideoStream.class);
        when(stream.getDeliveryMethod()).thenReturn(DeliveryMethod.PROGRESSIVE_HTTP);
        when(stream.isUrl()).thenReturn(true);
        when(stream.getContent()).thenReturn("https://example.test/" + name);
        return stream;
    }

    private static StreamInfo info(List<VideoStream> video, List<AudioStream> audio,
                                   List<SubtitlesStream> subtitles) {
        StreamInfo info = mock(StreamInfo.class);
        when(info.getVideoOnlyStreams()).thenReturn(video);
        when(info.getAudioStreams()).thenReturn(audio);
        when(info.getSubtitles()).thenReturn(subtitles);
        return info;
    }

    private static StreamInfo infoWithMuxed(VideoStream... muxedStreams) {
        StreamInfo info = mock(StreamInfo.class);
        when(info.getVideoStreams()).thenReturn(Arrays.asList(muxedStreams));
        return info;
    }

    private static MediaFormat formatWithMimeType(String mimeType) {
        MediaFormat format = mock(MediaFormat.class);
        when(format.getMimeType()).thenReturn(mimeType);
        return format;
    }

    // ── Video-only streams ───────────────────────────────────────────────

    @Nested
    @DisplayName("Video-only streams")
    class VideoTests {

        @Test
        @DisplayName("Keeps a fully playable stream, including ranges that start at byte 0")
        void keepsPlayableStream() {
            VideoStream good = video("good");

            PlayableStreams playable = filter.filter(info(List.of(good), List.of(), List.of()));

            assertThat(playable.video()).containsExactly(good);
        }

        @ParameterizedTest(name = "Drops a video stream with {0}")
        @MethodSource("org.example.api.component.DashPlayabilityFilterTest#videoRejections")
        @DisplayName("Drops a stream that breaks one playability rule, keeping the playable one")
        void dropsUnplayableStream(String name, Consumer<VideoStream> breakIt) {
            VideoStream broken = video("broken");
            breakIt.accept(broken);
            VideoStream good = video("good");

            PlayableStreams playable = filter.filter(info(List.of(broken, good), List.of(), List.of()));

            assertThat(playable.video()).containsExactly(good);
        }
    }

    static Stream<Arguments> videoRejections() {
        return Stream.of(
                // Delivery and URL
                Arguments.of("DASH delivery", (Consumer<VideoStream>) v ->
                        when(v.getDeliveryMethod()).thenReturn(DeliveryMethod.DASH)),
                Arguments.of("HLS delivery", (Consumer<VideoStream>) v ->
                        when(v.getDeliveryMethod()).thenReturn(DeliveryMethod.HLS)),
                Arguments.of("content that is not a URL", (Consumer<VideoStream>) v ->
                        when(v.isUrl()).thenReturn(false)),
                Arguments.of("a blank URL", (Consumer<VideoStream>) v ->
                        when(v.getContent()).thenReturn(" ")),
                // mimeType and codec
                Arguments.of("no MediaFormat", (Consumer<VideoStream>) v ->
                        when(v.getFormat()).thenReturn(null)),
                Arguments.of("a blank mimeType", (Consumer<VideoStream>) v -> {
                    MediaFormat format = formatWithMimeType("");
                    when(v.getFormat()).thenReturn(format);
                }),
                Arguments.of("no codec", (Consumer<VideoStream>) v ->
                        when(v.getCodec()).thenReturn(null)),
                Arguments.of("a blank codec", (Consumer<VideoStream>) v ->
                        when(v.getCodec()).thenReturn(" ")),
                // Byte ranges
                Arguments.of("unknown ranges (ItagItem leaves them at 0)", (Consumer<VideoStream>) v -> {
                    when(v.getInitEnd()).thenReturn(0);
                    when(v.getIndexStart()).thenReturn(0);
                    when(v.getIndexEnd()).thenReturn(0);
                }),
                Arguments.of("a negative init start", (Consumer<VideoStream>) v ->
                        when(v.getInitStart()).thenReturn(-1)),
                Arguments.of("an init end that isn't after its start", (Consumer<VideoStream>) v ->
                        when(v.getInitEnd()).thenReturn(0)),
                Arguments.of("an index range overlapping the init segment", (Consumer<VideoStream>) v ->
                        when(v.getIndexStart()).thenReturn(700)),
                Arguments.of("an index end that isn't after its start", (Consumer<VideoStream>) v ->
                        when(v.getIndexEnd()).thenReturn(741)));
    }

    // ── Audio streams ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Audio streams")
    class AudioTests {

        @Test
        @DisplayName("Keeps a fully playable stream")
        void keepsPlayableStream() {
            AudioStream good = audio("good");

            PlayableStreams playable = filter.filter(info(List.of(), List.of(good), List.of()));

            assertThat(playable.audio()).containsExactly(good);
        }

        /**
         * Audio shares the video rules; these cases prove the filter reads the audio
         * stream's own codec and ranges rather than only the Stream-level fields.
         */
        @ParameterizedTest(name = "Drops an audio stream with {0}")
        @MethodSource("org.example.api.component.DashPlayabilityFilterTest#audioRejections")
        @DisplayName("Drops a stream that breaks one playability rule, keeping the playable one")
        void dropsUnplayableStream(String name, Consumer<AudioStream> breakIt) {
            AudioStream broken = audio("broken");
            breakIt.accept(broken);
            AudioStream good = audio("good");

            PlayableStreams playable = filter.filter(info(List.of(), List.of(broken, good), List.of()));

            assertThat(playable.audio()).containsExactly(good);
        }
    }

    static Stream<Arguments> audioRejections() {
        return Stream.of(
                Arguments.of("DASH delivery", (Consumer<AudioStream>) a ->
                        when(a.getDeliveryMethod()).thenReturn(DeliveryMethod.DASH)),
                Arguments.of("no MediaFormat", (Consumer<AudioStream>) a ->
                        when(a.getFormat()).thenReturn(null)),
                Arguments.of("no codec", (Consumer<AudioStream>) a ->
                        when(a.getCodec()).thenReturn(null)),
                Arguments.of("unknown ranges (ItagItem leaves them at 0)", (Consumer<AudioStream>) a -> {
                    when(a.getInitEnd()).thenReturn(0);
                    when(a.getIndexStart()).thenReturn(0);
                    when(a.getIndexEnd()).thenReturn(0);
                }));
    }

    // ── Subtitles ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Subtitles")
    class SubtitleTests {

        @ParameterizedTest(name = "Drops {0}")
        @MethodSource("org.example.api.component.DashPlayabilityFilterTest#unparseableSubtitleFormats")
        @DisplayName("Drops formats a DASH player can't load from a BaseURL")
        void dropsUnparseableFormats(MediaFormat format) {
            SubtitlesStream unparseable = subtitle(YT_SUBTITLE_BASE + "fr", format);
            SubtitlesStream vtt = subtitle(YT_SUBTITLE_BASE + "en", MediaFormat.VTT);

            PlayableStreams playable = filter.filter(info(List.of(), List.of(), List.of(unparseable, vtt)));

            assertThat(playable.subtitles()).containsExactly(vtt);
        }

        @Test
        @DisplayName("Keeps TTML when it is the only serialization of a track")
        void keepsTtmlWhenItIsTheOnlyFormat() {
            SubtitlesStream ttml = subtitle(YT_SUBTITLE_BASE + "en", MediaFormat.TTML);

            PlayableStreams playable = filter.filter(info(List.of(), List.of(), List.of(ttml)));

            assertThat(playable.subtitles()).containsExactly(ttml);
        }

        @ParameterizedTest(name = "TTML listed {0}")
        @MethodSource("org.example.api.component.DashPlayabilityFilterTest#ttmlPositions")
        @DisplayName("Keeps one serialization per track, preferring VTT whatever the input order")
        void prefersVttForTheSameTrack(String position, boolean ttmlFirst) {
            SubtitlesStream vtt = subtitle(YT_SUBTITLE_BASE + "en", MediaFormat.VTT);
            SubtitlesStream ttml = subtitle(YT_SUBTITLE_BASE + "en", MediaFormat.TTML);
            List<SubtitlesStream> input = ttmlFirst ? List.of(ttml, vtt) : List.of(vtt, ttml);

            PlayableStreams playable = filter.filter(info(List.of(), List.of(), input));

            assertThat(playable.subtitles()).containsExactly(vtt);
        }

        @Test
        @DisplayName("Keeps manual and auto-generated tracks in the same language as separate tracks")
        void keepsManualAndAutoGeneratedTracks() {
            SubtitlesStream manual = subtitle(YT_SUBTITLE_BASE + "en", MediaFormat.VTT);
            SubtitlesStream auto = subtitle(YT_SUBTITLE_BASE + "en&kind=asr", MediaFormat.VTT);

            PlayableStreams playable = filter.filter(info(List.of(), List.of(), List.of(manual, auto)));

            assertThat(playable.subtitles()).containsExactly(manual, auto);
        }

        @Test
        @DisplayName("Preserves the order tracks were first seen in")
        void preservesFirstSeenTrackOrder() {
            SubtitlesStream esTtml = subtitle(YT_SUBTITLE_BASE + "es", MediaFormat.TTML);
            SubtitlesStream en = subtitle(YT_SUBTITLE_BASE + "en", MediaFormat.VTT);
            SubtitlesStream esVtt = subtitle(YT_SUBTITLE_BASE + "es", MediaFormat.VTT);

            PlayableStreams playable = filter.filter(info(List.of(), List.of(), List.of(esTtml, en, esVtt)));

            // "es" was seen first, so it stays first even though its VTT arrived last
            assertThat(playable.subtitles()).containsExactly(esVtt, en);
        }

        @Test
        @DisplayName("Drops subtitles that can't be fetched by URL")
        void dropsSubtitlesWithoutUrl() {
            SubtitlesStream notProgressive = subtitle(YT_SUBTITLE_BASE + "fr", MediaFormat.VTT);
            when(notProgressive.getDeliveryMethod()).thenReturn(DeliveryMethod.DASH);
            SubtitlesStream notUrl = subtitle(YT_SUBTITLE_BASE + "de", MediaFormat.VTT);
            when(notUrl.isUrl()).thenReturn(false);
            SubtitlesStream good = subtitle(YT_SUBTITLE_BASE + "en", MediaFormat.VTT);

            PlayableStreams playable = filter.filter(
                    info(List.of(), List.of(), List.of(notProgressive, notUrl, good)));

            assertThat(playable.subtitles()).containsExactly(good);
        }
    }

    static Stream<MediaFormat> unparseableSubtitleFormats() {
        List<MediaFormat> formats = new ArrayList<>(List.of(
                MediaFormat.TRANSCRIPT1, MediaFormat.TRANSCRIPT2, MediaFormat.TRANSCRIPT3, MediaFormat.SRT));
        formats.add(null); // unknown format
        return formats.stream();
    }

    static Stream<Arguments> ttmlPositions() {
        return Stream.of(
                Arguments.of("first", true),
                Arguments.of("second", false));
    }

    // ── Null and empty input ─────────────────────────────────────────────

    @Test
    @DisplayName("Treats null stream lists as empty instead of failing")
    void treatsNullListsAsEmpty() {
        PlayableStreams playable = filter.filter(info(null, null, null));

        assertThat(playable.video()).isEmpty();
        assertThat(playable.audio()).isEmpty();
        assertThat(playable.subtitles()).isEmpty();
    }

    // ── supportsDashManifest ─────────────────────────────────────────────

    static Stream<Arguments> manifestSupportCases() {
        return Stream.of(
                Arguments.of("video and audio",
                        new PlayableStreams(List.of(video("v")), List.of(audio("a")), List.of()), true),
                Arguments.of("no video",
                        new PlayableStreams(List.of(), List.of(audio("a")), List.of()), false),
                Arguments.of("no audio",
                        new PlayableStreams(List.of(video("v")), List.of(), List.of()), false),
                Arguments.of("only subtitles",
                        new PlayableStreams(List.of(), List.of(),
                                List.of(subtitle(YT_SUBTITLE_BASE + "en", MediaFormat.VTT))), false));
    }

    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("manifestSupportCases")
    @DisplayName("A manifest needs at least one playable video and one playable audio stream")
    void supportsDashManifest(String name, PlayableStreams playable, boolean expected) {
        assertThat(playable.supportsDashManifest()).isEqualTo(expected);
    }

    // ── Muxed fallback ───────────────────────────────────────────────────

    @Nested
    @DisplayName("muxedFallback")
    class MuxedFallbackTests {

        @Test
        @DisplayName("Returns a muxed stream that has a progressive URL, without needing codec or ranges")
        void returnsPlayableMuxedStream() {
            VideoStream stream = muxed("360p");

            assertThat(filter.muxedFallback(infoWithMuxed(stream))).contains(stream);
        }

        @Test
        @DisplayName("Skips muxed streams that can't be fetched by URL and returns the first playable one")
        void skipsUnplayableMuxedStreams() {
            VideoStream hls = muxed("hls");
            when(hls.getDeliveryMethod()).thenReturn(DeliveryMethod.HLS);
            VideoStream notUrl = muxed("not-url");
            when(notUrl.isUrl()).thenReturn(false);
            VideoStream first = muxed("first");
            VideoStream second = muxed("second");

            assertThat(filter.muxedFallback(infoWithMuxed(hls, notUrl, first, second))).contains(first);
        }

        @Test
        @DisplayName("Returns empty when no muxed stream is playable")
        void returnsEmptyWhenNothingIsPlayable() {
            VideoStream blank = muxed("blank");
            when(blank.getContent()).thenReturn("");

            assertThat(filter.muxedFallback(infoWithMuxed(blank))).isEmpty();
        }

        @Test
        @DisplayName("Returns empty when there are no muxed streams at all")
        void returnsEmptyWithoutMuxedStreams() {
            assertThat(filter.muxedFallback(infoWithMuxed())).isEmpty();
        }
    }
}