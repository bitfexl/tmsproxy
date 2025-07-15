package com.github.bitfexl.tmsproxy.config;

import java.util.List;

public record TileSourceConfig(
        String name,
        String cache,
        Integer minZoom,
        Integer maxZoom,
        List<String> sources
) {
}
