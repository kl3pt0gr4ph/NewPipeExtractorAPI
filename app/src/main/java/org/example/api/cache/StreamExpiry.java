package org.example.api.cache;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StreamExpiry {
    private static final Logger logger = LoggerFactory.getLogger(StreamExpiry.class);
    private static final Pattern EXPIRE = Pattern.compile("[?&]expire=(\\d+)");

    private StreamExpiry() {}

    /**
     * @return the earliest expiry across all streams, or empty if no stream URL
     *         carried a parseable {@code expire} parameter
     */
    public static Optional<Instant> earliest(StreamInfo info) {
        List<Stream> all = new ArrayList<>();
        all.addAll(info.getVideoOnlyStreams());
        all.addAll(info.getAudioStreams());
        all.addAll(info.getVideoStreams());

        long earliest = Long.MAX_VALUE;
        for (var stream : all) {
            String content = stream.getContent();
            if (content == null) continue;

            Matcher matcher = EXPIRE.matcher(content);
            if (!matcher.find()) continue;

            String raw = matcher.group(1);
            try {
                earliest = Math.min(earliest, Long.parseLong(raw));
            } catch (NumberFormatException e) {
                logger.warn("Ignoring unparseable expire value: {}", raw);
            }
        }
        return earliest == Long.MAX_VALUE ? Optional.empty() : Optional.of(Instant.ofEpochSecond(earliest));
    }
}
