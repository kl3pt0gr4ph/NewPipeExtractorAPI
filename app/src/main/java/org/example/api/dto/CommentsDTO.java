package org.example.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Response shapes for {@code /comments}. */
public class CommentsDTO {
    @Schema(name = "CommentImage")
    public record AvatarDto(String url, int height, int width) {}

    @Schema(name = "CommentReplies", description = "Continuation for this comment's replies")
    public record RepliesDto(String url, String id) {}

    @Schema(name = "CommentUploadDate")
    public record UploadDateDto(
            @Schema(description = "Whether the date was derived from a relative one, so is approximate")
            boolean approximation
    ) {}

    @Schema(name = "Comment")
    public record CommentItemDto(
            @Schema(description = "Always `COMMENT`", example = "COMMENT") String infoType,
            @Schema(description = "NewPipe service ID; 0 is YouTube")
            int serviceId,
            String url,
            String name,
            List<AvatarDto> thumbnails,
            String commentId,
            String commentText,
            @Schema(description = "Format of `commentText`: 1 = HTML, 2 = Markdown, 3 = plain text",
                    allowableValues = {"1", "2", "3"})
            int commentTextType,
            String uploaderName,
            List<AvatarDto> uploaderAvatars,
            String uploaderUrl,
            boolean uploaderVerified,
            @Schema(description = "Relative date as YouTube words it", example = "2 years ago")
            String textualUploadDate,
            @Schema(description = "Absent when no date could be parsed") UploadDateDto uploadDate,
            @Schema(description = "Likes, or -1 when unknown") int likeCount,
            @Schema(description = "Likes as YouTube displays them", example = "1.2K")
            String textualLikeCount,
            boolean heartedByUploader,
            boolean pinned,
            @Schema(description = "Timestamp in seconds the comment links to, or -1 when none")
            int streamPosition,
            @Schema(description = "Number of replies, or -1 when unknown") int replyCount,
            @Schema(description = "Absent when the comment has no replies") RepliesDto replies,
            @Schema(description = "Whether the commenter owns the video's channel") boolean channelOwner
    ) {}

    @Schema(name = "Comments", description = "First page of a video's comments. There is no `nextPage`; "
            + "see `/comments/page`.")
    public record CommentsResponseDto(
            @Schema(description = "NewPipe service ID; 0 is YouTube") int serviceId,
            String id,
            String url,
            String originalUrl,
            String name,
            @Schema(description = "Always empty; extraction failures surface as an error response")
            List<Object> errors,
            @Schema(description = "Comments on this page") List<CommentItemDto> relatedItems
    ) {}
}