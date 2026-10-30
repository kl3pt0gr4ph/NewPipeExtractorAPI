package org.example.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.api.dto.search.SearchPageDTO;
import org.example.api.dto.search.SearchResultDTO;
import org.example.api.service.SearchService;
import org.example.api.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.example.api.config.openapi.OpenApiDocs.*;

/**
 * REST controller for handling search-related API requests.
 * All endpoints are prefixed with "/api/v1/search".
 */
@Tag(name = TAG_SEARCH)
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Performs a search and returns the initial results.
     */
    @Operation(summary = "Search",
            description = "First page of results for a query, mixing videos, channels and playlists. "
                    + "Each item's `type` says which. Fetch more with `/search/page`.")
    @GetMapping
    public ResponseEntity<SearchResultDTO> search(
            @Parameter(description = "Search query; surrounding whitespace is trimmed", example = "lofi hip hop")
            @RequestParam(name = "searchString") String searchString,
            @Parameter(description = SORT_FILTER)
            @RequestParam(name = "sortFilter", required = false) String sortFilter,
            @Parameter(description = CONTENT_FILTERS, example = "videos")
            @RequestParam(name = "contentFilters", required = false) String contentFilters
    ) {
        searchString = searchString.trim();
        logger.debug("Search request received for: {}", searchString);

        ValidationUtils.requireNonEmpty(searchString, "searchString");

        List<String> contentFilterList = parseContentFilters(contentFilters);

        SearchResultDTO results = searchService.getSearchInfo(
                searchString,
                contentFilterList,
                sortFilter
        );

        return ResponseEntity.ok(results);
    }

    /**
     * Retrieves a specific page of search results.
     *
     * <p>Both {@code pageUrl} and {@code pageBody} must be passed verbatim from the
     * {@code nextPage} object in the previous response. The body is the Base64-encoded
     * InnerTube continuation token. {@code pageBody} is optional — some continuation
     * pages are URL-only and will have a null body in the previous response.</p>
     *
     * @param searchString   the original search query
     * @param pageUrl        from {@code nextPage.url} in the previous response
     * @param pageId         from {@code nextPage.Id} in the previous response
     * @param sortFilter     optional sort filter (must match the initial request)
     * @param contentFilters optional comma-separated content filters (must match the initial request)
     */
    @Operation(summary = "Next page of search results",
            description = "Repeat the original `searchString` and filters, and pass the previous "
                    + "response's `nextPage` back unmodified.")
    @GetMapping("/page")
    public ResponseEntity<SearchPageDTO> searchPage(
            @Parameter(description = "The original search query", example = "lofi hip hop")
            @RequestParam(name = "searchString") String searchString,
            @Parameter(description = "`nextPage.url`. " + FROM_NEXT_PAGE)
            @RequestParam(name = "pageUrl") String pageUrl,
            @Parameter(description = "`nextPage.id`, when present. " + FROM_NEXT_PAGE)
            @RequestParam(name = "pageId", required = false) String pageId,
            @Parameter(description = SORT_FILTER + " Must match the first request.")
            @RequestParam(name = "sortFilter", required = false) String sortFilter,
            @Parameter(description = CONTENT_FILTERS + " Must match the first request.")
            @RequestParam(name = "contentFilters", required = false) String contentFilters
    ) {
        logger.debug("Search page request for: {}", searchString);

        ValidationUtils.requireNonEmpty(searchString, "searchString");
        ValidationUtils.requireNonEmpty(pageUrl, "pageUrl");

        List<String> contentFilterList = parseContentFilters(contentFilters);

        SearchPageDTO page = searchService.getSearchPage(
                searchString,
                contentFilterList,
                sortFilter,
                pageUrl,
                pageId
        );

        return ResponseEntity.ok(page);
    }

    private List<String> parseContentFilters(String contentFilters) {
        if (contentFilters == null || contentFilters.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(contentFilters.split(","));
    }
}