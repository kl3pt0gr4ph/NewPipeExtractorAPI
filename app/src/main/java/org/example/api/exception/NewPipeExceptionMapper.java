package org.example.api.exception;

import org.schabi.newpipe.extractor.exceptions.AccountTerminatedException;
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.GeographicRestrictionException;
import org.schabi.newpipe.extractor.exceptions.PaidContentException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.exceptions.PrivateContentException;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.exceptions.SignInConfirmNotBotException;
import org.schabi.newpipe.extractor.exceptions.SoundCloudGoPlusContentException;
import org.schabi.newpipe.extractor.exceptions.UnsupportedContentInCountryException;
import org.schabi.newpipe.extractor.exceptions.YoutubeMusicPremiumContentException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

/**
 * Turns NewPipeExtractor's exceptions into {@link ApiException}s. This is the one place
 * the mapping from library exception to {@link ErrorCode} lives.
 *
 * <p>Services wrap their NewPipe calls like this:</p>
 * <pre>{@code
 * try {
 *     return ChannelInfo.getInfo(url);
 * } catch (Exception e) {
 *     throw NewPipeExceptionMapper.translate(e);
 * }
 * }</pre>
 *
 * <p>NewPipe's restriction exceptions all extend {@link ContentNotAvailableException}, which
 * extends {@link ParsingException}, so the tables are ordered most specific first and
 * matched with {@code instanceof}.</p>
 */
public final class NewPipeExceptionMapper {

    /** Exceptions that say exactly what happened. The first match anywhere in the cause chain wins. */
    private static final List<Map.Entry<Class<? extends Throwable>, ErrorCode>> SPECIFIC = List.of(
            entry(PrivateContentException.class, ErrorCode.CONTENT_PRIVATE),
            entry(AgeRestrictedContentException.class, ErrorCode.AGE_RESTRICTED),
            entry(GeographicRestrictionException.class, ErrorCode.GEO_RESTRICTED),
            // Thrown when a kiosk (e.g. trending_music) isn't offered in the server's country.
            entry(UnsupportedContentInCountryException.class, ErrorCode.GEO_RESTRICTED),
            entry(PaidContentException.class, ErrorCode.PAID_CONTENT),
            entry(YoutubeMusicPremiumContentException.class, ErrorCode.PAID_CONTENT),
            entry(SoundCloudGoPlusContentException.class, ErrorCode.PAID_CONTENT),
            entry(AccountTerminatedException.class, ErrorCode.ACCOUNT_TERMINATED),
            // After its subclasses above.
            entry(ContentNotAvailableException.class, ErrorCode.CONTENT_UNAVAILABLE),
            // Before ParsingException, which it extends.
            entry(SignInConfirmNotBotException.class, ErrorCode.UPSTREAM_BLOCKED),
            entry(ReCaptchaException.class, ErrorCode.UPSTREAM_BLOCKED));

    /**
     * Fallbacks, used only when nothing in the chain matches {@link #SPECIFIC}. Matched
     * against the outermost exception first.
     */
    private static final List<Map.Entry<Class<? extends Throwable>, ErrorCode>> GENERAL = List.of(
            entry(ParsingException.class, ErrorCode.UPSTREAM_CHANGED),
            entry(IOException.class, ErrorCode.UPSTREAM_UNREACHABLE),
            entry(org.schabi.newpipe.extractor.exceptions.ExtractionException.class, ErrorCode.EXTRACTION_ERROR));

    private static final int MAX_DEPTH = 16;

    private NewPipeExceptionMapper() {}

    /**
     * Returns the exception a service should throw in place of {@code thrown}.
     *
     * <ul>
     *   <li>An {@link ApiException} anywhere in the chain is returned unchanged, so code
     *       inside a {@code try} can throw its own.</li>
     *   <li>A NewPipe exception (or {@link IOException}) becomes an {@link ExtractionException}
     *       with the matching code, its default message, and {@code thrown} as the cause.</li>
     *   <li>Any other {@link RuntimeException} is returned unchanged: it's a bug, and the
     *       catch-all reports it as {@code INTERNAL_ERROR}.</li>
     *   <li>Any other checked exception is wrapped as {@code INTERNAL_ERROR}.</li>
     * </ul>
     */
    public static RuntimeException translate(Throwable thrown) {
        ErrorCode fallback = null;

        Throwable current = thrown;
        for (int depth = 0; current != null && depth < MAX_DEPTH; depth++) {
            if (current instanceof ApiException api) {
                return api;
            }
            ErrorCode specific = match(SPECIFIC, current);
            if (specific != null) {
                return new ExtractionException(specific, thrown);
            }
            if (fallback == null) {
                fallback = match(GENERAL, current);
            }
            current = current.getCause() == current ? null : current.getCause();
        }

        if (fallback != null) {
            return new ExtractionException(fallback, thrown);
        }
        if (thrown instanceof RuntimeException runtime) {
            return runtime;
        }
        return new ApiException(ErrorCode.INTERNAL_ERROR, thrown);
    }

    private static ErrorCode match(List<Map.Entry<Class<? extends Throwable>, ErrorCode>> table,
                                   Throwable t) {
        for (Map.Entry<Class<? extends Throwable>, ErrorCode> rule : table) {
            if (rule.getKey().isInstance(t)) {
                return rule.getValue();
            }
        }
        return null;
    }
}