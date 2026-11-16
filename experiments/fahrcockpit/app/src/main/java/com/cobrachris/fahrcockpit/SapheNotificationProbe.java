package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Explicitly opt-in, device-local diagnostic for Android Saphe Link notifications.
 * NO reading/copying of notification text, no service data integration,
 * no Bluetooth access, no network requests or real warning claims.
 */
final class SapheNotificationProbe {
    private static final String PREFS = "fahrcockpit_saphe_notification_probe_v1";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_LAST_EVENT = "last_event_at_ms";

    private SapheNotificationProbe() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean enabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_ENABLED, false);
    }

    static boolean setEnabled(Context ctx, boolean enabled) {
        // Avoid presenting a pre-existing notification as a new event.
        return prefs(ctx).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .remove(KEY_LAST_EVENT).commit();
    }

    static void onNotificationPosted(Context ctx, String packageName) {
        if (!SapheNotificationRules.shouldRecord(packageName, enabled(ctx))) return;
        // Event metadata only. Do not store package text, location, or identifiers.
        prefs(ctx).edit().putLong(KEY_LAST_EVENT, System.currentTimeMillis()).apply();
    }

    static long lastObservedAt(Context ctx) {
        if (!enabled(ctx)) return 0;
        return prefs(ctx).getLong(KEY_LAST_EVENT, 0L);
    }
}
