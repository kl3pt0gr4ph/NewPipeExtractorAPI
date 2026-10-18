package org.example.api.dto.kiosk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.Image.ResolutionLevel;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for KioskDTO.
 *
 * <p>{@link Image} is a plain value object, so real instances are used rather than mocks:
 * a mocked Image returns {@code null} from {@code getEstimatedResolutionLevel()}, which a
 * real one never does.</p>
 */
@DisplayName("KioskDTO Tests")
class KioskDTOTest {

    private static final Image THUMBNAIL =
            new Image("https://i.ytimg.com/vi/x/thumb.jpg", 180, 320, ResolutionLevel.MEDIUM);
    private static final Image AVATAR =
            new Image("https://yt3.ggpht.com/avatar=s48", 48, 48, ResolutionLevel.LOW);

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    /** A fully-stubbed stream item, shared by the end-to-end and item-level tests. */
    private static StreamInfoItem stubStreamItem() {
        StreamInfoItem streamItem = mock(StreamInfoItem.class);
        when(streamItem.getName()).thenReturn("Trending Video");
        when(streamItem.getUrl()).thenReturn("https://youtube.com/watch?v=x");
        when(streamItem.getUploaderName()).thenReturn("Some Channel");
        when(streamItem.getUploaderUrl()).thenReturn("https://youtube.com/channel/x");
        when(streamItem.isUploaderVerified()).thenReturn(true);
        when(streamItem.getUploaderAvatars()).thenReturn(List.of(AVATAR));
        when(streamItem.getDuration()).thenReturn(120L);
        when(streamItem.getViewCount()).thenReturn(500L);
        when(streamItem.getUploadDate()).thenReturn(null);
        when(streamItem.getTextualUploadDate()).thenReturn("2 days ago");
        when(streamItem.getStreamType()).thenReturn(StreamType.VIDEO_STREAM);
        when(streamItem.getThumbnails()).thenReturn(List.of(THUMBNAIL));
        return streamItem;
    }

    private static KioskDTO.KioskStreamItemDTO expectedStreamItemDto() {
        return new KioskDTO.KioskStreamItemDTO(
                "Trending Video",
                "https://youtube.com/watch?v=x",
                "Some Channel",
                "https://youtube.com/channel/x",
                true,
                List.of(new KioskDTO.ImageDTO("https://yt3.ggpht.com/avatar=s48", 48, 48, "LOW")),
                120L,
                500L,
                null,
                "2 days ago",
                "VIDEO_STREAM",
                List.of(new KioskDTO.ImageDTO(
                        "https://i.ytimg.com/vi/x/thumb.jpg", 320, 180, "MEDIUM"))
        );
    }

    // ── KioskDTO.from(KioskInfo) ────────────────────────────────────────

    @Nested
    @DisplayName("KioskDTO mapping")
    class KioskDtoMappingTests {

