package org.example.api.service;

import java.util.List;
import org.example.api.cache.StreamInfoCache;
import org.example.api.config.ExtractionProperties;
import org.example.api.exception.ExtractionException;
import org.example.api.exception.NewPipeExceptionMapper;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.stream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for extracting detailed information about video streams.
 * Returns DTOs/domain objects instead of JSON strings for better separation of concerns.
 */
@Service
public class VideoStreamingService {
    private final StreamInfoCache cache;
    private final int attempts;
    private static final Logger logger = LoggerFactory.getLogger(VideoStreamingService.class);

    public VideoStreamingService(StreamInfoCache cache, ExtractionProperties extraction) {
        this.cache = cache;
        this.attempts = extraction.attempts();
    }

    /**
     * Retrieves comprehensive information about a specific stream.
     */
    public StreamInfo getStreamInfo(String url) throws ExtractionException {
        return cache.get(url, key -> extractWithRetry(key, attempts));
    }

    /**
     * Performs the YouTube extraction, retrying while YouTube returns an incomplete
     * response, up to {@code opentube.extraction.attempts} attempts in total.
     *
     * <p>This is the loader for {@link StreamInfoCache} and must only be called from
     * {@link #getStreamInfo(String)}. Calling it directly bypasses the cache.</p>
     *
     * @param url               the YouTube video URL to extract
     * @param attemptsRemaining total attempts allowed, including this one; a retry
     *                          only happens while this is greater than 1
     * @return the extracted stream info
     * @throws ExtractionException if extraction itself fails
     */
    private StreamInfo extractWithRetry(String url, int attemptsRemaining) throws ExtractionException {
        try {
            logger.info("Extracting stream info for URL: {}", url);
            StreamInfo info = StreamInfo.getInfo(url);

            boolean hasAdaptiveStreams = !info.getVideoOnlyStreams().isEmpty()
                    || !info.getAudioStreams().isEmpty();
            boolean isValidResult = info.getDuration() > 0;

            if (!hasAdaptiveStreams && isValidResult && attemptsRemaining > 1) {
                logger.warn(
                        "Got incomplete streams for URL: {} (videoOnly={}, audio={}, muxed={}) — retrying ({} attempts remaining)",
                        url,
                        info.getVideoOnlyStreams().size(),
                        info.getAudioStreams().size(),
                        info.getVideoStreams().size(),
                        attemptsRemaining - 1);
                return extractWithRetry(url, attemptsRemaining - 1);
            }

            logger.info(
                    "Extracted streams for URL: {} (videoOnly={}, audio={}, muxed={})",
                    url,
                    info.getVideoOnlyStreams().size(),
                    info.getAudioStreams().size(),
                    info.getVideoStreams().size());
            return info;
        } catch (Exception e) {
            throw NewPipeExceptionMapper.translate(e);
        }
    }

    /**
     * Extracts available audio streams for a given stream URL.
     */
    public List<AudioStream> getAudioStreams(String url) throws ExtractionException {
        return getStreamInfo(url).getAudioStreams();
    }

    /**
     * Extracts available video streams for a given stream URL.
     */
    public List<VideoStream> getVideoStreams(String url) throws ExtractionException {
        return getStreamInfo(url).getVideoOnlyStreams();
    }

    /**
     * Extracts the DASH MPD URL for a given stream.
     */
    public String getDashMpdUrl(String url) throws ExtractionException {
        return getStreamInfo(url).getDashMpdUrl();
    }

    /**
     * Extracts thumbnail images for a given stream URL
     */
    public List<Image> getStreamThumbnails(String url) throws ExtractionException {
        return getStreamInfo(url).getThumbnails();
    }

    /**
     * Extracts available subtitle streams for a given stream URL.
     */
    public List<SubtitlesStream> getSubtitleStreams(String url) throws ExtractionException {
        return getStreamInfo(url).getSubtitles();
    }

    /**
     * Extracts stream segments for a given stream URL.
     */
    public List<StreamSegment> getStreamSegments(String url) throws ExtractionException {
        return getStreamInfo(url).getStreamSegments();
    }

    /**
     * Extracts preview frames for a given stream URL.
     */
    public List<Frameset> getPreviewFrames(String url) throws ExtractionException {
        return getStreamInfo(url).getPreviewFrames();
    }

    /**
     * Retrieves the description of a stream for a given URL.
     */
    public Description getStreamDescription(String url) throws ExtractionException {
        return getStreamInfo(url).getDescription();
    }

    /**
     * Retrieves related streams for a given URL.
     */
    public List<InfoItem> getRelatedStreams(String url) throws ExtractionException {
        return getStreamInfo(url).getRelatedItems();
    }
}
