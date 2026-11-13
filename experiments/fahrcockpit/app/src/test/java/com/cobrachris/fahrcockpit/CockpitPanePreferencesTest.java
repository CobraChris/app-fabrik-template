package com.cobrachris.fahrcockpit;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class CockpitPanePreferencesTest {
    @Test public void defaultSideOffersSwitchToLeft() {
        assertEquals("⇄ Spotify nach links (im Stand)",
            CockpitPanePreferences.toggleLabel(false, true));
    }

    @Test public void afterSwitchCanMoveBackToRight() {
        assertEquals("⇄ Spotify nach rechts (im Stand)",
            CockpitPanePreferences.toggleLabel(true, true));
    }

    @Test public void portraitLayoutIsNotMisrepresentedAsLeftRight() {
        assertEquals("Spotify links/rechts im Querformat einstellen",
            CockpitPanePreferences.toggleLabel(true, false));
    }
}
