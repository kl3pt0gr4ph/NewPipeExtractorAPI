package org.example.api.dto.dash;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.extractor.services.youtube.ItagItem;
import org.schabi.newpipe.extractor.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Data Transfer Object representing the complete DASH manifest configuration.
 * Contains all video, audio, and subtitle streams with manifest metadata.
 * This is the root DTO that holds all streams without filtering.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashManifestConfigDTO {

    private static final Logger logger = LoggerFactory.getLogger(DashManifestConfigDTO.class);

    /** Type of DASH manifest (typically "static" for VOD content) */
    @NotBlank(message = "Manifest type cannot be blank")
    @JsonProperty("type")
    private String type;

    /** Total duration of the media presentation in ISO 8601 format (e.g., "PT1M59.702S"). */
    @NotBlank(message = "Media presentation duration cannot be blank")
    @JsonProperty("mediaPresentationDuration")
    private String mediaPresentationDuration;

    /** Minimum buffer time required for playback in ISO 8601 format (e.g., "PT2S"). */
    @NotBlank(message = "Min buffer time cannot be blank")
    @JsonProperty("minBufferTime")
    private String minBufferTime;

    /** DASH profile identifier specifying which DASH features are used. */
    @NotBlank(message = "Profiles cannot be blank")
    @JsonProperty("profiles")
    private String profiles;

    /** List of all available video streams with different qualities/resolutions. */
    @NotNull(message = "Video streams list cannot be null")
    @JsonProperty("videoStreams")
    private List<VideoStreamMetadataDTO> videoStreams;

    /** List of all available audio streams with different qualities/languages. */
    @NotNull(message = "Audio streams list cannot be null")
    @JsonProperty("audioStreams")
    private List<AudioStreamMetadataDTO> audioStreams;

    /** List of all available subtitle tracks. */
    @NotNull(message = "Subtitle streams list cannot be null")
    @JsonProperty("subtitleStreams")
    private List<SubtitleMetadataDTO> subtitleStreams;

    /** Duration in seconds (for convenience, not part of XML manifest). */
    @Min(value = 1, message = "Duration must be at least 1 second")
    @JsonProperty("durationSeconds")
    private long durationSeconds;

    // Constructors

    /** Default constructor for Jackson deserialization. */
    public DashManifestConfigDTO() {
        this.type = "static";
        this.minBufferTime = "PT2S";
        this.profiles = "urn:mpeg:dash:profile:isoff-on-demand:2011";
        this.videoStreams = new ArrayList<>();
        this.audioStreams = new ArrayList<>();
        this.subtitleStreams = new ArrayList<>();
    }

    /** Full constructor for creating a DashManifestConfigDTO with all fields. */
    public DashManifestConfigDTO(String type, String mediaPresentationDuration, String minBufferTime,
                                 String profiles, List<VideoStreamMetadataDTO> videoStreams,
                                 List<AudioStreamMetadataDTO> audioStreams,
                                 List<SubtitleMetadataDTO> subtitleStreams, long durationSeconds) {
        this.type = type;
        this.mediaPresentationDuration = mediaPresentationDuration;
        this.minBufferTime = minBufferTime;
        this.profiles = profiles;
        this.videoStreams = videoStreams != null ? videoStreams : new ArrayList<>();
        this.audioStreams = audioStreams != null ? audioStreams : new ArrayList<>();
        this.subtitleStreams = subtitleStreams != null ? subtitleStreams : new ArrayList<>();
        this.durationSeconds = durationSeconds;
    }

    // Static Factory Method

    /**
     * Builds the manifest config from streams that already passed DashPlayabilityFilter.
     *
     * <p>Every stream passed in is converted and ends up in the manifest</p>
     */
    public static DashManifestConfigDTO from(StreamInfo streamInfo,
                                             List<VideoStream> videoStreams,
                                             List<AudioStream> audioStreams,
                                             List<SubtitlesStream> subtitleStreams) {
        Objects.requireNonNull(streamInfo, "streamInfo");
        Objects.requireNonNull(videoStreams, "videoStreams");
        Objects.requireNonNull(audioStreams, "audioStreams");
        Objects.requireNonNull(subtitleStreams, "subtitleStreams");

        DashManifestConfigDTO dto = new DashManifestConfigDTO();
        dto.setDurationSeconds(streamInfo.getDuration());
        dto.setMediaPresentationDuration(formatDuration(
                presentationDurationMs(streamInfo, videoStreams, audioStreams)));

        dto.setVideoStreams(convertAll(videoStreams, "video", VideoStreamMetadataDTO::from));
        dto.setAudioStreams(convertAll(audioStreams, "audio", AudioStreamMetadataDTO::from));
        dto.setSubtitleStreams(convertAll(subtitleStreams, "subtitle", SubtitleMetadataDTO::from));

        return dto;
    }

    @FunctionalInterface
    private interface IndexedConverter<S, D> {
        D convert(S stream, int index) throws Exception;
    }

    /**
     * Converts each stream to its DTO. Indexes start at 1 and advance even when a
     * conversion fails.
     */
    private static <S extends Stream, D> List<D> convertAll(List<S> streams, String kind,
                                                            IndexedConverter<S, D> converter) {
        List<D> converted = new ArrayList<>(streams.size());
        int index = 1;
        for (S stream : streams) {
            try {
                converted.add(converter.convert(stream, index++));
            } catch (Exception e) {
                logger.warn("Skipping {} stream {}: passed the playability filter but failed conversion: {}",
                        kind, stream.getId(), e.getMessage());
            }
        }
        return converted;
    }

    /**
     * Real media length in ms. YouTube reports approxDurationMs per itag; take the longest
     * so the presentation covers every track. Falls back to whole seconds from StreamInfo.
     */
    private static long presentationDurationMs(StreamInfo streamInfo,
                                               List<VideoStream> videoStreams,
                                               List<AudioStream> audioStreams) {
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

    /** Builder class for fluent construction of DashManifestConfigDTO instances. */
    public static class Builder {
        private String type = "static";
        private String mediaPresentationDuration;
        private String minBufferTime = "PT2S";
        private String profiles = "urn:mpeg:dash:profile:isoff-on-demand:2011";
        private List<VideoStreamMetadataDTO> videoStreams = new ArrayList<>();
        private List<AudioStreamMetadataDTO> audioStreams = new ArrayList<>();
        private List<SubtitleMetadataDTO> subtitleStreams = new ArrayList<>();
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
            return new DashManifestConfigDTO(type, mediaPresentationDuration, minBufferTime,
                    profiles, videoStreams, audioStreams, subtitleStreams, durationSeconds);
        }
    }

    // Getters and Setters

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMediaPresentationDuration() {
        return mediaPresentationDuration;
    }

    public void setMediaPresentationDuration(String mediaPresentationDuration) {
        this.mediaPresentationDuration = mediaPresentationDuration;
    }

    public String getMinBufferTime() {
        return minBufferTime;
    }

    public void setMinBufferTime(String minBufferTime) {
        this.minBufferTime = minBufferTime;
    }

    public String getProfiles() {
        return profiles;
    }

    public void setProfiles(String profiles) {
        this.profiles = profiles;
    }

    public List<VideoStreamMetadataDTO> getVideoStreams() {
        return videoStreams;
    }

    public void setVideoStreams(List<VideoStreamMetadataDTO> videoStreams) {
        this.videoStreams = videoStreams;
    }

    public List<AudioStreamMetadataDTO> getAudioStreams() {
        return audioStreams;
    }

    public void setAudioStreams(List<AudioStreamMetadataDTO> audioStreams) {
        this.audioStreams = audioStreams;
    }

    public List<SubtitleMetadataDTO> getSubtitleStreams() {
        return subtitleStreams;
    }

    public void setSubtitleStreams(List<SubtitleMetadataDTO> subtitleStreams) {
        this.subtitleStreams = subtitleStreams;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    @Override
    public String toString() {
        return "DashManifestConfigDTO{type='%s', mediaPresentationDuration='%s', durationSeconds=%d, videoStreams=%d, audioStreams=%d, subtitleStreams=%d}".formatted(type, mediaPresentationDuration, durationSeconds, videoStreams.size(), audioStreams.size(), subtitleStreams.size());
    }
}