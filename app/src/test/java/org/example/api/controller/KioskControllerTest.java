package org.example.api.controller;

import tools.jackson.databind.ObjectMapper;
import org.example.api.config.GlobalExceptionHandler;
import org.example.api.dto.kiosk.KioskDTO;
import org.example.api.exception.ExtractionException;
import org.example.api.service.KioskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("KioskController Tests")
class KioskControllerTest {

    private static final int SERVICE_ID = 0;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private KioskService kioskService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        KioskController kioskController = new KioskController(kioskService);
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(kioskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── GET /{kioskId} ───────────────────────────────────────────────────

    @Test
    @DisplayName("Returns 200 with the KioskDTO mapped from the service's KioskInfo")
    void getKioskInfo_returnsMappedDto() throws Exception {
        String kioskId = "trending_gaming";


        KioskInfo kioskInfo = mock(KioskInfo.class);
        when(kioskInfo.getServiceId()).thenReturn(SERVICE_ID);
        when(kioskInfo.getId()).thenReturn(kioskId);
        when(kioskInfo.getName()).thenReturn("Gaming");
        when(kioskInfo.getUrl()).thenReturn("https://youtube.com/gaming/trending");
        when(kioskInfo.getRelatedItems()).thenReturn(List.of());
        when(kioskInfo.getNextPage()).thenReturn(null);

        when(kioskService.getKioskInfo(SERVICE_ID, kioskId)).thenReturn(kioskInfo);

        String expectedJson = objectMapper.writeValueAsString(KioskDTO.from(kioskInfo));

        mockMvc.perform(get("/api/v1/kiosks/{kioskId}", kioskId)) // adjust base path if different
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson, true));

        verify(kioskService).getKioskInfo(SERVICE_ID, kioskId);
    }

    @Test
    @DisplayName("Passes the path variable straight through to the service, unmodified")
    void getKioskInfo_passesKioskIdUnmodified() throws Exception {
        String kioskId = "live";
        KioskInfo kioskInfo = mock(KioskInfo.class);
        when(kioskInfo.getServiceId()).thenReturn(SERVICE_ID);
        when(kioskInfo.getId()).thenReturn(kioskId);
        when(kioskInfo.getName()).thenReturn("Live");
        when(kioskInfo.getUrl()).thenReturn("https://youtube.com/live");
        when(kioskInfo.getRelatedItems()).thenReturn(List.of());
        when(kioskInfo.getNextPage()).thenReturn(null);
        when(kioskService.getKioskInfo(anyInt(), anyString())).thenReturn(kioskInfo);

        mockMvc.perform(get("/api/v1/kiosks/{kioskId}", kioskId))
                .andExpect(status().isOk());

        verify(kioskService).getKioskInfo(SERVICE_ID, "live");
    }

    // ── Exception propagation ─────────────────────────────────────────────

    static Stream<Arguments> extractionFailures() {
        return Stream.of(
                Arguments.of("kiosk not found upstream",
                        new ExtractionException("Kiosk not found")),
                Arguments.of("kiosk temporarily unavailable",
                        new ExtractionException("Kiosk unavailable")),
                Arguments.of("malformed response from extractor",
                        new ExtractionException("Failed to parse kiosk page")));
    }

    @ParameterizedTest(name = "{0} is handled by the extraction-error advice")
    @MethodSource("extractionFailures")
    @DisplayName("Propagates extraction failures to the exception handler")
    void getKioskInfo_whenServiceThrowsExtractionException_isHandledByAdvice(
            String scenario, ExtractionException thrown) throws Exception {
        String kioskId = "not_a_real_kiosk";
        when(kioskService.getKioskInfo(SERVICE_ID, kioskId)).thenThrow(thrown);

        mockMvc.perform(get("/api/v1/kiosks/{kioskId}", kioskId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("EXTRACTION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString(thrown.getMessage())));

        verify(kioskService).getKioskInfo(SERVICE_ID, kioskId);
    }

    @Test
    @DisplayName("Falls back to the generic handler for unexpected runtime exceptions")
    void getKioskInfo_whenServiceThrowsUnexpectedException_fallsBackToGenericHandler() throws Exception {
        String kioskId = "trending_gaming";
        when(kioskService.getKioskInfo(SERVICE_ID, kioskId))
                .thenThrow(new RuntimeException("something exploded"));

        mockMvc.perform(get("/api/v1/kiosks/{kioskId}", kioskId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));

        verify(kioskService).getKioskInfo(SERVICE_ID, kioskId);
    }
}