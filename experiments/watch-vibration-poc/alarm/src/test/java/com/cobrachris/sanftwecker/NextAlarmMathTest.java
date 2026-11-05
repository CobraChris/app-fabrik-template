package com.cobrachris.sanftwecker;

import org.junit.Test;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class NextAlarmMathTest {
    private static final TimeZone ZONE = TimeZone.getTimeZone("Europe/Berlin");
    private static long at(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance(ZONE);
        c.clear();
        c.set(year, month - 1, day, hour, minute);
        return c.getTimeInMillis();
    }

    @Test public void sameDayFuture() {
        assertEquals(at(2026, 11, 2, 7, 30),
                NextAlarmMath.nextWeekly(at(2026, 11, 2, 6, 0), ZONE, 7, 30, 1));
    }

    @Test public void missedMondayGoesNextMonday() {
        assertEquals(at(2026, 11, 9, 7, 30),
                NextAlarmMath.nextWeekly(at(2026, 11, 2, 7, 30), ZONE, 7, 30, 1));
    }

    @Test public void fridayToMonday() {
        assertEquals(at(2026, 11, 9, 7, 0),
                NextAlarmMath.nextWeekly(at(2026, 11, 6, 12, 0), ZONE, 7, 0, 0x1f));
    }

    @Test public void nextDaySaturday() {
        assertEquals(at(2026, 11, 7, 9, 15),
                NextAlarmMath.nextWeekly(at(2026, 11, 6, 20, 0), ZONE, 9, 15, 1 << 5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyWeekdays() {
        NextAlarmMath.nextWeekly(at(2026, 11, 2, 6, 0), ZONE, 7, 0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidHour() {
        NextAlarmMath.nextWeekly(at(2026, 11, 2, 6, 0), ZONE, 24, 0, 1);
    }
}
