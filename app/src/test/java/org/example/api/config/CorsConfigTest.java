package org.example.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the {@link CorsConfiguration} that {@link CorsConfig} registers, through the same
 * {@code checkOrigin} / {@code checkHttpMethod} calls Spring makes for a real request.
 */
@DisplayName("CorsConfig")
class CorsConfigTest {

    private static final CorsProperties DEFAULTS =
            new CorsProperties(List.of("*"), List.of("GET", "OPTIONS"));

    @Test
    @DisplayName("Maps /api/** only")
    void mapsApiPathOnly() {
        assertThat(register(DEFAULTS)).containsOnlyKeys("/api/**");
    }

    @Test
    @DisplayName("Defaults allow any origin, with GET and OPTIONS only")
    void defaults() {
        CorsConfiguration config = register(DEFAULTS).get("/api/**");

        assertThat(config.checkOrigin("https://anywhere.example")).isEqualTo("*");
        assertThat(config.checkHttpMethod(HttpMethod.GET)).isNotNull();
        assertThat(config.checkHttpMethod(HttpMethod.OPTIONS)).isNotNull();
        assertThat(config.checkHttpMethod(HttpMethod.POST)).isNull();
        assertThat(config.checkHttpMethod(HttpMethod.PUT)).isNull();
        assertThat(config.checkHttpMethod(HttpMethod.DELETE)).isNull();
    }

    @Test
    @DisplayName("Uses the configured origins and methods")
    void configuredOriginsAndMethods() {
        CorsConfiguration config = register(new CorsProperties(
                List.of("https://opentube.example", "http://localhost:5173"),
                List.of("GET", "HEAD", "OPTIONS"))).get("/api/**");

        assertThat(config.checkOrigin("https://opentube.example")).isEqualTo("https://opentube.example");
        assertThat(config.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
        assertThat(config.checkOrigin("https://other.example")).isNull();
        assertThat(config.checkHttpMethod(HttpMethod.HEAD)).isNotNull();
        assertThat(config.checkHttpMethod(HttpMethod.POST)).isNull();
    }

    @Test
    @DisplayName("Allows any request header and never credentials")
    void headersAndCredentials() {
        CorsConfiguration config = register(DEFAULTS).get("/api/**");

        assertThat(config.getAllowedHeaders()).containsExactly("*");
        assertThat(config.getAllowCredentials()).isFalse();
    }

    private static Map<String, CorsConfiguration> register(CorsProperties properties) {
        ExposedCorsRegistry registry = new ExposedCorsRegistry();
        new CorsConfig(properties).addCorsMappings(registry);
        return registry.getCorsConfigurations();
    }

    /** Makes the registry's result visible; Spring keeps the getter protected. */
    private static final class ExposedCorsRegistry extends CorsRegistry {
        @Override
        public Map<String, CorsConfiguration> getCorsConfigurations() {
            return super.getCorsConfigurations();
        }
    }
}
