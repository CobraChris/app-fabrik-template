package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class OsmTurnGuidanceTest {
    private BRouterRouteGeometry.Route route() {
        List<double[]> points=new ArrayList<>();
        points.add(new double[]{53.5500,9.9900});
        points.add(new double[]{53.5500,9.9915});
        points.add(new double[]{53.5510,9.9915});
        List<BRouterRouteGeometry.Turn> turns=new ArrayList<>();
        turns.add(new BRouterRouteGeometry.Turn(1,5,0));
        return new BRouterRouteGeometry.Route("{}",points,turns);
    }

    @Test public void announcesRealTurnAheadAndOnceNearJunction() {
        OsmTurnGuidance model=new OsmTurnGuidance(route());
        OsmTurnGuidance.Step far=model.onPosition(53.5500,9.9900,6f);
        assertTrue(far.speech.startsWith("In "));
        assertTrue(far.speech.contains("rechts abbiegen"));
        assertNull(model.onPosition(53.5500,9.9900,6f).speech);
        OsmTurnGuidance.Step near=model.onPosition(53.5500,9.99115,6f);
        assertEquals("Jetzt rechts abbiegen",near.speech);
        assertNull(model.onPosition(53.5500,9.99115,6f).speech);
    }

    @Test public void unreliableGpsCannotTriggerManeuverOrReroute() {
        OsmTurnGuidance model=new OsmTurnGuidance(route());
        OsmTurnGuidance.Step unreliable=model.onPosition(53.55,9.9900,90f);
        assertFalse(unreliable.reliable);
        assertNull(unreliable.speech);
        assertFalse(unreliable.offRoute);
        assertNull(model.onPosition(53.55,9.9900,Float.NaN).speech);
    }

    @Test public void threeAccurateOffRouteReadingsRecommendOneReroute() {
        OsmTurnGuidance model=new OsmTurnGuidance(route());
        assertFalse(model.onPosition(53.553,9.994,5f).offRoute);
        assertFalse(model.onPosition(53.553,9.994,5f).offRoute);
        assertTrue(model.onPosition(53.553,9.994,5f).offRoute);
        assertFalse(model.onPosition(53.553,9.994,5f).offRoute);
    }

    @Test public void noHintsNeverInventsTurn() {
        BRouterRouteGeometry.Route r=route();
        OsmTurnGuidance model=new OsmTurnGuidance(new BRouterRouteGeometry.Route(
            "{}",r.points,new ArrayList<>()));
        assertFalse(model.hasManeuvers());
        assertNull(model.onPosition(53.5500,9.9900,5f).speech);
    }

    @Test public void goalArrivalIsAnnouncedOnce() {
        OsmTurnGuidance model=new OsmTurnGuidance(route());
        assertEquals("Ziel erreicht",model.onPosition(53.5510,9.9915,5f).speech);
        assertNull(model.onPosition(53.5510,9.9915,5f).speech);
    }

    @Test public void distanceCalculationInMeters() {
        assertEquals(0.0,OsmTurnGuidance.distance(53.55,9.99,53.55,9.99),.01);
        assertTrue(OsmTurnGuidance.distance(53.55,9.99,53.55,9.991)>60);
    }
}
