package org.example.api.dto.dash;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.services.youtube.ItagItem;
import org.schabi.newpipe.extractor.stream.AudioStream;

import java.util.Locale;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AudioStreamMetadataDTO.
 */
@DisplayName("AudioStreamMetadataDTO Tests")
class AudioStreamMetadataDTOTest {

    private Validator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        objectMapper = new ObjectMapper();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static AudioStreamMetadataDTO.Builder validBuilder() {
        return AudioStreamMetadataDTO.builder()
                .id("audio-1")
                .url("https://example.com/audio.mp4")
                .codec("mp4a.40.2")
                .mimeType("audio/mp4")
                .bandwidth(130482)
                .audioSamplingRate("44100")
                .audioChannels(2)
                .language("en")
                .languageName("English");
    }

    private AudioStream audioStream(Locale locale, int audioChannels) {
        AudioStream stream = mock(AudioStream.class);
        MediaFormat format = mock(MediaFormat.class);
        ItagItem itagItem = mock(ItagItem.class);
        when(stream.getContent()).thenReturn("https://example.com/audio.mp4");
        when(stream.getCodec()).thenReturn("mp4a.40.2");
        when(stream.getFormat()).thenReturn(format);
        when(format.getMimeType()).thenReturn("audio/mp4");
        when(stream.getBitrate()).thenReturn(130482);
        when(stream.getItagItem()).thenReturn(itagItem);
        when(itagItem.getSampleRate()).thenReturn(44100);
        when(itagItem.getAudioChannels()).thenReturn(audioChannels);
        when(stream.getAudioLocale()).thenReturn(locale);
        when(stream.getInitStart()).thenReturn(-1);
        when(stream.getIndexStart()).thenReturn(-1);
        return stream;
    }

    // ── Builder ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Builder wires every field to its own getter")
    void builderWiresAllFields() {
        AudioStreamMetadataDTO dto = validBuilder()
                .initRange("0-722").indexRange("723-898").format("M4A")
                .build();

        assertThat(dto.getId()).isEqualTo("audio-1");
        assertThat(dto.getUrl()).isEqualTo("https://example.com/audio.mp4");
        assertThat(dto.getCodec()).isEqualTo("mp4a.40.2");
        assertThat(dto.getMimeType()).isEqualTo("audio/mp4");
        assertThat(dto.getBandwidth()).isEqualTo(130482);
        assertThat(dto.getAudioSamplingRate()).isEqualTo("44100");
        assertThat(dto.getAudioChannels()).isEqualTo(2);
        assertThat(dto.getLanguage()).isEqualTo("en");
        assertThat(dto.getLanguageName()).isEqualTo("English");
        assertThat(dto.getInitRange()).isEqualTo("0-722");
        assertThat(dto.getIndexRange()).isEqualTo("723-898");
        assertThat(dto.getFormat()).isEqualTo("M4A");
    }

    // ── Validation ───────────────────────────────────────────────────────

    @Test
    @DisplayName("A fully-populated DTO passes validation")
    void validDtoPassesValidation() {
        assertThat(validator.validate(validBuilder().build())).isEmpty();
    }

    static Stream<Arguments> invalidFieldCases() {
        return Stream.of(
                Arguments.of("blank id", (UnaryOperator<AudioStreamMetadataDTO.Builder>) b -> b.id(""),
                        "Audio stream ID cannot be blank"),
                Arguments.of("blank url", (UnaryOperator<AudioStreamMetadataDTO.Builder>) b -> b.url(""),
                        "Audio stream URL cannot be blank"),
                Arguments.of("blank codec", (UnaryOperator<AudioStreamMetadataDTO.Builder>) b -> b.codec(""),
                        "Audio codec cannot be blank"),
                Arguments.of("blank mimeType", (UnaryOperator<AudioStreamMetadataDTO.Builder>) b -> b.mimeType(""),
                        "Audio MIME type cannot be blank"),
                Arguments.of("blank audioSamplingRate",
                        (UnaryOperator<AudioStreamMetadataDTO.Builder>) b -> b.audioSamplingRate(""),
                        "Audio sampling rate cannot be blank"),
                Arguments.of("zero bandwidth", (UnaryOperator<AudioStreamMetadataDTO.Builder>) b -> b.bandwidth(0),
                        "Bandwidth must be at least 1"),
                Arguments.of("zero audioChannels",
                        (UnaryOperator<AudioStreamMetadataDTO.Builder>) b -> b.audioChannels(0),
                        "Audio channels must be at least 1"));
    }

