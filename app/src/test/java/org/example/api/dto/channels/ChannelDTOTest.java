package org.example.api.dto.channels;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;

/**
 * Unit tests for ChannelDTO
 */
@DisplayName("ChannelDTO Tests")
class ChannelDTOTest {

    // ── Helpers ──────────────────────────────────────────────────────────

    private ListLinkHandler tab(
            String url, String originalUrl, String id, List<String> contentFilters, String sortFilter) {
        ListLinkHandler handler = mock(ListLinkHandler.class);
        when(handler.getUrl()).thenReturn(url);
        when(handler.getOriginalUrl()).thenReturn(originalUrl);
        when(handler.getId()).thenReturn(id);
        when(handler.getContentFilters()).thenReturn(contentFilters);
        when(handler.getSortFilter()).thenReturn(sortFilter);
        return handler;
    }

    private ChannelInfo channelInfo() {
        ChannelInfo info = mock(ChannelInfo.class);
        when(info.getServiceId()).thenReturn(0);
        when(info.getId()).thenReturn("UCXuqSBlHAE6Xw-yeJA0Tunw");
        when(info.getUrl()).thenReturn("https://www.youtube.com/channel/UCXuqSBlHAE6Xw-yeJA0Tunw");
        when(info.getOriginalUrl()).thenReturn("https://www.youtube.com/@LinusTechTips");
        when(info.getName()).thenReturn("Linus Tech Tips");
        when(info.getParentChannelName()).thenReturn("LMG");
        when(info.getParentChannelUrl()).thenReturn("https://www.youtube.com/@LMG");
        when(info.getFeedUrl()).thenReturn("https://www.youtube.com/feeds/videos.xml?channel_id=UCXuq");
        when(info.getSubscriberCount()).thenReturn(15_000_000L);
        when(info.getDescription()).thenReturn("Tech can be fun!");
        when(info.isVerified()).thenReturn(true);
        when(info.getAvatars()).thenReturn(List.of());
        when(info.getBanners()).thenReturn(List.of());
        when(info.getParentChannelAvatars()).thenReturn(List.of());
        when(info.getTabs()).thenReturn(List.of());
        when(info.getTags()).thenReturn(List.of());
        return info;
    }

    // ── from() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Maps all scalar fields, tags, and images (order-preserving), with errors always empty")
    void mapsAllFieldsFromFullChannelInfo() {
        ChannelInfo info = channelInfo();
        when(info.getAvatars())
                .thenReturn(List.of(
                        new Image("https://img/avatar-low.jpg", 48, 48, Image.ResolutionLevel.LOW),
                        new Image("https://img/avatar-high.jpg", 176, 176, Image.ResolutionLevel.HIGH)));
        when(info.getBanners())
                .thenReturn(List.of(new Image("https://img/banner.jpg", 424, 1060, Image.ResolutionLevel.MEDIUM)));
        when(info.getParentChannelAvatars())
                .thenReturn(List.of(new Image("https://img/parent.jpg", 48, 48, Image.ResolutionLevel.LOW)));
        when(info.getTags()).thenReturn(List.of("tech", "reviews"));

        ChannelDTO dto = ChannelDTO.from(info);

        assertThat(dto.getServiceId()).isZero();
        assertThat(dto.getId()).isEqualTo("UCXuqSBlHAE6Xw-yeJA0Tunw");
        assertThat(dto.getUrl()).isEqualTo("https://www.youtube.com/channel/UCXuqSBlHAE6Xw-yeJA0Tunw");
        assertThat(dto.getOriginalUrl()).isEqualTo("https://www.youtube.com/@LinusTechTips");
        assertThat(dto.getName()).isEqualTo("Linus Tech Tips");
        assertThat(dto.getParentChannelName()).isEqualTo("LMG");
        assertThat(dto.getParentChannelUrl()).isEqualTo("https://www.youtube.com/@LMG");
        assertThat(dto.getFeedUrl()).isEqualTo("https://www.youtube.com/feeds/videos.xml?channel_id=UCXuq");
        assertThat(dto.getSubscriberCount()).isEqualTo(15_000_000L);
        assertThat(dto.getDescription()).isEqualTo("Tech can be fun!");
        assertThat(dto.isVerified()).isTrue();
        assertThat(dto.getTags()).containsExactly("tech", "reviews");
        assertThat(dto.getErrors()).isEmpty();

        assertThat(dto.getAvatars())
                .containsExactly(
                        new ChannelDTO.ImageDto("https://img/avatar-low.jpg", 48, 48, "LOW"),
                        new ChannelDTO.ImageDto("https://img/avatar-high.jpg", 176, 176, "HIGH"));
        assertThat(dto.getBanners())
                .containsExactly(new ChannelDTO.ImageDto("https://img/banner.jpg", 424, 1060, "MEDIUM"));
        assertThat(dto.getParentChannelAvatars())
                .containsExactly(new ChannelDTO.ImageDto("https://img/parent.jpg", 48, 48, "LOW"));
    }

