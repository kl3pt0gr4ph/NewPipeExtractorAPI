package org.example.api.integration.search;

import org.example.api.dto.search.SearchItemDTO;
import org.example.api.dto.search.SearchPageDTO;
import org.example.api.dto.search.SearchResultDTO;
import org.example.api.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for SearchController
 */
@AutoConfigureTestRestTemplate
class SearchControllerIT extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:%d/api/v1/search".formatted(port);
    }

    private ResponseEntity<SearchResultDTO> search(String query) {
        String url = "%s?searchString=%s".formatted(baseUrl(), URLEncoder.encode(query, StandardCharsets.UTF_8));
        return restTemplate.exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<SearchResultDTO>() { });
    }

    // ── Happy path ───────────────────────────────────────────────────────

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("A valid query returns well-formed, non-empty results with every item minimally populated")
    void search_returnsWellFormedResults() {
        ResponseEntity<SearchResultDTO> response = search("java tutorial");
        SearchResultDTO result = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result).isNotNull();
        assertThat(result.getSearchString()).isEqualTo("java tutorial");
        assertThat(result.getItems()).isNotEmpty();
        assertThat(result.getItems()).allSatisfy(item -> {
            assertThat(item.getName()).isNotBlank();
            assertThat(item.getUrl()).isNotBlank().contains("youtube.com");
            assertThat(item.getType()).isIn("stream", "channel", "playlist");
        });
    }

    @Test
    @DisplayName("Results are deduplicated by URL, matching the service's dedup contract")
    void search_deduplicatesResultsByUrl() {
        SearchResultDTO result = search("popular song").getBody();

        assertThat(result).isNotNull();
        List<SearchItemDTO> items = result.getItems();
        long uniqueUrls = items.stream().map(SearchItemDTO::getUrl).distinct().count();

        assertThat(uniqueUrls).isEqualTo(items.size());
    }

    // ── Validation ───────────────────────────────────────────────────────

    @ParameterizedTest(name = "searchString=\"{0}\" returns 400")
    @ValueSource(strings = {"", "   "})
    @DisplayName("A missing or blank searchString returns 400")
    void search_rejectsBlankSearchString(String searchString) {
        String url = "%s?searchString=%s".formatted(baseUrl(), searchString);
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("A request with no searchString parameter at all returns 400")
    void search_withoutSearchStringParam_returnsBadRequest() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Input shapes the real service must not choke on ─────────────────

    @ParameterizedTest(name = "Handles: {0}")
    @ValueSource(strings = {
            "a",                       // single character
            "12345",                   // numbers only
            "日本語",                    // Unicode (Japanese)
            "music \uD83C\uDFB5",      // emoji
            "\"java tutorial\"",       // embedded quotes
            "java tutorial tutorial tutorial tutorial tutorial tutorial tutorial tutorial tutorial tutorial"
            // long query (~100 chars)
    })
    @DisplayName("Unusual but valid query text returns 200 without the service choking")
    void search_handlesUnusualQueryText(String query) {
        ResponseEntity<SearchResultDTO> response = search(query);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    // ── Filters and sort: reach the real service without erroring ───────

    @Test
    @DisplayName("Content filters and a sort filter reach the real service and return 200 (NewPipe/YouTube own the actual filtering/sorting behaviour, not this codebase)")
    void search_withContentFiltersAndSort_reachesRealServiceSuccessfully() {
        String url = "%s?searchString=programming&contentFilters=videos&sortFilter=upload_date"
                .formatted(baseUrl());
        ResponseEntity<SearchResultDTO> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<SearchResultDTO>() { });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("An invalid sort filter value does not crash the request")
    void search_withInvalidSortFilter_doesNotCrash() {
        String url = "%s?searchString=test&sortFilter=invalid_sort".formatted(baseUrl());
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.BAD_REQUEST);
    }

    // ── Pagination (previously uncovered end-to-end) ─────────────────────

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Following a real nextPage cursor returns further, non-empty results")
    void searchPage_followsRealContinuation() {
        SearchResultDTO first = search("tutorial").getBody();
        assertThat(first).isNotNull();

        Assumptions.assumeTrue(first.isHasNextPage() && first.getNextPage() != null,
                "No further pages were returned for this query — nothing to test");

        String pageUrl = "%s/page?searchString=%s&pageUrl=%s&pageId=%s".formatted(
                baseUrl(),
                URLEncoder.encode("tutorial", StandardCharsets.UTF_8),
                URLEncoder.encode(first.getNextPage().url(), StandardCharsets.UTF_8),
                first.getNextPage().id() == null ? "" : URLEncoder.encode(first.getNextPage().id(), StandardCharsets.UTF_8));

        ResponseEntity<SearchPageDTO> pageResponse = restTemplate.exchange(
                pageUrl, HttpMethod.GET, null, new ParameterizedTypeReference<SearchPageDTO>() { });

        assertThat(pageResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        SearchPageDTO page = pageResponse.getBody();
        assertThat(page).isNotNull();
        assertThat(page.getItems()).isNotEmpty();
        assertThat(page.getItemCount()).isEqualTo(page.getItems().size());
    }

    // ── Concurrency smoke test ───────────────────────────────────────────

    @Test
    @DisplayName("Concurrent search requests are all handled successfully")
    void search_concurrentRequests_areAllHandledSuccessfully() throws InterruptedException {
        int threadCount = 5;
        AtomicInteger successCount = new AtomicInteger(0);
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                if (search("test").getStatusCode() == HttpStatus.OK) {
                    successCount.incrementAndGet();
                }
            });
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join(10_000);
        }

        assertThat(successCount.get()).isEqualTo(threadCount);
    }
}