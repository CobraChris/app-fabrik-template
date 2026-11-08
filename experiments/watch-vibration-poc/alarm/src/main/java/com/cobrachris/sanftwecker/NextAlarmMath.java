package com.cobrachris.sanftwecker;

import java.util.Calendar;
import java.util.TimeZone;

/** Pure calendar arithmetic, testable without an emulator. Monday is bit zero. */
public final class NextAlarmMath {
    private NextAlarmMath() {}

    public static long nextWeekly(long afterMillis, TimeZone zone, int hour,
                                  int minute, int dayMask) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59
                || (dayMask & 0x7f) == 0) {
            throw new IllegalArgumentException("Invalid time or selected weekdays");
        }
        Calendar candidate = Calendar.getInstance(zone);
        candidate.setTimeInMillis(afterMillis);
        candidate.set(Calendar.HOUR_OF_DAY, hour);
        candidate.set(Calendar.MINUTE, minute);
        candidate.set(Calendar.SECOND, 0);
        candidate.set(Calendar.MILLISECOND, 0);
        for (int offset = 0; offset <= 7; offset++) {
            int mondayIndex = (candidate.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            if ((dayMask & (1 << mondayIndex)) != 0
                    && candidate.getTimeInMillis() > afterMillis) {
                return candidate.getTimeInMillis();
            }
            candidate.add(Calendar.DAY_OF_MONTH, 1);
        }
        throw new IllegalStateException("No matching future weekday");
    }
}
