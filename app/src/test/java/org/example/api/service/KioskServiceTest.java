package org.example.api.service;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.example.api.exception.ApiException;
import org.example.api.exception.ErrorCode;
import org.example.api.exception.ExtractionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.extractor.kiosk.KioskList;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for KioskService.
 */
@DisplayName("KioskService Tests")
class KioskServiceTest {

    private static final int SERVICE_ID = 0;
    private static final String KIOSK_ID = "trending_gaming";

    private final KioskService kioskService = new KioskService();

    // ── getKioskIds ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Returns the available kiosk IDs from the streaming service's kiosk list")
    void getKioskIds_returnsAvailableKioskIds() throws Exception {
        StreamingService service = mock(StreamingService.class);
        KioskList kioskList = mock(KioskList.class);
        when(service.getKioskList()).thenReturn(kioskList);
        when(kioskList.getAvailableKiosks()).thenReturn(Set.of("trending_gaming", "live"));

        try (MockedStatic<NewPipe> newPipe = mockStatic(NewPipe.class)) {
            newPipe.when(() -> NewPipe.getService(SERVICE_ID)).thenReturn(service);

            List<String> result = kioskService.getKioskIds(SERVICE_ID);

            assertThat(result).containsExactlyInAnyOrder("trending_gaming", "live");
        }
    }

    // ── getKioskInfo ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Fetches the page and returns the KioskInfo built from the extractor")
    void getKioskInfo_returnsKioskInfoFromExtractor() throws Exception {
        StreamingService service = mock(StreamingService.class);
        KioskList kioskList = mock(KioskList.class);
        KioskExtractor<?> extractor = mock(KioskExtractor.class);
        KioskInfo expectedInfo = mock(KioskInfo.class);

        when(service.getKioskList()).thenReturn(kioskList);
        when(kioskList.getAvailableKiosks()).thenReturn(Set.of(KIOSK_ID));
        when(kioskList.getExtractorById(KIOSK_ID, null)).thenReturn(extractor);

        try (MockedStatic<NewPipe> newPipe = mockStatic(NewPipe.class);
             MockedStatic<KioskInfo> kioskInfoStatic = mockStatic(KioskInfo.class)) {
            newPipe.when(() -> NewPipe.getService(SERVICE_ID)).thenReturn(service);
            kioskInfoStatic.when(() -> KioskInfo.getInfo(extractor)).thenReturn(expectedInfo);

            KioskInfo result = kioskService.getKioskInfo(SERVICE_ID, KIOSK_ID);

            assertThat(result).isSameAs(expectedInfo);
            verify(extractor).fetchPage();
        }
    }

    @Test
    @DisplayName("An unknown kiosk ID is 404 NOT_FOUND listing the available kiosks, without calling the extractor")
    void getKioskInfo_unknownKiosk_isNotFound() throws Exception {
        StreamingService service = mock(StreamingService.class);
        KioskList kioskList = mock(KioskList.class);
        when(service.getKioskList()).thenReturn(kioskList);
        when(kioskList.getAvailableKiosks()).thenReturn(Set.of("trending_music", "trending_gaming"));

        try (MockedStatic<NewPipe> newPipe = mockStatic(NewPipe.class)) {
            newPipe.when(() -> NewPipe.getService(SERVICE_ID)).thenReturn(service);

            assertThatThrownBy(() -> kioskService.getKioskInfo(SERVICE_ID, "Trending"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Trending")
                    .hasMessageContaining("trending_gaming, trending_music")
                    .extracting(e -> ((ApiException) e).getCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
            verify(kioskList, never()).getExtractorById(anyString(), any());
        }
    }

    // ── Exception translation ─────────────────────────────────────────────

    static Stream<Arguments> serviceCalls() {
        KioskService service = new KioskService();
        return Stream.of(
                Arguments.of("getKioskIds",
                        (ThrowingCallable) () -> service.getKioskIds(SERVICE_ID)),
                Arguments.of("getKioskInfo",
                        (ThrowingCallable) () -> service.getKioskInfo(SERVICE_ID, KIOSK_ID)));
    }

    @ParameterizedTest(name = "{0} translates NewPipe failures")
    @MethodSource("serviceCalls")
    @DisplayName("Translates NewPipe failures, keeping the original as cause and its text out of the message")
    void translatesExtractorFailures(String methodName, ThrowingCallable call) {
        ParsingException cause = new ParsingException("Could not get Trending name");

        try (MockedStatic<NewPipe> newPipe = mockStatic(NewPipe.class)) {
            newPipe.when(() -> NewPipe.getService(SERVICE_ID)).thenThrow(cause);

            assertThatThrownBy(call)
                    .isInstanceOf(ExtractionException.class)
                    .hasMessage(ErrorCode.UPSTREAM_CHANGED.defaultMessage())
                    .hasCause(cause)
                    .extracting(e -> ((ApiException) e).getCode())
                    .isEqualTo(ErrorCode.UPSTREAM_CHANGED);
        }
    }
}