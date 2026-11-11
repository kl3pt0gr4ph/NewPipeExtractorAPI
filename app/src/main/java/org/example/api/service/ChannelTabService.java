package org.example.api.service;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.example.api.dto.channels.ChannelTabDTO;
import org.example.api.exception.ExtractionException;
import org.example.api.exception.NewPipeExceptionMapper;
import org.example.api.exception.ValidationException;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for extracting paginated content from YouTube channel tabs via
 * NewPipe's {@link ChannelTabInfo} API.
 *
 * <p>Supported tab types (use the string constants from {@link ChannelTabs}):</p>
 * <ul>
 *   <li>{@code "videos"}      — regular uploads</li>
 *   <li>{@code "shorts"}      — YouTube Shorts</li>
 *   <li>{@code "livestreams"} — past and upcoming live streams</li>
 *   <li>{@code "playlists"}   — channel playlists</li>
 *   <li>{@code "albums"}      — releases, on music channels</li>
 * </ul>
 *
 * <p>Only the Videos tab's content arrives with the channel request; every other tab is a
 * second request to YouTube, made by {@code fetchPage()} and {@code getInitialPage()}.</p>
 */
@Service
public class ChannelTabService {

    private static final Logger logger = LoggerFactory.getLogger(ChannelTabService.class);

    /**
     * Fetches the first page of items from a given channel tab.
     */
    public ChannelTabDTO getChannelTab(String channelUrl, String tab, String channelId) throws ExtractionException {
        try {
            logger.info("Fetching channel tab '{}' for URL: {}", tab, channelUrl);

            StreamingService service = NewPipe.getServiceByUrl(channelUrl);
            ChannelInfo channelInfo = ChannelInfo.getInfo(channelUrl);

            ListLinkHandler tabHandler = findTabHandler(channelInfo.getTabs(), tab)
                    .orElseThrow(() -> tabNotFound(tab, channelInfo.getTabs()));

            ChannelTabExtractor extractor = service.getChannelTabExtractor(tabHandler);
            extractor.fetchPage();
            ChannelTabInfo tabInfo = ChannelTabInfo.getInfo(extractor);
            checkExtractionErrors(tab, channelUrl, tabInfo.getRelatedItems(), tabInfo.getErrors());

            logger.info(
                    "Fetched {} item(s) from tab '{}' (hasNextPage={})",
                    tabInfo.getRelatedItems().size(),
                    tab,
                    tabInfo.hasNextPage());

            return ChannelTabDTO.from(tabInfo, tab, channelId);

        } catch (Exception e) {
            throw NewPipeExceptionMapper.translate(e);
        }
    }

    /**
     * Fetches a subsequent page of channel tab items.
     *
     * <p>{@link ChannelTabExtractor} is obtained for the original query, initialized with
     * {@code fetchPage()} to establish the InnerTube session state, and then
     * {@code getPage(pageInstance)} is called with reconstructed {@link Page}.
     * Using {@code ChannelTabInfo.getMoreItems} without this initialization step skips
     * the extractor setup and results in empty pages</p>
     *
     * @param channelId   channel ID — for the response DTO
     * @param tab         tab type string matching the initial request
     * @param pageUrl     from {@code nextPage.url} in the previous response
     * @param pageBody    from {@code nextPage.body} in the previous response (Base64)
     * @param pageIds     from {@code nextPage.ids} in the previous response
     */
    public ChannelTabDTO getChannelTabPage(
            String channelId, String tab, String pageUrl, String pageBody, List<String> pageIds)
            throws ExtractionException {
        try {
            logger.debug("Fetching next page for channel tab '{}', channelId: {}", tab, channelId);

            byte[] bodyBytes = pageBody != null ? Base64.getDecoder().decode(pageBody) : null;
            Page pageInstance = new Page(pageUrl, null, pageIds, null, bodyBytes);

            String channelUrl = pageIds != null && pageIds.size() >= 2 ? pageIds.get(1) : null;

            if (channelUrl == null) {
                throw new ValidationException(
                        "pageIds",
                        "missing the channel URL at index 1; pass nextPage.ids from the previous "
                                + "response unchanged");
            }

            StreamingService service = NewPipe.getServiceByUrl(channelUrl);
            ChannelInfo channelInfo = ChannelInfo.getInfo(channelUrl);
            ListLinkHandler tabHandler = findTabHandler(channelInfo.getTabs(), tab)
                    .orElseThrow(() -> tabNotFound(tab, channelInfo.getTabs()));

            ChannelTabExtractor extractor = service.getChannelTabExtractor(tabHandler);
            extractor.fetchPage();

            InfoItemsPage<InfoItem> page = extractor.getPage(pageInstance);
            checkExtractionErrors(tab, channelUrl, page.getItems(), page.getErrors());

            logger.info(
                    "Fetched {} item(s) from tab '{}' page (hasNextPage={})",
                    page.getItems().size(),
                    tab,
                    page.hasNextPage());

            return ChannelTabDTO.fromPage(page, tab, channelId);

        } catch (Exception e) {
            throw NewPipeExceptionMapper.translate(e);
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    /**
     * NewPipe doesn't throw when a tab's items fail to load: {@code ChannelTabInfo.getInfo}
     * catches the failure, records it in {@code getErrors()} and returns an empty page, and the
     * item collectors record items they can't parse the same way. Unchecked, a broken tab looks
     * exactly like an empty one.
     */
    private void checkExtractionErrors(
            String tab, String channelUrl, List<? extends InfoItem> items, List<Throwable> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }
        Throwable first = errors.getFirst();
        if (items == null || items.isEmpty()) {
            throw NewPipeExceptionMapper.translate(first);
        }
        logger.warn(
                "Tab '{}' for {} returned {} item(s) with {} extraction error(s); first: {}",
                tab,
                channelUrl,
                items.size(),
                errors.size(),
                first.toString());
    }

    private Optional<ListLinkHandler> findTabHandler(List<ListLinkHandler> tabs, String tab) {
        return tabs.stream()
                .filter(t -> t.getContentFilters().stream().anyMatch(f -> f.equalsIgnoreCase(tab)))
                .findFirst();
    }

    private ValidationException tabNotFound(String tab, List<ListLinkHandler> tabs) {
        return new ValidationException(
                "tab", "this channel has no '%s' tab. Available tabs: %s".formatted(tab, describeAvailableTabs(tabs)));
    }

    private String describeAvailableTabs(List<ListLinkHandler> tabs) {
        return tabs.stream()
                .flatMap(t -> t.getContentFilters().stream())
                .toList()
                .toString();
    }
}
