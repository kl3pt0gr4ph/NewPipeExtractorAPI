package org.example.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.api.dto.playlist.PlaylistDTO;
import org.example.api.service.PlaylistService;
import org.example.api.utils.ValidationUtils;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.playlist.PlaylistInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.example.api.config.openapi.OpenApiDocs.*;

@Tag(name = TAG_PLAYLISTS)
@RestController
@RequestMapping("/api/v1/playlists")
public class PlaylistController {
    private static final String YOUTUBE_PLAYLIST_URL = "https://www.youtube.com/playlist?list=";
    private static final Logger logger = LoggerFactory.getLogger(PlaylistController.class);
    private final PlaylistService playlistService;

    @Autowired
    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @Operation(summary = "Playlist info", description = "Playlist metadata and its first page of videos.")
    @GetMapping
    public ResponseEntity<PlaylistDTO> getPlaylistInfo(
            @Parameter(description = PLAYLIST_ID, example = PLAYLIST_ID_EXAMPLE)
            @RequestParam(name = "id") String id){
        logger.debug("Retrieving playlist info for id: {}", id);

        String url = YOUTUBE_PLAYLIST_URL + id;
        ValidationUtils.requireValidUrl(url);

        PlaylistInfo playlist = playlistService.getPlaylistInfo(url);
        PlaylistDTO playlistDTO = PlaylistDTO.from(playlist);
        return ResponseEntity.ok(playlistDTO);
    }

    @Operation(summary = "Next page of a playlist",
            description = "A later page of the playlist's videos. Note that `/playlists` doesn't return a "
                    + "`nextPage`, so the first `pageUrl` has to come from elsewhere for now."
                    + RAW_NEWPIPE_TYPE)
    @GetMapping("/page")
    public ResponseEntity<InfoItemsPage<StreamInfoItem>> getPlaylistPage(
            @Parameter(description = "Full playlist URL, not just the ID",
                    example = "https://www.youtube.com/playlist?list=" + PLAYLIST_ID_EXAMPLE)
            @RequestParam(name = "url") String url,
            @Parameter(description = "Continuation URL of the page to fetch")
            @RequestParam(name = "pageUrl") String pageUrl
    ) {
        logger.debug("Retrieving playlist page for url: {}", url);

        ValidationUtils.requireValidUrl(url);
        ValidationUtils.requireValidUrl(pageUrl);

        InfoItemsPage<StreamInfoItem> playlistPage = playlistService.getPlaylistPage(
                url,
                pageUrl
        );

        return ResponseEntity.ok(playlistPage);
    }
}