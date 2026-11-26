package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

public final class FuelLocationQualityTest {
    @Test public void requiresRecentAndAccurateGermanLocation() {
        long now = 1_000_000L;
        assertTrue(FuelLocationQuality.usable(now, now - 2_000L, 15f, 53.55, 10));
        assertTrue(FuelLocationQuality.usable(now, now - 15_000L, 250f, 53.55, 10));
        assertFalse(FuelLocationQuality.usable(now, now - 15_001L, 15f, 53.55, 10));
        assertFalse(FuelLocationQuality.usable(now, now - 2_000L, 300f, 53.55, 10));
        assertFalse(FuelLocationQuality.usable(now, now - 2_000L, Float.NaN, 53.55, 10));
        assertFalse(FuelLocationQuality.usable(now, now + 1_000L, 15f, 53.55, 10));
        assertFalse(FuelLocationQuality.usable(now, now - 2_000L, 15f, 35, 139));
    }
}
