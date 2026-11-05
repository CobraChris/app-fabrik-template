package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.SharedPreferences;

/** Preferences are local only, no network, account, analytics or backups. */
public final class AlarmRepository {
    private static final String PREF = "sanftwecker_settings_v2";
    private static final String LEGACY = "sanftwecker_settings_v1";
    private static final String LOG = "sanftwecker_diagnostics_v1";

    private AlarmRepository() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private static String k(int id, String key) { return "slot_" + id + "_" + key; }

    public static AlarmProfile get(Context c, int id) {
        AlarmProfile d = AlarmProfile.defaults(id);
        SharedPreferences p = prefs(c);
        if (!p.contains(k(id, "hour")) && id == 1) {
            SharedPreferences old = c.getSharedPreferences(LEGACY, Context.MODE_PRIVATE);
            if (old.contains("hour")) {
                try {
                    return new AlarmProfile(1, "Werktage",
                            old.getBoolean("enabled", false),
                            old.getInt("hour", 7), old.getInt("minute", 0),
                            old.getInt("days_mask", 0x1f), 0,
                            old.getInt("ramp_seconds", 600),
                            old.getInt("max_volume", 35), 6, 20, 5, 0);
                } catch (IllegalArgumentException ignored) { }
            }
        }
        try {
            return new AlarmProfile(id, p.getString(k(id, "name"), d.name),
                    p.getBoolean(k(id, "enabled"), false),
                    p.getInt(k(id, "hour"), d.hour),
                    p.getInt(k(id, "minute"), d.minute),
                    p.getInt(k(id, "days"), d.days),
                    p.getInt(k(id, "delay"), d.delaySeconds),
                    p.getInt(k(id, "ramp"), d.rampSeconds),
                    p.getInt(k(id, "max"), d.maxVolume),
                    p.getInt(k(id, "pulses"), d.watchPulses),
                    p.getInt(k(id, "interval"), d.watchIntervalSeconds),
                    p.getInt(k(id, "snooze"), d.snoozeMinutes),
                    p.getInt(k(id, "tone"), d.tone));
        } catch (IllegalArgumentException | ClassCastException error) {
            return d.withEnabled(false);
        }
    }

    public static void put(Context c, AlarmProfile a) {
        prefs(c).edit()
                .putString(k(a.id, "name"), a.name)
                .putBoolean(k(a.id, "enabled"), a.enabled)
                .putInt(k(a.id, "hour"), a.hour)
                .putInt(k(a.id, "minute"), a.minute)
                .putInt(k(a.id, "days"), a.days)
                .putInt(k(a.id, "delay"), a.delaySeconds)
                .putInt(k(a.id, "ramp"), a.rampSeconds)
                .putInt(k(a.id, "max"), a.maxVolume)
                .putInt(k(a.id, "pulses"), a.watchPulses)
                .putInt(k(a.id, "interval"), a.watchIntervalSeconds)
                .putInt(k(a.id, "snooze"), a.snoozeMinutes)
                .putInt(k(a.id, "tone"), a.tone).commit();
    }

    public static void next(Context c, int id, long when) {
        prefs(c).edit().putLong(k(id, "next"), when).apply();
    }
    public static long next(Context c, int id) {
        return prefs(c).getLong(k(id, "next"), 0L);
    }

    public static void diagnostic(Context c, String event) {
        c.getSharedPreferences(LOG, Context.MODE_PRIVATE).edit()
                .putString("last_event", event)
                .putLong("last_event_time", System.currentTimeMillis()).apply();
    }

    public static String lastEvent(Context c) {
        SharedPreferences p = c.getSharedPreferences(LOG, Context.MODE_PRIVATE);
        long when = p.getLong("last_event_time", 0L);
        return when == 0 ? "Noch kein Alarm ausgelöst" :
                new java.text.SimpleDateFormat("dd.MM. HH:mm:ss",
                        java.util.Locale.GERMANY).format(new java.util.Date(when))
                        + " – " + p.getString("last_event", "");
    }
}
