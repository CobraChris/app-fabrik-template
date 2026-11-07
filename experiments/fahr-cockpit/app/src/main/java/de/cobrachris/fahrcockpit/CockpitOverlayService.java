package de.cobrachris.fahrcockpit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * Deliberately tiny and dismissible window. No accessibility injection,
 * screen recording, background route inspection or touch interception outside
 * the visible window. User must start it while this app is foreground.
 */
public final class CockpitOverlayService extends Service {
    public static final String ACTION_START =
        "de.cobrachris.fahrcockpit.action.START";
    public static final String ACTION_STOP =
        "de.cobrachris.fahrcockpit.action.STOP";
    private static final String CHANNEL = "fahr-cockpit-active";
    private WindowManager windows;
    private View overlay;
    private WindowManager.LayoutParams params;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        ensureForeground();
        if (overlay == null) showOverlay();
        return START_NOT_STICKY; // no restart after process death/boot
    }

    private void ensureForeground() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL,
            "FahrCockpit aktiv", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 1,
            new Intent(this, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop = PendingIntent.getService(this, 2,
            new Intent(this, CockpitOverlayService.class).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification note = new Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("FahrCockpit ist aktiv")
            .setContentText("Spotify-Leiste läuft. Tippen zum Öffnen oder Beenden.")
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Beenden", stop)
            .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1001, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else startForeground(1001, note);
    }

    private void showOverlay() {
        windows = (WindowManager) getSystemService(WINDOW_SERVICE);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(5), dp(5), dp(5), dp(5));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(26, 54, 54));
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), Color.rgb(148, 207, 177));
        row.setBackground(bg);
        row.setElevation(dp(8));

        Button drag = button(row, "≡", "FahrCockpit verschieben", null);
        button(row, "⏮", "Vorheriger Titel", MediaControlGateway.Command.PREVIOUS);
        button(row, "−15", "15 Sekunden zurückspulen", MediaControlGateway.Command.REWIND_15);
        button(row, "⏯", "Wiedergabe starten oder pausieren", MediaControlGateway.Command.PLAY_PAUSE);
        button(row, "+15", "15 Sekunden vorspulen", MediaControlGateway.Command.FORWARD_15);
        button(row, "⏭", "Nächster Titel", MediaControlGateway.Command.NEXT);
        Button close = button(row, "×", "FahrCockpit schließen", null);
        close.setOnClickListener(view -> stopSelf());

        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(12);
        params.y = dp(150);
        overlay = row;
        windows.addView(overlay, params);
        drag.setOnTouchListener(new View.OnTouchListener() {
            float startX, startY;
            int initialX, initialY;
            @Override public boolean onTouch(View view, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    startX = event.getRawX(); startY = event.getRawY();
                    initialX = params.x; initialY = params.y;
                    return true;
                }
                if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    params.x = Math.max(0, initialX + Math.round(event.getRawX() - startX));
                    params.y = Math.max(0, initialY + Math.round(event.getRawY() - startY));
                    windows.updateViewLayout(overlay, params);
                    return true;
                }
                return event.getActionMasked() == MotionEvent.ACTION_UP;
            }
        });
    }
    private Button button(LinearLayout row, String text, String description,
                          MediaControlGateway.Command command) {
        Button result = new Button(this);
        result.setText(text);
        result.setTextSize(13);
        result.setAllCaps(false);
        result.setContentDescription(description);
        result.setMinWidth(0);
        result.setMinimumWidth(0);
        result.setMinHeight(0);
        result.setMinimumHeight(0);
        result.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams item = new LinearLayout.LayoutParams(dp(44), dp(48));
        row.addView(result, item);
        if (command != null) result.setOnClickListener(view -> {
            String problem = MediaControlGateway.perform(this, command);
            if (problem != null) Toast.makeText(this, problem, Toast.LENGTH_SHORT).show();
        });
        return result;
    }
    private int dp(int d) { return Math.round(d * getResources().getDisplayMetrics().density); }
    @Override public void onDestroy() {
        if (windows != null && overlay != null) {
            try { windows.removeView(overlay); } catch (IllegalArgumentException ignored) {}
            overlay = null;
        }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
