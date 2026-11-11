package org.example.api.dto.dash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.VideoStream;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit tests for VideoStreamMetadataDTO.
 */
@DisplayName("VideoStreamMetadataDTO Tests")
class VideoStreamMetadataDTOTest {

    private Validator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        objectMapper = new ObjectMapper();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static VideoStreamMetadataDTO.Builder validBuilder() {
        return VideoStreamMetadataDTO.builder()
                .id("video-1")
                .url("https://example.com/video.mp4")
                .codec("avc1.640028")
                .mimeType("video/mp4")
                .width(1920)
                .height(1080)
                .frameRate("24")
                .bandwidth(3423702);
    }

    // ── Builder ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Builder wires every field to its own getter")
    void builderWiresAllFields() {
        VideoStreamMetadataDTO dto = validBuilder()
                .initRange("0-740")
                .indexRange("741-1048")
                .format("MPEG_4")
                .build();

        assertThat(dto.id()).isEqualTo("video-1");
        assertThat(dto.url()).isEqualTo("https://example.com/video.mp4");
        assertThat(dto.codec()).isEqualTo("avc1.640028");
        assertThat(dto.mimeType()).isEqualTo("video/mp4");
        assertThat(dto.width()).isEqualTo(1920);
        assertThat(dto.height()).isEqualTo(1080);
        assertThat(dto.frameRate()).isEqualTo("24");
        assertThat(dto.bandwidth()).isEqualTo(3423702);
        assertThat(dto.initRange()).isEqualTo("0-740");
        assertThat(dto.indexRange()).isEqualTo("741-1048");
        assertThat(dto.format()).isEqualTo("MPEG_4");
    }

    // ── Validation ───────────────────────────────────────────────────────

    @Test
    @DisplayName("A fully-populated DTO passes validation")
    void validDtoPassesValidation() {
        assertThat(validator.validate(validBuilder().build())).isEmpty();
    }

    static Stream<Arguments> invalidFieldCases() {
        return Stream.of(
                Arguments.of(
                        "blank id",
                        (UnaryOperator<VideoStreamMetadataDTO.Builder>) b -> b.id(""),
                        "Video stream ID cannot be blank"),
                Arguments.of(
                        "blank url",
                        (UnaryOperator<VideoStreamMetadataDTO.Builder>) b -> b.url(""),
                        "Video stream URL cannot be blank"),
                Arguments.of(
                        "blank codec",
                        (UnaryOperator<VideoStreamMetadataDTO.Builder>) b -> b.codec(""),
                        "Video codec cannot be blank"),
                Arguments.of(
                        "blank mimeType",
                        (UnaryOperator<VideoStreamMetadataDTO.Builder>) b -> b.mimeType(""),
                        "Video MIME type cannot be blank"),
                Arguments.of(
                        "blank frameRate",
                        (UnaryOperator<VideoStreamMetadataDTO.Builder>) b -> b.frameRate(""),
                        "Frame rate cannot be blank"),
                Arguments.of(
                        "zero width",
                        (UnaryOperator<VideoStreamMetadataDTO.Builder>) b -> b.width(0),
                        "Video width must be at least 1"),
                Arguments.of(
                        "zero height",
                        (UnaryOperator<VideoStreamMetadataDTO.Builder>) b -> b.height(0),
                        "Video height must be at least 1"),
                Arguments.of(
                        "zero bandwidth",
                        (UnaryOperator<VideoStreamMetadataDTO.Builder>) b -> b.bandwidth(0),
                        "Bandwidth must be at least 1"));
    }

