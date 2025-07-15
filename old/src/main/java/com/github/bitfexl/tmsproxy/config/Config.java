package com.github.bitfexl.tmsproxy.config;

import com.github.bitfexl.tmsproxy.data.TileCache;
import com.github.bitfexl.tmsproxy.data.TileSource;
import lombok.Value;

import java.util.Map;

@Value
public class Config {
    int port;

    Map<String, TileSource> tileSources;

    Map<String, TileCache> tileCaches;
}
