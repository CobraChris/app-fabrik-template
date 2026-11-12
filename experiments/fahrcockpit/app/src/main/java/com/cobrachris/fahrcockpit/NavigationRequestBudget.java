package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Local extra spending guard for optional Google Navigation SDK builds.
 * Google Cloud's enforced Navigation Request quota is still essential:
 * clearing app data, reinstalling, or using another device bypasses this guard.
 */
final class NavigationRequestBudget {
    static final int MAX_DAILY_REQUESTS = 20;
    private static final String PREFS = "fahrcockpit_nav_request_guard_v1";
    private static final String DAY = "last_utc_epoch_day";
    private static final String COUNT = "reserved_requests";
    private static final long MILLIS_PER_DAY = 86_400_000L;
    private NavigationRequestBudget() {}

    /** Never accept a rollback of the device's calendar day. */
    static boolean mayReserve(long storedUtcDay, int usedToday, long currentUtcDay) {
        if (currentUtcDay < 0 || currentUtcDay < storedUtcDay) return false;
        if (storedUtcDay != currentUtcDay) return true;
        return usedToday >= 0 && usedToday < MAX_DAILY_REQUESTS;
    }

    static synchronized boolean reserve(Context context) {
        final long day = Math.floorDiv(System.currentTimeMillis(), MILLIS_PER_DAY);
        try {
            SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            long priorDay = preferences.getLong(DAY, -1L);
            int priorCount = preferences.getInt(COUNT, 0);
            if (!mayReserve(priorDay, priorCount, day)) return false;
            int nextCount = day == priorDay ? priorCount + 1 : 1;
            // Fail closed if storage fails; never roll back after SDK call.
            return preferences.edit().putLong(DAY, day).putInt(COUNT, nextCount).commit();
        } catch (RuntimeException cannotPersist) {
            return false;
        }
    }
}
