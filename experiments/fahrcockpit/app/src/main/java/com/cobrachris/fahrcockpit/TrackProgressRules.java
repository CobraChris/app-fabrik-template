package com.cobrachris.fahrcockpit;

import java.util.Locale;

/** Safe scaling between MediaSession milliseconds and Android's seek bar. */
final class TrackProgressRules {
    static final int MAX = 1000;
    private TrackProgressRules() {}

    static boolean hasDuration(long durationMs) { return durationMs > 0L; }

    static int sliderPosition(long positionMs, long durationMs) {
        if (!hasDuration(durationMs) || positionMs < 0L) return 0;
        double fraction = Math.max(0d, Math.min(1d, (double) positionMs / durationMs));
        return (int) Math.round(fraction * MAX);
    }

    static long targetPosition(int sliderProgress, long durationMs) {
        if (!hasDuration(durationMs)) return -1L;
        int position = Math.max(0, Math.min(MAX, sliderProgress));
        return Math.round(((double) durationMs * position) / MAX);
    }

    static String time(long positionMs) {
        if (positionMs < 0L) return "--:--";
        long seconds = positionMs / 1000L;
        long hours = seconds / 3600L;
        long minutes = (seconds / 60L) % 60L;
        long remainder = seconds % 60L;
        return hours > 0L
            ? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, remainder)
            : String.format(Locale.ROOT, "%d:%02d", seconds / 60L, remainder);
    }
}
