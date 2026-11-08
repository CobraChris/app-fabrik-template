package com.cobrachris.sanftwecker;
import org.junit.Test;
import static org.junit.Assert.*;

public class GentleGainTest {
    @Test public void trulyStartsAtZero() {
        assertEquals(0f, GentleGain.level(0, 0, 600, 35), 0f);
        assertEquals(0f, GentleGain.level(5000, 10, 600, 35), 0f);
        assertEquals(0f, GentleGain.level(10000, 10, 600, 35), 0f);
    }

    @Test public void gentleStartAndPeak() {
        assertEquals(0.0035f, GentleGain.level(60_000L, 0, 600, 35), 0.00001f);
        assertEquals(0.35f, GentleGain.level(600_000L, 0, 600, 35), 0.00001f);
        assertEquals(0.35f, GentleGain.level(900_000L, 0, 600, 35), 0.00001f);
    }

    @Test public void monotoneAndBounded() {
        float last = 0f;
        for (long millis=0; millis<2_000_000L; millis += 4000L) {
            float value = GentleGain.level(millis, 30, 900, 65);
            assertTrue(value >= last && value <= 0.65f);
            last = value;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void invalidEnvelope() {
        GentleGain.level(30_000L, 0, 0, 50);
    }
}
