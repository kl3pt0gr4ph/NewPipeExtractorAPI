package org.example.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.api.config.PropertiesTestSupport.assertRejected;
import static org.example.api.config.PropertiesTestSupport.environment;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("HttpClientProperties binding")
class HttpClientPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @EnableConfigurationProperties(HttpClientProperties.class)
    static class Config {}

    @Test
    @DisplayName("Defaults to 30s timeouts and the Firefox ESR User-Agent")
    void defaults() {
        runner.run(context -> assertThat(context.getBean(HttpClientProperties.class))
                .isEqualTo(new HttpClientProperties(
                        Duration.ofSeconds(30), Duration.ofSeconds(30), HttpClientProperties.DEFAULT_USER_AGENT)));
    }

    @Test
    @DisplayName("Binds overrides from properties")
    void overrides() {
        runner.withPropertyValues(
                        "opentube.http.connect-timeout=5s",
                        "opentube.http.read-timeout=1m",
                        "opentube.http.user-agent=Test/1.0")
                .run(context -> assertThat(context.getBean(HttpClientProperties.class))
                        .isEqualTo(new HttpClientProperties(Duration.ofSeconds(5), Duration.ofMinutes(1), "Test/1.0")));
    }

    @Test
    @DisplayName("Binds the documented env vars")
    void environmentVariables() {
        runner.withInitializer(environment(Map.of(
                        "OPENTUBE_HTTP_CONNECTTIMEOUT", "2s",
                        "OPENTUBE_HTTP_READTIMEOUT", "500ms",
                        "OPENTUBE_HTTP_USERAGENT", "Env/1.0")))
                .run(context -> assertThat(context.getBean(HttpClientProperties.class))
                        .isEqualTo(new HttpClientProperties(Duration.ofSeconds(2), Duration.ofMillis(500), "Env/1.0")));
    }

    @ParameterizedTest(name = "{0}={1}")
    @CsvSource({
        "connect-timeout, 0s,  connectTimeout",
        "connect-timeout, -5s, connectTimeout",
        "read-timeout,    0s,  readTimeout",
        "user-agent,      ' ', userAgent",
    })
    @DisplayName("Rejects zero or negative timeouts and a blank User-Agent")
    void rejectsInvalid(String property, String value, String field) {
        runner.withPropertyValues("opentube.http." + property + "=" + value)
                .run(context -> assertRejected(context, field));
    }
}
