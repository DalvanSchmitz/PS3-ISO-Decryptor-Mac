package com.ps3dec.util;

public class FormatUtils {

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public static String formatTime(long seconds) {
        if (seconds < 0) return "\u2014";
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return String.format("%dm %02ds", seconds / 60, seconds % 60);
        return String.format("%dh %02dm %02ds", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }
}
