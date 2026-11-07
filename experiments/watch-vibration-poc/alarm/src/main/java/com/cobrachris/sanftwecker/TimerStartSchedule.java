package com.cobrachris.sanftwecker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

/** One-time, user-requested timer start at a chosen local clock time.
 * This is independent of wake alarms and does not change their notification policy.
 */
public final class TimerStartSchedule {
    public static final String ACTION_START="com.cobrachris.sanftwecker.TIMER_START_SCHEDULED";
    private static final String PREF="sanftwecker_timer_start_schedule_v1";
    private static final int CODE_BASE=71000;
    private static final long MAX_LATE_MS=5L*60L*1000L;
    private TimerStartSchedule(){}

    private static SharedPreferences prefs(Context c){
        return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
    }

    /** Find the next occurrence of the selected hour/minute in the device's local zone.
     * Skipped DST times advance according to the platform calendar's normal behavior.
     */
    public static long nextLocalTime(int hour,int minute,long now,TimeZone zone){
        if(hour<0||hour>23||minute<0||minute>59||zone==null)
            throw new IllegalArgumentException("Ungültige Startzeit");
        Calendar date=Calendar.getInstance(zone);
        date.setTimeInMillis(now);
        date.set(Calendar.HOUR_OF_DAY,hour);
        date.set(Calendar.MINUTE,minute);
        date.set(Calendar.SECOND,0);
        date.set(Calendar.MILLISECOND,0);
        if(date.getTimeInMillis()<=now)date.add(Calendar.DATE,1);
        return date.getTimeInMillis();
    }

    public static long get(Context c,int id){
        if(id<1||id>9999)return 0;
        return prefs(c).getLong("start_"+id,0L);
    }

    private static PendingIntent pending(Context c,int id,long scheduledAt){
        Intent intent=new Intent(c,TimerReceiver.class)
                .setAction(ACTION_START)
                .putExtra(TimerScheduler.EXTRA_ID,id)
                .putExtra(TimerScheduler.EXTRA_DUE,scheduledAt);
        return PendingIntent.getBroadcast(c,CODE_BASE+id,intent,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static void arm(Context c,int id,long time){
        if(!AlarmScheduler.canSchedule(c))
            throw new SecurityException("Genaue Alarme sind in Android nicht erlaubt");
        AlarmManager manager=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(manager==null)throw new IllegalStateException("AlarmManager fehlt");
        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,pending(c,id,time));
    }
    private static void cancelAlarm(Context c,int id){
        AlarmManager manager=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(manager!=null)manager.cancel(pending(c,id,0));
    }

    public static synchronized void set(Context c,int id,long scheduledAt){
        TimerEntry item=TimerRepository.get(c,id);
        if(item==null)throw new IllegalArgumentException("Timer nicht gefunden");
        if(item.state==TimerEntry.RUNNING||item.state==TimerEntry.WAITING)
            throw new IllegalStateException("Laufenden Timer zuerst pausieren");
        long now=System.currentTimeMillis();
        if(scheduledAt<=now+500L||scheduledAt>now+2L*24L*60L*60L*1000L)
            throw new IllegalArgumentException("Zeit muss innerhalb der nächsten zwei Tage liegen");
        long old=get(c,id);
        arm(c,id,scheduledAt);
        if(!prefs(c).edit().putLong("start_"+id,scheduledAt).commit()){
            cancelAlarm(c,id);
            if(old>System.currentTimeMillis()){
                try{arm(c,id,old);}catch(RuntimeException ignored){}
            }
            throw new IllegalStateException("Startzeit konnte nicht gespeichert werden");
        }
    }

    public static synchronized void cancel(Context c,int id){
        cancelAlarm(c,id);
        if(!prefs(c).edit().remove("start_"+id).commit())
            throw new IllegalStateException("Geplante Startzeit konnte nicht gelöscht werden");
    }

    /** Only the currently stored appointment may start; very late broadcasts
     * must not unexpectedly launch a countdown after a reboot or Doze. */
    public static boolean isOnTime(long stored,long expectedAt,long now){
        return stored>0 && expectedAt==stored &&
                now+1500L>=stored && now-stored<=MAX_LATE_MS;
    }
    /** The receiver is deliberately idempotent; late or superseded starts are ignored. */
    public static synchronized void fired(Context c,int id,long expectedAt){
        long stored=get(c,id);
        if(stored==0||stored!=expectedAt)return;
        long now=System.currentTimeMillis();
        if(now+1500L<stored){
            try{arm(c,id,stored);}catch(RuntimeException ex){
                AlarmRepository.diagnostic(c,"WARNUNG: geplanter Timer nicht mehr aktiv");
            }
            return;
        }
        cancel(c,id);
        TimerEntry item=TimerRepository.get(c,id);
        if(item==null)return;
        if(!isOnTime(stored,expectedAt,now)){
            AlarmRepository.diagnostic(c,"Geplanter Timerstart verpasst: "+item.name);
            return;
        }
        if(item.state==TimerEntry.RUNNING||item.state==TimerEntry.WAITING)return;
        try{
            if(item.state==TimerEntry.FINISHED)TimerScheduler.reset(c,id);
            TimerScheduler.start(c,id);
            AlarmRepository.diagnostic(c,"Geplanter Timer gestartet: "+item.name);
        }catch(RuntimeException ex){
            AlarmRepository.diagnostic(c,"WARNUNG: geplanter Timer konnte nicht starten: "+item.name);
        }
    }

    /** Called after phone reboot, package replacement or clock/permission changes. */
    public static synchronized void restoreAll(Context c){
        for(Map.Entry<String,?> entry:prefs(c).getAll().entrySet()){
            if(!entry.getKey().startsWith("start_"))continue;
            try{
                int id=Integer.parseInt(entry.getKey().substring(6));
                if(id<1||id>9999)continue;
                long when=get(c,id);
                TimerEntry t=TimerRepository.get(c,id);
                if(t==null||when<=System.currentTimeMillis() ||
                        t.state==TimerEntry.RUNNING||t.state==TimerEntry.WAITING){
                    cancel(c,id);
                    if(t!=null&&when>0&&when<=System.currentTimeMillis())
                        AlarmRepository.diagnostic(c,"Geplanter Timerstart verpasst: "+t.name);
                }else{
                    arm(c,id,when);
                }
            }catch(RuntimeException ex){
                AlarmRepository.diagnostic(c,"WARNUNG: Timer-Zeitplan nicht wiederhergestellt");
            }
        }
    }
}
