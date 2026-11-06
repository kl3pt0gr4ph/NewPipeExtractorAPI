package org.example.api.service;

import org.example.api.dto.search.SearchItemDTO;
import org.example.api.dto.search.SearchPageDTO;
import org.example.api.dto.search.SearchResultDTO;
import org.example.api.exception.ErrorCode;
import org.example.api.exception.ExtractionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SearchService: deduplication, page reconstruction,
 * extractor initialisation order, and error wrapping.
 */
@DisplayName("SearchService Tests")
class SearchServiceTest {

    private SearchService searchService;
    private StreamingService mockStreamingService;
    private SearchExtractor mockSearchExtractor;

    @BeforeEach
    void setUp() throws Exception {
        searchService = new SearchService();
        mockStreamingService = mock(StreamingService.class);
        SearchQueryHandlerFactory mockQueryHandlerFactory = mock(SearchQueryHandlerFactory.class);
        mockSearchExtractor = mock(SearchExtractor.class);

        SearchQueryHandler mockHandler = mock(SearchQueryHandler.class);
        when(mockStreamingService.getSearchQHFactory()).thenReturn(mockQueryHandlerFactory);
        when(mockQueryHandlerFactory.fromQuery(any(), any(), any())).thenReturn(mockHandler);
        when(mockStreamingService.getSearchExtractor((SearchQueryHandler) any())).thenReturn(mockSearchExtractor);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private SearchInfo mockSearchInfo(String searchString, List<InfoItem> items) {
        SearchInfo info = mock(SearchInfo.class);
        when(info.getSearchString()).thenReturn(searchString);
        when(info.getRelatedItems()).thenReturn(items);
        when(info.getNextPage()).thenReturn(null);
        return info;
    }

    @SuppressWarnings("unchecked")
    private ListExtractor.InfoItemsPage<InfoItem> mockPage(List<InfoItem> items) {
        ListExtractor.InfoItemsPage<InfoItem> page = mock(ListExtractor.InfoItemsPage.class);
        when(page.getItems()).thenReturn(items);
        when(page.getNextPage()).thenReturn(null);
        return page;
    }

    /**
     * Minimal concrete StreamInfoItem subclass. StreamInfoItem has final
     * methods (getName, getUrl, getThumbnails) that Mockito cannot stub,
     * so we use an anonymous subclass.
     */
    private StreamInfoItem stubInfoItem(String name, String url) {
        return new StreamInfoItem(0, url, name, StreamType.VIDEO_STREAM) {
            @Override
            public java.util.List<org.schabi.newpipe.extractor.Image> getThumbnails() {
                return List.of();
            }
        };
    }

    // ── getSearchInfo ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSearchInfo")
    class GetSearchInfoTests {

        @Test
        @DisplayName("Returns the search string with URL-deduplicated items, keeping first occurrence and order")
        void returnsDeduplicatedResults() throws Exception {
            // One scenario covering what were three tests: dedup collapses
            // the duplicate URL, the FIRST occurrence's data wins, and
            // insertion order is preserved (LinkedHashMap merge).
            InfoItem first = stubInfoItem("Video A", "https://youtube.com/watch?v=same");
            InfoItem duplicate = stubInfoItem("Video A duplicate", "https://youtube.com/watch?v=same");
            InfoItem unique = stubInfoItem("Video B", "https://youtube.com/watch?v=unique");

            SearchInfo mockInfo = mockSearchInfo("java tutorial", List.of(first, duplicate, unique));

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class);
                 MockedStatic<SearchInfo> searchInfoMock = mockStatic(SearchInfo.class)) {

                newPipeMock.when(() -> NewPipe.getService(0)).thenReturn(mockStreamingService);
                searchInfoMock.when(() -> SearchInfo.getInfo(any(SearchExtractor.class)))
                        .thenReturn(mockInfo);

                SearchResultDTO result = searchService.getSearchInfo(
                        "java tutorial", Collections.emptyList(), null);

                assertEquals("java tutorial", result.getSearchString());
                assertEquals(
                        List.of("Video A", "Video B"),
                        result.getItems().stream().map(SearchItemDTO::getName).toList());
            }
        }

