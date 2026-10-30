package org.example.api.dto.kiosk;

import io.swagger.v3.oas.annotations.media.Schema;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.Base64;
import java.util.List;

/**
 * API representation of a kiosk (e.g. "trending_gaming", "live") and its
 * initial page of streams.
 */
@Schema(name = "Kiosk", description = "A curated YouTube feed and its items")
public record KioskDTO(
        @Schema(description = "NewPipe service ID; 0 is YouTube") int serviceId,
        @Schema(description = "Kiosk ID", example = "trending_music") String id,
        @Schema(description = "Display name") String name,
        String url,
        List<KioskStreamItemDTO> items,
        @Schema(description = "Always absent in practice: YouTube kiosks are a single page, "
                + "and there is no kiosk `/page` endpoint")
        NextPageDTO nextPage
) {
    public static KioskDTO from(KioskInfo info) {
        return new KioskDTO(
                info.getServiceId(),
                info.getId(),
                info.getName(),
                info.getUrl(),
                info.getRelatedItems().stream().map(KioskStreamItemDTO::from).toList(),
                NextPageDTO.from(info.getNextPage())
        );
    }

    @Schema(name = "KioskItem")
    public record KioskStreamItemDTO(
            String name,
            String url,
            String uploaderName,
            String uploaderUrl,
            boolean uploaderVerified,
            List<ImageDTO> uploaderAvatars,
            @Schema(description = "Length in seconds, or -1 when unknown (e.g. live)") long duration,
            @Schema(description = "Views, or -1 when unknown") long viewCount,
            @Schema(description = "Upload time as ISO-8601 with offset. Often approximate, since YouTube "
                    + "only gives a relative date for list items. Absent when unknown.",
                    example = "2026-10-08T12:00:00Z")
            String uploadDate,
            @Schema(description = "Relative upload date as YouTube words it", example = "3 weeks ago")
            String textualUploadDate,
            @Schema(allowableValues = {"NONE", "VIDEO_STREAM", "AUDIO_STREAM", "LIVE_STREAM",
                    "AUDIO_LIVE_STREAM", "POST_LIVE_STREAM", "POST_LIVE_AUDIO_STREAM"})
            String streamType,
            List<ImageDTO> thumbnails
    ) {
        public static KioskStreamItemDTO from(StreamInfoItem item) {
            return new KioskStreamItemDTO(
                    item.getName(),
                    item.getUrl(),
                    item.getUploaderName(),
                    item.getUploaderUrl(),
                    item.isUploaderVerified(),
                    ImageDTO.fromAll(item.getUploaderAvatars()),
                    item.getDuration(),
                    item.getViewCount(),
                    item.getUploadDate() != null
                            ? item.getUploadDate().offsetDateTime().toString()
                            : null,
                    item.getTextualUploadDate(),
                    item.getStreamType().name(),
                    ImageDTO.fromAll(item.getThumbnails())
            );
        }
    }

    @Schema(name = "KioskImage")
    public record ImageDTO(
            String url,
            @Schema(description = "Width in pixels, or -1 when unknown") int width,
            @Schema(description = "Height in pixels, or -1 when unknown") int height,
            @Schema(allowableValues = {"LOW", "MEDIUM", "HIGH", "UNKNOWN"}) String estimatedResolutionLevel
    ) {
        public static ImageDTO from(Image image) {
            return new ImageDTO(image.getUrl(), image.getWidth(), image.getHeight(),
                    image.getEstimatedResolutionLevel().name());
        }

        public static List<ImageDTO> fromAll(List<Image> images) {
            return images == null ? List.of() : images.stream().map(ImageDTO::from).toList();
        }
    }

    /**
     * Serialisable form of NewPipe's {@link Page}. The body is a raw byte[]
     * continuation token, so it is Base64-encoded here and must be sent back
     * verbatim via the {@code /page} endpoint (same convention as channel tabs).
     */
    @Schema(name = "KioskNextPage")
    public record NextPageDTO(
            String url,
            String id,
            List<String> ids,
            @Schema(description = "Base64-encoded continuation POST body", format = "byte") String body
    ) {
        public static NextPageDTO from(Page page) {
            if (page == null) {
                return null;
            }
            return new NextPageDTO(
                    page.getUrl(),
                    page.getId(),
                    page.getIds(),
                    page.getBody() != null
                            ? Base64.getEncoder().encodeToString(page.getBody())
                            : null
            );
        }
    }
}