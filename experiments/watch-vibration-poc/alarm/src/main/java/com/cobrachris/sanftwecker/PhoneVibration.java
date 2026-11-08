package com.cobrachris.sanftwecker;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

/**
 * Strong, finite handset haptics, capped at 90 seconds while an alarm rings.
 * No infinite hardware waveform: one motor phase <= 4s; emergency stop always cancels.
 */
public final class PhoneVibration {
    public static final int MAX_ALARM_MS = VibrationSafety.MAX_ALARM_MS;
    public static final int MAX_PREVIEW_MS = VibrationSafety.MAX_PREVIEW_MS;
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static Vibrator activeVibrator;
    private static boolean alarmRunning;
    private static int generation;

    private PhoneVibration() {}

    private static Vibrator find(Context context) {
        if (Build.VERSION.SDK_INT >= 31) {
            VibratorManager m = (VibratorManager)
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (m != null) return m.getDefaultVibrator();
        }
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private static AudioAttributes attributes() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
    }

    /** Play one finite rhythm. NEVER use an infinite repeat index. */
    private static void once(Vibrator v, int mode) {
        long[] timings=VibrationPresets.pattern(mode);
        VibrationEffect effect=v.hasAmplitudeControl()
                ? VibrationEffect.createWaveform(timings,
                        VibrationSafety.fullStrengthAmplitudes(timings),
                        VibrationSafety.NON_REPEATING)
                : VibrationEffect.createWaveform(timings,
                        VibrationSafety.NON_REPEATING);
        v.vibrate(effect, attributes());
    }

    public static synchronized void start(Context context, int mode) {
        stop();
        if (mode == 0) return; // Old profiles remain notification-only.
        Vibrator v=find(context);
        if (v == null || !v.hasVibrator()) {
            AlarmRepository.diagnostic(context, "Handyvibration nicht verfügbar");
            return;
        }
        activeVibrator=v;
        alarmRunning=true;
        final int token=generation;
        final long deadline=SystemClock.elapsedRealtime()+MAX_ALARM_MS;
        final Runnable[] cycle=new Runnable[1];
        cycle[0]=()->{
            synchronized(PhoneVibration.class) {
                if (!alarmRunning || token != generation) return;
                long remaining=deadline-SystemClock.elapsedRealtime();
                if (remaining <= 0) { stop(); return; }
                try {
                    once(v,mode);
                } catch (RuntimeException ex) {
                    AlarmRepository.diagnostic(context,"Handyvibration durch Android blockiert");
                    stop();
                    return;
                }
                long oneCycle=VibrationPresets.duration(mode);
                if(oneCycle<remaining)HANDLER.postDelayed(cycle[0],oneCycle);
            }
        };
        HANDLER.post(cycle[0]);
        HANDLER.postDelayed(()->{
            synchronized(PhoneVibration.class) {
                if(token==generation)stop();
            }
        },MAX_ALARM_MS);
        AlarmRepository.diagnostic(context,
                "Handyvibration gestartet – automatische Abschaltung nach 90 s");
    }

    /** One preview only; the activity shows the stop dialog BEFORE invoking this. */
    public static synchronized boolean preview(Context context,int mode) {
        if(alarmRunning)return false;
        stop();
        Vibrator v=find(context);
        if(v==null || !v.hasVibrator())return false;
        try {
            activeVibrator=v;
            int token=generation;
            once(v,mode);
            HANDLER.postDelayed(()->{
                synchronized(PhoneVibration.class) {
                    if(token==generation)stop();
                }
            },VibrationSafety.previewDuration(mode));
            return true;
        }catch(RuntimeException ex){
            stop();
            return false;
        }
    }

    /** Emergency stop always works independently of a foreground alarm service. */
    public static synchronized void stop() {
        generation++;
        alarmRunning=false;
        HANDLER.removeCallbacksAndMessages(null);
        if(activeVibrator!=null){
            try{activeVibrator.cancel();}catch(RuntimeException ignored){}
            activeVibrator=null;
        }
    }
}
