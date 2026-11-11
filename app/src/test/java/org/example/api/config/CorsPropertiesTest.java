package org.example.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.api.config.PropertiesTestSupport.assertRejected;
import static org.example.api.config.PropertiesTestSupport.environment;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("CorsProperties binding")
class CorsPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @EnableConfigurationProperties(CorsProperties.class)
    static class Config {}

    @Test
    @DisplayName("Defaults to any origin, GET and OPTIONS")
    void defaults() {
        runner.run(context -> assertThat(context.getBean(CorsProperties.class))
                .isEqualTo(new CorsProperties(List.of("*"), List.of("GET", "OPTIONS"))));
    }

    @Test
    @DisplayName("Binds comma-separated overrides from properties")
    void overrides() {
        runner.withPropertyValues(
                        "opentube.cors.allowed-origins=https://a.example,https://b.example:8443",
                        "opentube.cors.allowed-methods=GET,HEAD,OPTIONS")
                .run(context -> assertThat(context.getBean(CorsProperties.class))
                        .isEqualTo(new CorsProperties(
                                List.of("https://a.example", "https://b.example:8443"),
                                List.of("GET", "HEAD", "OPTIONS"))));
    }

    @Test
    @DisplayName("Binds the documented env vars")
    void environmentVariables() {
        runner.withInitializer(environment(Map.of(
                        "OPENTUBE_CORS_ALLOWEDORIGINS", "http://localhost:5173",
                        "OPENTUBE_CORS_ALLOWEDMETHODS", "GET")))
                .run(context -> assertThat(context.getBean(CorsProperties.class))
                        .isEqualTo(new CorsProperties(List.of("http://localhost:5173"), List.of("GET"))));
    }

    @ParameterizedTest(name = "{0}={1}")
    @CsvSource({
        "allowed-origins, example.com,              allowedOrigins",
        "allowed-origins, https://example.com/path, allowedOrigins",
        "allowed-origins, ftp://example.com,        allowedOrigins",
        "allowed-methods, get,                      allowedMethods",
        "allowed-methods, FETCH,                    allowedMethods",
    })
    @DisplayName("Rejects malformed origins and unknown methods")
    void rejectsInvalid(String property, String value, String field) {
        runner.withPropertyValues("opentube.cors." + property + "=" + value)
                .run(context -> assertRejected(context, field));
    }
}
