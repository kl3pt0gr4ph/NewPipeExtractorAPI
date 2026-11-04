package org.example.api.service;

import org.example.api.exception.ExtractionException;
import org.example.api.exception.NewPipeExceptionMapper;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.playlist.PlaylistInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PlaylistService {
    private static final Logger logger = LoggerFactory.getLogger(PlaylistService.class);

    public PlaylistInfo getPlaylistInfo(String url) throws ExtractionException {
        try {
            PlaylistInfo info = PlaylistInfo.getInfo(url);
            logger.info("Extracted playlist '{}' with {} item(s)", info.getName(), info.getRelatedItems().size());
            return info;
        } catch (Exception e) {
            throw NewPipeExceptionMapper.translate(e);
        }
    }

    public InfoItemsPage<StreamInfoItem> getPlaylistPage( String url, String pageUrl) {
        try {
            logger.debug("Extracting playlist page for URL: {}", url);

            StreamingService service = NewPipe.getServiceByUrl(url);
            Page pageInstance = new Page(pageUrl);
            InfoItemsPage<StreamInfoItem> page = PlaylistInfo.getMoreItems(service, url, pageInstance);

            logger.info("Extracted playlist page with {} item(s) (hasNextPage={})",
                    page.getItems().size(), page.hasNextPage());
            return page;
        } catch (Exception e) {
            throw NewPipeExceptionMapper.translate(e);
        }
    }

}