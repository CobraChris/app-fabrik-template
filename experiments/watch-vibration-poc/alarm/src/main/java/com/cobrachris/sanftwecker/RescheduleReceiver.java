package com.cobrachris.sanftwecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Correctly restore all stored alarm IDs after boot, update or timezone change. */
public final class RescheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent event) {
        if (event == null) return;
        try {
            AlarmScheduler.rescheduleAll(c);
            AlarmRepository.diagnostic(c,"Wecker nach Systemänderung neu geplant");
        } catch (RuntimeException ex) {
            AlarmRepository.diagnostic(c,"WARNUNG: Wecker-Neuplanung fehlgeschlagen");
            Log.e("Sanftwecker","Alarm reschedule failure",ex);
        }
        try {
            if (Intent.ACTION_BOOT_COMPLETED.equals(event.getAction()))
                StopwatchRepository.pauseAllAfterBoot(c);
            else StopwatchRepository.reconcile(c);
            TimerScheduler.restoreAll(c);
        } catch (RuntimeException ex) {
            AlarmRepository.diagnostic(c,"WARNUNG: Timer-Neuplanung fehlgeschlagen");
            Log.e("Sanftwecker","Timer reschedule failure",ex);
        }
    }
}
