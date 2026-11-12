package com.cobrachris.fahrcockpit;

/**
 * Calculates seek targets from Android MediaSession's last reported state.
 *
 * MediaSession PlaybackState positions are snapshots, not live counters.
 * The monotonic elapsed realtime clock is used to advance playing positions
 * at the reported playback speed, never a wall-clock timestamp.
 */
final class SpotifySeekRules {
    private SpotifySeekRules() {}

    static long seekTarget(long reportedMs, boolean playing, float speed,
                           long lastUpdateElapsedMs, long nowElapsedMs,
                           long durationMs, int changeMs) {
        if (reportedMs < 0L) return -1L; // No reported position: no seek.
        double estimate = reportedMs;
        if (playing && Float.isFinite(speed) && speed > 0f
                && lastUpdateElapsedMs > 0L && nowElapsedMs >= lastUpdateElapsedMs) {
            double elapsedMs = (double) nowElapsedMs - (double) lastUpdateElapsedMs;
            estimate += elapsedMs * speed;
        }
        double target = estimate + changeMs;
        if (!Double.isFinite(target)) return -1L;
        target = Math.max(0d, target);
        if (durationMs > 0L) target = Math.min(target, (double) durationMs);
        return (long) Math.min(target, (double) Long.MAX_VALUE);
    }
}
