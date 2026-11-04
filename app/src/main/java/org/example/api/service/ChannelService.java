package org.example.api.service;

import org.example.api.exception.ExtractionException;
import org.example.api.exception.NewPipeExceptionMapper;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChannelService {
    private static final Logger logger = LoggerFactory.getLogger(ChannelService.class);

    public ChannelInfo getChannelInfo(String url) throws ExtractionException {
        try {
            ChannelInfo info = ChannelInfo.getInfo(url);
            logger.info("Extracting channel info: '{}'", info.getName());
            return info;
        } catch (Exception e) {
            throw NewPipeExceptionMapper.translate(e);
        }
    }
}