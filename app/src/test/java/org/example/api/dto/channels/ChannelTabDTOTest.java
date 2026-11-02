package org.example.api.dto.channels;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo;
import org.schabi.newpipe.extractor.playlist.PlaylistInfo;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ChannelTabDTO
 */
@DisplayName("ChannelTabDTO Tests")
class ChannelTabDTOTest {

    private static final List<String> PAGE_IDS = List.of(
            "Linus Tech Tips",
            "https://www.youtube.com/channel/UCXuq",
            "VERIFIED");
    private static final String PAGE_URL = "https://www.youtube.com/youtubei/v1/browse?prettyPrint=false";

    // ── Helpers ──────────────────────────────────────────────────────────

    private StreamInfoItem streamItem(String url, String name) {
        StreamInfoItem item = mock(StreamInfoItem.class);
        when(item.getUrl()).thenReturn(url);
        when(item.getName()).thenReturn(name);
        when(item.getUploaderName()).thenReturn("Linus Tech Tips");
        when(item.getUploaderUrl()).thenReturn("https://www.youtube.com/@LinusTechTips");
        when(item.isUploaderVerified()).thenReturn(true);
        when(item.getDuration()).thenReturn(754L);
        when(item.getViewCount()).thenReturn(1_200_000L);
        when(item.getTextualUploadDate()).thenReturn("2 days ago");
        when(item.isShortFormContent()).thenReturn(false);
        when(item.getThumbnails()).thenReturn(List.of(
                new Image("https://img/thumb-low.jpg", 94, 168, Image.ResolutionLevel.LOW),
                new Image("https://img/thumb-high.jpg", 188, 336, Image.ResolutionLevel.MEDIUM)));
        return item;
    }

    private PlaylistInfoItem playlistItem(String url, String name) {
        PlaylistInfoItem item = mock(PlaylistInfoItem.class);
        when(item.getUrl()).thenReturn(url);
        when(item.getName()).thenReturn(name);
        when(item.getUploaderName()).thenReturn("Linus Tech Tips");
        when(item.getUploaderUrl()).thenReturn("https://www.youtube.com/@LinusTechTips");
        when(item.isUploaderVerified()).thenReturn(true);
        when(item.getStreamCount()).thenReturn(42L);
        when(item.getPlaylistType()).thenReturn(PlaylistInfo.PlaylistType.NORMAL);
        when(item.getThumbnails()).thenReturn(List.of(
                new Image("https://img/playlist.jpg", 94, 168, Image.ResolutionLevel.LOW)));
        return item;
    }

    private ChannelInfoItem channelItem(String url, String name) {
        ChannelInfoItem item = mock(ChannelInfoItem.class);
        when(item.getUrl()).thenReturn(url);
        when(item.getName()).thenReturn(name);
        when(item.getDescription()).thenReturn("Unboxings and first looks");
        when(item.getSubscriberCount()).thenReturn(2_000_000L);
        when(item.getStreamCount()).thenReturn(900L);
        when(item.isVerified()).thenReturn(true);
        when(item.getThumbnails()).thenReturn(List.of(
                new Image("https://img/avatar.jpg", 88, 88, Image.ResolutionLevel.LOW)));
        return item;
    }

    // ── VideoItemDto.from ────────────────────────────────────────────────

    @Test
    @DisplayName("Maps every StreamInfoItem field, preserving thumbnail order")
    void videoItemDto_mapsAllFields() {
        ChannelTabDTO.VideoItemDto dto =
                ChannelTabDTO.VideoItemDto.from(streamItem("https://youtube.com/watch?v=abc", "Video A"));

        // Whole-record equality: any field drift fails loudly.
        assertThat(dto).isEqualTo(new ChannelTabDTO.VideoItemDto(
                "STREAM",
                "https://youtube.com/watch?v=abc",
                "Video A",
                "Linus Tech Tips",
                "https://www.youtube.com/@LinusTechTips",
                true,
                754L,
                1_200_000L,
                "2 days ago",
                false,
                List.of(
                        new ChannelTabDTO.ThumbnailDto("https://img/thumb-low.jpg", 94, 168),
                        new ChannelTabDTO.ThumbnailDto("https://img/thumb-high.jpg", 188, 336))));
    }

