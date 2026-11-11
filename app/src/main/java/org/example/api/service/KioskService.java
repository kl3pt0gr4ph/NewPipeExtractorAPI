package org.example.api.service;

import java.util.List;
import org.example.api.exception.ApiException;
import org.example.api.exception.ErrorCode;
import org.example.api.exception.ExtractionException;
import org.example.api.exception.NewPipeExceptionMapper;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class KioskService {
    private static final Logger logger = LoggerFactory.getLogger(KioskService.class);

    public List<String> getKioskIds(int serviceId) throws ExtractionException {
        try {
            List<String> kioskIds =
                    List.copyOf(NewPipe.getService(serviceId).getKioskList().getAvailableKiosks());
            logger.info("Extracted {} kiosk ID(s)", kioskIds.size());
            return kioskIds;
        } catch (Exception e) {
            throw NewPipeExceptionMapper.translate(e);
        }
    }

    public KioskInfo getKioskInfo(int serviceId, String kioskId) throws ExtractionException {
        try {
            logger.debug("Extracting kiosk info for kioskId: {}", kioskId);

            StreamingService service = NewPipe.getService(serviceId);
            KioskList kiosks = service.getKioskList();

            // NewPipe reports an unknown ID as a bare ExtractionException, which would map to
            // EXTRACTION_ERROR. It's the client asking for something that doesn't exist.
            if (!kiosks.getAvailableKiosks().contains(kioskId)) {
                throw new ApiException(
                        ErrorCode.NOT_FOUND,
                        "No kiosk '%s'. Available kiosks: %s"
                                .formatted(
                                        kioskId,
                                        kiosks.getAvailableKiosks().stream()
                                                .sorted()
                                                .toList()));
            }

            KioskExtractor<?> extractor = kiosks.getExtractorById(kioskId, null);
            extractor.fetchPage();

            KioskInfo info = KioskInfo.getInfo(extractor);

            logger.info(
                    "Extracted {} item(s) from kiosk '{}'",
                    info.getRelatedItems().size(),
                    kioskId);

            return info;
        } catch (Exception e) {
            throw NewPipeExceptionMapper.translate(e);
        }
    }
}
