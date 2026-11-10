package com.cobrachris.fahrcockpit;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
 * The HTTPS map host starts OpenStreetMap without a key. Google Maps JS demo
 * is optional and never required to use the interface.
 * Neither map is a turn-by-turn driving navigator.
 */
public final class EmbeddedMapActivity extends Activity {
    private static final int REQUEST_GEO = 317;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WebView webView;
    private TextView musicStatus, track, elapsed, duration;
    private Button play, previous, next, back15, forward15;
    private SeekBar progress;
    private boolean touchingProgress;
    private long durationMs = -1L;
    private GeolocationPermissions.Callback pendingGeo;
    private String pendingGeoOrigin;

    private final Runnable refreshMusic = new Runnable() {
        @Override public void run() {
            if (track != null) updateMusic();
            handler.postDelayed(this, 1800L);
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        // Identify this WebView to community OSM tile servers; no impersonation.
        webView.getSettings().setUserAgentString(
            webView.getSettings().getUserAgentString()
                + " FahrCockpit/0.9.13-beta (+https://github.com/CobraChris/app-fabrik-template)");
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
                String target = request.getUrl().toString();
                if (EmbeddedMapRules.allowsPage(target)) return false;
                // Permit OSM's legally required attribution link outside the
                // embedded map without granting arbitrary in-app navigation.
                if ("https://www.openstreetmap.org/copyright".equals(target)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()));
                    } catch (RuntimeException ignored) {
                        // Still do not navigate the map WebView elsewhere.
                    }
                }
                return true;
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
        content.addView(Ui.text(this, "FahrCockpit · Spotify", 19, Ui.FG, true));
        musicStatus = Ui.text(this, "Medienstatus wird geprüft …", 12, Ui.MUTED, false);
        content.addView(musicStatus);
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
            "Die integrierte OSM-Karte startet ohne Schlüssel. Keine Abbiege- " +
            "oder Sprachführung: Für Fahrnavigation Google Maps öffnen.",
            11, Ui.WARN, false));
        Button settings = Ui.button(this, "FahrCockpit-Einstellungen");
        content.addView(settings);
        settings.setOnClickListener(v ->
            startActivity(new Intent(this, MainActivity.class)));
    }

    private void send(SpotifyMedia.Action action) {
        musicStatus.setText(SpotifyMedia.send(this, action));
        handler.postDelayed(this::updateMusic, 500L);
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
        if (!state.connected) musicStatus.setText(
            Permissions.isMediaListenerEnabled(this)
                ? "Spotify öffnen und Musik starten."
                : "Spotify-Medienzugriff in FahrCockpit freigeben.");
        else if (musicStatus.getText().toString().startsWith("Spotify "))
            musicStatus.setText("Spotify verbunden");
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
