package com.github.bitfexl.tmsproxy.data;

import io.vertx.core.buffer.Buffer;

/**
 * The result of a tile cache lookup.
 * At least one of filePath or fileContents must be set.
 */
public record TileCacheResult(String extension, String filePath, Buffer fileContents) {
    public static TileCacheResult EMPTY = new TileCacheResult(null, null, null);

    public static TileCacheResult ofFilePath(String filePath) {
        final String[] parts = filePath.split("[/\\\\]");
        final String lastPart = parts[parts.length - 1];
        return new TileCacheResult(lastPart.split("\\.", 2)[1], filePath, null);
    }

    public static TileCacheResult ofFileContents(String extension, Buffer fileContents) {
        return new TileCacheResult(extension, null, fileContents);
    }

    public boolean isEmpty() {
        if (this == EMPTY) {
            return true;
        }
        return extension == null && filePath == null && fileContents == null;
    }
}
