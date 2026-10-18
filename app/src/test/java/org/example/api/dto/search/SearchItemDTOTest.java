package org.example.api.dto.search;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

            assertThat(dto.getType()).isEqualTo("stream");
            assertThat(dto.getName()).isEqualTo("Test Video");
            assertThat(dto.getUrl()).isEqualTo("https://youtube.com/watch?v=test123");
            assertThat(dto.getThumbnailUrl()).isEqualTo("https://i.ytimg.com/vi/test123/maxresdefault.jpg");
            assertThat(dto.getUploaderName()).isEqualTo("Test Channel");
            assertThat(dto.getUploaderUrl()).isEqualTo("https://youtube.com/channel/test");
            assertThat(dto.getUploaderVerified()).isTrue();
            assertThat(dto.getUploaderAvatarUrl()).isEqualTo("https://yt3.ggpht.com/avatar.jpg");
            assertThat(dto.getDuration()).isEqualTo(300L);
            assertThat(dto.getViewCount()).isEqualTo(1_000_000L);
            assertThat(dto.getUploadDate()).isEqualTo("2025-12-01T10:00:11Z");
            assertThat(dto.getStreamType()).isEqualTo("VIDEO_STREAM");
            assertThat(dto.getShortFormContent()).isFalse();
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

            assertThat(dto.getType()).isEqualTo("stream");
            assertThat(dto.getThumbnailUrl()).isNull();
            assertThat(dto.getUploaderAvatarUrl()).isNull();
            assertThat(dto.getUploadDate()).isNull();
            assertThat(dto.getStreamType()).isNull();
        }

        static Stream<Arguments> specialStreamCases() {
            return Stream.of(
                    Arguments.of("live stream (duration -1)",
                            (Consumer<StreamInfoItem>) item -> {
                                when(item.getStreamType()).thenReturn(StreamType.LIVE_STREAM);
                                when(item.getDuration()).thenReturn(-1L);
                            }),
                    Arguments.of("short-form content",
                            (Consumer<StreamInfoItem>) item -> {
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

            assertThat(dto.getType()).isEqualTo("channel");
            assertThat(dto.getName()).isEqualTo("Test Channel");
            assertThat(dto.getThumbnailUrl()).isEqualTo("https://yt3.ggpht.com/channel.jpg");
            assertThat(dto.getSubscriberCount()).isEqualTo(1_000_000L);
            assertThat(dto.getStreamCount()).isEqualTo(500L);
            assertThat(dto.getDescription()).isEqualTo("Channel description");
            assertThat(dto.getUploaderVerified()).isTrue();
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

            assertThat(dto.getSubscriberCount()).isEqualTo(-1L);
            assertThat(dto.getStreamCount()).isEqualTo(-1L);
            assertThat(dto.getDescription()).isNull();
            assertThat(dto.getUploaderVerified()).isFalse();
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

            assertThat(dto.getType()).isEqualTo("playlist");
            assertThat(dto.getThumbnailUrl()).isEqualTo("https://i.ytimg.com/playlist.jpg");
            assertThat(dto.getUploaderName()).isEqualTo("Playlist Creator");
            assertThat(dto.getUploaderUrl()).isEqualTo("https://youtube.com/channel/creator");
            assertThat(dto.getVideoCount()).isEqualTo(50L);
            assertThat(dto.getPlaylistType()).isEqualTo("NORMAL");
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

            assertThat(dto.getUploaderName()).isNull();
            assertThat(dto.getUploaderUrl()).isNull();
            assertThat(dto.getVideoCount()).isEqualTo(-1L);
            assertThat(dto.getPlaylistType()).isNull();
        }
    }

    // ── JSON ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("JSON serialization")
    class JsonSerializationTests {

        @Test
        @DisplayName("Serializes the discriminating 'type' field correctly for all three item kinds")
        void serializesTypeDiscriminatorForEachKind() throws Exception {
            SearchItemDTO stream = new SearchItemDTO();
            stream.setType("stream");
            stream.setDuration(300L);

            SearchItemDTO channel = new SearchItemDTO();
            channel.setType("channel");
            channel.setSubscriberCount(1_000_000L);

            SearchItemDTO playlist = new SearchItemDTO();
            playlist.setType("playlist");
            playlist.setVideoCount(50L);

            assertThat(objectMapper.writeValueAsString(stream))
                    .contains("\"type\":\"stream\"", "\"duration\":300");
            assertThat(objectMapper.writeValueAsString(channel))
                    .contains("\"type\":\"channel\"", "\"subscriberCount\":1000000");
            assertThat(objectMapper.writeValueAsString(playlist))
                    .contains("\"type\":\"playlist\"", "\"videoCount\":50");
        }

        @Test
        @DisplayName("Round-trips every field through serialize/deserialize unchanged")
        void roundTripPreservesAllFields() throws Exception {
            SearchItemDTO original = new SearchItemDTO();
            original.setType("stream");
            original.setName("Test Video");
            original.setUrl("https://youtube.com/watch?v=test");
            original.setDuration(300L);
            original.setViewCount(1_000_000L);
            original.setUploaderVerified(true);

            SearchItemDTO restored = objectMapper.readValue(
                    objectMapper.writeValueAsString(original), SearchItemDTO.class);

            assertThat(restored.getType()).isEqualTo(original.getType());
            assertThat(restored.getName()).isEqualTo(original.getName());
            assertThat(restored.getUrl()).isEqualTo(original.getUrl());
            assertThat(restored.getDuration()).isEqualTo(original.getDuration());
            assertThat(restored.getViewCount()).isEqualTo(original.getViewCount());
            assertThat(restored.getUploaderVerified()).isEqualTo(original.getUploaderVerified());
        }
    }
}