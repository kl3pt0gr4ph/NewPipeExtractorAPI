package org.example.api.utils;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.springframework.stereotype.Component;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ValueSerializer;

/**
 * Utility class for custom JSON serialization of complex objects.
 *
 * This class provides specialized serialization methods to handle
 * complex objects from the NewPipe extractor, ensuring safe and
 * controlled JSON conversion.
 */
@Component
public class SerializationUtils {
    private final ObjectMapper objectMapper;

    public SerializationUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Custom JSON serializer for SearchInfo objects.
     *
     * This serializer provides a controlled way to convert SearchInfo
     * objects to JSON, limiting the depth and including only essential
     * information.
     */
    public static class CustomSearchInfoSerializer extends ValueSerializer<SearchInfo> {

        /**
         * Serialize a SearchInfo object with minimal, essential information.
         *
         * @param value The SearchInfo object to serialize
         * @param gen JsonGenerator for writing JSON content
         * @param ctxt Serialization context
         */
        @Override
        public void serialize(SearchInfo value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeStartObject();
            gen.writeStringProperty("searchString", value.getSearchString());
            gen.writeNumberProperty("serviceId", value.getService() != null ? value.getService().getServiceId() : -1);

            // Carefully serialize items
            gen.writeArrayPropertyStart("items");
            if (value.getRelatedItems() != null) {
                for (InfoItem item : value.getRelatedItems()) {
                    // Minimal serialization of items
                    gen.writeStartObject();
                    if (item != null) {
                        gen.writeStringProperty("name", item.getName());
                        gen.writeStringProperty("url", item.getUrl());
                        // Add other essential fields as needed
                    }
                    gen.writeEndObject();
                }
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
    }



    /**
     * Custom JSON serializer for StreamingService objects.
     *
     * This serializer provides a simplified JSON representation
     * of StreamingService, avoiding nested serialization.
     */
    public static class CustomStreamingServiceSerializer extends ValueSerializer<StreamingService> {

        /**
         * Serialize a StreamingService object with minimal information.
         *
         * @param value The StreamingService object to serialize
         * @param gen JsonGenerator for writing JSON content
         * @param ctxt Serialization context
         */
        @Override
        public void serialize(StreamingService value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeStartObject();
            gen.writeNumberProperty("serviceId", value.getServiceId());
            gen.writeEndObject();
        }
    }


    /**
     * Safely serialize an object to a JSON string.
     *
     * This method provides a robust way to convert objects to JSON,
     * with error handling and pretty-printing.
     *
     * @param obj The object to be serialized
     * @return A JSON string representation of the object
     */
    public String safeSerialize(Object obj) {
        try {
            ObjectWriter writer = objectMapper.writer()
                    .without(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                    .withDefaultPrettyPrinter();

            return writer.writeValueAsString(obj);
        } catch (Exception e) {
            System.err.printf("Serialization Error: %s%n", e.getMessage());
            // TODO: Replace printStackTrace with proper sl4j logging standard
            e.printStackTrace();
            return "{\"message\":\"Error serializing response\"}";
        }
    }
}