        @Test
        @DisplayName("Maps a KioskInfo end-to-end, including nested items and next page")
        void mapsKioskInfoEndToEnd() {
            StreamInfoItem streamItem = stubStreamItem();

            Page nextPage = new Page(
                    "https://youtube.com/next", "pageId", List.of("id1"), null, "token".getBytes());

            KioskInfo kioskInfo = mock(KioskInfo.class);
            when(kioskInfo.getServiceId()).thenReturn(0);
            when(kioskInfo.getId()).thenReturn("trending");
            when(kioskInfo.getName()).thenReturn("Trending");
            when(kioskInfo.getUrl()).thenReturn("https://youtube.com/feed/trending");
            when(kioskInfo.getRelatedItems()).thenReturn(List.of(streamItem));
            when(kioskInfo.getNextPage()).thenReturn(nextPage);

            KioskDTO expected = new KioskDTO(
                    0,
                    "trending",
                    "Trending",
                    "https://youtube.com/feed/trending",
                    List.of(expectedStreamItemDto()),
                    new KioskDTO.NextPageDTO(
                            "https://youtube.com/next",
                            "pageId",
                            List.of("id1"),
                            Base64.getEncoder().encodeToString("token".getBytes())
                    )
            );

            assertThat(KioskDTO.from(kioskInfo)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Leaves nextPage null when the kiosk has no next page")
        void leavesNextPageNullWhenAbsent() {
            KioskInfo kioskInfo = mock(KioskInfo.class);
            when(kioskInfo.getServiceId()).thenReturn(0);
            when(kioskInfo.getId()).thenReturn("trending");
            when(kioskInfo.getName()).thenReturn("Trending");
            when(kioskInfo.getUrl()).thenReturn("https://youtube.com/feed/trending");
            when(kioskInfo.getRelatedItems()).thenReturn(List.of());
            when(kioskInfo.getNextPage()).thenReturn(null);

            assertThat(KioskDTO.from(kioskInfo).nextPage()).isNull();
        }
    }

    // ── KioskStreamItemDTO.from(StreamInfoItem) ─────────────────────────

    @Nested
    @DisplayName("KioskStreamItemDTO mapping")
    class StreamItemMappingTests {

        @Test
        @DisplayName("Maps uploader avatars alongside thumbnails")
        void mapsUploaderAvatars() {
            assertThat(KioskDTO.KioskStreamItemDTO.from(stubStreamItem()).uploaderAvatars())
                    .containsExactly(new KioskDTO.ImageDTO(
                            "https://yt3.ggpht.com/avatar=s48", 48, 48, "LOW"));
        }

        @Test
        @DisplayName("Maps empty uploader avatars to an empty list")
        void mapsEmptyUploaderAvatars() {
            StreamInfoItem streamItem = stubStreamItem();
            when(streamItem.getUploaderAvatars()).thenReturn(List.of());

            assertThat(KioskDTO.KioskStreamItemDTO.from(streamItem).uploaderAvatars()).isEmpty();
        }

        @Test
        @DisplayName("Leaves uploadDate null when the stream has none")
        void leavesUploadDateNullWhenAbsent() {
            StreamInfoItem streamItem = mock(StreamInfoItem.class);
            when(streamItem.getThumbnails()).thenReturn(List.of());
            when(streamItem.getUploaderAvatars()).thenReturn(List.of());
            when(streamItem.getUploadDate()).thenReturn(null);
            when(streamItem.getStreamType()).thenReturn(StreamType.VIDEO_STREAM);

            assertThat(KioskDTO.KioskStreamItemDTO.from(streamItem).uploadDate()).isNull();
        }

        @Test
        @DisplayName("Currently throws NPE when streamType is null")
        void throwsWhenStreamTypeIsNull() {
            StreamInfoItem streamItem = mock(StreamInfoItem.class);
            when(streamItem.getThumbnails()).thenReturn(List.of());
            when(streamItem.getUploaderAvatars()).thenReturn(List.of());
            when(streamItem.getStreamType()).thenReturn(null);

            assertThatThrownBy(() -> KioskDTO.KioskStreamItemDTO.from(streamItem))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ── ImageDTO.from(Image) / fromAll(List<Image>) ─────────────────────

    @Nested
    @DisplayName("ImageDTO mapping")
    class ImageMappingTests {

        @Test
        @DisplayName("Maps url, width, height and resolution level from an Image")
        void mapsFromImage() {
            assertThat(KioskDTO.ImageDTO.from(THUMBNAIL))
                    .isEqualTo(new KioskDTO.ImageDTO(
                            "https://i.ytimg.com/vi/x/thumb.jpg", 320, 180, "MEDIUM"));
        }

        @Test
        @DisplayName("fromAll maps every image, preserving order")
        void fromAllMapsEveryImage() {
            assertThat(KioskDTO.ImageDTO.fromAll(List.of(AVATAR, THUMBNAIL)))
                    .extracting(KioskDTO.ImageDTO::url)
                    .containsExactly(AVATAR.getUrl(), THUMBNAIL.getUrl());
        }

        @Test
        @DisplayName("fromAll returns an empty list for null input")
        void fromAllHandlesNull() {
            assertThat(KioskDTO.ImageDTO.fromAll(null)).isEmpty();
        }
    }

    // ── NextPageDTO.from(Page) / toPage(...) ────────────────────────────

    @Nested
    @DisplayName("NextPageDTO mapping")
    class NextPageMappingTests {

        @Test
        @DisplayName("Returns null when the extractor page is null")
        void fromReturnsNullForNullPage() {
            assertThat(KioskDTO.NextPageDTO.from(null)).isNull();
        }

        @Test
        @DisplayName("Base64-encodes the body and leaves other fields untouched")
        void fromEncodesBody() {
            Page page = new Page(
                    "https://youtube.com/next", "pageId", List.of("id1"), null, "token".getBytes());

            assertThat(KioskDTO.NextPageDTO.from(page)).isEqualTo(new KioskDTO.NextPageDTO(
                    "https://youtube.com/next",
                    "pageId",
                    List.of("id1"),
                    Base64.getEncoder().encodeToString("token".getBytes())));
        }

        @Test
        @DisplayName("Leaves body null when the page body is null")
        void fromLeavesNullBodyNull() {
            Page page = new Page("https://youtube.com/next", "pageId", List.of("id1"), null, null);

            assertThat(KioskDTO.NextPageDTO.from(page).body()).isNull();
        }
    }

    // ── JSON ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("JSON serialization")
    class JsonSerializationTests {

        @Test
        @DisplayName("Serializes record accessor names as JSON property names")
        void serializesUsingAccessorNames() throws Exception {
            KioskDTO dto = new KioskDTO(
                    0, "trending", "Trending", "https://youtube.com/feed/trending", List.of(), null);

            String json = objectMapper.writeValueAsString(dto);

            assertThat(json).contains(
                    "\"serviceId\":0",
                    "\"id\":\"trending\"",
                    "\"name\":\"Trending\"",
                    "\"url\":\"https://youtube.com/feed/trending\"");
        }

        @Test
        @DisplayName("Serializes uploader avatars in the same shape as other responses")
        void serializesUploaderAvatars() throws Exception {
            String json = objectMapper.writeValueAsString(expectedStreamItemDto());

            assertThat(json).contains(
                    "\"uploaderAvatars\":[{",
                    "\"url\":\"https://yt3.ggpht.com/avatar=s48\"",
                    "\"width\":48",
                    "\"height\":48",
                    "\"estimatedResolutionLevel\":\"LOW\"");
        }
    }
}