package org.example.api.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;
import org.example.api.dto.channels.ChannelTabDTO;
import org.example.api.exception.ErrorCode;
import org.example.api.exception.ExtractionException;
import org.example.api.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

/**
 * Unit tests for ChannelTabService.
 */
@DisplayName("ChannelTabService Tests")
class ChannelTabServiceTest {

    private ChannelTabService channelTabService;

    @BeforeEach
    void setUp() {
        channelTabService = new ChannelTabService();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Mocks wired for a channel exposing a single tab. */
    private record TabMocks(
            StreamingService service,
            ChannelInfo channelInfo,
            ListLinkHandler tabHandler,
            ChannelTabExtractor extractor) {}

    private TabMocks stubChannelWithTab(String channelUrl, String tab) throws Exception {
        ListLinkHandler handler = mock(ListLinkHandler.class);
        when(handler.getUrl()).thenReturn(channelUrl + "/" + tab);
        when(handler.getContentFilters()).thenReturn(List.of(tab));

        ChannelInfo channelInfo = mock(ChannelInfo.class);
        when(channelInfo.getTabs()).thenReturn(List.of(handler));

        StreamingService service = mock(StreamingService.class);
        ChannelTabExtractor extractor = mock(ChannelTabExtractor.class);
        when(service.getChannelTabExtractor(handler)).thenReturn(extractor);

        return new TabMocks(service, channelInfo, handler, extractor);
    }

    private void wireStatics(
            MockedStatic<NewPipe> newPipeMock,
            MockedStatic<ChannelInfo> channelInfoMock,
            String channelUrl,
            TabMocks mocks) {
        newPipeMock.when(() -> NewPipe.getServiceByUrl(channelUrl)).thenReturn(mocks.service());
        channelInfoMock.when(() -> ChannelInfo.getInfo(channelUrl)).thenReturn(mocks.channelInfo());
    }

    @SuppressWarnings("unchecked")
    private InfoItemsPage<InfoItem> mockEmptyPage() {
        InfoItemsPage<InfoItem> page = mock(InfoItemsPage.class);
        when(page.getItems()).thenReturn(List.of());
        when(page.getNextPage()).thenReturn(null);
        when(page.hasNextPage()).thenReturn(false);
        return page;
    }

    // ── getChannelTab ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getChannelTab()")
    class GetChannelTabTests {

        private static final String CHANNEL_URL = "https://www.youtube.com/@LinusTechTips";
        private static final String CHANNEL_ID = "@LinusTechTips";

        @Test
        @DisplayName("Initialises the extractor with fetchPage() and returns the populated DTO")
        void initialisesExtractorAndReturnsDto() throws Exception {
            TabMocks mocks = stubChannelWithTab(CHANNEL_URL, "videos");
            ChannelTabInfo tabInfo = mock(ChannelTabInfo.class);
            when(tabInfo.getRelatedItems()).thenReturn(List.of());
            when(tabInfo.getNextPage()).thenReturn(null);

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class);
                    MockedStatic<ChannelInfo> channelInfoMock = mockStatic(ChannelInfo.class);
                    MockedStatic<ChannelTabInfo> tabInfoMock = mockStatic(ChannelTabInfo.class)) {

                wireStatics(newPipeMock, channelInfoMock, CHANNEL_URL, mocks);
                tabInfoMock
                        .when(() -> ChannelTabInfo.getInfo(mocks.extractor()))
                        .thenReturn(tabInfo);

                ChannelTabDTO result = channelTabService.getChannelTab(CHANNEL_URL, "videos", CHANNEL_ID);

                // Session initialisation before extraction (see service javadoc)
                verify(mocks.extractor()).fetchPage();

                assertEquals("videos", result.getTab());
                assertEquals(CHANNEL_ID, result.getChannelId());
                assertNotNull(result.getItems());
                assertNull(result.getNextPage());
            }
        }

