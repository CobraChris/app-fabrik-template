package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores only the latest parked-car coordinate on this device, never a route history. */
final class ParkingLocationStore {
    private static final String PREFS = "fahrcockpit_last_parking_v1";

    static final class Saved {
        final double lat, lng;
        final float accuracy;
        final long savedAt;
        final boolean automatic;
        Saved(double lat, double lng, float accuracy, long savedAt, boolean automatic) {
            this.lat = lat; this.lng = lng; this.accuracy = accuracy;
            this.savedAt = savedAt; this.automatic = automatic;
        }
    }

    private ParkingLocationStore() {}

    static boolean save(Context context, ParkingTripTracker.Spot spot, boolean automatic) {
        if (spot == null || !Double.isFinite(spot.lat) || !Double.isFinite(spot.lng)
            || spot.lat < -90d || spot.lat > 90d || spot.lng < -180d || spot.lng > 180d
            || !Float.isFinite(spot.accuracyMeters) || spot.accuracyMeters < 0f
            || spot.accuracyMeters > 75f) return false;
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong("lat", Double.doubleToRawLongBits(spot.lat))
            .putLong("lng", Double.doubleToRawLongBits(spot.lng))
            .putFloat("accuracy", spot.accuracyMeters)
            .putLong("saved_at", System.currentTimeMillis())
            .putBoolean("automatic", automatic)
            .commit();
    }

    static Saved read(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long savedAt = p.getLong("saved_at", 0L);
        double lat = Double.longBitsToDouble(p.getLong("lat", 0L));
        double lng = Double.longBitsToDouble(p.getLong("lng", 0L));
        float accuracy = p.getFloat("accuracy", Float.NaN);
        if (savedAt <= 0L || savedAt > System.currentTimeMillis()
            || !Double.isFinite(lat) || !Double.isFinite(lng)
            || lat < -90d || lat > 90d || lng < -180d || lng > 180d
            || !Float.isFinite(accuracy) || accuracy < 0f || accuracy > 75f) return null;
        return new Saved(lat, lng, accuracy, savedAt, p.getBoolean("automatic", false));
    }

    static boolean clear(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit();
    }
}
