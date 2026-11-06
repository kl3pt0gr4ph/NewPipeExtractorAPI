package org.example.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for error responses.
 *
 * @param includeDetail add a {@code detail} field to error responses holding the underlying
 *                      exception's class and message, e.g. NewPipeExtractor's own wording.
 *                      For development only: it exposes internals, so leave it off anywhere
 *                      users can see the responses. Env var: {@code OPENTUBE_ERRORS_INCLUDEDETAIL}.
 */
@ConfigurationProperties(prefix = "opentube.errors")
public record ApiErrorProperties(@DefaultValue("false") boolean includeDetail) {}