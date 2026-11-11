package org.example.api.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.stream.Collectors;
import org.schabi.newpipe.extractor.search.SearchInfo;

/**
 * Data Transfer Object for search results.
 * Maps SearchInfo from NewPipe extractor to a serializable format.
 */
public class SearchResultDTO {

    @JsonProperty("url")
    private String url;

    @JsonProperty("originalUrl")
    private String originalUrl;

    @JsonProperty("name")
    private String name;

    @JsonProperty("searchString")
    private String searchString;

    @JsonProperty("searchSuggestion")
    private String searchSuggestion;

    @JsonProperty("isCorrectedSearch")
    private boolean isCorrectedSearch;

    @JsonProperty("items")
    private List<SearchItemDTO> items;

    @JsonProperty("nextPage")
    private PageDto nextPage;

    @JsonProperty("hasNextPage")
    private boolean hasNextPage;

    // ── Nested Record ────────────────────────────────────────────────────────────

    /**
     * Pagination cursor — pass both fields back to {@code GET /api/v1/search/page}.
     *
     * <p>{@code YoutubeSearchExtractor.getPage()} reads two things from the
     * {@link org.schabi.newpipe.extractor.Page} it receives:</p>
     * <ul>
     *   <li><b>url</b> — the InnerTube search endpoint URL.</li>
     *   <li><b>body</b> — Base64-encoded JSON POST body containing the continuation token.
     *       Without this, the search request has no continuation and YouTube returns
     *       page 1 again.</li>
     * </ul>
     *
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record PageDto(String url, String id // the continuation token string from page.getId()
            ) {}

    // ── Constructors ─────────────────────────────────────────────────────────────

    public SearchResultDTO() {}

    // ── Static Factory ───────────────────────────────────────────────────────────

    /**
     * Creates a SearchResultDTO from a SearchInfo object.
     *
     * @param searchInfo The SearchInfo object from NewPipe extractor
     * @return A new SearchResultDTO with mapped data
     */
    public static SearchResultDTO from(SearchInfo searchInfo) {
        SearchResultDTO dto = new SearchResultDTO();

        dto.setUrl(searchInfo.getUrl());
        dto.setOriginalUrl(searchInfo.getOriginalUrl());
        dto.setName(searchInfo.getName());
        dto.setSearchString(searchInfo.getSearchString());
        dto.setSearchSuggestion(searchInfo.getSearchSuggestion());
        dto.setCorrectedSearch(searchInfo.isCorrectedSearch());

        dto.setItems(
                searchInfo.getRelatedItems().stream().map(SearchItemDTO::from).collect(Collectors.toList()));

        dto.setNextPage(buildNextPage(searchInfo.getNextPage()));
        dto.setHasNextPage(dto.nextPage != null);

        return dto;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private static PageDto buildNextPage(org.schabi.newpipe.extractor.Page page) {
        if (page == null || page.getUrl() == null) return null;
        return new PageDto(page.getUrl(), page.getId());
    }

    // ── Getters & Setters ────────────────────────────────────────────────────────

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getSearchSuggestion() {
        return searchSuggestion;
    }

    public void setSearchSuggestion(String searchSuggestion) {
        this.searchSuggestion = searchSuggestion;
    }

    public boolean isCorrectedSearch() {
        return isCorrectedSearch;
    }

    public void setCorrectedSearch(boolean correctedSearch) {
        isCorrectedSearch = correctedSearch;
    }

    public List<SearchItemDTO> getItems() {
        return items;
    }

    public void setItems(List<SearchItemDTO> items) {
        this.items = items;
    }

    public PageDto getNextPage() {
        return nextPage;
    }

    public void setNextPage(PageDto nextPage) {
        this.nextPage = nextPage;
    }

    public boolean isHasNextPage() {
        return hasNextPage;
    }

    public void setHasNextPage(boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }
}
