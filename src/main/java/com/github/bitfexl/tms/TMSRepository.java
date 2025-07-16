package com.github.bitfexl.tms;

import com.github.bitfexl.tmsproxy.config.Config;
import com.github.bitfexl.tmsproxy.config.TileCacheConfig;
import com.github.bitfexl.tmsproxy.config.TileSourceConfig;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TMSRepository {
    private final int FETCH_TILE_MAX_RETRIES = 3;

    private final TileHTTPClient httpClient = new TileHTTPClient();

    private final Map<String, TileCache> caches = new HashMap<>();

    private final Map<String, TileSource> sources = new HashMap<>();

    public TMSRepository(Config config) {
        final List<TileCache> cacheList = new ArrayList<>();

        if (config.caches() != null) {
            for (TileCacheConfig tileCacheConfig : config.caches()) {
                final TileCache cache = TileCache.create(tileCacheConfig);
                cacheList.add(cache);
                caches.put(cache.getConfig().name(), cache);
            }
        }

        for (TileSourceConfig tileSourceConfig : config.tiles()) {
            sources.put(tileSourceConfig.name(), new TileSource(tileSourceConfig, cacheList));
        }
    }

    private static final TileResult NO_CONTENT = new TileResult(null, null, 0, null);

    public record TileResult(String filePath, InputStream fileContents, int size, String mediaType) {
        /**
         * Construct a tile result from a cache result.
         * @param result The cache result.
         * @return The tile result or null if empty result.
         */
        public static TileResult ofCacheResult(TileCacheResult result) {
            if (result.isEmpty()) {
                return null;
            }
            if (result.noContent()) {
                return NO_CONTENT;
            }
            return new TileResult(result.filePath(), result.fileContents(), result.size(), result.mediaType());
        }

        public static TileResult ofFetchResult(TileHTTPClient.TileFetchResult result) {
            return ofFetchResult(result, result.tile());
        }

        public static TileResult ofFetchResult(TileHTTPClient.TileFetchResult result, InputStream contents) {
            if (result.failure()) {
                return null;
            }
            if (result.noContent()) {
                return NO_CONTENT;
            }
            return new TileResult(null, contents, result.size(), result.mediaType());
        }

        /**
         * Check if the tile contains content.
         * @return If false, 204 no content.
         */
        public boolean hasContent() {
            return filePath != null || fileContents != null;
        }

        /**
         * Check if the file size is known.
         */
        public boolean isSizeKnown() {
            return size >= 0;
        }
    }

    public TileResult getTile(String tileSetName, int z, int x, int y) {
        final TileSource source = sources.get(tileSetName);
        if (source == null) {
            return null;
        }

        return getTile(source, z, x, y);
    }

    @SneakyThrows
    private TileResult getTile(TileSource source, int z, int x, int y) {
        if (source.hasCache()) {
            final TileCacheResult cacheResult = source.getCache().retrieve(source.getConfig().name(), z, x, y);

            if (cacheResult.isEmpty() || cacheResult.expired()) {
                final TileHTTPClient.TileFetchResult fetchResult = fetchTile(source, z, x, y);
                if (!fetchResult.failure()) {
                    // TODO: optimize use duplicated input stream, store asynchronously
                    try (InputStream in = fetchResult.tile()) {
                        final byte[] contents = in.readAllBytes();
                        source.getCache().store(source.getConfig().name(), z, x, y, new ByteArrayInputStream(contents), fetchResult.mediaType());
                        return TileResult.ofFetchResult(fetchResult, new ByteArrayInputStream(contents));
                    }
                }
            }

            return TileResult.ofCacheResult(cacheResult);
        }

        return TileResult.ofFetchResult(fetchTile(source, z, x, y));
    }

    private TileHTTPClient.TileFetchResult fetchTile(TileSource source, int z, int x, int y) {
        TileHTTPClient.TileFetchResult result = null;

        for (int i = 0; i < FETCH_TILE_MAX_RETRIES; i++) {
            final TileSourceUrl url = source.getSourceUrl();
            result = httpClient.fetch(url.buildUrl(z, x, y));
            if (result.failure()) {
                source.recordFailure(url);
            } else {
                return result;
            }
        }

        return result;
    }
}
