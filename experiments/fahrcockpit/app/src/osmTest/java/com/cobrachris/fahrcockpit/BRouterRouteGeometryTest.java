package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

public final class BRouterRouteGeometryTest {
    private static final String VALID = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\","
        + "\"properties\":{},\"geometry\":{\"type\":\"LineString\","
        + "\"coordinates\":[[9.9937,53.5511],[9.9942,53.5520],[9.995,53.553]]}}]}";

    @Test public void parsesRealGeoJsonCoordinatesInLonLatOrder() {
        BRouterRouteGeometry.Route route = BRouterRouteGeometry.parse(VALID);
        assertEquals(3, route.points.size());
        assertEquals(53.5511, route.points.get(0)[0], 0.00001);
        assertEquals(9.9937, route.points.get(0)[1], 0.00001);
    }

    @Test public void rejectsMissingGeometry() {
        try {
            BRouterRouteGeometry.parse("{\"type\":\"FeatureCollection\",\"features\":[]}");
            fail("Must reject no road line");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected);
        }
    }

    @Test public void rejectsInvalidCoordinates() {
        try {
            BRouterRouteGeometry.parse(VALID.replace("53.5511", "100.0"));
            fail("Invalid latitude");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected);
        }
    }

    @Test public void rejectsErrorTextRatherThanDrawingFakeRoute() {
        try {
            BRouterRouteGeometry.parse("Profile motorcar does not exist");
            fail("Never draw a fake line");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected);
        }
    }
}
