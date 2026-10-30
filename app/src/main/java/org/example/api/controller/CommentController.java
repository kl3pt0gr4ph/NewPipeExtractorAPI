package org.example.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.api.dto.CommentsDTO;
import org.example.api.service.CommentService;
import org.example.api.utils.ValidationUtils;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.comments.CommentsInfo;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.example.api.config.openapi.OpenApiDocs.*;

@Tag(name = TAG_COMMENTS)
@RestController
@RequestMapping("/api/v1")
public class CommentController {
    private static final String YOUTUBE_URL = "https://www.youtube.com/watch?v=";
    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "Comments", description = "First page of a video's comments.")
    @GetMapping("/comments")
    public ResponseEntity<CommentsDTO.CommentsResponseDto> getComments(
            @Parameter(description = VIDEO_ID, example = VIDEO_ID_EXAMPLE)
            @RequestParam(name = "id") String id) {
        logger.debug("Retrieving comments info for ID: {}", id);

        String url = YOUTUBE_URL + id;
        ValidationUtils.requireValidUrl(url);

        CommentsInfo comments = commentService.getCommentsInfo(url);
        CommentsDTO.CommentsResponseDto dto = commentService.mapCommentsToDto(comments);

        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Next page of comments",
            description = "A later page of a video's comments, built from `pageUrl` alone. Note that "
                    + "`/comments` doesn't return a `nextPage`, so the `pageUrl` has to come from "
                    + "elsewhere for now." + RAW_NEWPIPE_TYPE)
    @GetMapping("/comments/page")
    public ResponseEntity<ListExtractor.InfoItemsPage<CommentsInfoItem>> getCommentsPage(
            @Parameter(description = VIDEO_ID, example = VIDEO_ID_EXAMPLE)
            @RequestParam(name = "id") String id,
            @Parameter(description = "Continuation URL of the page to fetch")
            @RequestParam(name = "pageUrl") String pageUrl
    ) {
        logger.debug("Retrieving comments page for id: {}", id);

        String url = YOUTUBE_URL + id;
        ValidationUtils.requireValidUrl(url);
        ValidationUtils.requireValidUrl(pageUrl);

        ListExtractor.InfoItemsPage<CommentsInfoItem> commentsPageJson = commentService.getCommentsPage(
                url,
                pageUrl
        );

        return ResponseEntity.ok(commentsPageJson);
    }
}