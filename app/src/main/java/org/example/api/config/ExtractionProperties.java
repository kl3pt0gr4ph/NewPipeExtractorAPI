package org.example.api.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * How stream extraction behaves.
 *
 * @param attempts how many times to extract a video, in total, when YouTube returns a valid
 *                 video with no adaptive streams. {@code 1} turns retrying off. Capped at 5,
 *                 since each attempt is a full request to YouTube.
 *                 Env var: {@code OPENTUBE_EXTRACTION_ATTEMPTS}.
 */
@Validated
@ConfigurationProperties(prefix = "opentube.extraction")
public record ExtractionProperties(
        @DefaultValue("2") @Min(1) @Max(5) int attempts
) {}
