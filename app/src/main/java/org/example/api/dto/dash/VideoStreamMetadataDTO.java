
package org.example.api.dto.dash;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.schabi.newpipe.extractor.stream.VideoStream;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object representing metadata for a video stream in a DASH manifest.
 * Contains information about video quality, codec, dimensions, and segment locations.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VideoStreamMetadataDTO {

    /**
     * Unique identifier for this video representation in the DASH manifest.
     */
    @NotBlank(message = "Video stream ID cannot be blank")
    @JsonProperty("id")
    private String id;

    /**
     * Direct URL to the video stream content.
     */
    @NotBlank(message = "Video stream URL cannot be blank")
    @JsonProperty("url")
    private String url;

    /**
     * Video codec identifier (e.g., "avc1.640028", "vp09.00.50.08").
     */
    @NotBlank(message = "Video codec cannot be blank")
    @JsonProperty("codec")
    private String codec;

    /**
     * MIME type of the video stream (e.g., "video/mp4", "video/webm").
     */
    @NotBlank(message = "Video MIME type cannot be blank")
    @JsonProperty("mimeType")
    private String mimeType;

    /**
     * Video width in pixels.
     */
    @Min(value = 1, message = "Video width must be at least 1")
    @JsonProperty("width")
    private int width;

    /**
     * Video height in pixels.
     */
    @Min(value = 1, message = "Video height must be at least 1")
    @JsonProperty("height")
    private int height;

    /**
     * Frame rate of the video (e.g., "24", "30", "60").
     */
    @NotBlank(message = "Frame rate cannot be blank")
    @JsonProperty("frameRate")
    private String frameRate;

    /**
     * Bandwidth required for this stream in bits per second.
     */
    @Min(value = 1, message = "Bandwidth must be at least 1")
    @JsonProperty("bandwidth")
    private int bandwidth;

    /**
     * Byte range for initialization segment (e.g., "0-740").
     */
    @JsonProperty("initRange")
    private String initRange;

    /**
     * Byte range for index segment (e.g., "741-1048").
     */
    @JsonProperty("indexRange")
    private String indexRange;

    /**
     * Original format identifier from the extractor (optional, for debugging).
     */
    @JsonProperty("format")
    private String format;

    // Constructors

    /**
     * Default constructor for Jackson deserialization.
     */
    public VideoStreamMetadataDTO() {
    }

    /**
     * Full constructor for creating a VideoStreamMetadataDTO with all fields.
     */
    public VideoStreamMetadataDTO(String id, String url, String codec, String mimeType,
                                  int width, int height, String frameRate, int bandwidth,
                                  String initRange, String indexRange, String format) {
        this.id = id;
        this.url = url;
        this.codec = codec;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
        this.bandwidth = bandwidth;
        this.initRange = initRange;
        this.indexRange = indexRange;
        this.format = format;
    }

    // Static Factory Method

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

        VideoStreamMetadataDTO dto = new VideoStreamMetadataDTO();
        dto.setId("video-%d".formatted(index));
        dto.setUrl(stream.getContent());
        dto.setCodec(stream.getCodec());
        dto.setMimeType(stream.getFormat() != null ? stream.getFormat().getMimeType() : null);
        dto.setWidth(stream.getWidth());
        dto.setHeight(stream.getHeight());
        if (stream.getFps() > 0) {
            dto.setFrameRate(String.valueOf(stream.getFps()));
        }
        dto.setBandwidth(stream.getBitrate());

        // Extract init and index ranges if available
        if (stream.getInitStart() >= 0 && stream.getInitEnd() > 0) {
            dto.setInitRange("%d-%d".formatted(stream.getInitStart(), stream.getInitEnd()));
        }
        if (stream.getIndexStart() >= 0 && stream.getIndexEnd() > 0) {
            dto.setIndexRange("%d-%d".formatted(stream.getIndexStart(), stream.getIndexEnd()));
        }

        dto.setFormat(stream.getFormat() != null ? stream.getFormat().getName() : null);

        return dto;
    }

    // Builder Pattern

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
            return new VideoStreamMetadataDTO(id, url, codec, mimeType, width, height,
                    frameRate, bandwidth, initRange, indexRange, format);
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

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(String frameRate) {
        this.frameRate = frameRate;
    }

    public int getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(int bandwidth) {
        this.bandwidth = bandwidth;
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
        return "VideoStreamMetadataDTO{id='%s', width=%d, height=%d, frameRate='%s', bandwidth=%d, codec='%s', mimeType='%s'}".formatted(id, width, height, frameRate, bandwidth, codec, mimeType);
    }
}