    @ParameterizedTest(name = "Rejects {0}")
    @MethodSource("invalidFieldCases")
    @DisplayName("Rejects DTOs violating a single constraint, with the matching message")
    void rejectsInvalidField(
            String name, UnaryOperator<VideoStreamMetadataDTO.Builder> mutator, String expectedMessage) {
        Set<ConstraintViolation<VideoStreamMetadataDTO>> violations =
                validator.validate(mutator.apply(validBuilder()).build());

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains(expectedMessage));
    }

    // ── from() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Maps every field from a fully-populated VideoStream, including both segment ranges")
    void mapsAllFieldsFromValidStream() {
        VideoStream stream = mock(VideoStream.class);
        MediaFormat format = mock(MediaFormat.class);
        when(stream.getContent()).thenReturn("https://example.com/video.mp4");
        when(stream.getCodec()).thenReturn("avc1.640028");
        when(stream.getFormat()).thenReturn(format);
        when(format.getMimeType()).thenReturn("video/mp4");
        when(format.getName()).thenReturn("MPEG_4");
        when(stream.getWidth()).thenReturn(1920);
        when(stream.getHeight()).thenReturn(1080);
        when(stream.getFps()).thenReturn(24);
        when(stream.getBitrate()).thenReturn(3423702);
        when(stream.getInitStart()).thenReturn(0);
        when(stream.getInitEnd()).thenReturn(740);
        when(stream.getIndexStart()).thenReturn(741);
        when(stream.getIndexEnd()).thenReturn(1048);

        VideoStreamMetadataDTO dto = VideoStreamMetadataDTO.from(stream, 1);

        assertThat(dto.id()).isEqualTo("video-1");
        assertThat(dto.url()).isEqualTo("https://example.com/video.mp4");
        assertThat(dto.width()).isEqualTo(1920);
        assertThat(dto.height()).isEqualTo(1080);
        assertThat(dto.frameRate()).isEqualTo("24");
        assertThat(dto.bandwidth()).isEqualTo(3423702);
        assertThat(dto.initRange()).isEqualTo("0-740");
        assertThat(dto.indexRange()).isEqualTo("741-1048");
        assertThat(dto.format()).isEqualTo("MPEG_4");
    }

    @Test
    @DisplayName("Rejects a null VideoStream with IllegalArgumentException")
    void rejectsNullVideoStream() {
        assertThatThrownBy(() -> VideoStreamMetadataDTO.from(null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VideoStream cannot be null");
    }

    @Test
    @DisplayName("Defaults mimeType to null when the stream's format is null")
    void handlesNullFormat() {
        VideoStream stream = mock(VideoStream.class);
        when(stream.getContent()).thenReturn("https://example.com/video.mp4");
        when(stream.getCodec()).thenReturn("avc1.640028");
        when(stream.getFormat()).thenReturn(null);
        when(stream.getWidth()).thenReturn(1920);
        when(stream.getHeight()).thenReturn(1080);
        when(stream.getFps()).thenReturn(24);
        when(stream.getBitrate()).thenReturn(3423702);
        when(stream.getInitStart()).thenReturn(-1);
        when(stream.getIndexStart()).thenReturn(-1);

        VideoStreamMetadataDTO dto = VideoStreamMetadataDTO.from(stream, 2);

        assertThat(dto.mimeType()).isNull();
        assertThat(dto.format()).isNull();
    }

    @Test
    @DisplayName("Omits both segment ranges when unavailable, using index 10 to also prove sequential-id generation")
    void omitsRangesWhenNotAvailable() {
        VideoStream stream = mock(VideoStream.class);
        MediaFormat format = mock(MediaFormat.class);
        when(stream.getContent()).thenReturn("https://example.com/video.mp4");
        when(stream.getCodec()).thenReturn("avc1.640028");
        when(stream.getFormat()).thenReturn(format);
        when(format.getMimeType()).thenReturn("video/mp4");
        when(stream.getWidth()).thenReturn(1920);
        when(stream.getHeight()).thenReturn(1080);
        when(stream.getFps()).thenReturn(24);
        when(stream.getBitrate()).thenReturn(3423702);
        when(stream.getInitStart()).thenReturn(-1);
        when(stream.getInitEnd()).thenReturn(0);
        when(stream.getIndexStart()).thenReturn(-1);
        when(stream.getIndexEnd()).thenReturn(0);

        VideoStreamMetadataDTO dto = VideoStreamMetadataDTO.from(stream, 10);

        assertThat(dto.id()).isEqualTo("video-10");
        assertThat(dto.initRange()).isNull();
        assertThat(dto.indexRange()).isNull();
    }

    // ── JSON ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Omits unset optional fields from the serialized JSON")
    void omitsNullFieldsInJson() throws Exception {
        VideoStreamMetadataDTO dto = validBuilder().build(); // no initRange/indexRange/format

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).doesNotContain("\"initRange\"", "\"indexRange\"", "\"format\"");
    }

    @Test
    @DisplayName("Round-trips every field through serialize/deserialize unchanged")
    void roundTripPreservesAllFields() throws Exception {
        VideoStreamMetadataDTO original = validBuilder()
                .initRange("0-740")
                .indexRange("741-1048")
                .format("MPEG_4")
                .build();

        VideoStreamMetadataDTO restored =
                objectMapper.readValue(objectMapper.writeValueAsString(original), VideoStreamMetadataDTO.class);

        assertThat(restored.id()).isEqualTo(original.id());
        assertThat(restored.url()).isEqualTo(original.url());
        assertThat(restored.codec()).isEqualTo(original.codec());
        assertThat(restored.mimeType()).isEqualTo(original.mimeType());
        assertThat(restored.width()).isEqualTo(original.width());
        assertThat(restored.height()).isEqualTo(original.height());
        assertThat(restored.frameRate()).isEqualTo(original.frameRate());
        assertThat(restored.bandwidth()).isEqualTo(original.bandwidth());
        assertThat(restored.initRange()).isEqualTo(original.initRange());
        assertThat(restored.indexRange()).isEqualTo(original.indexRange());
        assertThat(restored.format()).isEqualTo(original.format());
    }

    // ── toString ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString includes the key identifying fields")
    void toStringIncludesKeyFields() {
        String result = validBuilder().build().toString();

        assertThat(result).contains("video-1", "1920", "1080", "24", "3423702", "avc1.640028");
    }
}
