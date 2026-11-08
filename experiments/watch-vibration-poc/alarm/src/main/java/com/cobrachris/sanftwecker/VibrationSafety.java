package com.cobrachris.sanftwecker;

/** Pure haptics safety limits, independent of Android runtime. */
public final class VibrationSafety {
    public static final int MAX_PREVIEW_MS = 5_000;
    public static final int MAX_ALARM_MS = 90_000;
    public static final int FULL_AMPLITUDE = 255;
    public static final int NON_REPEATING = -1;
    private VibrationSafety() {}
    public static long previewDuration(int mode) {
        return Math.min(MAX_PREVIEW_MS, VibrationPresets.duration(mode));
    }
    /** Explicit highest REQUESTED amplitude; actual strength is hardware-limited. */
    public static int[] fullStrengthAmplitudes(long[] timings) {
        if(timings==null || timings.length<3 || timings[0]!=0 || timings.length%2!=1)
            throw new IllegalArgumentException("Invalid on/off pattern");
        int[] amplitudes=new int[timings.length];
        for(int i=0;i<timings.length;i++) {
            if(timings[i]<0 || timings[i]>4_000)
                throw new IllegalArgumentException("Vibration phase exceeds 4 seconds");
            amplitudes[i]=(i%2==1) ? FULL_AMPLITUDE : 0;
        }
        return amplitudes;
    }
    public static boolean isFinite(long duration) {
        return duration > 0 && duration <= MAX_PREVIEW_MS;
    }
}
