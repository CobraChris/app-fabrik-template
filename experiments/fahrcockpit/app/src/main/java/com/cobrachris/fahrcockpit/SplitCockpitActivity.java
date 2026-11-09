package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Real Android multi-window participant. The operating system (not our app)
 * controls split ratio and placement of third-party Google Maps.
 *
 * Split-screen is requested on Android 12L+ via LAUNCH_ADJACENT/NEW_TASK.
 * On unsupported Samsung configurations the user can arrange an app pair or
 * return to the already proven overlay. No hidden Accessibility automation.
 */
public final class SplitCockpitActivity extends Activity {
    private static final String MAPS_PACKAGE = "com.google.android.apps.maps";
    private static final String KEY_REQUESTED = "requested_adjacent_maps";
    private final Handler ui = new Handler(Looper.getMainLooper());

    private TextView splitStatus, spotifyStatus, track, currentTime, durationTime;
    private Button play, previous, next, back15, forward15;
    private SeekBar position;
    private boolean requested;
    private boolean scrubbing;
    private boolean pendingOverlayFallback;
    private boolean adjacentLaunchAttempted;
    private boolean fallbackGraceElapsed;
    private long activeDuration = -1L;

    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (track != null) updateMusic();
            if (isInMultiWindowMode() && pendingOverlayFallback) {
                // Late split acceptance must never leave two cockpits visible.
                stopService(new Intent(SplitCockpitActivity.this,
                    OverlayService.class));
                pendingOverlayFallback = false;
            }
            ui.postDelayed(this, 1800L);
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Reentering the native split must remove every existing overlay,
        // including an older service instance started before this activity.
        stopService(new Intent(this, OverlayService.class));
        stopService(new Intent(this, DockedCockpitService.class));
        requested = savedInstanceState != null && savedInstanceState.getBoolean(KEY_REQUESTED);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout root = Ui.column(this);
        root.setPadding(Ui.dp(this, 10), Ui.dp(this, 9),
            Ui.dp(this, 10), Ui.dp(this, 16));
        scroll.addView(root);
        setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(
                android.view.WindowInsets.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        TextView heading = Ui.text(this, "FahrCockpit", 19, Ui.FG, true);
        root.addView(heading);

        splitStatus = Ui.text(this,
            SplitPaneRules.statusText(isInMultiWindowMode()), 12, Ui.MUTED, false);
        root.addView(splitStatus);
        spotifyStatus = Ui.text(this, "Spotify wird gesucht …", 12, Ui.MUTED, false);
        root.addView(spotifyStatus);
        track = Ui.text(this, "–", 14, Ui.FG, true);
        track.setMaxLines(2);
        root.addView(track);

        position = new SeekBar(this);
        position.setMax(TrackProgressRules.MAX);
        position.setEnabled(false);
        root.addView(position);
        LinearLayout times = Ui.row(this);
        currentTime = Ui.text(this, "--:--", 11, Ui.MUTED, false);
        durationTime = Ui.text(this, "--:--", 11, Ui.MUTED, false);
        times.addView(currentTime, new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        times.addView(durationTime);
        root.addView(times);
        position.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                scrubbing = true;
            }
            @Override public void onProgressChanged(
                    SeekBar seekBar, int value, boolean fromUser) {
                if (fromUser) {
                    currentTime.setText(TrackProgressRules.time(
                        TrackProgressRules.targetPosition(value, activeDuration)));
                }
            }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                long target = TrackProgressRules.targetPosition(
                    seekBar.getProgress(), activeDuration);
                scrubbing = false;
                if (target >= 0) spotifyStatus.setText(
                    SpotifyMedia.seekAbsolute(SplitCockpitActivity.this, target));
                ui.postDelayed(() -> {
                    if (track != null) updateMusic();
                }, 650L);
            }
        });

        LinearLayout media = Ui.row(this);
        previous = control("⏮", media);
        play = control("▶", media);
        next = control("⏭", media);
        previous.setOnClickListener(v -> sendMedia(SpotifyMedia.Action.PREVIOUS));
        play.setOnClickListener(v -> sendMedia(SpotifyMedia.Action.TOGGLE));
        next.setOnClickListener(v -> sendMedia(SpotifyMedia.Action.NEXT));
        root.addView(media);

        LinearLayout seek = Ui.row(this);
        back15 = control("−15 s", seek);
        forward15 = control("+15 s", seek);
        back15.setOnClickListener(v -> sendMedia(SpotifyMedia.Action.SEEK_BACK));
        forward15.setOnClickListener(v -> sendMedia(SpotifyMedia.Action.SEEK_FORWARD));
        root.addView(seek);

        Ui.gap(this, root, 7);
        Button maps = Ui.button(this, "Google Maps nebenan anzeigen");
        root.addView(maps);
        maps.setOnClickListener(v -> launchMapsAdjacent());

        Button fuel = Ui.button(this, "⛽ Tankstellen");
        root.addView(fuel);
        fuel.setOnClickListener(v -> startActivity(
            new Intent(this, FuelActivity.class)));

        Button saphe = Ui.button(this, "Saphe öffnen");
        root.addView(saphe);
        saphe.setOnClickListener(v -> spotifyStatus.setText(
            WarningSource.openSaphe(this)
                ? "Saphe geöffnet. Hintergrundstatus nicht geprüft."
                : "Saphe nicht verfügbar."));

        Button overlay = Ui.button(this, "Stattdessen schwebendes Cockpit");
        root.addView(overlay);
        overlay.setOnClickListener(v -> {
            try {
                startForegroundService(new Intent(this, OverlayService.class)
                    .setAction(OverlayService.ACTION_SHOW));
                finish();
            } catch (RuntimeException e) {
                splitStatus.setText("Overlay nicht verfügbar: Freigabe in Einstellungen prüfen.");
            }
        });

        TextView note = Ui.text(this,
            "70 % Maps / 30 % FahrCockpit: Trennlinie einmalig in Samsung Multi Window " +
            "verschieben. Die genaue Aufteilung steuert Android. In Samsung kannst " +
            "du die beiden Apps danach als App-Paar speichern. " +
            "Im Querformat liegen die Fenster nebeneinander, hochkant " +
            "in der Regel übereinander. Nur im Stand bedienen.",
            12, Ui.MUTED, false);
        root.addView(note);
        Button settings = Ui.button(this, "FahrCockpit-Einstellungen");
        root.addView(settings);
        settings.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));

        if (!requested) {
            requested = true;
            ui.postDelayed(this::launchMapsAdjacent, 450L);
        }
        // Early status refresh must not start a fallback during split animation.
        ui.postDelayed(this::updateSplitStatus, 1500L);
    }

    private Button control(String label, LinearLayout row) {
        Button button = Ui.button(this, label);
        button.setMinWidth(0);
        button.setTextSize(16);
        row.addView(button, new LinearLayout.LayoutParams(
            0, Ui.dp(this, 51), 1));
        return button;
    }

    /**
     * Best-effort native system split. Not a custom Google Maps embedding:
     * Google Maps retains its own task, navigation, traffic and account.
     */
    private void launchMapsAdjacent() {
        Intent maps = GoogleMapsLaunch.mapIntent();
        if (!GoogleMapsLaunch.installed(this)) {
            splitStatus.setText("Google Maps ist nicht installiert oder nicht sichtbar.");
            return;
        }
        try {
            // Never start an overlay before Android has had a chance to enter
            // real split-screen. The previous implementation caused a second
            // cockpit to obscure an already successful Samsung split.
            stopService(new Intent(this, OverlayService.class));
            pendingOverlayFallback = false;
            adjacentLaunchAttempted = true;
            fallbackGraceElapsed = false;
            maps.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
            startActivity(maps);
            // Allow Samsung enough time to settle its split transition.
            ui.postDelayed(() -> {
                fallbackGraceElapsed = true;
                reconcileSplitAndOverlay();
            }, 2600L);
            ui.postDelayed(this::reconcileSplitAndOverlay, 4800L);
        } catch (RuntimeException e) {
            // The user can still select the proven overlay in this activity.
            adjacentLaunchAttempted = false;
            splitStatus.setText("Samsung konnte Google Maps nicht nebenan öffnen. " +
                "Bitte App-Paar über die letzten Apps anlegen.");
        }
    }

    private void sendMedia(SpotifyMedia.Action action) {
        spotifyStatus.setText(SpotifyMedia.send(this, action));
        ui.postDelayed(() -> {
            if (track != null) updateMusic();
        }, 400L);
    }

    private void updateMusic() {
        SpotifyMedia.State state = SpotifyMedia.read(this);
        track.setText(state.line);
        play.setText(state.playing ? "⏸" : "▶");
        play.setEnabled(state.connected);
        previous.setEnabled(state.canPrevious);
        next.setEnabled(state.canNext);
        back15.setEnabled(state.canSeek);
        forward15.setEnabled(state.canSeek);
        activeDuration = state.durationMs;
        boolean hasProgress = TrackProgressRules.hasDuration(state.durationMs)
            && state.positionMs >= 0;
        position.setEnabled(hasProgress && state.canSeek);
        position.setVisibility(hasProgress ? View.VISIBLE : View.GONE);
        if (!scrubbing && hasProgress) {
            position.setProgress(TrackProgressRules.sliderPosition(
                state.positionMs, state.durationMs));
            currentTime.setText(TrackProgressRules.time(state.positionMs));
        }
        currentTime.setVisibility(hasProgress ? View.VISIBLE : View.GONE);
        durationTime.setVisibility(hasProgress ? View.VISIBLE : View.GONE);
        if (hasProgress) durationTime.setText(
            TrackProgressRules.time(state.durationMs));
        if (!state.connected) spotifyStatus.setText(
            Permissions.isMediaListenerEnabled(this)
                ? "Spotify öffnen und Musik starten."
                : "Spotify-Medienzugriff in FahrCockpit freigeben.");
        else if (spotifyStatus.getText().toString().startsWith("Spotify "))
            spotifyStatus.setText("Spotify verbunden");
    }

    private void updateSplitStatus() {
        if (splitStatus != null) splitStatus.setText(
            SplitPaneRules.statusText(isInMultiWindowMode()));
    }

    private void reconcileSplitAndOverlay() {
        if (SplitPaneRules.mustRemoveOverlay(isInMultiWindowMode())) {
            // Always remove the service, not only one started by this activity:
            // the user's v6 screenshot showed a stale overlay on a real split.
            stopService(new Intent(this, OverlayService.class));
            pendingOverlayFallback = false;
            updateSplitStatus();
            return;
        }
        if (SplitPaneRules.shouldStartFallback(
                isInMultiWindowMode(), adjacentLaunchAttempted,
                fallbackGraceElapsed, pendingOverlayFallback,
                Settings.canDrawOverlays(this))) {
            try {
                startForegroundService(new Intent(this, OverlayService.class)
                    .setAction(OverlayService.ACTION_SHOW));
                pendingOverlayFallback = true;
            } catch (RuntimeException ignored) {
                // Leave the native controls available, do not claim success.
            }
        }
        if (splitStatus != null) {
            splitStatus.setText(pendingOverlayFallback
                ? "Samsung hat Split nicht bestätigt – schwebendes Cockpit als Ersatz."
                : SplitPaneRules.statusText(false));
        }
    }

    @Override public void onMultiWindowModeChanged(
            boolean inMultiWindowMode, Configuration newConfig) {
        super.onMultiWindowModeChanged(inMultiWindowMode, newConfig);
        reconcileSplitAndOverlay();
    }

    @Override public void onSaveInstanceState(Bundle state) {
        state.putBoolean(KEY_REQUESTED, requested);
        super.onSaveInstanceState(state);
    }

    @Override public void onStart() {
        super.onStart();
        // Reconciliation also runs on reentry from Maps, Spotify, and rotation.
        if (isInMultiWindowMode()) {
            stopService(new Intent(this, OverlayService.class));
            pendingOverlayFallback = false;
        }
        ui.removeCallbacks(refresh);
        ui.post(refresh);
    }

    @Override public void onResume() {
        super.onResume();
        if (isInMultiWindowMode()) {
            stopService(new Intent(this, OverlayService.class));
            pendingOverlayFallback = false;
        }
        ui.post(this::reconcileSplitAndOverlay);
    }

    @Override public void onStop() {
        ui.removeCallbacks(refresh);
        super.onStop();
    }

    @Override public void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
