package com.cobrachris.sanftwecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/** AlarmManager launches this receiver when the screen is locked. */
public final class AlarmReceiver extends BroadcastReceiver {
    public static final String DAILY = "com.cobrachris.sanftwecker.DAILY";
    public static final String PREVIEW = "com.cobrachris.sanftwecker.PREVIEW";
    public static final String SNOOZE = "com.cobrachris.sanftwecker.SNOOZE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!DAILY.equals(action) && !PREVIEW.equals(action) && !SNOOZE.equals(action)) return;
        if (DAILY.equals(action)) {
            if (!AlarmScheduler.prefs(context).getBoolean(AlarmScheduler.ENABLED, false)) return;
            try { AlarmScheduler.scheduleDaily(context); }
            catch (RuntimeException e) { Log.e("Sanftwecker", "Next alarm not scheduled", e); }
        } else if (PREVIEW.equals(action)) {
            AlarmScheduler.prefs(context).edit()
                    .putLong(AlarmScheduler.PREVIEW_NEXT, 0L).apply();
        }
        Intent service = new Intent(context, RingingService.class)
                .setAction(RingingService.START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(service);
        } else {
            context.startService(service);
        }
    }
}
