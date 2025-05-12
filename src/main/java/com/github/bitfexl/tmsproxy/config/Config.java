package com.github.bitfexl.tmsproxy.config;

import lombok.Value;

import java.util.Map;

@Value
public class Config {
    int port;

    Map<String, TileSource> tileSources;
}
