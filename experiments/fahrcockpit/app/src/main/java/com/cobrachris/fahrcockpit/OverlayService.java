package com.cobrachris.fahrcockpit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.res.Configuration;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
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
    private ScrollView fullScroll;
    private LinearLayout drivingContent;
    private LinearLayout fullProgress;
    private LinearLayout drivingProgress;
    private SeekBar fullSeek;
    private SeekBar drivingSeek;
    private TextView fullTime;
    private TextView fullDuration;
    private TextView drivingTime;
    private TextView drivingDuration;
    private boolean fullScrubbing;
    private boolean drivingScrubbing;
    private long progressDuration = -1L;
    private boolean automaticLandscape;
    private TextView track;
    private TextView drivingTrack;
    private Button drivingPlay;
    private Button drivingNext;
    private Button driveToggle;
    private CockpitViewRules.Mode viewMode = CockpitViewRules.Mode.FULL;
    private CockpitViewRules.Mode lastExpanded = CockpitViewRules.Mode.FULL;
    private Button play;
    private Button backward;
    private Button forward;
    private Button previous;
    private Button next;
    private Button warningButton;
    private Button warningStopButton;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (panel != null && viewMode != CockpitViewRules.Mode.COLLAPSED) {
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
            widthForState(), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        int displayWidth = getResources().getDisplayMetrics().widthPixels;
        int displayHeight = getResources().getDisplayMetrics().heightPixels;
        params.x = OverlayRules.clampPosition(
            getSharedPreferences(POSITION_PREFS, MODE_PRIVATE).getInt("x", Ui.dp(this, 16)),
            displayWidth, params.width);
        params.y = OverlayRules.clampPosition(
            getSharedPreferences(POSITION_PREFS, MODE_PRIVATE).getInt("y", Ui.dp(this, 120)),
            displayHeight, Ui.dp(this, 65));

        panel = Ui.column(this);
        panel.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 8));
        panel.setBackground(Ui.background(this, Ui.PANEL, 18));
        panel.setElevation(Ui.dp(this, 16));

        LinearLayout bar = Ui.row(this);
        TextView drag = Ui.text(this, "≡ FahrCockpit", 14, Ui.FG, true);
        drag.setSingleLine(true);
        drag.setEllipsize(TextUtils.TruncateAt.END);
        bar.addView(drag, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        driveToggle = small("Fahrt");
        Button minimize = small("−");
        Button close = small("×");
        bar.addView(driveToggle, new LinearLayout.LayoutParams(
            Ui.dp(this, 66), Ui.dp(this, 46)));
        bar.addView(minimize, new LinearLayout.LayoutParams(
            Ui.dp(this, 48), Ui.dp(this, 46)));
        bar.addView(close, new LinearLayout.LayoutParams(
            Ui.dp(this, 48), Ui.dp(this, 46)));
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
                            params.x = OverlayRules.clampPosition(
                                x + Math.round(dx), width, params.width);
                            params.y = OverlayRules.clampPosition(
                                y + Math.round(dy), height,
                                Math.max(panel.getHeight(), Ui.dp(OverlayService.this, 65)));
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
        driveToggle.setOnClickListener(v -> toggleDriveMode());
        minimize.setOnClickListener(v -> toggleCompact());
        close.setOnClickListener(v -> stopSelf());

        content = Ui.column(this);
        track = Ui.text(this, "Spotify wird gesucht …", 12, Ui.MUTED, false);
        track.setMaxLines(2);
        content.addView(track);
        fullSeek = new SeekBar(this);
        fullTime = progressLabel("--:--");
        fullDuration = progressLabel("--:--");
        fullProgress = progressWidget(fullSeek, fullTime, fullDuration);
        content.addView(fullProgress);

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

        // Explicit driving mode: small music-and-maps strip over the navigation,
        // leaving most of Google Maps visible. No automatic mode changes.
        drivingContent = Ui.column(this);
        drivingTrack = Ui.text(this, "Spotify wird gesucht …", 12, Ui.MUTED, false);
        drivingTrack.setSingleLine(true);
        drivingTrack.setEllipsize(TextUtils.TruncateAt.END);
        drivingContent.addView(drivingTrack);
        drivingSeek = new SeekBar(this);
        drivingTime = progressLabel("--:--");
        drivingDuration = progressLabel("--:--");
        drivingProgress = progressWidget(drivingSeek, drivingTime, drivingDuration);
        drivingContent.addView(drivingProgress);
        LinearLayout drivingMedia = Ui.row(this);
        drivingPlay = small("▶");
        drivingNext = small("⏭");
        Button drivingMap = small("Maps");
        for (Button button : new Button[] {drivingPlay, drivingNext, drivingMap}) {
            drivingMedia.addView(button, new LinearLayout.LayoutParams(
                0, Ui.dp(this, 48), 1));
        }
        drivingContent.addView(drivingMedia);
        drivingPlay.setOnClickListener(v -> media(SpotifyMedia.Action.TOGGLE));
        drivingNext.setOnClickListener(v -> media(SpotifyMedia.Action.NEXT));
        drivingMap.setOnClickListener(v -> openGoogleMaps());
        drivingContent.setVisibility(View.GONE);

        LinearLayout tools = Ui.row(this);
        Button fuel = small("⛽ Tanken");
        Button map = small("Maps");
        tools.addView(fuel, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1));
        tools.addView(map, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1));
        content.addView(tools);
        LinearLayout traffic = Ui.row(this);
        Button on = small("Saphe öffnen");
        warningButton = on;
        Button off = small("Blitzer Stop");
        warningStopButton = off;
        traffic.addView(on, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1));
        traffic.addView(off, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1));
        content.addView(traffic);
        map.setOnClickListener(v -> openGoogleMaps());
        on.setOnClickListener(v -> {
            String warning = WarningSource.selected(this);
            if (WarningSource.SAPHE.equals(warning)) {
                toast(WarningSource.openSaphe(this)
                    ? "Saphe Link geöffnet – Warnstatus wird nicht geprüft."
                    : "Saphe Link nicht installiert. Bitte später einrichten.");
            } else if (WarningSource.BLITZER.equals(warning)) {
                BlitzerBridge.SendResult result = BlitzerBridge.send(
                    this, BlitzerContract.Command.START);
                toast(result == BlitzerBridge.SendResult.SENT_UNCONFIRMED
                    ? "Blitzer.de Startbefehl gesendet (nicht bestätigt)"
                    : "Blitzer.de PRO nicht erreichbar.");
            } else {
                toast("Kein Warnsystem ausgewählt.");
            }
        });
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
            if (!WarningSource.BLITZER.equals(WarningSource.selected(this))) return;
            BlitzerBridge.SendResult result = BlitzerBridge.send(
                this, BlitzerContract.Command.STOP);
            toast(result == BlitzerBridge.SendResult.SENT_UNCONFIRMED
                ? "Blitzer.de Beenden-Befehl gesendet (nicht bestätigt)"
                : "Blitzer.de PRO nicht erreichbar.");
        });
        fullScroll = new ScrollView(this);
        fullScroll.setFillViewport(false);
        fullScroll.addView(content);
        panel.addView(fullScroll);
        panel.addView(drivingContent);
        onWarningButtonLabel();
        window.addView(panel, params);
        limitFullContentHeight();
        adaptToOrientation();
        panel.post(this::clampAndUpdatePanel);
        ui.post(refresh);
    }

    private TextView progressLabel(String text) {
        TextView view = Ui.text(this, text, 11, Ui.MUTED, false);
        view.setPadding(0, 0, 0, 0);
        return view;
    }

    private LinearLayout progressWidget(SeekBar bar, TextView elapsed, TextView duration) {
        LinearLayout wrapper = Ui.column(this);
        bar.setMax(TrackProgressRules.MAX);
        bar.setEnabled(false);
        wrapper.addView(bar, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 38)));
        LinearLayout times = Ui.row(this);
        times.addView(elapsed, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        times.addView(duration);
        wrapper.addView(times);
        wrapper.setVisibility(View.GONE); // Only shown with real duration metadata.
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar slider) {
                if (slider == fullSeek) fullScrubbing = true;
                else drivingScrubbing = true;
            }
            @Override public void onProgressChanged(
                    SeekBar slider, int position, boolean fromUser) {
                if (!fromUser) return;
                long target = TrackProgressRules.targetPosition(
                    position, progressDuration);
                elapsed.setText(TrackProgressRules.time(target));
            }
            @Override public void onStopTrackingTouch(SeekBar slider) {
                if (slider == fullSeek) fullScrubbing = false;
                else drivingScrubbing = false;
                long target = TrackProgressRules.targetPosition(
                    slider.getProgress(), progressDuration);
                if (target >= 0L) toast(SpotifyMedia.seekAbsolute(
                    OverlayService.this, target));
                ui.postDelayed(() -> {
                    if (panel != null) updateMusic();
                }, 600L);
            }
        });
        return wrapper;
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

    private void openGoogleMaps() {
        try {
            Intent open = getPackageManager().getLaunchIntentForPackage(
                "com.google.android.apps.maps");
            if (open != null) {
                open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(open);
            } else {
                toast("Google Maps nicht installiert.");
            }
        } catch (RuntimeException e) {
            toast("Google Maps nicht erreichbar.");
        }
    }

    private void onWarningButtonLabel() {
        if (warningButton == null) return;
        String selected = WarningSource.selected(this);
        warningButton.setText(OverlayRules.primaryLabel(selected));
        warningButton.setEnabled(OverlayRules.primaryEnabled(selected));
        if (warningStopButton != null) {
            warningStopButton.setVisibility(OverlayRules.showBlitzerStop(selected)
                ? View.VISIBLE : View.GONE);
            warningStopButton.setEnabled(OverlayRules.showBlitzerStop(selected));
        }
    }

    private void updateMusic() {
        SpotifyMedia.State state = SpotifyMedia.read(this);
        onWarningButtonLabel();
        track.setText(state.line);
        if (drivingTrack != null) drivingTrack.setText(state.line);
        if (drivingPlay != null) {
            drivingPlay.setText(state.playing ? "⏸" : "▶");
            drivingPlay.setEnabled(state.connected);
        }
        if (drivingNext != null) drivingNext.setEnabled(state.canNext);
        progressDuration = state.durationMs;
        updateProgress(fullProgress, fullSeek, fullTime, fullDuration,
            fullScrubbing, state);
        updateProgress(drivingProgress, drivingSeek, drivingTime, drivingDuration,
            drivingScrubbing, state);
        play.setText(state.playing ? "⏸" : "▶");
        play.setEnabled(state.connected);
        previous.setEnabled(state.canPrevious);
        next.setEnabled(state.canNext);
        backward.setEnabled(state.canSeek);
        forward.setEnabled(state.canSeek);
    }

    private void updateProgress(LinearLayout wrapper, SeekBar bar, TextView elapsed,
                                TextView total, boolean dragging, SpotifyMedia.State state) {
        if (wrapper == null || bar == null) return;
        boolean available = TrackProgressRules.hasDuration(state.durationMs)
            && state.positionMs >= 0L;
        wrapper.setVisibility(available ? View.VISIBLE : View.GONE);
        if (!available) return;
        bar.setEnabled(state.canSeek);
        if (!dragging) {
            bar.setProgress(TrackProgressRules.sliderPosition(
                state.positionMs, state.durationMs));
            elapsed.setText(TrackProgressRules.time(state.positionMs));
        }
        total.setText(TrackProgressRules.time(state.durationMs));
    }

    private void toggleCompact() {
        if (viewMode == CockpitViewRules.Mode.COLLAPSED) {
            applyMode(lastExpanded);
        } else {
            lastExpanded = viewMode;
            applyMode(CockpitViewRules.Mode.COLLAPSED);
        }
    }

    private void toggleDriveMode() {
        automaticLandscape = false; // A manual selection overrides auto layout.
        CockpitViewRules.Mode next = CockpitViewRules.driveToggle(viewMode);
        lastExpanded = next;
        applyMode(next);
    }

    private void applyMode(CockpitViewRules.Mode next) {
        viewMode = next;
        fullScroll.setVisibility(next == CockpitViewRules.Mode.FULL ? View.VISIBLE : View.GONE);
        drivingContent.setVisibility(
            next == CockpitViewRules.Mode.DRIVING ? View.VISIBLE : View.GONE);
        driveToggle.setText(CockpitViewRules.driveButtonLabel(next));
        params.width = widthForState();
        limitFullContentHeight();
        clampAndUpdatePanel();
        if (panel != null) panel.post(this::clampAndUpdatePanel);
        if (next != CockpitViewRules.Mode.COLLAPSED) updateMusic();
    }

    private int widthForState() {
        int displayWidth = getResources().getDisplayMetrics().widthPixels;
        return Math.min(Ui.dp(this,
                viewMode == CockpitViewRules.Mode.COLLAPSED ? 230 : 310),
            Math.max(1, displayWidth - Ui.dp(this, 16)));
    }

    private boolean isLandscape() {
        return getResources().getDisplayMetrics().widthPixels >
            getResources().getDisplayMetrics().heightPixels;
    }

    /** Scrollable full controls avoid falling off a short landscape display. */
    private void limitFullContentHeight() {
        if (fullScroll == null) return;
        int viewport = getResources().getDisplayMetrics().heightPixels;
        int maxHeight = Math.max(Ui.dp(this, 100), viewport - Ui.dp(this, 104));
        fullScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            isLandscape() ? maxHeight : LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    /** Switch to driving controls on landscape; restore full on portrait. */
    private void adaptToOrientation() {
        boolean enabled = getSharedPreferences(CockpitViewRules.DISPLAY_PREFS,
            MODE_PRIVATE).getBoolean(CockpitViewRules.KEY_AUTO_DRIVING, true);
        if (CockpitViewRules.shouldAutoDrive(isLandscape(), enabled, viewMode)) {
            automaticLandscape = true;
            lastExpanded = CockpitViewRules.Mode.DRIVING;
            applyMode(CockpitViewRules.Mode.DRIVING);
        } else if (automaticLandscape && (!isLandscape() || !enabled)) {
            automaticLandscape = false;
            if (viewMode == CockpitViewRules.Mode.DRIVING) {
                lastExpanded = CockpitViewRules.Mode.FULL;
                applyMode(CockpitViewRules.Mode.FULL);
            } else if (viewMode == CockpitViewRules.Mode.COLLAPSED
                    && lastExpanded == CockpitViewRules.Mode.DRIVING) {
                lastExpanded = CockpitViewRules.Mode.FULL;
            }
        }
    }

    private void clampAndUpdatePanel() {
        if (window == null || panel == null || params == null) return;
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        params.x = OverlayRules.clampPosition(params.x, width, params.width);
        params.y = OverlayRules.clampPosition(params.y, height,
            Math.max(panel.getHeight(), Ui.dp(this, 65)));
        try {
            window.updateViewLayout(panel, params);
        } catch (IllegalArgumentException ignored) {
            // The service may have been closed while a layout pass was pending.
        }
    }

    @Override public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        if (panel == null || params == null) return;
        limitFullContentHeight();
        adaptToOrientation();
        params.width = widthForState();
        clampAndUpdatePanel();
        panel.post(this::clampAndUpdatePanel);
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
