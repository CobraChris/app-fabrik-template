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
 * Audible alarm plus separate Samsung-mirrorable notifications.
 * No direct watch access. This does not guarantee watch vibration during DND.
 */
public final class RingingService extends Service {
    public static final String START = "com.cobrachris.sanftwecker.START";
    public static final String STOP = "com.cobrachris.sanftwecker.STOP";
    public static final String SNOOZE = "com.cobrachris.sanftwecker.SNOOZE";
    public static final String WATCH_CHANNEL = "sanftwecker_watch_v2";
    private static final String ACTIVE_CHANNEL = "sanftwecker_active_v3";
    private static final int ONGOING_ID = 601;
    private static final int WATCH_BASE_ID = 7200;
    private static final long MAX_RING = 30L * 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private NotificationManager manager;
    private AudioTrack audio;
    private Thread writer;
    private PowerManager.WakeLock wakeLock;
    private volatile boolean active;
    private long startedAt;
    private int pulseCount;
    private int ringId = 1;
    private AlarmProfile profile;
    private boolean toneOnlyPreview;

    private final Runnable gainTick = new Runnable() {
        @Override public void run() {
            if (!active || audio == null) return;
            long elapsed = SystemClock.elapsedRealtime() - startedAt;
            float gain = GentleGain.level(elapsed, profile.delaySeconds,
                    profile.rampEnabled ? profile.rampSeconds : 30,
                    profile.maxVolume);
            try { audio.setVolume(gain); }
            catch (IllegalStateException e) { Log.w("Sanftwecker", "Audio gain update", e); }
            if (elapsed < MAX_RING) handler.postDelayed(this, 200L);
        }
    };

