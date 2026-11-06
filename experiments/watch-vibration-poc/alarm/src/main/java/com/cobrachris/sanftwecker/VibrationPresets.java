package com.cobrachris.sanftwecker;

/** Portable vibration rhythm definitions, expressed as off/on durations in milliseconds. */
public final class VibrationPresets {
    public static final String[] LABELS = {
            "Standard – bisherige kurze Impulse",
            "Lang – kurz – lang",
            "Kurz – kurz – kurz",
            "Lang – lang",
            "Fast durchgehend (4 s / 0,5 s Pause)"
    };
    private VibrationPresets() {}

    public static long[] pattern(int mode) {
        switch (mode) {
            case 0: return new long[]{0, 350, 150, 350, 1200};
            case 1: return new long[]{0, 1000, 260, 250, 260, 1000, 1200};
            case 2: return new long[]{0, 300, 230, 300, 230, 300, 1300};
            case 3: return new long[]{0, 1600, 350, 1600, 1400};
            case 4: return new long[]{0, 4000, 500};
            default: throw new IllegalArgumentException("Unknown vibration mode: "+mode);
        }
    }
    public static long duration(int mode) {
        long sum=0;
        for (long duration : pattern(mode)) sum+=duration;
        return sum;
    }
}
