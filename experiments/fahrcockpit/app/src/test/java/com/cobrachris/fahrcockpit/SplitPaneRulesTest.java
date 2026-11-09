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
        assertEquals(false, SplitPaneRules.shouldStartFallback(true,
            true, false, true));
    }

    @Test public void fallbackOnlyStartsWhenSplitReallyFailed() {
        assertEquals(true, SplitPaneRules.shouldStartFallback(false,
            true, false, true));
        assertEquals(false, SplitPaneRules.shouldStartFallback(false,
            false, false, true));
        assertEquals(false, SplitPaneRules.shouldStartFallback(false,
            true, true, true));
        assertEquals(false, SplitPaneRules.shouldStartFallback(false,
            true, false, false));
    }

    @Test public void splitStateNeverClaimsAutomaticSizeControl() {
        assertEquals("Split-Ansicht aktiv · Trennlinie auf 70/30 einstellen",
            SplitPaneRules.statusText(true));
        assertEquals("Split noch nicht bestätigt · Samsung Multi Window/App-Paar verwenden",
            SplitPaneRules.statusText(false));
    }
}
