package org.example.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.api.dto.ErrorResponseDTO;
import org.example.api.dto.StreamDetailsDTO;
import org.example.api.dto.dash.DashManifestConfigDTO;
import org.example.api.exception.NoStreamsAvailableException;
import org.example.api.service.DashManifestGeneratorService;
import org.example.api.service.VideoStreamingService;
import org.example.api.component.DashPlayabilityFilter;
import org.example.api.component.DashPlayabilityFilter.PlayableStreams;
import org.example.api.utils.ValidationUtils;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.stream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

import java.util.List;

import static org.example.api.config.openapi.OpenApiDocs.*;

/**
 * REST controller for handling streaming-related API requests for YouTube information.
 * This class provides endpoints for retrieving stream and audio stream data based on provided IDs.
 * All endpoints are prefixed with "/api/v1/streams".
 */
@Tag(name = TAG_STREAMS)
@RestController
@RequestMapping("/api/v1/streams")
public class StreamingController {
    private static final String YOUTUBE_URL = "https://www.youtube.com/watch?v=";
    private static final MediaType URI_LIST = new MediaType("text", "uri-list", StandardCharsets.UTF_8);
    private static final MediaType DASH_MPD =
            new MediaType("application", "dash+xml", StandardCharsets.UTF_8);
    private static final Logger logger = LoggerFactory.getLogger(StreamingController.class);
    private final VideoStreamingService videoStreamingService;
    private final DashManifestGeneratorService dashManifestGeneratorService;
    private final DashPlayabilityFilter dashPlayabilityFilter;

    public StreamingController(VideoStreamingService videoStreamingService, DashManifestGeneratorService dashManifestGeneratorService, DashPlayabilityFilter dashPlayabilityFilter) {
        this.videoStreamingService = videoStreamingService;
        this.dashManifestGeneratorService = dashManifestGeneratorService;
        this.dashPlayabilityFilter = dashPlayabilityFilter;
    }

    /** Builds the watch URL for a video id, validating both. */
    private String videoUrl(String id) {
        ValidationUtils.requireNonEmpty(id, "id");
        String url = YOUTUBE_URL + id;
        ValidationUtils.requireValidUrl(url);
        return url;
    }

    /**
     * Handles HTTP GET requests to retrieve stream information based on a provided ID.
     *
     * @param id The ID of the stream to retrieve information for.
     * @return 200 with the stream information, 400 for a missing/empty id,
     *         or an error response from the exception handler.
     */
    @Operation(summary = "Full stream info",
            description = "Everything NewPipeExtractor extracts for the video in one object: metadata, "
                    + "every stream, subtitles, chapters and related items. Most clients want "
                    + "`/streams/details` and `/streams/dash` instead."
                    + RAW_NEWPIPE_TYPE)
    @GetMapping
    public ResponseEntity<StreamInfo> getStreamInfo(
            @Parameter(description = VIDEO_ID, example = VIDEO_ID_EXAMPLE)
            @RequestParam(name = "id") String id) {
        logger.debug("Retrieving stream info for ID: {}", id);

        StreamInfo info = videoStreamingService.getStreamInfo(videoUrl(id));
        return ResponseEntity.ok(info);
    }

    /** Get audio streams for a video. */
    @Operation(summary = "Audio streams",
            description = "Every audio stream YouTube offers for the video." + SIGNED_URLS + RAW_NEWPIPE_TYPE)
    @GetMapping("/audio")
    public ResponseEntity<List<AudioStream>> getAudioStreams(
            @Parameter(description = VIDEO_ID, example = VIDEO_ID_EXAMPLE)
            @RequestParam(name = "id") String id) {
        logger.debug("Retrieving audio stream for ID: {}", id);

        List<AudioStream> streams = videoStreamingService.getAudioStreams(videoUrl(id));
        return ResponseEntity.ok(streams);
    }

    /** Get video-only streams. */
    @Operation(summary = "Video-only streams",
            description = "Every video-only (no audio) stream YouTube offers for the video."
                    + SIGNED_URLS + RAW_NEWPIPE_TYPE)
    @GetMapping("/video")
    public ResponseEntity<List<VideoStream>> getVideoStreams(
            @Parameter(description = VIDEO_ID, example = VIDEO_ID_EXAMPLE)
            @RequestParam(name = "id") String id) {
        logger.debug("Retrieving video stream for ID: {}", id);

        List<VideoStream> streams = videoStreamingService.getVideoStreams(videoUrl(id));
        return ResponseEntity.ok(streams);
    }

