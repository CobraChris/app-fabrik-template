package com.cobrachris.fahrcockpit;

/** Minimum quality for a user-triggered fuel search; no implicit network call. */
final class FuelLocationQuality {
    static final long MAX_SEARCH_FIX_AGE_MS = 15_000L;
    static final float MAX_SEARCH_ACCURACY_METERS = 250f;

    private FuelLocationQuality() {}

    static boolean usable(long now, long fixTime, float accuracy,
                          double latitude, double longitude) {
        return FuelRules.inGermany(latitude, longitude)
            && fixTime > 0L && now >= fixTime
            && now - fixTime <= MAX_SEARCH_FIX_AGE_MS
            && Float.isFinite(accuracy) && accuracy >= 0f
            && accuracy <= MAX_SEARCH_ACCURACY_METERS;
    }
}
