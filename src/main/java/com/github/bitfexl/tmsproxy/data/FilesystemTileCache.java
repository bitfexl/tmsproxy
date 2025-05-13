package com.github.bitfexl.tmsproxy.data;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
public class FilesystemTileCache implements TileCache {
    private final static String fileSeparator = System.getProperty("file.separator");

    private final String directory;

    private final FileSystem fs;

    public FilesystemTileCache(Vertx vertx, String directory) {
        // normalize path for current os and without trailing file separator
        this.directory = Path.of(directory).toString();
        fs = vertx.fileSystem();
    }

    @Override
    public void store(String tileSetName, int z, int x, int y, Buffer file, String extension) {
        final String path = getPath(directory, tileSetName, z, x, y);
        fs.mkdirs(path).onSuccess(__ -> fs.writeFile(path + fileSeparator + "tile." + extension, file));
    }

    @Override
    public Future<TileCacheResult> retrieve(String tileSetName, int z, int x, int y) {
        final String path = getPath(directory, tileSetName, z, x, y);

        return fs.readDir(path).map(files -> {
            if (files.size() == 1) {
                return TileCacheResult.ofFilePath(files.getFirst());
            } else if (files.size() > 1) {
                log.warn("Expected only one file but got multiple. First is '{}'.", files.getFirst());
            }

            return TileCacheResult.EMPTY;
        });
    }

    private String getPath(Object... parts) {
        final StringBuilder path = new StringBuilder();

        for (Object part : parts) {
            if (!path.isEmpty()) {
                path.append(fileSeparator);
            }
            path.append(part);
        }

        return path.toString();
    }
}
