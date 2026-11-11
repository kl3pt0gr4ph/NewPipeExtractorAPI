package org.example.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.example.api.config.GlobalExceptionHandler;
import org.example.api.dto.ServiceDTO;
import org.example.api.service.RestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("NewPipeController")
class NewPipeControllerTest {

    private RestService restService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        restService = mock(RestService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new NewPipeController(restService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /services returns the services as a JSON array")
    void getServices_returnsServices() throws Exception {
        when(restService.getServices())
                .thenReturn(List.of(new ServiceDTO(0, "YouTube"), new ServiceDTO(1, "SoundCloud")));

        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceId").value(0))
                .andExpect(jsonPath("$[0].serviceName").value("YouTube"))
                .andExpect(jsonPath("$[1].serviceName").value("SoundCloud"));
    }

    @Test
    @DisplayName("A failure in /services is a 500 ErrorResponse like every other endpoint, without the exception text")
    void getServices_failure_returnsErrorResponse() throws Exception {
        when(restService.getServices())
                .thenThrow(new IllegalStateException("NewPipe services are not initialized or empty"));

        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value(not(containsString("NewPipe"))))
                .andExpect(jsonPath("$.path").value("/api/v1/services"));
    }

    @Test
    @DisplayName("An unknown path is 404 NOT_FOUND")
    void unknownPath_is404() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("An unsupported method is 405 METHOD_NOT_ALLOWED")
    void wrongMethod_is405() throws Exception {
        mockMvc.perform(post("/api/v1/services"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }
}
