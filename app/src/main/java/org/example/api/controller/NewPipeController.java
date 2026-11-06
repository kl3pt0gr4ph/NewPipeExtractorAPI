package org.example.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.api.dto.ServiceDTO;
import org.example.api.service.RestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.example.api.config.openapi.OpenApiDocs.*;

/**
 * REST controller for handling API requests for YouTube information from New Pipe Extractor.
 *
 * This class defines endpoints for fetching various types of stream data, such as
 * audio streams, video streams, subtitles, and more, from YouTube using a specified ID.
 * Utilizes the RestService to abstract the logic of interacting with YouTube.
 * All endpoints are prefixed with "/api/v1".
 *
 * <p>
 * The following constants and fields are defined:
 * </p>
 * <ul>
 *    <li><b>YOUTUBE_URL</b>: A constant URL string that serves as the base for constructing
 *        full URLs to YouTube videos based on their IDs.</li>
 *    <li><b>logger</b>: A logger instance for logging events and errors within the controller.</li>
 *    <li><b>restService</b>: An instance of RestService used to interact with YouTube for
 *        retrieving stream data.</li>
 * </ul>
 *
 * <p>
 * The constructor uses Spring's dependency injection to initialize the RestService instance.
 * </p>
 */
@Tag(name = TAG_SERVICES)
@RestController
@RequestMapping("/api/v1")
public class NewPipeController {
    private static final Logger logger = LoggerFactory.getLogger(NewPipeController.class);
    private final RestService restService;

    @Autowired
    public NewPipeController(RestService restService) {
        this.restService = restService;
    }

    /**
     * Lists the streaming services NewPipeExtractor supports.
     */
    @Operation(summary = "Supported services",
            description = "The streaming services NewPipeExtractor supports, with their IDs. "
                    + "The rest of this API only queries YouTube (service 0).")
    @ApiResponse(responseCode = "200", description = "Services",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ServiceDTO.class)),
                    examples = @ExampleObject(value = """
                            [
                              { "serviceId": 0, "serviceName": "YouTube" },
                              { "serviceId": 1, "serviceName": "SoundCloud" }
                            ]""")))
    @GetMapping("/services")
    public ResponseEntity<List<ServiceDTO>> getServices() {
        logger.debug("Retrieving services");
        return ResponseEntity.ok(restService.getServices());
    }
}