    @Test
    @DisplayName("Maps tabs (order-preserving) and derives baseUrl as scheme + host")
    void mapsTabsAndDerivesBaseUrl() {
        ChannelInfo info = channelInfo();
        ListLinkHandler videosTab = tab(
                "https://www.youtube.com/@LinusTechTips/videos",
                "https://www.youtube.com/@LinusTechTips/videos",
                "UCXuq",
                List.of("videos"),
                "");
        ListLinkHandler shortsTab = tab(
                "https://www.youtube.com/@LinusTechTips/shorts",
                "https://www.youtube.com/@LinusTechTips/shorts",
                "UCXuq",
                List.of("shorts"),
                "");
        when(info.getTabs()).thenReturn(List.of(videosTab, shortsTab));

        ChannelDTO dto = ChannelDTO.from(info);

        assertThat(dto.getTabs())
                .containsExactly(
                        new ChannelDTO.TabDto(
                                "https://www.youtube.com/@LinusTechTips/videos",
                                "https://www.youtube.com/@LinusTechTips/videos",
                                "UCXuq",
                                List.of("videos"),
                                "",
                                "https://www.youtube.com"),
                        new ChannelDTO.TabDto(
                                "https://www.youtube.com/@LinusTechTips/shorts",
                                "https://www.youtube.com/@LinusTechTips/shorts",
                                "UCXuq",
                                List.of("shorts"),
                                "",
                                "https://www.youtube.com"));
    }

    @Test
    @DisplayName("Falls back for tab URLs that are null, blank, or unparseable")
    void derivesBaseUrlFallbacksForOddTabUrls() {
        ChannelInfo info = channelInfo();
        ListLinkHandler nullUrlTab = tab(null, null, "t1", List.of("videos"), "");
        ListLinkHandler blankUrlTab = tab("   ", null, "t2", List.of("videos"), "");
        ListLinkHandler unparseableTab = tab("not-a-url", null, "t3", List.of("videos"), "");
        ListLinkHandler brokenHostTab = tab("ht tp://broken host", null, "t4", List.of("videos"), "");
        when(info.getTabs()).thenReturn(List.of(nullUrlTab, blankUrlTab, unparseableTab, brokenHostTab));

        ChannelDTO dto = ChannelDTO.from(info);

        assertThat(dto.getTabs())
                .extracting(ChannelDTO.TabDto::baseUrl)
                .containsExactly("", "", "https://", "https://");
    }

    @Test
    @DisplayName("Maps a minimal channel: empty lists stay empty, null description and hidden count pass through")
    void mapsMinimalChannelInfo() {
        ChannelInfo info = channelInfo();
        when(info.getDescription()).thenReturn(null);
        when(info.getSubscriberCount()).thenReturn(-1L); // NewPipe: hidden

        ChannelDTO dto = ChannelDTO.from(info);

        assertThat(dto.getAvatars()).isEmpty();
        assertThat(dto.getBanners()).isEmpty();
        assertThat(dto.getParentChannelAvatars()).isEmpty();
        assertThat(dto.getTabs()).isEmpty();
        assertThat(dto.getDescription()).isNull();
        assertThat(dto.getSubscriberCount()).isEqualTo(-1L);
    }

    @Test
    @DisplayName("Rejects a null ChannelInfo with IllegalArgumentException")
    void rejectsNullChannelInfo() {
        assertThatThrownBy(() -> ChannelDTO.from(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
