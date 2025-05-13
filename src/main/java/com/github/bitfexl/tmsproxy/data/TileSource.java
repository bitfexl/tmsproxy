package com.github.bitfexl.tmsproxy.data;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class TileSource {
    @Getter
    private final String name;

    @Getter
    private final String cache;

    @Getter
    private final int minZoom;

    @Getter
    private final int maxZoom;

    private final List<TileSourceUrl> urls;

    public String buildUrl(int z, int x, int y) {
        final int i = (int)(Math.random() * urls.size());
        return urls.get(i).buildUrl(z, x, y);
    }
}
