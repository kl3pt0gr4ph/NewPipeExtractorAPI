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
import org.example.api.service.PlaylistService;
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
import org.schabi.newpipe.extractor.playlist.PlaylistInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for PlaylistController (previously untested).
 */
@DisplayName("PlaylistController Tests")
class PlaylistControllerTest {

    private static final String TEST_PLAYLIST_ID = "PLtest123abc";
    private static final String PLAYLIST_URL = "https://www.youtube.com/playlist?list=" + TEST_PLAYLIST_ID;
    private static final String PAGE_URL = "https://www.youtube.com/youtubei/v1/browse?continuation=token";

    private MockMvc mockMvc;

    @Mock
    private PlaylistService playlistService;

    @InjectMocks
    private PlaylistController playlistController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(playlistController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── GET /api/v1/playlists ────────────────────────────────────────────

    @Test
    @DisplayName("Returns 200 with the mapped playlist, requesting the URL built from the id")
    void getPlaylistInfo_returnsMappedPlaylist() throws Exception {
        PlaylistInfo mockInfo = mock(PlaylistInfo.class);
        when(mockInfo.getName()).thenReturn("Test Playlist");
        when(mockInfo.getUrl()).thenReturn(PLAYLIST_URL);
        when(playlistService.getPlaylistInfo(PLAYLIST_URL)).thenReturn(mockInfo);

        mockMvc.perform(get("/api/v1/playlists").param("id", TEST_PLAYLIST_ID).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Playlist"));

        verify(playlistService).getPlaylistInfo(PLAYLIST_URL);
    }

    // ── GET /api/v1/playlists/page ───────────────────────────────────────

    @Test
    @DisplayName("Returns 200 with the playlist page, passing url and pageUrl verbatim")
    void getPlaylistPage_passesUrlsVerbatim() throws Exception {
        @SuppressWarnings("unchecked")
        ListExtractor.InfoItemsPage<StreamInfoItem> mockPage = mock(ListExtractor.InfoItemsPage.class);
        when(mockPage.getItems()).thenReturn(List.of());
        when(playlistService.getPlaylistPage(PLAYLIST_URL, PAGE_URL)).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/playlists/page")
                        .param("url", PLAYLIST_URL)
                        .param("pageUrl", PAGE_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(playlistService).getPlaylistPage(PLAYLIST_URL, PAGE_URL);
    }

    // ── Exception propagation (single advice-wiring test) ────────────────

    @Test
    @DisplayName("Propagates service failures to the exception handler")
    void propagatesFailuresToAdvice() throws Exception {
        when(playlistService.getPlaylistInfo(anyString())).thenThrow(new ExtractionException("Playlist unavailable"));

        mockMvc.perform(get("/api/v1/playlists").param("id", TEST_PLAYLIST_ID).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("EXTRACTION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("Playlist unavailable")));
    }

    // ── Required parameter binding (both endpoints) ──────────────────────

    static Stream<Arguments> requestsMissingARequiredParam() {
        return Stream.of(
                Arguments.of("/playlists without id", get("/api/v1/playlists")),
                Arguments.of(
                        "/playlists/page without url",
                        get("/api/v1/playlists/page").param("pageUrl", PAGE_URL)),
                Arguments.of(
                        "/playlists/page without pageUrl",
                        get("/api/v1/playlists/page").param("url", PLAYLIST_URL)));
    }

    @ParameterizedTest(name = "{0} returns 400")
    @MethodSource("requestsMissingARequiredParam")
    @DisplayName("Missing required parameters return 400")
    void missingRequiredParam_returnsBadRequest(String name, MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }
}
