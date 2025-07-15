package com.github.bitfexl.tmsproxy.config;

import java.util.List;

public record Config(Integer port, List<TileSourceConfig> tiles, List<TileCacheConfig> caches) {
}
