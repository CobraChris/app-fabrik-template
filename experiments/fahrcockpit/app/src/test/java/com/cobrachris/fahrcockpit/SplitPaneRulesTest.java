package com.cobrachris.fahrcockpit;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class SplitPaneRulesTest {
    @Test public void desiredRatioIsExactlySeventyThirty() {
        assertEquals(100, SplitPaneRules.DESIRED_MAP_PERCENT
            + SplitPaneRules.DESIRED_COCKPIT_PERCENT);
        assertEquals(70, SplitPaneRules.DESIRED_MAP_PERCENT);
    }

    @Test public void realSplitAlwaysRemovesAnyExistingOverlay() {
        assertEquals(true, SplitPaneRules.mustRemoveOverlay(true));
        assertEquals(false, SplitPaneRules.mustRemoveOverlay(false));
    }

    @Test public void noAutomaticOverlayFallbackIsPartOfSplitContract() {
        // The split activity controls the lifecycle and must never launch
        // a second floating cockpit after Samsung changes window mode.
        assertEquals(true, SplitPaneRules.mustRemoveOverlay(true));
    }

    @Test public void splitStateNeverClaimsAutomaticSizeControl() {
        assertEquals("Split aktiv · 70/30 über Trennlinie einstellen; als App-Paar speichern",
            SplitPaneRules.statusText(true));
        assertEquals("Split nicht bestätigt · App-Paar-Hilfe öffnen, kein Overlay-Autostart",
            SplitPaneRules.statusText(false));
    }
}
