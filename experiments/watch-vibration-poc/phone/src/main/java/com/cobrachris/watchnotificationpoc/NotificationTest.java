package com.cobrachris.watchnotificationpoc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import java.text.DateFormat;
import java.util.Date;

/**
 * Local notification test. Wearable mirroring is performed externally by Samsung.
 */
final class NotificationTest {
    private static final String CHANNEL_ID = "watch_notification_test_v1";
    static final String PREFS = "watch_notification_test";
    static final String LAST_TRIGGER = "last_trigger";
    static final String SCHEDULED_AT = "scheduled_at";

    private NotificationTest() {}

    static String localTime(long whenMillis) {
        return DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(whenMillis));
    }

    static void createChannel(Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Watch-Benachrichtigungstest", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Stille Testbenachrichtigungen zum Spiegeln an die Galaxy Watch");
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[] { 0L, 250L, 150L, 250L });
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
    }

    static boolean post(Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (!manager.areNotificationsEnabled()) return false;
        createChannel(context);
        long timestamp = System.currentTimeMillis();
        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Galaxy Watch – Standby-Test")
                .setContentText("Testbenachrichtigung ausgelöst um " + localTime(timestamp))
                .setCategory(Notification.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .build();
        try {
            manager.notify((int) (timestamp & 0x7fffffff), notification);
        } catch (SecurityException exception) {
            return false;
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putLong(LAST_TRIGGER, timestamp).apply();
        return true;
    }
}
