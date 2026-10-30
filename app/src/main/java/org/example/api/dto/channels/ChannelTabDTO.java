package org.example.api.dto.channels;

import io.swagger.v3.oas.annotations.media.Schema;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.Base64;
import java.util.List;

/**
 * Data Transfer Object representing paginated content from a channel tab
 * (Videos, Shorts, Livestreams, or Playlists).
 *
 * <p>Mirrors the structure of {@link ChannelTabInfo} and its paged items,
 * keeping raw NewPipe types out of the API surface.</p>
 */
@Schema(name = "ChannelTabPage", description = "One page of a channel tab")
public class ChannelTabDTO {

    // ── Nested Records ──────────────────────────────────────────────────────────

    /** Thumbnail image for a video item. */
    @Schema(name = "ChannelTabThumbnail")
    public record ThumbnailDto(String url, int height, int width) {}

    /**
     * A single video/short/livestream item within the tab.
     * Maps to {@link StreamInfoItem}.
     */
    @Schema(name = "ChannelTabItem")
    public record VideoItemDto(
            String url,
            String name,
            String uploaderName,
            String uploaderUrl,
            boolean uploaderVerified,
            @Schema(description = "Length in seconds, or -1 when unknown (e.g. live)") long duration,
            @Schema(description = "Views, or -1 when unknown") long viewCount,
            @Schema(description = "Relative upload date as YouTube words it", example = "3 weeks ago")
            String textualUploadDate,
            @Schema(description = "Whether the item is a Short") boolean isShortFormContent,
            List<ThumbnailDto> thumbnails
    ) {
        public static VideoItemDto from(StreamInfoItem item) {
            List<ThumbnailDto> thumbnails = item.getThumbnails().stream()
                    .map(img -> new ThumbnailDto(img.getUrl(), img.getHeight(), img.getWidth()))
                    .toList();
            return new VideoItemDto(
                    item.getUrl(),
                    item.getName(),
                    item.getUploaderName(),
                    item.getUploaderUrl(),
                    item.isUploaderVerified(),
                    item.getDuration(),
                    item.getViewCount(),
                    item.getTextualUploadDate(),
                    item.isShortFormContent(),
                    thumbnails
            );
        }
    }

    /**
     * Pagination cursor — pass ALL three fields back to {@code GET /channels/tab/page}.
     *
     * <p>NewPipe's {@link org.schabi.newpipe.extractor.Page} carries three essential pieces
     * of state, all of which are required for pagination to work correctly:</p>
     *
     * <ul>
     *   <li><b>url</b> — InnerTube browse endpoint URL.</li>
     *   <li><b>body</b> — Base64-encoded JSON POST body containing the continuation token.
     *       Without this, the browse request has no continuation and returns page 1 again.</li>
     *   <li><b>ids</b> — {@code ["channelName", "channelUrl", "verifiedStatus"]} metadata.
     *       {@code YoutubeChannelTabExtractor.getPage()} reads these back via
     *       {@code page.getIds()} to annotate each returned item with uploader info.
     *       Without this list, {@code channelIds} is null, causing the NPE in
     *       {@code collectItemsFrom} at line 235.</li>
     * </ul>
     *
     * <p>The previous implementation only sent {@code url}, discarding {@code body} and
     * {@code ids}, which broke pagination in two independent ways simultaneously.</p>
     */
    @Schema(name = "ChannelTabNextPage",
            description = "Continuation cursor. Pass all three fields back to `/channels/tab/page` "
                    + "as `pageUrl`, `pageBody` and `pageIds`, unmodified.")
    public record PageDto(
            @Schema(description = "InnerTube browse URL") String url,
            @Schema(description = "Base64-encoded continuation POST body", format = "byte")
            String body,     // Base64-encoded byte[] — the JSON continuation token POST body
            @Schema(description = "Channel name, channel URL and verified status, in that order")
            List<String> ids // ["channelName", "channelUrl", "verifiedStatus"]
    ) {}

    // ── Main DTO ────────────────────────────────────────────────────────────────

    /** The tab type that was queried (e.g. "videos", "shorts", "livestreams"). */
    @Schema(description = "Tab that was read", example = "videos")
    private String tab;

    /** Channel ID this tab belongs to. */
    @Schema(description = "Channel `id` as requested; pass it back as `channelId` for the next page",
            example = "@LinusTechTips")
    private String channelId;

    /** Items on the current page. Only {@link StreamInfoItem}s are mapped;
     *  other item types (playlists, channels) are silently skipped for now. */
    @Schema(description = "Items on this page. Only videos, Shorts and livestreams are included; "
            + "playlist and channel items are skipped.")
    private List<VideoItemDto> items;

    /** Pagination info. Null when no further pages exist. */
    @Schema(description = "Cursor for the next page; absent on the last page")
    private PageDto nextPage;

    // ── Constructors ────────────────────────────────────────────────────────────

    public ChannelTabDTO() {}

    // ── Static Factories ────────────────────────────────────────────────────────

    public static ChannelTabDTO from(ChannelTabInfo tabInfo, String tab, String channelId) {
        ChannelTabDTO dto = new ChannelTabDTO();
        dto.tab = tab;
        dto.channelId = channelId;
        dto.items = mapItems(tabInfo.getRelatedItems());
        dto.nextPage = buildNextPage(tabInfo.getNextPage());
        return dto;
    }

    public static ChannelTabDTO fromPage(InfoItemsPage<InfoItem> page, String tab, String channelId) {
        ChannelTabDTO dto = new ChannelTabDTO();
        dto.tab = tab;
        dto.channelId = channelId;
        dto.items = mapItems(page.getItems());
        dto.nextPage = buildNextPage(page.getNextPage());
        return dto;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private static List<VideoItemDto> mapItems(List<? extends InfoItem> rawItems) {
        return rawItems.stream()
                .filter(item -> item instanceof StreamInfoItem)
                .map(item -> VideoItemDto.from((StreamInfoItem) item))
                .toList();
    }

    private static PageDto buildNextPage(org.schabi.newpipe.extractor.Page nextPage) {
        if (nextPage == null || nextPage.getUrl() == null) return null;
        String bodyBase64 = nextPage.getBody() != null
                ? Base64.getEncoder().encodeToString(nextPage.getBody())
                : null;
        return new PageDto(nextPage.getUrl(), bodyBase64, nextPage.getIds());
    }

    // ── Getters & Setters ───────────────────────────────────────────────────────

    public String getTab() { return tab; }
    public void setTab(String tab) { this.tab = tab; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public List<VideoItemDto> getItems() { return items; }
    public void setItems(List<VideoItemDto> items) { this.items = items; }

    public PageDto getNextPage() { return nextPage; }
    public void setNextPage(PageDto nextPage) { this.nextPage = nextPage; }
}