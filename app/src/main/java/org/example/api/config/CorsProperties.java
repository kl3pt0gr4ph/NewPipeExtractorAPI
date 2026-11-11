package org.example.api.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Cross-origin access to {@code /api/**}. Only matters when a browser calls the API
 * directly; if the client calls it server-side it is not required.
 *
 * @param allowedOrigins origins allowed to call the API, as {@code scheme://host[:port]}, or
 *                       {@code *} for any. Comma-separated as an env var.
 *                       Env var: {@code OPENTUBE_CORS_ALLOWEDORIGINS}.
 * @param allowedMethods HTTP methods allowed cross-origin. Every endpoint is a {@code GET};
 *                       {@code OPTIONS} is the preflight.
 *                       Env var: {@code OPENTUBE_CORS_ALLOWEDMETHODS}.
 */
@Validated
@ConfigurationProperties(prefix = "opentube.cors")
public record CorsProperties(
        @DefaultValue("*") @NotEmpty List<
                        @Pattern(
                                regexp = "\\*|https?://[^/\\s]+/?",
                                message = "must be * or an origin such as https://example.com")
                        String>
                allowedOrigins,

        @DefaultValue({"GET", "OPTIONS"}) @NotEmpty List<
                        @Pattern(
                                regexp = "\\*|GET|HEAD|POST|PUT|PATCH|DELETE|OPTIONS",
                                message = "must be * or an upper-case HTTP method")
                        String>
                allowedMethods) {}
