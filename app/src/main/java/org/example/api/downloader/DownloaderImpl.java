package org.example.api.downloader;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class DownloaderImpl extends Downloader {
    /**
     * Default User-Agent, sent unless NewPipe sets its own for a request (it does for the
     * mobile InnerTube clients). Matches NewPipeExtractor's own test downloader: the latest
     * Firefox ESR. YouTube can serve old browsers a different page layout, which NewPipe
     * then can't parse, so keep this current when bumping NewPipeExtractor.
     */
    public static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0";

    private static DownloaderImpl instance;
    private String mCookies;
    private final OkHttpClient client;

    private DownloaderImpl(OkHttpClient.Builder builder) {
        this.client = builder
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static DownloaderImpl init(@Nullable OkHttpClient.Builder builder) {
        return instance = new DownloaderImpl(builder != null ? builder : new OkHttpClient.Builder());
    }

    public static DownloaderImpl getInstance() {
        return instance;
    }

    public String getCookies() {
        return mCookies;
    }

    public void setCookies(String cookies) {
        mCookies = cookies;
    }

    private static boolean isEmpty(@Nullable String value) {
        return value == null || value.isEmpty();
    }

    public long getContentLength(String url) throws IOException {
        try {
            final Response response = head(url);
            String contentLength = response.getHeader("Content-Length");
            if (contentLength == null) {
                throw new IOException("Content-Length header is missing");
            }
            return Long.parseLong(contentLength);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid content length", e);
        } catch (ReCaptchaException e) {
            throw new IOException(e);
        }
    }

    public InputStream stream(String siteUrl) throws IOException {
        try {
            final okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                    .method("GET", null).url(siteUrl)
                    .addHeader("User-Agent", USER_AGENT);

            if (!isEmpty(mCookies)) {
                requestBuilder.addHeader("Cookie", mCookies);
            }

            final okhttp3.Request request = requestBuilder.build();
            final okhttp3.Response response = client.newCall(request).execute();
            final ResponseBody body = response.body();

            if (response.code() == 429) {
                response.close();
                throw new ReCaptchaException("reCaptcha Challenge requested", siteUrl);
            }

            return body.byteStream();
        } catch (ReCaptchaException e) {
            throw new IOException(e.getMessage(), e.getCause());
        }
    }
    @Override
    public Response execute(@NonNull Request request) throws IOException, ReCaptchaException {
        try {
            final String httpMethod = request.httpMethod();
            final String url = request.url();
            final Map<String, List<String>> headers = request.headers();
            final byte[] dataToSend = request.dataToSend();

            RequestBody requestBody = null;
            if (dataToSend != null) {
                requestBody = RequestBody.create(dataToSend, okhttp3.MediaType.parse("application/json"));
            } else if (httpMethod.equals("POST") || httpMethod.equals("PUT") || httpMethod.equals("PATCH")) {
                requestBody = RequestBody.create(new byte[0], okhttp3.MediaType.parse("application/json"));
            }

            final okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                    .method(httpMethod, requestBody).url(url)
                    .addHeader("User-Agent", USER_AGENT);

            if (!isEmpty(mCookies)) {
                requestBuilder.addHeader("Cookie", mCookies);
            }

            for (Map.Entry<String, List<String>> pair : headers.entrySet()) {
                final String headerName = pair.getKey();
                final List<String> headerValueList = pair.getValue();

                if (headerValueList.size() > 1) {
                    requestBuilder.removeHeader(headerName);
                    for (String headerValue : headerValueList) {
                        requestBuilder.addHeader(headerName, headerValue);
                    }
                } else if (headerValueList.size() == 1) {
                    requestBuilder.header(headerName, headerValueList.getFirst());
                }

            }

            final okhttp3.Response response = client.newCall(requestBuilder.build()).execute();

            if (response.code() == 429) {
                response.close();
                throw new ReCaptchaException("reCaptcha Challenge requested", url);
            }

            final ResponseBody body = response.body();
            String responseBodyToReturn = null;

            if (body != null) {
                responseBodyToReturn = body.string();
            }

            return new Response(response.code(), response.message(), response.headers().toMultimap(), responseBodyToReturn, url);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid URL: %s".formatted(request.url()), e);
        }
    }
}