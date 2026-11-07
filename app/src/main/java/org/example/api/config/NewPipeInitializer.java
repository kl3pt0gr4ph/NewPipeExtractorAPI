package org.example.api.config;

import org.example.api.downloader.DownloaderImpl;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Hands NewPipeExtractor its downloader, localization and content country.
 *
 * <p>NewPipeExtractor keeps these in static state, and the services reach it through
 * {@link NewPipe}'s static methods.</p>
 */
@Component
@Lazy(false)
public class NewPipeInitializer implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(NewPipeInitializer.class);

    private final DownloaderImpl downloader;
    private final NewPipeProperties properties;

    public NewPipeInitializer(DownloaderImpl downloader, NewPipeProperties properties) {
        this.downloader = downloader;
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        NewPipe.init(downloader,
                new Localization(properties.language(), properties.country()),
                new ContentCountry(properties.country()));
        logger.info("NewPipe initialised: language={}, country={}",
                properties.language(), properties.country());
    }
}

