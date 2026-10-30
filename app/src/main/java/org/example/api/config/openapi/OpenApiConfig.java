package org.example.api.config.openapi;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.example.api.config.GlobalExceptionHandler;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.example.api.config.openapi.OpenApiDocs.*;

/**
 * Metadata and post-processing for the OpenAPI document springdoc generates.
 *
 * <p>Endpoint and schema details live as annotations on the controllers and DTOs;
 * this class only adds what applies to the whole API.</p>
 */
@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", matchIfMissing = true)
public class OpenApiConfig {

    static final String REPO_URL = "https://github.com/kl3pt0gr4ph/NewPipeExtractorAPI";

    static {
        // NewPipeExtractor's Info and InfoItemsPage types carry a List<Throwable> of
        // non-fatal errors. Left alone, the schema walks the whole Throwable graph
        // (stack traces, causes, suppressed); describe it as an opaque object instead.
        SpringDocUtils.getConfig().replaceWithSchema(Throwable.class, new ObjectSchema()
                .description("A non-fatal error NewPipeExtractor recorded during extraction, "
                        + "serialised from a Java exception."));
    }

    private static final String DESCRIPTION = """
            REST API over [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) \
            for YouTube metadata, search, channels, playlists, comments and DASH manifests.

            **Conventions**

            - Every endpoint is a `GET` under `/api/v1` and returns JSON unless noted.
            - Paginated responses carry a `nextPage` object, or none on the last page. Pass its \
            fields back **verbatim** to the matching `/page` endpoint: YouTube's continuation \
            tokens only work unmodified.
            - Errors share the `ErrorResponse` shape; branch on `errorCode`, not on `message`.
            - Some endpoints return NewPipeExtractor's own types unmapped. Their descriptions say \
            so; those shapes follow the library and can change when it is upgraded.

            The API is designed to run on a private network behind the OpenTube frontend.""";

    @Bean
    public OpenAPI openApi(ObjectProvider<BuildProperties> buildProperties, OpenApiProperties properties) {
        String version = buildProperties.stream()
                .map(BuildProperties::getVersion)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("dev");

        OpenAPI openApi = new OpenAPI()
                .info(new Info()
                        .title("NewPipeExtractorAPI")
                        .version(version)
                        .description(DESCRIPTION))
                .externalDocs(new ExternalDocumentation()
                        .description("README")
                        .url(REPO_URL + "#readme"))
                .tags(List.of(
                        new Tag().name(TAG_STREAMS).description(
                                "Metadata, stream lists and DASH manifests for one video. "
                                        + "All of these share one cached extraction per video."),
                        new Tag().name(TAG_SEARCH).description("Search and its continuation pages"),
                        new Tag().name(TAG_CHANNELS).description("Channel info and tab contents"),
                        new Tag().name(TAG_PLAYLISTS).description("Playlist info and its continuation pages"),
                        new Tag().name(TAG_COMMENTS).description("Video comments and their continuation pages"),
                        new Tag().name(TAG_KIOSKS).description("YouTube's curated feeds, such as trending music"),
                        new Tag().name(TAG_SERVICES).description("Streaming services NewPipeExtractor supports")
                ));

        String serverUrl = properties.serverUrl();
        if (serverUrl != null && !serverUrl.isBlank()) {
            openApi.servers(List.of(new Server().url(serverUrl.strip())));
        }
        return openApi;
    }

    /**
     * {@link GlobalExceptionHandler} declares a generic {@code 400} response, which
     * springdoc adds to every operation. Drop it where an operation takes no
     * parameters, since there is nothing for a client to get wrong.
     */
    @Bean
    public OpenApiCustomizer removeBadRequestFromParameterlessOperations() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().stream()
                    .flatMap(path -> Stream.of(path.getGet(), path.getPost(), path.getPut(),
                            path.getPatch(), path.getDelete()))
                    .filter(Objects::nonNull)
                    .filter(OpenApiConfig::hasNoParameters)
                    .forEach(op -> {
                        if (op.getResponses() != null) {
                            op.getResponses().remove("400");
                        }
                    });
        };
    }

    private static boolean hasNoParameters(Operation operation) {
        return (operation.getParameters() == null || operation.getParameters().isEmpty())
                && operation.getRequestBody() == null;
    }
}