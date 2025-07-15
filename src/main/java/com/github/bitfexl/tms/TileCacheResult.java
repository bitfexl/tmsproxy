package com.github.bitfexl.tms;

import java.io.InputStream;

/**
 * The result of a tile cache lookup.
 * At least one of filePath or fileContents must be set unless the tile is empty or does not have any content.
 * @param size The size of the file in bytes or -1 if unknown.
 * @param expired If true, the cached tile is already expired, a refresh should be done.
 * @param noContent If true, the tile has been cached but does not contain content.
 */
public record TileCacheResult(String mediaType, String filePath, InputStream fileContents, int size, boolean expired, boolean noContent) {
    public static TileCacheResult EMPTY = new TileCacheResult(null, null, null, 0, false, false);

    public static TileCacheResult NO_CONTENT = new TileCacheResult(null, null, null, 0, false, true);

    public static TileCacheResult NO_CONTENT_EXPIRED = new TileCacheResult(null, null, null, 0, true, true);

    public static TileCacheResult ofFilePath(String filePath, int size) {
        return ofFilePath(filePath, size, false);
    }

    public static TileCacheResult ofFileContents(String mediaType, InputStream fileContents, int size) {
        return ofFileContents(mediaType, fileContents, size, false);
    }

    public static TileCacheResult expiredOfFilePath(String filePath, int size) {
        return ofFilePath(filePath, size, true);
    }

    public static TileCacheResult expiredOfFileContents(String mediaType, InputStream fileContents, int size) {
        return ofFileContents(mediaType, fileContents, size, true);
    }

    public static TileCacheResult ofFilePath(String filePath, int size, boolean expired) {
        final String[] parts = filePath.split("[/\\\\]");
        final String lastPart = parts[parts.length - 1];
        return new TileCacheResult(lastPart.split("\\.", 2)[1], filePath, null, size, expired, false);
    }

    public static TileCacheResult ofFileContents(String mediaType, InputStream fileContents, int size, boolean expired) {
        return new TileCacheResult(mediaType, null, fileContents, size, expired, false);
    }

    /**
     * Check if the cache result is empty. e.g. both filePath and fileContents are missing and not noContent.
     */
    public boolean isEmpty() {
        if (this == EMPTY) {
            return true;
        }
        return !noContent && mediaType == null && filePath == null && fileContents == null;
    }

    /**
     * Check if the file size is known.
     */
    public boolean isSizeKnown() {
        return size >= 0;
    }
}
