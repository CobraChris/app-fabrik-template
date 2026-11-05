package com.cobrachris.sanftwecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Restores exact alarms after boot, app update, clock/timezone change or permission grant. */
public final class RescheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        try {
            AlarmScheduler.rescheduleAll(context);
            AlarmRepository.diagnostic(context, "Weckzeiten nach Systemereignis geprüft");
        } catch (RuntimeException e) {
            AlarmRepository.diagnostic(context, "WARNUNG: Weckzeiten konnten nicht geplant werden");
            Log.e("Sanftwecker", "Reschedule failed: " + intent.getAction(), e);
        }
    }
}
