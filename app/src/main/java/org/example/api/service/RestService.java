package org.example.api.service;

import org.example.api.model.Error;
import org.example.api.utils.SerializationUtils;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.springframework.stereotype.Service;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RestService {
    private final JsonMapper objectMapper;

    public RestService() {
        this.objectMapper = createCustomObjectMapper();
    }

    private JsonMapper createCustomObjectMapper() {
        return JsonMapper.builder()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addModule(new SimpleModule()
                        .addSerializer(SearchInfo.class, new SerializationUtils.CustomSearchInfoSerializer())
                        .addSerializer(StreamingService.class, new SerializationUtils.CustomStreamingServiceSerializer())
                )
                .build();
    }

    public String getServices() {
        try {
            // Verify NewPipe initialization
            if (NewPipe.getServices() == null || NewPipe.getServices().isEmpty()) {
                throw new IllegalStateException("NewPipe services are not initialized or empty");
            }

            List<StreamingService> services = NewPipe.getServices();
            System.out.printf("Found %d services%n", services.size());

            // Create a simplified list of services
            List<Map<String, Object>> serviceList = new ArrayList<>();
            for (StreamingService service : services) {
                Map<String, Object> serviceMap = new HashMap<>();
                serviceMap.put("serviceId", service.getServiceId());
                serviceMap.put("serviceName", service.getServiceInfo().getName());
                serviceList.add(serviceMap);
            }

            return objectMapper.writeValueAsString(serviceList);
        } catch (Exception e) {
            System.err.println("Services Extraction Error:");
            e.printStackTrace();
            return getError(e);
        }
    }

    public String getError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : "An error occurred";
        Error errorResponse = new Error(message);
        try {
            return objectMapper.writeValueAsString(errorResponse);
        } catch (Exception ex) {
            return "{\"message\":\"Error serializing error response\"}";
        }
    }
}