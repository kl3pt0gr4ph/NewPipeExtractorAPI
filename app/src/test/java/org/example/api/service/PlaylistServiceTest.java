package org.example.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.example.api.exception.ErrorCode;
import org.example.api.exception.ExtractionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.playlist.PlaylistInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

/**
 * Unit tests for PlaylistService (previously untested).
 *
 * <p>Follows the same shape as the other extraction-service suites: one
 * happy-path test per method verifying delegation and the reconstructed
 * Page, one exception-wrap test per method (each method has its own
 * catch block).</p>
 */
@DisplayName("PlaylistService Tests")
class PlaylistServiceTest {

    private static final String PLAYLIST_URL = "https://www.youtube.com/playlist?list=PLtest123";

    private PlaylistService playlistService;

    @BeforeEach
    void setUp() {
        playlistService = new PlaylistService();
    }

    @Nested
    @DisplayName("getPlaylistInfo")
    class GetPlaylistInfoTests {

        @Test
        @DisplayName("Returns the PlaylistInfo extracted for the requested URL")
        void returnsPlaylistInfo() throws Exception {
            PlaylistInfo mockInfo = mock(PlaylistInfo.class);

            try (MockedStatic<PlaylistInfo> playlistInfoMock = mockStatic(PlaylistInfo.class)) {
                playlistInfoMock.when(() -> PlaylistInfo.getInfo(PLAYLIST_URL)).thenReturn(mockInfo);

                assertSame(mockInfo, playlistService.getPlaylistInfo(PLAYLIST_URL));
            }
        }

        @Test
        @DisplayName("Translates NewPipe failures, keeping the cause and its text out of the message")
        void translatesFailures() {
            ContentNotAvailableException thrown = new ContentNotAvailableException("The playlist does not exist");

            try (MockedStatic<PlaylistInfo> playlistInfoMock = mockStatic(PlaylistInfo.class)) {
                playlistInfoMock.when(() -> PlaylistInfo.getInfo(PLAYLIST_URL)).thenThrow(thrown);

                ExtractionException ex =
                        assertThrows(ExtractionException.class, () -> playlistService.getPlaylistInfo(PLAYLIST_URL));

                assertEquals(ErrorCode.CONTENT_UNAVAILABLE, ex.getCode());
                assertFalse(ex.getMessage().contains("does not exist"));
                assertSame(thrown, ex.getCause());
            }
        }
    }

    @Nested
    @DisplayName("getPlaylistPage")
    class GetPlaylistPageTests {

        private static final String PAGE_URL = "https://www.youtube.com/youtubei/v1/browse?continuation=token";

        @Test
        @DisplayName("Resolves the service and requests more items with a Page carrying the pageUrl")
        void requestsPageReconstructedFromPageUrl() throws Exception {
            StreamingService mockService = mock(StreamingService.class);
            @SuppressWarnings("unchecked")
            InfoItemsPage<StreamInfoItem> mockPage = mock(InfoItemsPage.class);

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class);
                    MockedStatic<PlaylistInfo> playlistInfoMock = mockStatic(PlaylistInfo.class)) {

                newPipeMock.when(() -> NewPipe.getServiceByUrl(PLAYLIST_URL)).thenReturn(mockService);
                playlistInfoMock
                        .when(() -> PlaylistInfo.getMoreItems(
                                eq(mockService), eq(PLAYLIST_URL), argThat((Page p) -> PAGE_URL.equals(p.getUrl()))))
                        .thenReturn(mockPage);

                InfoItemsPage<StreamInfoItem> result = playlistService.getPlaylistPage(PLAYLIST_URL, PAGE_URL);

                assertSame(mockPage, result);
            }
        }

        @Test
        @DisplayName("Translates NewPipe failures, keeping the cause and its text out of the message")
        void translatesFailures() {
            ParsingException thrown = new ParsingException("Page failure");

            try (MockedStatic<NewPipe> newPipeMock = mockStatic(NewPipe.class)) {
                newPipeMock.when(() -> NewPipe.getServiceByUrl(PLAYLIST_URL)).thenThrow(thrown);

                ExtractionException ex = assertThrows(
                        ExtractionException.class, () -> playlistService.getPlaylistPage(PLAYLIST_URL, PAGE_URL));

                assertEquals(ErrorCode.UPSTREAM_CHANGED, ex.getCode());
                assertFalse(ex.getMessage().contains("Page failure"));
                assertSame(thrown, ex.getCause());
            }
        }
    }
}
