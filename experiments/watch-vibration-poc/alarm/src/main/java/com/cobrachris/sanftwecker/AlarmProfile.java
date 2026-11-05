package com.cobrachris.sanftwecker;

/** Validated, independent configuration for one of three alarm slots. */
public final class AlarmProfile {
    public static final int COUNT = 3;
    public final int id;
    public final String name;
    public final boolean enabled;
    public final int hour, minute, days, delaySeconds, rampSeconds, maxVolume;
    public final int watchPulses, watchIntervalSeconds, snoozeMinutes, tone;

    public AlarmProfile(int id, String name, boolean enabled, int hour, int minute,
            int days, int delaySeconds, int rampSeconds, int maxVolume,
            int watchPulses, int watchIntervalSeconds, int snoozeMinutes, int tone) {
        if (id < 1 || id > COUNT || hour < 0 || hour > 23 || minute < 0 || minute > 59
                || (days & 0x7f) == 0 || (days & ~0x7f) != 0
                || delaySeconds < 0 || delaySeconds > 300
                || rampSeconds < 60 || rampSeconds > 1800
                || maxVolume < 1 || maxVolume > 100
                || watchPulses < 0 || watchPulses > 12
                || watchIntervalSeconds < 10 || watchIntervalSeconds > 120
                || snoozeMinutes < 1 || snoozeMinutes > 30
                || tone < 0 || tone > 2) {
            throw new IllegalArgumentException("Invalid alarm settings");
        }
        this.id = id;
        this.name = name == null || name.trim().isEmpty() ? "Wecker " + id
                : name.trim().substring(0, Math.min(36, name.trim().length()));
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
    }

    public AlarmProfile withEnabled(boolean state) {
        return new AlarmProfile(id, name, state, hour, minute, days,
                delaySeconds, rampSeconds, maxVolume, watchPulses, watchIntervalSeconds,
                snoozeMinutes, tone);
    }

    public static AlarmProfile defaults(int id) {
        int days = id == 1 ? 0x1f : id == 2 ? 0x60 : 0x7f;
        String name = id == 1 ? "Werktage" : id == 2 ? "Wochenende" : "Zusatzwecker";
        return new AlarmProfile(id, name, false, id == 2 ? 9 : 7, 0,
                days, 0, 600, 35, 6, 20, 5, 0);
    }
}