    @ParameterizedTest(name = "Rejects {0}")
    @MethodSource("invalidFieldCases")
    @DisplayName("Rejects DTOs violating a single constraint, with the matching message")
    void rejectsInvalidField(String name, UnaryOperator<AudioStreamMetadataDTO.Builder> mutator,
                             String expectedMessage) {
        Set<ConstraintViolation<AudioStreamMetadataDTO>> violations =
                validator.validate(mutator.apply(validBuilder()).build());

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains(expectedMessage));
    }

    // ── from() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Maps every field from a fully-populated AudioStream, including both segment ranges")
    void mapsAllFieldsFromValidStream() {
        AudioStream stream = audioStream(Locale.ENGLISH, 2);
        when(stream.getFormat().getName()).thenReturn("M4A");
        when(stream.getInitStart()).thenReturn(0);
        when(stream.getInitEnd()).thenReturn(722);
        when(stream.getIndexStart()).thenReturn(723);
        when(stream.getIndexEnd()).thenReturn(898);

        AudioStreamMetadataDTO dto = AudioStreamMetadataDTO.from(stream, 1);

        assertThat(dto.getId()).isEqualTo("audio-1");
        assertThat(dto.getUrl()).isEqualTo("https://example.com/audio.mp4");
        assertThat(dto.getBandwidth()).isEqualTo(130482);
        assertThat(dto.getAudioSamplingRate()).isEqualTo("44100");
        assertThat(dto.getLanguage()).isEqualTo("en");
        assertThat(dto.getLanguageName()).isEqualTo("English");
        assertThat(dto.getInitRange()).isEqualTo("0-722");
        assertThat(dto.getIndexRange()).isEqualTo("723-898");
        assertThat(dto.getFormat()).isEqualTo("M4A");
    }

    @Test
    @DisplayName("Rejects a null AudioStream with IllegalArgumentException")
    void rejectsNullAudioStream() {
        assertThatThrownBy(() -> AudioStreamMetadataDTO.from(null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AudioStream cannot be null");
    }

    static Stream<Arguments> localeCases() {
        return Stream.of(
                Arguments.of("null locale", null, "und", "Unknown"),
                Arguments.of("Spanish", Locale.forLanguageTag("es"), "es", "Spanish"),
                Arguments.of("French", Locale.FRENCH, "fr", "French"),
                Arguments.of("German", Locale.GERMAN, "de", "German"));
    }

    @ParameterizedTest(name = "Resolves {0} to language/name")
    @MethodSource("localeCases")
    @DisplayName("Resolves language and languageName from the stream's audio locale, defaulting to und/Unknown")
    void resolvesLanguageFromLocale(String name, Locale locale, String expectedLanguage, String expectedName) {
        AudioStreamMetadataDTO dto = AudioStreamMetadataDTO.from(audioStream(locale, 2), 1);

        assertThat(dto.getLanguage()).isEqualTo(expectedLanguage);
        assertThat(dto.getLanguageName()).isEqualTo(expectedName);
    }

    @Test
    @DisplayName("Defaults mimeType to audio/mp4 and format to null when the stream's format is null")
    void handlesNullFormat() {
        AudioStream stream = mock(AudioStream.class);
        ItagItem itagItem = mock(ItagItem.class);
        when(stream.getContent()).thenReturn("https://example.com/audio.mp4");
        when(stream.getCodec()).thenReturn("mp4a.40.2");
        when(stream.getFormat()).thenReturn(null);
        when(stream.getBitrate()).thenReturn(130482);
        when(stream.getItagItem()).thenReturn(itagItem);
        when(itagItem.getSampleRate()).thenReturn(44100);
        when(itagItem.getAudioChannels()).thenReturn(2);
        when(stream.getInitStart()).thenReturn(-1);
        when(stream.getIndexStart()).thenReturn(-1);

        AudioStreamMetadataDTO dto = AudioStreamMetadataDTO.from(stream, 1);

        assertThat(dto.getMimeType()).isNull();
        assertThat(dto.getFormat()).isNull();
    }

    @Test
    @DisplayName("Defaults audioChannels to 2 when the itag reports zero")
    void defaultsAudioChannelsWhenItagReportsZero() {
        AudioStreamMetadataDTO dto = AudioStreamMetadataDTO.from(audioStream(null, 0), 1);

        assertThat(dto.getAudioChannels()).isEqualTo(2);
    }

    @Test
    @DisplayName("Omits both segment ranges when the stream reports no start offsets")
    void omitsRangesWhenNotAvailable() {
        AudioStreamMetadataDTO dto = AudioStreamMetadataDTO.from(audioStream(null, 2), 1);

        assertThat(dto.getInitRange()).isNull();
        assertThat(dto.getIndexRange()).isNull();
    }

    // ── JSON ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Omits unset optional fields from the serialized JSON")
    void omitsNullFieldsInJson() throws Exception {
        AudioStreamMetadataDTO dto = AudioStreamMetadataDTO.builder()
                .id("audio-1").url("https://example.com/audio.mp4").codec("mp4a.40.2")
                .mimeType("audio/mp4").bandwidth(130482).audioSamplingRate("44100").audioChannels(2)
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).doesNotContain("\"language\"", "\"languageName\"", "\"initRange\"", "\"format\"");
    }

    @Test
    @DisplayName("Round-trips every field through serialize/deserialize unchanged")
    void roundTripPreservesAllFields() throws Exception {
        AudioStreamMetadataDTO original = validBuilder()
                .initRange("0-722").indexRange("723-898").format("M4A").build();

        AudioStreamMetadataDTO restored = objectMapper.readValue(
                objectMapper.writeValueAsString(original), AudioStreamMetadataDTO.class);

        assertThat(restored.getId()).isEqualTo(original.getId());
        assertThat(restored.getUrl()).isEqualTo(original.getUrl());
        assertThat(restored.getCodec()).isEqualTo(original.getCodec());
        assertThat(restored.getMimeType()).isEqualTo(original.getMimeType());
        assertThat(restored.getBandwidth()).isEqualTo(original.getBandwidth());
        assertThat(restored.getAudioSamplingRate()).isEqualTo(original.getAudioSamplingRate());
        assertThat(restored.getAudioChannels()).isEqualTo(original.getAudioChannels());
        assertThat(restored.getLanguage()).isEqualTo(original.getLanguage());
        assertThat(restored.getLanguageName()).isEqualTo(original.getLanguageName());
        assertThat(restored.getInitRange()).isEqualTo(original.getInitRange());
        assertThat(restored.getIndexRange()).isEqualTo(original.getIndexRange());
        assertThat(restored.getFormat()).isEqualTo(original.getFormat());
    }

    // ── toString ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString includes the key identifying fields")
    void toStringIncludesKeyFields() {
        String result = validBuilder().build().toString();

        assertThat(result).contains("audio-1", "mp4a.40.2", "130482", "en", "English");
    }
}