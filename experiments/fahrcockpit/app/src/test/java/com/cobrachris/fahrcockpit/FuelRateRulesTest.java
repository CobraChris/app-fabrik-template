package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

public final class FuelRateRulesTest {
    @Test public void firstManualRequestIsImmediate() {
        assertEquals(0L, FuelRateRules.remainingMillis(1_000_000L, 0L, 0L));
    }
    @Test public void allRequestsShareASixtyFiveSecondMinimum() {
        long start = 1_000_000L;
        assertEquals(65_000L, FuelRateRules.remainingMillis(start, start, 0L));
        assertEquals(1_000L, FuelRateRules.remainingMillis(start+64_000, start, 0L));
        assertEquals(0L, FuelRateRules.remainingMillis(start+65_000, start, 0L));
    }
    @Test public void tooManyOrRateLimitedRequestsBackOffForAnHour() {
        assertEquals(3_600_000L, FuelRateRules.backoffMillis(503));
        assertEquals(3_600_000L, FuelRateRules.backoffMillis(429));
        long now = 1_000_000L;
        assertEquals(3_600_000L,
            FuelRateRules.remainingMillis(now, now, now+3_600_000L));
        assertEquals(0L, FuelRateRules.remainingMillis(
            now+3_600_000L, now, now+3_600_000L));
    }
    @Test public void transientServerFailuresAvoidRapidRetry() {
        assertEquals(300_000L, FuelRateRules.backoffMillis(500));
        assertEquals(300_000L, FuelRateRules.backoffMillis(502));
        assertEquals(300_000L, FuelRateRules.backoffMillis(504));
        assertEquals(0L, FuelRateRules.backoffMillis(200));
    }
    @Test public void clockRollbackNeverBypassesLimiter() {
        assertEquals(65_000L, FuelRateRules.remainingMillis(900L, 1_000L, 0L));
        assertEquals(65_000L, FuelRateRules.remainingMillis(0L, 0L, 0L));
    }
    @Test public void approvedAutomaticCadenceWouldNotExceedFiveMinutes() {
        assertTrue(FuelRateRules.MIN_APPROVED_AUTO_INTERVAL_MS
            >= 5L * 60_000L);
        assertTrue(FuelRateRules.MIN_APPROVED_AUTO_INTERVAL_MS
            > FuelRateRules.MIN_REQUEST_INTERVAL_MS);
    }
    @Test public void rateLimitMessageIsHumanReadable() {
        assertEquals("65 Sek.", FuelRateRules.remainingLabel(65_000L));
        assertEquals("2 Min.", FuelRateRules.remainingLabel(66_000L));
        assertEquals("60 Min.", FuelRateRules.remainingLabel(3_600_000L));
    }
}
