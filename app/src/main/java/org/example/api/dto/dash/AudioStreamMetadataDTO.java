package org.example.api.dto.dash;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.services.youtube.ItagItem;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.AudioTrackType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

/**
 * Data Transfer Object representing metadata for an audio stream in a DASH manifest.
 * Contains information about audio quality, codec, language, and segment locations.
 *
 * @param id                Unique identifier for this audio representation in the DASH manifest
 * @param url               Direct URL to the audio stream content
 * @param audioTrackId      Groups every itag of one audio track. Null on single-track videos
 * @param audioTrackName    Service-provided display name for the track
 * @param audioTrackType    Original, dubbed, descriptive or secondary. Null when the service doesn't say
 * @param codec             Audio codec identifier (e.g., "mp4a.40.2", "opus")
 * @param mimeType          MIME type of the audio stream (e.g., "audio/mp4", "audio/webm")
 * @param bandwidth         Bandwidth required for this stream in bits per second
 * @param audioSamplingRate Audio sampling rate in Hz (e.g., "44100", "48000")
 * @param audioChannels     Number of audio channels (e.g., 1 for mono, 2 for stereo, 6 for 5.1)
 * @param language          Language tag for this audio track (e.g., "en", "es-419", "und" for undefined)
 * @param languageName      Human-readable language name (e.g., "English", "Spanish", "Unknown")
 * @param initRange         Byte range for initialization segment (e.g., "0-722")
 * @param indexRange        Byte range for index segment (e.g., "723-898")
 * @param format            Original format identifier from the extractor (optional, for debugging)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AudioStreamMetadataDTO(
        @NotBlank(message = "Audio stream ID cannot be blank") String id,
        @NotBlank(message = "Audio stream URL cannot be blank") String url,
        String audioTrackId,
        String audioTrackName,
        AudioTrackType audioTrackType,
        @NotBlank(message = "Audio codec cannot be blank") String codec,
        @NotBlank(message = "Audio MIME type cannot be blank") String mimeType,
        @Min(value = 1, message = "Bandwidth must be at least 1") int bandwidth,
        @NotBlank(message = "Audio sampling rate cannot be blank") String audioSamplingRate,
        @Min(value = 1, message = "Audio channels must be at least 1") int audioChannels,
        String language,
        String languageName,
        String initRange,
        String indexRange,
        String format) {

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
        MediaFormat format = stream.getFormat();

        ItagItem itag = stream.getItagItem();
        int sampleRate = itag != null ? itag.getSampleRate() : ItagItem.SAMPLE_RATE_UNKNOWN;
        int channels = itag != null ? itag.getAudioChannels() : ItagItem.AUDIO_CHANNELS_NOT_APPLICABLE_OR_UNKNOWN;

        // Full BCP 47 tag so regional dubs stay distinguishable
        Locale locale = stream.getAudioLocale();

        return builder()
                .id("audio-%d".formatted(index))
                .url(stream.getContent())
                .audioTrackId(stream.getAudioTrackId())
                .audioTrackName(stream.getAudioTrackName())
                .audioTrackType(stream.getAudioTrackType())
                .codec(stream.getCodec())
                .mimeType(format != null ? format.getMimeType() : null)
                .bandwidth(stream.getBitrate())
                .audioSamplingRate(sampleRate > 0 ? String.valueOf(sampleRate) : null)
                .audioChannels(channels > 0 ? channels : 2)
                .language(locale != null ? locale.toLanguageTag() : "und")
                .languageName(getLanguageDisplayName(locale != null ? locale.getLanguage() : "und"))
                .initRange(byteRange(stream.getInitStart(), stream.getInitEnd()))
                .indexRange(byteRange(stream.getIndexStart(), stream.getIndexEnd()))
                .format(format != null ? format.getName() : null)
                .build();
    }

    /** "start-end", or null when the stream reports no range. */
    private static String byteRange(int start, int end) {
        return start >= 0 && end > 0 ? "%d-%d".formatted(start, end) : null;
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
        private String audioTrackId;
        private String audioTrackName;
        private AudioTrackType audioTrackType;
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

        public Builder audioTrackId(String audioTrackId) {
            this.audioTrackId = audioTrackId;
            return this;
        }

        public Builder audioTrackName(String audioTrackName) {
            this.audioTrackName = audioTrackName;
            return this;
        }

        public Builder audioTrackType(AudioTrackType audioTrackType) {
            this.audioTrackType = audioTrackType;
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
            return new AudioStreamMetadataDTO(id, url, audioTrackId, audioTrackName, audioTrackType,
                    codec, mimeType, bandwidth, audioSamplingRate, audioChannels, language, languageName,
                    initRange, indexRange, format);
        }
    }

    /** Leaves out the URL, which is long and signed, to keep log lines short. */
    @Override
    public String toString() {
        return ("AudioStreamMetadataDTO{id='%s', codec='%s', bandwidth=%d, audioSamplingRate='%s', "
                + "audioChannels=%d, language='%s', languageName='%s'}")
                .formatted(id, codec, bandwidth, audioSamplingRate, audioChannels, language, languageName);
    }
}