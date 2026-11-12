package com.cobrachris.fahrcockpit;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * FahrCockpit: a REAL allocated map viewport and native Spotify controls in
 * independent layout panes. Neither pane overlays the other.
 *
 * Only Google Maps JavaScript is embedded. An existing Demo Key can be
 * transferred with a one-time encrypted browser handoff without typing.
 * This is not turn-by-turn driving navigation.
 */
public final class EmbeddedMapActivity extends Activity {
    private static final int REQUEST_GEO = 317;
    private static final String ROUTES_PREFS = "fahrcockpit_manual_routes_v1";
    private static final String TARGET_STATE = "selected_route_target";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WebView webView;
    private TextView musicStatus, track, elapsed, duration, routeStatus;
    private String currentTarget;
    private Button play, previous, next, back15, forward15, spotifyAction;
    private SeekBar progress;
    private boolean touchingProgress;
    private long durationMs = -1L;
    private GeolocationPermissions.Callback pendingGeo;
    private String pendingGeoOrigin;
    private String importedGoogleKey;

    private final Runnable refreshMusic = new Runnable() {
        @Override public void run() {
            if (track != null) updateMusic();
            handler.postDelayed(this, 1800L);
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        importedGoogleKey = getIntent().getStringExtra(GoogleKeyTransfer.EXTRA_KEY);
        if (savedInstanceState != null) {
            currentTarget = savedInstanceState.getString(TARGET_STATE);
        }
        // Never let an old overlay cover the independent Google map viewport.
        stopService(new Intent(this, OverlayService.class));
        stopService(new Intent(this, DockedCockpitService.class));

        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        boolean landscape = getResources().getConfiguration().orientation
            == Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(landscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        setContentView(root);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars =
                insets.getInsets(android.view.WindowInsets.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        webView = new WebView(this);
        webView.setBackgroundColor(Ui.BG);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        webView.getSettings().setGeolocationEnabled(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view,
                    WebResourceRequest request) {
                // Subresources from Google are loaded normally. Never navigate the
                // main frame away from the dedicated map page.
                if (!request.isForMainFrame()) return false;
                Uri targetUri = request.getUrl();
                if ("fahrcockpit".equalsIgnoreCase(targetUri.getScheme())
                        && "destination".equalsIgnoreCase(targetUri.getHost())) {
                    // This unexported Activity only accepts map taps from the
                    // pinned HTTPS first-party WebView, never arbitrary pages.
                    if (EmbeddedMapRules.allowsPage(view.getUrl())) {
                        runOnUiThread(() -> acceptMapPoint(targetUri));
                    }
                    return true;
                }
                if (GoogleKeyTransfer.isImportStart(targetUri)) {
                    if (!GoogleKeyTransfer.openBrowser(EmbeddedMapActivity.this)
                            && musicStatus != null) {
                        musicStatus.setText("Browser für Google-Übernahme nicht verfügbar.");
                    }
                    return true;
                }
                // No navigation outside our pinned HTTPS map host.
                return !EmbeddedMapRules.allowsPage(request.getUrl().toString());
            }

            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (EmbeddedMapRules.allowsPage(url)) injectImportedGoogleKey();
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onGeolocationPermissionsShowPrompt(
                    String origin, GeolocationPermissions.Callback callback) {
                // Geolocation only happens after the user's map-position tap.
                // No broad JavaScript bridge, no silent location permission.
                if (!EmbeddedMapRules.allowsOrigin(origin)) {
                    callback.invoke(origin, false, false);
                    return;
                }
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                        || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false);
                } else {
                    pendingGeo = callback;
                    pendingGeoOrigin = origin;
                    requestPermissions(new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    }, REQUEST_GEO);
                }
            }
        });
        // No JavascriptInterface: the web page cannot invoke native media APIs.
        webView.loadUrl(EmbeddedMapRules.URL);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.PANEL);
        LinearLayout controlPanel = Ui.column(this);
        controlPanel.setPadding(Ui.dp(this, 12), Ui.dp(this, 6),
            Ui.dp(this, 12), Ui.dp(this, 12));
        scroll.addView(controlPanel);
        if (landscape) {
            root.addView(webView, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 7f));
            root.addView(scroll, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 3f));
        } else {
            root.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 7f));
            root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 3f));
        }
        buildControls(controlPanel);
    }

    private Button mediaButton(String text, LinearLayout row) {
        Button button = Ui.button(this, text);
        button.setMinWidth(0);
        button.setTextSize(16);
        row.addView(button, new LinearLayout.LayoutParams(
            0, Ui.dp(this, 50), 1));
        return button;
    }

    private void buildControls(LinearLayout content) {
        Button chooseRoute = Ui.button(this, "📍 Ziel / Route wählen");
        content.addView(chooseRoute);
        chooseRoute.setOnClickListener(v -> showDestinationDialog(currentTarget));
        Button splitNavigation = Ui.button(this,
            "▶ Google-Navigation + Spotify geteilt starten");
        content.addView(splitNavigation);
        splitNavigation.setOnClickListener(v -> {
            if (RouteTargetRules.clean(currentTarget) == null) {
                showDestinationDialog(null);
            } else {
                launchSplitNavigation(currentTarget);
            }
        });
        routeStatus = Ui.text(this,
            currentTarget == null ? "Google-Route auswählen · Navigation in Google Maps"
                : "Ziel: " + currentTarget,
            11, Ui.MUTED, false);
        routeStatus.setMaxLines(2);
        routeStatus.setVisibility(currentTarget == null ? View.GONE : View.VISIBLE);
        content.addView(routeStatus);
        content.addView(Ui.text(this, "FahrCockpit · Spotify", 19, Ui.FG, true));
        musicStatus = Ui.text(this, "Medienstatus wird geprüft …", 12, Ui.MUTED, false);
        content.addView(musicStatus);
        // This is the ONLY onboarding button needed inside the actual driving
        // pane. Hidden as soon as Android exposes an active Spotify session.
        spotifyAction = Ui.button(this, "Spotify öffnen");
        content.addView(spotifyAction);
        spotifyAction.setOnClickListener(v -> runSpotifyNextStep());
        track = Ui.text(this, "–", 14, Ui.FG, true);
        track.setMaxLines(2);
        content.addView(track);
        progress = new SeekBar(this);
        progress.setMax(TrackProgressRules.MAX);
        progress.setEnabled(false);
        content.addView(progress);
        LinearLayout times = Ui.row(this);
        elapsed = Ui.text(this, "--:--", 11, Ui.MUTED, false);
        duration = Ui.text(this, "--:--", 11, Ui.MUTED, false);
        times.addView(elapsed, new LinearLayout.LayoutParams(0, -2, 1f));
        times.addView(duration);
        content.addView(times);
        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar bar) {
                touchingProgress = true;
            }
            @Override public void onProgressChanged(SeekBar bar, int value,
                    boolean fromUser) {
                if (fromUser) elapsed.setText(TrackProgressRules.time(
                    TrackProgressRules.targetPosition(value, durationMs)));
            }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                touchingProgress = false;
                long target = TrackProgressRules.targetPosition(
                    bar.getProgress(), durationMs);
                if (target >= 0) musicStatus.setText(
                    SpotifyMedia.seekAbsolute(EmbeddedMapActivity.this, target));
            }
        });
        LinearLayout musicRow = Ui.row(this);
        previous = mediaButton("⏮", musicRow);
        play = mediaButton("▶", musicRow);
        next = mediaButton("⏭", musicRow);
        content.addView(musicRow);
        previous.setOnClickListener(v -> send(SpotifyMedia.Action.PREVIOUS));
        play.setOnClickListener(v -> send(SpotifyMedia.Action.TOGGLE));
        next.setOnClickListener(v -> send(SpotifyMedia.Action.NEXT));
        LinearLayout seekRow = Ui.row(this);
        back15 = mediaButton("−15 s", seekRow);
        forward15 = mediaButton("+15 s", seekRow);
        content.addView(seekRow);
        back15.setOnClickListener(v -> send(SpotifyMedia.Action.SEEK_BACK));
        forward15.setOnClickListener(v -> send(SpotifyMedia.Action.SEEK_FORWARD));

        Button fuel = Ui.button(this, "⛽ Tankstellen");
        content.addView(fuel);
        fuel.setOnClickListener(v ->
            startActivity(new Intent(this, FuelActivity.class)));
        Button saphe = Ui.button(this, "Saphe öffnen");
        content.addView(saphe);
        saphe.setOnClickListener(v -> musicStatus.setText(
            WarningSource.openSaphe(this)
                ? "Saphe geöffnet. Hintergrundstatus nicht geprüft."
                : "Saphe ist nicht verfügbar."));
        Button maps = Ui.button(this, "↗ Richtige Google-Maps-Navigation öffnen");
        content.addView(maps);
        maps.setOnClickListener(v -> {
            if (GoogleMapsLaunch.installed(this))
                startActivity(GoogleMapsLaunch.mapIntent());
            else musicStatus.setText("Google Maps ist nicht installiert.");
        });
        content.addView(Ui.text(this,
            "Google-Kartenprototyp ohne OpenStreetMap. Beim ersten Start den " +
            "vorhandenen Google-Demoschlüssel aus deiner Live-Karte übernehmen. " +
            "Keine Abbiege- oder Sprachführung: Für Fahrt-Navigation Google Maps öffnen.",
            11, Ui.WARN, false));
        Button settings = Ui.button(this, "FahrCockpit-Einstellungen");
        content.addView(settings);
        settings.setOnClickListener(v ->
            startActivity(new Intent(this, MainActivity.class)));
    }

    private void acceptMapPoint(Uri uri) {
        if (uri == null || !"fahrcockpit".equalsIgnoreCase(uri.getScheme())
                || !"destination".equalsIgnoreCase(uri.getHost())) return;
        String chosen = RouteTargetRules.coordinates(
            uri.getQueryParameter("lat"), uri.getQueryParameter("lng"));
        if (chosen == null) {
            if (routeStatus != null) routeStatus.setText("Ungültiger Kartenpunkt.");
            return;
        }
        currentTarget = chosen;
        showDestinationDialog(chosen); // User chooses an action; never auto-navigate.
    }

    private void selectedTarget(String target) {
        currentTarget = target;
        if (routeStatus != null) {
            routeStatus.setText("Ziel: " + target);
            routeStatus.setVisibility(View.VISIBLE);
        }
    }

    private java.util.List<String> savedDestinations() {
        SharedPreferences prefs = getSharedPreferences(ROUTES_PREFS, MODE_PRIVATE);
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String value = RouteTargetRules.clean(prefs.getString("target_" + i, ""));
            if (value != null && !result.contains(value)) result.add(value);
        }
        return result;
    }

    private void rememberDestination(String target) {
        java.util.List<String> list = savedDestinations();
        SharedPreferences.Editor editor =
            getSharedPreferences(ROUTES_PREFS, MODE_PRIVATE).edit();
        editor.clear();
        editor.putString("target_0", target);
        int slot = 1;
        for (String previous : list) {
            if (previous.equalsIgnoreCase(target)) continue;
            if (slot >= 5) break;
            editor.putString("target_" + slot, previous);
            slot++;
        }
        editor.apply();
    }

    private void launchSplitNavigation(String target) {
        String chosen = RouteTargetRules.clean(target);
        if (chosen == null) {
            Toast.makeText(this, "Bitte zuerst ein Ziel auswählen.",
                Toast.LENGTH_SHORT).show();
            return;
        }
        if (!GoogleMapsLaunch.installed(this)) {
            if (routeStatus != null) {
                routeStatus.setText("Google Maps fehlt · geteilter Bildschirm nicht gestartet.");
            }
            return;
        }
        selectedTarget(chosen);
        try {
            // The companion cockpit remains visible in a separate Android
            // window if Samsung honors LAUNCH_ADJACENT. Never use an overlay.
            startActivity(new Intent(this, SplitCockpitActivity.class)
                .putExtra(SplitCockpitActivity.EXTRA_NAVIGATION_TARGET, chosen));
        } catch (RuntimeException unavailable) {
            routeStatus.setText("Geteilte Fahransicht konnte nicht gestartet werden.");
        }
    }

    private void launchGoogleRoute(String target, boolean navigate) {
        String cleaned = RouteTargetRules.clean(target);
        if (cleaned == null) {
            Toast.makeText(this, "Bitte ein gültiges Ziel eingeben.", Toast.LENGTH_SHORT).show();
            return;
        }
        selectedTarget(cleaned);
        if (!GoogleMapsLaunch.installed(this)) {
            routeStatus.setText("Google Maps ist nicht installiert · Route nicht gestartet.");
            return;
        }
        Intent route = navigate ? GoogleMapsLaunch.navigationIntent(cleaned)
            : GoogleMapsLaunch.directionsIntent(cleaned);
        if (route == null) return;
        try {
            startActivity(route);
            routeStatus.setText("Ziel: " + cleaned + " · Google Maps geöffnet");
        } catch (RuntimeException unavailable) {
            routeStatus.setText("Google Maps konnte nicht geöffnet werden.");
        }
    }

    private void showDestinationDialog(String proposed) {
        final LinearLayout column = Ui.column(this);
        column.setPadding(Ui.dp(this, 15), Ui.dp(this, 5),
            Ui.dp(this, 15), Ui.dp(this, 10));
        TextView explanation = Ui.text(this,
            "Adresse, Ortsname oder Koordinaten. Die Route wird in der "
            + "installierten Google-Maps-App berechnet, nicht in dieser Karte.",
            12, Ui.MUTED, false);
        column.addView(explanation);
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextColor(Ui.FG);
        input.setHintTextColor(Ui.MUTED);
        input.setHint("Ziel, Adresse oder Ort");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setText(proposed == null ? "" : proposed);
        column.addView(input);

        android.widget.ScrollView scroller = new android.widget.ScrollView(this);
        scroller.setFillViewport(false);
        scroller.addView(column);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Google-Navigation")
            .setView(scroller)
            .setNegativeButton("Schließen", (d, which) -> {})
            .create();

        java.util.List<String> recent = savedDestinations();
        if (!recent.isEmpty()) {
            column.addView(Ui.text(this, "Gespeicherte Ziele", 13, Ui.FG, true));
            for (String remembered : recent) {
                Button item = Ui.button(this, remembered);
                item.setMaxLines(1);
                column.addView(item);
                item.setOnClickListener(v -> input.setText(remembered));
            }
            Button clear = Ui.button(this, "Gespeicherte Ziele löschen");
            column.addView(clear);
            clear.setOnClickListener(v -> {
                getSharedPreferences(ROUTES_PREFS, MODE_PRIVATE).edit().clear().apply();
                dialog.dismiss();
                showDestinationDialog(input.getText().toString());
            });
        }

        Button save = Ui.button(this, "Ziel merken");
        column.addView(save);
        save.setOnClickListener(v -> {
            String target = RouteTargetRules.clean(input.getText().toString());
            if (target == null) {
                input.setError("Bitte ein gültiges Ziel eingeben (max. 160 Zeichen).");
                return;
            }
            selectedTarget(target);
            rememberDestination(target);
            Toast.makeText(this, "Ziel gespeichert.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        Button preview = Ui.button(this, "Route in Google Maps ansehen");
        column.addView(preview);
        preview.setOnClickListener(v -> {
            String target = RouteTargetRules.clean(input.getText().toString());
            if (target == null) {
                input.setError("Bitte ein gültiges Ziel eingeben (max. 160 Zeichen).");
                return;
            }
            dialog.dismiss();
            launchGoogleRoute(target, false);
        });

        Button split = Ui.button(this,
            "Google-Navigation + Spotify im Splitscreen starten");
        column.addView(split);
        split.setOnClickListener(v -> {
            String target = RouteTargetRules.clean(input.getText().toString());
            if (target == null) {
                input.setError("Bitte ein gültiges Ziel eingeben (max. 160 Zeichen).");
                return;
            }
            dialog.dismiss();
            launchSplitNavigation(target);
        });

        Button start = Ui.button(this, "Autonavigation in Google Maps starten");
        column.addView(start);
        start.setOnClickListener(v -> {
            String target = RouteTargetRules.clean(input.getText().toString());
            if (target == null) {
                input.setError("Bitte ein gültiges Ziel eingeben (max. 160 Zeichen).");
                return;
            }
            dialog.dismiss();
            launchGoogleRoute(target, true);
        });
        dialog.show();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (currentTarget != null) state.putString(TARGET_STATE, currentTarget);
        super.onSaveInstanceState(state);
    }

    private void injectImportedGoogleKey() {
        String key = importedGoogleKey;
        if (webView == null || !GoogleKeyTransfer.validKey(key)) return;
        if (!EmbeddedMapRules.allowsPage(webView.getUrl())) return;
        importedGoogleKey = null;
        // JSON escaping and strict key validation prevent JS injection.
        String js = "localStorage.setItem('fahrcockpit-map-demo-key-v1', "
            + org.json.JSONObject.quote(key) + "); location.reload();";
        webView.evaluateJavascript(js, ignored -> {});
    }

    @Override protected void onNewIntent(Intent incoming) {
        super.onNewIntent(incoming);
        setIntent(incoming);
        String received = incoming.getStringExtra(GoogleKeyTransfer.EXTRA_KEY);
        if (GoogleKeyTransfer.validKey(received)) {
            importedGoogleKey = received;
            injectImportedGoogleKey();
        }
    }

    private void runSpotifyNextStep() {
        // Never request notification-listener access implicitly. Android owns
        // the consent UI, and we guide the user to the exact setting.
        if (!Permissions.isMediaListenerEnabled(this)) {
            musicStatus.setText("Android-Einstellungen → Benachrichtigungszugriff → "
                + "FahrCockpit (Test) aktivieren. Danach mit Zurück zurückkehren.");
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } catch (RuntimeException unavailable) {
                musicStatus.setText("Android-Einstellungen → Apps → Spezieller Zugriff → "
                    + "Benachrichtigungszugriff → FahrCockpit (Test) einschalten.");
            }
            return;
        }
        Intent spotify = getPackageManager().getLaunchIntentForPackage("com.spotify.music");
        if (spotify == null) {
            musicStatus.setText("Spotify ist nicht installiert oder lässt sich nicht öffnen.");
            return;
        }
        try {
            startActivity(spotify);
            musicStatus.setText("Spotify-App: Titel starten, danach mit Zurück "
                + "wieder zu FahrCockpit wechseln.");
        } catch (RuntimeException error) {
            musicStatus.setText("Spotify konnte nicht geöffnet werden.");
        }
    }

    private void send(SpotifyMedia.Action action) {
        musicStatus.setText(SpotifyMedia.send(this, action));
        handler.postDelayed(this::updateMusic, 500L);
    }

    private void updateMusic() {
        SpotifyMedia.State state = SpotifyMedia.read(this);
        boolean listenerGranted = Permissions.isMediaListenerEnabled(this);
        String nextAction = SpotifyDisplayRules.nextAction(listenerGranted, state.connected);
        spotifyAction.setVisibility(nextAction == null ? View.GONE : View.VISIBLE);
        if (nextAction != null) spotifyAction.setText(nextAction);
        track.setVisibility(state.connected ? View.VISIBLE : View.GONE);
        track.setText(state.line);
        play.setText(state.playing ? "⏸" : "▶");
        play.setEnabled(state.connected);
        previous.setEnabled(state.canPrevious);
        next.setEnabled(state.canNext);
        back15.setEnabled(state.canSeek);
        forward15.setEnabled(state.canSeek);
        boolean hasProgress = TrackProgressRules.hasDuration(state.durationMs)
            && state.positionMs >= 0;
        durationMs = state.durationMs;
        progress.setVisibility(hasProgress ? View.VISIBLE : View.GONE);
        elapsed.setVisibility(hasProgress ? View.VISIBLE : View.GONE);
        duration.setVisibility(hasProgress ? View.VISIBLE : View.GONE);
        progress.setEnabled(hasProgress && state.canSeek);
        if (hasProgress) {
            if (!touchingProgress) {
                progress.setProgress(TrackProgressRules.sliderPosition(
                    state.positionMs, state.durationMs));
                elapsed.setText(TrackProgressRules.time(state.positionMs));
            }
            duration.setText(TrackProgressRules.time(state.durationMs));
        }
        if (!state.connected) {
            musicStatus.setText(SpotifyDisplayRules.notConnected(listenerGranted));
        } else {
            musicStatus.setText(SpotifyDisplayRules.connectedStatus(state.playing));
        }
    }

    @Override public void onRequestPermissionsResult(int code,
            String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
        if (code == REQUEST_GEO && pendingGeo != null) {
            boolean granted = false;
            for (int grant : grants)
                if (grant == PackageManager.PERMISSION_GRANTED) granted = true;
            pendingGeo.invoke(pendingGeoOrigin, granted, false);
            pendingGeo = null;
            pendingGeoOrigin = null;
        }
    }

    @Override protected void onStart() {
        super.onStart();
        stopService(new Intent(this, OverlayService.class));
        stopService(new Intent(this, DockedCockpitService.class));
        handler.removeCallbacks(refreshMusic);
        handler.post(refreshMusic);
    }
    @Override protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }
    @Override protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }
    @Override protected void onStop() {
        handler.removeCallbacks(refreshMusic);
        super.onStop();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (pendingGeo != null) {
            pendingGeo.invoke(pendingGeoOrigin, false, false);
            pendingGeo = null;
        }
        if (webView != null) {
            ((ViewGroup)webView.getParent()).removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
