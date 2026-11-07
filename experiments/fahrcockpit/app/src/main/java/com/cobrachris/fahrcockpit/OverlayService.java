package com.cobrachris.fahrcockpit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * User-started system overlay, independent from the underlying Google Maps UI.
 * All foreground-service starts originate from the visible MainActivity.
 */
public final class OverlayService extends Service {
    static final String ACTION_SHOW = "com.cobrachris.fahrcockpit.SHOW";
    static final String ACTION_HIDE = "com.cobrachris.fahrcockpit.HIDE";
    private static final String CHANNEL_ID = "fahrcockpit_overlay_v1";
    private static final int NOTIFICATION_ID = 5101;
    private static final String POSITION_PREFS = "cockpit_position_v1";

    private WindowManager window;
    private WindowManager.LayoutParams params;
    private LinearLayout panel;
    private LinearLayout content;
    private TextView track;
    private Button play;
    private Button backward;
    private Button forward;
    private Button previous;
    private Button next;
    private boolean minimized;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (panel != null && !minimized) {
                updateMusic();
            }
            if (panel != null) ui.postDelayed(this, 2500L);
        }
    };

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        window = (WindowManager) getSystemService(WINDOW_SERVICE);
        NotificationManager notifications = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        if (notifications != null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "FahrCockpit Cockpit", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Aktive schwebende FahrCockpit-Steuerleiste");
            notifications.createNotificationChannel(channel);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_HIDE.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "FahrCockpit benötigt die Overlay-Freigabe.",
                Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            startForeground(NOTIFICATION_ID, notification());
            if (panel == null) attach();
        } catch (RuntimeException error) {
            Toast.makeText(this, "Overlay konnte nicht gestartet werden. Berechtigungen prüfen.",
                Toast.LENGTH_LONG).show();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private Notification notification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openIntent = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent close = new Intent(this, OverlayService.class).setAction(ACTION_HIDE);
        PendingIntent closeIntent = PendingIntent.getService(this, 1, close,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("FahrCockpit")
            .setContentText("Schwebende Mediensteuerung aktiv")
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                "Cockpit schließen", closeIntent)
            .setOngoing(true)
            .build();
    }

    private void attach() {
        params = new WindowManager.LayoutParams(
            Ui.dp(this, 310), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = getSharedPreferences(POSITION_PREFS, MODE_PRIVATE).getInt("x", Ui.dp(this, 16));
        params.y = getSharedPreferences(POSITION_PREFS, MODE_PRIVATE).getInt("y", Ui.dp(this, 120));

        panel = Ui.column(this);
        panel.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 8));
        panel.setBackground(Ui.background(this, Ui.PANEL, 18));
        panel.setElevation(Ui.dp(this, 16));

        LinearLayout bar = Ui.row(this);
        TextView drag = Ui.text(this, "≡  FahrCockpit", 15, Ui.FG, true);
        bar.addView(drag, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button minimize = small("−");
        Button close = small("×");
        bar.addView(minimize);
        bar.addView(close);
        panel.addView(bar);
        drag.setOnTouchListener(new View.OnTouchListener() {
            private int x, y;
            private float rawX, rawY;
            private boolean moved;
            @Override public boolean onTouch(View view, MotionEvent motion) {
                switch (motion.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        x = params.x; y = params.y;
                        rawX = motion.getRawX(); rawY = motion.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = motion.getRawX() - rawX;
                        float dy = motion.getRawY() - rawY;
                        if (Math.abs(dx) > Ui.dp(OverlayService.this, 5)
                                || Math.abs(dy) > Ui.dp(OverlayService.this, 5)) moved = true;
                        if (moved) {
                            int width = getResources().getDisplayMetrics().widthPixels;
                            int height = getResources().getDisplayMetrics().heightPixels;
                            params.x = Math.max(0, Math.min(width - Ui.dp(OverlayService.this, 75),
                                    x + Math.round(dx)));
                            params.y = Math.max(0, Math.min(height - Ui.dp(OverlayService.this, 65),
                                    y + Math.round(dy)));
                            window.updateViewLayout(panel, params);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) toggleCompact();
                        getSharedPreferences(POSITION_PREFS, MODE_PRIVATE).edit()
                            .putInt("x", params.x).putInt("y", params.y).apply();
                        return true;
                    default:
                        return true;
                }
            }
        });
        minimize.setOnClickListener(v -> toggleCompact());
        close.setOnClickListener(v -> stopSelf());

        content = Ui.column(this);
        track = Ui.text(this, "Spotify wird gesucht …", 12, Ui.MUTED, false);
        track.setMaxLines(2);
        content.addView(track);

        LinearLayout media = Ui.row(this);
        previous = small("⏮");
        backward = small("−15");
        play = small("▶");
        forward = small("+15");
        next = small("⏭");
        for (Button button : new Button[] {previous, backward, play, forward, next}) {
            media.addView(button, new LinearLayout.LayoutParams(
                0, Ui.dp(this, 48), 1));
        }
        content.addView(media);
        previous.setOnClickListener(v -> media(SpotifyMedia.Action.PREVIOUS));
        backward.setOnClickListener(v -> media(SpotifyMedia.Action.SEEK_BACK));
        play.setOnClickListener(v -> media(SpotifyMedia.Action.TOGGLE));
        forward.setOnClickListener(v -> media(SpotifyMedia.Action.SEEK_FORWARD));
        next.setOnClickListener(v -> media(SpotifyMedia.Action.NEXT));

        LinearLayout tools = Ui.row(this);
        Button fuel = small("⛽ Tanken");
        Button off = small("Blitzer AUS");
        tools.addView(fuel, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1));
        tools.addView(off, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1));
        content.addView(tools);
        fuel.setOnClickListener(v -> {
            try {
                Intent open = new Intent(this, FuelActivity.class);
                open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(open);
            } catch (RuntimeException e) {
                toast("Tankstellen-Ansicht konnte nicht geöffnet werden.");
            }
        });
        off.setOnClickListener(v -> {
            BlitzerBridge.SendResult result = BlitzerBridge.send(
                this, BlitzerContract.Command.STOP);
            toast(result == BlitzerBridge.SendResult.SENT_UNCONFIRMED
                ? "Blitzer.de Beenden-Befehl gesendet (nicht bestätigt)"
                : "Blitzer.de PRO nicht erreichbar.");
        });
        panel.addView(content);
        window.addView(panel, params);
        ui.post(refresh);
    }

    private Button small(String label) {
        Button button = Ui.button(this, label);
        button.setTextSize(12);
        button.setMinWidth(0);
        button.setPadding(0, 0, 0, 0);
        return button;
    }

    private void media(SpotifyMedia.Action action) {
        toast(SpotifyMedia.send(this, action));
        updateMusic();
    }

    private void updateMusic() {
        SpotifyMedia.State state = SpotifyMedia.read(this);
        track.setText(state.line);
        play.setText(state.playing ? "⏸" : "▶");
        play.setEnabled(state.connected);
        previous.setEnabled(state.canPrevious);
        next.setEnabled(state.canNext);
        backward.setEnabled(state.canSeek);
        forward.setEnabled(state.canSeek);
    }

    private void toggleCompact() {
        minimized = !minimized;
        content.setVisibility(minimized ? View.GONE : View.VISIBLE);
        params.width = Ui.dp(this, minimized ? 190 : 310);
        if (panel != null) window.updateViewLayout(panel, params);
        if (!minimized) updateMusic();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override public void onDestroy() {
        ui.removeCallbacks(refresh);
        if (panel != null && window != null) {
            try { window.removeView(panel); } catch (IllegalArgumentException ignored) { }
        }
        panel = null;
        super.onDestroy();
    }
}
