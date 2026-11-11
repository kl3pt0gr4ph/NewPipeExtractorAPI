package org.example.api.cache;

import java.time.Instant;
import java.util.List;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.VideoStream;

/**
 * Builders for the minimal {@link StreamInfo} shapes the cache tests need.
 * Real extractor objects rather than mocks, so the tests break if NewPipe's
 * model changes underneath us.
 */
final class StreamFixtures {

    static final String WATCH_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";

    private StreamFixtures() {}

    static String signedUrl(Instant expiry) {
        return signedUrl(String.valueOf(expiry.getEpochSecond()));
    }

    static String signedUrl(String expireValue) {
        return "https://rr3---sn-example.googlevideo.com/videoplayback" + "?itag=140&expire=" + expireValue
                + "&ei=abc&source=youtube";
    }

    static String unsignedUrl() {
        return "https://rr3---sn-example.googlevideo.com/videoplayback?itag=140&source=youtube";
    }

    static AudioStream audio(String content) {
        return new AudioStream.Builder()
                .setId("audio-140")
                .setContent(content, true)
                .setAverageBitrate(128)
                .build();
    }

    static VideoStream videoOnly(String content) {
        return new VideoStream.Builder()
                .setId("video-137")
                .setContent(content, true)
                .setIsVideoOnly(true)
                .setResolution("1080p")
                .build();
    }

    static VideoStream muxed(String content) {
        return new VideoStream.Builder()
                .setId("muxed-18")
                .setContent(content, true)
                .setIsVideoOnly(false)
                .setResolution("360p")
                .build();
    }

    /** A StreamInfo with no streams of any kind. */
    static StreamInfo empty() {
        StreamInfo info =
                new StreamInfo(0, WATCH_URL, WATCH_URL, StreamType.VIDEO_STREAM, "dQw4w9WgXcQ", "Test video", 0);
        info.setDuration(212);
        info.setAudioStreams(List.of());
        info.setVideoOnlyStreams(List.of());
        info.setVideoStreams(List.of());
        return info;
    }

    /** Healthy: adaptive audio + video-only streams, both signed with the same expiry. */
    static StreamInfo healthy(Instant expiry) {
        StreamInfo info = empty();
        info.setAudioStreams(List.of(audio(signedUrl(expiry))));
        info.setVideoOnlyStreams(List.of(videoOnly(signedUrl(expiry))));
        return info;
    }

    /** Healthy shape, but no stream URL carries an expire parameter. */
    static StreamInfo healthyWithoutExpiry() {
        StreamInfo info = empty();
        info.setAudioStreams(List.of(audio(unsignedUrl())));
        info.setVideoOnlyStreams(List.of(videoOnly(unsignedUrl())));
        return info;
    }

    /**
     * Degraded: the SABR case — no adaptive streams at all, only a muxed
     * progressive stream, despite a valid duration.
     */
    static StreamInfo degraded(Instant expiry) {
        StreamInfo info = empty();
        info.setVideoStreams(List.of(muxed(signedUrl(expiry))));
        return info;
    }

    static StreamInfo with(List<AudioStream> audio, List<VideoStream> videoOnly, List<VideoStream> muxed) {
        StreamInfo info = empty();
        info.setAudioStreams(audio);
        info.setVideoOnlyStreams(videoOnly);
        info.setVideoStreams(muxed);
        return info;
    }
}
