package org.example.api.service;

import org.example.api.dto.search.SearchPageDTO;
import org.example.api.dto.search.SearchResultDTO;
import org.example.api.dto.search.SearchItemDTO;
import org.example.api.exception.ExtractionException;
import org.example.api.exception.NewPipeExceptionMapper;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for performing and managing search operations across
 * different streaming services using NewPipe extractor.
 */
@Service
public class SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private static final int YOUTUBE_SERVICE_ID = 0;

    /**
     * Performs an initial search and returns deduplicated results.
     */
    public SearchResultDTO getSearchInfo(
            String searchString,
            List<String> contentFilters,
            String sortFilter
    ) throws ExtractionException {
        try {
            logger.info("Performing search for: {}", searchString);

            StreamingService service = NewPipe.getService(YOUTUBE_SERVICE_ID);

            SearchExtractor extractor = service.getSearchExtractor(
                    service.getSearchQHFactory().fromQuery(searchString, contentFilters, sortFilter)
            );
            extractor.fetchPage();

            SearchInfo info = SearchInfo.getInfo(extractor);
            SearchResultDTO dto = SearchResultDTO.from(info);

            List<SearchItemDTO> uniqueItems = deduplicateByUrl(dto.getItems());
            dto.setItems(uniqueItems);

            logger.info("Search completed with {} unique results(s)", uniqueItems.size());

            return dto;
        } catch (Exception e) {
            throw NewPipeExceptionMapper.translate(e);
        }
    }

    /**
     * Retrieves the next page of search results.
     *
     * <p>{@link SearchExtractor} is obtained for the original query, initialized with
     * {@code fetchPage()} to establish the InnerTube session state, and then
     * {@code getPage(pageInstance)} is called with the reconstructed {@link Page}.
     * Using {@code SearchInfo.getMoreItems()} without this initialization step skips
     * the extractor setup and results in empty pages.</p>
     *
     * @param searchString   the original query string
     * @param contentFilters list of filters
     * @param sortFilter     sort method
     * @param pageUrl        from {@code nextPage.url} in the previous response
     * @param pageId         from {@code nextPage.Id} in the previous response
     */
    public SearchPageDTO getSearchPage(
            String searchString,
            List<String> contentFilters,
            String sortFilter,
            String pageUrl,
            String pageId
    ) throws ExtractionException {
        try {
            logger.debug("Retrieving search page for: {}", searchString);

            StreamingService service = NewPipe.getService(YOUTUBE_SERVICE_ID);

            SearchExtractor extractor = service.getSearchExtractor(
                    service.getSearchQHFactory().fromQuery(searchString, contentFilters, sortFilter)
            );
            extractor.fetchPage();

            Page pageInstance = new Page(pageUrl, pageId);

            ListExtractor.InfoItemsPage<?> page = extractor.getPage(pageInstance);

            SearchPageDTO dto = SearchPageDTO.from(page);

            List<SearchItemDTO> uniqueItems = deduplicateByUrl(dto.getItems());
            dto.setItems(uniqueItems);
            dto.setItemCount(uniqueItems.size());

            logger.info("Retrieved page with {} unique result(s)", uniqueItems.size());

            return dto;
        } catch (Exception e) {
            throw NewPipeExceptionMapper.translate(e);
        }
    }

    private List<SearchItemDTO> deduplicateByUrl(List<SearchItemDTO> items) {
        return new ArrayList<>(items.stream()
                .collect(Collectors.toMap(
                        SearchItemDTO::getUrl,
                        item -> item,
                        (existing, _) -> existing,
                        LinkedHashMap::new
                ))
                .values());
    }
}