package com.cobrachris.fahrcockpit;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class SplitPaneRulesTest {
    @Test public void desiredRatioIsExactlySeventyThirty() {
        assertEquals(100, SplitPaneRules.DESIRED_MAP_PERCENT
            + SplitPaneRules.DESIRED_COCKPIT_PERCENT);
        assertEquals(70, SplitPaneRules.DESIRED_MAP_PERCENT);
    }

    @Test public void splitStateNeverClaimsAutomaticSizeControl() {
        assertEquals("Split-Ansicht aktiv · Trennlinie auf 70/30 einstellen",
            SplitPaneRules.statusText(true));
        assertEquals("Split noch nicht bestätigt · Samsung Multi Window/App-Paar verwenden",
            SplitPaneRules.statusText(false));
    }
}
