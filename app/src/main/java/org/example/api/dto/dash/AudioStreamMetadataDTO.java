package org.example.api.dto.dash;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.schabi.newpipe.extractor.services.youtube.ItagItem;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.AudioTrackType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

/**
 * Data Transfer Object representing metadata for an audio stream in a DASH manifest.
 * Contains information about audio quality, codec, language, and segment locations.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AudioStreamMetadataDTO {

    /**
     * Unique identifier for this audio representation in the DASH manifest.
     */
    @NotBlank(message = "Audio stream ID cannot be blank")
    @JsonProperty("id")
    private String id;

    /** Direct URL to the audio stream content */
    @NotBlank(message = "Audio stream URL cannot be blank")
    @JsonProperty("url")
    private String url;

    /** Groups every itag of one audio track. Null on single track videos */
    @JsonProperty("audioTrackId")
    private String audioTrackId;

    /** Service-provided display name for the track name */
    @JsonProperty("audioTrackName")
    private String audioTrackName;

    /** Original, dubbed, descriptive or secondary. Null when the servcie doesn't say */
    @JsonProperty("audioTrackType")
    private AudioTrackType audioTrackType;

    /** Audio codec identifier (e.g., "mp4a.40.2", "opus") */
    @NotBlank(message = "Audio codec cannot be blank")
    @JsonProperty("codec")
    private String codec;

    /** MIME type of the audio stream (e.g., "audio/mp4", "audio/webm") */
    @NotBlank(message = "Audio MIME type cannot be blank")
    @JsonProperty("mimeType")
    private String mimeType;

    /** Bandwidth required for this stream in bits per second */
    @Min(value = 1, message = "Bandwidth must be at least 1")
    @JsonProperty("bandwidth")
    private int bandwidth;

    /** Audio sampling rate in Hz (e.g., "44100", "48000") */
    @NotBlank(message = "Audio sampling rate cannot be blank")
    @JsonProperty("audioSamplingRate")
    private String audioSamplingRate;

    /** Number of audio channels (e.g., 1 for mono, 2 for stereo, 6 for 5.1) */
    @Min(value = 1, message = "Audio channels must be at least 1")
    @JsonProperty("audioChannels")
    private int audioChannels;

    /** Language code for this audio track (e.g., "en", "es", "und" for undefined) */
    @JsonProperty("language")
    private String language;

    /** Human-readable language name (e.g., "English", "Spanish", "Unknown") */
    @JsonProperty("languageName")
    private String languageName;

    /** Byte range for initialization segment (e.g., "0-722") */
    @JsonProperty("initRange")
    private String initRange;

    /** Byte range for index segment (e.g., "723-898") */
    @JsonProperty("indexRange")
    private String indexRange;

    /** Original format identifier from the extractor (optional, for debugging) */
    @JsonProperty("format")
    private String format;

    // Constructors

    /** Default constructor for Jackson deserialization */
    public AudioStreamMetadataDTO() {
    }

    /** Full constructor for creating an AudioStreamMetadataDTO with all fields */
    public AudioStreamMetadataDTO(String id, String url, String codec, String mimeType,
                                  int bandwidth, String audioSamplingRate, int audioChannels,
                                  String language, String languageName,
                                  String initRange, String indexRange, String format) {
        this.id = id;
        this.url = url;
        this.codec = codec;
        this.mimeType = mimeType;
        this.bandwidth = bandwidth;
        this.audioSamplingRate = audioSamplingRate;
        this.audioChannels = audioChannels;
        this.language = language;
        this.languageName = languageName;
        this.initRange = initRange;
        this.indexRange = indexRange;
        this.format = format;
    }

    // Static Factory Method

    /**
     * Creates an AudioStreamMetadataDTO from a NewPipe AudioStream object.
     *
     * @param stream The AudioStream from NewPipe extractor
     * @param index  The index used to generate a unique ID
     * @return A new AudioStreamMetadataDTO instance
     */
    public static AudioStreamMetadataDTO from(AudioStream stream, int index) {
        if (stream == null) {
            throw new IllegalArgumentException("AudioStream cannot be null");
        }

        AudioStreamMetadataDTO dto = new AudioStreamMetadataDTO();
        dto.setId("audio-%d".formatted(index));
        dto.setUrl(stream.getContent());
        dto.setCodec(stream.getCodec());
        dto.setMimeType(stream.getFormat() != null ? stream.getFormat().getMimeType() : null);
        dto.setBandwidth(stream.getBitrate());

        ItagItem itag = stream.getItagItem();
        int sampleRate = itag != null ? itag.getSampleRate() : ItagItem.SAMPLE_RATE_UNKNOWN;
        int channels = itag != null ? itag.getAudioChannels() : ItagItem.AUDIO_CHANNELS_NOT_APPLICABLE_OR_UNKNOWN;
        if (sampleRate > 0) {
            dto.setAudioSamplingRate(String.valueOf(sampleRate));
        }
        dto.setAudioChannels(channels > 0 ? channels : 2);

        // Full BCP 2 tag so regional dub stays distinguishable
        Locale locale = stream.getAudioLocale();
        dto.setLanguage(locale != null ? locale.toLanguageTag() : "und");
        dto.setLanguageName(getLanguageDisplayName(locale != null ? locale.getLanguage() : "und"));

        dto.setAudioTrackId(stream.getAudioTrackId());
        dto.setAudioTrackName(stream.getAudioTrackName());
        dto.setAudioTrackType(stream.getAudioTrackType());

        if (stream.getInitStart() >= 0 && stream.getInitEnd() > 0) {
            dto.setInitRange("%d-%d".formatted(stream.getInitStart(), stream.getInitEnd()));
        }
        if (stream.getIndexStart() >= 0 && stream.getIndexEnd() > 0) {
            dto.setIndexRange("%d-%d".formatted(stream.getIndexStart(), stream.getIndexEnd()));
        }

        dto.setFormat(stream.getFormat() != null ? stream.getFormat().getName() : null);

        return dto;
    }

    /**
     * Helper method to get display name for language code.
     * This is a simple implementation - can be expanded with more language mappings.
     *
     * @param languageCode The ISO language code
     * @return Human-readable language name
     */
    private static String getLanguageDisplayName(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            return "Unknown";
        }

        return switch (languageCode.toLowerCase()) {
            case "en" -> "English";
            case "es" -> "Spanish";
            case "fr" -> "French";
            case "de" -> "German";
            case "it" -> "Italian";
            case "pt" -> "Portuguese";
            case "ru" -> "Russian";
            case "ja" -> "Japanese";
            case "ko" -> "Korean";
            case "zh" -> "Chinese";
            case "ar" -> "Arabic";
            case "hi" -> "Hindi";
            case "und" -> "Unknown";
            default -> languageCode.toUpperCase();
        };
    }

    // Builder Pattern

    /**
     * Creates a new Builder for constructing AudioStreamMetadataDTO instances.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for fluent construction of AudioStreamMetadataDTO instances.
     */
    public static class Builder {
        private String id;
        private String url;
        private String codec;
        private String mimeType;
        private int bandwidth;
        private String audioSamplingRate;
        private int audioChannels;
        private String language;
        private String languageName;
        private String initRange;
        private String indexRange;
        private String format;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder codec(String codec) {
            this.codec = codec;
            return this;
        }

        public Builder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder bandwidth(int bandwidth) {
            this.bandwidth = bandwidth;
            return this;
        }

        public Builder audioSamplingRate(String audioSamplingRate) {
            this.audioSamplingRate = audioSamplingRate;
            return this;
        }

        public Builder audioChannels(int audioChannels) {
            this.audioChannels = audioChannels;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder languageName(String languageName) {
            this.languageName = languageName;
            return this;
        }

        public Builder initRange(String initRange) {
            this.initRange = initRange;
            return this;
        }

        public Builder indexRange(String indexRange) {
            this.indexRange = indexRange;
            return this;
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        public AudioStreamMetadataDTO build() {
            return new AudioStreamMetadataDTO(id, url, codec, mimeType, bandwidth,
                    audioSamplingRate, audioChannels, language, languageName,
                    initRange, indexRange, format);
        }
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCodec() {
        return codec;
    }

    public String getAudioTrackId() {
        return audioTrackId;
    }

    public void setAudioTrackId(String audioTrackId) {
        this.audioTrackId = audioTrackId;
    }

    public String getAudioTrackName() {
        return audioTrackName;
    }

    public void setAudioTrackName(String audioTrackName) {
        this.audioTrackName = audioTrackName;
    }

    public AudioTrackType getAudioTrackType() {
        return audioTrackType;
    }

    public void setAudioTrackType(AudioTrackType audioTrackType) {
        this.audioTrackType = audioTrackType;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(int bandwidth) {
        this.bandwidth = bandwidth;
    }

    public String getAudioSamplingRate() {
        return audioSamplingRate;
    }

    public void setAudioSamplingRate(String audioSamplingRate) {
        this.audioSamplingRate = audioSamplingRate;
    }

    public int getAudioChannels() {
        return audioChannels;
    }

    public void setAudioChannels(int audioChannels) {
        this.audioChannels = audioChannels;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLanguageName() {
        return languageName;
    }

    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    public String getInitRange() {
        return initRange;
    }

    public void setInitRange(String initRange) {
        this.initRange = initRange;
    }

    public String getIndexRange() {
        return indexRange;
    }

    public void setIndexRange(String indexRange) {
        this.indexRange = indexRange;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return (
                "AudioStreamMetadataDTO{id='%s'," +
                " codec='%s', " +
                "bandwidth=%d, " +
                "audioSamplingRate='%s', " +
                "audioChannels=%d, " +
                "language='%s', " +
                "languageName='%s'}"
        )
                .formatted(
                        id,
                        codec,
                        bandwidth,
                        audioSamplingRate,
                        audioChannels,
                        language,
                        languageName
                );
    }
}