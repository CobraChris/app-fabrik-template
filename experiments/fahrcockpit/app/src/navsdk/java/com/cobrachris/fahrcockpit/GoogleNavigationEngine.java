package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import com.google.android.libraries.navigation.ListenableResultFuture;
import com.google.android.libraries.navigation.NavigationApi;
import com.google.android.libraries.navigation.NavigationView;
import com.google.android.libraries.navigation.Navigator;
import com.google.android.libraries.navigation.RoutingOptions;
import com.google.android.libraries.navigation.Waypoint;

/**
 * Google Navigation SDK adapter. Included only in builds explicitly compiled
 * with -PwithGoogleNavSdk=true. No SDK initialization if no restricted key is
 * configured, and NO route request until the user confirms a destination.
 */
public final class GoogleNavigationEngine implements NavigationEngine {
    private static boolean apiKeyInitialized;
    private final Activity host;
    private final String apiKey;
    private NavigationView map;
    private Navigator navigator;
    private Status status = value -> {};
    private boolean startedRoute;
    private boolean calculating;
    private int routeGeneration;
    private boolean closed;

    public GoogleNavigationEngine(Activity host) {
        this.host = host;
        this.apiKey = BuildConfig.NAVSDK_API_KEY;
    }

    @Override public boolean configured() {
        return apiKey != null && apiKey.startsWith("AIza")
            && apiKey.length() >= 35 && apiKey.length() <= 95;
    }

    @Override public View createNavigationView(Bundle saved, Status callback) {
        status = callback;
        if (!configured()) throw new IllegalStateException("Google Navigation SDK key missing");
        // Must happen before constructing any Navigation SDK view or navigator.
        if (!apiKeyInitialized) {
            NavigationApi.setApiKey(apiKey);
            apiKeyInitialized = true;
        }
        map = new NavigationView(host);
        map.onCreate(saved);
        return map;
    }

    @Override public void initializeAfterLocationPermission() {
        if (!configured() || map == null || closed) return;
        status.onStatus("Google Navigation SDK · Nutzungsbedingungen werden geprüft …");
        NavigationApi.getNavigator(host, new NavigationApi.NavigatorListener() {
            @Override public void onNavigatorReady(Navigator result) {
                if (closed || host.isFinishing() || host.isDestroyed()) return;
                navigator = result;
                map.getMapAsync(googleMap -> {
                    if (host.checkSelfPermission(
                            android.Manifest.permission.ACCESS_FINE_LOCATION)
                            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        status.onStatus("Standortberechtigung fehlt – "
                            + "Google-Kamera folgt nicht.");
                        return;
                    }
                    try {
                        googleMap.followMyLocation(
                            com.google.android.gms.maps.GoogleMap.CameraPerspective.TILTED);
                    } catch (SecurityException permissionRevoked) {
                        status.onStatus("Standortberechtigung wurde entzogen.");
                    }
                });
                status.onStatus("Google Navigation bereit · bitte Ziel wählen.");
            }

            @Override public void onError(@NavigationApi.ErrorCode int code) {
                if (closed || host.isFinishing() || host.isDestroyed()) return;
                if (code == NavigationApi.ErrorCode.NOT_AUTHORIZED) {
                    status.onStatus("Google Navigation SDK nicht autorisiert. "
                        + "Cloud-Konfiguration prüfen.");
                } else if (code == NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED) {
                    status.onStatus("Google-Navigationsbedingungen wurden nicht akzeptiert.");
                } else if (code == NavigationApi.ErrorCode.LOCATION_PERMISSION_MISSING) {
                    status.onStatus("Standortberechtigung fehlt.");
                } else {
                    status.onStatus("Google Navigation konnte nicht starten (Code " + code + ").");
                }
            }
        });
    }

    @Override public boolean ready() {
        return !closed && navigator != null;
    }

    @Override public void startConfirmedRoute(double latitude, double longitude,
                                               String title) {
        if (!ready()) {
            status.onStatus("Navigation SDK noch nicht bereit.");
            return;
        }
        if (calculating) {
            status.onStatus("Routenberechnung läuft bereits · keine doppelte Anfrage.");
            return;
        }
        if (IntegratedNavigationRules.displayCoordinates(latitude, longitude) == null) {
            status.onStatus("Ungültige Zielkoordinaten – Route nicht angefordert.");
            return;
        }
        // This call can consume billable Navigation Request quota. The native
        // UI must have shown an explicit per-route confirmation first.
        final Waypoint waypoint = Waypoint.builder()
            .setLatLng(latitude, longitude)
            .setTitle(title == null ? "FahrCockpit Ziel" : title)
            .build();
        RoutingOptions options = new RoutingOptions();
        options.travelMode(RoutingOptions.TravelMode.DRIVING);
        startedRoute = false;
        calculating = true;
        final int requestGeneration = ++routeGeneration;
        status.onStatus("Google-Route wird berechnet (Navigation Request).");
        try {
            ListenableResultFuture<Navigator.RouteStatus> request =
                navigator.setDestination(waypoint, options);
            request.setOnResultListener(result -> {
            if (closed || requestGeneration != routeGeneration
                    || host.isFinishing() || host.isDestroyed()) return;
            calculating = false;
            if (result == Navigator.RouteStatus.OK) {
                // Nav SDK 7.6.1 still uses the integer guidance flags.
                // AudioGuidanceSettings was introduced in later SDK releases.
                navigator.setAudioGuidance(
                    Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE
                    | Navigator.AudioGuidance.VIBRATION
                    | Navigator.AudioGuidance.BLUETOOTH_AUDIO);
                navigator.startGuidance();
                startedRoute = true;
                status.onStatus("Echte Google-Abbiegenavigation in FahrCockpit aktiv.");
            } else {
                startedRoute = false;
                status.onStatus("Google-Route nicht verfügbar: " + result);
            }
            });
        } catch (RuntimeException routeFailure) {
            calculating = false;
            status.onStatus("Google-Routenanforderung fehlgeschlagen.");
        }
    }

    @Override public void stopGuidanceAndClearRoute() {
        routeGeneration++; // Ignore the result of a canceled async request.
        calculating = false;
        startedRoute = false;
        if (navigator == null) return;
        navigator.stopGuidance();
        navigator.clearDestinations();
        status.onStatus("Navigation gestoppt.");
    }

    @Override public void onStart() {if (map != null) map.onStart();}
    @Override public void onResume() {if (map != null) map.onResume();}
    @Override public void onPause() {if (map != null) map.onPause();}
    @Override public void onStop() {if (map != null) map.onStop();}
    @Override public void onDestroy(boolean activityFinishing) {
        closed = true;
        if (activityFinishing) stopGuidanceAndClearRoute();
        if (map != null) map.onDestroy();
        map = null;
    }
    @Override public void onSaveInstanceState(Bundle out) {
        if (map != null) map.onSaveInstanceState(out);
        out.putBoolean("nav_guidance_active", startedRoute);
    }
    @Override public void onConfigurationChanged(Configuration config) {
        if (map != null) map.onConfigurationChanged(config);
    }
    @Override public void onTrimMemory(int level) {
        if (map != null) map.onTrimMemory(level);
    }
}
