package org.example.api.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Ticker;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletionException;
import org.example.api.exception.ExtractionException;
import org.jspecify.annotations.NonNull;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Caches {@link  StreamInfo} per video URL for as long as the signed stream URLs
 * it contains remain valid
 */
@Component
public class StreamInfoCache {
    private final Cache<String, StreamInfo> cache;

    @Autowired
    public StreamInfoCache(StreamInfoCacheProperties properties) {
        this(properties, Clock.systemUTC(), Ticker.systemTicker());
    }

    /**
     * Visible for testing: lets the wall clock (used to derive the TTL from the
     * {@code expire} parameter) and Caffeine's elapsed-time source be driven
     * independently.
     */
    public StreamInfoCache(StreamInfoCacheProperties properties, Clock clock, Ticker ticker) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.maxEntries())
                .ticker(ticker)
                .expireAfter(new Expiry<String, StreamInfo>() {
                    @Override
                    public long expireAfterCreate(@NonNull String key, @NonNull StreamInfo value, long currentTime) {
                        return ttl(value, properties, clock.instant()).toNanos();
                    }

                    @Override
                    public long expireAfterUpdate(
                            @NonNull String key, @NonNull StreamInfo value, long currentTime, long currentDuration) {
                        return ttl(value, properties, clock.instant()).toNanos();
                    }

                    @Override
                    public long expireAfterRead(
                            @NonNull String key, @NonNull StreamInfo value, long currentTime, long currentDuration) {
                        //                        return ttl(value, properties, clock.instant()).toNanos();
                        return currentDuration;
                    }
                })
                .recordStats()
                .build();
    }

    /**
     * Computes how long an entry may live. Pure: takes {@code now} rather than
     * reading the clock, so it can be exercised directly in tests.
     */
    static Duration ttl(StreamInfo info, StreamInfoCacheProperties props, Instant now) {
        boolean degraded =
                info.getVideoOnlyStreams().isEmpty() && info.getAudioStreams().isEmpty();
        if (degraded) {
            // Don't pin a bad extraction for the lifetime of the signature.
            return props.degradedTtl();
        }

        Duration ttl = StreamExpiry.earliest(info)
                .map(expiry -> Duration.between(now, expiry).minus(props.safetyMargin()))
                .orElse(props.fallbackTtl());

        if (ttl.compareTo(props.maxTtl()) > 0) {
            ttl = props.maxTtl();
        }
        return ttl.isNegative() ? Duration.ZERO : ttl;
    }

    public StreamInfo get(String url, ExtractionLoader loader) throws ExtractionException {
        try {
            return cache.get(url, key -> {
                try {
                    return loader.load(key);
                } catch (ExtractionException e) {
                    throw new CompletionException(e);
                }
            });
        } catch (CompletionException e) {
            if (e.getCause() instanceof ExtractionException ex) throw ex;
            throw e;
        }
    }

    public void invalidate(String url) {
        cache.invalidate(url);
    }

    public CacheStats stats() {
        return cache.stats();
    }

    @FunctionalInterface
    public interface ExtractionLoader {
        StreamInfo load(String url) throws ExtractionException;
    }
}
