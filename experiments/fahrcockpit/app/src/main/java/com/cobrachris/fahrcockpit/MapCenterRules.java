package com.cobrachris.fahrcockpit;

/** Independent Android GPS eligibility rules for initial MapLibre viewport. */
final class MapCenterRules {
    static final long MAX_AGE_MS = 180_000L;
    static final float MAX_ACCURACY_METERS = 250f;
    private MapCenterRules() {}

    static boolean acceptable(double lat, double lon, float accuracy,
                              long timestampMs, long nowMs) {
        return Double.isFinite(lat) && Double.isFinite(lon)
            && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180
            && Float.isFinite(accuracy) && accuracy >= 0
            && accuracy <= MAX_ACCURACY_METERS
            && timestampMs > 0 && nowMs >= timestampMs
            && nowMs - timestampMs <= MAX_AGE_MS;
    }
}
