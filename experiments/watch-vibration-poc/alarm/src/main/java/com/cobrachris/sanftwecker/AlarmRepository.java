package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Persistent and migration-safe dynamic alarm collection, without account or Internet. */
public final class AlarmRepository {
    private static final String PREF = "sanftwecker_settings_v3";
    private static final String OLD = "sanftwecker_settings_v2";
    private static final String LEGACY = "sanftwecker_settings_v1";
    private static final String LOG = "sanftwecker_diagnostics_v1";

    private AlarmRepository() {}

    private static SharedPreferences storage(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }
    private static String k(int id, String key) { return "alarm_" + id + "_" + key; }
    private static String old(int id, String key) { return "slot_" + id + "_" + key; }

    private static List<Integer> ids(SharedPreferences p) {
        List<Integer> found = new ArrayList<>();
        String value = p.getString("ids", "");
        if (value == null || value.isEmpty()) return found;
        for (String part : value.split(",")) {
            try {
                int id = Integer.parseInt(part);
                if (id > 0 && id <= AlarmProfile.MAX_ALARMS && !found.contains(id)) found.add(id);
            } catch (NumberFormatException ignored) { }
        }
        Collections.sort(found);
        return found;
    }
    private static String csv(List<Integer> ids) {
        StringBuilder b = new StringBuilder();
        for (int id : ids) {
            if (b.length() > 0) b.append(',');
            b.append(id);
        }
        return b.toString();
    }

    private static void migrate(Context c) {
        SharedPreferences p = storage(c);
        if (p.getBoolean("migrated", false)) return;
        SharedPreferences previous = c.getSharedPreferences(OLD, Context.MODE_PRIVATE);
        SharedPreferences oldest = c.getSharedPreferences(LEGACY, Context.MODE_PRIVATE);
        List<AlarmProfile> existing = new ArrayList<>();
        for (int id = 1; id <= AlarmProfile.COUNT; id++) {
            if (!previous.contains(old(id, "hour"))) continue;
            try {
                AlarmProfile d = AlarmProfile.defaults(id);
                existing.add(new AlarmProfile(id,
                        previous.getString(old(id, "name"), d.name),
                        previous.getBoolean(old(id, "enabled"), false),
                        previous.getInt(old(id, "hour"), d.hour),
                        previous.getInt(old(id, "minute"), d.minute),
                        previous.getInt(old(id, "days"), d.days),
                        previous.getInt(old(id, "delay"), d.delaySeconds),
                        previous.getInt(old(id, "ramp"), d.rampSeconds),
                        previous.getInt(old(id, "max"), d.maxVolume),
                        previous.getInt(old(id, "pulses"), d.watchPulses),
                        previous.getInt(old(id, "interval"), d.watchIntervalSeconds),
                        previous.getInt(old(id, "snooze"), d.snoozeMinutes),
                        previous.getInt(old(id, "tone"), d.tone)));
            } catch (RuntimeException ignored) { }
        }
        if (existing.isEmpty() && oldest.contains("hour")) {
            try {
                existing.add(new AlarmProfile(1, "Werktage",
                        oldest.getBoolean("enabled", false),
                        oldest.getInt("hour", 7), oldest.getInt("minute", 0),
                        oldest.getInt("days_mask", 0x1f), 0,
                        oldest.getInt("ramp_seconds", 600),
                        oldest.getInt("max_volume", 35), 6, 20, 5, 0));
            } catch (RuntimeException ignored) { }
        }
        SharedPreferences.Editor edit = p.edit();
        List<Integer> imported = new ArrayList<>();
        for (AlarmProfile a : existing) {
            write(edit, a);
            imported.add(a.id);
        }
        edit.putString("ids", csv(imported));
        edit.putInt("next_id", imported.isEmpty() ? 1 : Collections.max(imported) + 1);
        edit.putBoolean("migrated", true);
        edit.commit();
    }

    private static void write(SharedPreferences.Editor e, AlarmProfile a) {
        e.putString(k(a.id, "name"), a.name)
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
                .putInt(k(a.id, "tone"), a.tone)
                .putBoolean(k(a.id, "ramp_enabled"), a.rampEnabled)
                .putInt(k(a.id, "autostop"), a.autoStopMinutes)
                .putLong(k(a.id, "skip"), a.skipAt)
                .putLong(k(a.id, "once"), a.oneTimeAt)
                .putLong(k(a.id, "replaced"), a.replacedAt)
                .putBoolean(k(a.id, "snooze_enabled"), a.snoozeEnabled)
                .putInt(k(a.id, "vibration_mode"), a.vibrationMode);
    }

