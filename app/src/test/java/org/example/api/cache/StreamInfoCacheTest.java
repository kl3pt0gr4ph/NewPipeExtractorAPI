package org.example.api.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.example.api.exception.ExtractionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.schabi.newpipe.extractor.stream.StreamInfo;

class StreamInfoCacheTest {

    private static final Instant NOW = Instant.parse("2026-08-09T12:00:00Z");
    private static final String URL = StreamFixtures.WATCH_URL;

    private static final StreamInfoCacheProperties PROPS = new StreamInfoCacheProperties(
            300,
            Duration.ofMinutes(10), // safetyMargin
            Duration.ofMinutes(5), // fallbackTtl
            Duration.ofSeconds(30), // degradedTtl
            Duration.ofHours(6)); // maxTtl

    private MutableClock clock;
    private MutableTicker ticker;
    private StreamInfoCache cache;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(NOW);
        ticker = new MutableTicker();
        cache = new StreamInfoCache(PROPS, clock, ticker);
    }

    /** Advances both time sources together, as real elapsed time would. */
    private void advance(Duration duration) {
        clock.advance(duration);
        ticker.advance(duration);
    }

    static Stream<Arguments> ttlCases() {
        return Stream.of(
                Arguments.of(
                        "safety margin subtracted from signed expiry",
                        StreamFixtures.healthy(NOW.plus(Duration.ofHours(1))),
                        Duration.ofMinutes(50)),
                Arguments.of(
                        "no expire parameter falls back", StreamFixtures.healthyWithoutExpiry(), PROPS.fallbackTtl()),
                Arguments.of(
                        "degraded extraction gets the short TTL",
                        StreamFixtures.degraded(NOW.plus(Duration.ofHours(1))),
                        PROPS.degradedTtl()),
                Arguments.of(
                        "degraded wins over a long signature",
                        StreamFixtures.degraded(NOW.plus(Duration.ofHours(5))),
                        PROPS.degradedTtl()),
                Arguments.of(
                        "already expired yields zero",
                        StreamFixtures.healthy(NOW.minus(Duration.ofHours(1))),
                        Duration.ZERO),
                Arguments.of(
                        "expiry inside the margin yields zero",
                        StreamFixtures.healthy(NOW.plus(Duration.ofMinutes(5))),
                        Duration.ZERO),
                Arguments.of(
                        "distant expiry is clamped to maxTtl",
                        StreamFixtures.healthy(NOW.plus(Duration.ofHours(24))),
                        PROPS.maxTtl()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ttlCases")
    @DisplayName("TTL computation")
    void computesTtl(String name, StreamInfo info, Duration expected) {
        assertThat(StreamInfoCache.ttl(info, PROPS, NOW)).isEqualTo(expected);
    }

    @Nested
    @DisplayName("caching behaviour")
    class CachingBehaviour {

        @Test
        @DisplayName("two gets for the same key invoke the loader once")
        void loaderInvokedOncePerKey() throws Exception {
            StreamInfo info = StreamFixtures.healthy(NOW.plus(Duration.ofHours(1)));
            AtomicInteger calls = new AtomicInteger();

            StreamInfo first = cache.get(URL, url -> {
                calls.incrementAndGet();
                return info;
            });
            StreamInfo second = cache.get(URL, url -> {
                calls.incrementAndGet();
                return info;
            });

            assertThat(calls).hasValue(1);
            assertThat(first).isSameAs(info);
            assertThat(second).isSameAs(info);
        }

        @Test
        @DisplayName("loader failure propagates as ExtractionException and is not cached")
        void failuresArePropagatedAndNotCached() throws Exception {
            AtomicInteger calls = new AtomicInteger();
            StreamInfo info = StreamFixtures.healthy(NOW.plus(Duration.ofHours(1)));

            assertThatThrownBy(() -> cache.get(URL, url -> {
                        calls.incrementAndGet();
                        throw new ExtractionException("upstream blew up");
                    }))
                    .isInstanceOf(ExtractionException.class)
                    .hasMessage("upstream blew up");

            StreamInfo recovered = cache.get(URL, url -> {
                calls.incrementAndGet();
                return info;
            });

            assertThat(calls).hasValue(2);
            assertThat(recovered).isSameAs(info);
        }

        @Test
        @DisplayName("entry is gone once the TTL has elapsed")
        void entryExpiresAfterTtl() throws Exception {
            // expire one hour out, minus the ten minute margin -> 50 minute TTL
            StreamInfo info = StreamFixtures.healthy(NOW.plus(Duration.ofHours(1)));
            AtomicInteger calls = new AtomicInteger();

            cache.get(URL, url -> {
                calls.incrementAndGet();
                return info;
            });
            advance(Duration.ofMinutes(51));
            cache.get(URL, url -> {
                calls.incrementAndGet();
                return info;
            });

            assertThat(calls).hasValue(2);
        }

        @Test
        @DisplayName("a read partway through the TTL does not extend it")
        void readDoesNotExtendTtl() throws Exception {
            StreamInfo info = StreamFixtures.healthyWithoutExpiry();
            AtomicInteger calls = new AtomicInteger();

            cache.get(URL, url -> {
                calls.incrementAndGet();
                return info;
            });

            advance(Duration.ofMinutes(3));
            cache.get(URL, url -> {
                calls.incrementAndGet();
                return info;
            });
            assertThat(calls).as("still within the 5 minute lease").hasValue(1);

            advance(Duration.ofMinutes(3));
            cache.get(URL, url -> {
                calls.incrementAndGet();
                return info;
            });

            assertThat(calls).as("lease is set at write, not by demand").hasValue(2);
        }
    }

    // --- test doubles -------------------------------------------------------

    /** A clock that only moves when the test moves it. */
    static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant start) {
            this.instant = start;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    /** Caffeine's elapsed-time source, under test control. */
    static final class MutableTicker implements Ticker {
        private final AtomicLong nanos = new AtomicLong();

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }

        @Override
        public long read() {
            return nanos.get();
        }
    }
}
