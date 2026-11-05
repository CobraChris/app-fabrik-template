package com.cobrachris.sanftwecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Restore the weekly alarm when Android clears scheduled PendingIntents. */
public final class RescheduleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        try {
            AlarmScheduler.scheduleDaily(context);
        } catch (RuntimeException e) {
            Log.e("Sanftwecker", "Could not restore alarm after " + intent.getAction(), e);
        }
    }
}
