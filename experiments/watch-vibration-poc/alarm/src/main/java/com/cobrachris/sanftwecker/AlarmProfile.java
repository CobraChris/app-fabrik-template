package com.cobrachris.sanftwecker;

/** Immutable user configuration for a stable alarm ID. */
public final class AlarmProfile {
    // Historical initial slots, retained for compatibility with existing unit tests.
    public static final int COUNT = 3;
    public static final int MAX_ALARMS = 1000;
    public final int id, hour, minute, days, delaySeconds, rampSeconds, maxVolume;
    public final int watchPulses, watchIntervalSeconds, snoozeMinutes, tone, autoStopMinutes;
    public final String name;
    public final boolean enabled, rampEnabled, snoozeEnabled;
    /** Stored original occurrence to omit once; epoch milliseconds. */
    public final long skipAt;
    /** Replacement occurrence and original occurrence for a one-time change. */
    public final long oneTimeAt, replacedAt;

    public AlarmProfile(int id, String name, boolean enabled, int hour, int minute,
            int days, int delaySeconds, int rampSeconds, int maxVolume,
            int watchPulses, int watchIntervalSeconds, int snoozeMinutes, int tone) {
        this(id, name, enabled, hour, minute, days, delaySeconds, rampSeconds,
                maxVolume, watchPulses, watchIntervalSeconds, snoozeMinutes, tone,
                true, 20, 0L, 0L, 0L);
    }

    public AlarmProfile(int id, String name, boolean enabled, int hour, int minute,
            int days, int delaySeconds, int rampSeconds, int maxVolume,
            int watchPulses, int watchIntervalSeconds, int snoozeMinutes, int tone,
            boolean rampEnabled, int autoStopMinutes, long skipAt, long oneTimeAt,
            long replacedAt) {
        this(id, name, enabled, hour, minute, days, delaySeconds, rampSeconds,
                maxVolume, watchPulses, watchIntervalSeconds, snoozeMinutes, tone,
                rampEnabled, autoStopMinutes, skipAt, oneTimeAt, replacedAt, true);
    }

    public AlarmProfile(int id, String name, boolean enabled, int hour, int minute,
            int days, int delaySeconds, int rampSeconds, int maxVolume,
            int watchPulses, int watchIntervalSeconds, int snoozeMinutes, int tone,
            boolean rampEnabled, int autoStopMinutes, long skipAt, long oneTimeAt,
            long replacedAt, boolean snoozeEnabled) {
        if (id < 1 || id > MAX_ALARMS || hour < 0 || hour > 23 || minute < 0 || minute > 59
                || (days & 0x7f) == 0 || (days & ~0x7f) != 0
                || delaySeconds < 0 || delaySeconds > 300 || rampSeconds < 60
                || rampSeconds > 1800 || maxVolume < 1 || maxVolume > 100
                || watchPulses < 0 || watchPulses > 12 || watchIntervalSeconds < 10
                || watchIntervalSeconds > 120 || snoozeMinutes < 1 || snoozeMinutes > 30
                || tone < 0 || tone > 2 || autoStopMinutes < 1
                || autoStopMinutes > 30 || skipAt < 0 || oneTimeAt < 0 || replacedAt < 0) {
            throw new IllegalArgumentException("Invalid alarm configuration");
        }
        this.id = id;
        String trimmed = name == null ? "" : name.trim();
        this.name = trimmed.isEmpty() ? "Wecker " + id :
                trimmed.substring(0, Math.min(36, trimmed.length()));
        this.enabled = enabled;
        this.hour = hour;
        this.minute = minute;
        this.days = days;
        this.delaySeconds = delaySeconds;
        this.rampSeconds = rampSeconds;
        this.maxVolume = maxVolume;
        this.watchPulses = watchPulses;
        this.watchIntervalSeconds = watchIntervalSeconds;
        this.snoozeMinutes = snoozeMinutes;
        this.tone = tone;
        this.rampEnabled = rampEnabled;
        this.snoozeEnabled = snoozeEnabled;
        this.autoStopMinutes = autoStopMinutes;
        this.skipAt = skipAt;
        this.oneTimeAt = oneTimeAt;
        this.replacedAt = replacedAt;
    }

    public AlarmProfile withEnabled(boolean enabled) {
        return new AlarmProfile(id, name, enabled, hour, minute, days,
                delaySeconds, rampSeconds, maxVolume, watchPulses, watchIntervalSeconds,
                snoozeMinutes, tone, rampEnabled, autoStopMinutes, skipAt, oneTimeAt, replacedAt, snoozeEnabled);
    }

    public AlarmProfile withExceptions(long skip, long once, long replaced) {
        return new AlarmProfile(id, name, enabled, hour, minute, days,
                delaySeconds, rampSeconds, maxVolume, watchPulses, watchIntervalSeconds,
                snoozeMinutes, tone, rampEnabled, autoStopMinutes, skip, once, replaced, snoozeEnabled);
    }

    public AlarmProfile duplicateAs(int id) {
        return new AlarmProfile(id, name + " Kopie", false, hour, minute, days,
                delaySeconds, rampSeconds, maxVolume, watchPulses, watchIntervalSeconds,
                snoozeMinutes, tone, rampEnabled, autoStopMinutes, 0, 0, 0, snoozeEnabled);
    }

    public boolean sameConfiguration(AlarmProfile b) {
        return b != null && id == b.id && name.equals(b.name) &&
                enabled == b.enabled && hour == b.hour && minute == b.minute &&
                days == b.days && delaySeconds == b.delaySeconds &&
                rampSeconds == b.rampSeconds && maxVolume == b.maxVolume &&
                watchPulses == b.watchPulses &&
                watchIntervalSeconds == b.watchIntervalSeconds &&
                snoozeMinutes == b.snoozeMinutes && tone == b.tone &&
                rampEnabled == b.rampEnabled && autoStopMinutes == b.autoStopMinutes &&
                snoozeEnabled == b.snoozeEnabled && skipAt == b.skipAt &&
                oneTimeAt == b.oneTimeAt && replacedAt == b.replacedAt;
    }

    public static AlarmProfile defaults(int id) {
        int mask = id == 1 ? 0x1f : id == 2 ? 0x60 : 0x7f;
        String title = id == 1 ? "Werktage" : id == 2 ? "Wochenende" : "Wecker " + id;
        return new AlarmProfile(id, title, false, id == 2 ? 9 : 7, 0,
                mask, 0, 600, 35, 6, 20, 5, 0);
    }
}
