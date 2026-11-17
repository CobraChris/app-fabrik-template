package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.content.SharedPreferences;

/** Reuses the existing Tankerkoenig settings screen without another dialog. */
final class FuelQuickSettings {
    private static final String PREFS = "fuel_settings_v1";
    private static final String TYPE_INDEX = "fuel_type_index";
    private static final String RADIUS_INDEX = "fuel_radius_index";
    private static final String SORT_INDEX = "fuel_sort_index";
    private static final String[] TYPES = {"e5", "e10", "diesel"};
    private static final int[] RADII = {5, 10, 20, 25};
    private FuelQuickSettings() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    static int typeIndex(Context c) {return bounded(prefs(c).getInt(TYPE_INDEX, 0), 3, 0);}
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
}