    // ── PlaylistItemDto.from / ChannelItemDto.from ───────────────────────

    @Test
    @DisplayName("Maps every PlaylistInfoItem field")
    void playlistItemDto_mapsAllFields() {
        ChannelTabDTO.PlaylistItemDto dto = ChannelTabDTO.PlaylistItemDto.from(
                playlistItem("https://youtube.com/playlist?list=PL1", "Builds"));

        assertThat(dto).isEqualTo(new ChannelTabDTO.PlaylistItemDto(
                "PLAYLIST",
                "https://youtube.com/playlist?list=PL1",
                "Builds",
                "Linus Tech Tips",
                "https://www.youtube.com/@LinusTechTips",
                true,
                42L,
                "NORMAL",
                List.of(new ChannelTabDTO.ThumbnailDto("https://img/playlist.jpg", 94, 168))));
    }

    @Test
    @DisplayName("Maps a playlist with no type to a null playlistType")
    void playlistItemDto_mapsMissingPlaylistTypeToNull() {
        PlaylistInfoItem item = playlistItem("https://youtube.com/playlist?list=PL1", "Builds");
        when(item.getPlaylistType()).thenReturn(null);

        assertThat(ChannelTabDTO.PlaylistItemDto.from(item).playlistType()).isNull();
    }

    @Test
    @DisplayName("Maps every ChannelInfoItem field")
    void channelItemDto_mapsAllFields() {
        ChannelTabDTO.ChannelItemDto dto = ChannelTabDTO.ChannelItemDto.from(
                channelItem("https://www.youtube.com/@ShortCircuit", "ShortCircuit"));

        assertThat(dto).isEqualTo(new ChannelTabDTO.ChannelItemDto(
                "CHANNEL",
                "https://www.youtube.com/@ShortCircuit",
                "ShortCircuit",
                "Unboxings and first looks",
                2_000_000L,
                900L,
                true,
                List.of(new ChannelTabDTO.ThumbnailDto("https://img/avatar.jpg", 88, 88))));
    }

    // ── from(ChannelTabInfo) ─────────────────────────────────────────────

    @Test
    @DisplayName("Maps stream, playlist and channel items in order, skips unknown types, and carries tab, channelId, and cursor")
    void from_mapsItemsAndCursor() {
        StreamInfoItem stream = streamItem("https://youtube.com/watch?v=1", "Stream");
        PlaylistInfoItem playlist = playlistItem("https://youtube.com/playlist?list=PL2", "Playlist");
        ChannelInfoItem channel = channelItem("https://www.youtube.com/@ShortCircuit", "Channel");
        InfoItem unknown = mock(InfoItem.class); // not a stream, playlist or channel

        byte[] body = "{\"continuation\":\"token\"}".getBytes();
        ChannelTabInfo tabInfo = mock(ChannelTabInfo.class);
        Mockito.<List<? extends InfoItem>>when(tabInfo.getRelatedItems())
                .thenReturn(List.of(stream, unknown, playlist, channel));
        when(tabInfo.getNextPage()).thenReturn(new Page(PAGE_URL, null, PAGE_IDS, null, body));

        ChannelTabDTO dto = ChannelTabDTO.from(tabInfo, "videos", "UCXuq");

        assertThat(dto.getTab()).isEqualTo("videos");
        assertThat(dto.getChannelId()).isEqualTo("UCXuq");
        assertThat(dto.getItems())
                .extracting(ChannelTabDTO.ItemDto::infoType)
                .containsExactly("STREAM", "PLAYLIST", "CHANNEL"); // unknown type dropped
        assertThat(dto.getItems())
                .hasExactlyElementsOfTypes(
                        ChannelTabDTO.VideoItemDto.class,
                        ChannelTabDTO.PlaylistItemDto.class,
                        ChannelTabDTO.ChannelItemDto.class);
        assertThat(dto.getNextPage()).isEqualTo(new ChannelTabDTO.PageDto(
                PAGE_URL,
                Base64.getEncoder().encodeToString(body),
                PAGE_IDS));
    }

