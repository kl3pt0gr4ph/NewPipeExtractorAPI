package org.example.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.example.api.dto.CommentsDTO;
import org.example.api.exception.ErrorCode;
import org.example.api.exception.ExtractionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.comments.CommentsInfo;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.stream.Description;

/**
 * Unit tests for CommentService.
 */
@DisplayName("CommentService Tests")
class CommentServiceTest {

    private static final String TEST_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService();
    }

    // ── getCommentsInfo ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getCommentsInfo")
    class GetCommentsInfoTests {

        @Test
        @DisplayName("Returns the CommentsInfo from the extractor")
        void returnsCommentsInfo() throws Exception {
            CommentsInfo mockInfo = mock(CommentsInfo.class);

            try (MockedStatic<CommentsInfo> commentsInfoMock = mockStatic(CommentsInfo.class)) {
                commentsInfoMock.when(() -> CommentsInfo.getInfo(TEST_URL)).thenReturn(mockInfo);

                assertSame(mockInfo, commentService.getCommentsInfo(TEST_URL));
            }
        }

        @Test
        @DisplayName("Translates failures, keeping the cause and its text out of the message")
        void translatesFailures() {
            try (MockedStatic<CommentsInfo> commentsInfoMock = mockStatic(CommentsInfo.class)) {
                commentsInfoMock
                        .when(() -> CommentsInfo.getInfo(TEST_URL))
                        .thenThrow(new IOException("Connection reset"));

                ExtractionException exception =
                        assertThrows(ExtractionException.class, () -> commentService.getCommentsInfo(TEST_URL));

                assertEquals(ErrorCode.UPSTREAM_UNREACHABLE, exception.getCode());
                assertFalse(exception.getMessage().contains("Connection reset"));
                assertNotNull(exception.getCause());
            }
        }
    }

    // ── getCommentsPage (previously untested) ────────────────────────────

    @Nested
    @DisplayName("getCommentsPage")
    class GetCommentsPageTests {

        @Test
        @DisplayName("Fetches the info, reconstructs the Page from pageUrl, and returns the page items")
        void fetchesInfoAndRequestsPage() throws Exception {
            String pageUrl = "https://www.youtube.com/comments?continuation=token";
            CommentsInfo mockInfo = mock(CommentsInfo.class);
            @SuppressWarnings("unchecked")
            ListExtractor.InfoItemsPage<CommentsInfoItem> mockPage = mock(ListExtractor.InfoItemsPage.class);

            try (MockedStatic<CommentsInfo> commentsInfoMock = mockStatic(CommentsInfo.class)) {
                // Documents the current behaviour flagged by the TODO in the
                // service: getInfo IS re-fetched for every page request.
                commentsInfoMock.when(() -> CommentsInfo.getInfo(TEST_URL)).thenReturn(mockInfo);
                commentsInfoMock
                        .when(() -> CommentsInfo.getMoreItems(
                                eq(mockInfo), argThat((Page p) -> pageUrl.equals(p.getUrl()))))
                        .thenReturn(mockPage);

                ListExtractor.InfoItemsPage<CommentsInfoItem> result =
                        commentService.getCommentsPage(TEST_URL, pageUrl);

                assertSame(mockPage, result);
            }
        }

        @Test
        @DisplayName("Translates failures, keeping the cause and its text out of the message")
        void translatesFailures() {
            try (MockedStatic<CommentsInfo> commentsInfoMock = mockStatic(CommentsInfo.class)) {
                commentsInfoMock
                        .when(() -> CommentsInfo.getInfo(TEST_URL))
                        .thenThrow(new ParsingException("Page failure"));

                ExtractionException exception = assertThrows(
                        ExtractionException.class, () -> commentService.getCommentsPage(TEST_URL, "https://page.url"));

                assertEquals(ErrorCode.UPSTREAM_CHANGED, exception.getCode());
                assertFalse(exception.getMessage().contains("Page failure"));
                assertNotNull(exception.getCause());
            }
        }
    }

    // ── mapCommentsToDto (previously untested) ───────────────────────────

    @Nested
    @DisplayName("mapCommentsToDto")
    class MapCommentsToDtoTests {

        @Test
        @DisplayName(
                "Maps a fully-populated comment and a minimal comment (null replies/uploadDate) to the response DTO")
        void mapsFullAndMinimalComments() {
            // One fully-populated item exercises every mapped field including
            // the replies and uploadDate branches; one minimal item exercises
            // both null branches. Whole-record equality catches any drift.
            OffsetDateTime uploadedAt = OffsetDateTime.of(2026, 7, 1, 12, 0, 0, 0, ZoneOffset.UTC);

            CommentsInfoItem full = new CommentsInfoItem(0, "https://youtube.com/comment/1", "Alice");
            full.setThumbnails(List.of(new Image("https://img/thumb.jpg", 48, 48, Image.ResolutionLevel.LOW)));
            full.setCommentId("comment-1");
            full.setCommentText(new Description("Great video!", Description.PLAIN_TEXT));
            full.setUploaderName("Alice");
            full.setUploaderAvatars(List.of(new Image("https://img/avatar.jpg", 32, 32, Image.ResolutionLevel.LOW)));
            full.setUploaderUrl("https://youtube.com/@alice");
            full.setUploaderVerified(true);
            full.setTextualUploadDate("2 days ago");
            full.setUploadDate(new DateWrapper(uploadedAt, true));
            full.setLikeCount(42);
            full.setTextualLikeCount("42");
            full.setHeartedByUploader(true);
            full.setPinned(true);
            full.setStreamPosition(10);
            full.setReplyCount(3);
            full.setReplies(new Page("https://replies.url", "replies-id"));
            full.setChannelOwner(true);

            CommentsInfoItem minimal = new CommentsInfoItem(0, "https://youtube.com/comment/2", "Bob");
            minimal.setThumbnails(List.of());
            minimal.setCommentId("comment-2");
            minimal.setCommentText(new Description("First", Description.PLAIN_TEXT));
            minimal.setUploaderName("Bob");
            minimal.setUploaderAvatars(List.of());
            minimal.setUploaderUrl("https://youtube.com/@bob");
            // replies and uploadDate deliberately left null

            CommentsInfo info = mock(CommentsInfo.class);
            when(info.getServiceId()).thenReturn(0);
            when(info.getId()).thenReturn("dQw4w9WgXcQ");
            when(info.getUrl()).thenReturn(TEST_URL);
            when(info.getOriginalUrl()).thenReturn(TEST_URL);
            when(info.getName()).thenReturn("Comments");
            when(info.getRelatedItems()).thenReturn(List.of(full, minimal));

            CommentsDTO.CommentsResponseDto expected = new CommentsDTO.CommentsResponseDto(
                    0,
                    "dQw4w9WgXcQ",
                    TEST_URL,
                    TEST_URL,
                    "Comments",
                    List.of(),
                    List.of(
                            new CommentsDTO.CommentItemDto(
                                    "COMMENT",
                                    0,
                                    "https://youtube.com/comment/1",
                                    "Alice",
                                    List.of(new CommentsDTO.AvatarDto("https://img/thumb.jpg", 48, 48)),
                                    "comment-1",
                                    "Great video!",
                                    Description.PLAIN_TEXT,
                                    "Alice",
                                    List.of(new CommentsDTO.AvatarDto("https://img/avatar.jpg", 32, 32)),
                                    "https://youtube.com/@alice",
                                    true,
                                    "2 days ago",
                                    new CommentsDTO.UploadDateDto(true),
                                    42,
                                    "42",
                                    true,
                                    true,
                                    10,
                                    3,
                                    new CommentsDTO.RepliesDto("https://replies.url", "replies-id"),
                                    true),
                            new CommentsDTO.CommentItemDto(
                                    "COMMENT",
                                    0,
                                    "https://youtube.com/comment/2",
                                    "Bob",
                                    List.of(),
                                    "comment-2",
                                    "First",
                                    Description.PLAIN_TEXT,
                                    "Bob",
                                    List.of(),
                                    "https://youtube.com/@bob",
                                    false,
                                    null,
                                    null, // uploadDate branch: null in, null out
                                    0,
                                    null,
                                    false,
                                    false,
                                    0,
                                    0,
                                    null, // replies branch: null in, null out
                                    false)));

            CommentsDTO.CommentsResponseDto actual = commentService.mapCommentsToDto(info);

            assertEquals(expected, actual);
        }

        @Test
        @DisplayName("Maps an info with no comments to an empty items list")
        void mapsEmptyComments() {
            CommentsInfo info = mock(CommentsInfo.class);
            when(info.getServiceId()).thenReturn(0);
            when(info.getId()).thenReturn("dQw4w9WgXcQ");
            when(info.getUrl()).thenReturn(TEST_URL);
            when(info.getOriginalUrl()).thenReturn(TEST_URL);
            when(info.getName()).thenReturn("Comments");
            when(info.getRelatedItems()).thenReturn(List.of());

            CommentsDTO.CommentsResponseDto expected = new CommentsDTO.CommentsResponseDto(
                    0, "dQw4w9WgXcQ", TEST_URL, TEST_URL, "Comments", List.of(), List.of());

            assertEquals(expected, commentService.mapCommentsToDto(info));
        }
    }
}
