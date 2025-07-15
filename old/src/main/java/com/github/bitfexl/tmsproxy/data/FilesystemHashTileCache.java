package com.github.bitfexl.tmsproxy.data;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;

import java.nio.file.Path;

public class FilesystemHashTileCache implements TileCache {
    private final String directory;

    private final FileSystem fs;

    public FilesystemHashTileCache(Vertx vertx, String directory) {
        // normalize path for current os and without trailing file separator
        this.directory = Path.of(directory).toString();
        fs = vertx.fileSystem();
    }

    @Override
    public void store(String tileSetName, int z, int x, int y, Buffer file, String extension) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<TileCacheResult> retrieve(String tileSetName, int z, int x, int y) {
        throw new UnsupportedOperationException();
    }
}
