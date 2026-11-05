package com.cobrachris.sanftwecker;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public final class OccurrencePlannerTest {
    private final TimeZone zone = TimeZone.getTimeZone("Europe/Berlin");
    private long monday(int day) {
        Calendar c = Calendar.getInstance(zone);
        c.clear();
        c.set(2026, Calendar.NOVEMBER, day, 6, 0);
        return c.getTimeInMillis();
    }
    @Test public void skipOneOccurrenceOnly() {
        AlarmProfile base = AlarmProfile.defaults(1).withEnabled(true);
        long now = monday(2);
        long first = OccurrencePlanner.next(base, now, zone);
        long second = OccurrencePlanner.next(base.withExceptions(first,0,0), now, zone);
        assertTrue(second > first);
        assertEquals(24L*60*60*1000, second-first);
    }
    @Test public void replacingNextWorks() {
        AlarmProfile base = AlarmProfile.defaults(1).withEnabled(true);
        long now=monday(2);
        long original=OccurrencePlanner.next(base,now,zone);
        long override=original - 10*60*1000L;
        assertEquals(override, OccurrencePlanner.next(base.withExceptions(0,override,original),now,zone));
    }
    @Test public void disabledProducesNoTime() {
        assertEquals(0, OccurrencePlanner.next(AlarmProfile.defaults(1), monday(2),zone));
    }
}
