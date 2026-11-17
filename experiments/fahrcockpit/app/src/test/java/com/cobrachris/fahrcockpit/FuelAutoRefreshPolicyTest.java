package com.cobrachris.fahrcockpit;

import org.junit.Test;
import static org.junit.Assert.*;

public final class FuelAutoRefreshPolicyTest {
    private static final long MIN = 60000L;
    @Test public void firstVisibleNavigationRequestsOnce() {
        assertTrue(FuelAutoRefreshPolicy.shouldFetch(50*MIN, 0, 0, 0,
            false, true, true, true, true));
        assertFalse(FuelAutoRefreshPolicy.shouldFetch(50*MIN, 0, 0, 0,
            false, false, true, true, true));
        assertFalse(FuelAutoRefreshPolicy.shouldFetch(50*MIN, 0, 0, 0,
            false, true, false, true, true));
        assertFalse(FuelAutoRefreshPolicy.shouldFetch(50*MIN, 0, 0, 0,
            false, true, true, false, true));
        assertFalse(FuelAutoRefreshPolicy.shouldFetch(50*MIN, 0, 0, 0,
            false, true, true, true, false));
    }
    @Test public void noRegularPollingInsideFifteenMinutes() {
        assertFalse(FuelAutoRefreshPolicy.shouldFetch(20*MIN, 10*MIN, 10*MIN,
            0, false, true, true, true, true));
        assertTrue(FuelAutoRefreshPolicy.shouldFetch(26*MIN, 10*MIN, 10*MIN,
            0, false, true, true, true, true));
    }
    @Test public void fiveKilometerMovementNeedsFiveMinuteRequestGap() {
        assertFalse(FuelAutoRefreshPolicy.shouldFetch(14*MIN, 10*MIN, 10*MIN,
            0, true, true, true, true, true));
        assertTrue(FuelAutoRefreshPolicy.shouldFetch(15*MIN, 10*MIN, 10*MIN,
            0, true, true, true, true, true));
    }
    @Test public void errorsBackOffExponentiallyAndCap() {
        assertEquals(5*MIN, FuelAutoRefreshPolicy.backoffMs(1));
        assertEquals(10*MIN, FuelAutoRefreshPolicy.backoffMs(2));
        assertEquals(20*MIN, FuelAutoRefreshPolicy.backoffMs(3));
        assertEquals(30*MIN, FuelAutoRefreshPolicy.backoffMs(4));
        assertEquals(30*MIN, FuelAutoRefreshPolicy.backoffMs(999));
        assertFalse(FuelAutoRefreshPolicy.shouldFetch(19*MIN, 10*MIN, 0,
            2, true, true, true, true, true));
        assertTrue(FuelAutoRefreshPolicy.shouldFetch(20*MIN, 10*MIN, 0,
            2, true, true, true, true, true));
    }
    @Test public void invalidFutureClockBlocksRepeatedRequests() {
        assertFalse(FuelAutoRefreshPolicy.shouldFetch(50*MIN, 51*MIN, 0, 0,
            true, true, true, true, true));
        assertFalse(FuelAutoRefreshPolicy.shouldFetch(50*MIN, 10*MIN, 51*MIN, 0,
            true, true, true, true, true));
    }
}
