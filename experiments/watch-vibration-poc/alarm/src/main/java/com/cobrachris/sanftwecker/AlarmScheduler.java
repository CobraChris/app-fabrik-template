package com.cobrachris.sanftwecker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.TimeZone;

/** Three independent weekly exact alarms plus one manual test and one snooze. */
public final class AlarmScheduler {
    public static final String ACTION_WEEKLY = "com.cobrachris.sanftwecker.ALARM";
    public static final String ACTION_TEST = "com.cobrachris.sanftwecker.TEST";
    public static final String ACTION_SNOOZE = "com.cobrachris.sanftwecker.SNOOZE_ALARM";
    public static final String EXTRA_ID = "alarm_id";
    public static final String PREF = "sanftwecker_runtime";
    public static final String TEST_WHEN = "test_when";

    private AlarmScheduler() {}

    public static boolean canSchedule(Context c) {
        AlarmManager manager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        return manager != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || manager.canScheduleExactAlarms());
    }

    private static PendingIntent pending(Context c, String action, int code, int alarmId) {
        Intent intent = new Intent(c, AlarmReceiver.class).setAction(action)
                .putExtra(EXTRA_ID, alarmId);
        return PendingIntent.getBroadcast(c, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static int requestCode(int id) { return 1000 + id; }

    private static void schedule(Context c, long when, String action, int code, int id) {
        AlarmManager manager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (manager == null || !canSchedule(c)) throw new SecurityException("Exact alarm not allowed");
        Intent intent = new Intent(c, MainActivity.class).putExtra(EXTRA_ID, id);
        PendingIntent show = PendingIntent.getActivity(c, 1100 + id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.setAlarmClock(new AlarmManager.AlarmClockInfo(when, show),
                pending(c, action, code, id));
    }

    public static void reschedule(Context c, int id) {
        AlarmProfile a = AlarmRepository.get(c, id);
        cancel(c, ACTION_WEEKLY, requestCode(id), id);
        if (!a.enabled || !canSchedule(c)) {
            AlarmRepository.next(c, id, 0L);
            return;
        }
        long next = NextAlarmMath.nextWeekly(System.currentTimeMillis(),
                TimeZone.getDefault(), a.hour, a.minute, a.days);
        try {
            schedule(c, next, ACTION_WEEKLY, requestCode(id), id);
            AlarmRepository.next(c, id, next);
        } catch (RuntimeException e) {
            AlarmRepository.next(c, id, 0L);
            throw e;
        }
    }

    public static void rescheduleAll(Context c) {
        for (int id = 1; id <= AlarmProfile.COUNT; id++) reschedule(c, id);
    }

    public static long test(Context c, int profileId) {
        if (!canSchedule(c)) throw new SecurityException("Exact alarm permission missing");
        long when = System.currentTimeMillis() + 30_000L;
        schedule(c, when, ACTION_TEST, 1901, profileId);
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putLong(TEST_WHEN, when).apply();
        return when;
    }

    public static void cancelTest(Context c) {
        cancel(c, ACTION_TEST, 1901, 1);
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putLong(TEST_WHEN, 0L).apply();
    }

    public static long testWhen(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(TEST_WHEN, 0L);
    }

    public static void snooze(Context c, int id, int minutes) {
        schedule(c, System.currentTimeMillis() + minutes * 60_000L,
                ACTION_SNOOZE, 1951, id);
        AlarmRepository.diagnostic(c, "Schlummern für " + minutes + " Minuten");
    }

    public static void cancelSnooze(Context c) {
        cancel(c, ACTION_SNOOZE, 1951, 1);
    }

    private static void cancel(Context c, String action, int requestCode, int id) {
        AlarmManager manager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) manager.cancel(pending(c, action, requestCode, id));
    }
}
