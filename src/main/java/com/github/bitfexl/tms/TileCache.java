package com.github.bitfexl.tms;

import com.github.bitfexl.tmsproxy.config.TileCacheConfig;

import java.io.InputStream;

public interface TileCache {
    static TileCache create(TileCacheConfig config) {
        // TODO: implement
        return new TileCache() {
            @Override
            public TileCacheConfig getConfig() {
                return config;
            }

            @Override
            public void store(String tileSetName, int z, int x, int y, InputStream file, String mediaType) {

            }

            @Override
            public void storeNoContent(String tileSetName, int z, int x, int y) {

            }

            @Override
            public TileCacheResult retrieve(String tileSetName, int z, int x, int y) {
                return TileCacheResult.EMPTY;
            }
        };
    }

    /**
     * Get the initial config for the cache.
     */
    TileCacheConfig getConfig();

    /**
     * Save a tile to the tile cache.
     * @param tileSetName The tile set name of the tile.
     * @param z The z parameter of the tile.
     * @param x The x parameter of the tile.
     * @param y The y parameter of the tile.
     * @param content The tile content to store.
     * @param mediaType The media type of the tile (subtype of image/...).
     */
    void store(String tileSetName, int z, int x, int y, InputStream content, String mediaType);

    /**
     * Save a tile to the tile cache which does not contain any content.
     * @param tileSetName The tile set name of the tile.
     * @param z The z parameter of the tile.
     * @param x The x parameter of the tile.
     * @param y The y parameter of the tile.
     */
    void storeNoContent(String tileSetName, int z, int x, int y);

    /**
     * Retrieve a previously stored tile.
     * @param tileSetName The tile set name of the tile.
     * @param z The z parameter of the tile.
     * @param x The x parameter of the tile.
     * @param y The y parameter of the tile.
     * @return A future resolving to the tile or on empty tile (all values null) if the file has not been
     * cached yet or the cached value has expired.
     */
    TileCacheResult retrieve(String tileSetName, int z, int x, int y);
}
