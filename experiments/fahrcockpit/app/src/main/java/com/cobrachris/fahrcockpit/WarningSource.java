package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * The vendor's Android launcher is public, but Saphe does NOT offer a public
 * documented external warning/device control contract. This class only
 * launches the installed app and stores an explicit user preference.
 */
final class WarningSource {
    static final String SAPHE = "saphe";
    static final String BLITZER = "blitzer";
    static final String NONE = "none";
    static final String SAPHE_PACKAGE = "my.saphelink";

    private static final String PREFS = "fahrcockpit_warning_source_v1";
    private static final String KEY = "selected_source";

    private WarningSource() {}

    static String selected(Context context) {
        String stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, SAPHE);
        return SAPHE.equals(stored) || BLITZER.equals(stored) || NONE.equals(stored)
            ? stored : SAPHE;
    }

    static void setSelected(Context context, String choice) {
        if (!SAPHE.equals(choice) && !BLITZER.equals(choice) && !NONE.equals(choice)) {
            throw new IllegalArgumentException("Unknown warning source");
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, choice).apply();
    }

    static boolean isSapheInstalled(Context context) {
        return context.getPackageManager().getLaunchIntentForPackage(SAPHE_PACKAGE) != null;
    }

    static boolean openSaphe(Context context) {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(SAPHE_PACKAGE);
        if (launch == null) return false;
        try {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launch);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
