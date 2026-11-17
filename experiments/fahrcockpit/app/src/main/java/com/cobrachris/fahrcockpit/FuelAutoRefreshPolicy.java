package com.cobrachris.fahrcockpit;

/**
 * Pure scheduling policy. Called only while the navigation Activity is RESUMED
 * and confirmed guidance is active. Never schedules Android background work.
 */
final class FuelAutoRefreshPolicy {
    static final long REFRESH_MS = 15L * 60 * 1000;
    static final long MIN_REQUEST_GAP_MS = 5L * 60 * 1000;
    static final long[] FAILURE_BACKOFF_MS = {
        5L * 60 * 1000, 10L * 60 * 1000, 20L * 60 * 1000, 30L * 60 * 1000
    };
    static final float DISTANCE_TRIGGER_METERS = 5000f;

    private FuelAutoRefreshPolicy() {}

    static long backoffMs(int failures) {
        return FAILURE_BACKOFF_MS[Math.min(
            Math.max(0, failures - 1), FAILURE_BACKOFF_MS.length - 1)];
    }

    static boolean shouldFetch(long now, long lastAttempt, long lastSuccess,
            int failures, boolean movedFiveKm, boolean visible, boolean navigating,
            boolean enabled, boolean keyAvailable) {
        if (!visible || !navigating || !enabled || !keyAvailable) return false;
        if (lastAttempt > now || lastSuccess > now) return false;
        if (lastAttempt > 0 && now - lastAttempt < MIN_REQUEST_GAP_MS) return false;
        if (failures > 0 && lastAttempt > 0
                && now - lastAttempt < backoffMs(failures)) return false;
        return lastSuccess <= 0 || now - lastSuccess >= REFRESH_MS || movedFiveKm;
    }
}