    /** Get DASH MPD URL for adaptive streaming. */
    @Operation(summary = "YouTube's DASH manifest URL",
            description = "The URL of YouTube's own DASH manifest, as plain text. YouTube only provides "
                    + "one for some videos (typically live streams); otherwise the body is empty. "
                    + "For on-demand playback use `/streams/dash`.")
    @ApiResponse(responseCode = "200", description = "Manifest URL, or an empty body",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    @GetMapping("/video/dash")
    public ResponseEntity<String> getDashMpdUrl(
            @Parameter(description = VIDEO_ID, example = VIDEO_ID_EXAMPLE)
            @RequestParam(name = "id") String id) {
        logger.debug("Retrieving DASH MPD URL for ID: {}", id);

        String dashUrl = videoStreamingService.getDashMpdUrl(videoUrl(id));
        return ResponseEntity.ok(dashUrl);
    }

    /** Get DASH Manifest describing every playable representation. */
    @Operation(summary = "DASH manifest for playback",
            description = """
                    An on-demand DASH manifest describing every playable video, audio and subtitle \
                    representation, so the player chooses quality, codec and language.

                    When no manifest can be built (e.g. SABR-restricted videos), the response is \
                    instead a single progressive MP4 URL as `text/uri-list`, playable by a plain \
                    `<video>` element. **Check `Content-Type` to tell the two apart**, not the body.

                    Stream URLs are signed, expire after a few hours, and only work from the \
                    public IP that extracted them.""")
    @ApiResponse(responseCode = "200",
            description = "A DASH manifest, or the muxed fallback URL followed by CRLF",
            content = {
                    @Content(mediaType = "application/dash+xml", schema = @Schema(type = "string",
                            description = "MPD document")),
                    @Content(mediaType = "text/uri-list", schema = @Schema(type = "string",
                            description = "One progressive stream URL followed by CRLF"))
            })
    @ApiResponse(responseCode = "503",
            description = "Nothing playable, not even a muxed stream (`NO_STREAMS_AVAILABLE`)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponseDTO.class)))
    @GetMapping("/dash")
    public ResponseEntity<String> getDashManifest(
            @Parameter(description = VIDEO_ID, example = VIDEO_ID_EXAMPLE)
            @RequestParam(name = "id") String id) {
        logger.debug("Generating DASH manifest for ID: {}", id);

        StreamInfo streamInfo = videoStreamingService.getStreamInfo(videoUrl(id));
        PlayableStreams playable = dashPlayabilityFilter.filter(streamInfo);

        if (!playable.supportsDashManifest()) {
            return muxedProgressiveFallback(id, streamInfo, playable);
        }

        DashManifestConfigDTO config = DashManifestConfigDTO.from(
                streamInfo, playable.video(), playable.audio(), playable.subtitles());
        String manifest = dashManifestGeneratorService.generateManifestXml(config);

        logger.info("Generated DASH manifest for ID {}: {} video, {} audio, {} subtitle representation(s)",
                id, config.getVideoStreams().size(), config.getAudioStreams().size(),
                config.getSubtitleStreams().size());

        return ResponseEntity.ok()
                .contentType(DASH_MPD)
                .body(manifest);
    }

    /**
     * Used when no valid on-demand manifest can be built, e.g. SABR-restricted videos
     * whose adaptive streams aren't progressive. Signalled by {@code Content-Type: text/uri-list}
     */
    private ResponseEntity<String> muxedProgressiveFallback(String id, StreamInfo streamInfo,
                                                            PlayableStreams playable) {
        VideoStream muxed = dashPlayabilityFilter.muxedFallback(streamInfo)
                .orElseThrow(() -> new NoStreamsAvailableException(
                        "No streams available — YouTube may be blocking extraction for this video"));

        logger.warn("No DASH manifest possible for ID {} ({}/{} video, {}/{} audio playable); "
                        + "falling back to muxed stream {}",
                id, playable.video().size(), streamInfo.getVideoOnlyStreams().size(),
                playable.audio().size(), streamInfo.getAudioStreams().size(), muxed.getId());

        return ResponseEntity.ok()
                .contentType(URI_LIST)
                .body(muxed.getContent() + "\r\n");
    }

    /** Get stream thumbnails */
    @Operation(summary = "Thumbnails", description = "Video thumbnails in every size YouTube offers."
            + RAW_NEWPIPE_TYPE)
    @GetMapping("/thumbnails")
    public ResponseEntity<List<Image>> getThumbnails(
            @Parameter(description = VIDEO_ID, example = VIDEO_ID_EXAMPLE)
            @RequestParam(name = "id") String id) {
        logger.debug("Retrieving stream thumbnails for ID: {}", id);

        List<Image> thumbnails = videoStreamingService.getStreamThumbnails(videoUrl(id));
        return ResponseEntity.ok(thumbnails);
    }

    /** Get subtitle streams. */
    @Operation(summary = "Subtitle tracks",
            description = "Manual and auto-generated subtitle tracks in every format YouTube offers."
                    + RAW_NEWPIPE_TYPE)
    @GetMapping("/subtitles")
    public ResponseEntity<List<SubtitlesStream>> getSubtitleStreams(
            @Parameter(description = VIDEO_ID, example = VIDEO_ID_EXAMPLE)
            @RequestParam(name = "id") String id) {
        logger.debug("Retrieving subtitle streams for ID: {}", id);

        List<SubtitlesStream> subtitles = videoStreamingService.getSubtitleStreams(videoUrl(id));
        return ResponseEntity.ok(subtitles);
    }

    /** Get stream segments (chapters). */
    @Operation(summary = "Chapters", description = "The video's chapters; empty when it has none."
            + RAW_NEWPIPE_TYPE)
    @GetMapping("/segments")
    public ResponseEntity<List<StreamSegment>> getStreamSegments(
            @Parameter(description = VIDEO_ID, example = VIDEO_ID_EXAMPLE)
            @RequestParam(name = "id") String id) {
        logger.debug("Retrieving stream segments for ID: {}", id);

        List<StreamSegment> segments = videoStreamingService.getStreamSegments(videoUrl(id));
        return ResponseEntity.ok(segments);
    }

    /** Get preview frames for video scrubbing. */
    @Operation(summary = "Seek preview frames",
            description = "Storyboard sprite sheets for showing previews while seeking." + RAW_NEWPIPE_TYPE)
    @GetMapping("/preview-frames")
    public ResponseEntity<List<Frameset>> getPreviewFrames(
            @Parameter(description = VIDEO_ID, example = VIDEO_ID_EXAMPLE)
            @RequestParam(name = "id") String id) {
        logger.debug("Retrieving preview frames stream for ID: {}", id);

        List<Frameset> frames = videoStreamingService.getPreviewFrames(videoUrl(id));
        return ResponseEntity.ok(frames);
    }

    /** Get stream description. */
    @Operation(summary = "Description",
            description = "The video description; `type` is 1 for HTML, 2 for Markdown, 3 for plain text."
                    + RAW_NEWPIPE_TYPE)
    @GetMapping("/description")
    public ResponseEntity<Description> getStreamDescription(
            @Parameter(description = VIDEO_ID, example = VIDEO_ID_EXAMPLE)
            @RequestParam(name = "id") String id) {
        logger.debug("Retrieving description stream for ID: {}", id);

        Description description = videoStreamingService.getStreamDescription(videoUrl(id));
        return ResponseEntity.ok(description);
    }

    /** Get comprehensive stream details (metadata). */
    @Operation(summary = "Video details",
            description = "Title, description, channel, view and like counts and upload date: what a "
                    + "watch page shows around the player.")
    @GetMapping("/details")
    public ResponseEntity<StreamDetailsDTO> getStreamDetails(
            @Parameter(description = VIDEO_ID, example = VIDEO_ID_EXAMPLE)
            @RequestParam(name = "id") String id) {
        logger.debug("Retrieving stream details for ID: {}", id);

        StreamInfo info = videoStreamingService.getStreamInfo(videoUrl(id));
        StreamDetailsDTO details = StreamDetailsDTO.from(info);

        return ResponseEntity.ok(details);
    }

    /** Get related videos/streams. */
    @Operation(summary = "Related items",
            description = "Videos, playlists and channels YouTube suggests next to the video. "
                    + "Each item's `infoType` (`STREAM`, `PLAYLIST` or `CHANNEL`) says which shape it has."
                    + RAW_NEWPIPE_TYPE)
    @ApiResponse(responseCode = "200", description = "Related items",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(
                    oneOf = {StreamInfoItem.class, PlaylistInfoItem.class, ChannelInfoItem.class}))))
    @GetMapping("/related")
    public ResponseEntity<List<InfoItem>> getRelatedStreams(
            @Parameter(description = VIDEO_ID, example = VIDEO_ID_EXAMPLE)
            @RequestParam(name = "id") String id) {
        logger.debug("Retrieving related streams for ID: {}", id);

        List<InfoItem> relatedItems = videoStreamingService.getRelatedStreams(videoUrl(id));
        return ResponseEntity.ok(relatedItems);
    }
}