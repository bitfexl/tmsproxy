package com.github.bitfexl.tmsproxy.data;

public class TileSourceUrl {
    private final String[] urlParts;

    public TileSourceUrl(String url) {
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
