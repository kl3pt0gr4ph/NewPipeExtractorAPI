package org.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.stream.Description;
import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.util.List;

/**
 * DTO for stream details including metadata like title, description,
 * view counts, likes, channel information, etc.
 */
@Schema(name = "StreamDetails", description = "The metadata a watch page shows for one video")
public class StreamDetailsDTO {

    @Schema(description = "Video title")
    @JsonProperty("videoTitle")
    private String videoTitle;

    @Schema(description = "Video description; `type` is 1 for HTML, 2 for Markdown, 3 for plain text")
    @JsonProperty("description")
    private Description description;

    @Schema(description = "Channel avatar in the sizes YouTube offers")
    @JsonProperty("uploaderAvatars")
    private List<Image> uploaderAvatars;

    @Schema(description = "Views, or -1 when unknown")
    @JsonProperty("viewCount")
    private long viewCount;

    @Schema(description = "Likes, or -1 when hidden or unknown")
    @JsonProperty("likeCount")
    private long likeCount;

    @Schema(description = "Dislikes. YouTube no longer exposes these, so this is normally -1.")
    @JsonProperty("dislikeCount")
    private long dislikeCount;

    @Schema(description = "Uploader channel name")
    @JsonProperty("channelName")
    private String channelName;

    @Schema(description = "Subscribers of the uploader channel, or -1 when hidden or unknown")
    @JsonProperty("channelSubscriberCount")
    private long channelSubscriberCount;

    @Schema(description = "Upload date as YouTube reports it. Currently an ISO-8601 timestamp with offset, "
            + "but it is passed through unparsed, so treat the format as best-effort.",
            example = "2021-10-29T16:00:13-07:00")
    @JsonProperty("uploadDate")
    private String uploadDate;

    public StreamDetailsDTO() {}

    /**
     * Creates a StreamDetailsDTO from a StreamInfo object.
     *
     * @param streamInfo The StreamInfo from NewPipe extractor
     * @return A new StreamDetailsDTO with mapped data
     */
    public static StreamDetailsDTO from(StreamInfo streamInfo) {
        StreamDetailsDTO dto = new StreamDetailsDTO();

        dto.setVideoTitle(streamInfo.getName());
        dto.setDescription(streamInfo.getDescription());
        dto.setUploaderAvatars(streamInfo.getUploaderAvatars());
        dto.setViewCount(streamInfo.getViewCount());
        dto.setLikeCount(streamInfo.getLikeCount());
        dto.setDislikeCount(streamInfo.getDislikeCount());
        dto.setChannelName(streamInfo.getUploaderName());
        dto.setChannelSubscriberCount(streamInfo.getUploaderSubscriberCount());
        dto.setUploadDate(streamInfo.getTextualUploadDate());

        return dto;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }

    public Description getDescription() {
        return description;
    }

    public void setDescription(Description description) {
        this.description = description;
    }

    public List<Image> getUploaderAvatars() {
        return uploaderAvatars;
    }

    public void setUploaderAvatars(List<Image> uploaderAvatars) {
        this.uploaderAvatars = uploaderAvatars;
    }

    public long getViewCount() {
        return viewCount;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(long likeCount) {
        this.likeCount = likeCount;
    }

    public long getDislikeCount() {
        return dislikeCount;
    }

    public void setDislikeCount(long dislikeCount) {
        this.dislikeCount = dislikeCount;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public long getChannelSubscriberCount() {
        return channelSubscriberCount;
    }

    public void setChannelSubscriberCount(long channelSubscriberCount) {
        this.channelSubscriberCount = channelSubscriberCount;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }
}