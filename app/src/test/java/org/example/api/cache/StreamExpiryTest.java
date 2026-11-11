package org.example.api.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;

class StreamExpiryTest {

    private static final Instant EARLY = Instant.ofEpochSecond(1_800_000_000L);
    private static final Instant LATE = Instant.ofEpochSecond(1_800_003_600L);

    static Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of(
                        "single signed stream",
                        StreamFixtures.with(
                                List.of(StreamFixtures.audio(StreamFixtures.signedUrl(EARLY))), List.of(), List.of()),
                        Optional.of(EARLY)),
                Arguments.of(
                        "several streams with different expiries -> earliest wins",
                        StreamFixtures.with(
                                List.of(StreamFixtures.audio(StreamFixtures.signedUrl(LATE))),
                                List.of(StreamFixtures.videoOnly(StreamFixtures.signedUrl(EARLY))),
                                List.of(StreamFixtures.muxed(StreamFixtures.signedUrl(LATE)))),
                        Optional.of(EARLY)),
                Arguments.of(
                        "expiry only on audio, video unsigned -> still found",
                        StreamFixtures.with(
                                List.of(StreamFixtures.audio(StreamFixtures.signedUrl(EARLY))),
                                List.of(StreamFixtures.videoOnly(StreamFixtures.unsignedUrl())),
                                List.of()),
                        Optional.of(EARLY)),
                Arguments.of(
                        "no expire parameter anywhere -> empty",
                        StreamFixtures.healthyWithoutExpiry(),
                        Optional.empty()),
                Arguments.of("no streams at all -> empty", StreamFixtures.empty(), Optional.empty()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void parsesEarliestExpiry(String name, StreamInfo info, Optional<Instant> expected) {
        assertThat(StreamExpiry.earliest(info)).isEqualTo(expected);
    }

    @Test
    @DisplayName("a stream with null content is skipped rather than throwing")
    void nullContentIsSkipped() {
        // The builders reject null content, so this state is only reachable via a mock
        AudioStream nullContent = mock(AudioStream.class);
        when(nullContent.getContent()).thenReturn(null);

        StreamInfo info = StreamFixtures.with(
                List.of(nullContent, StreamFixtures.audio(StreamFixtures.signedUrl(EARLY))), List.of(), List.of());

        assertThat(StreamExpiry.earliest(info)).contains(EARLY);
    }

    @Test
    @DisplayName("an expire value too large for a long is ignored, not propagated")
    void oversizedExpireValueIsIgnored() {
        String tooBig = "9".repeat(25);

        StreamInfo info = StreamFixtures.with(
                List.of(StreamFixtures.audio(StreamFixtures.signedUrl(tooBig))), List.of(), List.of());

        assertThatCode(() -> StreamExpiry.earliest(info)).doesNotThrowAnyException();
        assertThat(StreamExpiry.earliest(info)).isEmpty();
    }

    @Test
    @DisplayName("an oversized value does not mask a valid expiry on another stream")
    void oversizedValueDoesNotHideValidExpiry() {
        StreamInfo info = StreamFixtures.with(
                List.of(StreamFixtures.audio(StreamFixtures.signedUrl("9".repeat(25)))),
                List.of(StreamFixtures.videoOnly(StreamFixtures.signedUrl(EARLY))),
                List.of());

        assertThat(StreamExpiry.earliest(info)).contains(EARLY);
    }
}
