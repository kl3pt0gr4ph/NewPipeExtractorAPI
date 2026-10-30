package org.example.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.api.dto.kiosk.KioskDTO;
import org.example.api.service.KioskService;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.example.api.config.openapi.OpenApiDocs.*;

@Tag(name = TAG_KIOSKS)
@RestController
@RequestMapping("/api/v1/kiosks")
public class KioskController {
    private static final Logger logger = LoggerFactory.getLogger(KioskController.class);
    private static final int YOUTUBE_SERVICE_ID = 0;
    private final KioskService kioskService;


    @Autowired
    public KioskController(KioskService kioskService) {
        this.kioskService = kioskService;
    }

    /**
     * Returns the kiosk IDs available for a service,
     */
    @Operation(summary = "Kiosk IDs", description = "The kiosks YouTube offers, for use with `/kiosks/{kioskId}`.")
    @ApiResponse(responseCode = "200", description = "Kiosk IDs",
            content = @Content(mediaType = "application/json", array = @ArraySchema(
                    schema = @Schema(type = "string", example = "trending_music"))))
    @GetMapping
    public ResponseEntity<List<String>> getKioskIds() {

        List<String> kioskIds = kioskService.getKioskIds(YOUTUBE_SERVICE_ID);
        return ResponseEntity.ok(kioskIds);
    }

    /**
     * Returns kiosk metadata and the initial page of streams.
     */
    @Operation(summary = "Kiosk feed",
            description = "A kiosk's name and its items. Kiosks are a single page. The category kiosks "
                    + "work; the general `Trending` kiosk fails because YouTube retired that page.")
    @GetMapping("/{kioskId}")
    public ResponseEntity<KioskDTO> getKioskInfo(
            @Parameter(description = "A kiosk ID from `/kiosks`", example = "trending_music")
            @PathVariable(name = "kioskId") String kioskId
    ) {
        logger.debug("Retrieving kiosk info for kioskId: {}", kioskId);

        KioskInfo info = kioskService.getKioskInfo(YOUTUBE_SERVICE_ID, kioskId);
        return ResponseEntity.ok(KioskDTO.from(info));
    }
}