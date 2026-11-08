package com.cobrachris.fahrcockpit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public final class OverlayRulesTest {
    @Test public void clampsPositionToFullyVisibleArea() {
        assertEquals(0, OverlayRules.clampPosition(-100, 1080, 940));
        assertEquals(140, OverlayRules.clampPosition(300, 1080, 940));
        assertEquals(140, OverlayRules.clampPosition(140, 1080, 940));
        assertEquals(0, OverlayRules.clampPosition(500, 350, 500));
        assertEquals(0, OverlayRules.clampPosition(500, 0, 200));
        assertEquals(0, OverlayRules.clampPosition(500, 300, 0));
    }

    @Test public void sapheChoiceNeverShowsMisleadingBlitzerStop() {
        assertEquals("Saphe öffnen", OverlayRules.primaryLabel(WarningSource.SAPHE));
        assertTrue(OverlayRules.primaryEnabled(WarningSource.SAPHE));
        assertFalse(OverlayRules.showBlitzerStop(WarningSource.SAPHE));
    }

    @Test public void blitzerChoiceShowsStartAndExplicitStopRequest() {
        assertEquals("Blitzer Start", OverlayRules.primaryLabel(WarningSource.BLITZER));
        assertTrue(OverlayRules.primaryEnabled(WarningSource.BLITZER));
        assertTrue(OverlayRules.showBlitzerStop(WarningSource.BLITZER));
    }

    @Test public void noWarningChoiceDisablesWarningButtons() {
        assertEquals("Keine Auswahl", OverlayRules.primaryLabel(WarningSource.NONE));
        assertFalse(OverlayRules.primaryEnabled(WarningSource.NONE));
        assertFalse(OverlayRules.showBlitzerStop(WarningSource.NONE));
        assertFalse(OverlayRules.primaryEnabled("unknown"));
    }
}
