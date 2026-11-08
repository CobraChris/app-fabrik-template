package com.cobrachris.sanftwecker;

import java.util.Locale;

/** Pure presentation functions; never modify countdown deadlines. */
public final class TimerDisplay {
    private TimerDisplay(){}
    public static String time(long remainingMs,int format){
        long seconds=Math.max(0,remainingMs/1000+(remainingMs%1000>0?1:0));
        if(format==TimerAlertSettings.FORMAT_MS)
            return String.format(Locale.GERMANY,"%02d:%02d",seconds/60,seconds%60);
        return String.format(Locale.GERMANY,"%02d:%02d:%02d",
                seconds/3600,(seconds/60)%60,seconds%60);
    }
    public static String formatLabel(int format){
        return format==TimerAlertSettings.FORMAT_MS?"Min.:Sek.":"Std.:Min.:Sek.";
    }
    public static String extraLabel(int extra){
        if(extra==TimerAlertSettings.EXTRA_DURATION)return "Ursprüngliche Dauer";
        if(extra==TimerAlertSettings.EXTRA_NONE)return "Keine";
        return "Ziel-Uhrzeit";
    }
}
