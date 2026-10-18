package org.example.api.dto.kiosk;

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
public record KioskDTO(
        int serviceId,
        String id,
        String name,
        String url,
        List<KioskStreamItemDTO> items,
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

    public record KioskStreamItemDTO(
            String name,
            String url,
            String uploaderName,
            String uploaderUrl,
            boolean uploaderVerified,
            List<ImageDTO> uploaderAvatars,
            long duration,
            long viewCount,
            String uploadDate,
            String textualUploadDate,
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

    public record ImageDTO(String url, int width, int height, String estimatedResolutionLevel) {
        public static ImageDTO from(Image image) {
            return new ImageDTO(image.getUrl(), image.getWidth(), image.getHeight(),
                    image.getEstimatedResolutionLevel().name());
        }

        public static List<ImageDTO> fromAll(List<Image> images) {
            return images == null ? List.of() : images.stream().map(ImageDTO::from).toList();
        }
    }

    /**
     * Serializable form of NewPipe's {@link Page}. The body is a raw byte[]
     * continuation token, so it is Base64-encoded here and must be sent back
     * verbatim via the {@code /page} endpoint (same convention as channel tabs).
     */
    public record NextPageDTO(String url, String id, List<String> ids, String body) {
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