package com.github.bitfexl.tmsproxy.config;

import com.github.bitfexl.tmsproxy.data.FilesystemTileCache;
import com.github.bitfexl.tmsproxy.data.TileCacheResult;
import com.github.bitfexl.tmsproxy.data.TileSource;
import com.github.bitfexl.tmsproxy.data.TileSourceUrl;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;

public class ConfigParser {
    public static final int DEFAULT_PORT = 80;
    public static final int DEFAULT_TILE_MIN_ZOOM = 0;
    public static final int DEFAULT_TILE_MAX_ZOOM = 20;
    public static final String DEFAULT_CACHE_MAX_AGE = "48h";
    public static final int DEFAULT_CACHE_MAX_ELEMENTS = 500_000;

    public Config parseConfig(JsonObject rawConfig, Vertx vertx) {
        final int port = rawConfig.getInteger("port", DEFAULT_PORT);

        final Config config = new Config(port, new HashMap<>(), new HashMap<>());

        // ----- parse tiles -----

        final JsonArray tiles;
        try {
            tiles = rawConfig.getJsonArray("tiles");
        } catch (Exception ex) {
            throw new InvalidConfigurationException("'tiles' must be an array of tile source configurations.", ex);
        }
        if (tiles == null || tiles.isEmpty()) {
            throw new InvalidConfigurationException("At least one tile source configuration must be provided with the 'tiles' configuration array.");
        }

        for (int i = 0; i < tiles.size(); i++) {
            final JsonObject tileJsonConfig = tiles.getJsonObject(i);

            final String name = tileJsonConfig.getString("name");
            if (name == null) {
                throw new InvalidConfigurationException("'name' configuration parameter is missing from tile source configuration.");
            }

            if (config.getTileSources().containsKey(name)) {
                throw new InvalidConfigurationException("Duplicate tile sources named '" + name + "' found.");
            }

            final List<TileSourceUrl> sources;
            try {
                final JsonArray rawSources = tileJsonConfig.getJsonArray("sources");
                if (rawSources == null) {
                    sources = List.of();
                } else {
                    sources = rawSources.stream().map(url -> new TileSourceUrl((String) url)).toList();
                }
            } catch (RuntimeException ex) {
                throw new InvalidConfigurationException("'sources' must be an array of tile source urls.", ex);
            }
            if (sources.isEmpty()) {
                throw new InvalidConfigurationException("At least one tile source url must be provided with the 'tiles.sources' configuration array.");
            }

            config.getTileSources().put(name,
                    new TileSource(
                            name,
                            tileJsonConfig.getString("cache"),
                            tileJsonConfig.getInteger("minZoom", DEFAULT_TILE_MIN_ZOOM),
                            tileJsonConfig.getInteger("maxZoom", DEFAULT_TILE_MAX_ZOOM),
                            sources
                    )
            );
        }

        // ----- parse caches -----

        JsonArray caches;
        try {
            caches = rawConfig.getJsonArray("caches");
        } catch (Exception ex) {
            throw new InvalidConfigurationException("'caches' must be an array of cache configurations.", ex);
        }
        if (caches == null) {
            caches = new JsonArray();
        }

        for (int i = 0; i < caches.size(); i++) {
            final JsonObject cacheJsonConfig = caches.getJsonObject(i);

            final String name = cacheJsonConfig.getString("name");
            if (name == null) {
                throw new InvalidConfigurationException("'name' configuration parameter is missing from cache configuration.");
            }

            if (config.getTileCaches().containsKey(name)) {
                throw new InvalidConfigurationException("Duplicate cache named '" + name + "' found.");
            }

            final Duration maxAge = parseDuration(cacheJsonConfig.getString("maxAge", DEFAULT_CACHE_MAX_AGE));

            final int maxElements;
            try {
                maxElements = cacheJsonConfig.getInteger("maxElements", DEFAULT_CACHE_MAX_ELEMENTS);
            } catch (Exception ex) {
                throw new InvalidConfigurationException("'caches.maxElements' configuration parameter must be an integer.");
            }

            final String directory = cacheJsonConfig.getString("directory");
            if (directory == null) {
                throw new InvalidConfigurationException("'directory' configuration parameter is missing from cache configuration.");
            }

            // todo: all parameters
            config.getTileCaches().put(
                    name,
                    new FilesystemTileCache(vertx, directory)
            );
        }

        return config;
    }

    /**
     * Parse a duration. Might throw and error if the duration is set incorrectly.
     * Supports seconds ('s'), minutes ('m'), hours ('h') and days ('d'), defaults to hours.
     * @return The max age as a duration.
     */
    public Duration parseDuration(String duration) {
        char c = duration.charAt(duration.length() - 1);
        if (Character.isDigit(c)) {
            return Duration.ofHours(Integer.parseInt(duration));
        }

        final int value = Integer.parseInt(duration.substring(0, duration.length() - 1).trim());

        return switch (c) {
            case 's' -> Duration.ofSeconds(value);
            case 'm' -> Duration.ofMinutes(value);
            case 'h' -> Duration.ofHours(value);
            case 'd' -> Duration.ofDays(value);
            default -> throw new InvalidConfigurationException("Unable to parse duration '" + duration + "'.");
        };
    }
}
