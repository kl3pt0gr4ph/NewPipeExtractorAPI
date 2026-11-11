package org.example.api.dto.dash;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.schabi.newpipe.extractor.services.youtube.ItagItem;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data Transfer Object representing the complete DASH manifest configuration.
 * Contains all video, audio, and subtitle streams with manifest metadata.
 * This is the root DTO that holds all streams without filtering.
 *
 * <p>The stream lists are copied on construction and can't be modified afterwards;
 * a {@code null} list becomes an empty one.</p>
 *
 * @param type                      Type of DASH manifest (typically "static" for VOD content)
 * @param mediaPresentationDuration Total duration of the media presentation in ISO 8601 format
 *                                  (e.g., "PT1M59.702S")
 * @param minBufferTime             Minimum buffer time required for playback in ISO 8601 format (e.g., "PT2S")
 * @param profiles                  DASH profile identifier specifying which DASH features are used
 * @param videoStreams              All available video streams with different qualities/resolutions
 * @param audioStreams              All available audio streams with different qualities/languages
 * @param subtitleStreams           All available subtitle tracks
 * @param durationSeconds           Duration in seconds (for convenience, not part of XML manifest)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashManifestConfigDTO(
        @NotBlank(message = "Manifest type cannot be blank") String type,

        @NotBlank(message = "Media presentation duration cannot be blank") String mediaPresentationDuration,

        @NotBlank(message = "Min buffer time cannot be blank") String minBufferTime,

        @NotBlank(message = "Profiles cannot be blank") String profiles,

        @NotNull(message = "Video streams list cannot be null") List<VideoStreamMetadataDTO> videoStreams,

        @NotNull(message = "Audio streams list cannot be null") List<AudioStreamMetadataDTO> audioStreams,

        @NotNull(message = "Subtitle streams list cannot be null") List<SubtitleMetadataDTO> subtitleStreams,

        @Min(value = 1, message = "Duration must be at least 1 second") long durationSeconds) {

    private static final Logger logger = LoggerFactory.getLogger(DashManifestConfigDTO.class);

    public DashManifestConfigDTO {
        videoStreams = videoStreams == null ? List.of() : List.copyOf(videoStreams);
        audioStreams = audioStreams == null ? List.of() : List.copyOf(audioStreams);
        subtitleStreams = subtitleStreams == null ? List.of() : List.copyOf(subtitleStreams);
    }

    // Static Factory Method

    /**
     * Builds the manifest config from streams that already passed DashPlayabilityFilter.
     *
     * <p>Every stream passed in is converted and ends up in the manifest</p>
     */
    public static DashManifestConfigDTO from(
            StreamInfo streamInfo,
            List<VideoStream> videoStreams,
            List<AudioStream> audioStreams,
            List<SubtitlesStream> subtitleStreams) {
        Objects.requireNonNull(streamInfo, "streamInfo");
        Objects.requireNonNull(videoStreams, "videoStreams");
        Objects.requireNonNull(audioStreams, "audioStreams");
        Objects.requireNonNull(subtitleStreams, "subtitleStreams");

        return builder()
                .durationSeconds(streamInfo.getDuration())
                .mediaPresentationDuration(
                        formatDuration(presentationDurationMs(streamInfo, videoStreams, audioStreams)))
                .videoStreams(convertAll(videoStreams, "video", VideoStreamMetadataDTO::from))
                .audioStreams(convertAll(audioStreams, "audio", AudioStreamMetadataDTO::from))
                .subtitleStreams(convertAll(subtitleStreams, "subtitle", SubtitleMetadataDTO::from))
                .build();
    }

    @FunctionalInterface
    private interface IndexedConverter<S, D> {
        D convert(S stream, int index) throws Exception;
    }

    /**
     * Converts each stream to its DTO. Indexes start at 1 and advance even when a
     * conversion fails.
     */
    private static <S extends Stream, D> List<D> convertAll(
            List<S> streams, String kind, IndexedConverter<S, D> converter) {
        List<D> converted = new ArrayList<>(streams.size());
        int index = 1;
        for (S stream : streams) {
            try {
                converted.add(converter.convert(stream, index++));
            } catch (Exception e) {
                logger.warn(
                        "Skipping {} stream {}: passed the playability filter but failed conversion: {}",
                        kind,
                        stream.getId(),
                        e.getMessage());
            }
        }
        return converted;
    }

    /**
     * Real media length in ms. YouTube reports approxDurationMs per itag; take the longest
     * so the presentation covers every track. Falls back to whole seconds from StreamInfo.
     */
    private static long presentationDurationMs(
            StreamInfo streamInfo, List<VideoStream> videoStreams, List<AudioStream> audioStreams) {
        long longest = ItagItem.APPROX_DURATION_MS_UNKNOWN;
        for (VideoStream stream : videoStreams) {
            longest = Math.max(longest, approxDurationMs(stream));
        }
        for (AudioStream stream : audioStreams) {
            longest = Math.max(longest, approxDurationMs(stream));
        }
        return longest > 0 ? longest : TimeUnit.SECONDS.toMillis(streamInfo.getDuration());
    }

    private static long approxDurationMs(Stream stream) {
        ItagItem itag = stream.getItagItem();
        return itag != null ? itag.getApproxDurationMs() : ItagItem.APPROX_DURATION_MS_UNKNOWN;
    }

    /** ISO 8601 duration, e.g. 119702 ms to "PT1M59.702S". */
    private static String formatDuration(long durationMs) {
        return durationMs > 0 ? Duration.ofMillis(durationMs).toString() : "PT0S";
    }

    // Builder Pattern

    /**
     * Creates a new Builder for constructing DashManifestConfigDTO instances.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for fluent construction of DashManifestConfigDTO instances.
     * Starts with the on-demand defaults: static type, PT2S buffer, isoff-on-demand profile.
     */
    public static class Builder {
        private String type = "static";
        private String mediaPresentationDuration;
        private String minBufferTime = "PT2S";
        private String profiles = "urn:mpeg:dash:profile:isoff-on-demand:2011";
        private List<VideoStreamMetadataDTO> videoStreams = List.of();
        private List<AudioStreamMetadataDTO> audioStreams = List.of();
        private List<SubtitleMetadataDTO> subtitleStreams = List.of();
        private long durationSeconds;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder mediaPresentationDuration(String mediaPresentationDuration) {
            this.mediaPresentationDuration = mediaPresentationDuration;
            return this;
        }

        public Builder minBufferTime(String minBufferTime) {
            this.minBufferTime = minBufferTime;
            return this;
        }

        public Builder profiles(String profiles) {
            this.profiles = profiles;
            return this;
        }

        public Builder videoStreams(List<VideoStreamMetadataDTO> videoStreams) {
            this.videoStreams = videoStreams;
            return this;
        }

        public Builder audioStreams(List<AudioStreamMetadataDTO> audioStreams) {
            this.audioStreams = audioStreams;
            return this;
        }

        public Builder subtitleStreams(List<SubtitleMetadataDTO> subtitleStreams) {
            this.subtitleStreams = subtitleStreams;
            return this;
        }

        public Builder durationSeconds(long durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }

        public DashManifestConfigDTO build() {
            return new DashManifestConfigDTO(
                    type,
                    mediaPresentationDuration,
                    minBufferTime,
                    profiles,
                    videoStreams,
                    audioStreams,
                    subtitleStreams,
                    durationSeconds);
        }
    }

    /** Prints stream counts rather than the streams themselves. */
    @Override
    public String toString() {
        return "DashManifestConfigDTO{type='%s', mediaPresentationDuration='%s', durationSeconds=%d, videoStreams=%d, audioStreams=%d, subtitleStreams=%d}"
                .formatted(
                        type,
                        mediaPresentationDuration,
                        durationSeconds,
                        videoStreams.size(),
                        audioStreams.size(),
                        subtitleStreams.size());
    }
}
