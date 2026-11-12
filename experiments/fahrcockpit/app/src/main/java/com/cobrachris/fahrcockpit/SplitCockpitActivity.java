package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.app.AlertDialog;
import android.provider.Settings;
import android.widget.EditText;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
 * If Samsung declines the split request, show instructions for Multi Window
 * or an App pair. Never activate an overlay automatically. No Accessibility automation.
 */
public final class SplitCockpitActivity extends Activity {
    static final String EXTRA_NAVIGATION_TARGET = "fahrcockpit_split_navigation_target";
    private static final String KEY_TARGET = "selected_split_target";
    private static final String KEY_REQUESTED = "requested_adjacent_maps";
    private static final String ROUTES_PREFS = "fahrcockpit_manual_routes_v1";
    private final Handler ui = new Handler(Looper.getMainLooper());

    private TextView splitStatus, spotifyStatus, track, currentTime, durationTime, targetLabel;
    private Button play, previous, next, back15, forward15, spotifyAction;
    private String routeTarget;
    private SeekBar position;
    private boolean requested;
    private boolean scrubbing;
    private long activeDuration = -1L;

    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (track != null) updateMusic();
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
        routeTarget = RouteTargetRules.clean(savedInstanceState != null
            ? savedInstanceState.getString(KEY_TARGET)
            : getIntent().getStringExtra(EXTRA_NAVIGATION_TARGET));
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
        targetLabel = Ui.text(this, routeTarget == null
            ? "Google Maps rechts/links · Ziel noch nicht festgelegt"
            : "Google-Navigation zum Ziel: " + routeTarget,
            12, Ui.ACCENT, false);
        targetLabel.setMaxLines(3);
        root.addView(targetLabel);
        Button targetButton = Ui.button(this, "📍 Ziel für Google-Navigation wählen");
        root.addView(targetButton);
        targetButton.setOnClickListener(v -> chooseDrivingDestination());

