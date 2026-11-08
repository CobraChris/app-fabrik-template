package com.cobrachris.sanftwecker;

/** The optional fallback is applied only if the Android alarm stream is fully muted. */
public final class AlarmVolumePolicy {
    private AlarmVolumePolicy(){}

    public static int temporaryLevel(int max) {
        if(max <= 0)throw new IllegalArgumentException("No alarm volume range");
        return Math.max(1,Math.min(max,(int)Math.ceil(max*0.75)));
    }

    public static boolean shouldRestore(int original,int current,int boosted) {
        // Never undo a user change made during ringing.
        return original==0 && boosted>0 && current==boosted;
    }
}
