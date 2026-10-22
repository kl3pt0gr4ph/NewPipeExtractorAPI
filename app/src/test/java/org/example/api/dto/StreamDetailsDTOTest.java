package org.example.api.dto;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.stream.Description;
import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StreamDetailsDTO.
 */
@DisplayName("StreamDetailsDTO Tests")
class StreamDetailsDTOTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Maps every field from a fully-populated StreamInfo, including multiple uploader avatars")
    void mapsAllFieldsFromStreamInfo() {
        StreamInfo streamInfo = mock(StreamInfo.class);
        when(streamInfo.getName()).thenReturn("Test Video Title");

        Description description = mock(Description.class);
        when(streamInfo.getDescription()).thenReturn(description);

        Image avatar1 = mock(Image.class);
        when(avatar1.getUrl()).thenReturn("https://yt3.ggpht.com/avatar-small.jpg");
        Image avatar2 = mock(Image.class);
        when(avatar2.getUrl()).thenReturn("https://yt3.ggpht.com/avatar-large.jpg");
        when(streamInfo.getUploaderAvatars()).thenReturn(List.of(avatar1, avatar2));

        when(streamInfo.getViewCount()).thenReturn(1_000_000L);
        when(streamInfo.getLikeCount()).thenReturn(50_000L);
        when(streamInfo.getDislikeCount()).thenReturn(500L);
        when(streamInfo.getUploaderName()).thenReturn("Channel Name");
        when(streamInfo.getUploaderSubscriberCount()).thenReturn(500_000L);
        when(streamInfo.getTextualUploadDate()).thenReturn("2025-12-01");

        StreamDetailsDTO dto = StreamDetailsDTO.from(streamInfo);

        assertThat(dto.getVideoTitle()).isEqualTo("Test Video Title");
        assertThat(dto.getDescription()).isSameAs(description);
        assertThat(dto.getUploaderAvatars())
                .extracting(Image::getUrl)
                .containsExactly("https://yt3.ggpht.com/avatar-small.jpg", "https://yt3.ggpht.com/avatar-large.jpg");
        assertThat(dto.getViewCount()).isEqualTo(1_000_000L);
        assertThat(dto.getLikeCount()).isEqualTo(50_000L);
        assertThat(dto.getDislikeCount()).isEqualTo(500L);
        assertThat(dto.getChannelName()).isEqualTo("Channel Name");
        assertThat(dto.getChannelSubscriberCount()).isEqualTo(500_000L);
        assertThat(dto.getUploadDate()).isEqualTo("2025-12-01");
    }

    @Test
    @DisplayName("Passes through nulls and NewPipe's -1 hidden/unknown sentinels unchanged")
    void passesThroughNullsAndSentinelValues() {
        StreamInfo streamInfo = mock(StreamInfo.class);
        when(streamInfo.getName()).thenReturn("Minimal Video");
        when(streamInfo.getDescription()).thenReturn(null);
        when(streamInfo.getUploaderAvatars()).thenReturn(List.of());
        when(streamInfo.getViewCount()).thenReturn(0L);
        when(streamInfo.getDislikeCount()).thenReturn(-1L); // NewPipe: unknown
        when(streamInfo.getUploaderName()).thenReturn(null);
        when(streamInfo.getUploaderSubscriberCount()).thenReturn(-1L); // NewPipe: hidden
        when(streamInfo.getTextualUploadDate()).thenReturn(null);

        StreamDetailsDTO dto = StreamDetailsDTO.from(streamInfo);

        assertThat(dto.getDescription()).isNull();
        assertThat(dto.getUploaderAvatars()).isEmpty();
        assertThat(dto.getDislikeCount()).isEqualTo(-1L);
        assertThat(dto.getChannelName()).isNull();
        assertThat(dto.getChannelSubscriberCount()).isEqualTo(-1L);
        assertThat(dto.getUploadDate()).isNull();
    }

    @Test
    @DisplayName("Round-trips every scalar field through serialize/deserialize unchanged")
    void roundTripPreservesScalarFields() throws Exception {
        StreamDetailsDTO original = new StreamDetailsDTO();
        original.setVideoTitle("Test Video");
        original.setViewCount(1_000_000L);
        original.setLikeCount(50_000L);
        original.setDislikeCount(500L);
        original.setChannelName("Channel Name");
        original.setChannelSubscriberCount(500_000L);
        original.setUploadDate("2025-12-01");

        StreamDetailsDTO restored = objectMapper.readValue(
                objectMapper.writeValueAsString(original), StreamDetailsDTO.class);

        assertThat(restored.getVideoTitle()).isEqualTo(original.getVideoTitle());
        assertThat(restored.getViewCount()).isEqualTo(original.getViewCount());
        assertThat(restored.getLikeCount()).isEqualTo(original.getLikeCount());
        assertThat(restored.getDislikeCount()).isEqualTo(original.getDislikeCount());
        assertThat(restored.getChannelName()).isEqualTo(original.getChannelName());
        assertThat(restored.getChannelSubscriberCount()).isEqualTo(original.getChannelSubscriberCount());
        assertThat(restored.getUploadDate()).isEqualTo(original.getUploadDate());
    }
}