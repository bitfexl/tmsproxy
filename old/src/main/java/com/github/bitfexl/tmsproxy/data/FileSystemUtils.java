package com.github.bitfexl.tmsproxy.data;

public final class FileSystemUtils {
    private FileSystemUtils() {}

    private final static String fileSeparator = System.getProperty("file.separator");

    /**
     * Get the path to a file or directory.
     * @param parts The path parts, will be concatenated with the file separator.
     * @return The final path.
     */
    public static String getPath(Object... parts) {
        final StringBuilder path = new StringBuilder();

        for (Object part : parts) {
            if (!path.isEmpty()) {
                path.append(fileSeparator);
            }
            path.append(part);
        }

        return path.toString();
    }
}
