package org.example.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.api.config.PropertiesTestSupport.assertRejected;
import static org.example.api.config.PropertiesTestSupport.environment;

@DisplayName("ExtractionProperties binding")
class ExtractionPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class);

    @EnableConfigurationProperties(ExtractionProperties.class)
    static class Config {}

    @Test
    @DisplayName("Defaults to 2 attempts")
    void defaults() {
        runner.run(context -> assertThat(context.getBean(ExtractionProperties.class).attempts())
                .isEqualTo(2));
    }

    @Test
    @DisplayName("Binds an override from properties")
    void overrides() {
        runner.withPropertyValues("opentube.extraction.attempts=1")
                .run(context -> assertThat(context.getBean(ExtractionProperties.class).attempts())
                        .isEqualTo(1));
    }

    @Test
    @DisplayName("Binds the documented env var")
    void environmentVariable() {
        runner.withInitializer(environment(Map.of("OPENTUBE_EXTRACTION_ATTEMPTS", "5")))
                .run(context -> assertThat(context.getBean(ExtractionProperties.class).attempts())
                        .isEqualTo(5));
    }

    @ParameterizedTest(name = "attempts={0}")
    @ValueSource(ints = {0, -1, 6})
    @DisplayName("Rejects fewer than 1 or more than 5 attempts")
    void rejectsOutOfRange(int attempts) {
        runner.withPropertyValues("opentube.extraction.attempts=" + attempts)
                .run(context -> assertRejected(context, "attempts"));
    }
}
