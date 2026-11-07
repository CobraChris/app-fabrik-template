package com.cobrachris.sanftwecker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * One exact pending interval and one preparation cue per timer.
 * Old broadcasts cannot fire after a pause, reset, edit or restart:
 * both the expected timer deadline and the milestone timestamp are verified.
 */
public final class TimerMilestoneScheduler {
    public static final String ACTION_INTERVAL=
            "com.cobrachris.sanftwecker.TIMER_INTERVAL";
    public static final String ACTION_PREPARE=
            "com.cobrachris.sanftwecker.TIMER_PREPARE";
    public static final String EXTRA_EVENT_AT="milestone_at";
    private static final int INTERVAL_CODE=50000, PREPARE_CODE=60000;
    private TimerMilestoneScheduler(){}

    private static PendingIntent pending(Context c,int id,boolean prep,long due,long event){
        Intent intent=new Intent(c,TimerReceiver.class)
                .setAction(prep?ACTION_PREPARE:ACTION_INTERVAL)
                .putExtra(TimerScheduler.EXTRA_ID,id)
                .putExtra(TimerScheduler.EXTRA_DUE,due)
                .putExtra(EXTRA_EVENT_AT,event);
        return PendingIntent.getBroadcast(c,
                (prep?PREPARE_CODE:INTERVAL_CODE)+id,intent,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    public static void cancel(Context c,int id){
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(am==null)return;
        am.cancel(pending(c,id,false,0,0));
        am.cancel(pending(c,id,true,0,0));
    }
    private static void schedule(Context c,TimerEntry timer,boolean prep,long event){
        if(event<=0)return;
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(am==null)return;
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,event,
                pending(c,timer.id,prep,timer.dueWallMs,event));
    }
    /** Fail safe: a milestone failure must never cancel the actual finishing alarm. */
    public static void reschedule(Context c,int id){
        cancel(c,id);
        TimerEntry timer=TimerRepository.get(c,id);
        if(timer==null)return;
        long now=System.currentTimeMillis();
        try{
            int interval=TimerAlertSettings.intervalSeconds(c,id);
            if(interval>0){
                long time=TimerMilestones.nextIntervalWall(timer,interval,now);
                if(time>0)schedule(c,timer,false,time);
            }
            if(TimerAlertSettings.prepareCue(c,id)){
                long time=TimerMilestones.preparationWall(timer,now);
                if(time>0)schedule(c,timer,true,time);
            }
        }catch(RuntimeException ex){
            AlarmRepository.diagnostic(c,"Timer-Zwischenalarm nicht geplant: "+timer.name);
        }
    }
    public static synchronized void fired(Context c,int id,boolean preparation,
            long expectedDue,long expectedEvent){
        TimerEntry timer=TimerRepository.get(c,id);
        long now=System.currentTimeMillis();
        if(timer==null||expectedDue!=timer.dueWallMs ||
                expectedEvent<=0||expectedEvent>now+1500L||
                (timer.state!=TimerEntry.RUNNING&&timer.state!=TimerEntry.WAITING))
            return;
        if(now>expectedEvent+15_000L){
            // Do not make noise for a stale milestone after Doze; keep
            // future intervals scheduled, if any remain.
            if(!preparation)reschedule(c,id);
            return;
        }
        if(preparation){
            if(!TimerAlertSettings.prepareCue(c,id) ||
                    TimerMilestones.startWall(timer)-3000L!=expectedEvent ||
                    timer.state!=TimerEntry.WAITING)return;
            TimerAlerts.showMilestone(c,timer,true);
            // No next preparation event in this cycle.
        }else{
            int interval=TimerAlertSettings.intervalSeconds(c,id);
            if(interval<=0||expectedEvent>=timer.dueWallMs ||
                    expectedEvent<=TimerMilestones.startWall(timer) ||
                    (expectedEvent-TimerMilestones.startWall(timer))%
                            (interval*1000L)!=0)return;
            // Set next event before showing notification to survive a transient UI failure.
            try{
                long next=TimerMilestones.nextIntervalWall(
                        timer,interval,Math.max(now,expectedEvent));
                if(next>0)schedule(c,timer,false,next);
            }catch(RuntimeException ex){
                AlarmRepository.diagnostic(c,"Intervall-Alarm neu planen fehlgeschlagen");
            }
            TimerAlerts.showMilestone(c,timer,false);
        }
    }
}