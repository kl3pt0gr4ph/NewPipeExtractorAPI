package org.example.api.service;

import java.util.List;
import org.example.api.dto.CommentsDTO;
import org.example.api.exception.ExtractionException;
import org.example.api.exception.NewPipeExceptionMapper;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.comments.CommentsInfo;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CommentService {
    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    public CommentsInfo getCommentsInfo(String url) throws ExtractionException {
        try {
            CommentsInfo info = CommentsInfo.getInfo(url);

            logger.info("Extracted {} comment(s)", info.getRelatedItems().size());
            return info;
        } catch (Exception e) {
            throw NewPipeExceptionMapper.translate(e);
        }
    }

    public ListExtractor.InfoItemsPage<CommentsInfoItem> getCommentsPage(String url, String pageUrl)
            throws ExtractionException {
        try {
            // TODO optimize this. init page is fetched every time
            logger.debug("Extracting comments page for URL: {}", url);
            CommentsInfo info = CommentsInfo.getInfo(url);
            Page pageInstance = new Page(pageUrl);
            return CommentsInfo.getMoreItems(info, pageInstance);
        } catch (Exception e) {
            throw NewPipeExceptionMapper.translate(e);
        }
    }

    public CommentsDTO.CommentsResponseDto mapCommentsToDto(CommentsInfo info) {
        List<CommentsDTO.CommentItemDto> items = info.getRelatedItems().stream()
                .map(item -> {
                    List<CommentsDTO.AvatarDto> thumbnails = item.getThumbnails().stream()
                            .map(t -> new CommentsDTO.AvatarDto(t.getUrl(), t.getHeight(), t.getWidth()))
                            .toList();

                    List<CommentsDTO.AvatarDto> avatars = item.getUploaderAvatars().stream()
                            .map(a -> new CommentsDTO.AvatarDto(a.getUrl(), a.getHeight(), a.getWidth()))
                            .toList();

                    CommentsDTO.RepliesDto replies = null;
                    if (item.getReplies() != null) {
                        replies = new CommentsDTO.RepliesDto(
                                item.getReplies().getUrl(), item.getReplies().getId());
                    }

                    CommentsDTO.UploadDateDto uploadDate = null;
                    if (item.getUploadDate() != null) {
                        uploadDate = new CommentsDTO.UploadDateDto(
                                item.getUploadDate().isApproximation());
                    }

                    return new CommentsDTO.CommentItemDto(
                            "COMMENT",
                            item.getServiceId(),
                            item.getUrl(),
                            item.getName(),
                            thumbnails,
                            item.getCommentId(),
                            item.getCommentText().getContent(),
                            item.getCommentText().getType(),
                            item.getUploaderName(),
                            avatars,
                            item.getUploaderUrl(),
                            item.isUploaderVerified(),
                            item.getTextualUploadDate(),
                            uploadDate,
                            item.getLikeCount(),
                            item.getTextualLikeCount(),
                            item.isHeartedByUploader(),
                            item.isPinned(),
                            item.getStreamPosition(),
                            item.getReplyCount(),
                            replies,
                            item.isChannelOwner());
                })
                .toList();

        return new CommentsDTO.CommentsResponseDto(
                info.getServiceId(),
                info.getId(),
                info.getUrl(),
                info.getOriginalUrl(),
                info.getName(),
                List.of(),
                items);
    }
}
