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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import java.util.ArrayList;
import java.util.List;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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
    private static final int FUEL_PICK_REQUEST = 421;
    private static final String STATE_TARGET = "target";
    private static final String STATE_SELECTED_DISPLAY = "selected_display";
    private static final String STATE_SELECTED_TARGET = "selected_target";
    private static final String ROUTES_PREFS = "fahrcockpit_manual_routes_v1";
    private final Handler handler = new Handler(Looper.getMainLooper());

    private NavigationEngine engine;
    private TextView status, spotifyState, track, elapsed, duration;
    private EditText destination;
    private SeekBar seek;
    private Button previous, play, next, backward, forward, spotifySetup, stopButton;
    private ImageButton voiceButton;
    private TextView sapheBanner;
    private long sapheLastShownAt;
    private final Runnable hideSapheBanner = () -> {
        if (sapheBanner != null) sapheBanner.setVisibility(View.GONE);
    };
    private Button toggleRoutes;
    private LinearLayout routePanel;
    private ScrollView controlsScroll;
    private LinearLayout suggestionPanel;
    private PlacesAutocompleteBridge placesAutocomplete;
    private Runnable pendingSuggestions;
    private int suggestionGeneration;
    private boolean applyingSuggestion;
    private String selectedDisplay;
    private String selectedTarget;
    private List<DestinationStore.Entry> addressSuggestions = new ArrayList<>();
    private List<PlacesAutocompleteBridge.Prediction> googleSuggestions = new ArrayList<>();
    private boolean touchingSeek;
    private String chosenTarget;
    private long totalDuration = -1;

    private final Runnable musicRefresh = new Runnable() {
        @Override public void run() {
            if (spotifyState != null) {
                updateMusic();
                refreshSapheNotificationProbe();
            }
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
        if (savedInstanceState != null) {
            selectedDisplay = RouteTargetRules.clean(
                savedInstanceState.getString(STATE_SELECTED_DISPLAY));
            selectedTarget = RouteTargetRules.clean(
                savedInstanceState.getString(STATE_SELECTED_TARGET));
        }
        engine = loadEngine();
        placesAutocomplete = PlacesAutocompleteBridge.approved(this);

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
        controlsScroll = scroll;
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
                    @Override public void onGuidanceStateChanged(boolean active) {
                        runOnUiThread(() -> {
                            updateScreenAwake(active);
                            if (active) collapseDestinationMenu();
                        });
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
        // Icon-only speaker control: custom monochrome Drawable instead of
        // Samsung color emoji (which mixed a mute slash with colored waves).
        voiceButton = new ImageButton(this);
        voiceButton.setPadding(Ui.dp(this, 11), Ui.dp(this, 11),
            Ui.dp(this, 11), Ui.dp(this, 11));
        voiceButton.setBackground(Ui.background(this, 0xee222730, 24));
        voiceButton.setElevation(Ui.dp(this, 5));
        voiceButton.setFocusable(true);
        updateVoiceButton();
        voiceButton.setOnClickListener(v -> {
            boolean muted = !NavigationVoicePreferences.isMuted(this);
            if (!NavigationVoicePreferences.save(this, muted)) {
                showStatus("Einstellung für Navigationsansagen konnte nicht gespeichert werden.");
                return;
            }
            if (engine != null) engine.setVoiceMuted(muted);
            updateVoiceButton();
        });
        FrameLayout.LayoutParams voicePosition = new FrameLayout.LayoutParams(
            Ui.dp(this, 48), Ui.dp(this, 48), Gravity.END | Gravity.CENTER_VERTICAL);
        // Mid-right avoids Google's top turn banner and bottom ETA card.
        voicePosition.rightMargin = Ui.dp(this, 14);
        mapPane.addView(voiceButton, voicePosition);

        // Map-local diagnostic banner; never overlays the Spotify area and
        // never represents itself as a verified speed-camera or hazard alert.
        sapheBanner = Ui.text(this, "", 14, Ui.FG, true);
        sapheBanner.setGravity(Gravity.CENTER);
        sapheBanner.setMaxLines(3);
        sapheBanner.setPadding(Ui.dp(this, 12), Ui.dp(this, 9),
            Ui.dp(this, 12), Ui.dp(this, 9));
        sapheBanner.setBackground(Ui.background(this, 0xee222730, 12));
        sapheBanner.setElevation(Ui.dp(this, 6));
        sapheBanner.setVisibility(View.GONE);
        FrameLayout.LayoutParams sapheBannerPosition = new FrameLayout.LayoutParams(
            -1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        sapheBannerPosition.leftMargin = Ui.dp(this, 18);
        sapheBannerPosition.rightMargin = Ui.dp(this, 18);
        sapheBannerPosition.topMargin = Ui.dp(this, landscape ? 100 : 120);
        mapPane.addView(sapheBanner, sapheBannerPosition);

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
        routePanel = Ui.column(this);
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
        suggestionPanel = Ui.column(this);
        suggestionPanel.setVisibility(View.GONE);
        routePanel.addView(suggestionPanel);
        destination.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable value) {
                if (applyingSuggestion) return;
                selectedDisplay = null;
                selectedTarget = null;
                scheduleDestinationSuggestions(value.toString());
            }
        });
        destination.setOnFocusChangeListener((view, focused) -> {
            if (focused && routePanel.getVisibility() == View.VISIBLE) {
                scheduleDestinationSuggestions(destination.getText().toString());
                scroll.post(() -> destination.requestRectangleOnScreen(
                    new android.graphics.Rect(0, 0,
                        destination.getWidth(), destination.getHeight()), true));
            } else if (suggestionPanel != null) {
                // Let the tapped suggestion finish its click before closing
                // the parent, otherwise Samsung may lose the selected target.
                handler.postDelayed(() -> {
                    if (!destination.hasFocus() && suggestionPanel != null)
                        suggestionPanel.setVisibility(View.GONE);
                }, 180L);
            }
        });

        // Two primary destination actions on one line instead of two large
        // full-width buttons: all actions remain accessible.
        LinearLayout destinationActions = Ui.row(this);
        Button saveDestination = Ui.button(this, "☆ Speichern");
        saveDestination.setTextSize(12);
        saveDestination.setMinHeight(Ui.dp(this, 42));
        destinationActions.addView(saveDestination,
            new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f));
        saveDestination.setOnClickListener(v -> saveCurrentDestination());

        Button remembered = Ui.button(this, "★ Meine Ziele");
        remembered.setTextSize(12);
        remembered.setMinHeight(Ui.dp(this, 42));
        destinationActions.addView(remembered,
            new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f));
        remembered.setOnClickListener(v -> showSavedTargets());
        routePanel.addView(destinationActions);

        Button route = Ui.button(this, isOsmPreview()
            ? "OSM-Abbiegeführung zum Testen planen"
            : "Route berechnen");
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
        toggleRoutes = Ui.button(this, "📍 Ziel / Route");
        panel.addView(toggleRoutes);
        panel.addView(routePanel);
        toggleRoutes.setOnClickListener(v -> {
            boolean open = routePanel.getVisibility() != View.VISIBLE;
            routePanel.setVisibility(open ? View.VISIBLE : View.GONE);
            toggleRoutes.setText(open ? "▲ Zielmenü schließen" : "📍 Ziel / Route");
            if (open) scroll.post(() -> scroll.smoothScrollTo(0,
                routePanel.getTop()));
        });

        Button fuel = Ui.button(this, "⛽ Tankstellen");
        panel.addView(fuel);
        fuel.setOnClickListener(v -> startActivityForResult(
            new Intent(this, FuelActivity.class)
                .putExtra(FuelActivity.EXTRA_RETURN_TO_COCKPIT, true),
            FUEL_PICK_REQUEST));
        LinearLayout sapheActions = Ui.row(this);
        Button saphe = Ui.button(this, "Saphe öffnen");
        Button sapheCheck = Ui.button(this, "Meldung testen");
        sapheActions.addView(saphe, new LinearLayout.LayoutParams(0, -2, 1f));
        sapheActions.addView(sapheCheck, new LinearLayout.LayoutParams(0, -2, 1f));
        panel.addView(sapheActions);
        saphe.setOnClickListener(v -> {
            Intent launch = getPackageManager().getLaunchIntentForPackage("my.saphelink");
            if (launch == null) {
                showStatus("Saphe-App auf diesem Gerät nicht gefunden.");
            } else {
                startActivity(launch);
            }
        });
        sapheCheck.setOnClickListener(v -> showSapheProbeDialog());
        // The Google billing disclaimer is already included in the explicit
        // route-confirmation dialog. Do not permanently occupy the tiny
        // Spotify viewport with redundant small-print during guidance.
        if (isOsmPreview()) {
            panel.addView(Ui.text(this,
                "OSM-Entwicklungstest: nicht als Fahrnavigation verwenden.",
                11, Ui.MUTED, false));
        }
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

    /** Local suggestions display immediately; Android Geocoder runs only after
     * debounced typing. No billable Places service is enabled in this build.
     */
    private void scheduleDestinationSuggestions(String text) {
        final String query = text == null ? "" : text.trim();
        final int generation = ++suggestionGeneration;
        if (pendingSuggestions != null) handler.removeCallbacks(pendingSuggestions);
        addressSuggestions = new ArrayList<>();
        googleSuggestions = new ArrayList<>();
        renderDestinationSuggestions(query);
        if (!DestinationMatchRules.maySuggestOnline(query)) return;
        pendingSuggestions = () -> {
            if (isFinishing() || isDestroyed()) return;
            if (placesAutocomplete != null) {
                placesAutocomplete.suggest(query, new PlacesAutocompleteBridge.Predictions() {
                    @Override public void success(List<PlacesAutocompleteBridge.Prediction> values) {
                        runOnUiThread(() -> {
                            if (!suggestionsCurrent(generation, query)) return;
                            googleSuggestions = values == null ? new ArrayList<>() : values;
                            renderDestinationSuggestions(query);
                        });
                    }
                    @Override public void failure() {
                        runOnUiThread(() -> {
                            if (!suggestionsCurrent(generation, query)) return;
                            googleSuggestions = new ArrayList<>();
                            renderDestinationSuggestions(query);
                        });
                    }
                });
            } else {
                AddressSuggestionResolver.suggest(this, query, values -> {
                    if (!suggestionsCurrent(generation, query)) return;
                    addressSuggestions = values;
                    renderDestinationSuggestions(query);
                });
            }
        };
        handler.postDelayed(pendingSuggestions, 650L);
    }

    private boolean suggestionsCurrent(int generation, String query) {
        return !isFinishing() && !isDestroyed()
            && generation == suggestionGeneration && destination.hasFocus()
            && query.equals(destination.getText().toString().trim());
    }

    private void renderDestinationSuggestions(String query) {
        if (suggestionPanel == null) return;
        suggestionPanel.removeAllViews();
        if (!destination.hasFocus()) {
            suggestionPanel.setVisibility(View.GONE);
            return;
        }
        List<DestinationStore.Entry> local = DestinationStore.suggestions(this, query, 4);
        int count = 0;
        for (DestinationStore.Entry entry : local) {
            if (count++ >= 4) break;
            String heading = (entry.favorite ? "⭐ " : "↶ ") + entry.label;
            String detail = entry.target.equalsIgnoreCase(entry.label)
                ? "" : "\n" + entry.target;
            addSuggestionRow(heading + detail,
                () -> applyDestinationSuggestion(entry.label, entry.target));
        }
        int remaining = Math.max(0, 6 - count);
        if (placesAutocomplete != null) {
            for (PlacesAutocompleteBridge.Prediction prediction : googleSuggestions) {
                if (remaining-- <= 0) break;
                addSuggestionRow("📍 Google: " + prediction.display, () -> {
                    final int generation = ++suggestionGeneration;
                    placesAutocomplete.resolve(prediction,
                        new PlacesAutocompleteBridge.Selection() {
                        @Override public void success(String label, String target) {
                            runOnUiThread(() -> {
                                if (generation == suggestionGeneration)
                                    applyDestinationSuggestion(label, target);
                            });
                        }
                        @Override public void failure() {
                            runOnUiThread(() -> showStatus(
                                "Google-Ort konnte nicht geladen werden. Bitte Adresse eintippen."));
                        }
                    });
                });
            }
        } else {
            for (DestinationStore.Entry entry : addressSuggestions) {
                if (remaining-- <= 0) break;
                boolean duplicate = false;
                for (DestinationStore.Entry stored : local)
                    if (DestinationMatchRules.sameTarget(stored.target, entry.target))
                        duplicate = true;
                if (duplicate) continue;
                addSuggestionRow("📍 " + entry.label,
                    () -> applyDestinationSuggestion(entry.label, entry.target));
            }
        }
        suggestionPanel.setVisibility(suggestionPanel.getChildCount() == 0
            ? View.GONE : View.VISIBLE);
    }

    private void addSuggestionRow(String label, Runnable select) {
        Button item = Ui.button(this, label);
        item.setTextSize(12);
        item.setMaxLines(2);
        item.setContentDescription(label);
        item.setOnClickListener(v -> select.run());
        suggestionPanel.addView(item);
    }

    private void applyDestinationSuggestion(String label, String target) {
        String display = RouteTargetRules.clean(label);
        String actual = RouteTargetRules.clean(target);
        if (display == null || actual == null) return;
        suggestionGeneration++;
        if (pendingSuggestions != null) handler.removeCallbacks(pendingSuggestions);
        applyingSuggestion = true;
        destination.setText(display);
        destination.setSelection(display.length());
        applyingSuggestion = false;
        selectedDisplay = display;
        selectedTarget = actual;
        suggestionPanel.removeAllViews();
        suggestionPanel.setVisibility(View.GONE);
        InputMethodManager keyboard = (InputMethodManager)
            getSystemService(INPUT_METHOD_SERVICE);
        if (keyboard != null)
            keyboard.hideSoftInputFromWindow(destination.getWindowToken(), 0);
        destination.clearFocus();
    }

    private String selectedNavigationTarget() {
        String text = RouteTargetRules.clean(destination.getText().toString());
        return text != null && text.equals(selectedDisplay) && selectedTarget != null
            ? selectedTarget : text;
    }

    private void saveCurrentDestination() {
        String display = RouteTargetRules.clean(destination.getText().toString());
        String target = selectedNavigationTarget();
        if (display == null || target == null) {
            destination.setError("Bitte zuerst ein Ziel eingeben oder auswählen.");
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Ziel als Favorit speichern")
            .setItems(new String[]{"🏠 Zuhause", "💼 Arbeit", "☆ Eigenen Namen vergeben"},
                (dialog, which) -> {
                    if (which < 2) {
                        saveNamedDestination(which == 0 ? "Zuhause" : "Arbeit", target);
                    } else {
                        showFavoriteEditor(null, "", target);
                    }
                })
            .setNegativeButton("Abbrechen", (dialog, which) -> {})
            .show();
    }

    /**
     * Name and real navigation destination are deliberately separate fields:
     * "Tante Emma" is displayed, while the street/coordinates are routed to.
     * Editing an existing favorite is one atomic preferences write; canceling
     * or invalid input never deletes the previous entry.
     */
    private void showFavoriteEditor(String previousTarget, String currentLabel,
                                    String currentAddress) {
        showFavoriteEditor(previousTarget, currentLabel, currentAddress, false, false);
    }

    private void showFavoriteEditor(String previousTarget, String currentLabel,
                                    String currentAddress, boolean olderFavorite,
                                    boolean returnToManager) {
        LinearLayout form = Ui.column(this);
        int padding = Ui.dp(this, 20);
        form.setPadding(padding, Ui.dp(this, 8), padding, 0);
        form.addView(Ui.text(this, "Name des Ziels", 14, Ui.FG, true));
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint("z. B. Tante Emma oder Onkel Klaus");
        name.setText(currentLabel);
        form.addView(name);
        form.addView(Ui.text(this, "Adresse oder Koordinaten", 14, Ui.FG, true));
        EditText address = new EditText(this);
        address.setSingleLine(true);
        address.setHint("Straße, Hausnummer, Ort");
        address.setText(currentAddress);
        form.addView(address);
        AlertDialog editor = new AlertDialog.Builder(this)
            .setTitle(previousTarget == null ? "Eigenes Ziel speichern"
                : "Favorit bearbeiten")
            .setView(form)
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Speichern", null)
            .create();
        editor.setOnShowListener(ignored -> editor.getButton(
            AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String label = DestinationMatchRules.label(name.getText().toString());
            String target = RouteTargetRules.clean(address.getText().toString());
            if (label == null) {
                name.setError("Bitte einen Namen eingeben.");
                return;
            }
            if (target == null) {
                address.setError("Bitte eine gültige Adresse eingeben.");
                return;
            }
            boolean updated = previousTarget == null
                ? DestinationStore.saveFavorite(this, label, target)
                : olderFavorite
                    ? DestinationStore.updateLegacyFavorite(this, previousTarget,
                        label, target)
                    : DestinationStore.updateFavorite(this, previousTarget, label, target);
            if (!updated) {
                showStatus("Favorit nicht gespeichert. Name oder Ziel bereits vergeben?");
                return;
            }
            // Keep name and address associated in the current navigation field.
            if (previousTarget != null && selectedTarget != null &&
                    DestinationMatchRules.sameTarget(previousTarget, selectedTarget)) {
                applyDestinationSuggestion(label, target);
            } else if (previousTarget == null) {
                applyDestinationSuggestion(label, target);
            }
            showStatus("Favorit gespeichert: " + label);
            editor.dismiss();
            if (returnToManager) showManageFavorites();
        }));
        editor.show();
    }

    private void saveNamedDestination(String label, String target) {
        boolean saved = DestinationStore.saveFavorite(this, label, target);
        showStatus(saved ? "Ziel als Favorit gespeichert: " + label
            : "Ziel konnte nicht gespeichert werden.");
    }

    private void showSavedTargets() {
        List<DestinationStore.Entry> entries = DestinationStore.suggestions(this, "", 40);
        if (entries.isEmpty()) {
            showStatus("Keine Ziele gespeichert. Jetzt über ☆ Ziel speichern anlegen.");
            return;
        }
        String[] labels = new String[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            DestinationStore.Entry item = entries.get(i);
            // Private custom labels are the visible names; the underlying
            // address is passed on selection and remains editable in Manage.
            labels[i] = (item.favorite ? "★ " : "↶ ") + item.label;
        }
        new AlertDialog.Builder(this).setTitle("Gespeicherte und letzte Ziele")
            .setItems(labels, (dialog, which) ->
                applyDestinationSuggestion(entries.get(which).label,
                    entries.get(which).target))
            .setNeutralButton("Favoriten verwalten", (dialog, which) ->
                showManageFavorites())
            .setNegativeButton("Abbrechen", (dialog, which) -> {})
            .show();
    }

    private void showManageFavorites() {
        // The selection list contains modern favorites, older stored targets and
        // recent destinations. All of them must be removable from this screen.
        List<DestinationStore.Entry> entries = DestinationStore.manageable(this);
        if (entries.isEmpty()) {
            showStatus("Keine gespeicherten oder zuletzt verwendeten Ziele vorhanden.");
            return;
        }
        String[] labels = new String[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            DestinationStore.Entry item = entries.get(i);
            if (item.source == DestinationStore.Source.RECENT) {
                labels[i] = "↶ " + item.label + " (zuletzt verwendet)";
            } else if (item.source == DestinationStore.Source.LEGACY_FAVORITE) {
                labels[i] = "★ " + item.label + " (älterer Favorit)";
            } else {
                labels[i] = "★ " + item.label;
            }
        }
        new AlertDialog.Builder(this).setTitle("Ziele bearbeiten oder löschen")
            .setItems(labels, (dialog, which) -> {
                DestinationStore.Entry selected = entries.get(which);
                if (selected.source == DestinationStore.Source.RECENT) {
                    // Recent destinations have no saved custom name to edit.
                    new AlertDialog.Builder(this)
                        .setTitle(selected.label)
                        .setMessage("Zuletzt verwendetes Ziel: " + selected.target)
                        .setPositiveButton("Aus Liste entfernen", (d, n) ->
                            confirmRemoveDestination(selected))
                        .setNegativeButton("Abbrechen", (d, n) -> showManageFavorites())
                        .show();
                    return;
                }
                new AlertDialog.Builder(this)
                    .setTitle(selected.label)
                    .setMessage("Hinterlegte Adresse: " + selected.target)
                    .setPositiveButton("Bearbeiten", (d, n) ->
                        showFavoriteEditor(selected.target, selected.label,
                            selected.target,
                            selected.source == DestinationStore.Source.LEGACY_FAVORITE,
                            true))
                    .setNegativeButton("Löschen", (d, n) ->
                        confirmRemoveDestination(selected))
                    .setNeutralButton("Abbrechen", (d, n) -> showManageFavorites())
                    .show();
            })
            .setNegativeButton("Schließen", (dialog, which) -> {})
            .show();
    }

    private void confirmRemoveDestination(DestinationStore.Entry selected) {
        new AlertDialog.Builder(this)
            .setTitle("Ziel aus der Liste löschen?")
            .setMessage("„" + selected.label + "“ wird aus den Favoriten und "
                + "der Liste zuletzt verwendeter Ziele entfernt. "
                + "Eine laufende Navigation wird nicht beendet.")
            .setNegativeButton("Abbrechen", (d, n) -> showManageFavorites())
            .setPositiveButton("Löschen", (d, n) -> {
                boolean removed = DestinationStore.removeManaged(this, selected.target);
                showStatus(removed ? "Ziel entfernt: " + selected.label
                    : "Ziel konnte nicht vollständig entfernt werden.");
                showManageFavorites();
            })
            .show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FUEL_PICK_REQUEST || resultCode != RESULT_OK || data == null)
            return;
        double lat = data.getDoubleExtra(FuelActivity.RESULT_LATITUDE, Double.NaN);
        double lng = data.getDoubleExtra(FuelActivity.RESULT_LONGITUDE, Double.NaN);
        String coords = FuelRules.stationCoordinates(lat, lng);
        if (coords == null) {
            showStatus("Tankstelle hat ungültige Zielkoordinaten.");
            return;
        }
        String name = RouteTargetRules.clean(
            data.getStringExtra(FuelActivity.RESULT_NAME));
        if (name == null) name = "Tankstelle";
        chosenTarget = name;
        applyDestinationSuggestion(name, coords);
        if (engine == null || !engine.ready()) {
            showStatus("Tankstelle übernommen. Navigation noch nicht bereit.");
            return;
        }
        // No silent Google route switch or billable request: require consent.
        final String chosenName = name;
        new AlertDialog.Builder(this)
            .setTitle("Google Navigation: Tankstelle anfahren")
            .setMessage(chosenName + "\nZielkoordinaten: " + coords
                + "\n\nDie neue Route ersetzt nach Bestätigung gegebenenfalls "
                + "die bisherige Route. Die Berechnung zählt gegen die "
                + "Google-Navigation-SDK-Quote.")
            .setNegativeButton("Abbrechen", (dialog, which) -> {})
            .setPositiveButton("Route berechnen", (dialog, which) -> {
                if (engine != null && engine.ready()) {
                    DestinationStore.recordRecent(this, chosenName, coords);
                    engine.startConfirmedRoute(lat, lng, chosenName);
                    collapseDestinationMenu();
                }
            })
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
        String display = RouteTargetRules.clean(destination.getText().toString());
        String target = selectedNavigationTarget();
        if (display == null || target == null) {
            destination.setError("Bitte ein gültiges Ziel eingeben.");
            return;
        }
        chosenTarget = display;
        showStatus(isOsmPreview()
            ? "Ziel wird ermittelt – noch keine Offline-Routenberechnung."
            : "Ziel wird ermittelt – noch keine Google-SDK-Routenanfrage.");
        NavigationDestinationResolver.resolve(this, target,
            new NavigationDestinationResolver.Callback() {
            @Override public void resolved(double lat, double lng, String name) {
                if (isFinishing() || isDestroyed()) return;
                final String readable = display.equals(selectedDisplay)
                    ? display : name;
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
                            DestinationStore.recordRecent(
                                IntegratedNavigationActivity.this, readable, coords);
                            engine.startConfirmedRoute(lat, lng, readable);
                            collapseDestinationMenu();
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

    private void updateVoiceButton() {
        if (voiceButton == null) return;
        boolean muted = NavigationVoicePreferences.isMuted(this);
        voiceButton.setImageDrawable(new NavigationAudioIcon(muted));
        voiceButton.setContentDescription(muted
            ? "Navigationsansagen aus. Zum Einschalten tippen."
            : "Navigationsansagen an. Zum Ausschalten tippen.");
        voiceButton.setSelected(muted);
    }

    private void collapseDestinationMenu() {
        if (routePanel == null || routePanel.getVisibility() != View.VISIBLE)
            return;
        routePanel.setVisibility(View.GONE);
        if (toggleRoutes != null) toggleRoutes.setText("📍 Ziel / Route");
        if (controlsScroll != null)
            controlsScroll.post(() -> controlsScroll.smoothScrollTo(0, 0));
    }

    private void updateScreenAwake(boolean routeActive) {
        if (routeActive) {
            // Only an active route keeps the display awake. It cannot defeat
            // a manual power-button lock or system-enforced security policy.
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void showSapheBanner(boolean simulated) {
        if (sapheBanner == null) return;
        String text = simulated
            ? "TEST · FahrCockpit-Einblendung\nKeine echte Saphe-Warnung"
            : "Saphe-Link-Mitteilung erkannt\nWarnungstyp nicht verifiziert";
        sapheBanner.setText(text);
        sapheBanner.setContentDescription(text);
        sapheBanner.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideSapheBanner);
        handler.postDelayed(hideSapheBanner, 6500L);
    }

    private void refreshSapheNotificationProbe() {
        if (sapheBanner == null || !SapheNotificationProbe.enabled(this)) return;
        long observedAt = SapheNotificationProbe.lastObservedAt(this);
        if (observedAt > sapheLastShownAt) {
            sapheLastShownAt = observedAt;
            if (SapheNotificationRules.isRecent(
                    observedAt, System.currentTimeMillis())) showSapheBanner(false);
        }
    }

    private void showSapheProbeDialog() {
        boolean hasAndroidAccess = Permissions.isMediaListenerEnabled(this);
        boolean optedIn = SapheNotificationProbe.enabled(this);
        long seen = SapheNotificationProbe.lastObservedAt(this);
        String details = "FahrCockpit prüft ausschließlich, ob Android eine "
            + "Benachrichtigung der offiziellen Saphe-Link-App meldet. "
            + "Der Inhalt bleibt ungelesen. Das ist noch keine echte "
            + "Verkehrs- oder Blitzerwarnung.\n\n"
            + "Benachrichtigungszugriff: " + (hasAndroidAccess ? "erlaubt" : "fehlt")
            + "\nDiagnose: " + (optedIn ? "eingeschaltet" : "ausgeschaltet")
            + "\nSaphe-Link-Mitteilung erkannt: " + (seen > 0 ? "ja" : "noch nein")
            + "\n\nDer Test ist für das stehende Fahrzeug vorgesehen.";
        new AlertDialog.Builder(this)
            .setTitle("Saphe-Meldungen prüfen")
            .setMessage(details)
            .setNeutralButton("Testanzeige", (dialog, which) ->
                showSapheBanner(true))
            .setNegativeButton("Schließen", (dialog, which) -> {})
            .setPositiveButton(!hasAndroidAccess ? "Android-Zugriff prüfen"
                : optedIn ? "Diagnose ausschalten" : "Diagnose einschalten",
                (dialog, which) -> {
                    if (!hasAndroidAccess) {
                        try {
                            startActivity(new Intent(
                                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                        } catch (RuntimeException unavailable) {
                            showStatus("Benachrichtigungszugriff nicht geöffnet.");
                        }
                        return;
                    }
                    boolean next = !optedIn;
                    if (!SapheNotificationProbe.setEnabled(this, next)) {
                        showStatus("Saphe-Diagnose konnte nicht gespeichert werden.");
                        return;
                    }
                    sapheLastShownAt = 0L;
                    handler.removeCallbacks(hideSapheBanner);
                    if (sapheBanner != null) sapheBanner.setVisibility(View.GONE);
                    showStatus(next
                        ? "Saphe-Diagnose aktiv: wartet auf eine Android-Mitteilung."
                        : "Saphe-Diagnose ausgeschaltet.");
                })
            .show();
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
        if (placesAutocomplete != null) placesAutocomplete.reset();
        if (engine != null) engine.onDestroy(isFinishing());
        updateScreenAwake(false);
        super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle bundle) {
        bundle.putString(STATE_TARGET, destination.getText().toString());
        bundle.putString(STATE_SELECTED_DISPLAY, selectedDisplay);
        bundle.putString(STATE_SELECTED_TARGET, selectedTarget);
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
