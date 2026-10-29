package org.example.api.config.openapi;

/**
 * Shared text for OpenAPI annotations, so parameters that mean the same thing
 * are described the same way on every endpoint.
 */
public final class OpenApiDocs {

    private OpenApiDocs() {}

    // ── Tags ────────────────────────────────────────────────────────────────

    public static final String TAG_STREAMS = "Streams";
    public static final String TAG_SEARCH = "Search";
    public static final String TAG_CHANNELS = "Channels";
    public static final String TAG_PLAYLISTS = "Playlists";
    public static final String TAG_COMMENTS = "Comments";
    public static final String TAG_KIOSKS = "Kiosks";
    public static final String TAG_SERVICES = "Services";

    // ── Common parameters ───────────────────────────────────────────────────

    public static final String VIDEO_ID = "YouTube video ID, the `v` parameter of a watch URL.";
    public static final String VIDEO_ID_EXAMPLE = "dQw4w9WgXcQ";

    public static final String CHANNEL_ID =
            "The part of the channel URL after `youtube.com/`: a handle (`@name`) or `channel/UC…`.";
    public static final String CHANNEL_ID_EXAMPLE = "@LinusTechTips";

    public static final String PLAYLIST_ID = "YouTube playlist ID, the `list` parameter of a playlist URL.";
    public static final String PLAYLIST_ID_EXAMPLE = "PLHovnlOusNLiJz3sm0d5i2Evwa2LDLdrg";

    public static final String CHANNEL_TAB =
            "Channel tab to read. Only video-like items are returned, so `playlists` currently "
                    + "comes back with an empty `items` list.";

    public static final String FROM_NEXT_PAGE =
            "Taken verbatim from the `nextPage` object of the previous response.";

    /** Appended to descriptions of endpoints that hand out googlevideo URLs. */
    public static final String SIGNED_URLS =
            " Stream URLs are signed, expire after a few hours, and only work from the public IP "
                    + "that extracted them.";

    public static final String CONTENT_FILTERS =
            "Result type to restrict the search to: `all`, `videos`, `channels`, `playlists`, or "
                    + "the YouTube Music filters `music_songs`, `music_videos`, `music_albums`, "
                    + "`music_playlists`, `music_artists`. Accepts a comma-separated list, but "
                    + "YouTube only applies the first entry. Omit for all types.";

    public static final String SORT_FILTER =
            "Passed through to NewPipeExtractor, whose YouTube service currently defines no sort "
                    + "filters, so leave it unset.";

    // ── Raw NewPipeExtractor responses ──────────────────────────────────────

    /** Appended to descriptions of endpoints that return NewPipeExtractor types unmapped. */
    public static final String RAW_NEWPIPE_TYPE =
            "\n\nThe body is NewPipeExtractor's own type serialised as-is, so its shape follows "
                    + "the library and can change when NewPipeExtractor is upgraded.";
}