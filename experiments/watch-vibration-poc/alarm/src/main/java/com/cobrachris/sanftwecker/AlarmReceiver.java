package com.cobrachris.sanftwecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public final class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        int id = intent.getIntExtra(AlarmScheduler.EXTRA_ID, 1);
        if (id < 1 || id > AlarmProfile.COUNT) return;

        if (AlarmScheduler.ACTION_WEEKLY.equals(action)) {
            if (!AlarmRepository.get(context, id).enabled) return;
            AlarmRepository.diagnostic(context, "Weckzeit ausgelöst: Wecker " + id);
            // Re-arm immediately. The receiver itself does not play audio.
            try { AlarmScheduler.reschedule(context, id); }
            catch (RuntimeException e) {
                Log.e("Sanftwecker", "Could not reschedule following alarm", e);
            }
        } else if (AlarmScheduler.ACTION_TEST.equals(action)) {
            context.getSharedPreferences(AlarmScheduler.PREF, Context.MODE_PRIVATE)
                    .edit().putLong(AlarmScheduler.TEST_WHEN, 0L).apply();
            AlarmRepository.diagnostic(context, "Testwecker ausgelöst: Wecker " + id);
        } else if (AlarmScheduler.ACTION_SNOOZE.equals(action)) {
            AlarmRepository.diagnostic(context, "Schlummerwecker ausgelöst: Wecker " + id);
        } else {
            return;
        }

        try {
            Intent start = new Intent(context, RingingService.class)
                    .setAction(RingingService.START)
                    .putExtra(AlarmScheduler.EXTRA_ID, id);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(start);
            else context.startService(start);
        } catch (RuntimeException e) {
            AlarmRepository.diagnostic(context, "FEHLER: Hintergrundalarm konnte nicht starten");
            Log.e("Sanftwecker", "Could not start ringing foreground service", e);
        }
    }
}
