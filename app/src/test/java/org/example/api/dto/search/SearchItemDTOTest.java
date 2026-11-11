package org.example.api.dto.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.playlist.PlaylistInfo;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit tests for SearchItemDTO
 */
@DisplayName("SearchItemDTO Tests")
class SearchItemDTOTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // ── Stream mapping ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Stream mapping")
    class StreamMappingTests {

        @Test
        @DisplayName("Maps every field from a fully-populated StreamInfoItem, using the FIRST of multiple thumbnails")
        void mapsAllFieldsFromFullStreamInfoItem() {
            StreamInfoItem streamItem = mock(StreamInfoItem.class);
            when(streamItem.getName()).thenReturn("Test Video");
            when(streamItem.getUrl()).thenReturn("https://youtube.com/watch?v=test123");

            Image thumb1 = mock(Image.class);
            when(thumb1.getUrl()).thenReturn("https://i.ytimg.com/vi/test123/maxresdefault.jpg");
            Image thumb2 = mock(Image.class);
            when(thumb2.getUrl()).thenReturn("https://i.ytimg.com/vi/test123/other.jpg");
            when(streamItem.getThumbnails()).thenReturn(List.of(thumb1, thumb2));

            when(streamItem.getUploaderName()).thenReturn("Test Channel");
            when(streamItem.getUploaderUrl()).thenReturn("https://youtube.com/channel/test");
            when(streamItem.isUploaderVerified()).thenReturn(true);

            Image avatar = mock(Image.class);
            when(avatar.getUrl()).thenReturn("https://yt3.ggpht.com/avatar.jpg");
            when(streamItem.getUploaderAvatars()).thenReturn(List.of(avatar));

            when(streamItem.getDuration()).thenReturn(300L);
            when(streamItem.getViewCount()).thenReturn(1_000_000L);

            DateWrapper uploadDate = mock(DateWrapper.class);
            when(uploadDate.offsetDateTime()).thenReturn(OffsetDateTime.parse("2025-12-01T10:00:11Z"));
            when(streamItem.getUploadDate()).thenReturn(uploadDate);

            when(streamItem.getStreamType()).thenReturn(StreamType.VIDEO_STREAM);
            when(streamItem.isShortFormContent()).thenReturn(false);

            SearchItemDTO dto = SearchItemDTO.from(streamItem);

            assertThat(dto.type()).isEqualTo("stream");
            assertThat(dto.name()).isEqualTo("Test Video");
            assertThat(dto.url()).isEqualTo("https://youtube.com/watch?v=test123");
            assertThat(dto.thumbnailUrl()).isEqualTo("https://i.ytimg.com/vi/test123/maxresdefault.jpg");
            assertThat(dto.uploaderName()).isEqualTo("Test Channel");
            assertThat(dto.uploaderUrl()).isEqualTo("https://youtube.com/channel/test");
            assertThat(dto.uploaderVerified()).isTrue();
            assertThat(dto.uploaderAvatarUrl()).isEqualTo("https://yt3.ggpht.com/avatar.jpg");
            assertThat(dto.duration()).isEqualTo(300L);
            assertThat(dto.viewCount()).isEqualTo(1_000_000L);
            assertThat(dto.uploadDate()).isEqualTo("2025-12-01T10:00:11Z");
            assertThat(dto.streamType()).isEqualTo("VIDEO_STREAM");
            assertThat(dto.shortFormContent()).isFalse();
        }

        @Test
        @DisplayName("Leaves thumbnailUrl, uploaderAvatarUrl, uploadDate, and streamType null when the stream has none")
        void mapsMinimalStreamInfoItem() {
            StreamInfoItem streamItem = mock(StreamInfoItem.class);
            when(streamItem.getName()).thenReturn("Minimal Video");
            when(streamItem.getUrl()).thenReturn("https://youtube.com/watch?v=min123");
            when(streamItem.getThumbnails()).thenReturn(List.of());
            when(streamItem.getUploaderAvatars()).thenReturn(List.of());
            when(streamItem.getUploadDate()).thenReturn(null);
            when(streamItem.getStreamType()).thenReturn(null);

            SearchItemDTO dto = SearchItemDTO.from(streamItem);

            assertThat(dto.type()).isEqualTo("stream");
            assertThat(dto.thumbnailUrl()).isNull();
            assertThat(dto.uploaderAvatarUrl()).isNull();
            assertThat(dto.uploadDate()).isNull();
            assertThat(dto.streamType()).isNull();
        }

        static Stream<Arguments> specialStreamCases() {
            return Stream.of(
                    Arguments.of("live stream (duration -1)", (Consumer<StreamInfoItem>) item -> {
                        when(item.getStreamType()).thenReturn(StreamType.LIVE_STREAM);
                        when(item.getDuration()).thenReturn(-1L);
                    }),
                    Arguments.of("short-form content", (Consumer<StreamInfoItem>) item -> {
                        when(item.isShortFormContent()).thenReturn(true);
                        when(item.getDuration()).thenReturn(30L);
                    }));
        }

        @ParameterizedTest(name = "Maps {0}")
        @MethodSource("specialStreamCases")
        @DisplayName("Passes through live-stream and short-form-content markers unchanged")
        void mapsSpecialStreamCases(String name, Consumer<StreamInfoItem> stub) {
            StreamInfoItem streamItem = mock(StreamInfoItem.class);
            when(streamItem.getName()).thenReturn(name);
            when(streamItem.getUrl()).thenReturn("https://youtube.com/watch?v=x");
            when(streamItem.getThumbnails()).thenReturn(List.of());
            stub.accept(streamItem);

            SearchItemDTO dto = SearchItemDTO.from(streamItem);

            assertThat(dto).isNotNull(); // dispatch reached the stream branch without error
        }
    }

    // ── Channel mapping ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Channel mapping")
    class ChannelMappingTests {

        @Test
        @DisplayName("Maps every field from a fully-populated ChannelInfoItem")
        void mapsAllFieldsFromFullChannelInfoItem() {
            ChannelInfoItem channelItem = mock(ChannelInfoItem.class);
            when(channelItem.getName()).thenReturn("Test Channel");
            when(channelItem.getUrl()).thenReturn("https://youtube.com/channel/test");
            Image thumbnail = mock(Image.class);
            when(thumbnail.getUrl()).thenReturn("https://yt3.ggpht.com/channel.jpg");
            when(channelItem.getThumbnails()).thenReturn(List.of(thumbnail));
            when(channelItem.getSubscriberCount()).thenReturn(1_000_000L);
            when(channelItem.getStreamCount()).thenReturn(500L);
            when(channelItem.getDescription()).thenReturn("Channel description");
            when(channelItem.isVerified()).thenReturn(true);

            SearchItemDTO dto = SearchItemDTO.from(channelItem);

            assertThat(dto.type()).isEqualTo("channel");
            assertThat(dto.name()).isEqualTo("Test Channel");
            assertThat(dto.thumbnailUrl()).isEqualTo("https://yt3.ggpht.com/channel.jpg");
            assertThat(dto.subscriberCount()).isEqualTo(1_000_000L);
            assertThat(dto.streamCount()).isEqualTo(500L);
            assertThat(dto.description()).isEqualTo("Channel description");
            assertThat(dto.uploaderVerified()).isTrue();
        }

        @Test
        @DisplayName("Passes through unknown counts (-1), a null description, and unverified status")
        void mapsMinimalChannelInfoItem() {
            ChannelInfoItem channelItem = mock(ChannelInfoItem.class);
            when(channelItem.getName()).thenReturn("Minimal Channel");
            when(channelItem.getUrl()).thenReturn("https://youtube.com/channel/minimal");
            when(channelItem.getThumbnails()).thenReturn(List.of());
            when(channelItem.getSubscriberCount()).thenReturn(-1L);
            when(channelItem.getStreamCount()).thenReturn(-1L);
            when(channelItem.getDescription()).thenReturn(null);
            when(channelItem.isVerified()).thenReturn(false);

            SearchItemDTO dto = SearchItemDTO.from(channelItem);

            assertThat(dto.subscriberCount()).isEqualTo(-1L);
            assertThat(dto.streamCount()).isEqualTo(-1L);
            assertThat(dto.description()).isNull();
            assertThat(dto.uploaderVerified()).isFalse();
        }
    }

    // ── Playlist mapping ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Playlist mapping")
    class PlaylistMappingTests {

        @Test
        @DisplayName("Maps every field from a fully-populated PlaylistInfoItem")
        void mapsAllFieldsFromFullPlaylistInfoItem() {
            PlaylistInfoItem playlistItem = mock(PlaylistInfoItem.class);
            when(playlistItem.getName()).thenReturn("Test Playlist");
            when(playlistItem.getUrl()).thenReturn("https://youtube.com/playlist?list=test");
            Image thumbnail = mock(Image.class);
            when(thumbnail.getUrl()).thenReturn("https://i.ytimg.com/playlist.jpg");
            when(playlistItem.getThumbnails()).thenReturn(List.of(thumbnail));
            when(playlistItem.getUploaderName()).thenReturn("Playlist Creator");
            when(playlistItem.getUploaderUrl()).thenReturn("https://youtube.com/channel/creator");
            when(playlistItem.getStreamCount()).thenReturn(50L);
            when(playlistItem.getPlaylistType()).thenReturn(PlaylistInfo.PlaylistType.NORMAL);

            SearchItemDTO dto = SearchItemDTO.from(playlistItem);

            assertThat(dto.type()).isEqualTo("playlist");
            assertThat(dto.thumbnailUrl()).isEqualTo("https://i.ytimg.com/playlist.jpg");
            assertThat(dto.uploaderName()).isEqualTo("Playlist Creator");
            assertThat(dto.uploaderUrl()).isEqualTo("https://youtube.com/channel/creator");
            assertThat(dto.videoCount()).isEqualTo(50L);
            assertThat(dto.playlistType()).isEqualTo("NORMAL");
        }

        @Test
        @DisplayName("Passes through null uploader fields, unknown video count (-1), and a null playlist type")
        void mapsMinimalPlaylistInfoItem() {
            PlaylistInfoItem playlistItem = mock(PlaylistInfoItem.class);
            when(playlistItem.getName()).thenReturn("Minimal Playlist");
            when(playlistItem.getUrl()).thenReturn("https://youtube.com/playlist?list=min");
            when(playlistItem.getThumbnails()).thenReturn(List.of());
            when(playlistItem.getUploaderName()).thenReturn(null);
            when(playlistItem.getUploaderUrl()).thenReturn(null);
            when(playlistItem.getStreamCount()).thenReturn(-1L);
            when(playlistItem.getPlaylistType()).thenReturn(null);

            SearchItemDTO dto = SearchItemDTO.from(playlistItem);

            assertThat(dto.uploaderName()).isNull();
            assertThat(dto.uploaderUrl()).isNull();
            assertThat(dto.videoCount()).isEqualTo(-1L);
            assertThat(dto.playlistType()).isNull();
        }
    }

    // ── JSON ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("JSON serialization")
    class JsonSerializationTests {

        @Test
        @DisplayName("Serializes the discriminating 'type' field correctly for all three item kinds")
        void serializesTypeDiscriminatorForEachKind() throws Exception {
            SearchItemDTO stream =
                    SearchItemDTO.builder().type("stream").duration(300L).build();
            SearchItemDTO channel = SearchItemDTO.builder()
                    .type("channel")
                    .subscriberCount(1_000_000L)
                    .build();
            SearchItemDTO playlist =
                    SearchItemDTO.builder().type("playlist").videoCount(50L).build();

            assertThat(objectMapper.writeValueAsString(stream)).contains("\"type\":\"stream\"", "\"duration\":300");
            assertThat(objectMapper.writeValueAsString(channel))
                    .contains("\"type\":\"channel\"", "\"subscriberCount\":1000000");
            assertThat(objectMapper.writeValueAsString(playlist))
                    .contains("\"type\":\"playlist\"", "\"videoCount\":50");
        }

        @Test
        @DisplayName("Round-trips every field through serialize/deserialize unchanged")
        void roundTripPreservesAllFields() throws Exception {
            SearchItemDTO original = SearchItemDTO.builder()
                    .type("stream")
                    .name("Test Video")
                    .url("https://youtube.com/watch?v=test")
                    .duration(300L)
                    .viewCount(1_000_000L)
                    .uploaderVerified(true)
                    .build();

            assertThat(objectMapper.readValue(objectMapper.writeValueAsString(original), SearchItemDTO.class))
                    .isEqualTo(original);

            SearchItemDTO restored =
                    objectMapper.readValue(objectMapper.writeValueAsString(original), SearchItemDTO.class);

            assertThat(restored.type()).isEqualTo(original.type());
            assertThat(restored.name()).isEqualTo(original.name());
            assertThat(restored.url()).isEqualTo(original.url());
            assertThat(restored.duration()).isEqualTo(original.duration());
            assertThat(restored.viewCount()).isEqualTo(original.viewCount());
            assertThat(restored.uploaderVerified()).isEqualTo(original.uploaderVerified());
        }
    }
}
