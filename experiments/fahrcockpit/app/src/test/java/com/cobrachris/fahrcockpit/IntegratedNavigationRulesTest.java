package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

public final class IntegratedNavigationRulesTest {
    @Test public void mapOccupiesSevenPartsAndControlsThree() {
        assertEquals(7, IntegratedNavigationRules.MAP_WEIGHT);
        assertEquals(3, IntegratedNavigationRules.CONTROLS_WEIGHT);
        assertEquals(10, IntegratedNavigationRules.MAP_WEIGHT
            + IntegratedNavigationRules.CONTROLS_WEIGHT);
    }
    @Test public void coordinateTargetsMustBeFiniteAndInRange() {
        double[] position=IntegratedNavigationRules.parseCoordinates("35.6895, 139.6917");
        assertNotNull(position);
        assertEquals(35.6895, position[0],0.000001);
        assertEquals(139.6917, position[1],0.000001);
        assertNull(IntegratedNavigationRules.parseCoordinates("91,139"));
        assertNull(IntegratedNavigationRules.parseCoordinates("NaN,139"));
        assertNull(IntegratedNavigationRules.parseCoordinates("1,2,3"));
        assertNull(IntegratedNavigationRules.parseCoordinates("Tokyo Station"));
        assertNull(IntegratedNavigationRules.parseCoordinates(""));
    }
    @Test public void doNotAcceptInvalidGeocoderResults() {
        assertNull(IntegratedNavigationRules.displayCoordinates(Double.NaN, 139));
        assertNull(IntegratedNavigationRules.displayCoordinates(90, 181));
        assertEquals("-33.856800, 151.215300",
            IntegratedNavigationRules.displayCoordinates(-33.8568,151.2153));
    }
    @Test public void preserveNameAndLengthRulesFromPreviousRouteImplementation() {
        assertTrue(IntegratedNavigationRules.isValidTarget("Tokyo Station"));
        assertTrue(IntegratedNavigationRules.isValidTarget("京都駅"));
        assertFalse(IntegratedNavigationRules.isValidTarget("  "));
        assertFalse(IntegratedNavigationRules.isValidTarget("x".repeat(161)));
    }
}
