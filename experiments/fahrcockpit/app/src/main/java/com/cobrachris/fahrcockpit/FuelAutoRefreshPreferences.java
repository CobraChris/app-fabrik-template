package com.cobrachris.fahrcockpit;

import android.content.Context;

/**
 * Separate, default-off user consent for a future provider-approved auto mode.
 * A user opt-in cannot bypass the independent provider permission gate.
 */
final class FuelAutoRefreshPreferences {
    private static final String PREFS = "fahrcockpit_fuel_auto_v1";
    private static final String ENABLED = "enabled";
    private FuelAutoRefreshPreferences() {}

    static boolean enabled(Context context) {
        return FuelAutoRefreshPolicy.PROVIDER_APPROVED
            && context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(ENABLED, false);
    }

    static boolean saveEnabled(Context context, boolean enabled) {
        if (enabled && !FuelAutoRefreshPolicy.PROVIDER_APPROVED)
            return false;
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(ENABLED, enabled).commit();
    }
}
