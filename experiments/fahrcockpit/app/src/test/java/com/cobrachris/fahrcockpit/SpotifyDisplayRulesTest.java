package com.cobrachris.fahrcockpit;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/** Regression coverage for real Samsung Spotify connection states. */
public final class SpotifyDisplayRulesTest {
    @Test public void missingPermissionHasSpecificAction() {
        assertEquals("Spotify-Medienzugriff fehlt – in FahrCockpit freigeben",
            SpotifyDisplayRules.notConnected(false));
    }

    @Test public void grantedPermissionWithoutSessionIsNotReportedAsDenied() {
        assertEquals("Spotify nicht aktiv – in Spotify Musik starten",
            SpotifyDisplayRules.notConnected(true));
    }

    @Test public void correctNextActionForMissingPermission() {
        assertEquals("Medienzugriff freigeben (Android)",
            SpotifyDisplayRules.nextAction(false, false));
    }

    @Test public void grantedPermissionOpensSpotifyButDoesNotRepeatConsent() {
        assertEquals("Spotify öffnen und Musik starten",
            SpotifyDisplayRules.nextAction(true, false));
    }

    @Test public void connectedSpotifyHidesSetupAction() {
        assertEquals(null, SpotifyDisplayRules.nextAction(true, true));
        assertEquals(null, SpotifyDisplayRules.nextAction(false, true));
        assertEquals("Spotify spielt · Steuerung aktiv",
            SpotifyDisplayRules.connectedStatus(true));
        assertEquals("Spotify verbunden · Wiedergabe pausiert",
            SpotifyDisplayRules.connectedStatus(false));
    }

    @Test public void formatsTitleAndArtist() {
        assertEquals("Elektrobank – Single Version · The Chemical Brothers",
            SpotifyDisplayRules.trackLabel("Elektrobank – Single Version",
                "The Chemical Brothers"));
    }

    @Test public void metadataMayBeMissingOrBlank() {
        assertEquals("Spotify verbunden", SpotifyDisplayRules.trackLabel(null, null));
        assertEquals("Spotify verbunden · Artist",
            SpotifyDisplayRules.trackLabel("  ", "Artist"));
        assertEquals("Track", SpotifyDisplayRules.trackLabel("Track", ""));
        assertEquals("Track · Artist",
            SpotifyDisplayRules.trackLabel(" Track ", " Artist "));
    }
}
