package com.cobrachris.fahrcockpit;

import org.junit.Test;
import static org.junit.Assert.*;

public final class ParkingTripTrackerTest {
    @Test public void successfulCarTripStoppedAtDestinationProducesParkingCandidate() {
        ParkingTripTracker t = movingTrip();
        t.observe(76_000L, 53.55, 10.007, 12f, 0.2f);
        ParkingTripTracker.Spot spot = t.finish(80_000L);
        assertNotNull(spot);
        assertEquals(53.55, spot.lat, 0.000001);
        assertEquals(10.007, spot.lng, 0.000001);
    }

    @Test public void movingCarIsNeverAutomaticallyClassifiedAsParked() {
        ParkingTripTracker t = movingTrip();
        assertNull(t.finish(60_000L));
    }

    @Test public void shortTripOrTrafficLightWithoutRouteEndNeverSaves() {
        ParkingTripTracker t = new ParkingTripTracker();
        t.start(1_000L);
        t.observe(10_000L, 53.55, 10.0, 10f, 7f);
        t.observe(15_000L, 53.55, 10.0003, 10f, 0.1f);
        assertNull(t.finish(70_000L));
    }

    @Test public void staleOrInaccurateFixCannotBeUsedForAutomaticOrManualParking() {
        ParkingTripTracker t = movingTrip();
        t.observe(76_000L, 53.55, 10.007, 60f, 0.2f);
        assertNull(t.finish(80_000L));
        assertNull(t.manualFix(120_000L));
    }

    @Test public void rejectsInvalidCoordinatesAndOldManualFixes() {
        ParkingTripTracker t = new ParkingTripTracker();
        t.observe(10_000L, Double.NaN, 10d, 10f, 0f);
        assertNull(t.manualFix(10_100L));
        t.observe(11_000L, 53.55, 10d, 12f, 0.2f);
        assertNotNull(t.manualFix(12_000L));
        assertNull(t.manualFix(32_000L));
    }

    @Test public void routeResetDoesNotReuseOldDrivingHistory() {
        ParkingTripTracker t = movingTrip();
        t.start(77_000L);
        t.observe(80_000L, 53.55, 10.007, 12f, 0.2f);
        assertNull(t.finish(160_000L));
    }

    private static ParkingTripTracker movingTrip() {
        ParkingTripTracker t = new ParkingTripTracker();
        t.start(1_000L);
        for (int i = 0; i <= 5; i++) {
            t.observe(6_000L + 10_000L * i,
                53.55, 10.0014d * i, 12f, 8f);
        }
        return t;
    }
}
