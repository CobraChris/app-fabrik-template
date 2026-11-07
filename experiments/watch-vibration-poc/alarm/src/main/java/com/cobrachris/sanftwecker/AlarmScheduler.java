package com.cobrachris.sanftwecker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.util.TimeZone;

/** Independent PendingIntents for every stable ID plus preview and snooze. */
public final class AlarmScheduler {
    public static final String ACTION_WEEKLY = "com.cobrachris.sanftwecker.ALARM";
    public static final String ACTION_TEST = "com.cobrachris.sanftwecker.TEST";
    public static final String ACTION_SNOOZE = "com.cobrachris.sanftwecker.SNOOZE_ALARM";
    public static final String EXTRA_ID = "alarm_id";
    public static final String EXTRA_WHEN = "alarm_when";
    public static final String PREF = "sanftwecker_runtime";
    public static final String TEST_WHEN = "test_when";
    private static final int TEST_REQUEST = 3101;
    private static final int SNOOZE_REQUEST = 3102;

    private AlarmScheduler() {}

    public static boolean canSchedule(Context c) {
        AlarmManager manager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        return manager != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || manager.canScheduleExactAlarms());
    }

    private static PendingIntent pending(Context c, String action, int code, int id, long when) {
        Intent intent = new Intent(c, AlarmReceiver.class).setAction(action)
                .putExtra(EXTRA_ID, id).putExtra(EXTRA_WHEN, when);
        return PendingIntent.getBroadcast(c, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static int weeklyCode(int id) { return 1000 + id; }

    private static void schedule(Context c, long when, String action, int code, int id) {
        AlarmManager manager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (manager == null || !canSchedule(c)) throw new SecurityException("Exact alarm not allowed");
        PendingIntent open = PendingIntent.getActivity(c, 5000 + id,
                new Intent(c, MainActivity.class).putExtra(EXTRA_ID, id),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.setAlarmClock(new AlarmManager.AlarmClockInfo(when, open),
                pending(c, action, code, id, when));
    }

    public static synchronized void reschedule(Context c, int id) {
        AlarmProfile p = AlarmRepository.get(c, id);
        cancel(c, ACTION_WEEKLY, weeklyCode(id), id);
        if (p == null || !p.enabled || !canSchedule(c)) {
            AlarmRepository.next(c, id, 0L);
            return;
        }
        long now = System.currentTimeMillis();
        long when = OccurrencePlanner.next(p, now, TimeZone.getDefault());
        try {
            schedule(c, when, ACTION_WEEKLY, weeklyCode(id), id);
            AlarmRepository.next(c, id, when);
        } catch (RuntimeException ex) {
            AlarmRepository.next(c, id, 0L);
            throw ex;
        }
    }

    public static void rescheduleAll(Context c) {
        RuntimeException first = null;
        for (int id : AlarmRepository.ids(c)) {
            try { reschedule(c, id); }
            catch (RuntimeException e) { if (first == null) first = e; }
        }
        if (first != null) throw first;
    }

    public static void cancelWeekly(Context c, int id) {
        cancel(c, ACTION_WEEKLY, weeklyCode(id), id);
        AlarmRepository.next(c, id, 0L);
    }

    public static void toggle(Context c, int id, boolean on) {
        AlarmProfile previous = AlarmRepository.get(c, id);
        if (previous == null) return;
        if (on && !canSchedule(c)) throw new SecurityException("Exakte Alarme nicht erlaubt");
        AlarmRepository.put(c, previous.withEnabled(on));
        try { reschedule(c, id); }
        catch (RuntimeException e) {
            AlarmRepository.put(c, previous);
            reschedule(c, id);
            throw e;
        }
    }

    public static void skipNext(Context c, int id) {
        AlarmProfile p = AlarmRepository.get(c, id);
        if (p == null || !p.enabled) return;
        long original = p.oneTimeAt > System.currentTimeMillis() && p.replacedAt > 0
                ? p.replacedAt
                : OccurrencePlanner.originalNext(p, System.currentTimeMillis(), TimeZone.getDefault());
        AlarmRepository.put(c, p.withExceptions(original, 0L, 0L));
        reschedule(c, id);
    }

    public static void undoSkip(Context c, int id) {
        AlarmProfile p = AlarmRepository.get(c, id);
        if (p == null) return;
        AlarmRepository.put(c, p.withExceptions(0, p.oneTimeAt, p.replacedAt));
        reschedule(c, id);
    }

    public static void changeNext(Context c, int id, long replacement) {
        AlarmProfile p = AlarmRepository.get(c, id);
        long now = System.currentTimeMillis();
        if (p == null || !p.enabled || replacement <= now ||
                replacement > now + 60L * 24 * 60 * 60 * 1000) {
            throw new IllegalArgumentException("Zeitpunkt ungültig oder Wecker nicht aktiv");
        }
        long original = p.oneTimeAt > now && p.replacedAt > 0 ? p.replacedAt
                : OccurrencePlanner.originalNext(p, now, TimeZone.getDefault());
        AlarmRepository.put(c, p.withExceptions(0L, replacement, original));
        reschedule(c, id);
    }

    public static void onFired(Context c, int id, long scheduledAt) {
        AlarmProfile p = AlarmRepository.get(c, id);
        if (p == null || !p.enabled) return;
        if (p.oneTimeAt > 0 && Math.abs(scheduledAt - p.oneTimeAt) < 1000L) {
            // The original weekly event is now suppressed after its one-off replacement.
            AlarmRepository.put(c, p.withExceptions(p.replacedAt, 0L, 0L));
        } else if (p.skipAt > 0 && scheduledAt > p.skipAt) {
            AlarmRepository.put(c, p.withExceptions(0L, p.oneTimeAt, p.replacedAt));
        }
        reschedule(c, id);
    }

    public static long test(Context c, int id) {
        if (AlarmRepository.get(c, id) == null) throw new IllegalArgumentException("Wecker fehlt");
        long when = System.currentTimeMillis() + 30_000L;
        schedule(c, when, ACTION_TEST, TEST_REQUEST, id);
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putLong(TEST_WHEN, when).apply();
        return when;
    }

    public static void cancelTest(Context c) {
        cancel(c, ACTION_TEST, TEST_REQUEST, 1);
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putLong(TEST_WHEN, 0L).apply();
    }

    public static long testWhen(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(TEST_WHEN, 0L);
    }

    public static long snooze(Context c, int id, int minutes) {
        if (AlarmRepository.get(c, id) == null) throw new IllegalArgumentException("Wecker fehlt");
        long when = System.currentTimeMillis() + minutes * 60_000L;
        schedule(c, when, ACTION_SNOOZE, SNOOZE_REQUEST, id);
        AlarmRepository.diagnostic(c, "Schlummern für " + minutes + " Minuten");
        return when;
    }

    public static void cancelSnooze(Context c) {
        cancel(c, ACTION_SNOOZE, SNOOZE_REQUEST, 1);
    }

    public static void delete(Context c, int id) {
        cancelWeekly(c, id);
        // Prevent a deleted alarm from ringing through a pending preview or snooze.
        cancelTest(c);
        cancelSnooze(c);
        AlarmRepository.delete(c, id);
    }

    private static void cancel(Context c, String action, int code, int id) {
        AlarmManager manager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) manager.cancel(pending(c, action, code, id, 0L));
    }
}
