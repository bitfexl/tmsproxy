package com.github.bitfexl.tmsproxy.data;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.WriteStream;
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

    @SuppressWarnings("unchecked")
    @Override
    public Future<WriteStream<Buffer>> store(String tileSetName, int z, int x, int y, String extension) {
        final String path = getPath(directory, tileSetName, z, x, y);
        return fs.mkdirs(path)
                .compose(event -> {
                    final Future<AsyncFile> fileFuture = fs.open(path + fileSeparator + "tile." + extension, new OpenOptions().setCreate(true).setWrite(true));
                    return (Future<WriteStream<Buffer>>) (Object) fileFuture;
                })
                .recover(throwable -> {
                        log.error("Path '{}' exists but is not a directory.", path);
                        return Future.failedFuture(throwable);
                });
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
