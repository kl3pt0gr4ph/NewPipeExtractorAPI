package org.example.api.component;

import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Decides which extracted streams can be described in an on-demand DASH manifest.
 *
 * <p>This is the only place that removes streams before manifest generation. Every rule
 * is about playability: could a player fetch and decode this stream from the manifest
 * we write?</p>
 */
@Component
public class DashPlayabilityFilter {

    private static final Logger logger = LoggerFactory.getLogger(DashPlayabilityFilter.class);

    /**
     * Text formats a DASH player can load from a single BaseURL, best first.
     */
    private static final List<MediaFormat> PLAYABLE_SUBTITLE_FORMATS =
            List.of(MediaFormat.VTT, MediaFormat.TTML);

    /** NewPipe builds each subtitle URL as the track's base URL plus &fmt=<suffix>. */
    private static final Pattern FORMAT_PARAM = Pattern.compile("[?&]fmt=[^&]*");

    /** The playable subset of one video's streams. */
    public record PlayableStreams(List<VideoStream> video,
                                  List<AudioStream> audio,
                                  List<SubtitlesStream> subtitles) {

        /** An on-demand manifest needs at least one video and one audio representation. */
        public boolean supportsDashManifest() {
            return !video.isEmpty() && !audio.isEmpty();
        }
    }

    public PlayableStreams filter(StreamInfo info) {
        List<VideoStream> video = keep(info.getVideoOnlyStreams(), "video",
                v -> adaptiveRejection(v, v.getCodec(),
                        v.getInitStart(), v.getInitEnd(), v.getIndexStart(), v.getIndexEnd()));

        List<AudioStream> audio = keep(info.getAudioStreams(), "audio",
                a -> adaptiveRejection(a, a.getCodec(),
                        a.getInitStart(), a.getInitEnd(), a.getIndexStart(), a.getIndexEnd()));

        List<SubtitlesStream> subtitles = oneSerializationPerTrack(
                keep(info.getSubtitles(), "subtitle", DashPlayabilityFilter::subtitleRejection));

        return new PlayableStreams(video, audio, subtitles);
    }

    /** First muxed (audio+video) stream that can be played directly by a <video> element. */
    public Optional<VideoStream> muxedFallback(StreamInfo info) {
        return keep(info.getVideoStreams(), "muxed", DashPlayabilityFilter::urlRejection)
                .stream()
                .findFirst();
    }

    // --- Rules -----------------------------------------------------------------

    /** Returns why a stream can't be fetched by URL, or null if it can. */
    private static String urlRejection(Stream stream) {
        if (stream.getDeliveryMethod() != DeliveryMethod.PROGRESSIVE_HTTP) {
            return "not progressive HTTP";
        }
        if (!stream.isUrl() || isBlank(stream.getContent())) {
            return "no URL";
        }
        return null;
    }

    /** Returns why an adaptive stream can't be a SegmentBase Representation, or null. */
    private static String adaptiveRejection(Stream stream, String codec,
                                            int initStart, int initEnd,
                                            int indexStart, int indexEnd) {
        String reason = urlRejection(stream);
        if (reason != null) {
            return reason;
        }
        if (stream.getFormat() == null || isBlank(stream.getFormat().getMimeType())) {
            return "no mimeType";
        }
        if (isBlank(codec)) {
            return "no codec";
        }
        // ItagItem leaves these at 0 when unknown, so "0-0" must not count as a range.
        // The index (sidx) box always follows the init segment.
        if (initStart < 0 || initEnd <= initStart
                || indexStart <= initEnd || indexEnd <= indexStart) {
            return "no byte ranges";
        }
        return null;
    }

    private static String subtitleRejection(SubtitlesStream subtitle) {
        String reason = urlRejection(subtitle);
        if (reason != null) {
            return reason;
        }
        MediaFormat format = subtitle.getFormat();
        return format != null && PLAYABLE_SUBTITLE_FORMATS.contains(format)
                ? null
                : "format players can't parse";
    }

    // --- Helpers ---------------------------------------------------------------

    /**
     * The same track is offered in several serializations (VTT and TTML). Keep one per
     * track so the player's track list doesn't show each language twice. Distinct tracks,
     * including manual and auto-generated ones in the same language, are all kept.
     */
    private static List<SubtitlesStream> oneSerializationPerTrack(List<SubtitlesStream> subtitles) {
        Map<String, SubtitlesStream> byTrack = new LinkedHashMap<>();
        for (SubtitlesStream subtitle : subtitles) {
            byTrack.merge(trackKey(subtitle), subtitle,
                    (current, candidate) ->
                            formatRank(candidate) < formatRank(current) ? candidate : current);
        }
        return List.copyOf(byTrack.values());
    }

    private static String trackKey(SubtitlesStream subtitle) {
        return FORMAT_PARAM.matcher(subtitle.getContent()).replaceAll("");
    }

    private static int formatRank(SubtitlesStream subtitle) {
        return PLAYABLE_SUBTITLE_FORMATS.indexOf(subtitle.getFormat());
    }

    /** Applies a rejection rule and logs one DEBUG summary of what was dropped and why. */
    private static <T extends Stream> List<T> keep(List<T> streams, String kind,
                                                   Function<T, String> rejection) {
        if (streams == null || streams.isEmpty()) {
            return List.of();
        }

        List<T> kept = new ArrayList<>(streams.size());
        Map<String, Integer> dropped = new TreeMap<>();
        for (T stream : streams) {
            String reason = rejection.apply(stream);
            if (reason == null) {
                kept.add(stream);
            } else {
                dropped.merge(reason, 1, Integer::sum);
            }
        }

        if (!dropped.isEmpty()) {
            logger.debug("Kept {} of {} {} stream(s); dropped {}",
                    kept.size(), streams.size(), kind, dropped);
        }
        return List.copyOf(kept);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
