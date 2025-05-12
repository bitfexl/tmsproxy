package com.github.bitfexl.tmsproxy.config;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.util.HashMap;

import static java.util.Objects.requireNonNullElse;

public class ConfigParser {
    public static final int DEFAULT_PORT = 80;
    public static final int DEFAULT_TILE_MIN_ZOOM = 0;
    public static final int DEFAULT_TILE_MAX_ZOOM = 20;
    public static final String DEFAULT_CACHE_MAX_AGE = "48h";
    public static final int DEFAULT_CACHE_MAX_ELEMENTS = 500_000;

    public Config parseConfig(JsonObject rawConfig) {
        final int port = requireNonNullElse(rawConfig.getInteger("port"), DEFAULT_PORT);

        final Config config = new Config(port, new HashMap<>());

        final JsonArray tiles;
        try {
            tiles = rawConfig.getJsonArray("tiles");
        } catch (Exception ex) {
            throw new RuntimeException("'tiles' must be an array of tile set configurations.", ex);
        }
        if (tiles == null || tiles.isEmpty()) {
            throw new RuntimeException("At least one tile set configuration must be provided with the 'tiles' configuration array.");
        }

        for (int i = 0; i < tiles.size(); i++) {
            final JsonObject tileJsonConfig = tiles.getJsonObject(i);

            final String name = tileJsonConfig.getString("name");
            if (name == null) {
                throw new RuntimeException("'name' configuration parameter is missing from tile set configuration.");
            }

            if (config.getTileSources().containsKey(name)) {
                throw new RuntimeException("Duplicate tile set '" + name + "' found.");
            }

            // todo: multiple sources, cache, min and max zoom
            config.getTileSources().put(name, new TileSource(name, tileJsonConfig.getJsonArray("sources").getString(0)));
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
            default -> throw new RuntimeException("Unable to parse duration '" + duration + "'.");
        };
    }
}
