package org.example.api.config;

import org.example.api.downloader.DownloaderImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.NewPipe;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NewPipe keeps its settings in static state, so these tests leave it initialised with
 * whatever they last set. Nothing else in the unit tests reads that state: the service
 * tests mock NewPipe's static methods.
 */
@DisplayName("NewPipeInitializer")
class NewPipeInitializerTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DownloaderConfig.class, NewPipeInitializer.class, Config.class);

    @EnableConfigurationProperties({NewPipeProperties.class, HttpClientProperties.class})
    static class Config {}

    @Test
    @DisplayName("Initialises NewPipe with the downloader bean and the configured language and country")
    void initialisesNewPipeFromProperties() {
        runner.withPropertyValues("opentube.newpipe.language=de", "opentube.newpipe.country=AT")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(NewPipe.getDownloader()).isSameAs(context.getBean(DownloaderImpl.class));
                    assertThat(NewPipe.getPreferredLocalization().getLanguageCode()).isEqualTo("de");
                    assertThat(NewPipe.getPreferredLocalization().getCountryCode()).isEqualTo("AT");
                    assertThat(NewPipe.getPreferredContentCountry().getCountryCode()).isEqualTo("AT");
                });
    }

    @Test
    @DisplayName("Defaults to en-GB, as App.main() used to hard-code")
    void defaultsToEnGb() {
        runner.run(context -> {
            assertThat(NewPipe.getPreferredLocalization().getLocalizationCode()).isEqualTo("en-GB");
            assertThat(NewPipe.getPreferredContentCountry().getCountryCode()).isEqualTo("GB");
        });
    }
}
