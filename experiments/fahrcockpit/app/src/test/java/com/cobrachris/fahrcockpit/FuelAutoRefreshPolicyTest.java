package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

public final class FuelAutoRefreshPolicyTest {
    private static final long START = 1_000_000L;
    private static final double HAMBURG_LAT = 53.568d;
    private static final double HAMBURG_LNG = 9.981d;

    @Test public void providerApprovalIsRequiredBeforeAnyAutoRequests() {
        assertFalse(FuelAutoRefreshPolicy.PROVIDER_APPROVED);
        assertEquals(900_000L, FuelAutoRefreshPolicy.REGULAR_INTERVAL_MS);
        assertEquals(300_000L, FuelAutoRefreshPolicy.DISTANCE_MIN_INTERVAL_MS);
        assertEquals(5_000f, FuelAutoRefreshPolicy.DISTANCE_TRIGGER_METERS, 0.1f);
    }

    @Test public void firstAttemptCanBeImmediateAfterApprovalAndUserOptIn() {
        assertTrue(FuelAutoRefreshPolicy.isDue(START, 0L, 0L, false,
            0d, 0d, HAMBURG_LAT, HAMBURG_LNG));
    }

    @Test public void regularIntervalIsFifteenMinutes() {
        assertFalse(FuelAutoRefreshPolicy.isDue(START+899_000L, START, START,
            false, 0d, 0d, HAMBURG_LAT, HAMBURG_LNG));
        assertTrue(FuelAutoRefreshPolicy.isDue(START+900_000L, START, START,
            false, 0d, 0d, HAMBURG_LAT, HAMBURG_LNG));
    }

    @Test public void movementRequiresFiveKmAndFiveMinutes() {
        double movedLatitude = HAMBURG_LAT + 0.05d; // approx 5.56 km north
        assertTrue(FuelAutoRefreshPolicy.distanceMeters(HAMBURG_LAT, HAMBURG_LNG,
            movedLatitude, HAMBURG_LNG) > 5000d);
        assertFalse(FuelAutoRefreshPolicy.isDue(START+299_000L, START, START,
            true, HAMBURG_LAT, HAMBURG_LNG, movedLatitude, HAMBURG_LNG));
        assertTrue(FuelAutoRefreshPolicy.isDue(START+300_000L, START, START,
            true, HAMBURG_LAT, HAMBURG_LNG, movedLatitude, HAMBURG_LNG));
        assertFalse(FuelAutoRefreshPolicy.isDue(START+300_000L, START, START,
            true, HAMBURG_LAT, HAMBURG_LNG, HAMBURG_LAT+0.02d, HAMBURG_LNG));
    }

    @Test public void staleOrInaccuratePositionCannotTriggerNetwork() {
        assertTrue(FuelAutoRefreshPolicy.locationUsable(START, START-60_000L,
            10f, HAMBURG_LAT, HAMBURG_LNG));
        assertFalse(FuelAutoRefreshPolicy.locationUsable(START, START-121_000L,
            10f, HAMBURG_LAT, HAMBURG_LNG));
        assertFalse(FuelAutoRefreshPolicy.locationUsable(START, START-60_000L,
            200f, HAMBURG_LAT, HAMBURG_LNG));
        assertFalse(FuelAutoRefreshPolicy.locationUsable(START, START-60_000L,
            Float.NaN, HAMBURG_LAT, HAMBURG_LNG));
    }

    @Test public void invalidCoordinatesAndClockRollbackBlockScheduler() {
        assertFalse(FuelAutoRefreshPolicy.isDue(START, 0L, 0L, false,
            0d, 0d, 100d, HAMBURG_LNG));
        assertFalse(FuelAutoRefreshPolicy.isDue(START, 0L, 0L, false,
            0d, 0d, Double.NaN, HAMBURG_LNG));
        assertFalse(FuelAutoRefreshPolicy.isDue(START, START+1L, 0L, false,
            0d, 0d, HAMBURG_LAT, HAMBURG_LNG));
    }
}
