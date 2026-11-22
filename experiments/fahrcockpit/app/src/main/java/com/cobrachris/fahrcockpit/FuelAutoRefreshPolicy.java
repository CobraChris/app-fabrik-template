package com.cobrachris.fahrcockpit;

/**
 * Prepared automatic fuel-price scheduling. No scheduled API access is
 * authorized for the free Tankerkoenig app tier as of 22.11.2026.
 *
 * Changing PROVIDER_APPROVED requires new explicit written provider approval,
 * a separate code review and a separate protected release approval.
 */
final class FuelAutoRefreshPolicy {
    // Fail closed until the provider explicitly approves recurring in-app reads.
    static final boolean PROVIDER_APPROVED = false;
    static final long REGULAR_INTERVAL_MS = 15L * 60_000L;
    static final long DISTANCE_MIN_INTERVAL_MS = 5L * 60_000L;
    static final float DISTANCE_TRIGGER_METERS = 5_000f;
    static final long LOCATION_MAX_AGE_MS = 120_000L;
    static final float LOCATION_MAX_ACCURACY_METERS = 150f;
    static final long UI_TICK_INTERVAL_MS = 30_000L;

    private FuelAutoRefreshPolicy() {}

    /**
     * Pure candidate rule; the higher-level caller MUST gate on
     * PROVIDER_APPROVED, explicit opt-in and visible active navigation.
     */
    static boolean isDue(long now, long lastAttemptAt, long lastSuccessAt,
                         boolean hasLastCenter, double centerLat, double centerLng,
                         double currentLat, double currentLng) {
        if (now <= 0 || !valid(currentLat, currentLng)) return false;
        if (lastAttemptAt > now || lastSuccessAt > now) return false;
        if (lastAttemptAt > 0 && now - lastAttemptAt < DISTANCE_MIN_INTERVAL_MS)
            return false;
        if (lastSuccessAt <= 0) return true;
        if (now - lastSuccessAt >= REGULAR_INTERVAL_MS) return true;
        return hasLastCenter && valid(centerLat, centerLng)
            && distanceMeters(centerLat, centerLng, currentLat, currentLng)
                >= DISTANCE_TRIGGER_METERS;
    }

    static boolean locationUsable(long now, long measuredAt, float accuracyMeters,
                                  double lat, double lng) {
        return valid(lat, lng) && measuredAt > 0 && now >= measuredAt
            && now - measuredAt <= LOCATION_MAX_AGE_MS
            && Float.isFinite(accuracyMeters)
            && accuracyMeters >= 0 && accuracyMeters <= LOCATION_MAX_ACCURACY_METERS;
    }

    private static boolean valid(double latitude, double longitude) {
        return Double.isFinite(latitude) && Double.isFinite(longitude)
            && latitude >= -90d && latitude <= 90d
            && longitude >= -180d && longitude <= 180d;
    }

    static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        if (!valid(lat1, lon1) || !valid(lat2, lon2)) return Double.POSITIVE_INFINITY;
        double radLat = Math.toRadians(lat2 - lat1);
        double radLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(radLat / 2), 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.pow(Math.sin(radLon / 2), 2);
        return 6_371_000d * 2d * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0,1-a)));
    }
}
