package com.cobrachris.fahrcockpit;

import android.content.Context;

/** Local per-device choice for the Google Navigation SDK guidance audio. */
final class NavigationVoicePreferences {
    private static final String PREFS = "fahrcockpit_voice_guidance_v1";
    private static final String KEY_MUTED = "muted";

    private NavigationVoicePreferences() {}

    static boolean isMuted(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_MUTED, false);
    }

    static boolean save(Context context, boolean muted) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_MUTED, muted).commit();
    }

    static String buttonLabel(boolean muted) {
        return muted ? "🔇 Ansagen aus" : "🔊 Ansagen an";
    }
}
