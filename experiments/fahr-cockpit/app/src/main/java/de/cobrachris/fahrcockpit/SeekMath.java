package de.cobrachris.fahrcockpit;

/** Pure range-safe seek math, testable without Android APIs. */
public final class SeekMath {
    private SeekMath() {}

    public static long target(long positionMs, long deltaMs, long durationMs) {
        if (positionMs < 0) return -1L;
        long target;
        if (deltaMs > 0 && positionMs > Long.MAX_VALUE - deltaMs) {
            target = Long.MAX_VALUE;
        } else if (deltaMs < 0 && positionMs < Long.MIN_VALUE - deltaMs) {
            target = Long.MIN_VALUE;
        } else {
            target = positionMs + deltaMs;
        }
        target = Math.max(0L, target);
        return durationMs > 0L ? Math.min(target, durationMs) : target;
    }
}
