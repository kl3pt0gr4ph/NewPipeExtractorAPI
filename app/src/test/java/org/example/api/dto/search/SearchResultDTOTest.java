package org.example.api.dto.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SearchResultDTO.
 */
@DisplayName("SearchResultDTO Tests")
class SearchResultDTOTest {

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

    private SearchInfo searchInfo(String searchString, List<InfoItem> items, Page nextPage) {
        SearchInfo info = mock(SearchInfo.class);
        when(info.getUrl()).thenReturn("https://youtube.com/results?search_query=" + searchString);
        when(info.getSearchString()).thenReturn(searchString);
        when(info.getRelatedItems()).thenReturn(items);
        when(info.getNextPage()).thenReturn(nextPage);
        return info;
    }

    // ── from() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Maps every scalar field, the item list, and a present cursor from a fully-populated SearchInfo")
    void mapsAllFieldsFromFullSearchInfo() {
        SearchInfo info = searchInfo("test",
                List.of(streamItem("Video 1", "https://youtube.com/watch?v=1")),
                new Page("https://youtube.com/results?page=2", "4qmFsgJcEBIYdmlkZW8"));
        when(info.getOriginalUrl()).thenReturn("https://youtube.com/search?q=test");
        when(info.getName()).thenReturn("Search Results");
        when(info.getSearchSuggestion()).thenReturn("test suggestion");
        when(info.isCorrectedSearch()).thenReturn(true);

        SearchResultDTO dto = SearchResultDTO.from(info);

        assertThat(dto.getUrl()).isEqualTo("https://youtube.com/results?search_query=test");
        assertThat(dto.getOriginalUrl()).isEqualTo("https://youtube.com/search?q=test");
        assertThat(dto.getName()).isEqualTo("Search Results");
        assertThat(dto.getSearchString()).isEqualTo("test");
        assertThat(dto.getSearchSuggestion()).isEqualTo("test suggestion");
        assertThat(dto.isCorrectedSearch()).isTrue();
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.isHasNextPage()).isTrue();
        assertThat(dto.getNextPage().url()).isEqualTo("https://youtube.com/results?page=2");
        assertThat(dto.getNextPage().id()).isEqualTo("4qmFsgJcEBIYdmlkZW8");
    }

    @Test
    @DisplayName("Leaves originalUrl/searchSuggestion null, items empty, and hasNextPage false for a minimal, last-page SearchInfo")
    void mapsMinimalSearchInfo() {
        SearchInfo info = searchInfo("query", List.of(), null);
        when(info.getOriginalUrl()).thenReturn(null);
        when(info.getSearchSuggestion()).thenReturn(null);
        when(info.isCorrectedSearch()).thenReturn(false);

        SearchResultDTO dto = SearchResultDTO.from(info);

        assertThat(dto.getOriginalUrl()).isNull();
        assertThat(dto.getSearchSuggestion()).isNull();
        assertThat(dto.isCorrectedSearch()).isFalse();
        assertThat(dto.getItems()).isEmpty();
        assertThat(dto.isHasNextPage()).isFalse();
        assertThat(dto.getNextPage()).isNull();
    }

    // ── Pagination cursor (buildNextPage) ─────────────────────────────────

    static Stream<Arguments> cursorCases() {
        return Stream.of(
                Arguments.of("null Page", null, null, null),
                Arguments.of("Page with null url", new Page(null, "someToken"), null, null),
                Arguments.of("Page with null id (token absent)",
                        new Page("https://youtube.com/browse", (String) null),
                        "https://youtube.com/browse", null),
                Arguments.of("Page with a continuation token",
                        new Page("https://youtube.com/browse", "4qmFsgJcEBIYdmlkZW8_token"),
                        "https://youtube.com/browse", "4qmFsgJcEBIYdmlkZW8_token"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cursorCases")
    @DisplayName("Builds the pagination cursor per the url/id contract, passing the token through as id verbatim")
    void buildsCursorPerContract(String name, Page nextPage, String expectedUrl, String expectedId) {
        SearchResultDTO dto = SearchResultDTO.from(searchInfo("query", List.of(), nextPage));

        if (expectedUrl == null) {
            assertThat(dto.getNextPage()).isNull();
            assertThat(dto.isHasNextPage()).isFalse();
        } else {
            assertThat(dto.getNextPage().url()).isEqualTo(expectedUrl);
            assertThat(dto.getNextPage().id()).isEqualTo(expectedId);
        }
    }

    // ── JSON ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Serializes nextPage as an object when present, and as null when absent")
    void serializesNextPageAsObjectOrNull() throws Exception {
        SearchResultDTO withPage = SearchResultDTO.from(
                searchInfo("query", List.of(), new Page("https://youtube.com/browse", "token123")));
        SearchResultDTO withoutPage = SearchResultDTO.from(searchInfo("query", List.of(), null));

        assertThat(objectMapper.writeValueAsString(withPage))
                .contains("\"nextPage\":{", "\"url\"", "\"id\"", "https://youtube.com/browse");
        assertThat(objectMapper.writeValueAsString(withoutPage))
                .contains("\"nextPage\":null");
    }

    @Test
    @DisplayName("Round-trips every scalar field and the item count through serialize/deserialize unchanged")
    void roundTripPreservesFieldsAndItemCount() throws Exception {
        SearchResultDTO original = SearchResultDTO.from(searchInfo("test",
                List.of(streamItem("Video 1", "https://youtube.com/watch?v=1")),
                new Page("https://youtube.com/results?page=2", "token")));

        SearchResultDTO restored = objectMapper.readValue(
                objectMapper.writeValueAsString(original), SearchResultDTO.class);

        assertThat(restored.getSearchString()).isEqualTo(original.getSearchString());
        assertThat(restored.getUrl()).isEqualTo(original.getUrl());
        assertThat(restored.isHasNextPage()).isEqualTo(original.isHasNextPage());
        assertThat(restored.getItems()).hasSize(original.getItems().size());
    }
}