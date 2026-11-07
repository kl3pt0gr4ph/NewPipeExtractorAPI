package org.example.api.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for integration tests. The application context initialises NewPipeExtractor
 * itself (see {@link org.example.api.config.NewPipeInitializer}), from the same
 * {@code opentube.newpipe} and {@code opentube.http} settings as production.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Tag("integration")
public abstract class BaseIntegrationTest {

    /**
     * Wait for a condition with timeout.
     */
    protected void waitFor(long timeoutMs, long pollIntervalMs, java.util.function.BooleanSupplier condition)
            throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw new AssertionError("Condition not met within timeout");
            }
            Thread.sleep(pollIntervalMs);
        }
    }
}
