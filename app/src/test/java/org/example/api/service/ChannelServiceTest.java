package org.example.api.service;

import org.example.api.exception.ErrorCode;
import org.example.api.exception.ExtractionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.exceptions.AccountTerminatedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Unit tests for ChannelService.
 */
@DisplayName("ChannelService Tests")
class ChannelServiceTest {

    private ChannelService channelService;

    @BeforeEach
    void setUp() {
        channelService = new ChannelService();
    }

    @Test
    @DisplayName("Returns the ChannelInfo extracted for the requested URL")
    void returnsChannelInfoForRequestedUrl() throws Exception {
        String url = "https://www.youtube.com/@LinusTechTips";
        ChannelInfo mockChannelInfo = mock(ChannelInfo.class);

        try (MockedStatic<ChannelInfo> channelInfoMock = mockStatic(ChannelInfo.class)) {
            channelInfoMock.when(() -> ChannelInfo.getInfo(url)).thenReturn(mockChannelInfo);

            ChannelInfo result = channelService.getChannelInfo(url);

            assertSame(mockChannelInfo, result);
            channelInfoMock.verify(() -> ChannelInfo.getInfo(url));
        }
    }

    @Test
    @DisplayName("Translates NewPipe failures, keeping the thrown exception as cause and its text out of the message")
    void translatesFailures() {
        String url = "https://www.youtube.com/@LinusTechTips";
        AccountTerminatedException thrown = new AccountTerminatedException("This account has been terminated");

        try (MockedStatic<ChannelInfo> channelInfoMock = mockStatic(ChannelInfo.class)) {
            channelInfoMock.when(() -> ChannelInfo.getInfo(url)).thenThrow(thrown);

            ExtractionException ex = assertThrows(ExtractionException.class,
                    () -> channelService.getChannelInfo(url));

            assertEquals(ErrorCode.ACCOUNT_TERMINATED, ex.getCode());
            assertEquals(ErrorCode.ACCOUNT_TERMINATED.defaultMessage(), ex.getMessage());
            assertSame(thrown, ex.getCause());
        }
    }

    @Test
    @DisplayName("Lets unexpected runtime exceptions through for the catch-all")
    void passesThroughUnexpectedExceptions() {
        String url = "https://www.youtube.com/@LinusTechTips";
        NullPointerException bug = new NullPointerException("bug");

        try (MockedStatic<ChannelInfo> channelInfoMock = mockStatic(ChannelInfo.class)) {
            channelInfoMock.when(() -> ChannelInfo.getInfo(url)).thenThrow(bug);

            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> channelService.getChannelInfo(url));

            assertSame(bug, ex);
        }
    }
}