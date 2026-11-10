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

        assertThat(dto.videoTitle()).isEqualTo("Test Video Title");
        assertThat(dto.description()).isSameAs(description);
        assertThat(dto.uploaderAvatars())
                .extracting(Image::getUrl)
                .containsExactly("https://yt3.ggpht.com/avatar-small.jpg", "https://yt3.ggpht.com/avatar-large.jpg");
        assertThat(dto.viewCount()).isEqualTo(1_000_000L);
        assertThat(dto.likeCount()).isEqualTo(50_000L);
        assertThat(dto.dislikeCount()).isEqualTo(500L);
        assertThat(dto.channelName()).isEqualTo("Channel Name");
        assertThat(dto.channelSubscriberCount()).isEqualTo(500_000L);
        assertThat(dto.uploadDate()).isEqualTo("2025-12-01");
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

        assertThat(dto.description()).isNull();
        assertThat(dto.uploaderAvatars()).isEmpty();
        assertThat(dto.dislikeCount()).isEqualTo(-1L);
        assertThat(dto.channelName()).isNull();
        assertThat(dto.channelSubscriberCount()).isEqualTo(-1L);
        assertThat(dto.uploadDate()).isNull();
    }

    @Test
    @DisplayName("Round-trips every scalar field through serialize/deserialize unchanged")
    void roundTripPreservesScalarFields() throws Exception {
        StreamDetailsDTO original = new StreamDetailsDTO("Test Video", null, List.of(),
                1_000_000L, 50_000L, 500L, "Channel Name", 500_000L, "2025-12-01");

        assertThat(objectMapper.readValue(objectMapper.writeValueAsString(original), StreamDetailsDTO.class))
                .isEqualTo(original);
    }
}