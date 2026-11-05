package com.cobrachris.sanftwecker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.util.TimeZone;

/** Single upcoming weekly alarm plus separate preview and snooze alarms. */
public final class AlarmScheduler {
    public static final String PREF = "sanftwecker_settings_v1";
    public static final String ENABLED = "enabled";
    public static final String HOUR = "hour";
    public static final String MINUTE = "minute";
    public static final String DAYS = "days_mask";
    public static final String RAMP = "ramp_seconds";
    public static final String MAX_VOLUME = "max_volume";
    public static final String NEXT = "next_trigger";
    public static final String PREVIEW_NEXT = "preview_trigger";
    public static final int DEFAULT_DAYS = 0x1f; // Mon-Fri

    private static final int DAILY_REQUEST = 401;
    private static final int PREVIEW_REQUEST = 402;
    private static final int SNOOZE_REQUEST = 403;

    private AlarmScheduler() {}

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static boolean canSchedule(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return manager != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || manager.canScheduleExactAlarms());
    }

    private static PendingIntent broadcast(Context context, String action, int requestCode) {
        Intent alarm = new Intent(context, AlarmReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(context, requestCode, alarm,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void setAlarm(Context context, long when, String action, int requestCode) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) throw new IllegalStateException("AlarmManager unavailable");
        if (!canSchedule(context)) throw new SecurityException("Exact alarms not permitted");
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent show = PendingIntent.getActivity(context, 405, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.setAlarmClock(new AlarmManager.AlarmClockInfo(when, show),
                broadcast(context, action, requestCode));
    }

    public static long scheduleDaily(Context context) {
        SharedPreferences p = prefs(context);
        if (!p.getBoolean(ENABLED, false)) {
            cancelDaily(context);
            return 0L;
        }
        if (!canSchedule(context)) {
            p.edit().putLong(NEXT, 0L).apply();
            return 0L;
        }
        long when = NextAlarmMath.nextWeekly(System.currentTimeMillis(),
                TimeZone.getDefault(), p.getInt(HOUR, 7), p.getInt(MINUTE, 0),
                p.getInt(DAYS, DEFAULT_DAYS));
        setAlarm(context, when, AlarmReceiver.DAILY, DAILY_REQUEST);
        p.edit().putLong(NEXT, when).apply();
        return when;
    }

    public static void cancelDaily(Context context) {
        cancel(context, AlarmReceiver.DAILY, DAILY_REQUEST);
        prefs(context).edit().putLong(NEXT, 0L).apply();
    }

    public static long schedulePreview(Context context) {
        long when = System.currentTimeMillis() + 30_000L;
        setAlarm(context, when, AlarmReceiver.PREVIEW, PREVIEW_REQUEST);
        prefs(context).edit().putLong(PREVIEW_NEXT, when).apply();
        return when;
    }

    public static void cancelPreview(Context context) {
        cancel(context, AlarmReceiver.PREVIEW, PREVIEW_REQUEST);
        prefs(context).edit().putLong(PREVIEW_NEXT, 0L).apply();
    }

    public static void scheduleSnooze(Context context) {
        setAlarm(context, System.currentTimeMillis() + 5 * 60_000L,
                AlarmReceiver.SNOOZE, SNOOZE_REQUEST);
    }

    public static void cancelSnooze(Context context) {
        cancel(context, AlarmReceiver.SNOOZE, SNOOZE_REQUEST);
    }

    private static void cancel(Context context, String action, int requestCode) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) manager.cancel(broadcast(context, action, requestCode));
    }
}
