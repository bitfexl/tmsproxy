package com.github.bitfexl.tmsproxy.config;


import lombok.Getter;

public class TileSource {
    @Getter
    private final String name;

    private final String[] urlParts;

    public TileSource(String name, String url) {
        this.name = name;
        urlParts = url.split("[{}]");
        for (int i = 0; i < urlParts.length; i++) {
            if (urlParts[i].length() == 1) {
                urlParts[i] = urlParts[i].toLowerCase();
            }
        }
    }

    public String buildUrl(int z, int x, int y) {
        final StringBuilder url = new StringBuilder();

        for (String part : urlParts) {
            url.append(switch (part) {
                case "z" -> z;
                case "x" -> x;
                case "y" -> y;
                default -> part;
            });
        }

        return url.toString();
    }
}
