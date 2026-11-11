package org.example.api.dto.search;

import java.util.List;
import org.schabi.newpipe.extractor.ListExtractor;

/**
 * Data Transfer Object for paginated search results.
 * Used when retrieving additional pages of search results.
 */
public record SearchPageDTO(
        List<SearchItemDTO> items, SearchResultDTO.PageDto nextPage, boolean hasNextPage, int itemCount) {

    public SearchPageDTO {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static SearchPageDTO from(ListExtractor.InfoItemsPage<?> page) {
        List<SearchItemDTO> items =
                page.getItems().stream().map(SearchItemDTO::from).toList();
        SearchResultDTO.PageDto nextPage = buildNextPage(page.getNextPage());
        return new SearchPageDTO(items, nextPage, nextPage != null, items.size());
    }

    /** Copy with a different item list; itemCount follows the list. */
    public SearchPageDTO withItems(List<SearchItemDTO> items) {
        return new SearchPageDTO(items, nextPage, hasNextPage, items.size());
    }

    private static SearchResultDTO.PageDto buildNextPage(org.schabi.newpipe.extractor.Page page) {
        if (page == null || page.getUrl() == null) return null;
        return new SearchResultDTO.PageDto(page.getUrl(), page.getId());
    }
}
