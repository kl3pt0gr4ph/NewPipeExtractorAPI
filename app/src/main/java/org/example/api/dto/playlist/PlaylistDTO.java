package org.example.api.dto.playlist;

import io.swagger.v3.oas.annotations.media.Schema;
import org.schabi.newpipe.extractor.playlist.PlaylistInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.List;
import java.util.Objects;

@Schema(name = "Playlist", description = "A playlist's metadata and its first page of videos")
public class PlaylistDTO {

    private String id;
    private String url;
    private String originalUrl;
    private String name;
    private String uploaderUrl;
    private String uploaderName;
    @Schema(description = "Sub-channel the playlist belongs to, where YouTube reports one")
    private String subChannelUrl;
    private String subChannelName;
    @Schema(description = "Absent when the playlist has no description")
    private DescriptionDto description;
    private List<ThumbnailDto> thumbnails;
    private List<ThumbnailDto> banners;
    @Schema(description = "Uploader channel avatar")
    private List<AvatarDto> uploaderAvatars;
    @Schema(description = "First page of videos. This response has no `nextPage`, so it doesn't "
            + "provide the `pageUrl` that `/playlists/page` needs.")
    private List<RelatedItemDto> relatedItems;
    @Schema(description = "Total videos in the playlist, or -1 when unknown")
    private long streamCount;

    // ── Nested Records ───────────────────────────────────────────────────────────

    @Schema(name = "PlaylistDescription")
    public record DescriptionDto(
            String content,
            @Schema(description = "1 = HTML, 2 = Markdown, 3 = plain text", allowableValues = {"1", "2", "3"})
            int type
    ) {}
    @Schema(name = "PlaylistImage")
    public record ThumbnailDto(String url, int height, int width) {}
    @Schema(name = "PlaylistAvatar")
    public record AvatarDto(String url, int height, int width) {}
    @Schema(name = "PlaylistItem")
    public record RelatedItemDto(
            @Schema(description = "Always `STREAM`; other item types are skipped", example = "STREAM")
            String infoType,
            String url,
            String name,
            String uploaderName,
            String uploaderUrl,
            @Schema(description = "Relative upload date as YouTube words it", example = "3 weeks ago")
            String textualUploadDate,
            @Schema(description = "Views, or -1 when unknown") long viewCount,
            @Schema(description = "Length in seconds, or -1 when unknown") long duration,
            boolean uploaderVerified,
            boolean shortFormContent,
            List<ThumbnailDto> thumbnails,
            @Schema(description = "Always empty; not mapped yet")
            List<AvatarDto> uploaderAvatars
    ) {}

    // ── Constructors ─────────────────────────────────────────────────────────────

    public PlaylistDTO() {}

    // ── Static Factory ───────────────────────────────────────────────────────────

    public static PlaylistDTO from(PlaylistInfo info) {
        PlaylistDTO dto = new PlaylistDTO();

        dto.setId(info.getId());
        dto.setUrl(info.getUrl());
        dto.setOriginalUrl(info.getOriginalUrl());
        dto.setName(info.getName());
        dto.setUploaderUrl(info.getUploaderUrl());
        dto.setUploaderName(info.getUploaderName());
        dto.setSubChannelUrl(info.getSubChannelUrl());
        dto.setSubChannelName(info.getSubChannelName());
        dto.setDescription(info.getDescription() != null
                ? new DescriptionDto(
                info.getDescription().getContent(),
                info.getDescription().getType())
                : null);
        dto.setThumbnails(info.getThumbnails().stream()
                .map(t -> new ThumbnailDto(t.getUrl(), t.getHeight(), t.getWidth()))
                .toList());
        dto.setBanners(info.getBanners().stream()
                .map(b -> new ThumbnailDto(b.getUrl(), b.getHeight(), b.getWidth()))
                .toList());
        dto.setUploaderAvatars(info.getUploaderAvatars().stream()
                .map(a -> new AvatarDto(a.getUrl(), a.getHeight(), a.getWidth()))
                .toList());
        dto.setRelatedItems(info.getRelatedItems().stream()
                .filter(Objects::nonNull)
                .map(item -> {
                    return new RelatedItemDto(
                            "STREAM",
                            ((StreamInfoItem) item).getUrl(),
                            ((StreamInfoItem) item).getName(),
                            ((StreamInfoItem) item).getUploaderName(),
                            ((StreamInfoItem) item).getUploaderUrl(),
                            ((StreamInfoItem) item).getTextualUploadDate(),
                            ((StreamInfoItem) item).getViewCount(),
                            ((StreamInfoItem) item).getDuration(),
                            ((StreamInfoItem) item).isUploaderVerified(),
                            ((StreamInfoItem) item).isShortFormContent(),
                            ((StreamInfoItem) item).getThumbnails().stream()
                                    .map(t -> new ThumbnailDto(t.getUrl(), t.getHeight(), t.getWidth()))
                                    .toList(),
                            List.of()
                    );
                })
                .toList());
        dto.setStreamCount(info.getStreamCount());

        return dto;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUploaderUrl() { return uploaderUrl; }
    public void setUploaderUrl(String uploaderUrl) { this.uploaderUrl = uploaderUrl; }

    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }

    public String getSubChannelUrl() { return subChannelUrl; }
    public void setSubChannelUrl(String subChannelUrl) { this.subChannelUrl = subChannelUrl; }

    public String getSubChannelName() { return subChannelName; }
    public void setSubChannelName(String subChannelName) { this.subChannelName = subChannelName; }

    public DescriptionDto getDescription() { return description; }
    public void setDescription(DescriptionDto description) { this.description = description; }

    public List<ThumbnailDto> getThumbnails() { return thumbnails; }
    public void setThumbnails(List<ThumbnailDto> thumbnails) { this.thumbnails = thumbnails; }

    public List<ThumbnailDto> getBanners() { return banners; }
    public void setBanners(List<ThumbnailDto> banners) { this.banners = banners; }

    public List<AvatarDto> getUploaderAvatars() { return uploaderAvatars; }
    public void setUploaderAvatars(List<AvatarDto> uploaderAvatars) { this.uploaderAvatars = uploaderAvatars; }

    public List<RelatedItemDto> getRelatedItems() { return relatedItems; }
    public void setRelatedItems(List<RelatedItemDto> relatedItems) { this.relatedItems = relatedItems; }

    public long getStreamCount() { return streamCount; }
    public void setStreamCount(long streamCount) { this.streamCount = streamCount; }

    @Override
    public String toString() {
        return "PlaylistDTO{id='%s', name='%s', streamCount=%d}".formatted(id, name, streamCount);
    }
}