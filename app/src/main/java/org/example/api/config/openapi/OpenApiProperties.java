package org.example.api.config.openapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the generated OpenAPI document.
 *
 * @param serverUrl public base URL to advertise in the spec's {@code servers} list,
 *                  e.g. when the API sits behind a reverse proxy. When unset, springdoc
 *                  uses the URL each request for the spec arrived on.
 *                  Env var: {@code OPENTUBE_OPENAPI_SERVERURL}.
 */
@ConfigurationProperties(prefix = "opentube.openapi")
public record OpenApiProperties(String serverUrl) {}