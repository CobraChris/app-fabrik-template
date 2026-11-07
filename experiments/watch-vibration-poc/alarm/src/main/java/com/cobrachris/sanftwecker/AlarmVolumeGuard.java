package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;

/**
 * Explicitly opted-in mute fallback; only manipulates STREAM_ALARM, never the
 * ringer mode or Do Not Disturb. Stores restoration metadata before changing
 * volume, then restores on stop/snooze, service destruction, app reopen, reboot.
 * If the user changed volume during the alarm, that new choice is preserved.
 */
public final class AlarmVolumeGuard {
    private static final String PREF="sanftwecker_temporary_alarm_volume_v1";
    private static final String PENDING="pending";
    private static final String ORIGINAL="original";
    private static final String BOOSTED="boosted";
    private static boolean active;
    private AlarmVolumeGuard(){}

    private static SharedPreferences prefs(Context c){
        return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
    }
    private static AudioManager audio(Context c){
        return (AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
    }

    public static synchronized boolean ensureAudible(Context c) {
        if(active)return true; // the current ringing alarm already requested it
        recoverIfIdle(c);
        AudioManager a=audio(c);
        if(a==null || a.isVolumeFixed())return false;
        try {
            int old=a.getStreamVolume(AudioManager.STREAM_ALARM);
            if(old!=0)return false;
            int target=AlarmVolumePolicy.temporaryLevel(
                    a.getStreamMaxVolume(AudioManager.STREAM_ALARM));
            if(!prefs(c).edit().putBoolean(PENDING,true)
                    .putInt(ORIGINAL,old).putInt(BOOSTED,target).commit())
                return false;
            a.setStreamVolume(AudioManager.STREAM_ALARM,target,0);
            if(a.getStreamVolume(AudioManager.STREAM_ALARM)!=target){
                restore(c);
                return false;
            }
            active=true;
            return true;
        }catch(RuntimeException ex){
            AlarmRepository.diagnostic(c,
                    "Android hat temporäre Änderung der Weckerlautstärke abgelehnt");
            restore(c);
            return false;
        }
    }

    public static synchronized void recoverIfIdle(Context c){
        if(!active)restore(c);
    }

    public static synchronized void restore(Context c) {
        SharedPreferences p=prefs(c);
        if(!p.getBoolean(PENDING,false)) {active=false;return;}
        AudioManager a=audio(c);
        if(a==null) {active=false;return;}
        int original=p.getInt(ORIGINAL,0);
        int boosted=p.getInt(BOOSTED,-1);
        try {
            int current=a.getStreamVolume(AudioManager.STREAM_ALARM);
            if(AlarmVolumePolicy.shouldRestore(original,current,boosted)) {
                a.setStreamVolume(AudioManager.STREAM_ALARM,original,0);
                if(a.getStreamVolume(AudioManager.STREAM_ALARM)!=original){
                    active=false;
                    return; // Keep marker for another restoration attempt.
                }
            }
            p.edit().clear().commit();
            active=false;
        }catch(RuntimeException ex){
            AlarmRepository.diagnostic(c,
                    "Weckerlautstärke nicht automatisch wiederhergestellt – bitte prüfen");
            active=false;
        }
    }
}
