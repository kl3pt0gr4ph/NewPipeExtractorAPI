package org.example.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;
import org.example.api.config.GlobalExceptionHandler;
import org.example.api.exception.ExtractionException;
import org.example.api.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.comments.CommentsInfo;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for CommentController.
 */
@DisplayName("CommentController Tests")
class CommentControllerTest {

    private static final String TEST_VIDEO_ID = "dQw4w9WgXcQ";
    private static final String VIDEO_URL = "https://www.youtube.com/watch?v=" + TEST_VIDEO_ID;

    private MockMvc mockMvc;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(commentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── GET /api/v1/comments ─────────────────────────────────────────────

    @Test
    @DisplayName("Returns 200 with mapped comments, requesting the URL built from the id")
    void getComments_returnsMappedComments() throws Exception {
        CommentsInfo mockInfo = mock(CommentsInfo.class);
        when(commentService.getCommentsInfo(VIDEO_URL)).thenReturn(mockInfo);

        mockMvc.perform(get("/api/v1/comments").param("id", TEST_VIDEO_ID).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(commentService).getCommentsInfo(VIDEO_URL);
        verify(commentService).mapCommentsToDto(mockInfo);
    }

    // ── GET /api/v1/comments/page (previously untested) ──────────────────

    @Test
    @DisplayName("Returns 200 with the comments page, passing the constructed URL and pageUrl")
    void getCommentsPage_passesConstructedUrlAndPageUrl() throws Exception {
        String pageUrl = "https://www.youtube.com/comments?continuation=token";
        @SuppressWarnings("unchecked")
        ListExtractor.InfoItemsPage<CommentsInfoItem> mockPage = mock(ListExtractor.InfoItemsPage.class);
        when(mockPage.getItems()).thenReturn(List.of());
        when(commentService.getCommentsPage(VIDEO_URL, pageUrl)).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/comments/page")
                        .param("id", TEST_VIDEO_ID)
                        .param("pageUrl", pageUrl)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(commentService).getCommentsPage(VIDEO_URL, pageUrl);
    }

    // ── Exception propagation (single advice-wiring test) ────────────────

    @Test
    @DisplayName("Propagates service failures to the exception handler")
    void propagatesFailuresToAdvice() throws Exception {
        when(commentService.getCommentsInfo(anyString()))
                .thenThrow(new ExtractionException("Failed to retrieve comments information"));

        mockMvc.perform(get("/api/v1/comments").param("id", TEST_VIDEO_ID).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("EXTRACTION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("Failed to retrieve")));
    }

    // ── Required parameter binding (both endpoints) ──────────────────────

    static Stream<Arguments> requestsMissingARequiredParam() {
        return Stream.of(
                Arguments.of("/comments without id", get("/api/v1/comments")),
                Arguments.of(
                        "/comments/page without id",
                        get("/api/v1/comments/page").param("pageUrl", "https://youtube.com/comments?c=t")),
                Arguments.of(
                        "/comments/page without pageUrl",
                        get("/api/v1/comments/page").param("id", TEST_VIDEO_ID)));
    }

    @ParameterizedTest(name = "{0} returns 400")
    @MethodSource("requestsMissingARequiredParam")
    @DisplayName("Missing required parameters return 400")
    void missingRequiredParam_returnsBadRequest(String name, MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }
}
