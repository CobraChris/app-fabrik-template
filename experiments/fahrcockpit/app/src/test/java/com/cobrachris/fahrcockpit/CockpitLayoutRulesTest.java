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
}
