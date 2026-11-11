package org.example.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;
import org.example.api.config.GlobalExceptionHandler;
import org.example.api.dto.channels.ChannelTabDTO;
import org.example.api.exception.ExtractionException;
import org.example.api.service.ChannelService;
import org.example.api.service.ChannelTabService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for ChannelController.
 */
@DisplayName("ChannelController Tests")
class ChannelControllerTest {

    private static final String CHANNEL_ID = "@LinusTechTips";
    private static final String CHANNEL_URL = "https://www.youtube.com/" + CHANNEL_ID;

    private MockMvc mockMvc;

    @Mock
    private ChannelService channelService;

    @Mock
    private ChannelTabService channelTabService;

    @InjectMocks
    private ChannelController channelController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(channelController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private ChannelTabDTO channelTabDTO(String tab, String channelId, boolean withNextPage) {
        ChannelTabDTO dto = new ChannelTabDTO();
        dto.setTab(tab);
        dto.setChannelId(channelId);
        dto.setItems(List.of());
        if (withNextPage) {
            dto.setNextPage(new ChannelTabDTO.PageDto(
                    "https://www.youtube.com/youtubei/v1/browse?prettyPrint=false",
                    "eyJjb250aW51YXRpb24iOiJ0b2tlbiJ9",
                    List.of(
                            "Linus Tech Tips",
                            "https://www.youtube.com/channel/UCXuqSBlHAE6Xw-yeJA0Tunw",
                            "VERIFIED")));
        }
        return dto;
    }

    // ── GET /api/v1/channels ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/channels")
    class ChannelInfoTests {

        @Test
        @DisplayName("Returns 200 with the mapped channel, requesting the URL built from the id")
        void returnsChannelInfo_forConstructedUrl() throws Exception {
            ChannelInfo mockInfo = mock(ChannelInfo.class);
            when(mockInfo.getId()).thenReturn("UCXuqSBlHAE6Xw-yeJA0Tunw");
            when(mockInfo.getName()).thenReturn("Linus Tech Tips");
            when(mockInfo.getSubscriberCount()).thenReturn(15_000_000L);
            when(mockInfo.isVerified()).thenReturn(true);
            when(mockInfo.getAvatars()).thenReturn(List.of());
            when(mockInfo.getBanners()).thenReturn(List.of());
            when(mockInfo.getParentChannelAvatars()).thenReturn(List.of());
            when(mockInfo.getTabs()).thenReturn(List.of());
            when(mockInfo.getTags()).thenReturn(List.of());
            when(channelService.getChannelInfo(CHANNEL_URL)).thenReturn(mockInfo);

            mockMvc.perform(get("/api/v1/channels").param("id", CHANNEL_ID).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value("UCXuqSBlHAE6Xw-yeJA0Tunw"))
                    .andExpect(jsonPath("$.name").value("Linus Tech Tips"))
                    .andExpect(jsonPath("$.subscriberCount").value(15_000_000))
                    .andExpect(jsonPath("$.verified").value(true));

            verify(channelService).getChannelInfo(CHANNEL_URL);
        }

        @Test
        @DisplayName(
                "Propagates service failures to the exception handler (single advice-wiring test for this controller)")
        void propagatesFailuresToAdvice() throws Exception {
            when(channelService.getChannelInfo(anyString()))
                    .thenThrow(new ExtractionException("Failed to extract channel info"));

            mockMvc.perform(get("/api/v1/channels").param("id", CHANNEL_ID).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value("EXTRACTION_ERROR"))
                    .andExpect(jsonPath("$.message").value(containsString("Failed to extract")));
        }
    }

    // ── GET /api/v1/channels/tab ─────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/channels/tab")
    class ChannelTabTests {

        @Test
        @DisplayName("Returns 200 with tab results, defaulting tab to 'videos' and passing the constructed URL")
        void returnsTabResults_withDefaultedTabAndConstructedUrl() throws Exception {
            when(channelTabService.getChannelTab(anyString(), anyString(), anyString()))
                    .thenReturn(channelTabDTO("videos", CHANNEL_ID, true));

            mockMvc.perform(get("/api/v1/channels/tab").param("id", CHANNEL_ID).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.tab").value("videos"))
                    .andExpect(jsonPath("$.channelId").value(CHANNEL_ID))
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.nextPage.url").isNotEmpty())
                    .andExpect(jsonPath("$.nextPage.body").isNotEmpty())
                    .andExpect(jsonPath("$.nextPage.ids", hasSize(3)));

            verify(channelTabService).getChannelTab(CHANNEL_URL, "videos", CHANNEL_ID);
        }
    }

    // ── GET /api/v1/channels/tab/page ────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/channels/tab/page")
    class ChannelTabPageTests {

        @Test
        @DisplayName(
                "Returns 200, defaulting tab and passing all page parameters (including the pageIds list) verbatim")
        void returnsPage_passingAllParametersVerbatim() throws Exception {
            when(channelTabService.getChannelTabPage(anyString(), anyString(), anyString(), anyString(), anyList()))
                    .thenReturn(channelTabDTO("videos", "UCXuq", true));

            mockMvc.perform(get("/api/v1/channels/tab/page")
                            .param("channelId", "UCXuq")
                            .param("pageUrl", "https://youtube.com/browse")
                            .param("pageBody", "bodyBase64")
                            .param("pageIds", "Channel Name")
                            .param("pageIds", "https://youtube.com/channel/UC1")
                            .param("pageIds", "VERIFIED")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tab").value("videos"))
                    .andExpect(jsonPath("$.items").isArray());

            verify(channelTabService)
                    .getChannelTabPage(
                            eq("UCXuq"),
                            eq("videos"), // defaultValue binding
                            eq("https://youtube.com/browse"),
                            eq("bodyBase64"),
                            eq(List.of("Channel Name", "https://youtube.com/channel/UC1", "VERIFIED")));
        }

        @Test
        @DisplayName("Omits nextPage from the response on the final page")
        void omitsNextPageOnFinalPage() throws Exception {
            when(channelTabService.getChannelTabPage(anyString(), anyString(), anyString(), anyString(), anyList()))
                    .thenReturn(channelTabDTO("videos", "UCXuq", false));

            mockMvc.perform(get("/api/v1/channels/tab/page")
                            .param("channelId", "UCXuq")
                            .param("pageUrl", "https://youtube.com/browse")
                            .param("pageBody", "body")
                            .param("pageIds", "Ch")
                            .param("pageIds", "https://youtube.com/channel/UC1")
                            .param("pageIds", "false")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nextPage").doesNotExist());
        }
    }

    // ── Required parameter binding (all endpoints) ───────────────────────

    static Stream<Arguments> requestsMissingARequiredParam() {
        return Stream.of(
                Arguments.of("/channels without id", get("/api/v1/channels")),
                Arguments.of("/channels/tab without id", get("/api/v1/channels/tab")),
                Arguments.of(
                        "/channels/tab/page without channelId",
                        get("/api/v1/channels/tab/page")
                                .param("pageUrl", "https://youtube.com/browse")
                                .param("pageBody", "body")
                                .param("pageIds", "Ch")),
                Arguments.of(
                        "/channels/tab/page without pageUrl",
                        get("/api/v1/channels/tab/page")
                                .param("channelId", "UCXuq")
                                .param("pageBody", "body")
                                .param("pageIds", "Ch")),
                Arguments.of(
                        "/channels/tab/page without pageBody",
                        get("/api/v1/channels/tab/page")
                                .param("channelId", "UCXuq")
                                .param("pageUrl", "https://youtube.com/browse")
                                .param("pageIds", "Ch")),
                Arguments.of(
                        "/channels/tab/page without pageIds",
                        get("/api/v1/channels/tab/page")
                                .param("channelId", "UCXuq")
                                .param("pageUrl", "https://youtube.com/browse")
                                .param("pageBody", "body")));
    }

    @ParameterizedTest(name = "{0} returns 400")
    @MethodSource("requestsMissingARequiredParam")
    @DisplayName("Missing required parameters return 400")
    void missingRequiredParam_returnsBadRequest(String name, MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }
}