    public static synchronized List<Integer> ids(Context c) {
        migrate(c);
        return ids(storage(c));
    }

    public static synchronized List<AlarmProfile> all(Context c) {
        migrate(c);
        List<AlarmProfile> profiles = new ArrayList<>();
        for (int id : ids(storage(c))) {
            AlarmProfile p = get(c, id);
            if (p != null) profiles.add(p);
        }
        return profiles;
    }

    public static synchronized AlarmProfile get(Context c, int id) {
        migrate(c);
        SharedPreferences p = storage(c);
        if (!ids(p).contains(id)) return null;
        AlarmProfile d = AlarmProfile.defaults(id);
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
                    p.getInt(k(id, "tone"), d.tone),
                    p.getBoolean(k(id, "ramp_enabled"), true),
                    p.getInt(k(id, "autostop"), 20),
                    p.getLong(k(id, "skip"), 0L),
                    p.getLong(k(id, "once"), 0L),
                    p.getLong(k(id, "replaced"), 0L),
                    p.getBoolean(k(id, "snooze_enabled"), true),
                    p.getInt(k(id, "vibration_mode"), 0));
        } catch (RuntimeException invalid) {
            diagnostic(c, "WARNUNG: ungültiger Eintrag " + id + ", deaktiviert");
            return d.withEnabled(false);
        }
    }

    public static synchronized int create(Context c, AlarmProfile source) {
        migrate(c);
        SharedPreferences p = storage(c);
        List<Integer> list = ids(p);
        int id = Math.max(p.getInt("next_id", 1), list.isEmpty() ? 1 : list.get(list.size()-1)+1);
        if (id > AlarmProfile.MAX_ALARMS) throw new IllegalStateException("Maximum reached");
        AlarmProfile fresh = source == null ? AlarmProfile.defaults(id)
                : source.duplicateAs(id);
        SharedPreferences.Editor e = p.edit();
        list.add(id);
        Collections.sort(list);
        write(e, fresh);
        if (!e.putString("ids", csv(list)).putInt("next_id", id+1).commit())
            throw new IllegalStateException("Alarm could not be stored");
        return id;
    }

    public static synchronized void put(Context c, AlarmProfile a) {
        migrate(c);
        SharedPreferences p = storage(c);
        if (!ids(p).contains(a.id)) throw new IllegalArgumentException("Unknown alarm");
        if (!writeAndCommit(p, a)) throw new IllegalStateException("Could not save alarm");
    }

    private static boolean writeAndCommit(SharedPreferences p, AlarmProfile a) {
        SharedPreferences.Editor e = p.edit();
        write(e, a);
        return e.commit();
    }

    public static synchronized void delete(Context c, int id) {
        migrate(c);
        SharedPreferences p = storage(c);
        List<Integer> found = ids(p);
        if (!found.remove(Integer.valueOf(id))) return;
        SharedPreferences.Editor e = p.edit().putString("ids", csv(found));
        String[] keys = {"name","enabled","hour","minute","days","delay","ramp","max",
                "pulses","interval","snooze","tone","ramp_enabled","autostop",
                "skip","once","replaced","next","snooze_enabled","vibration_mode"};
        for (String key : keys) e.remove(k(id,key));
        if (!e.commit()) throw new IllegalStateException("Could not delete alarm");
    }

    public static void next(Context c, int id, long when) {
        storage(c).edit().putLong(k(id, "next"), when).apply();
    }
    public static long next(Context c, int id) {
        return storage(c).getLong(k(id, "next"), 0);
    }

    public static synchronized void diagnostic(Context c, String event) {
        SharedPreferences p = c.getSharedPreferences(LOG, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        String stamp = new SimpleDateFormat("dd.MM. HH:mm:ss", Locale.GERMANY)
                .format(new Date(now));
        String previous = p.getString("history", "");
        String next = stamp + "  " + event + "\n" + (previous == null ? "" : previous);
        p.edit().putString("last_event", event)
                .putLong("last_event_time", now)
                .putString("history", next.substring(0, Math.min(next.length(), 2400)))
                .apply();
    }
    public static String history(Context c) {
        return c.getSharedPreferences(LOG, Context.MODE_PRIVATE)
                .getString("history", "Noch keine Alarm-Ereignisse");
    }
    public static String lastEvent(Context c) {
        SharedPreferences p = c.getSharedPreferences(LOG, Context.MODE_PRIVATE);
        long when = p.getLong("last_event_time", 0L);
        return when == 0 ? "Noch kein Alarm ausgelöst" :
                new SimpleDateFormat("dd.MM. HH:mm:ss", Locale.GERMANY)
                        .format(new Date(when)) + " – " + p.getString("last_event", "");
    }
}
