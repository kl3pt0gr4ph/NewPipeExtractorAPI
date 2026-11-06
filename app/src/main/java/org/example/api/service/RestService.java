package org.example.api.service;

import org.example.api.dto.ServiceDTO;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestService {
    private static final Logger logger = LoggerFactory.getLogger(RestService.class);

    /**
     * Lists the streaming services NewPipeExtractor supports.
     *
     * @throws IllegalStateException if NewPipe has no services, which means it wasn't
     *                               initialised: reported as {@code 500 INTERNAL_ERROR}
     */
    public List<ServiceDTO> getServices() {
        List<StreamingService> services = NewPipe.getServices();
        if (services == null || services.isEmpty()) {
            throw new IllegalStateException("NewPipe services are not initialized or empty");
        }

        List<ServiceDTO> result = services.stream()
                .map(service -> new ServiceDTO(service.getServiceId(), service.getServiceInfo().getName()))
                .toList();

        logger.info("Listed {} service(s)", result.size());
        return result;
    }
}