package org.example.api.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * The HTTP client NewPipeExtractor uses to reach YouTube.
 *
 * @param connectTimeout how long to wait for a connection to YouTube.
 *                       Env var: {@code OPENTUBE_HTTP_CONNECTTIMEOUT}.
 * @param readTimeout    how long to wait for data once connected.
 *                       Env var: {@code OPENTUBE_HTTP_READTIMEOUT}.
 * @param userAgent      sent with every request unless NewPipeExtractor sets its own, which it
 *                       does for the mobile InnerTube clients.
 *                       Env var: {@code OPENTUBE_HTTP_USERAGENT}.
 */
@Validated
@ConfigurationProperties(prefix = "opentube.http")
public record HttpClientProperties(
        @DefaultValue("30s") @NotNull @DurationMin(millis = 1)
        Duration connectTimeout,

        @DefaultValue("30s") @NotNull @DurationMin(millis = 1)
        Duration readTimeout,

        @DefaultValue(HttpClientProperties.DEFAULT_USER_AGENT) @NotBlank String userAgent) {
    /**
     * Matches NewPipeExtractor's own test downloader: the latest Firefox ESR. YouTube can
     * serve old browsers a different page layout, which NewPipeExtractor then can't parse,
     * so keep this current when bumping NewPipeExtractor.
     */
    public static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0";
}