    private final Runnable pulse = new Runnable() {
        @Override public void run() {
            if (!active || profile.watchPulses == 0 ||
                    pulseCount >= profile.watchPulses) return;
            sendWatchNotification(pulseCount++);
            if (pulseCount < profile.watchPulses) {
                handler.postDelayed(this, profile.watchIntervalSeconds * 1000L);
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        ensureChannels();
    }

    public static void ensureChannels(android.content.Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel running = new NotificationChannel(
                ACTIVE_CHANNEL, "Aktiver Alarm", NotificationManager.IMPORTANCE_HIGH);
        running.setSound(null, null);
        nm.createNotificationChannel(running);
        NotificationChannel watch = new NotificationChannel(
                WATCH_CHANNEL, "Vibration an Galaxy Watch", NotificationManager.IMPORTANCE_HIGH);
        watch.setDescription("Alarmbenachrichtigungen zur Weiterleitung durch Galaxy Wearable");
        watch.enableVibration(true);
        watch.setVibrationPattern(new long[]{0L, 300L, 160L, 300L});
        watch.setSound(null, null);
        nm.createNotificationChannel(watch);
    }

    private void ensureChannels() { ensureChannels(this); }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (STOP.equals(action) || SNOOZE.equals(action)) {
            int id = ringId;
            int minutes = profile == null ? 5 : profile.snoozeMinutes;
            if (SNOOZE.equals(action) && active) {
                try { AlarmScheduler.snooze(this, id, minutes); }
                catch (RuntimeException e) {
                    AlarmRepository.diagnostic(this, "FEHLER: Schlummeralarm konnte nicht geplant werden");
                    Log.e("Sanftwecker", "Snooze failed", e);
                }
            }
            if (STOP.equals(action)) AlarmScheduler.cancelSnooze(this);
            AlarmRepository.diagnostic(this,
                    SNOOZE.equals(action) ? "Schlummern aktiv" : "Alarm gestoppt");
            stopPlaying();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!START.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        int requestedId = intent.getIntExtra(AlarmScheduler.EXTRA_ID, 1);
        if (requestedId < 1 || requestedId > AlarmProfile.MAX_ALARMS ||
                AlarmRepository.get(this,requestedId)==null) {
            stopSelf(); return START_NOT_STICKY;
        }
        if (active) {
            // An overlapping alarm replaces the earlier ringtone, not a second audio stream.
            stopPlaying();
        }
        ringId = requestedId;
        profile = AlarmRepository.get(this, ringId);
        toneOnlyPreview = intent.getBooleanExtra("tone_only_preview", false);
        final long ringDuration = toneOnlyPreview ? 15_000L :
                profile.autoStopMinutes * 60_000L;
        try {
            startForegroundNotification();
            active = true;
            startedAt = SystemClock.elapsedRealtime();
            pulseCount = 0;
            try {
                acquireWakeLock();
            } catch (RuntimeException e) {
                Log.w("Sanftwecker", "WakeLock unavailable; continue with active service", e);
            }
            // Even if audio hardware fails, keep the watch alerts and stop/snooze controls.
            try {
                beginAudio();
                handler.post(gainTick);
                AlarmRepository.diagnostic(this, "Weckton gestartet: " + profile.name);
            } catch (RuntimeException audioError) {
                Log.e("Sanftwecker", "Audio unavailable, watch alerts still active", audioError);
                AlarmRepository.diagnostic(this, "WARNUNG: Audio fehlgeschlagen; Watch-Impulse laufen");
            }
            if (!toneOnlyPreview) handler.post(pulse);
            handler.postDelayed(() -> {
                AlarmRepository.diagnostic(this, "Alarm automatisch beendet");
                stopPlaying();
                stopSelf();
            }, ringDuration);
        } catch (RuntimeException e) {
            Log.e("Sanftwecker", "Could not launch foreground alarm", e);
            AlarmRepository.diagnostic(this, "FEHLER: Hintergrundalarm konnte nicht starten");
            stopPlaying();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void acquireWakeLock() {
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        if (power != null) {
            wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sanftwecker:Ringing");
            wakeLock.acquire(MAX_RING + 5_000L);
        }
    }

    private PendingIntent serviceAction(String action, int id) {
        return PendingIntent.getService(this, id,
                new Intent(this, RingingService.class).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent openApp() {
        Intent open = new Intent(this, AlarmActivity.class)
                .putExtra(AlarmScheduler.EXTRA_ID, ringId)
                .putExtra("from_alarm", true);
        return PendingIntent.getActivity(this, 1600 + ringId, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification.Action action(String label, String type, int icon, int id) {
        return new Notification.Action.Builder(icon, label, serviceAction(type, id)).build();
    }

    private void startForegroundNotification() {
        Notification running = new Notification.Builder(this, ACTIVE_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(profile.name + " klingelt")
                .setContentText("Sanfter Ton • Stoppen oder " + profile.snoozeMinutes + " Min. schlummern")
                .setContentIntent(openApp())
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setFullScreenIntent(openApp(), true)
                .setLocalOnly(true)
                .addAction(action("Stoppen", STOP, android.R.drawable.ic_media_pause, 612))
                .addAction(action("Schlummern", SNOOZE, android.R.drawable.ic_media_play, 613))
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(ONGOING_ID, running,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(ONGOING_ID, running);
        }
    }

    private void sendWatchNotification(int index) {
        if (manager == null || !manager.areNotificationsEnabled()) return;
        Notification watch = new Notification.Builder(this, WATCH_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Sanftwecker: " + profile.name)
                .setContentText("Aufwachen – Erinnerung " + (index + 1)
                        + " von " + profile.watchPulses)
                // Same category as the successful standalone notification proof of concept.
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(openApp())
                .addAction(action("Stoppen", STOP, android.R.drawable.ic_media_pause, 612))
                .addAction(action("Schlummern", SNOOZE, android.R.drawable.ic_media_play, 613))
                .setAutoCancel(true)
                .build();
        try {
            manager.notify(WATCH_BASE_ID + index, watch);
            AlarmRepository.diagnostic(this, "Watch-Benachrichtigung " + (index + 1) +
                    " von " + profile.watchPulses + " gesendet (Empfang unbekannt)");
        } catch (SecurityException e) {
            Log.e("Sanftwecker", "Notification permission missing", e);
        }
    }

    private void beginAudio() {
        final int rate = 22050;
        int minBytes = AudioTrack.getMinBufferSize(rate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minBytes <= 0) throw new IllegalStateException("Audio device unavailable");
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        AudioFormat format = new AudioFormat.Builder()
                .setSampleRate(rate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
        audio = new AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(Math.max(minBytes, rate / 5 * 2))
                .setTransferMode(AudioTrack.MODE_STREAM).build();
        if (audio.getState() != AudioTrack.STATE_INITIALIZED) {
            throw new IllegalStateException("AudioTrack was not initialized");
        }
        audio.setVolume(0f);
        audio.play();
        AudioTrack sink = audio;
        int tone = profile.tone;
        writer = new Thread(() -> {
            short[] buffer = new short[2205];
            long sampleIndex = 0;
            while (active && !Thread.currentThread().isInterrupted()) {
                for (int i = 0; i < buffer.length; i++, sampleIndex++) {
                    double t = sampleIndex / (double) rate;
                    double oscillation;
                    if (tone == 1) {
                        oscillation = Math.sin(2 * Math.PI * 392.0 * t);
                    } else if (tone == 2) {
                        oscillation = 0.60 * Math.sin(2 * Math.PI * 262.0 * t)
                                + 0.40 * Math.sin(2 * Math.PI * 330.0 * t);
                    } else {
                        oscillation = 0.65 * Math.sin(2 * Math.PI * 392.0 * t)
                                + 0.35 * Math.sin(2 * Math.PI * 523.25 * t);
                    }
                    // Slowly breathe to avoid a sharp onset, but preserve a smooth continuous waveform.
                    double breathe = 0.75 + 0.25 * Math.cos(2 * Math.PI * 0.24 * t);
                    buffer[i] = (short) (Short.MAX_VALUE * 0.36 * breathe * oscillation);
                }
                try { if (sink.write(buffer, 0, buffer.length) < 0) break; }
                catch (IllegalStateException e) { break; }
            }
        }, "SanftweckerTone");
        writer.start();
    }

    private void stopPlaying() {
        active = false;
        handler.removeCallbacksAndMessages(null);
        AudioTrack old = audio;
        audio = null;
        if (old != null) {
            try { old.pause(); old.flush(); }
            catch (RuntimeException e) { Log.w("Sanftwecker", "Stopping audio", e); }
        }
        if (writer != null) {
            writer.interrupt();
            try { writer.join(300L); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            writer = null;
        }
        if (old != null) {
            try { old.release(); }
            catch (RuntimeException e) { Log.w("Sanftwecker", "Audio release", e); }
        }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = null;
        if (manager != null) for (int i = 0; i < 12; i++) manager.cancel(WATCH_BASE_ID + i);
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    @Override public void onDestroy() {
        stopPlaying();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
