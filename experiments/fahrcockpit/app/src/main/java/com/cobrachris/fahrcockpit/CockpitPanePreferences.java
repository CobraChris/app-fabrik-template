package com.cobrachris.fahrcockpit;

import android.content.Context;

/** Persist the user's Spotify control-pane position across app restarts. */
final class CockpitPanePreferences {
    private static final String PREFS = "fahrcockpit_cockpit_layout_v1";
    private static final String KEY_SPOTIFY_LEFT = "spotify_left";

    private CockpitPanePreferences() {}

    static boolean spotifyLeft(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SPOTIFY_LEFT, false); // Google map left by default.
    }

    static boolean saveSpotifyLeft(Context context, boolean left) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SPOTIFY_LEFT, left).commit();
    }

    static String toggleLabel(boolean spotifyLeft, boolean landscape) {
        if (!landscape) return "Spotify links/rechts im Querformat einstellen";
        return spotifyLeft ? "⇄ Spotify nach rechts (im Stand)"
                           : "⇄ Spotify nach links (im Stand)";
    }
}
