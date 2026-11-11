package org.example.api.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "opentube.cache.streaminfo")
public record StreamInfoCacheProperties(
        @DefaultValue("300") int maxEntries,
        @DefaultValue("10m") Duration safetyMargin,
        @DefaultValue("5m") Duration fallbackTtl,
        @DefaultValue("30s") Duration degradedTtl,
        @DefaultValue("6h") Duration maxTtl) {}
