package com.cobrachris.sanftwecker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/** Separate exact alarm per countdown; never shares a request ID with wake alarms. */
public final class TimerScheduler {
    public static final String ACTION_FINISH="com.cobrachris.sanftwecker.TIMER_FINISH";
    public static final String EXTRA_ID="timer_id";
    public static final String EXTRA_DUE="timer_due";
    private static final int CODE_BASE=30000;
    private TimerScheduler(){}

    private static PendingIntent pending(Context c,int id,long due){
        Intent i=new Intent(c,TimerReceiver.class)
                .setAction(ACTION_FINISH).putExtra(EXTRA_ID,id).putExtra(EXTRA_DUE,due);
        return PendingIntent.getBroadcast(c,CODE_BASE+id,i,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static void cancelPending(Context c,int id){
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(am!=null)am.cancel(pending(c,id,0));
    }
    private static void arm(Context c,TimerEntry t){
        if(!AlarmScheduler.canSchedule(c))
            throw new SecurityException("Alarme und Erinnerungen fehlen");
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(am==null)throw new IllegalStateException("AlarmManager fehlt");
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,t.dueWallMs,
                pending(c,t.id,t.dueWallMs));
    }
    public static synchronized void start(Context c,int id){
        TimerEntry before=TimerRepository.get(c,id);
        if(before==null)return;
        if(before.state==TimerEntry.RUNNING)return;
        TimerEntry after=before.start(System.currentTimeMillis(),SystemClock.elapsedRealtime());
        if(after==before)return;
        try{
            arm(c,after);
            TimerRepository.put(c,after);
        }catch(RuntimeException e){
            cancelPending(c,id);
            TimerRepository.put(c,before);
            throw e;
        }
    }
    public static synchronized void pause(Context c,int id){
        TimerEntry before=TimerRepository.get(c,id);
        if(before==null)return;
        cancelPending(c,id);
        if(before.state==TimerEntry.RUNNING)
            TimerRepository.put(c,before.pause(System.currentTimeMillis(),
                    SystemClock.elapsedRealtime()));
    }
    public static synchronized void reset(Context c,int id){
        TimerEntry before=TimerRepository.get(c,id);
        if(before==null)return;
        cancelPending(c,id);
        TimerRepository.put(c,before.reset());
        TimerAlerts.dismiss(c,id);
    }
    public static synchronized void edit(Context c,int id,String name,long durationMs){
        TimerEntry t=TimerRepository.get(c,id);
        if(t==null)return;
        cancelPending(c,id);
        TimerRepository.put(c,t.edit(name,durationMs));
        TimerAlerts.dismiss(c,id);
    }
    public static synchronized void delete(Context c,int id){
        cancelPending(c,id);
        TimerAlerts.dismiss(c,id);
        TimerRepository.delete(c,id);
    }
    public static synchronized void fired(Context c,int id,long expectedDue) {
        TimerEntry current=TimerRepository.get(c,id);
        if(current==null || current.state!=TimerEntry.RUNNING ||
                current.dueWallMs!=expectedDue)return; // stale/already paused
        long left=current.remaining(System.currentTimeMillis(),SystemClock.elapsedRealtime());
        if(left>1500){
            // Android can deliver an alarm slightly early after a clock adjustment.
            try{arm(c,current);}catch(RuntimeException e){
                AlarmRepository.diagnostic(c,"Timer konnte nicht neu geplant werden");
            }
            return;
        }
        TimerRepository.put(c,current.finish());
        TimerAlerts.show(c,current);
        AlarmRepository.diagnostic(c,"Timer beendet: "+current.name);
    }
    public static synchronized void restoreAll(Context c){
        for(TimerEntry t:TimerRepository.all(c)){
            if(t.state!=TimerEntry.RUNNING)continue;
            if(t.dueWallMs<=System.currentTimeMillis()){
                // Do not buzz unexpectedly just because the phone rebooted late.
                TimerRepository.put(c,t.finish());
                AlarmRepository.diagnostic(c,"Timer nach Neustart abgeschlossen: "+t.name);
            }else{
                try{arm(c,t);}catch(RuntimeException e){
                    // Keep the timer visible with a warning rather than claiming reliability.
                    AlarmRepository.diagnostic(c,"WARNUNG: Timer nicht geplant: "+t.name);
                }
            }
        }
    }
}
