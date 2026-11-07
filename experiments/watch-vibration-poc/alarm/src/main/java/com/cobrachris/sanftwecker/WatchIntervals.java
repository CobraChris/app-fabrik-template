package com.cobrachris.sanftwecker;

/** Exact persisted watch-notification intervals, with backwards-compatible UI mapping. */
public final class WatchIntervals {
    // Existing 10..120 second values retain identical meaning across updates.
    private static final int[] SECONDS = {
        3, 5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120
    };

    private WatchIntervals() {}

    public static int maxProgress() { return SECONDS.length - 1; }

    public static int secondsAt(int progress) {
        if(progress < 0 || progress >= SECONDS.length)
            throw new IllegalArgumentException("Invalid watch interval selection");
        return SECONDS[progress];
    }

    public static int progressFor(int seconds) {
        for(int i=0;i<SECONDS.length;i++)
            if(SECONDS[i] == seconds) return i;
        throw new IllegalArgumentException("Unsupported saved watch interval: "+seconds);
    }

    public static boolean isSupported(int seconds) {
        for(int value:SECONDS) if(value == seconds)return true;
        return false;
    }
}
