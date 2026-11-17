package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.content.SharedPreferences;

/** Reuses the existing Tankerkoenig settings screen without another dialog. */
final class FuelQuickSettings {
    private static final String PREFS = "fuel_settings_v1";
    private static final String TYPE_INDEX = "fuel_type_index";
    private static final String RADIUS_INDEX = "fuel_radius_index";
    private static final String SORT_INDEX = "fuel_sort_index";
    private static final String AUTO_ENABLED = "fuel_auto_enabled";
    private static final String V30_INITIALIZED = "fuel_v30_defaults_initialized";
    private static final String LAST_AUTO_ATTEMPT = "fuel_auto_attempt_wall_ms";
    private static final String AUTO_FAILURES = "fuel_auto_failures";
    private static final String[] TYPES = {"e5", "e10", "diesel"};
    private static final int[] RADII = {5, 10, 20, 25};
    private FuelQuickSettings() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    static int typeIndex(Context c) {return bounded(prefs(c).getInt(TYPE_INDEX, 2), 3, 0);}
    static int radiusIndex(Context c) {return bounded(prefs(c).getInt(RADIUS_INDEX, 1), 4, 1);}
    static int sortIndex(Context c) {return bounded(prefs(c).getInt(SORT_INDEX, 0), 2, 0);}
    private static int bounded(int n, int max, int fallback) {
        return n >= 0 && n < max ? n : fallback;
    }
    static String type(Context c) {return TYPES[typeIndex(c)];}
    static int radiusKm(Context c) {return RADII[radiusIndex(c)];}
    static void saveType(Context c, int n) {prefs(c).edit().putInt(TYPE_INDEX, bounded(n,3,0)).apply();}
    static void saveRadius(Context c, int n) {prefs(c).edit().putInt(RADIUS_INDEX,bounded(n,4,1)).apply();}
    static void saveSort(Context c, int n) {prefs(c).edit().putInt(SORT_INDEX,bounded(n,2,0)).apply();}
    static void initializeV30Defaults(Context c) {
        SharedPreferences p = prefs(c);
        if (p.getBoolean(V30_INITIALIZED, false)) return;
        // User-approved v30 migration: Diesel, 10 km and enabled by default.
        // Keep the existing private API key and all unrelated preferences.
        p.edit().putInt(TYPE_INDEX, 2).putInt(RADIUS_INDEX, 1)
            .putBoolean(AUTO_ENABLED, true).putBoolean(V30_INITIALIZED, true)
            .putInt(AUTO_FAILURES, 0).apply();
    }
    static boolean autoEnabled(Context c) {return prefs(c).getBoolean(AUTO_ENABLED, true);}
    static void setAutoEnabled(Context c, boolean enabled) {prefs(c).edit().putBoolean(AUTO_ENABLED, enabled).apply();}
    static long lastAutoAttempt(Context c) {return prefs(c).getLong(LAST_AUTO_ATTEMPT, 0L);}
    static int autoFailures(Context c) {return Math.max(0, prefs(c).getInt(AUTO_FAILURES, 0));}
    static void recordAutoAttempt(Context c, long at) {prefs(c).edit().putLong(LAST_AUTO_ATTEMPT, at).apply();}
    static void recordAutoResult(Context c, boolean successful) {
        SharedPreferences preferences = prefs(c);
        int failures = successful ? 0 : Math.min(4, autoFailures(c) + 1);
        preferences.edit().putInt(AUTO_FAILURES, failures).apply();
    }
}
