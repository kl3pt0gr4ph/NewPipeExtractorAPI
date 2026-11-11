package org.example.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A streaming service NewPipeExtractor supports.
 */
@Schema(name = "Service", description = "A streaming service NewPipeExtractor supports")
public record ServiceDTO(
        @Schema(description = "NewPipe's ID for the service", example = "0")
        int serviceId,

        @Schema(description = "Display name", example = "YouTube")
        String serviceName) {}
