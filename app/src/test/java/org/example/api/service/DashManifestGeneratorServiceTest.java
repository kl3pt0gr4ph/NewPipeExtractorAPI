package org.example.api.service;

import org.example.api.dto.dash.AudioStreamMetadataDTO;
import org.example.api.dto.dash.DashManifestConfigDTO;
import org.example.api.dto.dash.SubtitleMetadataDTO;
import org.example.api.dto.dash.VideoStreamMetadataDTO;
import org.example.api.exception.ValidationException;
import org.example.api.utils.ManifestXmlBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.schabi.newpipe.extractor.stream.AudioTrackType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for DashManifestGeneratorService.
 *
 * <p>The generator's contract: every representation in the config ends up in the manifest,
 * grouped and labelled for the player. Anything the playability filter should have removed
 * is rejected with IllegalStateException rather than written as a broken manifest.</p>
 */
@DisplayName("DashManifestGeneratorService Tests")
class DashManifestGeneratorServiceTest {

    private DashManifestGeneratorService service;

    @BeforeEach
    void setUp() {
        service = new DashManifestGeneratorService();
    }

    // -- XML helpers ------------------------------------------------------

    /** Parses the manifest, failing the test with the manifest text if it isn't well-formed. */
    private Document parse(String manifest) {
        try {
            return DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(manifest)));
        } catch (Exception e) {
            throw new AssertionError("Generated manifest is not well-formed XML:\n" + manifest, e);
        }
    }

    private Document generate(DashManifestConfigDTO config) {
        return parse(service.generateManifestXml(config));
    }

    private List<Element> elements(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        return IntStream.range(0, nodes.getLength())
                .mapToObj(i -> (Element) nodes.item(i))
                .toList();
    }

    private List<Element> childElements(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        return IntStream.range(0, nodes.getLength())
                .mapToObj(i -> (Element) nodes.item(i))
                .toList();
    }

    private Element only(List<Element> list) {
        assertThat(list).hasSize(1);
        return list.getFirst();
    }

    private String baseUrl(Element representation) {
        return only(childElements(representation, "BaseURL")).getTextContent();
    }

    private List<Element> setsOfType(Document doc, String contentType) {
        return elements(doc, "AdaptationSet").stream()
                .filter(set -> contentType.equals(set.getAttribute("contentType")))
                .toList();
    }

    private String role(Element adaptationSet) {
        return only(childElements(adaptationSet, "Role")).getAttribute("value");
    }

    private String label(Element adaptationSet) {
        return only(childElements(adaptationSet, "Label")).getTextContent();
    }

    // -- Fixture factories ------------------------------------------------

    private static DashManifestConfigDTO createBasicConfig() {
        DashManifestConfigDTO config = new DashManifestConfigDTO();
        config.setType("static");
        config.setMediaPresentationDuration("PT2M");
        config.setMinBufferTime("PT2S");
        config.setProfiles("urn:mpeg:dash:profile:isoff-on-demand:2011");
        config.setDurationSeconds(120);
        config.setVideoStreams(new ArrayList<>());
        config.setAudioStreams(new ArrayList<>());
        config.setSubtitleStreams(new ArrayList<>());
        return config;
    }

    private static DashManifestConfigDTO configWith(List<VideoStreamMetadataDTO> video,
                                                    List<AudioStreamMetadataDTO> audio,
                                                    List<SubtitleMetadataDTO> subtitles) {
        DashManifestConfigDTO config = createBasicConfig();
        config.setVideoStreams(new ArrayList<>(video));
        config.setAudioStreams(new ArrayList<>(audio));
        config.setSubtitleStreams(new ArrayList<>(subtitles));
        return config;
    }

    /** Smallest valid manifest: one video and one audio representation. */
    private static DashManifestConfigDTO minimalConfig() {
        return configWith(List.of(defaultVideo()), List.of(defaultAudio()), List.of());
    }

    private static VideoStreamMetadataDTO defaultVideo() {
        return createVideoStream("video-1", 1920, 1080, 3423702);
    }

    private static AudioStreamMetadataDTO defaultAudio() {
        return createAudioStream("audio-1", "en", 130482);
    }

    private static VideoStreamMetadataDTO createVideoStream(String id, int width, int height, int bandwidth) {
        return createVideoStream(id, "video/mp4", "avc1.640028", width, height, bandwidth);
    }

    /** Playable by default: byte ranges are always set, as the filter guarantees. */
    private static VideoStreamMetadataDTO createVideoStream(String id, String mimeType, String codec,
                                                            int width, int height, int bandwidth) {
        return VideoStreamMetadataDTO.builder()
                .id(id)
                .url("https://example.com/" + id)
                .codec(codec)
                .mimeType(mimeType)
                .width(width)
                .height(height)
                .frameRate("24")
                .bandwidth(bandwidth)
                .initRange("0-740")
                .indexRange("741-1048")
                .build();
    }

    private static AudioStreamMetadataDTO createAudioStream(String id, String language, int bandwidth) {
        return createAudioStream(id, language, "audio/mp4", "mp4a.40.2", bandwidth);
    }

    /** Playable by default: byte ranges are always set, as the filter guarantees. */
    private static AudioStreamMetadataDTO createAudioStream(String id, String language, String mimeType,
                                                            String codec, int bandwidth) {
        return AudioStreamMetadataDTO.builder()
                .id(id)
                .url("https://example.com/" + id)
                .codec(codec)
                .mimeType(mimeType)
                .bandwidth(bandwidth)
                .audioSamplingRate("44100")
                .audioChannels(2)
                .language(language)
                .languageName(ManifestXmlBuilder.getLanguageName(language))
                .initRange("0-600")
                .indexRange("601-900")
                .build();
    }

    /** Marks an audio stream as part of a YouTube multi-track video. */
    private static AudioStreamMetadataDTO inTrack(AudioStreamMetadataDTO audio, String trackId,
                                                  AudioTrackType type, String trackName) {
        audio.setAudioTrackId(trackId);
        audio.setAudioTrackType(type);
        audio.setAudioTrackName(trackName);
        return audio;
    }

    private static SubtitleMetadataDTO createSubtitleStream(String id, String language, String kind) {
        return SubtitleMetadataDTO.builder()
                .id(id)
                .url("https://example.com/" + id + ".vtt")
                .language(language)
                .languageName(ManifestXmlBuilder.getLanguageName(language))
                .mimeType("text/vtt")
                .kind(kind)
                .bandwidth(256)
                .build();
    }

    private static Supplier<DashManifestConfigDTO> mutated(Supplier<DashManifestConfigDTO> base,
                                                           Consumer<DashManifestConfigDTO> mutation) {
        return () -> {
            DashManifestConfigDTO config = base.get();
            mutation.accept(config);
            return config;
        };
    }

    // -- Validation -------------------------------------------------------

    static Stream<Arguments> invalidConfigs() {
        Supplier<DashManifestConfigDTO> base = DashManifestGeneratorServiceTest::minimalConfig;
        return Stream.of(
                Arguments.of("null config", (Supplier<DashManifestConfigDTO>) () -> null),
                Arguments.of("zero duration", mutated(base, c -> c.setDurationSeconds(0))),
                Arguments.of("negative duration", mutated(base, c -> c.setDurationSeconds(-10))),
                Arguments.of("null video streams", mutated(base, c -> c.setVideoStreams(null))),
                Arguments.of("null audio streams", mutated(base, c -> c.setAudioStreams(null))),
                Arguments.of("null subtitle streams", mutated(base, c -> c.setSubtitleStreams(null))));
    }

    @ParameterizedTest(name = "Rejects {0}")
    @MethodSource("invalidConfigs")
    @DisplayName("Rejects malformed configurations with ValidationException")
    void rejectsInvalidConfigs(String name, Supplier<DashManifestConfigDTO> configSupplier) {
        assertThatThrownBy(() -> service.generateManifestXml(configSupplier.get()))
                .isInstanceOf(ValidationException.class);
    }

    static Stream<Arguments> contractViolations() {
        Supplier<DashManifestConfigDTO> base = DashManifestGeneratorServiceTest::minimalConfig;
        String noAdaptive = "at least one video and one audio";
        String missingField = "missing a required field";
        return Stream.of(
                Arguments.of("no video streams",
                        (Supplier<DashManifestConfigDTO>) () ->
                                configWith(List.of(), List.of(defaultAudio()), List.of()),
                        noAdaptive),
                Arguments.of("no audio streams",
                        (Supplier<DashManifestConfigDTO>) () ->
                                configWith(List.of(defaultVideo()), List.of(), List.of()),
                        noAdaptive),
                Arguments.of("video without indexRange",
                        mutated(base, c -> c.getVideoStreams().getFirst().setIndexRange(null)), missingField),
                Arguments.of("video with blank initRange",
                        mutated(base, c -> c.getVideoStreams().getFirst().setInitRange(" ")), missingField),
                Arguments.of("video without codec",
                        mutated(base, c -> c.getVideoStreams().getFirst().setCodec(null)), missingField),
                Arguments.of("audio without mimeType",
                        mutated(base, c -> c.getAudioStreams().getFirst().setMimeType(null)), missingField),
                Arguments.of("audio with blank url",
                        mutated(base, c -> c.getAudioStreams().getFirst().setUrl("")), missingField),
                Arguments.of("subtitle without mimeType",
                        mutated(base, c -> {
                            SubtitleMetadataDTO subtitle = createSubtitleStream("subtitle-1", "en", "subtitles");
                            subtitle.setMimeType(null);
                            c.getSubtitleStreams().add(subtitle);
                        }), missingField));
    }

    @ParameterizedTest(name = "Rejects {0}")
    @MethodSource("contractViolations")
    @DisplayName("Rejects configs the playability filter should have prevented, with IllegalStateException")
    void rejectsContractViolations(String name, Supplier<DashManifestConfigDTO> configSupplier,
                                   String expectedMessage) {
        assertThatThrownBy(() -> service.generateManifestXml(configSupplier.get()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(expectedMessage);
    }

    // -- Document structure -----------------------------------------------

    @Test
    @DisplayName("MPD root and Period carry the configuration attributes")
    void mpdHeaderAndPeriod_carryConfigAttributes() {
        String manifest = service.generateManifestXml(minimalConfig());

        assertThat(manifest).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        Document doc = parse(manifest);
        Element mpd = doc.getDocumentElement();
        assertThat(mpd.getTagName()).isEqualTo("MPD");
        assertThat(mpd.getAttribute("xmlns")).isEqualTo("urn:mpeg:dash:schema:mpd:2011");
        assertThat(mpd.getAttribute("type")).isEqualTo("static");
        assertThat(mpd.getAttribute("mediaPresentationDuration")).isEqualTo("PT2M");
        assertThat(mpd.getAttribute("minBufferTime")).isEqualTo("PT2S");
        assertThat(mpd.getAttribute("profiles")).isEqualTo("urn:mpeg:dash:profile:isoff-on-demand:2011");

        Element period = only(elements(doc, "Period"));
        assertThat(period.getAttribute("duration")).isEqualTo("PT2M");
    }

    @Test
    @DisplayName("Writes SegmentBase on every video and audio representation")
    void writesSegmentBaseOnEveryAdaptiveRepresentation() {
        DashManifestConfigDTO config = configWith(
                List.of(createVideoStream("video-1080", 1920, 1080, 3_000_000),
                        createVideoStream("video-720", 1280, 720, 1_200_000)),
                List.of(createAudioStream("audio-128", "en", 128_000),
                        createAudioStream("audio-64", "en", 64_000)),
                List.of());

        Document doc = generate(config);

        assertThat(elements(doc, "Representation")).hasSize(4)
                .allSatisfy(rep -> {
                    Element segmentBase = only(childElements(rep, "SegmentBase"));
                    assertThat(segmentBase.getAttribute("indexRange")).isNotBlank();
                    assertThat(only(childElements(segmentBase, "Initialization")).getAttribute("range"))
                            .isNotBlank();
                });
    }

    @Test
    @DisplayName("Produces identical output regardless of input order across groups")
    void outputIsDeterministic() {
        VideoStreamMetadataDTO avc = createVideoStream("video-avc", "video/mp4", "avc1.640028", 1920, 1080, 3_000_000);
        VideoStreamMetadataDTO vp9 = createVideoStream("video-vp9", "video/webm", "vp9", 1920, 1080, 2_000_000);
        AudioStreamMetadataDTO aac = createAudioStream("audio-aac", "en", "audio/mp4", "mp4a.40.2", 128_000);
        AudioStreamMetadataDTO opus = createAudioStream("audio-opus", "en", "audio/webm", "opus", 160_000);

        String forward = service.generateManifestXml(configWith(List.of(avc, vp9), List.of(aac, opus), List.of()));
        String reversed = service.generateManifestXml(configWith(List.of(vp9, avc), List.of(opus, aac), List.of()));

        assertThat(reversed).isEqualTo(forward);
    }

    // -- Video ------------------------------------------------------------

    @Nested
    @DisplayName("Video AdaptationSets")
    class VideoAdaptationSetTests {

        @Test
        @DisplayName("Sorts representations by height descending and carries full attributes")
        void sortsByHeightWithFullAttributes() {
            // Deliberately out of order
            DashManifestConfigDTO config = configWith(List.of(
                            createVideoStream("video-360", 640, 360, 467558),
                            createVideoStream("video-1080", 1920, 1080, 3423702),
                            createVideoStream("video-720", 1280, 720, 1215044)),
                    List.of(defaultAudio()), List.of());

            Document doc = generate(config);

            Element set = only(setsOfType(doc, "video"));
            assertThat(set.getAttribute("id")).isEqualTo("0");
            assertThat(set.getAttribute("mimeType")).isEqualTo("video/mp4");
            assertThat(set.getAttribute("subsegmentAlignment")).isEqualTo("true");
            assertThat(set.getAttribute("startWithSAP")).isEqualTo("1");

            List<Element> reps = childElements(set, "Representation");
            assertThat(reps).extracting(r -> r.getAttribute("height"))
                    .containsExactly("1080", "720", "360");

            Element top = reps.getFirst();
            assertThat(top.getAttribute("id")).isEqualTo("video-1080");
            assertThat(top.getAttribute("bandwidth")).isEqualTo("3423702");
            assertThat(top.getAttribute("codecs")).isEqualTo("avc1.640028");
            assertThat(top.getAttribute("width")).isEqualTo("1920");
            assertThat(top.getAttribute("frameRate")).isEqualTo("24");
            assertThat(baseUrl(top)).isEqualTo("https://example.com/video-1080");
        }

        @Test
        @DisplayName("Splits into one AdaptationSet per container and codec family, in sorted key order")
        void splitsByContainerAndCodecFamily() {
            DashManifestConfigDTO config = configWith(List.of(
                            createVideoStream("video-avc-1080", "video/mp4", "avc1.640028", 1920, 1080, 3_000_000),
                            createVideoStream("video-vp9-1080", "video/webm", "vp9", 1920, 1080, 2_000_000),
                            createVideoStream("video-avc-720", "video/mp4", "avc1.4d401f", 1280, 720, 1_200_000),
                            createVideoStream("video-av1-1080", "video/mp4", "av01.0.08M.08", 1920, 1080, 1_500_000)),
                    List.of(defaultAudio()), List.of());

            List<Element> sets = setsOfType(generate(config), "video");

            // Keys: video/mp4|av01 < video/mp4|avc1 < video/webm|vp9
            assertThat(sets).extracting(s -> s.getAttribute("mimeType"))
                    .containsExactly("video/mp4", "video/mp4", "video/webm");
            assertThat(childElements(sets.get(0), "Representation"))
                    .extracting(r -> r.getAttribute("id")).containsExactly("video-av1-1080");
            assertThat(childElements(sets.get(1), "Representation"))
                    .extracting(r -> r.getAttribute("id")).containsExactly("video-avc-1080", "video-avc-720");
            assertThat(childElements(sets.get(2), "Representation"))
                    .extracting(r -> r.getAttribute("id")).containsExactly("video-vp9-1080");
        }

        @Test
        @DisplayName("Omits frameRate when it is unknown instead of writing \"null\"")
        void omitsUnknownFrameRate() {
            VideoStreamMetadataDTO video = defaultVideo();
            video.setFrameRate(null);

            Document doc = generate(configWith(List.of(video), List.of(defaultAudio()), List.of()));

            Element rep = only(childElements(only(setsOfType(doc, "video")), "Representation"));
            assertThat(rep.hasAttribute("frameRate")).isFalse();
        }

        @Test
        @DisplayName("Escapes user-provided content so values round-trip through a real XML parser")
        void escapesUserProvidedContent() {
            VideoStreamMetadataDTO video = createVideoStream("video-<test>", 1920, 1080, 3000000);
            video.setUrl("https://example.com/video?a=1&b=2");
            video.setCodec("avc1.640028 & more");

            // parse() failing IS the escaping test; round-tripping the
            // original values proves escaping is correct, not just present.
            Document doc = generate(configWith(List.of(video), List.of(defaultAudio()), List.of()));

            Element rep = only(childElements(only(setsOfType(doc, "video")), "Representation"));
            assertThat(rep.getAttribute("id")).isEqualTo("video-<test>");
            assertThat(rep.getAttribute("codecs")).isEqualTo("avc1.640028 & more");
            assertThat(baseUrl(rep)).isEqualTo("https://example.com/video?a=1&b=2");
        }
    }

    // -- Audio ------------------------------------------------------------

    @Nested
    @DisplayName("Audio AdaptationSets")
    class AudioAdaptationSetTests {

        @Test
        @DisplayName("Splits one track by container, so no set mixes MP4/AAC with WebM/Opus")
        void splitsOneTrackByContainer() {
            DashManifestConfigDTO config = configWith(List.of(defaultVideo()), List.of(
                    createAudioStream("audio-aac", "en", "audio/mp4", "mp4a.40.2", 128_000),
                    createAudioStream("audio-opus", "en", "audio/webm", "opus", 160_000)), List.of());

            List<Element> sets = setsOfType(generate(config), "audio");

            assertThat(sets).extracting(s -> s.getAttribute("mimeType"))
                    .containsExactly("audio/mp4", "audio/webm");
            assertThat(sets).allSatisfy(set -> {
                assertThat(childElements(set, "Representation")).hasSize(1);
                assertThat(set.getAttribute("lang")).isEqualTo("en");
                assertThat(role(set)).isEqualTo("main");
                assertThat(label(set)).isEqualTo("English");
            });
        }

        @Test
        @DisplayName("Keeps original, dubbed and descriptive tracks apart, with matching Roles")
        void separatesTracksWithRoles() {
            DashManifestConfigDTO config = configWith(List.of(defaultVideo()), List.of(
                    inTrack(createAudioStream("audio-en", "en", 128_000),
                            "en.4", AudioTrackType.ORIGINAL, "English original"),
                    inTrack(createAudioStream("audio-de", "de", 128_000),
                            "de.3", AudioTrackType.DUBBED, "German"),
                    inTrack(createAudioStream("audio-en-ad", "en", 96_000),
                            "en.5", AudioTrackType.DESCRIPTIVE, "English descriptive")), List.of());

            List<Element> sets = setsOfType(generate(config), "audio");

            // Two English tracks are not merged just because they share a language
            assertThat(sets).hasSize(3);
            Map<String, String> roleByLabel = sets.stream()
                    .collect(Collectors.toMap(DashManifestGeneratorServiceTest.this::label, DashManifestGeneratorServiceTest.this::role));
            assertThat(roleByLabel)
                    .containsEntry("English original", "main")
                    .containsEntry("German", "dub")
                    .containsEntry("English descriptive", "description");
        }

        @Test
        @DisplayName("Uses Role 'alternate' for secondary tracks and for untyped tracks when there are several")
        void usesAlternateRoleWhenNotIdentifiable() {
            DashManifestConfigDTO config = configWith(List.of(defaultVideo()), List.of(
                            inTrack(createAudioStream("audio-en", "en", 128_000), "en.1", null, "English"),
                            inTrack(createAudioStream("audio-fr", "fr", 128_000), "fr.2", AudioTrackType.SECONDARY, "French")),
                    List.of());

            List<Element> sets = setsOfType(generate(config), "audio");

            assertThat(sets).hasSize(2)
                    .allSatisfy(set -> assertThat(role(set)).isEqualTo("alternate"));
        }

        @ParameterizedTest(name = "trackName={0}, languageName={1} -> {2}")
        @CsvSource({
                "'German (Germany) original', Deutsch, 'German (Germany) original'", // track name wins
                ",                            Deutsch, Deutsch",                     // then language name
                ",                            ,        German"                       // then lookup by code
        })
        @DisplayName("Writes a Label element from track name, then language name, then a code lookup")
        void writesLabelElementWithFallbacks(String trackName, String languageName, String expected) {
            AudioStreamMetadataDTO audio = createAudioStream("audio-1", "de", 128_000);
            audio.setAudioTrackName(trackName);
            audio.setLanguageName(languageName);

            Element set = only(setsOfType(
                    generate(configWith(List.of(defaultVideo()), List.of(audio), List.of())), "audio"));

            assertThat(label(set)).isEqualTo(expected);
            assertThat(set.hasAttribute("label")).isFalse();
        }

        @Test
        @DisplayName("Uses 'und' when the language is unknown")
        void usesUndForUnknownLanguage() {
            AudioStreamMetadataDTO audio = createAudioStream("audio-1", null, 128_000);

            Element set = only(setsOfType(
                    generate(configWith(List.of(defaultVideo()), List.of(audio), List.of())), "audio"));

            assertThat(set.getAttribute("lang")).isEqualTo("und");
        }

        @Test
        @DisplayName("Labels a single track with no name or language 'Original'")
        void labelsUntaggedMainTrackOriginal() {
            AudioStreamMetadataDTO audio = createAudioStream("audio-1", null, 128_000);

            Element set = only(setsOfType(
                    generate(configWith(List.of(defaultVideo()), List.of(audio), List.of())), "audio"));

            assertThat(role(set)).isEqualTo("main");
            assertThat(label(set)).isEqualTo("Original");
        }

        @Test
        @DisplayName("Labels a non-main track with no name or language 'Unknown'")
        void labelsUntaggedNonMainTrackUnknown() {
            AudioStreamMetadataDTO audio = inTrack(createAudioStream("audio-1", null, 128_000),
                    "und.3", AudioTrackType.DUBBED, null);

            Element set = only(setsOfType(
                    generate(configWith(List.of(defaultVideo()), List.of(audio), List.of())), "audio"));

            assertThat(role(set)).isEqualTo("dub");
            assertThat(label(set)).isEqualTo("Unknown");
        }

        @Test
        @DisplayName("Keeps YouTube's track name even when the language is unknown")
        void keepsTrackNameWhenLanguageUnknown() {
            AudioStreamMetadataDTO audio = inTrack(createAudioStream("audio-1", null, 128_000),
                    "en.4", AudioTrackType.ORIGINAL, "English original");

            Element set = only(setsOfType(
                    generate(configWith(List.of(defaultVideo()), List.of(audio), List.of())), "audio"));

            assertThat(role(set)).isEqualTo("main");
            assertThat(label(set)).isEqualTo("English original");
        }

        @Test
        @DisplayName("Sorts representations by bandwidth descending with full attributes, channel config, and SegmentBase")
        void sortsByBandwidthWithFullAttributes() {
            AudioStreamMetadataDTO top = createAudioStream("audio-256", "en", 256000);
            top.setInitRange("0-640");
            top.setIndexRange("641-900");
            AudioStreamMetadataDTO mid = createAudioStream("audio-128", "en", 128000);
            AudioStreamMetadataDTO low = createAudioStream("audio-64", "en", 64000);

            Document doc = generate(configWith(List.of(defaultVideo()), List.of(low, top, mid), List.of()));

            Element set = only(setsOfType(doc, "audio"));
            assertThat(set.getAttribute("id")).isEqualTo("1"); // continues after the video set
            List<Element> reps = childElements(set, "Representation");
            assertThat(reps).extracting(r -> r.getAttribute("bandwidth"))
                    .containsExactly("256000", "128000", "64000");

            Element rep = reps.getFirst();
            assertThat(rep.getAttribute("id")).isEqualTo("audio-256");
            assertThat(rep.getAttribute("codecs")).isEqualTo("mp4a.40.2");
            assertThat(rep.getAttribute("audioSamplingRate")).isEqualTo("44100");
            assertThat(baseUrl(rep)).isEqualTo("https://example.com/audio-256");

            Element channelConfig = only(childElements(rep, "AudioChannelConfiguration"));
            assertThat(channelConfig.getAttribute("schemeIdUri"))
                    .isEqualTo("urn:mpeg:dash:23003:3:audio_channel_configuration:2011");
            assertThat(channelConfig.getAttribute("value")).isEqualTo("2");

            Element segmentBase = only(childElements(rep, "SegmentBase"));
            assertThat(segmentBase.getAttribute("indexRange")).isEqualTo("641-900");
            assertThat(only(childElements(segmentBase, "Initialization")).getAttribute("range"))
                    .isEqualTo("0-640");
        }

        @Test
        @DisplayName("Omits audioSamplingRate when it is unknown instead of writing \"null\"")
        void omitsUnknownSamplingRate() {
            AudioStreamMetadataDTO audio = defaultAudio();
            audio.setAudioSamplingRate(null);

            Document doc = generate(configWith(List.of(defaultVideo()), List.of(audio), List.of()));

            Element rep = only(childElements(only(setsOfType(doc, "audio")), "Representation"));
            assertThat(rep.hasAttribute("audioSamplingRate")).isFalse();
        }
    }

    // -- Subtitles --------------------------------------------------------

    @Nested
    @DisplayName("Subtitle AdaptationSets")
    class SubtitleAdaptationSetTests {

        @Test
        @DisplayName("Creates one AdaptationSet per subtitle in input order, with ids after the media sets")
        void onePerSubtitleInInputOrder() {
            DashManifestConfigDTO config = configWith(List.of(defaultVideo()), List.of(defaultAudio()), List.of(
                    createSubtitleStream("subtitle-es", "es", "subtitles"),
                    createSubtitleStream("subtitle-en", "en", "subtitles")));

            List<Element> sets = setsOfType(generate(config), "text");

            assertThat(sets).extracting(s -> s.getAttribute("lang"))
                    .containsExactly("es", "en"); // no server-side language ordering
            assertThat(sets).extracting(s -> s.getAttribute("id"))
                    .containsExactly("2", "3");   // after video set 0 and audio set 1

            Element set = sets.get(1);
            assertThat(set.getAttribute("mimeType")).isEqualTo("text/vtt");
            assertThat(only(childElements(set, "Role")).getAttribute("schemeIdUri"))
                    .isEqualTo("urn:mpeg:dash:role:2011");
            assertThat(label(set)).isEqualTo("English");

            Element rep = only(childElements(set, "Representation"));
            assertThat(rep.getAttribute("id")).isEqualTo("subtitle-en");
            assertThat(rep.getAttribute("bandwidth")).isEqualTo("256");
            assertThat(baseUrl(rep)).isEqualTo("https://example.com/subtitle-en.vtt");
            assertThat(childElements(rep, "SegmentBase")).isEmpty();
        }

        @ParameterizedTest(name = "kind {0} -> Role {1}")
        @CsvSource({
                "subtitles, subtitle",
                "asr,       subtitle",
                "captions,  caption",
                ",          subtitle"
        })
        @DisplayName("Maps kind to the singular DASH role values")
        void mapsKindToSingularRole(String kind, String expectedRole) {
            DashManifestConfigDTO config = configWith(List.of(defaultVideo()), List.of(defaultAudio()),
                    List.of(createSubtitleStream("subtitle-1", "en", kind)));

            Element set = only(setsOfType(generate(config), "text"));

            assertThat(role(set)).isEqualTo(expectedRole);
        }

        @Test
        @DisplayName("Keeps manual and auto-generated tracks, labelling the auto-generated one")
        void labelsAutoGeneratedTracks() {
            DashManifestConfigDTO config = configWith(List.of(defaultVideo()), List.of(defaultAudio()), List.of(
                    createSubtitleStream("subtitle-manual", "en", "subtitles"),
                    createSubtitleStream("subtitle-auto", "en", "asr")));

            List<Element> sets = setsOfType(generate(config), "text");

            assertThat(sets).extracting(DashManifestGeneratorServiceTest.this::label)
                    .containsExactly("English", "English (auto-generated)");
        }
    }

    // -- Whole manifest ---------------------------------------------------

    @Test
    @DisplayName("Generates a well-formed manifest with all stream types and unique sequential set ids")
    void generatesCompleteManifestWithAllStreamTypes() {
        DashManifestConfigDTO config = configWith(
                List.of(createVideoStream("video-1", 1920, 1080, 3423702),
                        createVideoStream("video-2", 1280, 720, 1215044)),
                List.of(createAudioStream("audio-1", "en", 130482),
                        createAudioStream("audio-2", "es", 128000)),
                List.of(createSubtitleStream("subtitle-1", "en", "subtitles"),
                        createSubtitleStream("subtitle-2", "es", "subtitles")));

        Document doc = generate(config);

        assertThat(setsOfType(doc, "video")).hasSize(1);
        assertThat(setsOfType(doc, "audio")).hasSize(2);
        assertThat(setsOfType(doc, "text")).hasSize(2);
        assertThat(elements(doc, "AdaptationSet")).extracting(s -> s.getAttribute("id"))
                .containsExactly("0", "1", "2", "3", "4");
    }
}