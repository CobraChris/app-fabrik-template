package com.cobrachris.sanftwecker;
import org.junit.Test;
import static org.junit.Assert.*;

public class AlarmProfileTest {
    @Test public void threeDefaultsDisabled() {
        assertEquals(AlarmProfile.COUNT, 3);
        for (int id=1; id<=AlarmProfile.COUNT; id++) {
            AlarmProfile p = AlarmProfile.defaults(id);
            assertFalse(p.enabled);
            assertTrue(p.maxVolume > 0);
            assertTrue(p.days != 0);
        }
        assertEquals(0x1f, AlarmProfile.defaults(1).days);
        assertEquals(0x60, AlarmProfile.defaults(2).days);
    }
    @Test public void enablesWithoutLosingValues() {
        AlarmProfile p = AlarmProfile.defaults(2);
        AlarmProfile q = p.withEnabled(true);
        assertTrue(q.enabled);
        assertEquals(p.rampSeconds, q.rampSeconds);
        assertEquals(p.days, q.days);
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsNoWeekday() {
        new AlarmProfile(1, "X", true, 7, 0, 0, 0, 300, 30, 5, 20, 5, 0);
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsUnsafeHighDelay() {
        new AlarmProfile(1, "X", true, 7, 0, 1, 4000, 300, 30, 5, 20, 5, 0);
    }
}
