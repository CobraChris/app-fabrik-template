package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

/** Prevents accidental regression to a non-specific Maps launcher open. */
public final class GoogleMapsUrlRulesTest {
    @Test public void supportedActionExplicitlyOpensMapContent() {
        assertEquals("https://www.google.com/maps/@?api=1&map_action=map&layer=traffic",
            GoogleMapsUrlRules.MAP_WITH_TRAFFIC);
        assertTrue(GoogleMapsUrlRules.MAP_WITH_TRAFFIC.contains("api=1"));
        assertTrue(GoogleMapsUrlRules.MAP_WITH_TRAFFIC.contains("map_action=map"));
        assertFalse(GoogleMapsUrlRules.MAP_WITH_TRAFFIC.contains("map_action=pano"));
    }
    @Test public void trafficRequestIsPreserved() {
        assertTrue(GoogleMapsUrlRules.MAP_WITH_TRAFFIC.endsWith("&layer=traffic"));
        assertEquals("com.google.android.apps.maps",
            GoogleMapsUrlRules.GOOGLE_MAPS_PACKAGE);
    }
}
