package com.cobrachris.sanftwecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public final class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        int id = intent.getIntExtra(AlarmScheduler.EXTRA_ID, -1);
        long when = intent.getLongExtra(AlarmScheduler.EXTRA_WHEN, 0L);
        AlarmProfile profile = AlarmRepository.get(c, id);
        if (profile == null) return;

        if (AlarmScheduler.ACTION_WEEKLY.equals(action)) {
            if (!profile.enabled) return;
            AlarmRepository.diagnostic(c, "Weckzeit ausgelöst: " + profile.name);
            try { AlarmScheduler.onFired(c, id, when); }
            catch (RuntimeException e) {
                Log.e("Sanftwecker", "Next occurrence not scheduled", e);
                AlarmRepository.diagnostic(c, "WARNUNG: nächster Alarm nicht planbar");
            }
        } else if (AlarmScheduler.ACTION_TEST.equals(action)) {
            c.getSharedPreferences(AlarmScheduler.PREF, Context.MODE_PRIVATE).edit()
                    .putLong(AlarmScheduler.TEST_WHEN, 0).apply();
            AlarmRepository.diagnostic(c, "Testwecker ausgelöst: " + profile.name);
        } else if (AlarmScheduler.ACTION_SNOOZE.equals(action)) {
            AlarmRepository.diagnostic(c, "Schlummerwecker ausgelöst: " + profile.name);
        } else return;

        // Post the first watch alert directly from the exact-alarm receiver.
        // Even a failed foreground-service startup must not silently skip the watch alert.
        if (profile.watchPulses > 0) WatchAlert.send(c, profile, 0);
        try {
            Intent start = new Intent(c, RingingService.class)
                    .setAction(RingingService.START)
                    .putExtra(AlarmScheduler.EXTRA_ID, id)
                    .putExtra(RingingService.EXTRA_QUICK_TEST,
                            AlarmScheduler.ACTION_TEST.equals(action));
            AlarmRepository.diagnostic(c, "Audiodienst angefordert: " + profile.name);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) c.startForegroundService(start);
            else c.startService(start);
        } catch (RuntimeException ex) {
            AlarmRepository.diagnostic(c, "FEHLER: Alarmausgabe nicht gestartet");
            Log.e("Sanftwecker", "Foreground service failed", ex);
        }
    }
}
