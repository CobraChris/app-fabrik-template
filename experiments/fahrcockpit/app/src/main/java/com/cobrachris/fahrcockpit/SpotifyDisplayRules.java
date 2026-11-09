package com.cobrachris.fahrcockpit;

/**
 * Pure, testable Spotify presentation rules.
 *
 * A missing MediaSession is NOT evidence that the user's Notification Listener
 * permission has been revoked. Android may simply have no active Spotify session.
 */
final class SpotifyDisplayRules {
    private SpotifyDisplayRules() {}

    static String notConnected(boolean hasMediaAccess) {
        return hasMediaAccess
            ? "Spotify nicht aktiv – in Spotify Musik starten"
            : "Spotify-Medienzugriff fehlt – in FahrCockpit freigeben";
    }

    static String trackLabel(String title, String artist) {
        String safeTitle = title == null ? "" : title.trim();
        String safeArtist = artist == null ? "" : artist.trim();
        String primary = safeTitle.isEmpty() ? "Spotify verbunden" : safeTitle;
        return safeArtist.isEmpty() ? primary : primary + " · " + safeArtist;
    }
}
