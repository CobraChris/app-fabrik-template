package com.cobrachris.sanftwecker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;

/** One finite system notification when a countdown completes. No direct vibration loop. */
public final class TimerAlerts {
    private static final String CHANNEL="sanftwecker_timer_done_v1";
    private static final int BASE_ID=42000;
    private TimerAlerts(){}

    public static void ensureChannel(Context c){
        NotificationManager m=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(m==null)return;
        // Preserve the existing channel for timers with both notifications enabled:
        // Android owns channel settings after first creation.
        createChannel(m,CHANNEL,"Fertige Timer",true,true);
        createChannel(m,TimerAlertSettings.channelId(true,false),
                "Timer – nur Alarmton",true,false);
        createChannel(m,TimerAlertSettings.channelId(false,true),
                "Timer – nur Vibration",false,true);
        createChannel(m,TimerAlertSettings.channelId(false,false),
                "Timer – lautlos",false,false);
    }
    private static void createChannel(NotificationManager m,String id,String label,
            boolean tone,boolean vibration){
        NotificationChannel ch=new NotificationChannel(id,label,
                vibration||tone?NotificationManager.IMPORTANCE_HIGH:
                        NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Benachrichtigung für einen beendeten Countdown");
        if(tone)ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),null);
        else ch.setSound(null,null);
        ch.enableVibration(vibration);
        if(vibration)ch.setVibrationPattern(new long[]{0,250,150,250});
        m.createNotificationChannel(ch);
    }
    public static void show(Context c,TimerEntry timer){
        ensureChannel(c);
        NotificationManager m=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(m==null||!m.areNotificationsEnabled()){
            AlarmRepository.diagnostic(c,"ACHTUNG: Timer-Benachrichtigungen blockiert");
            return;
        }
        Intent open=new Intent(c,TimerActivity.class);
        PendingIntent launch=PendingIntent.getActivity(c,BASE_ID+timer.id,open,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent dismiss=new Intent(c,TimerReceiver.class)
                .setAction(TimerReceiver.ACTION_DISMISS).putExtra(TimerScheduler.EXTRA_ID,timer.id);
        PendingIntent stop=PendingIntent.getBroadcast(c,BASE_ID+timer.id,dismiss,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String channelId=TimerAlertSettings.channelId(
                TimerAlertSettings.tone(c,timer.id),
                TimerAlertSettings.vibration(c,timer.id));
        Notification notification=new Notification.Builder(c,channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(timer.name+" ist fertig")
                .setContentText("Timer abgelaufen – öffnen oder quittieren")
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(launch)
                .setAutoCancel(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel,"Quittieren",stop).build())
                .build();
        try{m.notify(BASE_ID+timer.id,notification);}
        catch(RuntimeException ex){AlarmRepository.diagnostic(c,"Timer-Benachrichtigung fehlgeschlagen");}
    }
    public static void dismiss(Context c,int id){
        NotificationManager m=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(m!=null)m.cancel(BASE_ID+id);
    }
}
