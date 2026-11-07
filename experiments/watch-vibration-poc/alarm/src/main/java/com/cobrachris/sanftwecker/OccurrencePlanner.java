package com.cobrachris.sanftwecker;

import java.util.TimeZone;

/** Pure schedule computation: next weekly event, one skip, or a one-off replacement. */
public final class OccurrencePlanner {
    private OccurrencePlanner() {}

    public static long next(AlarmProfile p, long now, TimeZone timezone) {
        if (p == null || !p.enabled) return 0L;
        if (p.oneTimeAt > now) return p.oneTimeAt;
        long future = NextAlarmMath.nextWeekly(now, timezone, p.hour, p.minute, p.days);
        if (p.skipAt != 0 && Math.abs(future - p.skipAt) < 1000L) {
            return NextAlarmMath.nextWeekly(future, timezone, p.hour, p.minute, p.days);
        }
        return future;
    }

    public static long originalNext(AlarmProfile p, long now, TimeZone timezone) {
        return NextAlarmMath.nextWeekly(now, timezone, p.hour, p.minute, p.days);
    }
}
