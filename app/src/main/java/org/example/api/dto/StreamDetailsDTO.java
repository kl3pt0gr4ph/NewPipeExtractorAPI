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
public record StreamDetailsDTO (

        @Schema(description = "Video title") String videoTitle,
        @Schema(description = "Video description; `type` is 1 for HTML, 2 for Markdown, 3 for plain text") Description description,
        @Schema(description = "Channel avatar in the sizes YouTube offers") List<Image> uploaderAvatars,
        @Schema(description = "Views, or -1 when unknown") long viewCount,
        @Schema(description = "Likes, or -1 when hidden or unknown") long likeCount,
        @Schema(description = "Dislikes. YouTube no longer exposes these, so this is normally -1.") long dislikeCount,
        @Schema(description = "Uploader channel name") String channelName,
        @Schema(description = "Subscribers of the uploader channel, or -1 when hidden or unknown") long channelSubscriberCount,
        @Schema(description = "Upload date as YouTube reports it. Currently an ISO-8601 timestamp with offset, "
                + "but it is passed through unparsed, so treat the format as best-effort.",
                example = "2021-10-29T16:00:13-07:00") String uploadDate) {

    public StreamDetailsDTO {
        uploaderAvatars = uploaderAvatars == null ? List.of() : List.copyOf(uploaderAvatars);
    }

    public static StreamDetailsDTO from(StreamInfo streamInfo) {
        return new StreamDetailsDTO(
                streamInfo.getName(),
                streamInfo.getDescription(),
                streamInfo.getUploaderAvatars(),
                streamInfo.getViewCount(),
                streamInfo.getLikeCount(),
                streamInfo.getDislikeCount(),
                streamInfo.getUploaderName(),
                streamInfo.getUploaderSubscriberCount(),
                streamInfo.getTextualUploadDate());
    }
}