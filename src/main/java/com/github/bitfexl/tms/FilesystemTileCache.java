package com.github.bitfexl.tms;

import com.github.bitfexl.tmsproxy.config.TileCacheConfig;
import lombok.Getter;

import java.io.InputStream;

public class FilesystemTileCache implements TileCache {
    @Getter
    private final TileCacheConfig config;

    public FilesystemTileCache(TileCacheConfig config) {
        this.config = config;
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
}
