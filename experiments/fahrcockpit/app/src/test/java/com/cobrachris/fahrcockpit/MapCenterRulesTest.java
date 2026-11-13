package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

public final class MapCenterRulesTest {
    private static final long NOW = 1_800_000_000_000L;
    @Test public void freshAndAccuratePositionCanCenterMap() {
        assertTrue(MapCenterRules.acceptable(60.3172,24.9633,18f,NOW-2000,NOW));
        assertTrue(MapCenterRules.acceptable(53.55,9.99,250f,NOW-180000,NOW));
    }
    @Test public void rejectsStaleOrUnreliableLocation() {
        assertFalse(MapCenterRules.acceptable(60,25,251f,NOW-100,NOW));
        assertFalse(MapCenterRules.acceptable(60,25,5f,NOW-180001,NOW));
        assertFalse(MapCenterRules.acceptable(60,25,5f,NOW+1,NOW));
        assertFalse(MapCenterRules.acceptable(Double.NaN,25,5f,NOW-10,NOW));
        assertFalse(MapCenterRules.acceptable(91,25,5f,NOW-10,NOW));
        assertFalse(MapCenterRules.acceptable(60,181,5f,NOW-10,NOW));
        assertFalse(MapCenterRules.acceptable(60,25,Float.NaN,NOW-10,NOW));
        assertFalse(MapCenterRules.acceptable(60,25,-1f,NOW-10,NOW));
    }
}
