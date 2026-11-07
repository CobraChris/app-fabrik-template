package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Per-timer notification preferences. Independent of countdown serialization:
 * modifying alert preferences must never reset a running timer's deadline.
 */
public final class TimerAlertSettings {
    private static final String PREF="sanftwecker_timer_alert_options_v1";
    private TimerAlertSettings(){}

    private static SharedPreferences prefs(Context c){
        return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
    }
    private static String key(String option,int id){
        if(id<1||id>9999)throw new IllegalArgumentException("Invalid timer ID");
        return option+"_"+id;
    }
    public static boolean tone(Context c,int id){
        return prefs(c).getBoolean(key("tone",id),true);
    }
    public static boolean vibration(Context c,int id){
        return prefs(c).getBoolean(key("vibration",id),true);
    }
    public static void setTone(Context c,int id,boolean enabled){
        if(!prefs(c).edit().putBoolean(key("tone",id),enabled).commit())
            throw new IllegalStateException("Alarmton-Einstellung konnte nicht gespeichert werden");
    }
    public static void setVibration(Context c,int id,boolean enabled){
        if(!prefs(c).edit().putBoolean(key("vibration",id),enabled).commit())
            throw new IllegalStateException("Vibration-Einstellung konnte nicht gespeichert werden");
    }
    public static void delete(Context c,int id){
        prefs(c).edit().remove(key("tone",id)).remove(key("vibration",id)).commit();
    }
    /** Unit-testable channel selector. The existing both-on channel is retained. */
    public static String channelId(boolean tone,boolean vibrate){
        if(tone&&vibrate)return "sanftwecker_timer_done_v1";
        if(tone)return "sanftwecker_timer_tone_only_v1";
        if(vibrate)return "sanftwecker_timer_vibration_only_v1";
        return "sanftwecker_timer_silent_v1";
    }
}
