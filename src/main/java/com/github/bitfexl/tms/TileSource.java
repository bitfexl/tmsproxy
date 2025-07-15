package com.github.bitfexl.tms;

import com.github.bitfexl.tmsproxy.config.TileSourceConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class TileSource {
    @Getter
    private final TileSourceConfig config;

    @RequiredArgsConstructor
    private static class AnnotatedTileSourceUrl {
        final TileSourceUrl source;
        volatile int failures = 0;
    }

    private final AnnotatedTileSourceUrl[] sources;

    private volatile int nextSourceIndex = 0;

    @Getter
    private final TileCache cache;

    public TileSource(TileSourceConfig config, List<TileCache> caches) {
        this.config = config;

        this.sources = new AnnotatedTileSourceUrl[config.sources().size()];
        int i = 0;
        for (String source : config.sources()) {
            this.sources[i++] = new AnnotatedTileSourceUrl(new TileSourceUrl(source));
        }

        this.cache = caches.stream().filter(c -> c.getConfig().name().equals(config.cache())).findAny().orElse(null);
    }

    /**
     * Get the next source url to use.
     * @return The url which should be used next.
     */
    public TileSourceUrl getSourceUrl() {
        final int index = nextSourceIndex;

        final AnnotatedTileSourceUrl source = sources[index];

        if (index + 1 < sources.length) {
            final AnnotatedTileSourceUrl nextSource = sources[index + 1];

            if (nextSource.failures > source.failures) {
                nextSourceIndex = 0;
            } else {
                nextSourceIndex = index + 1;
            }
        } else {
            nextSourceIndex = 0;
        }

        return source.source;
    }

    /**
     * Call if the tile source did not work. Useful when multiple sources are available.
     * @param source The source which did not work.
     */
    public void recordFailure(TileSourceUrl source) {
        for (AnnotatedTileSourceUrl annotatedTileSourceUrl : sources) {
            if (annotatedTileSourceUrl.source.equals(source)) {
                annotatedTileSourceUrl.failures++;
                sortSources();
                return;
            }
        }
    }

    public boolean hasCache() {
        return cache != null;
    }

    private void sortSources() {
        Arrays.sort(sources, Comparator.comparingInt(s -> s.failures));
    }
}
