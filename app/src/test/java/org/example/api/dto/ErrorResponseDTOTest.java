package org.example.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit tests for ErrorResponseDTO.
 */
@DisplayName("ErrorResponseDTO Tests")
class ErrorResponseDTOTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("of() stamps a fresh, parseable, recent timestamp")
    void factoryStampsRecentTimestamp() {
        ErrorResponseDTO dto = ErrorResponseDTO.of(500, "Internal Server Error", null, null, null);

        assertThat(Instant.parse(dto.timestamp()))
                .isCloseTo(Instant.now(), org.assertj.core.api.Assertions.within(5, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("of() wires status/error/message/errorCode/path and leaves detail unset")
    void factoryWiresFields() {
        ErrorResponseDTO dto = ErrorResponseDTO.of(404, "Not Found", "Video not found", "VIDEO_NOT_FOUND", "/api/v1/x");

        assertThat(dto.status()).isEqualTo(404);
        assertThat(dto.error()).isEqualTo("Not Found");
        assertThat(dto.message()).isEqualTo("Video not found");
        assertThat(dto.errorCode()).isEqualTo("VIDEO_NOT_FOUND");
        assertThat(dto.path()).isEqualTo("/api/v1/x");
        assertThat(dto.detail()).isNull();
        assertThat(dto.withDetail("boom").detail()).isEqualTo("boom");
    }

    @Test
    @DisplayName("Omits unset fields (message, errorCode, path, detail) from the serialized JSON")
    void omitsUnsetFieldsFromJson() throws Exception {
        String json =
                objectMapper.writeValueAsString(ErrorResponseDTO.of(500, "Internal Server Error", null, null, null));

        assertThat(json).contains("\"status\":500", "\"error\":\"Internal Server Error\"");
        assertThat(json).doesNotContain("\"message\"", "\"errorCode\"", "\"path\"", "\"detail\"");
    }

    @Test
    @DisplayName("Round-trips every field through serialize/deserialize unchanged")
    void roundTripPreservesAllFields() throws Exception {
        ErrorResponseDTO original =
                ErrorResponseDTO.of(403, "Forbidden", "Access denied", "ACCESS_DENIED", "/api/v1/restricted");

        assertThat(objectMapper.readValue(objectMapper.writeValueAsString(original), ErrorResponseDTO.class))
                .isEqualTo(original);
    }
}
