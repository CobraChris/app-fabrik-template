package com.cobrachris.fahrcockpit;

/**
 * Foreground-only parking candidate from existing Cockpit GPS fixes.
 * Navigation ending is a hint, not proof of parking: require a real trip,
 * a fresh accurate fix and very low speed. No background tracking or Saphe API.
 */
final class ParkingTripTracker {
    static final class Spot {
        final double lat, lng;
        final float accuracyMeters;
        Spot(double lat, double lng, float accuracyMeters) {
            this.lat = lat; this.lng = lng; this.accuracyMeters = accuracyMeters;
        }
    }

    private boolean active;
    private long startedAt, lastFixAt, arrivalAt;
    private double lastLat, lastLng, distanceMeters;
    private float lastSpeed, lastAccuracy;
    private int movingFixes;
    private boolean hasFix;

    void start(long elapsedMs) {
        active = true;
        startedAt = elapsedMs;
        lastFixAt = 0L;
        arrivalAt = 0L;
        distanceMeters = 0d;
        movingFixes = 0;
        hasFix = false;
    }

    void observe(long elapsedMs, double lat, double lng, float accuracy, float speed) {
        if (elapsedMs <= 0 || !valid(lat, lng)
            || !Float.isFinite(accuracy) || accuracy < 0f || accuracy > 75f
            || !Float.isFinite(speed) || speed < 0f || speed > 85f
            || (hasFix && elapsedMs <= lastFixAt)) return;
        if (active) {
            if (hasFix && lastFixAt > 0 && elapsedMs - lastFixAt <= 30_000L
                && speed >= 3.5f && lastSpeed >= 3.5f) {
                double meters = distance(lastLat, lastLng, lat, lng);
                // Ignore GNSS jumps; the trip must really move.
                if (meters > Math.max(12d, accuracy * 0.5d)
                    && meters < 250d) distanceMeters += meters;
            }
            if (speed >= 3.5f) movingFixes++;
        }
        lastFixAt = elapsedMs;
        lastLat = lat;
        lastLng = lng;
        lastAccuracy = accuracy;
        lastSpeed = speed;
        hasFix = true;
    }

    /** Final destination reported by the official Navigation SDK, not a nearby waypoint. */
    void onFinalArrival(long elapsedMs) {
        if (active && elapsedMs >= startedAt) arrivalAt = elapsedMs;
    }

    /** Save only after arrival AND a later accurate standstill, never while moving. */
    Spot takeArrivedParking(long elapsedMs) {
        if (!active || arrivalAt <= 0L || elapsedMs < arrivalAt
            || elapsedMs - arrivalAt > 180_000L || lastSpeed > 1.2f
            || !fresh(elapsedMs, 20_000L, 50f)
            || elapsedMs - startedAt < 60_000L
            || distanceMeters < 350d || movingFixes < 5) return null;
        return finish(elapsedMs);
    }

    Spot manualFix(long elapsedMs) {
        return fresh(elapsedMs, 20_000L, 75f) ?
            new Spot(lastLat, lastLng, lastAccuracy) : null;
    }

    Spot finish(long elapsedMs) {
        Spot result = active && startedAt > 0 && elapsedMs >= startedAt
            && elapsedMs - startedAt >= 60_000L
            && distanceMeters >= 350d && movingFixes >= 5
            && lastSpeed <= 2f && fresh(elapsedMs, 20_000L, 50f)
            ? new Spot(lastLat, lastLng, lastAccuracy) : null;
        active = false;
        // Do not erase the last GPS fix: manual save remains possible.
        return result;
    }

    private boolean fresh(long now, long maxAge, float maxAccuracy) {
        return hasFix && lastFixAt > 0 && now >= lastFixAt
            && now - lastFixAt <= maxAge && lastAccuracy <= maxAccuracy;
    }

    private static boolean valid(double lat, double lng) {
        return Double.isFinite(lat) && Double.isFinite(lng)
            && lat >= -90d && lat <= 90d && lng >= -180d && lng <= 180d;
    }

    private static double distance(double aLat, double aLng, double bLat, double bLng) {
        double dLat = Math.toRadians(bLat - aLat);
        double dLng = Math.toRadians(bLng - aLng);
        double h = Math.pow(Math.sin(dLat / 2d), 2d)
            + Math.cos(Math.toRadians(aLat)) * Math.cos(Math.toRadians(bLat))
            * Math.pow(Math.sin(dLng / 2d), 2d);
        return 6_371_000d * 2d * Math.atan2(Math.sqrt(h), Math.sqrt(Math.max(0d, 1d - h)));
    }
}
