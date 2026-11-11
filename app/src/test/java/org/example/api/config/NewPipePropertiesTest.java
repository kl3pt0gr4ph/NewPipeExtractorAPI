package org.example.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.api.config.PropertiesTestSupport.assertRejected;
import static org.example.api.config.PropertiesTestSupport.environment;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("NewPipeProperties binding")
class NewPipePropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @EnableConfigurationProperties(NewPipeProperties.class)
    static class Config {}

    @Test
    @DisplayName("Defaults to en / GB")
    void defaults() {
        runner.run(context ->
                assertThat(context.getBean(NewPipeProperties.class)).isEqualTo(new NewPipeProperties("en", "GB")));
    }

    @Test
    @DisplayName("Binds overrides from properties")
    void overrides() {
        runner.withPropertyValues("opentube.newpipe.language=de", "opentube.newpipe.country=AT")
                .run(context -> assertThat(context.getBean(NewPipeProperties.class))
                        .isEqualTo(new NewPipeProperties("de", "AT")));
    }

    @Test
    @DisplayName("Binds the documented env vars")
    void environmentVariables() {
        runner.withInitializer(environment(Map.of(
                        "OPENTUBE_NEWPIPE_LANGUAGE", "fr",
                        "OPENTUBE_NEWPIPE_COUNTRY", "CA")))
                .run(context -> assertThat(context.getBean(NewPipeProperties.class))
                        .isEqualTo(new NewPipeProperties("fr", "CA")));
    }

    @ParameterizedTest(name = "{0}={1}")
    @CsvSource({
        "language, EN",
        "language, english",
        "language, e",
        "country,  gb",
        "country,  GBR",
        "country,  G1",
    })
    @DisplayName("Rejects invalid codes")
    void rejectsInvalid(String field, String value) {
        runner.withPropertyValues("opentube.newpipe." + field + "=" + value)
                .run(context -> assertRejected(context, field));
    }
}
