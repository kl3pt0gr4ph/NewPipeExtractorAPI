package org.example.api.dto.dash;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.VideoStream;

/**
 * Data Transfer Object representing metadata for a video stream in a DASH manifest.
 * Contains information about video quality, codec, dimensions, and segment locations.
 *
 * @param id         Unique identifier for this video representation in the DASH manifest
 * @param url        Direct URL to the video stream content
 * @param codec      Video codec identifier (e.g., "avc1.640028", "vp09.00.50.08")
 * @param mimeType   MIME type of the video stream (e.g., "video/mp4", "video/webm")
 * @param width      Video width in pixels
 * @param height     Video height in pixels
 * @param frameRate  Frame rate of the video (e.g., "24", "30", "60")
 * @param bandwidth  Bandwidth required for this stream in bits per second
 * @param initRange  Byte range for initialization segment (e.g., "0-740")
 * @param indexRange Byte range for index segment (e.g., "741-1048")
 * @param format     Original format identifier from the extractor (optional, for debugging)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VideoStreamMetadataDTO(
        @NotBlank(message = "Video stream ID cannot be blank") String id,

        @NotBlank(message = "Video stream URL cannot be blank") String url,

        @NotBlank(message = "Video codec cannot be blank") String codec,

        @NotBlank(message = "Video MIME type cannot be blank") String mimeType,

        @Min(value = 1, message = "Video width must be at least 1") int width,

        @Min(value = 1, message = "Video height must be at least 1") int height,

        @NotBlank(message = "Frame rate cannot be blank") String frameRate,

        @Min(value = 1, message = "Bandwidth must be at least 1") int bandwidth,

        String initRange,
        String indexRange,
        String format) {

    /**
     * Creates a VideoStreamMetadataDTO from a NewPipe VideoStream object.
     *
     * @param stream The VideoStream from NewPipe extractor
     * @param index  The index used to generate a unique ID
     * @return A new VideoStreamMetadataDTO instance
     */
    public static VideoStreamMetadataDTO from(VideoStream stream, int index) {
        if (stream == null) {
            throw new IllegalArgumentException("VideoStream cannot be null");
        }
        MediaFormat format = stream.getFormat();

        return builder()
                .id("video-%d".formatted(index))
                .url(stream.getContent())
                .codec(stream.getCodec())
                .mimeType(format != null ? format.getMimeType() : null)
                .width(stream.getWidth())
                .height(stream.getHeight())
                .frameRate(stream.getFps() > 0 ? String.valueOf(stream.getFps()) : null)
                .bandwidth(stream.getBitrate())
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
     * Creates a new Builder for constructing VideoStreamMetadataDTO instances.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for fluent construction of VideoStreamMetadataDTO instances.
     */
    public static class Builder {
        private String id;
        private String url;
        private String codec;
        private String mimeType;
        private int width;
        private int height;
        private String frameRate;
        private int bandwidth;
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

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder frameRate(String frameRate) {
            this.frameRate = frameRate;
            return this;
        }

        public Builder bandwidth(int bandwidth) {
            this.bandwidth = bandwidth;
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

        public VideoStreamMetadataDTO build() {
            return new VideoStreamMetadataDTO(
                    id, url, codec, mimeType, width, height, frameRate, bandwidth, initRange, indexRange, format);
        }
    }

    /** Leaves out the URL, which is long and signed, to keep log lines short. */
    @Override
    public String toString() {
        return "VideoStreamMetadataDTO{id='%s', width=%d, height=%d, frameRate='%s', bandwidth=%d, codec='%s', mimeType='%s'}"
                .formatted(id, width, height, frameRate, bandwidth, codec, mimeType);
    }
}
