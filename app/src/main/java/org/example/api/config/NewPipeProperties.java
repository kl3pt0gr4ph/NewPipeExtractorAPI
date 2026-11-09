package org.example.api.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * The language and region NewPipeExtractor asks YouTube for.
 *
 * @param language ISO 639 language code, lower case, e.g. {@code en} or {@code de}. Sets the
 *                 language of titles, descriptions, dates and counts YouTube returns.
 *                 Env var: {@code OPENTUBE_NEWPIPE_LANGUAGE}.
 * @param country  ISO 3166 country code, upper case, e.g. {@code GB} or {@code US}. Sets the
 *                 region for kiosks and search ranking, and the regional variant of the
 *                 language. Env var: {@code OPENTUBE_NEWPIPE_COUNTRY}.
 */
@Validated
@ConfigurationProperties(prefix = "opentube.newpipe")
public record NewPipeProperties(
        @DefaultValue("en")
        @NotNull @Pattern(regexp = "[a-z]{2,3}", message = "must be a lower-case ISO 639 code, e.g. en")
        String language,

        @DefaultValue("GB")
        @NotNull @Pattern(regexp = "[A-Z]{2}", message = "must be an upper-case ISO 3166 code, e.g. GB")
        String country
) {}

