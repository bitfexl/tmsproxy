package com.github.bitfexl.tmsproxy.data;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;

public interface TileCache {
    /**
     * Save a file to the tile cache.
     * @param tileSetName The tile set name of the tile.
     * @param z The z parameter of the tile.
     * @param x The x parameter of the file.
     * @param y The y parameter of the file.
     * @param file The file contents to store.
     * @param extension The file extension e.g. jpeg, png, ... (subtype of image/... mediatype).
     */
    void store(String tileSetName, int z, int x, int y, Buffer file, String extension);

    /**
     * Retrieve a previously stored file.
     * @param tileSetName The tile set name of the tile.
     * @param z The z parameter of the tile.
     * @param x The x parameter of the file.
     * @param y The y parameter of the file.
     * @return A future resolving to the tile or on empty tile (all values null) if the file has not been
     * cached yet or the cached value has expired.
     */
    Future<TileCacheResult> retrieve(String tileSetName, int z, int x, int y);
}
