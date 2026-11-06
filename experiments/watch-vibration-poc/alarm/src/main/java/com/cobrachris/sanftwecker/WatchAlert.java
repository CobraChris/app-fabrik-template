package com.cobrachris.sanftwecker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/** Independent first alarm notification, even if the audio foreground service fails. */
public final class WatchAlert {
    public static final int ID_BASE = 7200;
    private WatchAlert() {}

    public static String channelId(int mode) {
        // Reliability first: Samsung already forwarded this exact channel successfully.
        // Mirroring cannot guarantee per-pattern haptics on the Galaxy Watch.
        return RingingService.WATCH_CHANNEL;
    }

    public static void ensureChannel(Context c) {
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null)return;
        NotificationChannel channel=new NotificationChannel(
                RingingService.WATCH_CHANNEL, "Galaxy Watch: Weckimpulse",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Tonlose Alarm-Erinnerungen an Galaxy Wearable weitergeben");
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0,350,150,350});
        channel.setSound(null,null);
        nm.createNotificationChannel(channel);
    }

    public static boolean canSend(Context c,int mode) {
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null||!nm.areNotificationsEnabled())return false;
        NotificationChannel channel=nm.getNotificationChannel(channelId(mode));
        return channel!=null&&channel.getImportance()!=NotificationManager.IMPORTANCE_NONE;
    }

    public static boolean send(Context c,AlarmProfile p,int index) {
        ensureChannel(c);
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null||!canSend(c,p.vibrationMode)){
            AlarmRepository.diagnostic(c,"WATCH BLOCKIERT: Android-/Kanal-Benachrichtigung aus");
            return false;
        }
        Intent open=new Intent(c,AlarmActivity.class)
                .putExtra(AlarmScheduler.EXTRA_ID,p.id);
        PendingIntent openIntent=PendingIntent.getActivity(c,7000+p.id,open,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(c,9012,
                new Intent(c,RingingService.class).setAction(RingingService.STOP),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder n=new Notification.Builder(c,channelId(p.vibrationMode))
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Sanftwecker: "+p.name)
                .setContentText("Aufwachen – Impuls "+(index+1)+" von "+p.watchPulses)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(openIntent)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause,"Stoppen",stop).build())
                .setAutoCancel(true);
        try{
            nm.notify(ID_BASE+index,n.build());
            AlarmRepository.diagnostic(c,"Watch-Impuls "+(index+1)+" von "+p.watchPulses+
                    " an Android übergeben (Uhr-Empfang unbekannt)");
            return true;
        }catch(RuntimeException e){
            AlarmRepository.diagnostic(c,"FEHLER: Watch-Benachrichtigung nicht gesendet");
            return false;
        }
    }
}
