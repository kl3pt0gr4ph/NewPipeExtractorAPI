package org.example.api.service;

import java.io.StringWriter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.example.api.dto.dash.AudioStreamMetadataDTO;
import org.example.api.dto.dash.DashManifestConfigDTO;
import org.example.api.dto.dash.SubtitleMetadataDTO;
import org.example.api.dto.dash.VideoStreamMetadataDTO;
import org.example.api.utils.ManifestXmlBuilder;
import org.schabi.newpipe.extractor.stream.AudioTrackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Writes an on-demand DASH manifest describing every representation in the config.
 *
 * <p>Input contract: the config was built from streams that passed
 * {@link DashPlayabilityFilter}. This class never selects, drops or ranks streams. It
 * groups them into AdaptationSets and labels them (lang, Role, Label) so the player
 * can make those choices.</p>
 */
@Service
public class DashManifestGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(DashManifestGeneratorService.class);

    private static final String DASH_NAMESPACE = "urn:mpeg:dash:schema:mpd:2011";
    private static final String ROLE_SCHEME = "urn:mpeg:dash:role:2011";
    private static final String AUDIO_CHANNEL_SCHEME = "urn:mpeg:dash:23003:3:audio_channel_configuration:2011";
    private static final String UNDETERMINED_LANGUAGE = "und";
    private static final String ROLE_MAIN = "main";

    /** Configured once and never changed, so it is safe to share between requests. */
    private static final XMLOutputFactory XML_OUTPUT = XMLOutputFactory.newInstance();

    /**
     * Generates a complete DASH manifest XML from a pre-built configuration DTO.
     *
     * @param config the configuration used to generate the manifest
     * @return the generated DASH manifest as an XML string
     */
    public String generateManifestXml(DashManifestConfigDTO config) {
        validateConfig(config);

        StringWriter out = new StringWriter();
        try {
            XMLStreamWriter xml = XML_OUTPUT.createXMLStreamWriter(out);
            xml.writeStartDocument("UTF-8", "1.0");
            writeMpd(xml, config);
            xml.writeEndDocument();
            xml.close();
        } catch (XMLStreamException e) {
            throw new IllegalStateException("Failed to write DASH manifest", e);
        }
        return out.toString();
    }

    // --- Validation ------------------------------------------------------------

    /**
     * The config is built by {@code StreamingController} from an extraction, never from
     * client input, so every failure here is a bug: {@link IllegalStateException}, which the
     * catch-all reports as {@code 500 INTERNAL_ERROR}. A {@code 400} would blame the client.
     */
    private void validateConfig(DashManifestConfigDTO config) {
        if (config == null) {
            throw new IllegalStateException("Manifest configuration cannot be null");
        }
        if (config.durationSeconds() <= 0) {
            throw new IllegalStateException("Duration must be greater than 0");
        }

        // Past this point a failure is a bug in the filter or the DTO conversion,
        if (config.videoStreams().isEmpty() || config.audioStreams().isEmpty()) {
            throw new IllegalStateException("A manifest needs at least one video and one audio "
                    + "representation; the caller should have used the muxed fallback");
        }
        for (VideoStreamMetadataDTO v : config.videoStreams()) {
            requireNonBlank("video", v.id(), v.url(), v.mimeType(), v.codec(), v.initRange(), v.indexRange());
        }
        for (AudioStreamMetadataDTO a : config.audioStreams()) {
            requireNonBlank("audio", a.id(), a.url(), a.mimeType(), a.codec(), a.initRange(), a.indexRange());
        }
        for (SubtitleMetadataDTO s : config.subtitleStreams()) {
            requireNonBlank("subtitle", s.id(), s.url(), s.mimeType());
        }
    }

    private static void requireNonBlank(String kind, String id, String... fields) {
        boolean missing = isBlank(id);
        for (String field : fields) {
            missing |= isBlank(field);
        }
        if (missing) {
            throw new IllegalStateException("%s representation %s is missing a required field; ".formatted(kind, id)
                    + "DashPlayabilityFilter should have dropped it");
        }
    }

    // --- MPD and Period --------------------------------------------------------

    private void writeMpd(XMLStreamWriter xml, DashManifestConfigDTO config) throws XMLStreamException {
        xml.writeStartElement("MPD");
        xml.writeDefaultNamespace(DASH_NAMESPACE);
        xml.writeAttribute("type", config.type());
        xml.writeAttribute("mediaPresentationDuration", config.mediaPresentationDuration());
        xml.writeAttribute("minBufferTime", config.minBufferTime());
        xml.writeAttribute("profiles", config.profiles());

        xml.writeStartElement("Period");
        xml.writeAttribute("duration", config.mediaPresentationDuration());

        // One counter across all AdaptationSets
        int nextId = 0;
        nextId = writeVideoAdaptationSets(xml, config.videoStreams(), nextId);
        nextId = writeAudioAdaptationSets(xml, config.audioStreams(), nextId);
        nextId = writeSubtitleAdaptationSets(xml, config.subtitleStreams(), nextId);

        xml.writeEndElement(); // Period
        xml.writeEndElement(); // MPD

        logger.debug(
                "Wrote {} AdaptationSet(s) for {} video, {} audio, {} subtitle representation(s)",
                nextId,
                config.videoStreams().size(),
                config.audioStreams().size(),
                config.subtitleStreams().size());
    }

    // --- Video -----------------------------------------------------------------

    /** One AdaptationSet per container and codec family; players can't switch codecs mid-set. */
    private int writeVideoAdaptationSets(XMLStreamWriter xml, List<VideoStreamMetadataDTO> videos, int nextId)
            throws XMLStreamException {
        Map<String, List<VideoStreamMetadataDTO>> byCodec =
                groupBy(videos, v -> v.mimeType() + "|" + codecFamily(v.codec()));

        for (List<VideoStreamMetadataDTO> group : byCodec.values()) {
            xml.writeStartElement("AdaptationSet");
            xml.writeAttribute("id", String.valueOf(nextId++));
            xml.writeAttribute("contentType", "video");
            xml.writeAttribute("mimeType", group.getFirst().mimeType());
            xml.writeAttribute("subsegmentAlignment", "true");
            xml.writeAttribute("startWithSAP", "1");

            List<VideoStreamMetadataDTO> sorted = group.stream()
                    .sorted(Comparator.comparingInt(VideoStreamMetadataDTO::height)
                            .reversed())
                    .toList();
            for (VideoStreamMetadataDTO video : sorted) {
                writeVideoRepresentation(xml, video);
            }

            xml.writeEndElement(); // AdaptationSet
        }
        return nextId;
    }

    private void writeVideoRepresentation(XMLStreamWriter xml, VideoStreamMetadataDTO video) throws XMLStreamException {
        xml.writeStartElement("Representation");
        xml.writeAttribute("id", video.id());
        xml.writeAttribute("bandwidth", String.valueOf(video.bandwidth()));
        xml.writeAttribute("codecs", video.codec());
        xml.writeAttribute("width", String.valueOf(video.width()));
        xml.writeAttribute("height", String.valueOf(video.height()));
        if (!isBlank(video.frameRate())) {
            xml.writeAttribute("frameRate", video.frameRate());
        }
        writeBaseUrl(xml, video.url());
        writeSegmentBase(xml, video.initRange(), video.indexRange());
        xml.writeEndElement(); // Representation
    }

    // --- Audio -----------------------------------------------------------------

    /**
     * Groups by track first (original, each dub, descriptive), then by container and codec
     * family inside each track. Every AdaptationSet of one track carries the same lang,
     * Role and Label, so the player sees one track offered in two codecs.
     */
    private int writeAudioAdaptationSets(XMLStreamWriter xml, List<AudioStreamMetadataDTO> audios, int nextId)
            throws XMLStreamException {
        Map<String, List<AudioStreamMetadataDTO>> byTrack =
                groupBy(audios, DashManifestGeneratorService::audioTrackKey);
        boolean singleTrack = byTrack.size() == 1;

        for (List<AudioStreamMetadataDTO> track : byTrack.values()) {
            AudioStreamMetadataDTO first = track.getFirst();
            String language = languageOrUnd(first.language());
            String role = audioRole(first.audioTrackType(), singleTrack);
            String label = audioLabel(first, language, role);

            Map<String, List<AudioStreamMetadataDTO>> byCodec =
                    groupBy(track, a -> a.mimeType() + "|" + codecFamily(a.codec()));

            for (List<AudioStreamMetadataDTO> group : byCodec.values()) {
                xml.writeStartElement("AdaptationSet");
                xml.writeAttribute("id", String.valueOf(nextId++));
                xml.writeAttribute("contentType", "audio");
                xml.writeAttribute("mimeType", group.getFirst().mimeType());
                xml.writeAttribute("lang", language);
                xml.writeAttribute("subsegmentAlignment", "true");
                xml.writeAttribute("startWithSAP", "1");
                writeRole(xml, role);
                writeLabel(xml, label);

                List<AudioStreamMetadataDTO> sorted = group.stream()
                        .sorted(Comparator.comparingInt(AudioStreamMetadataDTO::bandwidth)
                                .reversed())
                        .toList();
                for (AudioStreamMetadataDTO audio : sorted) {
                    writeAudioRepresentation(xml, audio);
                }

                xml.writeEndElement(); // AdaptationSet
            }
        }
        return nextId;
    }

    private void writeAudioRepresentation(XMLStreamWriter xml, AudioStreamMetadataDTO audio) throws XMLStreamException {
        xml.writeStartElement("Representation");
        xml.writeAttribute("id", audio.id());
        xml.writeAttribute("bandwidth", String.valueOf(audio.bandwidth()));
        xml.writeAttribute("codecs", audio.codec());
        if (!isBlank(audio.audioSamplingRate())) {
            xml.writeAttribute("audioSamplingRate", audio.audioSamplingRate());
        }
        xml.writeEmptyElement("AudioChannelConfiguration");
        xml.writeAttribute("schemeIdUri", AUDIO_CHANNEL_SCHEME);
        xml.writeAttribute("value", String.valueOf(audio.audioChannels()));

        writeBaseUrl(xml, audio.url());
        writeSegmentBase(xml, audio.initRange(), audio.indexRange());
        xml.writeEndElement(); // Representation
    }

    /** Track id when YouTube provides one (multi-track videos), otherwise the language. */
    private static String audioTrackKey(AudioStreamMetadataDTO audio) {
        String trackId = audio.audioTrackId();
        return isBlank(trackId) ? "lang:" + languageOrUnd(audio.language()) : "track:" + trackId;
    }

    /**
     * DASH Role for an audio track. Player marks Role "main" as primary, and uses the primary
     * track when the viewer's preferred language matches nothing.
     */
    private static String audioRole(AudioTrackType type, boolean singleTrack) {
        if (type == null) {
            return singleTrack ? ROLE_MAIN : "alternate";
        }
        return switch (type) {
            case ORIGINAL -> ROLE_MAIN;
            case DUBBED -> "dub";
            case DESCRIPTIVE -> "description";
            default -> "alternate"; // SECONDARY, and anything NewPipe adds later
        };
    }

    /**
     * YouTube's track name when there is one. Without a name or a language, the main track
     * is the video's own audio, so it reads "Original"; any other such track is "Unknown".
     */
    private static String audioLabel(AudioStreamMetadataDTO audio, String language, String role) {
        if (!isBlank(audio.audioTrackName())) {
            return audio.audioTrackName();
        }
        if (UNDETERMINED_LANGUAGE.equals(language)) {
            return ROLE_MAIN.equals(role) ? "Original" : "Unknown";
        }
        if (!isBlank(audio.languageName())) {
            return audio.languageName();
        }
        return ManifestXmlBuilder.getLanguageName(language);
    }

    // --- Subtitles -------------------------------------------------------------

    /** One AdaptationSet per track, in the order the filter returned them. */
    private int writeSubtitleAdaptationSets(XMLStreamWriter xml, List<SubtitleMetadataDTO> subtitles, int nextId)
            throws XMLStreamException {
        for (SubtitleMetadataDTO subtitle : subtitles) {
            String language = languageOrUnd(subtitle.language());

            xml.writeStartElement("AdaptationSet");
            xml.writeAttribute("id", String.valueOf(nextId++));
            xml.writeAttribute("contentType", "text");
            xml.writeAttribute("mimeType", subtitle.mimeType());
            xml.writeAttribute("lang", language);
            writeRole(xml, textRole(subtitle.kind()));
            writeLabel(xml, subtitleLabel(subtitle, language));

            xml.writeStartElement("Representation");
            xml.writeAttribute("id", subtitle.id());
            xml.writeAttribute("bandwidth", String.valueOf(subtitle.bandwidth()));
            writeBaseUrl(xml, subtitle.url());
            xml.writeEndElement(); // Representation

            xml.writeEndElement(); // AdaptationSet
        }
        return nextId;
    }

    /** DASH role values are singular: "subtitle" and "caption". */
    private static String textRole(String kind) {
        return kind != null && kind.toLowerCase(Locale.ROOT).startsWith("caption") ? "caption" : "subtitle";
    }

    /** Auto-generated tracks get a suffix so the picker can tell them from manual ones. */
    private static String subtitleLabel(SubtitleMetadataDTO subtitle, String language) {
        String name = !isBlank(subtitle.languageName())
                ? subtitle.languageName()
                : ManifestXmlBuilder.getLanguageName(language);
        return "asr".equalsIgnoreCase(subtitle.kind()) ? name + " (auto-generated)" : name;
    }

    // --- Shared elements -------------------------------------------------------

    private static void writeRole(XMLStreamWriter xml, String value) throws XMLStreamException {
        xml.writeEmptyElement("Role");
        xml.writeAttribute("schemeIdUri", ROLE_SCHEME);
        xml.writeAttribute("value", value);
    }

    /** The MPD schema defines Label as a child element, not an attribute. */
    private static void writeLabel(XMLStreamWriter xml, String label) throws XMLStreamException {
        if (isBlank(label)) {
            return;
        }
        xml.writeStartElement("Label");
        xml.writeCharacters(label);
        xml.writeEndElement();
    }

    private static void writeBaseUrl(XMLStreamWriter xml, String url) throws XMLStreamException {
        xml.writeStartElement("BaseURL");
        xml.writeCharacters(url);
        xml.writeEndElement();
    }

    /** Always written: validation guarantees both ranges are present. */
    private static void writeSegmentBase(XMLStreamWriter xml, String initRange, String indexRange)
            throws XMLStreamException {
        xml.writeStartElement("SegmentBase");
        xml.writeAttribute("indexRange", indexRange);
        xml.writeEmptyElement("Initialization");
        xml.writeAttribute("range", initRange);
        xml.writeEndElement(); // SegmentBase
    }

    // --- Helpers ---------------------------------------------------------------

    /** Groups in sorted key order, so the same input always produces the same manifest. */
    private static <T> Map<String, List<T>> groupBy(List<T> items, Function<T, String> key) {
        return items.stream().collect(Collectors.groupingBy(key, TreeMap::new, Collectors.toList()));
    }

    /** "avc1.640028" to "avc1", "vp09.00.51.08" to "vp09", "mp4a.40.2" to "mp4a". */
    private static String codecFamily(String codec) {
        return codec.trim().split("\\.")[0].toLowerCase(Locale.ROOT);
    }

    private static String languageOrUnd(String language) {
        return isBlank(language) ? UNDETERMINED_LANGUAGE : language;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
