package org.example.api.dto.channels;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * Data Transfer Object representing paginated content from a channel tab
 * (Videos, Shorts, Livestreams, Playlists or Albums).
 *
 * <p>Mirrors the structure of {@link ChannelTabInfo} and its paged items,
 * keeping raw NewPipe types out of the API surface.</p>
 */
@Schema(name = "ChannelTabPage", description = "One page of a channel tab")
public class ChannelTabDTO {

    // ── Nested Records ──────────────────────────────────────────────────────────

    /** Thumbnail image for a tab item. */
    @Schema(name = "ChannelTabThumbnail")
    public record ThumbnailDto(String url, int height, int width) {
        static List<ThumbnailDto> fromAll(List<Image> images) {
            return images == null ? List.of() : images.stream()
                    .map(img -> new ThumbnailDto(img.getUrl(), img.getHeight(), img.getWidth()))
                    .toList();
        }
    }

    /**
     * One item in a channel tab. {@code infoType} says which record it is, using the same
     * values as NewPipe's {@code InfoItem.InfoType} and the related-items response.
     */
    public sealed interface ItemDto permits VideoItemDto, PlaylistItemDto, ChannelItemDto {
        String infoType();
    }

    /**
     * A video, Short or livestream. Maps to {@link StreamInfoItem}.
     */
    @Schema(name = "ChannelTabItem")
    public record VideoItemDto(
            @Schema(allowableValues = "STREAM") String infoType,
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
    ) implements ItemDto {
        public static final String INFO_TYPE = "STREAM";

        public static VideoItemDto from(StreamInfoItem item) {
            return new VideoItemDto(
                    INFO_TYPE,
                    item.getUrl(),
                    item.getName(),
                    item.getUploaderName(),
                    item.getUploaderUrl(),
                    item.isUploaderVerified(),
                    item.getDuration(),
                    item.getViewCount(),
                    item.getTextualUploadDate(),
                    item.isShortFormContent(),
                    ThumbnailDto.fromAll(item.getThumbnails())
            );
        }
    }

    /**
     * A playlist or album, from the Playlists or Albums tab. Maps to {@link PlaylistInfoItem}.
     */
    @Schema(name = "ChannelTabPlaylist")
    public record PlaylistItemDto(
            @Schema(allowableValues = "PLAYLIST") String infoType,
            String url,
            String name,
            String uploaderName,
            String uploaderUrl,
            boolean uploaderVerified,
            @Schema(description = "Number of videos, or -1 when unknown") long streamCount,
            @Schema(description = "A normal playlist or one of YouTube's auto-generated mixes",
                    allowableValues = {"NORMAL", "MIX_STREAM", "MIX_MUSIC", "MIX_CHANNEL", "MIX_GENRE"})
            String playlistType,
            List<ThumbnailDto> thumbnails
    ) implements ItemDto {
        public static final String INFO_TYPE = "PLAYLIST";

        public static PlaylistItemDto from(PlaylistInfoItem item) {
            return new PlaylistItemDto(
                    INFO_TYPE,
                    item.getUrl(),
                    item.getName(),
                    item.getUploaderName(),
                    item.getUploaderUrl(),
                    item.isUploaderVerified(),
                    item.getStreamCount(),
                    item.getPlaylistType() != null ? item.getPlaylistType().name() : null,
                    ThumbnailDto.fromAll(item.getThumbnails())
            );
        }
    }

    /**
     * A channel, e.g. a featured channel. Maps to {@link ChannelInfoItem}.
     */
    @Schema(name = "ChannelTabChannel")
    public record ChannelItemDto(
            @Schema(allowableValues = "CHANNEL") String infoType,
            String url,
            String name,
            @Schema(description = "Description snippet") String description,
            @Schema(description = "Subscribers, or -1 when hidden or unknown") long subscriberCount,
            @Schema(description = "Number of videos, or -1 when unknown") long streamCount,
            boolean verified,
            List<ThumbnailDto> thumbnails
    ) implements ItemDto {
        public static final String INFO_TYPE = "CHANNEL";

        public static ChannelItemDto from(ChannelInfoItem item) {
            return new ChannelItemDto(
                    INFO_TYPE,
                    item.getUrl(),
                    item.getName(),
                    item.getDescription(),
                    item.getSubscriberCount(),
                    item.getStreamCount(),
                    item.isVerified(),
                    ThumbnailDto.fromAll(item.getThumbnails())
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

    /** Items on the current page: videos, playlists or channels, told apart by {@code infoType}. */
    @ArraySchema(
            arraySchema = @Schema(description = "Items on this page. Each item's `infoType` says "
                    + "which shape it has: `videos`, `shorts` and `livestreams` hold `STREAM` "
                    + "items; `playlists` and `albums` hold `PLAYLIST` items."),
            schema = @Schema(
                    oneOf = {VideoItemDto.class, PlaylistItemDto.class, ChannelItemDto.class},
                    discriminatorProperty = "infoType",
                    discriminatorMapping = {
                            @DiscriminatorMapping(value = VideoItemDto.INFO_TYPE,
                                    schema = VideoItemDto.class),
                            @DiscriminatorMapping(value = PlaylistItemDto.INFO_TYPE,
                                    schema = PlaylistItemDto.class),
                            @DiscriminatorMapping(value = ChannelItemDto.INFO_TYPE,
                                    schema = ChannelItemDto.class)
                    }))
    private List<ItemDto> items;

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

    /** Maps every item type a channel tab can hold, in order; any other type is skipped. */
    private static List<ItemDto> mapItems(List<? extends InfoItem> rawItems) {
        return rawItems.stream()
                .map(ChannelTabDTO::toItemDto)
                .filter(Objects::nonNull)
                .toList();
    }

    private static ItemDto toItemDto(InfoItem item) {
        return switch (item) {
            case StreamInfoItem stream -> VideoItemDto.from(stream);
            case PlaylistInfoItem playlist -> PlaylistItemDto.from(playlist);
            case ChannelInfoItem channel -> ChannelItemDto.from(channel);
            case null, default -> null;
        };
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

    public List<ItemDto> getItems() { return items; }
    public void setItems(List<ItemDto> items) { this.items = items; }

    public PageDto getNextPage() { return nextPage; }
    public void setNextPage(PageDto nextPage) { this.nextPage = nextPage; }
}