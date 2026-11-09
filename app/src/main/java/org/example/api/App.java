package org.example.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point. NewPipeExtractor is initialised by
 * {@link org.example.api.config.NewPipeInitializer} as the context starts.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}

