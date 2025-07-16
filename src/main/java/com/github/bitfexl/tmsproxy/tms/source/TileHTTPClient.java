package com.github.bitfexl.tmsproxy.tms.source;

import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.InputStream;
import java.time.Duration;

public class TileHTTPClient {
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .cache(null)
            .callTimeout(Duration.ofSeconds(20))
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(false)
            .followSslRedirects(false)
            .build();

    /**
     * The result of a tile fetch.
     * @param failure If true, something went wrong, do not expect a tile to be present.
     * @param noContent If true, the tile contains no content (http 204) (e.g. over water), do not expect a tile to be present.
     * @param tile If present, the tile which the upstream server returned.
     * @param size The size in bytes if known or -1.
     * @param mediaType The media type of the tile or null.
     */
    public record TileFetchResult (boolean failure, boolean noContent, InputStream tile, int size, String mediaType) {
    }

    private final TileFetchResult FAILURE = new TileFetchResult(true, false, null, 0, null);
    private final TileFetchResult NO_CONTENT = new TileFetchResult(false, true, null, 0, null);

    @SneakyThrows
    public TileFetchResult fetch(String url) {
        final Response response = httpClient.newCall(new Request.Builder().url(url).get().build()).execute();

        if (response.code() < 200 || response.code() > 299) {
            try {
                return FAILURE;
            } finally {
                response.close();
            }
        }

        if (response.code() == 204 || response.body() == null) {
            try {
                return NO_CONTENT;
            } finally {
                response.close();
            }
        }

        return new TileFetchResult(false, false, response.body().byteStream(), Integer.parseInt(response.header("Content-Length", "-1")), response.header("Content-Type"));
    }
}
