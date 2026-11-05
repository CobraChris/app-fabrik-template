package com.cobrachris.watchnotificationpoc;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Proof of concept: phone notification forwarded to a Samsung Galaxy Watch.
 * The scheduled notification uses AlarmManager, not a UI Handler timer.
 */
public final class MainActivity extends Activity {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 100;
    private static final long DELAY_MILLIS = 20_000L;

    private NotificationManager notifications;
    private TextView status;
    private TextView history;
    private long pendingDelayMillis = -1L;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        notifications = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationTest.createChannel(this);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHistory();
    }

    private void buildUi() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER);
        screen.setPadding(32, 36, 32, 36);
        screen.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("Galaxy Watch: Standby-Test");
        title.setTextSize(23);
        title.setTextColor(Color.BLACK);
        screen.addView(title);

        TextView information = new TextView(this);
        information.setText(
                "Prüft Benachrichtigungen bei gesperrtem Bildschirm. " +
                "Keine Watch-App oder WLAN-Debugging nötig. " +
                "Ein echter Wecker ist das noch nicht.");
        information.setTextColor(Color.DKGRAY);
        information.setTextSize(16);
        information.setPadding(0, 24, 0, 28);
        screen.addView(information);

        Button immediate = new Button(this);
        immediate.setText("Jetzt senden (Kontrolltest)");
        screen.addView(immediate, fullWidth());
        immediate.setOnClickListener(v -> requestOrSend(0L));

        Button delayed = new Button(this);
        delayed.setText("In 20 Sekunden senden (Handy sperren)");
        screen.addView(delayed, fullWidth());
        delayed.setOnClickListener(v -> requestOrSend(DELAY_MILLIS));

        Button settings = new Button(this);
        settings.setText("Benachrichtigungseinstellungen");
        screen.addView(settings, fullWidth());
        settings.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        });

        status = new TextView(this);
        status.setText("Bereit. Watch-Benachrichtigungstest in Galaxy Wearable zulassen.");
        status.setTextColor(Color.BLACK);
        status.setTextSize(16);
        status.setPadding(0, 28, 0, 8);
        screen.addView(status);

        history = new TextView(this);
        history.setTextSize(16);
        history.setTextColor(Color.DKGRAY);
        screen.addView(history);
        setContentView(screen);
        refreshHistory();
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void refreshHistory() {
        if (history == null) return;
        long posted = getSharedPreferences(NotificationTest.PREFS, MODE_PRIVATE)
                .getLong(NotificationTest.LAST_TRIGGER, 0L);
        long scheduled = getSharedPreferences(NotificationTest.PREFS, MODE_PRIVATE)
                .getLong(NotificationTest.SCHEDULED_AT, 0L);
        history.setText(
                "Letzte Benachrichtigung vom Handy ausgelöst: " +
                (posted == 0L ? "noch keine" : NotificationTest.localTime(posted)) +
                "\nLetzter geplanter Test: " +
                (scheduled == 0L ? "noch keiner" : NotificationTest.localTime(scheduled)));
    }

    private void requestOrSend(long delayMillis) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            pendingDelayMillis = delayMillis;
            requestPermissions(
                    new String[] { Manifest.permission.POST_NOTIFICATIONS },
                    NOTIFICATION_PERMISSION_REQUEST);
            return;
        }
        if (!notifications.areNotificationsEnabled()) {
            status.setText("Benachrichtigungen sind deaktiviert. In den App-Einstellungen erlauben.");
            return;
        }
        if (delayMillis == 0L) {
            boolean ok = NotificationTest.post(this);
            status.setText(ok
                    ? "Benachrichtigung jetzt ausgelöst. Prüfe die Uhr."
                    : "Konnte keine Benachrichtigung senden.");
            refreshHistory();
            return;
        }
        scheduleSystemAlarm(delayMillis);
    }

    private void scheduleSystemAlarm(long delayMillis) {
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (manager == null) {
            status.setText("Android-Alarmdienst nicht verfügbar.");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            status.setText(
                    "Einmalig 'Alarme und Erinnerungen' für Watch-Benachrichtigungstest erlauben. " +
                    "Danach zurück zur App und den 20-Sekunden-Test erneut starten.");
            try {
                Intent permission = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                permission.setData(Uri.parse("package:" + getPackageName()));
                startActivity(permission);
            } catch (RuntimeException error) {
                status.setText("Bitte manuell in Android: Spezieller Zugriff → Alarme und Erinnerungen erlauben.");
            }
            return;
        }
        long trigger = System.currentTimeMillis() + delayMillis;
        Intent intent = new Intent(this, TestNotificationReceiver.class)
                .setAction(TestNotificationReceiver.ACTION);
        PendingIntent operation = PendingIntent.getBroadcast(
                this, 2200, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, operation);
            getSharedPreferences(NotificationTest.PREFS, MODE_PRIVATE)
                    .edit().putLong(NotificationTest.SCHEDULED_AT, trigger).apply();
            status.setText("Android-Systemalarm für " + NotificationTest.localTime(trigger) +
                    " geplant. Handy jetzt sperren.");
            refreshHistory();
        } catch (SecurityException error) {
            status.setText("Exakte Alarmberechtigung fehlt. Bitte in den Einstellungen erlauben.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != NOTIFICATION_PERMISSION_REQUEST) return;
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted && pendingDelayMillis >= 0) {
            long delay = pendingDelayMillis;
            pendingDelayMillis = -1L;
            requestOrSend(delay);
        } else {
            pendingDelayMillis = -1L;
            status.setText("Benachrichtigungen wurden nicht erlaubt.");
        }
    }
}
