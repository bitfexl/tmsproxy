package com.github.bitfexl.tms;

import com.github.bitfexl.tmsproxy.config.TileCacheConfig;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public class FilesystemTileCache implements TileCache {
    @Getter
    private final TileCacheConfig config;

    private final String basePath;

    public FilesystemTileCache(TileCacheConfig config) {
        this.config = config;
        String tempBasePath = config.directory().replace('\\', '/');
        if (tempBasePath.endsWith("/") && !tempBasePath.equals("./") && !tempBasePath.equals("../")) {
            tempBasePath = tempBasePath.substring(0, tempBasePath.length() - 1);
        }
        basePath = tempBasePath;
    }

    @Override
    @SneakyThrows
    public void store(String tileSetName, int z, int x, int y, InputStream contents, String mediaType) {
        final String path = String.join("/", basePath, tileSetName, String.valueOf(z), String.valueOf(x), String.valueOf(y));
        final File directory = new File(path);
        if (!directory.mkdirs()) {
            throw new RuntimeException("Error creating directories for path: " + path);
        }
        for (final File file : directory.listFiles()) {
            file.delete();
        }
        try (final OutputStream out = new FileOutputStream(path + "/" + mediaType.replace('/', '_')); contents) {
            contents.transferTo(out);
        }
    }

    @Override
    public void storeNoContent(String tileSetName, int z, int x, int y) {
        store(tileSetName, z, x, y, InputStream.nullInputStream(), "no_content");
    }

    @Override
    public TileCacheResult retrieve(String tileSetName, int z, int x, int y) {
        final String path = String.join("/", basePath, tileSetName, String.valueOf(z), String.valueOf(x), String.valueOf(y));
        final File directory = new File(path);
        final String[] files = directory.list();
        if (files == null || files.length == 0) {
            return TileCacheResult.EMPTY;
        }
        final String fileName = files[0];
        if (fileName.equals("no_content")) {
            return TileCacheResult.NO_CONTENT;
        }
        final String completeFilename = path + "/" + fileName;
        final int size = (int) new File(completeFilename).length();
        return TileCacheResult.ofFilePath(fileName.replace('_', '/'), completeFilename, size);
    }
}
