package org.example.api.dto.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit tests for SearchPageDTO.
 */
@DisplayName("SearchPageDTO Tests")
class SearchPageDTOTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private InfoItem streamItem(String name, String url) {
        return new StreamInfoItem(0, url, name, StreamType.VIDEO_STREAM) {
            @Override
            public List<org.schabi.newpipe.extractor.Image> getThumbnails() {
                return List.of();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private ListExtractor.InfoItemsPage<InfoItem> infoItemsPage(List<InfoItem> items, Page nextPage) {
        ListExtractor.InfoItemsPage<InfoItem> page = mock(ListExtractor.InfoItemsPage.class);
        when(page.getItems()).thenReturn(items);
        when(page.getNextPage()).thenReturn(nextPage);
        return page;
    }

    // ── from() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Maps items (as type=stream) and a present cursor, with itemCount matching the item list")
    void mapsItemsAndCursorWhenNextPageExists() {
        List<InfoItem> items = List.of(
                streamItem("Video 1", "https://youtube.com/watch?v=1"),
                streamItem("Video 2", "https://youtube.com/watch?v=2"));
        Page nextPage = new Page("https://youtube.com/search?q=test&page=2", "4qmFsgJcEBIYdmlkZW8continuation");

        SearchPageDTO dto = SearchPageDTO.from(infoItemsPage(items, nextPage));

        assertThat(dto.itemCount()).isEqualTo(2);
        assertThat(dto.items()).extracting(SearchItemDTO::type).containsExactly("stream", "stream");
        assertThat(dto.hasNextPage()).isTrue();
        assertThat(dto.nextPage().url()).isEqualTo("https://youtube.com/search?q=test&page=2");
        assertThat(dto.nextPage().id()).isEqualTo("4qmFsgJcEBIYdmlkZW8continuation");
    }

    @Test
    @DisplayName("Maps an empty final page to zero items and no cursor")
    void mapsEmptyPageWithoutNextPage() {
        SearchPageDTO dto = SearchPageDTO.from(infoItemsPage(List.of(), null));

        assertThat(dto.itemCount()).isZero();
        assertThat(dto.items()).isEmpty();
        assertThat(dto.hasNextPage()).isFalse();
        assertThat(dto.nextPage()).isNull();
    }

    @Test
    @DisplayName("Sets the cursor's id to null when the Page carries no continuation token")
    void setsNextPageIdToNullWhenPageHasNoId() {
        Page nextPage = new Page("https://youtube.com/browse", (String) null);

        SearchPageDTO dto = SearchPageDTO.from(infoItemsPage(List.of(), nextPage));

        assertThat(dto.nextPage()).isNotNull();
        assertThat(dto.nextPage().url()).isEqualTo("https://youtube.com/browse");
        assertThat(dto.nextPage().id()).isNull();
    }

    // ── JSON ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Serializes nextPage as an object when present, and as null when absent")
    void serializesNextPageAsObjectOrNull() throws Exception {
        SearchPageDTO withPage =
                SearchPageDTO.from(infoItemsPage(List.of(), new Page("https://next-page.com", "bodyBase64==")));
        SearchPageDTO withoutPage = SearchPageDTO.from(infoItemsPage(List.of(), null));

        assertThat(objectMapper.writeValueAsString(withPage))
                .contains("\"nextPage\":{", "\"url\"", "\"id\"", "https://next-page.com");
        assertThat(objectMapper.writeValueAsString(withoutPage)).contains("\"nextPage\":null", "\"hasNextPage\":false");
    }

    @Test
    @DisplayName("Round-trips itemCount and hasNextPage through serialize/deserialize unchanged")
    void roundTripPreservesFields() throws Exception {
        SearchPageDTO original =
                SearchPageDTO.from(infoItemsPage(List.of(streamItem("Video", "https://youtube.com/watch?v=1")), null));

        SearchPageDTO restored = objectMapper.readValue(objectMapper.writeValueAsString(original), SearchPageDTO.class);

        assertThat(restored.itemCount()).isEqualTo(original.itemCount());
        assertThat(restored.hasNextPage()).isEqualTo(original.hasNextPage());
        assertThat(restored.nextPage()).isNull();
    }
}
