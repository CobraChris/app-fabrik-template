package com.cobrachris.sanftwecker;

import org.junit.Test;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class NextAlarmMathTest {
    private static final TimeZone BERLIN = TimeZone.getTimeZone("Europe/Berlin");
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");

    private static long at(TimeZone zone, int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance(zone);
        c.clear();
        c.set(year, month - 1, day, hour, minute);
        return c.getTimeInMillis();
    }

    @Test public void futureAlarmToday() {
        assertEquals(at(BERLIN, 2026, 11, 2, 7, 30),
                NextAlarmMath.nextWeekly(at(BERLIN, 2026, 11, 2, 6, 0), BERLIN, 7, 30, 1));
    }

    @Test public void sameTimeAlreadyPassedGetsFollowingWeek() {
        assertEquals(at(BERLIN, 2026, 11, 9, 7, 30),
                NextAlarmMath.nextWeekly(at(BERLIN, 2026, 11, 2, 7, 30), BERLIN, 7, 30, 1));
    }

    @Test public void fridayNextWorkingDayIsMonday() {
        assertEquals(at(BERLIN, 2026, 11, 9, 7, 0),
                NextAlarmMath.nextWeekly(at(BERLIN, 2026, 11, 6, 12, 0), BERLIN, 7, 0, 0x1f));
    }

    @Test public void independentWeekendSlot() {
        assertEquals(at(TOKYO, 2026, 11, 7, 9, 15),
                NextAlarmMath.nextWeekly(at(TOKYO, 2026, 11, 6, 20, 0), TOKYO, 9, 15, 0x60));
    }

    @Test public void dstSpringForwardPreservesLocalWakeTime() {
        assertEquals(at(BERLIN, 2026, 3, 30, 7, 0),
                NextAlarmMath.nextWeekly(at(BERLIN, 2026, 3, 27, 20, 0),
                        BERLIN, 7, 0, 0x1f));
    }

    @Test public void dstFallBackPreservesLocalWakeTime() {
        assertEquals(at(BERLIN, 2026, 10, 26, 7, 0),
                NextAlarmMath.nextWeekly(at(BERLIN, 2026, 10, 23, 20, 0),
                        BERLIN, 7, 0, 0x1f));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyWeekdays() {
        NextAlarmMath.nextWeekly(at(BERLIN, 2026, 11, 2, 6, 0), BERLIN, 7, 0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidHour() {
        NextAlarmMath.nextWeekly(at(BERLIN, 2026, 11, 2, 6, 0), BERLIN, 24, 0, 1);
    }
}
