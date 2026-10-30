package org.example.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.api.dto.channels.ChannelDTO;
import org.example.api.dto.channels.ChannelTabDTO;
import org.example.api.service.ChannelService;
import org.example.api.service.ChannelTabService;
import org.example.api.utils.ValidationUtils;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.example.api.config.openapi.OpenApiDocs.*;

@Tag(name = TAG_CHANNELS)
@RestController
@RequestMapping("/api/v1/channels")
public class ChannelController {
    private static final String YOUTUBE_URL = "https://www.youtube.com/";
    private static final Logger logger = LoggerFactory.getLogger(ChannelController.class);

    private final ChannelService channelService;
    private final ChannelTabService channelTabService;

    @Autowired
    public ChannelController(ChannelService channelService, ChannelTabService channelTabService) {
        this.channelService = channelService;
        this.channelTabService = channelTabService;
    }

    @Operation(summary = "Channel info",
            description = "Name, avatars, banners, description, subscriber count and the tabs the channel has.")
    @GetMapping
    public ResponseEntity<ChannelDTO> getChannelInfo(
            @Parameter(description = CHANNEL_ID, example = CHANNEL_ID_EXAMPLE)
            @RequestParam(name = "id") String id) {
        logger.debug("Retrieving channel info for ID: {}", id);

        String url = YOUTUBE_URL + id;
        ValidationUtils.requireValidUrl(url);

        ChannelInfo channel = channelService.getChannelInfo(url);
        ChannelDTO channelDTO = ChannelDTO.from(channel);

        return ResponseEntity.ok(channelDTO);
    }

    @Operation(summary = "Channel tab",
            description = "First page of one channel tab. Fetch more with `/channels/tab/page`.")
    @GetMapping("/tab")
    public ResponseEntity<ChannelTabDTO> getChannelTab(
            @Parameter(description = CHANNEL_ID, example = CHANNEL_ID_EXAMPLE)
            @RequestParam(name = "id") String id,
            @Parameter(description = CHANNEL_TAB, schema = @Schema(type = "string",
                    defaultValue = ChannelTabs.VIDEOS,
                    allowableValues = {"videos", "shorts", "livestreams", "playlists"}))
            @RequestParam(name = "tab", defaultValue = ChannelTabs.VIDEOS) String tab
    ) {
        logger.debug("Fetching channel tab '{}' for ID: {}", tab, id);

        String channelUrl = YOUTUBE_URL + id;
        ValidationUtils.requireValidUrl(channelUrl);

        ChannelTabDTO result = channelTabService.getChannelTab(channelUrl, tab, id);
        return ResponseEntity.ok(result);
    }

    /**
     * Returns a subsequent page of channel tab items.
     *
     * <p>All three {@code nextPage.*} parameters must be taken verbatim from the
     * {@code nextPage} object in the previous response — the body and ids are required
     * by NewPipe to correctly make the continuation request and annotate returned items.</p>
     *
     * @param channelId  channel ID from the previous response's {@code channelId} field
     * @param tab        tab type — must match the initial request
     * @param pageUrl    from {@code nextPage.url}
     * @param pageBody   from {@code nextPage.body} (Base64-encoded continuation token)
     * @param pageIds    from {@code nextPage.ids} (["channelName", "channelUrl", "verifiedStatus"])
     */
    @Operation(summary = "Next page of a channel tab",
            description = "Pass the previous response's `channelId` and `tab`, and all three "
                    + "`nextPage` fields unmodified. `pageIds` is repeated once per entry: "
                    + "`pageIds=…&pageIds=…&pageIds=…`.")
    @GetMapping("/tab/page")
    public ResponseEntity<ChannelTabDTO> getChannelTabPage(
            @Parameter(description = "`channelId` from the previous response", example = CHANNEL_ID_EXAMPLE)
            @RequestParam(name = "channelId") String channelId,
            @Parameter(description = "Same tab as the first request", schema = @Schema(type = "string",
                    defaultValue = ChannelTabs.VIDEOS,
                    allowableValues = {"videos", "shorts", "livestreams", "playlists"}))
            @RequestParam(name = "tab", defaultValue = ChannelTabs.VIDEOS) String tab,
            @Parameter(description = "`nextPage.url`. " + FROM_NEXT_PAGE)
            @RequestParam(name = "pageUrl") String pageUrl,
            @Parameter(description = "`nextPage.body`, Base64. " + FROM_NEXT_PAGE)
            @RequestParam(name = "pageBody") String pageBody,
            @Parameter(description = "`nextPage.ids`, all three entries in order. " + FROM_NEXT_PAGE)
            @RequestParam(name = "pageIds") List<String> pageIds
    ) {
        logger.debug("Retrieving channel tab '{}' page for channelId: {}", tab, channelId);

        ChannelTabDTO result = channelTabService.getChannelTabPage(
                channelId, tab, pageUrl, pageBody, pageIds);
        return ResponseEntity.ok(result);
    }
}