        @Test
        @DisplayName("Finds the tab handler case-insensitively")
        void findsTabCaseInsensitively() throws Exception {
            // Handler registered lowercase; request uppercase
            TabMocks mocks = stubChannelWithTab(CHANNEL_URL, "videos");
            ChannelTabInfo tabInfo = mock(ChannelTabInfo.class);
            when(tabInfo.getRelatedItems()).thenReturn(List.of());
            when(tabInfo.getNextPage()).thenReturn(null);

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class);
                    MockedStatic<ChannelInfo> channelInfoMock = mockStatic(ChannelInfo.class);
                    MockedStatic<ChannelTabInfo> tabInfoMock = mockStatic(ChannelTabInfo.class)) {

                wireStatics(newPipeMock, channelInfoMock, CHANNEL_URL, mocks);
                tabInfoMock
                        .when(() -> ChannelTabInfo.getInfo(mocks.extractor()))
                        .thenReturn(tabInfo);

                ChannelTabDTO result = channelTabService.getChannelTab(CHANNEL_URL, "VIDEOS", CHANNEL_ID);

                assertNotNull(result);
            }
        }

        @Test
        @DisplayName("A tab the channel doesn't have is 400 VALIDATION_ERROR naming it and the available ones")
        void throwsWhenTabNotFound() throws Exception {
            TabMocks mocks = stubChannelWithTab(CHANNEL_URL, "videos");

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class);
                    MockedStatic<ChannelInfo> channelInfoMock = mockStatic(ChannelInfo.class)) {

                wireStatics(newPipeMock, channelInfoMock, CHANNEL_URL, mocks);

                ValidationException ex = assertThrows(
                        ValidationException.class,
                        () -> channelTabService.getChannelTab(CHANNEL_URL, "playlists", CHANNEL_ID));

                assertEquals(ErrorCode.VALIDATION_ERROR, ex.getCode());
                assertTrue(ex.getMessage().contains("no 'playlists' tab"));
                assertTrue(ex.getMessage().contains("videos")); // available tabs listed
            }
        }

        private ChannelTabInfo tabInfoWith(List<? extends InfoItem> items, List<Throwable> errors) {
            ChannelTabInfo tabInfo = mock(ChannelTabInfo.class);
            Mockito.<List<? extends InfoItem>>when(tabInfo.getRelatedItems()).thenReturn(items);
            when(tabInfo.getErrors()).thenReturn(errors);
            when(tabInfo.getNextPage()).thenReturn(null);
            return tabInfo;
        }

        @Test
        @DisplayName("Fails instead of returning an empty tab when NewPipe recorded an error, translating that error")
        void failsWhenTabIsEmptyBecauseOfAnError() throws Exception {
            // ChannelTabInfo.getInfo catches a failed initial page into getErrors()
            ParsingException swallowed = new ParsingException("Could not get tab content");
            TabMocks mocks = stubChannelWithTab(CHANNEL_URL, "shorts");
            ChannelTabInfo tabInfo = tabInfoWith(List.of(), List.of(swallowed));

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class);
                    MockedStatic<ChannelInfo> channelInfoMock = mockStatic(ChannelInfo.class);
                    MockedStatic<ChannelTabInfo> tabInfoMock = mockStatic(ChannelTabInfo.class)) {

                wireStatics(newPipeMock, channelInfoMock, CHANNEL_URL, mocks);
                tabInfoMock
                        .when(() -> ChannelTabInfo.getInfo(mocks.extractor()))
                        .thenReturn(tabInfo);

                ExtractionException ex = assertThrows(
                        ExtractionException.class,
                        () -> channelTabService.getChannelTab(CHANNEL_URL, "shorts", CHANNEL_ID));

                assertEquals(ErrorCode.UPSTREAM_CHANGED, ex.getCode());
                assertFalse(ex.getMessage().contains("Could not get tab content"));
                assertSame(swallowed, ex.getCause());
            }
        }

        @Test
        @DisplayName("Returns the items when errors were recorded alongside them")
        void returnsItemsDespitePartialErrors() throws Exception {
            TabMocks mocks = stubChannelWithTab(CHANNEL_URL, "videos");
            ChannelTabInfo tabInfo = tabInfoWith(
                    List.of(mock(StreamInfoItem.class)), List.of(new ParsingException("Could not get upload date")));

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class);
                    MockedStatic<ChannelInfo> channelInfoMock = mockStatic(ChannelInfo.class);
                    MockedStatic<ChannelTabInfo> tabInfoMock = mockStatic(ChannelTabInfo.class)) {

                wireStatics(newPipeMock, channelInfoMock, CHANNEL_URL, mocks);
                tabInfoMock
                        .when(() -> ChannelTabInfo.getInfo(mocks.extractor()))
                        .thenReturn(tabInfo);

                ChannelTabDTO result = channelTabService.getChannelTab(CHANNEL_URL, "videos", CHANNEL_ID);

                assertEquals(1, result.getItems().size());
            }
        }

        @Test
        @DisplayName("Translates NewPipe exceptions, preserving the thrown exception as cause")
        void translatesNewPipeExceptions() {
            org.schabi.newpipe.extractor.exceptions.ExtractionException thrown =
                    new org.schabi.newpipe.extractor.exceptions.ExtractionException("No service can handle the url");

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class)) {
                newPipeMock.when(() -> NewPipe.getServiceByUrl(CHANNEL_URL)).thenThrow(thrown);

                ExtractionException ex = assertThrows(
                        ExtractionException.class,
                        () -> channelTabService.getChannelTab(CHANNEL_URL, "videos", CHANNEL_ID));

                assertEquals(ErrorCode.EXTRACTION_ERROR, ex.getCode());
                assertFalse(ex.getMessage().contains("No service"));
                assertSame(thrown, ex.getCause());
            }
        }
    }

    // ── getChannelTabPage ────────────────────────────────────────────────

    @Nested
    @DisplayName("getChannelTabPage()")
    class GetChannelTabPageTests {

        private static final List<String> PAGE_IDS =
                List.of("Channel Name", "https://www.youtube.com/channel/UCtest", "false");
        private static final String CHANNEL_URL = PAGE_IDS.get(1);
        private static final String PAGE_URL = "https://youtube.com/browse";

        @Test
        @DisplayName("Reconstructs the Page with url, ids, and decoded body — the three NPE-critical fields")
        void reconstructsFullPageAndReturnsDto() throws Exception {
            byte[] rawBody = "continuation_token".getBytes();
            String pageBody = Base64.getEncoder().encodeToString(rawBody);

            TabMocks mocks = stubChannelWithTab(CHANNEL_URL, "videos");
            InfoItemsPage<InfoItem> emptyPage = mockEmptyPage();
            when(mocks.extractor().getPage(any(Page.class))).thenReturn(emptyPage);

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class);
                    MockedStatic<ChannelInfo> channelInfoMock = mockStatic(ChannelInfo.class)) {

                wireStatics(newPipeMock, channelInfoMock, CHANNEL_URL, mocks);

                ChannelTabDTO result =
                        channelTabService.getChannelTabPage("UCtest", "videos", PAGE_URL, pageBody, PAGE_IDS);

                // fetchPage() before getPage(), and the Page carries all
                // three fields the service javadoc documents as required.
                ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
                InOrder callOrder = inOrder(mocks.extractor());
                callOrder.verify(mocks.extractor()).fetchPage();
                callOrder.verify(mocks.extractor()).getPage(pageCaptor.capture());

                Page sent = pageCaptor.getValue();
                assertEquals(PAGE_URL, sent.getUrl());
                assertEquals(PAGE_IDS, sent.getIds());
                assertArrayEquals(rawBody, sent.getBody());

                assertEquals("videos", result.getTab());
                assertEquals("UCtest", result.getChannelId());
            }
        }

        @Test
        @DisplayName("Handles a null pageBody, sending a Page with a null body")
        void handlesNullPageBody() throws Exception {
            TabMocks mocks = stubChannelWithTab(CHANNEL_URL, "videos");
            InfoItemsPage<InfoItem> emptyPage = mockEmptyPage();
            when(mocks.extractor().getPage(any(Page.class))).thenReturn(emptyPage);

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class);
                    MockedStatic<ChannelInfo> channelInfoMock = mockStatic(ChannelInfo.class)) {

                wireStatics(newPipeMock, channelInfoMock, CHANNEL_URL, mocks);

                ChannelTabDTO result =
                        channelTabService.getChannelTabPage("UCtest", "videos", PAGE_URL, null, PAGE_IDS);

                assertNotNull(result);

                ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
                verify(mocks.extractor()).getPage(pageCaptor.capture());
                assertNull(pageCaptor.getValue().getBody());
            }
        }

        @Test
        @DisplayName("Fails instead of returning an empty page when NewPipe recorded an error")
        void failsWhenPageIsEmptyBecauseOfAnError() throws Exception {
            ParsingException swallowed = new ParsingException("Could not parse continuation");
            TabMocks mocks = stubChannelWithTab(CHANNEL_URL, "playlists");
            InfoItemsPage<InfoItem> emptyPage = mockEmptyPage();
            when(emptyPage.getErrors()).thenReturn(List.of(swallowed));
            when(mocks.extractor().getPage(any(Page.class))).thenReturn(emptyPage);

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class);
                    MockedStatic<ChannelInfo> channelInfoMock = mockStatic(ChannelInfo.class)) {

                wireStatics(newPipeMock, channelInfoMock, CHANNEL_URL, mocks);

                ExtractionException ex = assertThrows(
                        ExtractionException.class,
                        () -> channelTabService.getChannelTabPage("UCtest", "playlists", PAGE_URL, null, PAGE_IDS));

                assertEquals(ErrorCode.UPSTREAM_CHANGED, ex.getCode());
                assertSame(swallowed, ex.getCause());
            }
        }

        @Test
        @DisplayName("Returns the page's items when errors were recorded alongside them")
        void returnsPageItemsDespitePartialErrors() throws Exception {
            TabMocks mocks = stubChannelWithTab(CHANNEL_URL, "videos");
            @SuppressWarnings("unchecked")
            InfoItemsPage<InfoItem> page = mock(InfoItemsPage.class);
            when(page.getItems()).thenReturn(List.of(mock(StreamInfoItem.class)));
            when(page.getErrors()).thenReturn(List.of(new ParsingException("Could not get view count")));
            when(mocks.extractor().getPage(any(Page.class))).thenReturn(page);

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class);
                    MockedStatic<ChannelInfo> channelInfoMock = mockStatic(ChannelInfo.class)) {

                wireStatics(newPipeMock, channelInfoMock, CHANNEL_URL, mocks);

                ChannelTabDTO result =
                        channelTabService.getChannelTabPage("UCtest", "videos", PAGE_URL, null, PAGE_IDS);

                assertEquals(1, result.getItems().size());
            }
        }

        static Stream<Arguments> invalidPageIds() {
            return Stream.of(
                    Arguments.of("null pageIds", null),
                    Arguments.of("pageIds without channelUrl at index 1", List.of("Channel Name")));
        }

        @ParameterizedTest(name = "Rejects {0}")
        @MethodSource("invalidPageIds")
        @DisplayName("Rejects pageIds that cannot supply the channelUrl with 400 VALIDATION_ERROR")
        void rejectsInvalidPageIds(String name, List<String> pageIds) {
            ValidationException ex = assertThrows(
                    ValidationException.class,
                    () -> channelTabService.getChannelTabPage("UCtest", "videos", PAGE_URL, "bodyBase64", pageIds));

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getCode());
            assertTrue(ex.getMessage().contains("pageIds"));
            assertTrue(ex.getMessage().contains("channel URL at index 1"));
        }

        @Test
        @DisplayName("A tab the channel doesn't have is 400 VALIDATION_ERROR")
        void throwsWhenTabNotFound() throws Exception {
            TabMocks mocks = stubChannelWithTab(CHANNEL_URL, "videos");

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class);
                    MockedStatic<ChannelInfo> channelInfoMock = mockStatic(ChannelInfo.class)) {

                wireStatics(newPipeMock, channelInfoMock, CHANNEL_URL, mocks);

                ValidationException ex = assertThrows(
                        ValidationException.class,
                        () -> channelTabService.getChannelTabPage("UCtest", "playlists", PAGE_URL, "body", PAGE_IDS));

                assertEquals(ErrorCode.VALIDATION_ERROR, ex.getCode());
                assertTrue(ex.getMessage().contains("no 'playlists' tab"));
            }
        }

        @Test
        @DisplayName("Translates NewPipe exceptions, preserving the thrown exception as cause")
        void translatesNewPipeExceptions() {
            org.schabi.newpipe.extractor.exceptions.ExtractionException thrown =
                    new org.schabi.newpipe.extractor.exceptions.ExtractionException("No service can handle the url");

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class)) {
                newPipeMock.when(() -> NewPipe.getServiceByUrl(CHANNEL_URL)).thenThrow(thrown);

                ExtractionException ex = assertThrows(
                        ExtractionException.class,
                        () -> channelTabService.getChannelTabPage("UCtest", "videos", PAGE_URL, "body", PAGE_IDS));

                assertEquals(ErrorCode.EXTRACTION_ERROR, ex.getCode());
                assertSame(thrown, ex.getCause());
            }
        }
    }
}
