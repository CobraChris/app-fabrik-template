package com.cobrachris.fahrcockpit;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Permanent 70/30 single-Activity layout. Does not use Samsung Multi Window.
 * An optional NavigationEngine supplies the Google NavigationView. The default
 * signed build has NO SDK, credential, Cloud Billing or hidden route requests.
 */
public final class IntegratedNavigationActivity extends Activity {
    static final String EXTRA_TARGET = "integrated_navigation_destination";
    static final String EXTRA_FORCE_OSM_TEST = "integrated_force_osm_test";
    private static final int LOCATION_REQUEST = 420;
    private static final String STATE_TARGET = "target";
    private static final String ROUTES_PREFS = "fahrcockpit_manual_routes_v1";
    private final Handler handler = new Handler(Looper.getMainLooper());

    private NavigationEngine engine;
    private TextView status, spotifyState, track, elapsed, duration;
    private EditText destination;
    private SeekBar seek;
    private Button previous, play, next, backward, forward, spotifySetup, stopButton;
    private boolean touchingSeek;
    private String chosenTarget;
    private long totalDuration = -1;

    private final Runnable musicRefresh = new Runnable() {
        @Override public void run() {
            if (spotifyState != null) updateMusic();
            handler.postDelayed(this, 1800L);
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        // Android 15+ draws edge-to-edge; handle system bars and IME explicitly.
        getWindow().setDecorFitsSystemWindows(false);
        stopService(new Intent(this, OverlayService.class));
        stopService(new Intent(this, DockedCockpitService.class));

        chosenTarget = RouteTargetRules.clean(savedInstanceState != null
            ? savedInstanceState.getString(STATE_TARGET)
            : getIntent().getStringExtra(EXTRA_TARGET));
        engine = loadEngine();

        boolean landscape = getResources().getConfiguration().orientation
            == Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(landscape
            ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        FrameLayout mapPane = new FrameLayout(this);
        mapPane.setBackgroundColor(0xff173139);
        boolean spotifyLeft = CockpitPanePreferences.spotifyLeft(this);
        LinearLayout.LayoutParams mapParams = landscape
            ? new LinearLayout.LayoutParams(0, -1,
                IntegratedNavigationRules.MAP_WEIGHT)
            : new LinearLayout.LayoutParams(-1, 0,
                CockpitLayoutRules.mapWeight(false));
        // Add the map and Spotify panes in the chosen order below.
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.PANEL);
        LinearLayout.LayoutParams controlParams = landscape
            ? new LinearLayout.LayoutParams(0, -1,
                IntegratedNavigationRules.CONTROLS_WEIGHT)
            : new LinearLayout.LayoutParams(-1, 0,
                CockpitLayoutRules.controlsWeight(false));
        if (landscape && spotifyLeft) {
            root.addView(scroll, controlParams);
            root.addView(mapPane, mapParams);
        } else {
            root.addView(mapPane, mapParams);
            root.addView(scroll, controlParams);
        }

        LinearLayout panel = Ui.column(this);
        panel.setPadding(Ui.dp(this, landscape ? 8 : 12),
            Ui.dp(this, landscape ? 2 : 6),
            Ui.dp(this, landscape ? 8 : 12), Ui.dp(this, 20));
        scroll.addView(panel);

        if (engine != null && engine.configured()) {
            View navigation = engine.createNavigationView(savedInstanceState,
                new NavigationEngine.Status() {
                    @Override public void onStatus(String message) {
                        runOnUiThread(() -> showStatus(message));
                    }
                    @Override public void onGoogleFailure(
                            NavigationProviderPolicy.Failure reason) {
                        runOnUiThread(() -> showProviderFailure(reason));
                    }
                });
            mapPane.addView(navigation, new FrameLayout.LayoutParams(-1, -1));
        } else {
            TextView placeholder = Ui.text(this, BuildConfig.GOOGLE_ONLY
                ? "Google Navigation SDK noch nicht eingerichtet. "
                    + "Bitte den lokalen Schlüssel verwalten."
                : "Google Navigation SDK nicht verfügbar\n\n"
                    + "Diese Fläche ist keine Abbiegenavigation. "
                    + "Die kostenlose Google-Maps-App kann separat "
                    + "über das alte Cockpit geöffnet werden.",
                16, Ui.FG, true);
            placeholder.setGravity(Gravity.CENTER);
            placeholder.setPadding(Ui.dp(this, 24), 0, Ui.dp(this, 24), 0);
            mapPane.addView(placeholder, new FrameLayout.LayoutParams(-1, -1));
        }

        // State the actual provider prominently: preview packages NEVER
        // contain Google SDK. Google remains the shipping product target.
        panel.addView(Ui.text(this, BuildConfig.GOOGLE_ONLY
            ? "FahrCockpit · Google Navigation"
            : (BuildConfig.VISUAL_PREVIEW
                ? "FahrCockpit v21 · OSM-Sichttest"
                : "FahrCockpit · Google-Navigation"), 17, Ui.FG, true));
        if (BuildConfig.VISUAL_PREVIEW) {
            panel.addView(Ui.text(this,
                "Dies ist NUR die separat installierte OSM-Kartenvorschau. "
                + "Die fest integrierte Google-Navigation bleibt das Hauptziel "
                + "und ist ausschließlich für die noch nicht veröffentlichte "
                + "Original-App mit Google Navigation SDK vorgesehen. "
                + "Hier ist kein Google-SDK enthalten.",
                13, Ui.WARN, true));
        }
        Button sideToggle = Ui.button(this,
            landscape ? (spotifyLeft ? "⇄ Spotify rechts" : "⇄ Spotify links")
                : CockpitPanePreferences.toggleLabel(spotifyLeft, landscape));
        sideToggle.setTextSize(landscape ? 12 : 14);
        sideToggle.setContentDescription(
            CockpitPanePreferences.toggleLabel(spotifyLeft, landscape));
        sideToggle.setEnabled(landscape);
        // The greyed-out horizontal control wasted portrait screen height.
        sideToggle.setVisibility(landscape ? View.VISIBLE : View.GONE);
        panel.addView(sideToggle);
        sideToggle.setOnClickListener(v -> {
            // Layout reparenting, not Activity recreation: preserve the active
            // navigation engine, the route state and Spotify playback.
            boolean newLeft = !CockpitPanePreferences.spotifyLeft(this);
            if (!CockpitPanePreferences.saveSpotifyLeft(this, newLeft)) {
                showStatus("Spotify-Seite konnte nicht gespeichert werden.");
                return;
            }
            if (landscape) {
                root.removeView(mapPane);
                root.removeView(scroll);
                if (newLeft) {
                    root.addView(scroll, controlParams);
                    root.addView(mapPane, mapParams);
                } else {
                    root.addView(mapPane, mapParams);
                    root.addView(scroll, controlParams);
                }
            }
            sideToggle.setText(landscape
                ? (newLeft ? "⇄ Spotify rechts" : "⇄ Spotify links")
                : CockpitPanePreferences.toggleLabel(newLeft, landscape));
            sideToggle.setContentDescription(
                CockpitPanePreferences.toggleLabel(newLeft, landscape));
        });
        status = Ui.text(this, isOsmPreview()
            ? "OSM-Abbiegeführung (Entwicklungstest, nicht für reale Fahrten)"
            : (engine != null && engine.configured()
                ? "Google Navigation SDK wird vorbereitet …"
                : "Vorschau · Navigation SDK nicht aktiviert · keine Gebühren"),
            12, Ui.WARN, false);
        status.setMaxLines(3);
        panel.addView(status);

        // Route creation is intentionally collapsed during driving; Spotify
        // stays visible in the limited 30 % panel without manual searching.
        LinearLayout routePanel = Ui.column(this);
        routePanel.setVisibility(View.GONE);
        destination = new EditText(this);
        destination.setSingleLine(true);
        // Keep Samsung's keyboard inline in landscape, not in extract mode.
        destination.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            | android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        destination.setTextColor(Ui.FG);
        destination.setHintTextColor(Ui.MUTED);
        destination.setTextSize(14);
        destination.setHint("Ziel, Adresse oder Koordinaten");
        destination.setText(chosenTarget == null ? "" : chosenTarget);
        routePanel.addView(destination);
        destination.setOnFocusChangeListener((view, focused) -> {
            if (focused && routePanel.getVisibility() == View.VISIBLE) {
                scroll.post(() -> destination.requestRectangleOnScreen(
                    new android.graphics.Rect(0, 0,
                        destination.getWidth(), destination.getHeight()), true));
            }
        });

        Button remembered = Ui.button(this, "Gespeicherte Ziele wählen");
        routePanel.addView(remembered);
        remembered.setOnClickListener(v -> showSavedTargets());

        Button route = Ui.button(this, isOsmPreview()
            ? "OSM-Abbiegeführung zum Testen planen"
            : "Route in dieser App berechnen");
        routePanel.addView(route);
        route.setEnabled(engine != null && engine.configured());
        route.setOnClickListener(v -> resolveAndConfirmRoute());
        stopButton = Ui.button(this, "Navigation beenden");
        routePanel.addView(stopButton);
        stopButton.setEnabled(engine != null && engine.configured());
        stopButton.setOnClickListener(v -> {
            if (engine == null) return;
            engine.stopGuidanceAndClearRoute();
            showStatus("Navigation beendet · Route gelöscht.");
        });

        // Keep the free navigation visible even when the route form is collapsed.
        Button fallback = Ui.button(this,
            "Google Maps separat öffnen (Samsung-Splitscreen)");
        fallback.setOnClickListener(v -> {
            String text = RouteTargetRules.clean(destination.getText().toString());
            Intent intent = new Intent(this, SplitCockpitActivity.class);
            if (text != null)
                intent.putExtra(SplitCockpitActivity.EXTRA_NAVIGATION_TARGET, text);
            startActivity(intent);
        });

        panel.addView(Ui.text(this, "Spotify", 16, Ui.FG, true));
        spotifyState = Ui.text(this, "Spotify-Status wird geprüft …",
            12, Ui.MUTED, false);
        panel.addView(spotifyState);
        spotifySetup = Ui.button(this, "Spotify öffnen und Musik starten");
        panel.addView(spotifySetup);
        spotifySetup.setOnClickListener(v -> openSpotifyOrPermissions());
        track = Ui.text(this, "", 13, Ui.FG, true);
        track.setMaxLines(2);
        panel.addView(track);

        seek = new SeekBar(this);
        seek.setMax(TrackProgressRules.MAX);
        panel.addView(seek);
        LinearLayout timestamps = Ui.row(this);
        elapsed = Ui.text(this, "0:00", 11, Ui.MUTED, false);
        duration = Ui.text(this, "0:00", 11, Ui.MUTED, false);
        timestamps.addView(elapsed, new LinearLayout.LayoutParams(0, -2, 1));
        timestamps.addView(duration);
        panel.addView(timestamps);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar bar) {
                touchingSeek = true;
            }
            @Override public void onProgressChanged(SeekBar bar, int val,
                    boolean fromUser) {
                if (fromUser && totalDuration > 0)
                    elapsed.setText(TrackProgressRules.time(
                        TrackProgressRules.targetPosition(val, totalDuration)));
            }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                touchingSeek = false;
                if (totalDuration > 0) SpotifyMedia.seekAbsolute(
                    IntegratedNavigationActivity.this,
                    TrackProgressRules.targetPosition(bar.getProgress(),
                        totalDuration));
            }
        });

        LinearLayout media = Ui.row(this);
        previous = Ui.button(this, "⏮");
        play = Ui.button(this, "▶");
        next = Ui.button(this, "⏭");
        media.addView(previous, new LinearLayout.LayoutParams(0, -2, 1f));
        media.addView(play, new LinearLayout.LayoutParams(0, -2, 1f));
        media.addView(next, new LinearLayout.LayoutParams(0, -2, 1f));
        panel.addView(media);
        previous.setOnClickListener(v -> send(SpotifyMedia.Action.PREVIOUS));
        play.setOnClickListener(v -> send(SpotifyMedia.Action.TOGGLE));
        next.setOnClickListener(v -> send(SpotifyMedia.Action.NEXT));

        LinearLayout skip = Ui.row(this);
        backward = Ui.button(this, "−15 s");
        forward = Ui.button(this, "+15 s");
        skip.addView(backward, new LinearLayout.LayoutParams(0, -2, 1f));
        skip.addView(forward, new LinearLayout.LayoutParams(0, -2, 1f));
        panel.addView(skip);
        backward.setOnClickListener(v -> send(SpotifyMedia.Action.SEEK_BACK));
        forward.setOnClickListener(v -> send(SpotifyMedia.Action.SEEK_FORWARD));

        if (!BuildConfig.GOOGLE_ONLY) {
            panel.addView(fallback); // Legacy external Samsung split only.
        }
        Button toggleRoutes = Ui.button(this, "📍 Ziel / Route");
        panel.addView(toggleRoutes);
        panel.addView(routePanel);
        toggleRoutes.setOnClickListener(v -> {
            boolean open = routePanel.getVisibility() != View.VISIBLE;
            routePanel.setVisibility(open ? View.VISIBLE : View.GONE);
            toggleRoutes.setText(open ? "Zielmenü schließen" : "📍 Ziel / Route");
            if (open) scroll.post(() -> scroll.smoothScrollTo(0,
                routePanel.getTop()));
        });

        Button fuel = Ui.button(this, "⛽ Tankstellen");
        panel.addView(fuel);
        fuel.setOnClickListener(v -> startActivity(
            new Intent(this, FuelActivity.class)));
        Button saphe = Ui.button(this, "Saphe öffnen");
        panel.addView(saphe);
        saphe.setOnClickListener(v -> {
            Intent launch = getPackageManager().getLaunchIntentForPackage("my.saphelink");
            if (launch == null) {
                showStatus("Saphe-App auf diesem Gerät nicht gefunden.");
            } else {
                startActivity(launch);
            }
        });
        panel.addView(Ui.text(this, isOsmPreview()
            ? "OSM: Die Karte lädt OpenFreeMap. BRouter kann offline eine "
                + "Straßenroute berechnen, wenn die regionalen Daten vorhanden sind. "
                + "GPS-Abbiegehinweise und Android-Sprachausgabe im Entwicklungstest. "
                + "Nicht als Fahrnavigation verwenden, bis reale Tests bestanden sind."
            : "Google: Das SDK benötigt aktives Cloud Billing. "
                + "Eine kostenrelevante Route wird nur nach Bestätigung gestartet.",
            11, Ui.MUTED, false));
        setContentView(root);
        // Keep interactive content away from Android's navigation/status bars
        // and the IME. While typing a destination, temporarily hide the map;
        // it returns to the fixed 70/30 layout as soon as the keyboard closes.
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(
                WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            android.graphics.Insets keyboard = insets.getInsets(WindowInsets.Type.ime());
            boolean keyboardVisible = insets.isVisible(WindowInsets.Type.ime())
                && keyboard.bottom > 0;
            root.setPadding(bars.left, bars.top, bars.right,
                CockpitLayoutRules.safeBottomInset(bars.bottom, keyboard.bottom));
            int mapVisibility = CockpitLayoutRules.shouldShowMap(keyboardVisible)
                ? View.VISIBLE : View.GONE;
            if (mapPane.getVisibility() != mapVisibility)
                mapPane.setVisibility(mapVisibility);
            if (keyboardVisible && routePanel.getVisibility() == View.VISIBLE
                    && destination.hasFocus()) {
                scroll.post(() -> destination.requestRectangleOnScreen(
                    new android.graphics.Rect(0, 0,
                        destination.getWidth(), destination.getHeight()), true));
            }
            return insets; // Let the Google NavigationView receive its insets too.
        });
        root.requestApplyInsets();

        if (BuildConfig.GOOGLE_ONLY && (engine == null || !engine.configured())) {
            // Fail closed: never silently reveal a MapLibre or external
            // Samsung split screen when the Google key is missing.
            startActivity(new Intent(this, GoogleSetupActivity.class));
            finish();
            return;
        }
        if (engine != null && engine.configured()) askLocationThenInitialize();
    }

    private NavigationEngine loadEngine() {
        if (BuildConfig.GOOGLE_ONLY) {
            // Google or no navigation at all, irrespective of Intent extras.
            return optionalEngine(IntegratedNavigationRules.ENGINE_CLASS);
        }
        if (getIntent().getBooleanExtra(EXTRA_FORCE_OSM_TEST,false)) {
            // Explicit TEST selection: no Google route/POI may be transferred.
            return optionalEngine(
                "com.cobrachris.fahrcockpit.OsmRoutingPreviewEngine");
        }
        // Google Navigation SDK is always preferred when installed/authorized.
        NavigationEngine google = optionalEngine(IntegratedNavigationRules.ENGINE_CLASS);
        if (google != null) return google;
        // The independent OSM map/route is currently a preview, not turn guidance.
        return optionalEngine("com.cobrachris.fahrcockpit.OsmRoutingPreviewEngine");
    }

    private NavigationEngine optionalEngine(String className) {
        try {
            Class<?> type = Class.forName(className);
            NavigationEngine result = (NavigationEngine)
                type.getConstructor(Activity.class).newInstance(this);
            return result.configured() ? result : null;
        } catch (ReflectiveOperationException | LinkageError unavailable) {
            return null; // No optional provider in ordinary protected signed builds.
        }
    }

    private boolean isOsmPreview() {
        return engine != null && engine.getClass().getName().equals(
            "com.cobrachris.fahrcockpit.OsmRoutingPreviewEngine");
    }

    private void askLocationThenInitialize() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            engine.initializeAfterLocationPermission();
        } else {
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_REQUEST);
        }
    }

    @Override public void onRequestPermissionsResult(int code, String[] names,
            int[] grants) {
        super.onRequestPermissionsResult(code, names, grants);
        if (code == LOCATION_REQUEST && engine != null) {
            if (grants.length > 0
                    && grants[0] == PackageManager.PERMISSION_GRANTED) {
                engine.initializeAfterLocationPermission();
            } else {
                showStatus("Standortberechtigung fehlt · keine SDK-Navigation.");
            }
        }
    }

    private void showSavedTargets() {
        android.content.SharedPreferences prefs =
            getSharedPreferences(ROUTES_PREFS, MODE_PRIVATE);
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String target = RouteTargetRules.clean(
                prefs.getString("target_" + i, ""));
            if (target != null && !values.contains(target)) values.add(target);
        }
        if (values.isEmpty()) {
            showStatus("Keine gespeicherten Ziele · in FahrCockpit v18/v19 merken.");
            return;
        }
        new AlertDialog.Builder(this).setTitle("Gespeicherte Ziele")
            .setItems(values.toArray(new String[0]),
                (dialog, which) -> destination.setText(values.get(which)))
            .setNegativeButton("Abbrechen", (dialog, which) -> {})
            .show();
    }

    private void resolveAndConfirmRoute() {
        if (engine == null || !engine.ready()) {
            showStatus(isOsmPreview()
                ? "BRouter noch nicht bereit: Offline-Daten, App und MapLibre prüfen."
                : "Google Navigation SDK noch nicht bereit. "
                    + "Die kostenlose Google-Maps-App bleibt verfügbar.");
            return;
        }
        String target = RouteTargetRules.clean(destination.getText().toString());
        if (target == null) {
            destination.setError("Bitte ein gültiges Ziel eingeben.");
            return;
        }
        chosenTarget = target;
        showStatus(isOsmPreview()
            ? "Ziel wird ermittelt – noch keine Offline-Routenberechnung."
            : "Ziel wird ermittelt – noch keine Google-SDK-Routenanfrage.");
        NavigationDestinationResolver.resolve(this, target,
            new NavigationDestinationResolver.Callback() {
            @Override public void resolved(double lat, double lng, String name) {
                if (isFinishing() || isDestroyed()) return;
                String coords = IntegratedNavigationRules.displayCoordinates(lat,lng);
                if (coords == null) {
                    showStatus("Ortssuche hat ungültige Koordinaten geliefert.");
                    return;
                }
                boolean osm = isOsmPreview();
                new AlertDialog.Builder(IntegratedNavigationActivity.this)
                    .setTitle(osm ? "OSM-Abbiegeführung (nicht für Fahrten freigegeben)"
                        : "Google Navigation: Route bestätigen")
                    .setMessage(osm
                        ? ("OpenStreetMap / BRouter: " + name
                            + "\n\nZielkoordinaten: " + coords
                            + "\n\nEchte Offline-Straßenroute ohne Google-Cloud-Kosten. "
                            + "BRouter-Hinweise, GPS und deutsche Sprachausgabe werden "
                            + "im Entwicklungstest verwendet, falls verfügbar. "
                            + "Noch NICHT im Straßenverkehr geprüft.")
                        : ("Google Navigation SDK: " + name
                            + "\n\nZielkoordinaten: " + coords
                            + "\n\nDer Routenabruf zählt gegen die Google-"
                            + "Navigation-SDK-Quote. "
                            + "Nur nach Freigabe der Abrechnung fortfahren."))
                    .setNegativeButton("Abbrechen", (d,which) -> {})
                    .setPositiveButton(osm ? "Straßenroute planen"
                        : "Route berechnen", (d,which) -> {
                        if (engine != null && engine.ready()) {
                            engine.startConfirmedRoute(lat, lng, name);
                        }
                    })
                    .show();
            }
            @Override public void failed(String message) {
                showStatus(message);
            }
        });
    }

    private void openSpotifyOrPermissions() {
        if (!Permissions.isMediaListenerEnabled(this)) {
            showStatus("Android-Benachrichtigungszugriff für FahrCockpit einschalten.");
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } catch (RuntimeException ignored) {
                showStatus("Android-Einstellungen → Benachrichtigungszugriff.");
            }
            return;
        }
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.spotify.music");
        if (intent == null) {
            showStatus("Spotify ist nicht installiert.");
        } else {
            startActivity(intent);
        }
    }

    private void send(SpotifyMedia.Action action) {
        spotifyState.setText(SpotifyMedia.send(this, action));
        handler.postDelayed(this::updateMusic, 500L);
    }

    private void updateMusic() {
        SpotifyMedia.State state = SpotifyMedia.read(this);
        boolean allowed = Permissions.isMediaListenerEnabled(this);
        String nextAction = SpotifyDisplayRules.nextAction(allowed, state.connected);
        spotifySetup.setVisibility(nextAction == null ? View.GONE : View.VISIBLE);
        if (nextAction != null) spotifySetup.setText(nextAction);
        spotifyState.setText(state.connected
            ? SpotifyDisplayRules.connectedStatus(state.playing)
            : SpotifyDisplayRules.notConnected(allowed));
        track.setText(state.line);
        play.setText(state.playing ? "⏸" : "▶");
        play.setEnabled(state.connected);
        previous.setEnabled(state.canPrevious);
        next.setEnabled(state.canNext);
        backward.setEnabled(state.canSeek);
        forward.setEnabled(state.canSeek);
        boolean showProgress = TrackProgressRules.hasDuration(state.durationMs)
            && state.positionMs >= 0;
        totalDuration = state.durationMs;
        seek.setVisibility(showProgress ? View.VISIBLE : View.GONE);
        elapsed.setVisibility(showProgress ? View.VISIBLE : View.GONE);
        duration.setVisibility(showProgress ? View.VISIBLE : View.GONE);
        seek.setEnabled(showProgress && state.canSeek);
        if (showProgress && !touchingSeek) {
            seek.setProgress(TrackProgressRules.sliderPosition(
                state.positionMs, state.durationMs));
            elapsed.setText(TrackProgressRules.time(state.positionMs));
            duration.setText(TrackProgressRules.time(state.durationMs));
        }
    }

    private void showStatus(String message) {
        if (status != null) status.setText(message);
    }

    private void showProviderFailure(NavigationProviderPolicy.Failure reason) {
        if (BuildConfig.GOOGLE_ONLY) {
            // Never fall back to an unrelated map in the signed v21 app.
            showStatus("Google Navigation SDK nicht nutzbar ("+reason+"). "
                +"Google-Cloud-Berechtigung, Kontingent und Netzwerk prüfen. "
                +"Es wird keine Ersatzkarte oder neue kostenpflichtige "
                +"Routenanfrage gestartet.");
            return;
        }
        // Older unsigned prototypes preserve their original behavior.
        NavigationProviderPolicy.Provider next =
            NavigationProviderPolicy.fallback(reason,false);
        if (next == NavigationProviderPolicy.Provider.GOOGLE_MAPS_EXTERNAL) {
            showStatus("Google Navigation SDK derzeit nicht verfügbar ("+reason
                +"). Kostenlose externe Maps-App nur über den manuellen Knopf.");
        }
    }

    @Override protected void onStart() {
        super.onStart();
        if (engine != null) engine.onStart();
        handler.removeCallbacks(musicRefresh);
        handler.post(musicRefresh);
    }
    @Override protected void onResume() {
        super.onResume();
        if (engine != null) engine.onResume();
    }
    @Override protected void onPause() {
        if (engine != null) engine.onPause();
        super.onPause();
    }
    @Override protected void onStop() {
        handler.removeCallbacks(musicRefresh);
        if (engine != null) engine.onStop();
        super.onStop();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (engine != null) engine.onDestroy(isFinishing());
        super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle bundle) {
        bundle.putString(STATE_TARGET, destination.getText().toString());
        if (engine != null) engine.onSaveInstanceState(bundle);
        super.onSaveInstanceState(bundle);
    }
    @Override public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        if (engine != null) engine.onConfigurationChanged(config);
    }
    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (engine != null) engine.onTrimMemory(level);
    }
}
