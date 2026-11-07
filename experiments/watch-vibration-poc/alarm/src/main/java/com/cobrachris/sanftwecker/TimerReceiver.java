package com.cobrachris.sanftwecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Exact timer delivery and notification acknowledgement; never starts the wake alarm service. */
public final class TimerReceiver extends BroadcastReceiver {
    public static final String ACTION_DISMISS="com.cobrachris.sanftwecker.TIMER_DISMISS";

    @Override public void onReceive(Context c,Intent intent){
        if(intent==null)return;
        int id=intent.getIntExtra(TimerScheduler.EXTRA_ID,-1);
        if(id<1)return;
        if(TimerScheduler.ACTION_FINISH.equals(intent.getAction())){
            TimerScheduler.fired(c,id,intent.getLongExtra(TimerScheduler.EXTRA_DUE,-1));
        }else if(TimerMilestoneScheduler.ACTION_INTERVAL.equals(intent.getAction()) ||
                TimerMilestoneScheduler.ACTION_PREPARE.equals(intent.getAction())){
            TimerMilestoneScheduler.fired(c,id,
                    TimerMilestoneScheduler.ACTION_PREPARE.equals(intent.getAction()),
                    intent.getLongExtra(TimerScheduler.EXTRA_DUE,-1),
                    intent.getLongExtra(TimerMilestoneScheduler.EXTRA_EVENT_AT,-1));
        }else if(ACTION_DISMISS.equals(intent.getAction())){
            TimerAlerts.dismiss(c,id);
        }
    }
}
