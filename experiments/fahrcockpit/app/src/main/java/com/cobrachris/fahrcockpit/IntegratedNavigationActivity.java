package com.cobrachris.fahrcockpit;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import android.widget.PopupMenu;
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
    private static final int MENU_SWAP_PANE = 5101;
    private static final int MENU_APP_UPDATE = 5102;
    private static final int MENU_FUEL_SETTINGS = 5103;
    private static final int MENU_SAPHE_DIAGNOSIS = 5104;
    private static final int MENU_FUEL_AUTO = 5105;
    private static final int MENU_PARKING_SHOW = 5106;
    private static final int MENU_PARKING_SAVE = 5107;
    private static final String STATE_TARGET = "target";
    private static final String STATE_SELECTED_DISPLAY = "selected_display";
    private static final String STATE_SELECTED_TARGET = "selected_target";
    private static final String ROUTES_PREFS = "fahrcockpit_manual_routes_v1";
    private final Handler handler = new Handler(Looper.getMainLooper());

    private NavigationEngine engine;
    private final Runnable expireFuelMarkers = () -> updateFuelMarkers();
    private TextView status, spotifyState, track, elapsed, duration, spotifyHeader, fuelInfo;
    private LinearLayout mediaControls, mediaSkip, spotifyHeaderRow, mediaProgressRow;
    private Button fuelButton, fuelRefreshButton;
    private FuelQuickSearch fuelQuickSearch;
    private FuelAutoRefreshController autoFuelRefreshController;
    private boolean fuelMarkersVisible = true;
    private EditText destination;
    private SeekBar seek;
    private Button previous, play, next, backward, forward, spotifySetup, stopButton;
    private ImageButton voiceButton;
    private TextView sapheBanner;
    private TextView safetyEventStatus;
    private TextView currentSpeedView;
    private boolean navigationGuidanceActive;
    private boolean cockpitResumed;
    private final ParkingTripTracker parkingTrip = new ParkingTripTracker();
    private LocationManager speedLocationManager;
    private LocationListener speedLocationListener;
    private final Runnable expireSpeedReading = () -> showUnknownSpeed();
    private long sapheLastShownAt;
    private long lastSafetyShownAt;
    private int safetyEventCount;
    private static final long SAFETY_COOLDOWN_MS = 10000L;
    private final Runnable hideSapheBanner = () -> {
        if (sapheBanner != null) {
            sapheBanner.clearAnimation();
            sapheBanner.setVisibility(View.GONE);
        }
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
        fuelQuickSearch = new FuelQuickSearch(this);
        autoFuelRefreshController =
            new FuelAutoRefreshController(this, this::startQuickFuelSearch);
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
        // Keep the GPS speed visible above the scrollable cockpit controls.
        // The 70/30 pane weights and native Google map controls stay intact.
        LinearLayout cockpitPane = new LinearLayout(this);
        cockpitPane.setOrientation(LinearLayout.VERTICAL);
        cockpitPane.setBackgroundColor(Ui.PANEL);
        currentSpeedView = Ui.text(this, "GPS — km/h",
            landscape ? 20 : 23, Ui.FG, true);
        currentSpeedView.setContentDescription("GPS-Geschwindigkeit nicht verfügbar");
        currentSpeedView.setGravity(Gravity.CENTER_VERTICAL);
        currentSpeedView.setPadding(Ui.dp(this, 12), Ui.dp(this, 7),
            Ui.dp(this, 12), Ui.dp(this, 7));
        cockpitPane.addView(currentSpeedView,
            new LinearLayout.LayoutParams(-1, -2));
        cockpitPane.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        LinearLayout.LayoutParams controlParams = landscape
            ? new LinearLayout.LayoutParams(0, -1,
                IntegratedNavigationRules.CONTROLS_WEIGHT)
            : new LinearLayout.LayoutParams(-1, 0,
                CockpitLayoutRules.controlsWeight(false));
        if (landscape && spotifyLeft) {
            root.addView(cockpitPane, controlParams);
            root.addView(mapPane, mapParams);
        } else {
            root.addView(mapPane, mapParams);
            root.addView(cockpitPane, controlParams);
        }

        LinearLayout panel = Ui.column(this);
        panel.setPadding(Ui.dp(this, landscape ? 8 : 12),
            Ui.dp(this, landscape ? 2 : 4),
            Ui.dp(this, landscape ? 8 : 12), Ui.dp(this, landscape ? 8 : 10));
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
                    @Override public void onFinalDestinationArrived() {
                        runOnUiThread(() -> {
                            parkingTrip.onFinalArrival(SystemClock.elapsedRealtime());
                            trySaveArrivedParking();
                        });
                    }
                    @Override public void onGuidanceStateChanged(boolean active) {
                        runOnUiThread(() -> {
                            boolean wasActive = navigationGuidanceActive;
                            navigationGuidanceActive = active;
                            if (active && !wasActive)
                                parkingTrip.start(SystemClock.elapsedRealtime());
                            if (!active && wasActive) {
                                ParkingTripTracker.Spot spot =
                                    parkingTrip.finish(SystemClock.elapsedRealtime());
                                if (spot != null && ParkingLocationStore.save(IntegratedNavigationActivity.this, spot, true))
                                    showStatus("Parkplatz nach Fahrtende automatisch gemerkt.");
                            }
                            if (autoFuelRefreshController != null)
                                autoFuelRefreshController.setNavigationActive(active);
                            updateScreenAwake(active);
                            // During Google guidance the native map speedometer
                            // is the sole speed display. Outside guidance the
                            // cockpit shows its own GPS speed instead.
                            if (currentSpeedView != null)
                                currentSpeedView.setVisibility(
                                    active ? View.GONE : View.VISIBLE);
                            if (active) {
                                collapseDestinationMenu();
                            }
                            // Keep GPS fixes for parking even while the native
                            // Google speedometer is the only speed display.
                            startSpeedUpdates();
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

        // Compact, single-line identity: preserve the provider distinction,
        // but do not waste a separate row in the narrow driving cockpit.
        TextView cockpitTitle = Ui.text(this, BuildConfig.GOOGLE_ONLY
            ? "FahrCockpit"
            : (BuildConfig.VISUAL_PREVIEW
                ? "FahrCockpit · OSM-Test"
                : "FahrCockpit"), landscape ? 13 : 14, Ui.FG, true);
        cockpitTitle.setSingleLine(true);
        cockpitTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        cockpitTitle.setPadding(0, 0, Ui.dp(this, 5), 0);
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
        // Keep voice controls off the live map: Google's compass, guidance
        // banner and ETA stay unobstructed in both screen orientations.
        voiceButton.setMinimumWidth(Ui.dp(this, 44));
        voiceButton.setMinimumHeight(Ui.dp(this, 44));

        // Full-screen temporary Safety diagnostic, dramatically more visible
        // than the former map strip. A Saphe notification is NOT a verified
        // camera warning; always label its type as unverified.
        sapheBanner = Ui.text(this, "", landscape ? 48 : 58, 0xffffffff, true);
        sapheBanner.setGravity(Gravity.CENTER);
        sapheBanner.setMaxLines(3);
        sapheBanner.setPadding(Ui.dp(this, 22), Ui.dp(this, 32),
            Ui.dp(this, 22), Ui.dp(this, 32));
        sapheBanner.setBackgroundColor(0xf2ab1020);
        sapheBanner.setElevation(Ui.dp(this, 10));
        sapheBanner.setVisibility(View.GONE);
        sapheBanner.setContentDescription("Safety-Ereignis; Typ nicht verifiziert");

        // Keep options and navigation speech available while scrolling
        // Spotify and destinations. This row stays in the cockpit pane,
        // never on top of Google's turn banner, speedometer or ETA.
        LinearLayout tools = Ui.row(this);
        tools.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        tools.addView(cockpitTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        tools.addView(voiceButton, new LinearLayout.LayoutParams(
            Ui.dp(this, 44), Ui.dp(this, 44)));
        Button optionsButton = cockpitButton("⋮", false);
        optionsButton.setTextSize(22);
        optionsButton.setContentDescription("FahrCockpit-Optionen öffnen");
        LinearLayout.LayoutParams optionsParams =
            new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44));
        optionsParams.leftMargin = Ui.dp(this, 5);
        tools.addView(optionsButton, optionsParams);
        cockpitPane.addView(tools, 1,
            new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));
        optionsButton.setOnClickListener(v -> {
            PopupMenu options = new PopupMenu(this, optionsButton);
            if (landscape) {
                boolean panelLeft = CockpitPanePreferences.spotifyLeft(this);
                options.getMenu().add(0, MENU_SWAP_PANE, 0,
                    panelLeft ? "Bedienfeld nach rechts" : "Bedienfeld nach links");
            }
            options.getMenu().add(0, MENU_APP_UPDATE, 1, "App aktualisieren");
            options.getMenu().add(0, MENU_FUEL_SETTINGS, 2,
                "Tankstellen-Einstellungen");
            options.getMenu().add(0, MENU_SAPHE_DIAGNOSIS, 3, "Saphe-Diagnose");
            options.getMenu().add(0, MENU_PARKING_SHOW, 5, "Parkplatz anzeigen");
            options.getMenu().add(0, MENU_PARKING_SAVE, 6, "Parkplatz jetzt speichern");
            options.getMenu().add(0, MENU_FUEL_AUTO, 4,
                !FuelAutoRefreshPolicy.PROVIDER_APPROVED
                    ? "Auto-Preise: Freigabe ausstehend"
                    : (FuelAutoRefreshPreferences.enabled(this)
                        ? "Auto-Preise ausschalten" : "Auto-Preise einschalten"));
            options.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case MENU_SWAP_PANE:
                        // Reparent only the existing views: never recreate
                        // navigation or interrupt Spotify playback.
                        if (!landscape) return true;
                        boolean newLeft = !CockpitPanePreferences.spotifyLeft(this);
                        if (!CockpitPanePreferences.saveSpotifyLeft(this, newLeft)) {
                            showStatus("Cockpit-Seite konnte nicht gespeichert werden.");
                            return true;
                        }
                        root.removeView(mapPane);
                        root.removeView(cockpitPane);
                        if (newLeft) {
                            root.addView(cockpitPane, controlParams);
                            root.addView(mapPane, mapParams);
                        } else {
                            root.addView(mapPane, mapParams);
                            root.addView(cockpitPane, controlParams);
                        }
                        return true;
                    case MENU_APP_UPDATE:
                        startActivity(new Intent(this, UpdateActivity.class));
                        return true;
                    case MENU_FUEL_SETTINGS:
                        startActivityForResult(new Intent(this, FuelActivity.class)
                            .putExtra(FuelActivity.EXTRA_RETURN_TO_COCKPIT, true),
                            FUEL_PICK_REQUEST);
                        return true;
                    case MENU_SAPHE_DIAGNOSIS:
                        showSapheProbeDialog();
                        return true;
                    case MENU_PARKING_SHOW:
                        showParkingLocation();
                        return true;
                    case MENU_PARKING_SAVE:
                        saveParkingManually();
                        return true;
                    case MENU_FUEL_AUTO:
                        if (!FuelAutoRefreshPolicy.PROVIDER_APPROVED) {
                            new AlertDialog.Builder(this)
                                .setTitle("Automatische Tankstellenpreise")
                                .setMessage("Die automatische Aktualisierung während der "
                                    + "Navigation ist vorbereitet, aber für die kostenlose "
                                    + "Tankerkönig-API noch nicht ausdrücklich genehmigt. "
                                    + "Bis zur schriftlichen Anbieterfreigabe bleiben "
                                    + "regelmäßige automatische API-Abfragen gesperrt. "
                                    + "Bereits gespeicherte Preise sind mit Zeitstempel "
                                    + "weiterhin sichtbar.")
                                .setPositiveButton("Verstanden", null).show();
                        } else {
                            boolean enabled = !FuelAutoRefreshPreferences.enabled(this);
                            if (!FuelAutoRefreshPreferences.saveEnabled(this, enabled)) {
                                showStatus("Auto-Preis-Einstellung konnte nicht gespeichert werden.");
                            } else if (autoFuelRefreshController != null) {
                                autoFuelRefreshController.refreshUserSetting();
                            }
                        }
                        return true;
                    default:
                        return false;
                }
            });
            options.show();
        });
        status = Ui.text(this, isOsmPreview()
            ? "OSM-Abbiegeführung (Entwicklungstest, nicht für reale Fahrten)"
            : (engine != null && engine.configured()
                ? "Google Navigation SDK wird vorbereitet …"
                : "Vorschau · Navigation SDK nicht aktiviert · keine Gebühren"),
            11, Ui.WARN, false);
        status.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 2));
        status.setMaxLines(3);
        // A routine success message is diagnostic, not permanent driving UI.
        status.setVisibility(engine != null && engine.configured()
            && BuildConfig.GOOGLE_ONLY ? View.GONE : View.VISIBLE);
        panel.addView(status);
        // Saphe status is moved beside its own controls further below. An
        // enabled notification probe is NOT proof of a connected device.
        safetyEventStatus = Ui.text(this, "", 10, Ui.MUTED, false);
        safetyEventStatus.setSingleLine(true);
        safetyEventStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);
        safetyEventStatus.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        safetyEventStatus.setPadding(0, 0, 0, 0);

        // Route creation is intentionally collapsed during driving; Spotify
        // stays visible in the limited 30 % panel without manual searching.
        routePanel = Ui.column(this);
        routePanel.setPadding(Ui.dp(this, 8), Ui.dp(this, 6),
            Ui.dp(this, 8), Ui.dp(this, 8));
        routePanel.setBackground(Ui.background(this, 0xff2b3643, 10));
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
        Button saveDestination = cockpitButton("☆ Speichern", false);
        saveDestination.setTextSize(12);
        saveDestination.setMinHeight(Ui.dp(this, 42));
        destinationActions.addView(saveDestination,
            new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f));
        saveDestination.setOnClickListener(v -> saveCurrentDestination());

        Button remembered = cockpitButton("★ Meine Ziele", false);
        remembered.setTextSize(12);
        remembered.setMinHeight(Ui.dp(this, 42));
        destinationActions.addView(remembered,
            new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f));
        remembered.setOnClickListener(v -> showSavedTargets());
        routePanel.addView(destinationActions);

        Button route = cockpitButton(isOsmPreview()
            ? "OSM-Test planen" : "Route berechnen", true);
        routePanel.addView(route);
        route.setEnabled(engine != null && engine.configured());
        route.setOnClickListener(v -> resolveAndStartRoute());
        stopButton = cockpitButton("Navigation beenden", false);
        routePanel.addView(stopButton);
        stopButton.setEnabled(engine != null && engine.configured());
        stopButton.setOnClickListener(v -> {
            if (engine == null) return;
            engine.stopGuidanceAndClearRoute();
            showStatus("Navigation beendet · Route gelöscht.");
        });

        // Keep the free navigation visible even when the route form is collapsed.
        Button fallback = cockpitButton(
            "Google Maps separat öffnen (Samsung-Splitscreen)", false);
        fallback.setOnClickListener(v -> {
            String text = RouteTargetRules.clean(destination.getText().toString());
            Intent intent = new Intent(this, SplitCockpitActivity.class);
            if (text != null)
                intent.putExtra(SplitCockpitActivity.EXTRA_NAVIGATION_TARGET, text);
            startActivity(intent);
        });

        Ui.gap(this, panel, 4);
        spotifyHeaderRow = Ui.row(this);
        spotifyHeader = Ui.text(this, "Spotify", 14, Ui.FG, true);
        spotifyHeader.setPadding(0, 0, 0, 0);
        spotifyState = Ui.text(this, "Status wird geprüft …",
            10, Ui.MUTED, false);
        spotifyState.setPadding(0, 0, 0, 0);
        spotifyState.setSingleLine(true);
        spotifyState.setEllipsize(android.text.TextUtils.TruncateAt.END);
        spotifyState.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        spotifyHeaderRow.addView(spotifyHeader,
            new LinearLayout.LayoutParams(-2, Ui.dp(this, 30)));
        spotifyHeaderRow.addView(spotifyState,
            new LinearLayout.LayoutParams(0, -2, 1f));
        panel.addView(spotifyHeaderRow);
        spotifySetup = cockpitButton("Spotify öffnen", false);
        panel.addView(spotifySetup, new LinearLayout.LayoutParams(-1, Ui.dp(this, 44)));
        spotifySetup.setOnClickListener(v -> openSpotifyOrPermissions());
        track = Ui.text(this, "", 12, Ui.FG, true);
        track.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 2));
        track.setMaxLines(2);
        track.setEllipsize(android.text.TextUtils.TruncateAt.END);
        panel.addView(track);

        mediaProgressRow = Ui.row(this);
        elapsed = Ui.text(this, "0:00", 10, Ui.MUTED, false);
        duration = Ui.text(this, "0:00", 10, Ui.MUTED, false);
        elapsed.setPadding(0, 0, 0, 0);
        duration.setPadding(0, 0, 0, 0);
        elapsed.setSingleLine(true);
        duration.setSingleLine(true);
        duration.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        seek = new SeekBar(this);
        seek.setMax(TrackProgressRules.MAX);
        seek.setContentDescription("Wiedergabeposition");
        mediaProgressRow.addView(elapsed,
            new LinearLayout.LayoutParams(Ui.dp(this, 37), -2));
        mediaProgressRow.addView(seek,
            new LinearLayout.LayoutParams(0, Ui.dp(this, 38), 1f));
        mediaProgressRow.addView(duration,
            new LinearLayout.LayoutParams(Ui.dp(this, 37), -2));
        panel.addView(mediaProgressRow);
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

        mediaControls = Ui.row(this);
        previous = cockpitButton("⏮", false);
        play = cockpitButton("▶", true);
        next = cockpitButton("⏭", false);
        previous.setContentDescription("Vorheriger Titel");
        play.setContentDescription("Wiedergabe starten oder pausieren");
        next.setContentDescription("Nächster Titel");
        mediaControls.addView(previous, cockpitActionParams(1f, true));
        mediaControls.addView(play, cockpitActionParams(1f, true));
        mediaControls.addView(next, cockpitActionParams(1f, false));
        panel.addView(mediaControls);
        previous.setOnClickListener(v -> send(SpotifyMedia.Action.PREVIOUS));
        play.setOnClickListener(v -> send(SpotifyMedia.Action.TOGGLE));
        next.setOnClickListener(v -> send(SpotifyMedia.Action.NEXT));

        mediaSkip = Ui.row(this);
        backward = cockpitButton("−15 s", false);
        forward = cockpitButton("+15 s", false);
        backward.setContentDescription("15 Sekunden zurückspringen");
        forward.setContentDescription("15 Sekunden vorspringen");
        mediaSkip.addView(backward, cockpitActionParams(1f, true));
        mediaSkip.addView(forward, cockpitActionParams(1f, false));
        mediaSkip.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 3));
        panel.addView(mediaSkip);
        backward.setOnClickListener(v -> send(SpotifyMedia.Action.SEEK_BACK));
        forward.setOnClickListener(v -> send(SpotifyMedia.Action.SEEK_FORWARD));

        if (!BuildConfig.GOOGLE_ONLY) {
            panel.addView(fallback); // Legacy external Samsung split only.
        }
        Ui.gap(this, panel, 7);
        toggleRoutes = cockpitButton("Ziel / Route", true);
        toggleRoutes.setContentDescription("Zielmenü öffnen und Route berechnen");
        panel.addView(toggleRoutes,
            new LinearLayout.LayoutParams(-1, Ui.dp(this, 46)));
        panel.addView(routePanel);
        toggleRoutes.setOnClickListener(v -> {
            boolean open = routePanel.getVisibility() != View.VISIBLE;
            routePanel.setVisibility(open ? View.VISIBLE : View.GONE);
            toggleRoutes.setText(open ? "Zielmenü schließen" : "Ziel / Route");
            toggleRoutes.setContentDescription(open
                ? "Zielmenü schließen" : "Zielmenü öffnen und Route berechnen");
            if (open) scroll.post(() -> scroll.smoothScrollTo(0,
                routePanel.getTop()));
        });

        Ui.gap(this, panel, 5);
        LinearLayout fuelActions = Ui.row(this);
        fuelButton = cockpitButton("Preise anzeigen", false);
        fuelButton.setContentDescription("Tankstellenpreise auf Karte anzeigen oder ausblenden");
        fuelRefreshButton = cockpitButton("↻", false);
        fuelRefreshButton.setTextSize(20);
        fuelRefreshButton.setContentDescription("Tankstellenpreise neu abrufen");
        fuelActions.addView(fuelButton, cockpitActionParams(1f, true));
        fuelActions.addView(fuelRefreshButton, new LinearLayout.LayoutParams(
            Ui.dp(this, 44), Ui.dp(this, 44)));
        panel.addView(fuelActions);
        fuelInfo = Ui.text(this, "", 10, Ui.MUTED, false);
        fuelInfo.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 2));
        fuelInfo.setMaxLines(2);
        fuelInfo.setEllipsize(android.text.TextUtils.TruncateAt.END);
        panel.addView(fuelInfo);
        fuelButton.setOnClickListener(v -> {
            if (fuelQuickSearch.isSearching()) return;
            if (fuelMarkersVisible
                    && !FuelMapSnapshot.readForDisplay(this).stations.isEmpty()) {
                fuelMarkersVisible = false;
                updateFuelMarkers();
            } else {
                fuelMarkersVisible = true;
                FuelMapSnapshot.Snapshot cached = FuelMapSnapshot.readForDisplay(this);
                if (cached.stations.isEmpty()
                        || !cached.type.equals(FuelQuickSettings.type(this))) {
                    startQuickFuelSearch();
                } else {
                    updateFuelMarkers();
                    // This is an explicit user gesture. Show cached prices at
                    // once; if old, try to refresh subject to shared rate guard.
                    if (FuelMapSnapshot.isStale(cached, System.currentTimeMillis()))
                        startQuickFuelSearch();
                }
            }
        });
        fuelRefreshButton.setOnClickListener(v -> {
            fuelMarkersVisible = true;
            startQuickFuelSearch();
        });
        Ui.gap(this, panel, 7);
        LinearLayout sapheHeader = Ui.row(this);
        TextView sapheLabel = Ui.text(this, "Saphe", 13, Ui.FG, true);
        sapheLabel.setPadding(0, 0, 0, 0);
        sapheHeader.addView(sapheLabel,
            new LinearLayout.LayoutParams(-2, Ui.dp(this, 28)));
        sapheHeader.addView(safetyEventStatus,
            new LinearLayout.LayoutParams(0, -2, 1f));
        panel.addView(sapheHeader);
        refreshSapheStatus();
        LinearLayout sapheActions = Ui.row(this);
        Button saphe = cockpitButton("Saphe öffnen", false);
        sapheActions.addView(saphe,
            new LinearLayout.LayoutParams(-1, Ui.dp(this, 44)));
        panel.addView(sapheActions);
        saphe.setOnClickListener(v -> {
            Intent launch = getPackageManager().getLaunchIntentForPackage("my.saphelink");
            if (launch == null) {
                showStatus("Saphe-App auf diesem Gerät nicht gefunden.");
            } else {
                startActivity(launch);
            }
        });
        // Update and diagnostics are available from the persistent ⋮ menu.
        // Keep the navigation viewport free of billing prompt text.
        // Google route requests remain bound to explicit route actions
        // and the existing local NavigationRequestBudget limit.
        if (isOsmPreview()) {
            panel.addView(Ui.text(this,
                "OSM-Entwicklungstest: nicht als Fahrnavigation verwenden.",
                11, Ui.MUTED, false));
        }
        FrameLayout displayRoot = new FrameLayout(this);
        displayRoot.addView(root, new FrameLayout.LayoutParams(-1, -1));
        displayRoot.addView(sapheBanner, new FrameLayout.LayoutParams(-1, -1));
        setContentView(displayRoot);
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


    // Shared touch-friendly styling; compact enough for the 30 % side pane
    // without turning secondary controls into oversized gray default buttons.
    private Button cockpitButton(String label, boolean primary) {
        Button button = Ui.button(this, label);
        button.setTextSize(getResources().getConfiguration().orientation
            == Configuration.ORIENTATION_LANDSCAPE ? 12 : 13);
        button.setTextColor(Ui.FG);
        button.setMinHeight(Ui.dp(this, 44));
        button.setSingleLine(true);
        button.setEllipsize(android.text.TextUtils.TruncateAt.END);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(label);
        button.setPadding(Ui.dp(this, 6), 0, Ui.dp(this, 6), 0);
        android.graphics.drawable.Drawable background =
            Ui.background(this, primary ? 0xff315d6b : 0xff374452, 9);
        button.setBackground(new android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x55ffffff),
            background, null));
        return button;
    }

    private LinearLayout.LayoutParams cockpitActionParams(float weight,
                                                           boolean gapAfter) {
        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(0, Ui.dp(this, 44), weight);
        if (gapAfter) params.rightMargin = Ui.dp(this, 6);
        return params;
    }

    private void showUnknownSpeed() {
        if (currentSpeedView == null) return;
        currentSpeedView.setText("GPS — km/h");
        currentSpeedView.setContentDescription("GPS-Geschwindigkeit nicht verfügbar");
    }

    private void onSpeedLocation(Location location) {
        if (currentSpeedView == null || location == null
                || !location.hasSpeed() || !location.hasAccuracy()
                || location.getAccuracy() > 80f) return;
        long ageNanos = SystemClock.elapsedRealtimeNanos()
            - location.getElapsedRealtimeNanos();
        if (ageNanos < 0L || ageNanos > 5_000_000_000L) return;
        float metersPerSecond = location.getSpeed();
        if (Float.isNaN(metersPerSecond) || Float.isInfinite(metersPerSecond)
                || metersPerSecond < 0f || metersPerSecond > 85f) return;
        if (android.os.Build.VERSION.SDK_INT >= 26
                && location.hasSpeedAccuracy()
                && location.getSpeedAccuracyMetersPerSecond() > 8f) return;
        parkingTrip.observe(SystemClock.elapsedRealtime(), location.getLatitude(),
            location.getLongitude(), location.getAccuracy(), metersPerSecond);
        trySaveArrivedParking();
        if (navigationGuidanceActive) return; // Google's single native speedometer.
        int kph = Math.round(metersPerSecond * 3.6f);
        currentSpeedView.setText("GPS " + kph + " km/h");
        currentSpeedView.setContentDescription(
            "GPS-Geschwindigkeit " + kph + " Kilometer pro Stunde");
        handler.removeCallbacks(expireSpeedReading);
        handler.postDelayed(expireSpeedReading, 8000L);
    }

    private void startSpeedUpdates() {
        if (!cockpitResumed || currentSpeedView == null || speedLocationListener != null) return;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            showUnknownSpeed();
            return;
        }
        LocationManager manager =
            (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) { showUnknownSpeed(); return; }
        LocationListener listener = this::onSpeedLocation;
        try {
            boolean registered = false;
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    1000L, 0f, listener, Looper.getMainLooper());
                registered = true;
            }
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    2000L, 0f, listener, Looper.getMainLooper());
                registered = true;
            }
            if (registered) {
                speedLocationManager = manager;
                speedLocationListener = listener;
            } else {
                showUnknownSpeed();
            }
        } catch (SecurityException | IllegalArgumentException failure) {
            manager.removeUpdates(listener);
            showUnknownSpeed();
        }
    }

    private void stopSpeedUpdates() {
        if (speedLocationManager != null && speedLocationListener != null) {
            speedLocationManager.removeUpdates(speedLocationListener);
        }
        speedLocationManager = null;
        speedLocationListener = null;
        handler.removeCallbacks(expireSpeedReading);
        showUnknownSpeed();
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
                startSpeedUpdates();
            } else {
                showUnknownSpeed();
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
            .setNeutralButton("Alle Ziele verwalten", (dialog, which) ->
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
        acceptFuelStation(lat, lng, data.getStringExtra(FuelActivity.RESULT_NAME));
    }

    private void acceptFuelStation(double lat, double lng, String displayName) {
        String coords = FuelRules.stationCoordinates(lat, lng);
        if (coords == null) {
            showStatus("Tankstelle hat ungültige Zielkoordinaten.");
            return;
        }
        String name = RouteTargetRules.clean(displayName);
        if (name == null) name = "Tankstelle";
        chosenTarget = name;
        applyDestinationSuggestion(name, coords);
        if (engine == null || !engine.ready()) {
            showStatus("Tankstelle übernommen. Navigation noch nicht bereit.");
            return;
        }
        // A fuel-station handoff is a separate user action. Confirm replacing
        // an existing route; never silently start Google guidance.
        final String chosenName = name;
        new AlertDialog.Builder(this)
            .setTitle("Google Navigation: Tankstelle anfahren")
            .setMessage(chosenName + "\nZiel: " + coords
                + "\n\nDie neue Route kann die bisherige ersetzen.")
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

    private void resolveAndStartRoute() {
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
                if (isOsmPreview()) {
                    // Historical development-only OSM preview still requires
                    // its explicit warning and confirmation.
                    new AlertDialog.Builder(IntegratedNavigationActivity.this)
                        .setTitle("OSM-Abbiegeführung (nicht für Fahrten freigegeben)")
                        .setMessage("OpenStreetMap / BRouter: " + name
                            + "\n\nZielkoordinaten: " + coords
                            + "\n\nEchte Offline-Straßenroute ohne Google-Cloud-Kosten. "
                            + "BRouter-Hinweise, GPS und deutsche Sprachausgabe werden "
                            + "im Entwicklungstest verwendet, falls verfügbar. "
                            + "Noch NICHT im Straßenverkehr geprüft.")
                        .setNegativeButton("Abbrechen", (d,which) -> {})
                        .setPositiveButton("Straßenroute planen", (d,which) -> {
                            if (engine != null && engine.ready()) {
                                DestinationStore.recordRecent(
                                    IntegratedNavigationActivity.this, readable, coords);
                                engine.startConfirmedRoute(lat, lng, readable);
                                collapseDestinationMenu();
                            }
                        })
                        .show();
                    return;
                }
                // The user's deliberate 'Route berechnen' button press is the
                // sole trigger for this Google Navigation SDK route request.
                // Geocoding may be asynchronous: reject an obsolete target,
                // and let the existing NavigationRequestBudget guard every call.
                if (!target.equals(selectedNavigationTarget())) {
                    showStatus("Ziel wurde geändert. Bitte Route erneut berechnen.");
                    return;
                }
                if (engine != null && engine.ready()) {
                    DestinationStore.recordRecent(
                        IntegratedNavigationActivity.this, readable, coords);
                    engine.startConfirmedRoute(lat, lng, readable);
                    collapseDestinationMenu();
                } else {
                    showStatus("Google Navigation ist nicht mehr bereit.");
                }
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
        if (nextAction != null) {
            spotifySetup.setText(nextAction);
            spotifySetup.setContentDescription(nextAction);
        }
        // When Spotify is unavailable, reclaim its disabled transport controls
        // without removing the one-tap Spotify launch action.
        boolean showMedia = state.connected;
        spotifyHeaderRow.setVisibility(showMedia ? View.VISIBLE : View.GONE);
        spotifyHeader.setVisibility(showMedia ? View.VISIBLE : View.GONE);
        spotifyState.setVisibility(showMedia ? View.VISIBLE : View.GONE);
        track.setVisibility(showMedia ? View.VISIBLE : View.GONE);
        mediaControls.setVisibility(showMedia ? View.VISIBLE : View.GONE);
        mediaSkip.setVisibility(showMedia ? View.VISIBLE : View.GONE);
        // Compact visual state, but keep the full status for accessibility.
        String fullSpotifyStatus = state.connected
            ? SpotifyDisplayRules.connectedStatus(state.playing)
            : SpotifyDisplayRules.notConnected(allowed);
        spotifyState.setText(state.connected
            ? (state.playing ? "Spielt" : "Pausiert") : fullSpotifyStatus);
        spotifyState.setContentDescription(fullSpotifyStatus);
        track.setText(state.line);
        // Avoid Samsung's colorful emoji rendering for the pause icon.
        play.setText(state.playing ? "❚❚" : "▶");
        play.setContentDescription(state.playing
            ? "Wiedergabe pausieren" : "Wiedergabe starten");
        play.setEnabled(state.connected);
        previous.setEnabled(state.canPrevious);
        next.setEnabled(state.canNext);
        backward.setEnabled(state.canSeek);
        forward.setEnabled(state.canSeek);
        boolean showProgress = TrackProgressRules.hasDuration(state.durationMs)
            && state.positionMs >= 0;
        totalDuration = state.durationMs;
        mediaProgressRow.setVisibility(showProgress ? View.VISIBLE : View.GONE);
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

    private void startQuickFuelSearch() {
        if (fuelQuickSearch == null || fuelQuickSearch.isSearching()) return;
        fuelButton.setText("Preise laden …");
        fuelButton.setEnabled(false);
        fuelRefreshButton.setEnabled(false);
        fuelInfo.setVisibility(View.VISIBLE);
        fuelInfo.setText("Standort und Tankerkönig-Preise werden abgerufen …");
        fuelQuickSearch.start(new FuelQuickSearch.Result() {
            @Override public void onSuccess(FuelMapSnapshot.Snapshot snapshot) {
                fuelButton.setEnabled(true);
                fuelRefreshButton.setEnabled(true);
                updateFuelMarkers();
                if (snapshot.stations.isEmpty()) {
                    fuelInfo.setVisibility(View.VISIBLE);
                    fuelInfo.setText("Keine gültigen Preise im gewählten Suchradius.");
                }
            }
            @Override public void onFailure(String reason) {
                fuelButton.setEnabled(true);
                fuelRefreshButton.setEnabled(true);
                updateFuelMarkers();
                fuelInfo.setVisibility(View.VISIBLE);
                fuelInfo.setText(reason);
            }
        });
    }

    private void updateFuelMarkers() {
        FuelMapSnapshot.Snapshot snapshot = FuelMapSnapshot.readForDisplay(this);
        boolean valid = !snapshot.stations.isEmpty()
            && snapshot.type.equals(FuelQuickSettings.type(this));
        long now = System.currentTimeMillis();
        boolean stale = valid && FuelMapSnapshot.isStale(snapshot, now);
        if (engine != null) {
            FuelMapSnapshot.Snapshot displayed = fuelMarkersVisible && valid
                ? snapshot : new FuelMapSnapshot.Snapshot("", 0L, new ArrayList<>());
            engine.showFuelPrices(displayed,
                station -> acceptFuelStation(station.lat, station.lng, station.name));
        }
        if (fuelButton != null) {
            fuelButton.setText(fuelMarkersVisible && valid
                ? "Preise ausblenden" : "Preise anzeigen");
        }
        if (fuelInfo != null) {
            if (valid && fuelMarkersVisible) {
                java.text.DateFormat time = java.text.DateFormat.getDateTimeInstance(
                    java.text.DateFormat.SHORT, java.text.DateFormat.SHORT,
                    java.util.Locale.GERMANY);
                fuelInfo.setText(snapshot.stations.size() + " Preise · "
                    + snapshot.type.toUpperCase(java.util.Locale.GERMANY)
                    + (stale ? " · ALT, kein Live-Preis" : " · aktuell")
                    + " · Stand " + time.format(new java.util.Date(snapshot.fetchedAt)));
                fuelInfo.setTextColor(stale ? Ui.WARN : Ui.MUTED);
                fuelInfo.setVisibility(View.VISIBLE);
            } else if (valid) {
                fuelInfo.setText("Preise ausgeblendet · ↻ zum Aktualisieren");
                fuelInfo.setTextColor(Ui.MUTED);
                fuelInfo.setVisibility(View.VISIBLE);
            } else {
                fuelInfo.setVisibility(View.GONE);
            }
        }
        handler.removeCallbacks(expireFuelMarkers);
        if (valid && fuelMarkersVisible) {
            // Update the age indicator after 15 min; remove the display only
            // after the separate 24h retained-cache deadline. Never poll here.
            long deadline = stale ? FuelMapSnapshot.MAX_DISPLAY_AGE_MS
                : FuelMapSnapshot.MAX_AGE_MS;
            long delay = Math.max(1000L, deadline - (now - snapshot.fetchedAt) + 100L);
            handler.postDelayed(expireFuelMarkers, delay);
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
        if (toggleRoutes != null) {
            toggleRoutes.setText("Ziel / Route");
            toggleRoutes.setContentDescription("Zielmenü öffnen und Route berechnen");
        }
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

    private void trySaveArrivedParking() {
        ParkingTripTracker.Spot spot = parkingTrip.takeArrivedParking(
            SystemClock.elapsedRealtime());
        if (spot != null && ParkingLocationStore.save(this, spot, true))
            showStatus("Parkplatz bei Ankunft automatisch gemerkt.");
    }

    private void saveParkingManually() {
        ParkingTripTracker.Spot spot = parkingTrip.manualFix(SystemClock.elapsedRealtime());
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || spot == null) {
            new AlertDialog.Builder(this)
                .setTitle("Parkplatz speichern")
                .setMessage("Es liegt noch kein ausreichend genauer und aktueller GPS-Standort vor. "
                    + "Standort freigeben, einen Moment im Cockpit lassen und erneut tippen. "
                    + "Es wird kein geschätzter Parkplatz gespeichert.")
                .setPositiveButton("Verstanden", null).show();
            return;
        }
        if (ParkingLocationStore.save(this, spot, false))
            showStatus("Parkplatz auf diesem Gerät gespeichert.");
        else showStatus("Parkplatz konnte nicht gespeichert werden.");
    }

    private void showParkingLocation() {
        ParkingLocationStore.Saved saved = ParkingLocationStore.read(this);
        if (saved == null) {
            new AlertDialog.Builder(this)
                .setTitle("Parkplatz")
                .setMessage("Noch kein Parkplatz gespeichert. Nach einer längeren Fahrt "
                    + "kann FahrCockpit beim Beenden der Navigation einen Parkplatz "
                    + "automatisch merken, sofern das Fahrzeug steht. Alternativ "
                    + "„Parkplatz jetzt speichern“ wählen.")
                .setPositiveButton("OK", null).show();
            return;
        }
        java.text.DateFormat time = java.text.DateFormat.getDateTimeInstance(
            java.text.DateFormat.SHORT, java.text.DateFormat.SHORT,
            java.util.Locale.GERMANY);
        String details = (saved.automatic ? "Automatisch nach Fahrtende" : "Manuell")
            + " gespeichert: " + time.format(new java.util.Date(saved.savedAt))
            + "\nGPS-Genauigkeit: ca. " + Math.round(saved.accuracy)
            + " m. Der Standort bleibt ausschließlich auf diesem Smartphone.";
        new AlertDialog.Builder(this)
            .setTitle("Mein geparktes Auto")
            .setMessage(details)
            .setPositiveButton("Fußweg in Google Maps", (dialog, which) -> {
                String url = String.format(java.util.Locale.US,
                    "https://www.google.com/maps/dir/?api=1&destination=%.7f,%.7f&travelmode=walking",
                    saved.lat, saved.lng);
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (RuntimeException unavailable) {
                    showStatus("Google Maps oder Browser konnte nicht geöffnet werden.");
                }
            })
            .setNeutralButton("Parkplatz löschen", (dialog, which) -> {
                showStatus(ParkingLocationStore.clear(this)
                    ? "Gespeicherter Parkplatz gelöscht." : "Parkplatz konnte nicht gelöscht werden.");
            })
            .setNegativeButton("Schließen", null).show();
    }

    private void showSapheBanner(boolean simulated) {
        if (sapheBanner == null) return;
        long now = System.currentTimeMillis();
        if (!simulated && lastSafetyShownAt > 0
                && now - lastSafetyShownAt < SAFETY_COOLDOWN_MS) return;
        // A manual test must not suppress a following genuine Android event.
        if (!simulated) {
            lastSafetyShownAt = now;
            safetyEventCount++;
        }
        String title = simulated ? "TEST · ACHTUNG" : "ACHTUNG";
        String subtitle = simulated
            ? "Safety-Testanzeige · keine echte Warnung"
            : "Safety-Ereignis · Typ nicht verifiziert";
        String message = title + "\n" + subtitle;
        android.text.SpannableString styled = new android.text.SpannableString(message);
        styled.setSpan(new android.text.style.RelativeSizeSpan(0.33f),
            title.length() + 1, message.length(),
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sapheBanner.setText(styled);
        sapheBanner.setContentDescription(message);
        sapheBanner.clearAnimation();
        sapheBanner.setVisibility(View.VISIBLE);
        // Gentle, legible red pulses instead of rapid full-black strobing.
        AlphaAnimation pulse = new AlphaAnimation(0.78f, 1f);
        pulse.setDuration(360L);
        pulse.setRepeatCount(3);
        pulse.setRepeatMode(Animation.REVERSE);
        sapheBanner.startAnimation(pulse);
        handler.removeCallbacks(hideSapheBanner);
        handler.postDelayed(hideSapheBanner, 3200L);
        if (!simulated) refreshSapheStatus();
    }

    private void refreshSapheStatus() {
        if (safetyEventStatus == null) return;
        boolean enabled = SapheNotificationProbe.enabled(this);
        String summary = enabled ? "Erkennung an" : "Erkennung aus";
        String accessibility = "Saphe: " + summary;
        if (safetyEventCount > 0) {
            java.text.DateFormat clock = java.text.DateFormat.getTimeInstance(
                java.text.DateFormat.SHORT, java.util.Locale.GERMANY);
            String time = clock.format(new java.util.Date(lastSafetyShownAt));
            summary = (enabled ? "An" : "Aus") + " · "
                + safetyEventCount + " · " + time;
            accessibility += ", " + safetyEventCount
                + " empfangene Ereignisse, zuletzt " + time;
        }
        // Enabled means that notification detection was enabled by the user,
        // not that the hardware or real-world warning type was verified.
        safetyEventStatus.setText(summary);
        safetyEventStatus.setContentDescription(accessibility);
    }

    private void refreshSapheNotificationProbe() {
        refreshSapheStatus();
        if (sapheBanner == null) return;
        long observedAt = SapheNotificationProbe.enabled(this)
            ? SapheNotificationProbe.lastObservedAt(this) : 0L;
        long diagnosticAt = SapheDiagnosticSession.active(this, System.currentTimeMillis())
            ? SapheDiagnosticSession.lastSapheAt(this) : 0L;
        // Both probes can observe one Android notification. Use the later
        // timestamp as a single visual event rather than double counting.
        observedAt = Math.max(observedAt, diagnosticAt);
        if (observedAt > sapheLastShownAt) {
            sapheLastShownAt = observedAt;
            if (SapheNotificationRules.isRecent(
                    observedAt, System.currentTimeMillis())) showSapheBanner(false);
        }
    }

    private void sendSapheReceiverSelfTest() {
        long now = System.currentTimeMillis();
        if (!SapheDiagnosticSession.active(this, now)) {
            showStatus("Zuerst die 10-Minuten-Saphe-Diagnose starten.");
            return;
        }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this)
                .setTitle("Benachrichtigungstest nicht verfügbar")
                .setMessage("FahrCockpit hat keine Android-Mitteilungsberechtigung. "
                    + "Die Saphe-Erkennung bleibt davon getrennt. "
                    + "Du kannst die Berechtigung in den Android-App-Einstellungen "
                    + "prüfen; es wird nichts automatisch geändert.")
                .setPositiveButton("Verstanden", (dialog, which) -> {})
                .show();
            return;
        }
        NotificationManager manager =
            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null || !SapheDiagnosticSession.requestSelfTest(this, now)) {
            showStatus("Saphe-Empfängertest konnte nicht gestartet werden.");
            return;
        }
        try {
            String channelId = "fahrcockpit_saphe_receiver_test";
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel channel = new NotificationChannel(channelId,
                    "FahrCockpit Diagnose", NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("Einmaliger lokaler Empfängertest, keine Saphe-Warnung");
                channel.enableVibration(false);
                channel.setSound(null, null);
                manager.createNotificationChannel(channel);
            }
            Notification notification = new Notification.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("FahrCockpit Empfängertest")
                .setContentText("Lokaler Test – keine Saphe-Warnung")
                .setAutoCancel(true)
                .setDefaults(0)
                .setOnlyAlertOnce(true)
                .build();
            manager.notify(31031, notification);
            handler.postDelayed(() -> {
                manager.cancel(31031);
                if (!isFinishing() && !isDestroyed()) showSapheProbeDialog();
            }, 1600L);
        } catch (RuntimeException exception) {
            SapheDiagnosticSession.stop(this);
            showStatus("Lokaler Mitteilungstest konnte nicht gesendet werden.");
        }
    }

    private void showSapheProbeDialog() {
        long now = System.currentTimeMillis();
        boolean hasAndroidAccess = Permissions.isMediaListenerEnabled(this);
        boolean optedIn = SapheNotificationProbe.enabled(this);
        boolean active = SapheDiagnosticSession.active(this, now);
        long last = SapheDiagnosticSession.lastSapheAt(this);
        int count = SapheDiagnosticSession.sapheCount(this);
        long seen = SapheNotificationProbe.lastObservedAt(this);
        long connected = SapheDiagnosticSession.listenerConnectedAt(this);
        boolean listenerSeen = active && connected >= now - SapheDiagnosticSession.DURATION_MS;
        java.text.DateFormat clock = java.text.DateFormat.getTimeInstance(
            java.text.DateFormat.SHORT, java.util.Locale.GERMANY);
        String details = "Erkennt nur Android-Benachrichtigungen der offiziellen "
            + "Saphe-Link-App. Keine Bluetooth- oder Blitzerwarnschnittstelle. "
            + "Es werden keine Mitteilungstexte gelesen.\n\n"
            + "Android-Zugriff: " + (hasAndroidAccess ? "erlaubt" : "fehlt")
            + "\nBenachrichtigungsempfänger: " + (listenerSeen
                ? "aktiv bestätigt" : "noch nicht bestätigt")
            + "\nBestehende Saphe-Erkennung: " + (optedIn ? "eingeschaltet" : "ausgeschaltet")
            + "\n10-Minuten-Diagnose: " + (active ? "aktiv" : "nicht aktiv")
            + (active ? " (bis " + clock.format(new java.util.Date(
                SapheDiagnosticSession.endsAt(this))) + ")" : "")
            + "\nSaphe-Mitteilungen im Test: " + (active ? count : 0)
            + "\nLetzte Saphe-Mitteilung: " + (active && last > 0
                ? clock.format(new java.util.Date(last)) : "keine")
            + "\nLokaler Android-Empfängertest: " +
                (SapheDiagnosticSession.selfTestSeen(this) ? "erfolgreich" : "noch nicht bestätigt")
            + "\nAlte Saphe-Erkennung: " + (seen > 0 ? "ja" : "noch nein")
            + "\n\nDieser Test ist für das stehende Fahrzeug vorgesehen.";
        // Extra action is explicitly in this dialog; it does not claim that
        // Saphe warnings or the device's Bluetooth events have been received.
        LinearLayout content = Ui.column(this);
        content.setPadding(Ui.dp(this, 18), Ui.dp(this, 8),
            Ui.dp(this, 18), Ui.dp(this, 8));
        TextView summary = Ui.text(this, details, 14, Ui.FG, false);
        content.addView(summary);
        Button receiverTest = Ui.button(this, "Android-Empfänger testen");
        content.addView(receiverTest);
        AlertDialog sapheDialog = new AlertDialog.Builder(this)
            .setTitle("Saphe-Diagnose")
            .setView(content)
            .setNeutralButton("Testanzeige", (dialog, which) ->
                showSapheBanner(true))
            .setNegativeButton("Schließen", (dialog, which) -> {})
            .setPositiveButton(!hasAndroidAccess ? "Android-Zugriff prüfen"
                : active ? "Diagnose beenden" : "10 Minuten starten",
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
                    if (active) {
                        SapheDiagnosticSession.stop(this);
                        showStatus("Saphe-Diagnose beendet; keine neuen Ereignisse werden erfasst.");
                    } else {
                        // Independent temporary session: preserve legacy opt-in unchanged.
                        if (!SapheDiagnosticSession.start(this, System.currentTimeMillis())) {
                            showStatus("Saphe-Diagnose konnte nicht gestartet werden.");
                            return;
                        }
                        showStatus("Saphe-Diagnose für 10 Minuten aktiviert.");
                    }
                })
            .create();
        receiverTest.setOnClickListener(v -> {
            sapheDialog.dismiss();
            sendSapheReceiverSelfTest();
        });
        sapheDialog.show();
    }

    private void showStatus(String message) {
        if (status == null) return;
        // Routine SDK guidance status consumes scarce screen space. Errors,
        // warnings and user actions remain visible (including Saphe feedback).
        boolean routine = BuildConfig.GOOGLE_ONLY && message != null
            && (message.contains("Echte Google-Abbiegenavigation")
                || message.equals("Google Navigation SDK wird vorbereitet …")
                || message.startsWith("Google Navigation bereit"));
        status.setText(message);
        status.setVisibility(routine ? View.GONE : View.VISIBLE);
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
        cockpitResumed = true;
        startSpeedUpdates();
        if (autoFuelRefreshController != null)
            autoFuelRefreshController.setVisible(true);
        if (engine != null) {
            engine.onResume();
            // Only display prices previously requested via the Tankerkoenig
            // search. No background polling or Google billable route calls.
            updateFuelMarkers();
        }
    }
    @Override protected void onPause() {
        if (autoFuelRefreshController != null)
            autoFuelRefreshController.setVisible(false);
        stopSpeedUpdates();
        cockpitResumed = false;
        if (engine != null) engine.onPause();
        super.onPause();
    }
    @Override protected void onStop() {
        if (autoFuelRefreshController != null)
            autoFuelRefreshController.setVisible(false);
        handler.removeCallbacks(musicRefresh);
        handler.removeCallbacks(expireFuelMarkers);
        if (fuelQuickSearch != null) fuelQuickSearch.cancel();
        if (engine != null) engine.onStop();
        super.onStop();
    }
    @Override protected void onDestroy() {
        if (autoFuelRefreshController != null)
            autoFuelRefreshController.shutdown();
        stopSpeedUpdates();
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