        @Test
        @DisplayName("Returns empty items when the search has no results")
        void returnsEmptyItems_whenNoResults() throws Exception {
            SearchInfo mockInfo = mockSearchInfo("no results query", Collections.emptyList());

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class);
                 MockedStatic<SearchInfo> searchInfoMock = mockStatic(SearchInfo.class)) {

                newPipeMock.when(() -> NewPipe.getService(0)).thenReturn(mockStreamingService);
                searchInfoMock.when(() -> SearchInfo.getInfo(any(SearchExtractor.class)))
                        .thenReturn(mockInfo);

                SearchResultDTO result = searchService.getSearchInfo(
                        "no results query", Collections.emptyList(), null);

                assertNotNull(result.getItems());
                assertTrue(result.getItems().isEmpty());
            }
        }

        @Test
        @DisplayName("Translates NewPipe failures, keeping their text out of the message")
        void translatesFailures() {
            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class)) {
                newPipeMock.when(() -> NewPipe.getService(anyInt()))
                        .thenThrow(new ReCaptchaException("reCaptcha Challenge requested", "https://www.youtube.com"));

                ExtractionException exception = assertThrows(ExtractionException.class, () ->
                        searchService.getSearchInfo("test", Collections.emptyList(), null));

                assertEquals(ErrorCode.UPSTREAM_BLOCKED, exception.getCode());
                assertFalse(exception.getMessage().contains("reCaptcha"));
                assertNotNull(exception.getCause());
            }
        }
    }

    // ── getSearchPage ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSearchPage")
    class GetSearchPageTests {

        private static final String PAGE_URL = "https://www.youtube.com/youtubei/v1/search?prettyPrint=false";
        private static final String PAGE_ID = "4qmFsgJcEBIYdmlkZW8token";

        @Test
        @DisplayName("Initialises the extractor before requesting the reconstructed page, and itemCount reflects deduplication")
        void initialisesExtractorAndRequestsReconstructedPage() throws Exception {
            // Duplicate URLs in the page verify the dedup wiring on this
            // path too — and itemCount, which only the page path sets.
            InfoItem first = stubInfoItem("First Video", "https://youtube.com/watch?v=dup");
            InfoItem duplicate = stubInfoItem("Second Video (dup)", "https://youtube.com/watch?v=dup");

            ListExtractor.InfoItemsPage<InfoItem> page = mockPage(List.of(first, duplicate));

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class)) {
                newPipeMock.when(() -> NewPipe.getService(0)).thenReturn(mockStreamingService);
                when(mockSearchExtractor.getPage(any(Page.class))).thenReturn(page);

                SearchPageDTO result = searchService.getSearchPage(
                        "java", Collections.emptyList(), null, PAGE_URL, PAGE_ID);

                // fetchPage() MUST precede getPage(): skipping the InnerTube
                // session initialisation yields empty pages (see service javadoc).
                InOrder callOrder = inOrder(mockSearchExtractor);
                callOrder.verify(mockSearchExtractor).fetchPage();
                callOrder.verify(mockSearchExtractor).getPage(argThat(p ->
                        PAGE_URL.equals(p.getUrl()) && PAGE_ID.equals(p.getId())));

                assertEquals(1, result.getItems().size());
                assertEquals("First Video", result.getItems().get(0).getName());
                assertEquals(1, result.getItemCount());
            }
        }

        @Test
        @DisplayName("Handles a null pageId without throwing")
        void handlesNullPageId() throws Exception {
            ListExtractor.InfoItemsPage<InfoItem> page = mockPage(Collections.emptyList());

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class)) {
                newPipeMock.when(() -> NewPipe.getService(0)).thenReturn(mockStreamingService);
                when(mockSearchExtractor.getPage(any(Page.class))).thenReturn(page);

                SearchPageDTO result = searchService.getSearchPage(
                        "test", Collections.emptyList(), null, PAGE_URL, null);

                assertNotNull(result);
            }
        }

        @Test
        @DisplayName("Translates NewPipe failures, keeping their text out of the message")
        void translatesFailures() {
            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class)) {
                newPipeMock.when(() -> NewPipe.getService(anyInt()))
                        .thenThrow(new ParsingException("Pagination failure"));

                ExtractionException exception = assertThrows(ExtractionException.class, () ->
                        searchService.getSearchPage(
                                "test", Collections.emptyList(), null,
                                "https://youtube.com/next", "someToken"));

                assertEquals(ErrorCode.UPSTREAM_CHANGED, exception.getCode());
                assertFalse(exception.getMessage().contains("Pagination failure"));
            }
        }
    }
}