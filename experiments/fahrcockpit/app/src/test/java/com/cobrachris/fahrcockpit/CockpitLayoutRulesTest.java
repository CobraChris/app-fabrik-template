package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

public final class CockpitLayoutRulesTest {
    @Test public void landscapeKeepsStrictSeventyThirty() {
        assertEquals(7, CockpitLayoutRules.mapWeight(true));
        assertEquals(3, CockpitLayoutRules.controlsWeight(true));
    }
    @Test public void portraitReservesRoomForSpotify() {
        assertEquals(9, CockpitLayoutRules.mapWeight(false));
        assertEquals(11, CockpitLayoutRules.controlsWeight(false));
        assertEquals(20, CockpitLayoutRules.mapWeight(false)
            + CockpitLayoutRules.controlsWeight(false));
    }

    @Test public void keyboardHidesMapUntilInputIsFinished() {
        assertFalse(CockpitLayoutRules.shouldShowMap(true));
        assertTrue(CockpitLayoutRules.shouldShowMap(false));
    }

    @Test public void safeInsetAvoidsBothKeyboardAndSystemNavigation() {
        assertEquals(320, CockpitLayoutRules.safeBottomInset(24, 320));
        assertEquals(24, CockpitLayoutRules.safeBottomInset(24, 0));
        assertEquals(30, CockpitLayoutRules.safeBottomInset(12, 30));
    }
}
