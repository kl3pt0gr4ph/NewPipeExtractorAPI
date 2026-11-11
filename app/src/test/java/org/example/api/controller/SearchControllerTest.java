package org.example.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.example.api.config.GlobalExceptionHandler;
import org.example.api.dto.search.SearchItemDTO;
import org.example.api.dto.search.SearchPageDTO;
import org.example.api.dto.search.SearchResultDTO;
import org.example.api.exception.ExtractionException;
import org.example.api.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for SearchController.
 */
@DisplayName("SearchController Tests")
class SearchControllerTest {

    private static final String PAGE_URL = "https://www.youtube.com/youtubei/v1/search?prettyPrint=false";
    private static final String PAGE_ID = "eyJjb250aW51YXRpb24iOiJ0b2tlbiJ9";

    private MockMvc mockMvc;

    @Mock
    private SearchService searchService;

    @InjectMocks
    private SearchController searchController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(searchController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private SearchResultDTO searchResultDTO(String searchString, int itemCount) {
        SearchResultDTO dto = new SearchResultDTO();
        dto.setSearchString(searchString);
        dto.setItems(Collections.nCopies(itemCount, null));
        dto.setHasNextPage(true);
        dto.setNextPage(new SearchResultDTO.PageDto(PAGE_URL, PAGE_ID));
        return dto;
    }

    private SearchPageDTO searchPageDTO(int itemCount) {
        List<SearchItemDTO> items =
                Collections.nCopies(itemCount, SearchItemDTO.builder().build());
        return new SearchPageDTO(items, null, false, itemCount);
    }

    // ── GET /api/v1/search ───────────────────────────────────────────────

    @Test
    @DisplayName("Returns 200 with results, trimming the search string and passing the sort filter")
    void search_trimsSearchStringAndPassesSortFilter() throws Exception {
        when(searchService.getSearchInfo(eq("java tutorial"), anyList(), eq("upload_date")))
                .thenReturn(searchResultDTO("java tutorial", 5));

        mockMvc.perform(get("/api/v1/search")
                        .param("searchString", "  java tutorial  ")
                        .param("sortFilter", "upload_date")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.searchString").value("java tutorial"))
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.nextPage.url").isNotEmpty())
                .andExpect(jsonPath("$.nextPage.id").isNotEmpty());

        verify(searchService).getSearchInfo(eq("java tutorial"), eq(Collections.emptyList()), eq("upload_date"));
    }

    static Stream<Arguments> contentFilterCases() {
        return Stream.of(
                Arguments.of("absent", null, Collections.emptyList()),
                Arguments.of("empty", "", Collections.emptyList()),
                Arguments.of("single filter", "videos", List.of("videos")),
                Arguments.of("comma-separated filters", "video,channel", List.of("video", "channel")));
    }

    @ParameterizedTest(name = "Parses {0} content filters")
    @MethodSource("contentFilterCases")
    @DisplayName("Parses the contentFilters parameter into the list passed to the service")
    void search_parsesContentFilters(String name, String contentFilters, List<String> expectedList) throws Exception {
        when(searchService.getSearchInfo(eq("test"), eq(expectedList), isNull()))
                .thenReturn(searchResultDTO("test", 1));

        MockHttpServletRequestBuilder request =
                get("/api/v1/search").param("searchString", "test").contentType(MediaType.APPLICATION_JSON);
        if (contentFilters != null) {
            request.param("contentFilters", contentFilters);
        }

        mockMvc.perform(request).andExpect(status().isOk());

        verify(searchService).getSearchInfo(eq("test"), eq(expectedList), isNull());
    }

    @ParameterizedTest(name = "Rejects searchString \"{0}\"")
    @ValueSource(strings = {"", "   "})
    @DisplayName("Rejects empty and whitespace-only search strings with 400")
    void search_rejectsBlankSearchString(String searchString) throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("searchString", searchString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/v1/search/page ──────────────────────────────────────────

    @Test
    @DisplayName("Returns 200 with the page, passing all parameters verbatim to the service")
    void searchPage_passesAllParametersVerbatim() throws Exception {
        when(searchService.getSearchPage(eq("java"), anyList(), isNull(), eq(PAGE_URL), eq(PAGE_ID)))
                .thenReturn(searchPageDTO(3));

        mockMvc.perform(get("/api/v1/search/page")
                        .param("searchString", "java")
                        .param("pageUrl", PAGE_URL)
                        .param("pageId", PAGE_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.itemCount").value(3));

        verify(searchService)
                .getSearchPage(eq("java"), eq(Collections.emptyList()), isNull(), eq(PAGE_URL), eq(PAGE_ID));
    }

    @Test
    @DisplayName("Treats pageId as optional, passing null to the service (URL-only continuation pages)")
    void searchPage_treatsPageIdAsOptional() throws Exception {
        when(searchService.getSearchPage(eq("test"), anyList(), isNull(), eq(PAGE_URL), isNull()))
                .thenReturn(searchPageDTO(2));

        mockMvc.perform(get("/api/v1/search/page")
                        .param("searchString", "test")
                        .param("pageUrl", PAGE_URL)
                        // no pageId param
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(searchService).getSearchPage(eq("test"), anyList(), isNull(), eq(PAGE_URL), isNull());
    }

    // ── Exception propagation (single advice-wiring test) ────────────────

    @Test
    @DisplayName("Propagates service failures to the exception handler")
    void propagatesFailuresToAdvice() throws Exception {
        when(searchService.getSearchInfo(anyString(), anyList(), isNull()))
                .thenThrow(new ExtractionException("NewPipe failure"));

        mockMvc.perform(get("/api/v1/search").param("searchString", "test").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("EXTRACTION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("NewPipe failure")));
    }

    // ── Required parameter binding (both endpoints) ──────────────────────

    static Stream<Arguments> requestsMissingARequiredParam() {
        return Stream.of(
                Arguments.of("/search without searchString", get("/api/v1/search")),
                Arguments.of(
                        "/search/page without searchString",
                        get("/api/v1/search/page").param("pageUrl", PAGE_URL).param("pageId", PAGE_ID)),
                Arguments.of(
                        "/search/page without pageUrl",
                        get("/api/v1/search/page").param("searchString", "test").param("pageId", PAGE_ID)));
    }

    @ParameterizedTest(name = "{0} returns 400")
    @MethodSource("requestsMissingARequiredParam")
    @DisplayName("Missing required parameters return 400")
    void missingRequiredParam_returnsBadRequest(String name, MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }
}
