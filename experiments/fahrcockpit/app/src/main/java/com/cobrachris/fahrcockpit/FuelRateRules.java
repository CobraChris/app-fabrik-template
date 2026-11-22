package com.cobrachris.fahrcockpit;

/**
 * Shared limits for every Tankerkoenig request from a single app installation.
 * The server's technical ceiling (one request per minute per API key) is not
 * permission for continuous background polling.
 */
public final class FuelRateRules {
    /** Safety margin over the published 60-second per-key hard ceiling. */
    public static final long MIN_REQUEST_INTERVAL_MS = 65_000L;
    /** If automatic polling is ever explicitly approved: never more than 5 min. */
    public static final long MIN_APPROVED_AUTO_INTERVAL_MS = 300_000L;
    /** After a rate-limit response, stop requests for an hour. */
    public static final long RATE_LIMIT_BACKOFF_MS = 3_600_000L;
    /** Do not hammer a struggling or unavailable service. */
    public static final long TRANSIENT_BACKOFF_MS = 300_000L;

    private FuelRateRules() {}

    public static long remainingMillis(long now, long previousAttempt, long blockedUntil) {
        if (now <= 0) return MIN_REQUEST_INTERVAL_MS;
        long wait = 0L;
        if (previousAttempt > 0) {
            // After a backward clock jump, fail closed until time is sane.
            if (previousAttempt > now) return MIN_REQUEST_INTERVAL_MS;
            wait = Math.max(0L, MIN_REQUEST_INTERVAL_MS - (now - previousAttempt));
        }
        if (blockedUntil > now) wait = Math.max(wait, blockedUntil - now);
        return wait;
    }

    public static long backoffMillis(int httpCode) {
        if (httpCode == 503 || httpCode == 429)
            return RATE_LIMIT_BACKOFF_MS;
        if (httpCode == 500 || httpCode == 502 || httpCode == 504)
            return TRANSIENT_BACKOFF_MS;
        return 0L;
    }

    public static String remainingLabel(long remainingMillis) {
        long seconds = Math.max(1L, (remainingMillis + 999L) / 1000L);
        if (seconds >= 3_600L) return ((seconds + 59L) / 60L) + " Min.";
        if (seconds >= 60L)
            return (seconds / 60L) + " Min. " + (seconds % 60L) + " Sek.";
        return seconds + " Sek.";
    }
}
