package com.cobrachris.watchnotificationpoc;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Minimal notification mirroring PoC. A watch vibration is NOT guaranteed or
 * measurable here: Galaxy Wearable decides if and how to mirror notifications.
 */
public final class MainActivity extends Activity {
    private static final String CHANNEL_ID = "watch_notification_test_v1";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 100;
    private static final long DELAY_MILLIS = 10000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private NotificationManager notifications;
    private TextView status;
    private Button immediate;
    private Button delayed;
    private long pendingDelayMillis = -1L;
    private int notificationId = 1000;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        notifications = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
        buildUi();
    }

    private void buildUi() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER);
        screen.setPadding(32, 36, 32, 36);
        screen.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("Galaxy Watch: Benachrichtigungstest");
        title.setTextSize(23);
        title.setTextColor(Color.BLACK);
        screen.addView(title);

        TextView information = new TextView(this);
        information.setText(
                "Keine Watch-App, kein WLAN-Debugging. Samsung Galaxy Wearable " +
                "entscheidet, ob deine Uhr die Benachrichtigung empfängt und vibriert. " +
                "Das ist noch kein Wecker. Dein Handy kann beim Test ebenfalls kurz vibrieren.");
        information.setTextColor(Color.DKGRAY);
        information.setTextSize(16);
        information.setPadding(0, 24, 0, 28);
        screen.addView(information);

        immediate = new Button(this);
        immediate.setText("Testbenachrichtigung jetzt senden");
        screen.addView(immediate, fullWidth());
        immediate.setOnClickListener(v -> requestOrSend(0L));

        delayed = new Button(this);
        delayed.setText("In 10 Sekunden senden (Handy sperren)");
        screen.addView(delayed, fullWidth());
        delayed.setOnClickListener(v -> requestOrSend(DELAY_MILLIS));

        Button settings = new Button(this);
        settings.setText("Benachrichtigungsrechte öffnen");
        screen.addView(settings, fullWidth());
        settings.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        });

        status = new TextView(this);
        status.setText("Bereit. Bitte Benachrichtigungen dieser App auf Handy UND Uhr erlauben.");
        status.setTextColor(Color.BLACK);
        status.setTextSize(16);
        status.setPadding(0, 28, 0, 0);
        screen.addView(status);
        setContentView(screen);
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Watch-Vibrationstest", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Nur manuelle Testbenachrichtigungen; keine Weckfunktion");
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[] { 0L, 250L, 150L, 250L });
        channel.setSound(null, null); // No alarm sound from the phone during this test.
        notifications.createNotificationChannel(channel);
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
            status.setText("Benachrichtigungen sind deaktiviert. Bitte in den App-Einstellungen einschalten.");
            return;
        }
        sendOrSchedule(delayMillis);
    }

    private void sendOrSchedule(long delayMillis) {
        if (delayMillis == 0L) {
            postTestNotification();
        } else {
            delayed.setEnabled(false);
            status.setText("Test startet in 10 Sekunden. Jetzt Handybildschirm sperren.");
            handler.postDelayed(() -> {
                delayed.setEnabled(true);
                postTestNotification();
            }, delayMillis);
        }
    }

    private void postTestNotification() {
        try {
            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Galaxy Watch – Vibrationstest")
                    .setContentText("Vibriert deine Uhr? Testbenachrichtigung vom Handy.")
                    .setCategory(Notification.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(false)
                    .build();
            notifications.notify(notificationId++, notification);
            status.setText(
                    "Benachrichtigung am Handy ausgelöst. " +
                    "Bitte selbst prüfen, ob die Uhr vibriert hat – " +
                    "diese App erhält keine Watch-Empfangsbestätigung.");
        } catch (SecurityException e) {
            status.setText("Benachrichtigungsberechtigung fehlt. Bitte in Android erlauben.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != NOTIFICATION_PERMISSION_REQUEST) return;
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted && pendingDelayMillis >= 0) {
            long requested = pendingDelayMillis;
            pendingDelayMillis = -1;
            sendOrSchedule(requested);
        } else {
            pendingDelayMillis = -1;
            status.setText("Benachrichtigungen wurden nicht erlaubt. Der Test kann so nicht starten.");
        }
    }
}