    @Test
    @DisplayName("Maps an empty final tab to empty items and a null cursor")
    void from_mapsEmptyFinalTab() {
        ChannelTabInfo tabInfo = mock(ChannelTabInfo.class);
        Mockito.<List<? extends InfoItem>>when(tabInfo.getRelatedItems()).thenReturn(List.of());
        when(tabInfo.getNextPage()).thenReturn(null);

        ChannelTabDTO dto = ChannelTabDTO.from(tabInfo, "videos", "UCXuq");

        assertThat(dto.getItems()).isEmpty();
        assertThat(dto.getNextPage()).isNull();
    }

    // ── fromPage(InfoItemsPage) ──────────────────────────────────────────

    @Test
    @DisplayName("Builds the DTO from an InfoItemsPage through the same mapping and cursor logic")
    void fromPage_buildsDtoFromPage() {
        StreamInfoItem item = streamItem("https://youtube.com/watch?v=3", "Page item");

        @SuppressWarnings("unchecked")
        InfoItemsPage<InfoItem> page = mock(InfoItemsPage.class);
        when(page.getItems()).thenReturn(List.of(item));
        when(page.getNextPage()).thenReturn(new Page(PAGE_URL, null, PAGE_IDS, null, null));

        ChannelTabDTO dto = ChannelTabDTO.fromPage(page, "shorts", "UCXuq");

        assertThat(dto.getTab()).isEqualTo("shorts");
        assertThat(dto.getChannelId()).isEqualTo("UCXuq");
        assertThat(dto.getItems())
                .singleElement()
                .isInstanceOfSatisfying(ChannelTabDTO.VideoItemDto.class,
                        video -> assertThat(video.name()).isEqualTo("Page item"));
        assertThat(dto.getNextPage()).isEqualTo(
                new ChannelTabDTO.PageDto(PAGE_URL, null, PAGE_IDS));
    }

    // ── Cursor construction (buildNextPage branches) ─────────────────────

    static Stream<Arguments> cursorCases() {
        byte[] body = "continuation_token".getBytes();
        return Stream.of(
                Arguments.of("null Page",
                        null,
                        null),
                Arguments.of("Page with null url",
                        new Page(null, null, PAGE_IDS, null, body),
                        null),
                Arguments.of("Page with null body (URL-only continuation)",
                        new Page(PAGE_URL, null, PAGE_IDS, null, null),
                        new ChannelTabDTO.PageDto(PAGE_URL, null, PAGE_IDS)),
                Arguments.of("Page with body -> Base64-encoded",
                        new Page(PAGE_URL, null, PAGE_IDS, null, body),
                        new ChannelTabDTO.PageDto(
                                PAGE_URL,
                                Base64.getEncoder().encodeToString(body),
                                PAGE_IDS)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cursorCases")
    @DisplayName("Builds the pagination cursor per the url/body/ids contract")
    void buildsCursorPerContract(String name, Page nextPage, ChannelTabDTO.PageDto expected) {
        ChannelTabInfo tabInfo = mock(ChannelTabInfo.class);
        Mockito.<List<? extends InfoItem>>when(tabInfo.getRelatedItems()).thenReturn(List.of());
        when(tabInfo.getNextPage()).thenReturn(nextPage);

        ChannelTabDTO dto = ChannelTabDTO.from(tabInfo, "videos", "UCXuq");

        assertThat(dto.getNextPage()).isEqualTo(expected);
    }
}