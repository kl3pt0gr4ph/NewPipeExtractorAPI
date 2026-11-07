package org.example.api.config;

import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helpers for the {@code *PropertiesTest} classes, which bind each
 * {@code @ConfigurationProperties} record in an {@code ApplicationContextRunner}.
 */
final class PropertiesTestSupport {

    private PropertiesTestSupport() {}

    /**
     * Replaces the system environment with {@code variables}, so a test can check that the
     * env var names in the README bind. Spring only applies its env var mapping
     * ({@code OPENTUBE_HTTP_READTIMEOUT} → {@code opentube.http.read-timeout}) to a
     * source with this name, which a plain {@code withPropertyValues} wouldn't exercise.
     */
    static ApplicationContextInitializer<ConfigurableApplicationContext> environment(
            Map<String, Object> variables) {
        return context -> context.getEnvironment().getPropertySources().addFirst(
                new SystemEnvironmentPropertySource(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, variables));
    }

    /**
     * Asserts the context failed to start because {@code field} broke a constraint. A
     * list element's error is reported on {@code field[index]}, which also matches.
     */
    static void assertRejected(AssertableApplicationContext context, String field) {
        assertThat(context).hasFailed();
        assertThat(context.getStartupFailure())
                .rootCause()
                .isInstanceOf(BindValidationException.class)
                .hasMessageContaining("field '" + field);
    }
}
