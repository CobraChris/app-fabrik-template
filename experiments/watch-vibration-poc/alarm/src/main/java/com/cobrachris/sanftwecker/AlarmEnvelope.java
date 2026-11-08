package com.cobrachris.sanftwecker;

/** Keep real alarms extremely gentle, but make short diagnostics audible. */
public final class AlarmEnvelope {
    private AlarmEnvelope() {}

    public static float gain(AlarmProfile profile, boolean shortTest, long elapsedMs) {
        if (profile == null) throw new IllegalArgumentException("Profile required");
        if (shortTest) {
            // No user-configured delay: the diagnostic ends before a long delay would elapse.
            return GentleGain.level(elapsedMs, 0, 8, Math.max(45, profile.maxVolume));
        }
        return GentleGain.level(elapsedMs, profile.delaySeconds,
                profile.rampEnabled ? profile.rampSeconds : 30, profile.maxVolume);
    }
}
