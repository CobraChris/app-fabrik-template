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
    public static final int FORMAT_HMS=0, FORMAT_MS=1;
    public static final int EXTRA_TARGET=0, EXTRA_DURATION=1, EXTRA_NONE=2;
    public static final long MIN_REPEAT_MS=10_000L;
    public static boolean canRepeat(long durationMs){
        return durationMs>=MIN_REPEAT_MS && durationMs<=TimerEntry.MAX_MS;
    }
    public static int timeFormat(Context c,int id){
        int v=prefs(c).getInt(key("format",id),FORMAT_HMS);
        return v==FORMAT_MS?FORMAT_MS:FORMAT_HMS;
    }
    public static int extraInfo(Context c,int id){
        int v=prefs(c).getInt(key("extra",id),EXTRA_TARGET);
        return v>=EXTRA_TARGET && v<=EXTRA_NONE?v:EXTRA_TARGET;
    }
    public static boolean autoRepeat(Context c,int id){
        return prefs(c).getBoolean(key("repeat",id),false);
    }
    public static int startDelaySeconds(Context c,int id){
        int value=prefs(c).getInt(key("start_delay",id),0);
        return value>=0&&value<=60?value:0;
    }
    public static void setStartDelaySeconds(Context c,int id,int seconds){
        if(seconds!=0&&seconds!=3&&seconds!=5&&seconds!=10&&seconds!=30&&seconds!=60)
            throw new IllegalArgumentException("Ungültige Startverzögerung");
        if(!prefs(c).edit().putInt(key("start_delay",id),seconds).commit())
            throw new IllegalStateException("Startverzögerung konnte nicht gespeichert werden");
    }
    public static void setTimeFormat(Context c,int id,int value){
        if(value!=FORMAT_HMS&&value!=FORMAT_MS)
            throw new IllegalArgumentException("Unbekanntes Zeitformat");
        if(!prefs(c).edit().putInt(key("format",id),value).commit())
            throw new IllegalStateException("Zeitformat konnte nicht gespeichert werden");
    }
    public static void setExtraInfo(Context c,int id,int value){
        if(value<EXTRA_TARGET||value>EXTRA_NONE)
            throw new IllegalArgumentException("Unbekannte Zusatz-Info");
        if(!prefs(c).edit().putInt(key("extra",id),value).commit())
            throw new IllegalStateException("Zusatz-Info konnte nicht gespeichert werden");
    }
    public static void setAutoRepeat(Context c,int id,boolean enabled){
        if(enabled){
            TimerEntry timer=TimerRepository.get(c,id);
            if(timer==null||!canRepeat(timer.durationMs))
                throw new IllegalArgumentException("Wiederholung erst ab 10 Sekunden möglich");
        }
        if(!prefs(c).edit().putBoolean(key("repeat",id),enabled).commit())
            throw new IllegalStateException("Wiederholung konnte nicht gespeichert werden");
    }
    public static void copy(Context c,int sourceId,int targetId){
        SharedPreferences.Editor editor=prefs(c).edit();
        editor.putBoolean(key("tone",targetId),tone(c,sourceId))
              .putBoolean(key("vibration",targetId),vibration(c,sourceId))
              .putInt(key("format",targetId),timeFormat(c,sourceId))
              .putInt(key("extra",targetId),extraInfo(c,sourceId))
              .putInt(key("start_delay",targetId),startDelaySeconds(c,sourceId));
        TimerEntry target=TimerRepository.get(c,targetId);
        if(target!=null&&canRepeat(target.durationMs))
            editor.putBoolean(key("repeat",targetId),autoRepeat(c,sourceId));
        if(!editor.commit())throw new IllegalStateException("Timer-Einstellungen konnten nicht kopiert werden");
    }
    public static void delete(Context c,int id){
        if(!prefs(c).edit().remove(key("tone",id))
                .remove(key("vibration",id)).remove(key("format",id))
                .remove(key("extra",id)).remove(key("repeat",id))
                .remove(key("start_delay",id)).commit())
            throw new IllegalStateException("Timer-Einstellungen konnten nicht gelöscht werden");
    }
    /** Unit-testable channel selector. The existing both-on channel is retained. */
    public static String channelId(boolean tone,boolean vibrate){
        if(tone&&vibrate)return "sanftwecker_timer_done_v1";
        if(tone)return "sanftwecker_timer_tone_only_v1";
        if(vibrate)return "sanftwecker_timer_vibration_only_v1";
        return "sanftwecker_timer_silent_v1";
    }
}
