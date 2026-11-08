package com.cobrachris.sanftwecker;

/** Pure gain envelope: 0 until delay, then gentle quadratic rise. */
public final class GentleGain {
    private GentleGain() {}

    public static float level(long elapsedMs, int delaySeconds, int rampSeconds,
            int maxPercent) {
        if (elapsedMs <= delaySeconds * 1000L) return 0f;
        if (rampSeconds < 1 || maxPercent < 0 || maxPercent > 100
                || delaySeconds < 0) throw new IllegalArgumentException("Bad envelope");
        double t = (elapsedMs - delaySeconds * 1000L) / (rampSeconds * 1000.0);
        t = Math.max(0, Math.min(1, t));
        return (float) ((maxPercent / 100.0) * t * t);
    }
}
