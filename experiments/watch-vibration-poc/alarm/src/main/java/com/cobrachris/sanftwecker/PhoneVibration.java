package com.cobrachris.sanftwecker;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

/** Independent direct handset haptics, unlike mirrored watch notifications. */
public final class PhoneVibration {
    private static final Handler HANDLER=new Handler(Looper.getMainLooper());
    private static boolean alarmRunning;
    private static Vibrator activeVibrator;
    private static int generation;

    private PhoneVibration() {}

    private static Vibrator find(Context context) {
        if(Build.VERSION.SDK_INT>=31){
            VibratorManager manager=(VibratorManager)context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if(manager!=null) return manager.getDefaultVibrator();
        }
        return (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private static AudioAttributes alarmAttributes() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
    }

    /** Mode 0 is intentionally legacy: existing alarms stay unchanged. */
    public static synchronized void start(Context context,int mode) {
        if(mode==0)return;
        stop();
        Vibrator v=find(context);
        if(v==null||!v.hasVibrator()) {
            AlarmRepository.diagnostic(context,"Handy-Vibration nicht verfügbar");
            return;
        }
        try {
            alarmRunning=true;
            activeVibrator=v;
            int token=++generation;
            v.vibrate(VibrationEffect.createWaveform(VibrationPresets.pattern(mode),0),alarmAttributes());
            // Bounded continuous haptics: 4 s on, 0.5 s off, max 60 seconds.
            // Other patterns stop with the service, max 120 seconds.
            int maxDuration=mode==4?60_000:120_000;
            HANDLER.postDelayed(()->{
                synchronized(PhoneVibration.class) {
                    if(alarmRunning && token==generation)stop();
                }
            },maxDuration);
            AlarmRepository.diagnostic(context,"Handy-Vibration: "+
                    VibrationPresets.LABELS[mode]);
        }catch(RuntimeException e){
            alarmRunning=false;
            activeVibrator=null;
            AlarmRepository.diagnostic(context,"Handy-Vibration vom System blockiert");
        }
    }

    public static synchronized boolean preview(Context context,int mode) {
        if(alarmRunning)return false;
        Vibrator v=find(context);
        if(v==null||!v.hasVibrator())return false;
        try {
            v.vibrate(VibrationEffect.createWaveform(VibrationPresets.pattern(mode),-1),alarmAttributes());
            return true;
        }catch(RuntimeException error) {return false;}
    }

    public static synchronized void stop() {
        generation++;
        alarmRunning=false;
        if(activeVibrator!=null){
            try{activeVibrator.cancel();}catch(RuntimeException ignored){}
            activeVibrator=null;
        }
    }
}