        spotifyStatus = Ui.text(this, "Spotify wird gesucht …", 12, Ui.MUTED, false);
        root.addView(spotifyStatus);
        spotifyAction = Ui.button(this, "Spotify öffnen und Musik starten");
        root.addView(spotifyAction);
        spotifyAction.setOnClickListener(v -> runSpotifyNextStep());
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
        maps.setOnClickListener(v -> launchMapsAdjacent(routeTarget));
        Button help = Ui.button(this, "70/30 einmalig als Samsung-App-Paar speichern");
        root.addView(help);
        help.setOnClickListener(v -> showSplitHelp());

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
            "Google Maps bietet echte Abbiegenavigation ohne FahrCockpit-Cloud-"
            + "Abrechnung. Für wiederholtes 70/30 das Samsung-App-Paar "
            + "einmalig im Stand speichern. Die Fenstergröße bestimmt Samsung.",
            12, Ui.MUTED, false);
        root.addView(note);
        Button settings = Ui.button(this, "FahrCockpit-Einstellungen");
        root.addView(settings);
        settings.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));

        if (!requested) {
            requested = true;
            ui.postDelayed(() -> launchMapsAdjacent(routeTarget), 450L);
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
    private void launchMapsAdjacent(String target) {
        Intent maps = GoogleMapsLaunch.adjacentDrivingIntent(target);
        if (!GoogleMapsLaunch.installed(this)) {
            splitStatus.setText("Google Maps ist nicht installiert oder nicht sichtbar.");
            return;
        }
        try {
            // Never start an overlay before Android has had a chance to enter
            // real split-screen. The previous implementation caused a second
            // cockpit to obscure an already successful Samsung split.
            stopService(new Intent(this, OverlayService.class));
            stopService(new Intent(this, DockedCockpitService.class));
            // The explicit Google Intent already carries NEW_TASK+LAUNCH_ADJACENT.
            // This only requests an Android split; it cannot force Samsung's UI.
            startActivity(maps);
            targetLabel.setText(target == null
                ? "Google Maps als zweites Fenster angefordert"
                : "Google-Navigation angefordert: " + target);
            // Confirm state only after Samsung has had time to arrange both windows.
            ui.postDelayed(this::updateSplitStatus, 2600L);
            ui.postDelayed(this::updateSplitStatus, 4800L);
        } catch (RuntimeException e) {
            // The user can still select the proven overlay in this activity.
            splitStatus.setText("Samsung konnte Google Maps nicht nebenan öffnen. " +
                "Bitte App-Paar über die letzten Apps anlegen.");
        }
    }

    private void chooseDrivingDestination() {
        LinearLayout content = Ui.column(this);
        content.setPadding(Ui.dp(this, 18), Ui.dp(this, 8),
            Ui.dp(this, 18), Ui.dp(this, 8));
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setTextColor(Ui.FG);
        field.setHintTextColor(Ui.MUTED);
        field.setHint("Adresse, Ort oder Koordinaten");
        field.setText(routeTarget == null ? "" : routeTarget);
        content.addView(field);
        android.content.SharedPreferences prefs =
            getSharedPreferences(ROUTES_PREFS, MODE_PRIVATE);
        for (int i = 0; i < 5; i++) {
            String target = RouteTargetRules.clean(prefs.getString("target_" + i, ""));
            if (target == null) continue;
            Button saved = Ui.button(this, target);
            saved.setMaxLines(1);
            content.addView(saved);
            saved.setOnClickListener(v -> field.setText(target));
        }
        ScrollView dialogScroll = new ScrollView(this);
        dialogScroll.setFillViewport(false);
        dialogScroll.addView(content);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Google-Navigation im Splitscreen")
            .setView(dialogScroll)
            .setNegativeButton("Abbrechen", (d, which) -> {})
            .setPositiveButton("Navigation starten", null)
            .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(
            AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String target = RouteTargetRules.clean(field.getText().toString());
            if (target == null) {
                field.setError("Bitte ein gültiges Ziel angeben (maximal 160 Zeichen).");
                return;
            }
            routeTarget = target;
            dialog.dismiss();
            launchMapsAdjacent(routeTarget);
        }));
        dialog.show();
    }

    private void showSplitHelp() {
        new AlertDialog.Builder(this)
            .setTitle("70/30 dauerhaft ohne Google Cloud")
            .setMessage("Einmalig auf deinem Samsung im Stand einrichten:\n\n"
                + "1. Google Maps und FahrCockpit in der geteilten "
                + "Bildschirmansicht öffnen. Falls Maps Vollbild zeigt: "
                + "Android-Übersicht 'Letzte Apps' öffnen, das FahrCockpit-"
                + "Appsymbol antippen und 'In geteilter Bildschirmansicht "
                + "öffnen' wählen; danach Google Maps als zweite App wählen.\n\n"
                + "2. Die Trennlinie zwischen Google Maps und FahrCockpit "
                + "so verschieben, dass Maps ungefähr 70 % einnimmt. "
                + "Je nach Ausrichtung kann die Anordnung unterschiedlich sein.\n\n"
                + "3. Die Trennlinie antippen und das Symbol zum Hinzufügen "
                + "eines App-Paars (Plus/Stern, je nach One UI) wählen. "
                + "Falls angeboten, das App-Paar zum Startbildschirm oder "
                + "Seiten-Paneel hinzufügen.\n\n"
                + "Danach das gespeicherte App-Paar zum gemeinsamen Start "
                + "verwenden. Samsung verwaltet Position und Teilungsverhältnis; "
                + "FahrCockpit kann 70/30 nicht erzwingen. Für diese Variante "
                + "ist kein FahrCockpit-Google-Cloud-Konto nötig. "
                + "Nur im Stand einrichten.")
            .setPositiveButton("Verstanden", (d, which) -> {})
            .show();
    }

    private void runSpotifyNextStep() {
        if (!Permissions.isMediaListenerEnabled(this)) {
            spotifyStatus.setText("Medienzugriff für FahrCockpit in Android freigeben.");
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } catch (RuntimeException unavailable) {
                spotifyStatus.setText("Android-Einstellungen: Benachrichtigungszugriff "
                    + "für FahrCockpit freigeben.");
            }
            return;
        }
        Intent spotify = getPackageManager().getLaunchIntentForPackage("com.spotify.music");
        if (spotify == null) {
            spotifyStatus.setText("Spotify ist nicht installiert.");
            return;
        }
        try {
            startActivity(spotify);
            spotifyStatus.setText("Spotify: Musik starten und zu FahrCockpit zurückkehren.");
        } catch (RuntimeException unavailable) {
            spotifyStatus.setText("Spotify konnte nicht geöffnet werden.");
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
        boolean granted = Permissions.isMediaListenerEnabled(this);
        String nextStep = SpotifyDisplayRules.nextAction(granted, state.connected);
        spotifyAction.setVisibility(nextStep == null ? View.GONE : View.VISIBLE);
        if (nextStep != null) spotifyAction.setText(nextStep);
        track.setVisibility(state.connected ? View.VISIBLE : View.GONE);
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
        spotifyStatus.setText(state.connected
            ? SpotifyDisplayRules.connectedStatus(state.playing)
            : SpotifyDisplayRules.notConnected(granted));
    }

    private void updateSplitStatus() {
        if (splitStatus != null) splitStatus.setText(
            SplitPaneRules.statusText(isInMultiWindowMode()));
    }

    private void enforceSplitWithoutOverlay() {
        // Even when split mode fails, an overlay must never hide Maps.
        stopService(new Intent(this, OverlayService.class));
        stopService(new Intent(this, DockedCockpitService.class));
        updateSplitStatus();
    }

    @Override public void onMultiWindowModeChanged(
            boolean inMultiWindowMode, Configuration newConfig) {
        super.onMultiWindowModeChanged(inMultiWindowMode, newConfig);
        enforceSplitWithoutOverlay();
    }

    @Override public void onSaveInstanceState(Bundle state) {
        state.putBoolean(KEY_REQUESTED, requested);
        state.putString(KEY_TARGET, routeTarget);
        super.onSaveInstanceState(state);
    }

    @Override public void onStart() {
        super.onStart();
        // Reconciliation also runs on reentry from Maps, Spotify, and rotation.
        enforceSplitWithoutOverlay();
        ui.removeCallbacks(refresh);
        ui.post(refresh);
    }

    @Override public void onResume() {
        super.onResume();
        enforceSplitWithoutOverlay();
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
