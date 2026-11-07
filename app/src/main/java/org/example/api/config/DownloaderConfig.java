package org.example.api.config;

import okhttp3.OkHttpClient;
import org.example.api.downloader.DownloaderImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DownloaderConfig {

    /**
     * The downloader {@link NewPipeInitializer} hands to NewPipeExtractor, built from
     * {@code opentube.http}.
     */
    @Bean
    public DownloaderImpl downloader(HttpClientProperties http) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(http.connectTimeout())
                .readTimeout(http.readTimeout());

        return DownloaderImpl.init(builder, http.userAgent());
    }
}
