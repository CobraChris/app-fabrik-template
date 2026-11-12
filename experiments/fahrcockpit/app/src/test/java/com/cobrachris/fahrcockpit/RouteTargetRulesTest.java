package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

/** Real URL formats per Google's documented no-key Maps intents. */
public final class RouteTargetRulesTest {
    @Test public void normalizesNamesWithoutSilentRouting() {
        assertEquals("Tokyo Station, Japan", RouteTargetRules.clean("  Tokyo   Station, Japan "));
        assertNull(RouteTargetRules.clean("  "));
        assertNull(RouteTargetRules.clean(null));
        assertNull(RouteTargetRules.clean("a".repeat(161)));
        assertEquals("Nikkō 東武日光駅", RouteTargetRules.clean("Nikkō 東武日光駅"));
    }
    @Test public void coordinatesAreFiniteAndRangeChecked() {
        assertEquals("36.748400,139.614300",
            RouteTargetRules.coordinates("36.7484","139.6143"));
        assertNull(RouteTargetRules.coordinates("91", "139"));
        assertNull(RouteTargetRules.coordinates("36", "181"));
        assertNull(RouteTargetRules.coordinates("NaN", "139"));
        assertNull(RouteTargetRules.coordinates("Infinity", "0"));
        assertNull(RouteTargetRules.coordinates("abc", "0"));
    }
    @Test public void routeOverviewIsActualGoogleMapsDirectionsWithoutApiKey() {
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=Tokyo+Station%2C+Japan&travelmode=driving",
            RouteTargetRules.directionsUrl("Tokyo Station, Japan"));
        assertNull(RouteTargetRules.directionsUrl("   "));
    }
    @Test public void startNavigationUsesDocumentedGoogleNavigationScheme() {
        assertEquals("google.navigation:q=36.748400%2C139.614300&mode=d",
            RouteTargetRules.navigationUri("36.748400,139.614300"));
        assertEquals("google.navigation:q=Nikk%C5%8D+%E6%9D%B1%E6%AD%A6%E6%97%A5%E5%85%89%E9%A7%85&mode=d",
            RouteTargetRules.navigationUri("Nikkō 東武日光駅"));
        assertNull(RouteTargetRules.navigationUri("\t "));
    }
}
