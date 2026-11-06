package com.cobrachris.sanftwecker;

import java.util.Locale;

/** No Android dependencies: formatting covered by local JUnit tests. */
public final class ClockFormat {
    private ClockFormat(){}
    public static String stopwatch(long millis){
        long ms=Math.max(0,millis);
        long hours=ms/3_600_000L;
        long minutes=(ms/60_000L)%60;
        long seconds=(ms/1000L)%60;
        return String.format(Locale.GERMANY,"%02d:%02d:%02d.%03d",
                hours,minutes,seconds,ms%1000);
    }
    public static String countdown(long millis){
        // Round up: a running timer shows 00:01 until the last millisecond.
        long secs=Math.max(0,millis/1000L+(millis%1000L>0?1:0));
        return String.format(Locale.GERMANY,"%02d:%02d:%02d",
                secs/3600,(secs/60)%60,secs%60);
    }
    public static String split(long total,long previous){
        return stopwatch(Math.max(0,total-previous));
    }
}
