package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Temporary diagnostic only. No notification text, routes, coordinates, or
 * bluetooth data are read or stored. No network or background polling.
 */
final class SapheDiagnosticSession {
    static final long DURATION_MS = 10L * 60 * 1000L;
    private static final String PREFS = "fahrcockpit_saphe_diag_v31";
    private static final String STARTED = "started";
    private static final String ENDS = "ends";
    private static final String SAPHE_COUNT = "saphe_count";
    private static final String LAST_SAPHE = "last_saphe";
    private static final String TEST_PENDING = "test_pending";
    private static final String TEST_SEEN = "test_seen";
    private static final String LISTENER_CONNECTED = "listener_connected";
    private SapheDiagnosticSession() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean start(Context c, long now) {
        if (now <= 0L) return false;
        return prefs(c).edit().putLong(STARTED, now).putLong(ENDS, now + DURATION_MS)
            .putInt(SAPHE_COUNT, 0).remove(LAST_SAPHE)
            .putBoolean(TEST_PENDING, false).putLong(TEST_SEEN, 0)
            .commit();
    }

    static boolean active(Context c, long now) {
        SharedPreferences p = prefs(c);
        long start = p.getLong(STARTED, 0), ends = p.getLong(ENDS, 0);
        return start > 0 && now >= start && now < ends;
    }

    static long endsAt(Context c) { return prefs(c).getLong(ENDS, 0L); }
    static int sapheCount(Context c) { return prefs(c).getInt(SAPHE_COUNT, 0); }
    static long lastSapheAt(Context c) { return prefs(c).getLong(LAST_SAPHE, 0L); }
    static long listenerConnectedAt(Context c) {
        return prefs(c).getLong(LISTENER_CONNECTED, 0L);
    }
    static boolean selfTestSeen(Context c) {return prefs(c).getLong(TEST_SEEN, 0) > 0;}

    static void stop(Context c) {
        prefs(c).edit().remove(STARTED).remove(ENDS)
            .putBoolean(TEST_PENDING, false).apply();
    }

    static boolean requestSelfTest(Context c, long now) {
        if (!active(c, now)) return false;
        return prefs(c).edit().putBoolean(TEST_PENDING, true).putLong(TEST_SEEN, 0)
            .commit();
    }
    static void onListenerConnected(Context c, long now) {
        if (active(c, now)) prefs(c).edit().putLong(LISTENER_CONNECTED, now).apply();
    }
    static void onNotification(Context c, String packageName, long now) {
        if (!active(c, now)) return;
        if (WarningSource.SAPHE_PACKAGE.equals(packageName)) {
            SharedPreferences p = prefs(c);
            p.edit().putInt(SAPHE_COUNT, Math.min(9999, p.getInt(SAPHE_COUNT, 0) + 1))
                .putLong(LAST_SAPHE, now).apply();
        } else if (c.getPackageName().equals(packageName)
                && prefs(c).getBoolean(TEST_PENDING, false)) {
            prefs(c).edit().putLong(TEST_SEEN, now)
                .putBoolean(TEST_PENDING, false).apply();
        }
    }
}
