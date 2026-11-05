package com.cobrachris.sanftwecker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

/**
 * Active alarm only. FGS plays a gentle self-generated tone from volume zero,
 * and posts a succession of normal notifications for Samsung watch mirroring.
 * Notification mirroring does not guarantee a watch vibration.
 */
public final class RingingService extends Service {
    public static final String START = "com.cobrachris.sanftwecker.START";
    public static final String STOP = "com.cobrachris.sanftwecker.STOP";
    public static final String SNOOZE = "com.cobrachris.sanftwecker.SNOOZE";
    private static final String ONGOING_CHANNEL = "sanftwecker_active_v1";
    private static final String WATCH_CHANNEL = "sanftwecker_watch_v1";
    private static final int ONGOING_ID = 601;
    private static final int WATCH_BASE_ID = 7000;
    private static final long MAX_RING_MILLIS = 15 * 60_000L;
    private static final int MAX_WATCH_ALERTS = 6;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private NotificationManager notifications;
    private AudioTrack track;
    private Thread audioThread;
    private PowerManager.WakeLock wakeLock;
    private boolean ringing;
    private long startedAt;
    private int watchAlertCount;

    private final Runnable ramp = new Runnable() {
        @Override public void run() {
            if (!ringing || track == null) return;
            int durationSeconds = AlarmScheduler.prefs(RingingService.this)
                    .getInt(AlarmScheduler.RAMP, 300);
            int maxPercent = AlarmScheduler.prefs(RingingService.this)
                    .getInt(AlarmScheduler.MAX_VOLUME, 35);
            double elapsed = (SystemClock.elapsedRealtime() - startedAt) / 1000.0;
            double progress = Math.max(0, Math.min(1, elapsed / Math.max(60, durationSeconds)));
            // Quadratic easing: no sudden minimum volume; initial gain is exactly zero.
            float gain = (float) ((maxPercent / 100.0) * progress * progress);
            try { track.setVolume(gain); }
            catch (IllegalStateException ignored) { }
            handler.postDelayed(this, 200L);
        }
    };

    private final Runnable watchPulse = new Runnable() {
        @Override public void run() {
            if (!ringing || watchAlertCount >= MAX_WATCH_ALERTS) return;
            int index = watchAlertCount++;
            sendWatchNotification(index);
            if (watchAlertCount < MAX_WATCH_ALERTS) handler.postDelayed(this, 20_000L);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        notifications = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        setupChannels();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (STOP.equals(action) || SNOOZE.equals(action)) {
            if (SNOOZE.equals(action)) {
                try { AlarmScheduler.scheduleSnooze(this); }
                catch (RuntimeException e) { Log.e("Sanftwecker", "Snooze scheduling failed", e); }
            } else {
                AlarmScheduler.cancelSnooze(this);
            }
            cleanupAndStop();
            return START_NOT_STICKY;
        }
        if (!START.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ringing) return START_NOT_STICKY;
        showOngoingNotification();
        ringing = true;
        startedAt = SystemClock.elapsedRealtime();
        watchAlertCount = 0;
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sanftwecker:Alarm");
                wakeLock.acquire(MAX_RING_MILLIS);
            }
            beginAudio();
            handler.post(ramp);
            handler.post(watchPulse);
            handler.postDelayed(this::cleanupAndStop, MAX_RING_MILLIS);
        } catch (RuntimeException e) {
            Log.e("Sanftwecker", "Could not start alarm audio", e);
            cleanupAndStop();
        }
        return START_NOT_STICKY;
    }

    private PendingIntent serviceAction(String action, int code) {
        Intent intent = new Intent(this, RingingService.class).setAction(action);
        return PendingIntent.getService(this, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void setupChannels() {
        NotificationChannel running = new NotificationChannel(
                ONGOING_CHANNEL, "Aktiver Sanftwecker", NotificationManager.IMPORTANCE_LOW);
        running.setSound(null, null);
        notifications.createNotificationChannel(running);
        NotificationChannel watch = new NotificationChannel(
                WATCH_CHANNEL, "Sanftwecker an Galaxy Watch", NotificationManager.IMPORTANCE_HIGH);
        watch.setSound(null, null);
        watch.enableVibration(true);
        watch.setVibrationPattern(new long[]{0L, 300L, 200L, 300L});
        notifications.createNotificationChannel(watch);
    }

    private void showOngoingNotification() {
        PendingIntent open = PendingIntent.getActivity(this, 610,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, ONGOING_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Sanftwecker klingelt")
                .setContentText("Ton wird langsam lauter. Zum Beenden tippen.")
                .setContentIntent(open)
                .setOngoing(true)
                .setLocalOnly(true)
                .setCategory(Notification.CATEGORY_ALARM)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause, "Stoppen",
                        serviceAction(STOP, 611)).build())
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_play, "5 Min. schlummern",
                        serviceAction(SNOOZE, 612)).build())
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(ONGOING_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(ONGOING_ID, notification);
        }
    }

    private void sendWatchNotification(int index) {
        if (!notifications.areNotificationsEnabled()) return;
        Notification notification = new Notification.Builder(this, WATCH_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Sanftwecker: Jetzt aufwachen")
                .setContentText("Uhr-Vibrationstest " + (index + 1) + "/" + MAX_WATCH_ALERTS)
                .setCategory(Notification.CATEGORY_ALARM)
                .setAutoCancel(true)
                .build();
        try { notifications.notify(WATCH_BASE_ID + index, notification); }
        catch (SecurityException e) {
            Log.w("Sanftwecker", "No notification permission for watch alerts", e);
        }
    }

    private void beginAudio() {
        final int sampleRate = 22050;
        int minBytes = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferBytes = Math.max(8820, minBytes);
        if (minBytes <= 0) throw new IllegalStateException("No supported audio output");
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        AudioFormat format = new AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build();
        track = new AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferBytes)
                .build();
        if (track.getState() != AudioTrack.STATE_INITIALIZED) {
            throw new IllegalStateException("AudioTrack could not initialize");
        }
        track.setVolume(0f);
        track.play();
        final AudioTrack sink = track;
        audioThread = new Thread(() -> {
            short[] buffer = new short[2205]; // 100 ms of an original, gentle tone
            long sample = 0L;
            while (ringing && !Thread.currentThread().isInterrupted()) {
                for (int i = 0; i < buffer.length; i++) {
                    double t = (sample++) / (double) sampleRate;
                    double envelope = 0.65 + 0.35 * Math.sin(Math.PI * 2.0 * 0.35 * t);
                    double tone = 0.72 * Math.sin(2.0 * Math.PI * 392.0 * t)
                            + 0.28 * Math.sin(2.0 * Math.PI * 523.25 * t);
                    buffer[i] = (short) (Short.MAX_VALUE * 0.40 * envelope * tone);
                }
                try {
                    if (sink.write(buffer, 0, buffer.length) < 0) break;
                } catch (IllegalStateException e) {
                    break;
                }
            }
        }, "SanftweckerAudio");
        audioThread.start();
    }

    private void cleanupAndStop() {
        if (!ringing) {
            stopSelf();
            return;
        }
        ringing = false;
        handler.removeCallbacksAndMessages(null);
        if (audioThread != null) audioThread.interrupt();
        if (track != null) {
            try {
                track.pause();
                track.flush();
                track.release();
            } catch (RuntimeException e) {
                Log.w("Sanftwecker", "Audio cleanup", e);
            }
            track = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        for (int i = 0; i < MAX_WATCH_ALERTS; i++) notifications.cancel(WATCH_BASE_ID + i);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override public void onDestroy() {
        cleanupAndStop();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
