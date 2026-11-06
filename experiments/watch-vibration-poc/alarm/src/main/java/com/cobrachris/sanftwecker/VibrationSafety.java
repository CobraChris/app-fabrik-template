package com.cobrachris.sanftwecker;

/** Pure haptics safety limits, independent of Android runtime. */
public final class VibrationSafety {
    public static final int MAX_PREVIEW_MS = 5_000;
    public static final int MAX_ALARM_MS = 12_000;
    public static final int NON_REPEATING = -1;
    private VibrationSafety() {}
    public static long previewDuration(int mode) {
        return Math.min(MAX_PREVIEW_MS, VibrationPresets.duration(mode));
    }
    public static boolean isFinite(long duration) {
        return duration > 0 && duration <= MAX_PREVIEW_MS;
    }
}
