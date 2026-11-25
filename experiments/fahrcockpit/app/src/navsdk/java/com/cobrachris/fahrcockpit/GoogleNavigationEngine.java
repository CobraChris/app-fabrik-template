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
import com.google.android.libraries.navigation.TimeAndDistance;
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
    private GoogleFuelPriceMarkers priceMarkers;
    private int fuelMarkerGeneration;
    private Navigator navigator;
    private Navigator.ArrivalListener arrivalListener;
    private Navigator.RemainingTimeOrDistanceChangedListener progressListener;
    private Status status = value -> {};
    private boolean startedRoute;
    private boolean calculating;
    private int routeGeneration;
    private boolean closed;

    public GoogleNavigationEngine(Activity host) {
        this.host = host;
        String deviceKey = NavigationSdkKeyStore.read(host);
        // Local Android Keystore is preferred. Optional developer-only build
        // configuration remains keyless in all regular/release CI builds.
        this.apiKey = deviceKey.isEmpty() ? BuildConfig.NAVSDK_API_KEY : deviceKey;
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
        priceMarkers = new GoogleFuelPriceMarkers(host);
        map.onCreate(saved);
        // Navigation SDK controls are available during guided navigation.
        // Show only Google-provided speed limits; never guess missing limits.
        map.setSpeedometerEnabled(true);
        map.setSpeedLimitIconEnabled(true);
        return map;
    }

    @Override public void initializeAfterLocationPermission() {
        if (!configured() || map == null || closed) return;
        status.onStatus("Google Navigation SDK · Nutzungsbedingungen werden geprüft …");
        NavigationApi.getNavigator(host, new NavigationApi.NavigatorListener() {
            @Override public void onNavigatorReady(Navigator result) {
                if (closed || host.isFinishing() || host.isDestroyed()) return;
                navigator = result;
                // Listen only to the final destination. This is a passive
                // SDK event; do not stop/restart guidance or issue route calls.
                if (arrivalListener != null)
                    navigator.removeArrivalListener(arrivalListener);
                arrivalListener = event -> {
                    if (!closed && startedRoute && event.isFinalDestination())
                        status.onFinalDestinationArrived();
                };
                navigator.addArrivalListener(arrivalListener);
                // Passive arrival estimates, event-driven: no additional
                // setDestination(), poller, billing API or route requests.
                if (progressListener != null)
                    navigator.removeRemainingTimeOrDistanceChangedListener(progressListener);
                progressListener = GoogleNavigationEngine.this::notifyRemainingProgress;
                navigator.addRemainingTimeOrDistanceChangedListener(
                    30, 100, progressListener);
                // Navigator is a singleton and may still guide after rotation.
                startedRoute = navigator.isGuidanceRunning();
                status.onGuidanceStateChanged(startedRoute);
                if (startedRoute) notifyRemainingProgress();
                applyAudioPreference();
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
                // SDK can resume an already selected route after Activity restore.
                // Do not incorrectly demand a new destination when reconnecting.
                status.onStatus(startedRoute
                    ? "Echte Google-Abbiegenavigation in FahrCockpit aktiv."
                    : "Google Navigation bereit.");
            }

            @Override public void onError(@NavigationApi.ErrorCode int code) {
                if (closed || host.isFinishing() || host.isDestroyed()) return;
                if (code == NavigationApi.ErrorCode.NOT_AUTHORIZED) {
                    status.onGoogleFailure(
                        NavigationProviderPolicy.Failure.BILLING_OR_AUTHORIZATION_UNAVAILABLE);
                } else if (code == NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED) {
                    status.onStatus("Google-Navigationsbedingungen wurden nicht akzeptiert.");
                } else if (code == NavigationApi.ErrorCode.LOCATION_PERMISSION_MISSING) {
                    status.onStatus("Standortberechtigung fehlt.");
                } else if (code == NavigationApi.ErrorCode.NETWORK_ERROR) {
                    status.onGoogleFailure(
                        NavigationProviderPolicy.Failure.GOOGLE_SERVICE_UNAVAILABLE);
                } else {
                    status.onGoogleFailure(
                        NavigationProviderPolicy.Failure.GOOGLE_SERVICE_UNAVAILABLE);
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
        // Reserve the local daily allowance before any potentially billable
        // setDestination() call. Deny routing if storage cannot commit.
        // This is defense in depth, NOT a substitute for Google's Cloud quota.
        if (!NavigationRequestBudget.reserve(host)) {
            status.onGoogleFailure(
                NavigationProviderPolicy.Failure.LOCAL_REQUEST_LIMIT);
            return;
        }
        // This may consume a billable Navigation Request. The caller must
        // be an explicit route-button press or a confirmed fuel destination;
        // NavigationRequestBudget above still enforces the local limit.
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
            request.setOnResultListener(result -> host.runOnUiThread(() -> {
            if (closed || requestGeneration != routeGeneration
                    || host.isFinishing() || host.isDestroyed()) return;
            calculating = false;
            if (result == Navigator.RouteStatus.OK) {
                // Apply the persisted preference on every route, even after
                // re-routing. Never silently re-enable turn-by-turn speech.
                applyAudioPreference();
                navigator.startGuidance();
                startedRoute = true;
                status.onGuidanceStateChanged(true);
                notifyRemainingProgress();
                status.onStatus("Echte Google-Abbiegenavigation in FahrCockpit aktiv.");
            } else {
                startedRoute = navigator.isGuidanceRunning();
                status.onGuidanceStateChanged(startedRoute);
                if (result == Navigator.RouteStatus.QUOTA_CHECK_FAILED) {
                    status.onGoogleFailure(
                        NavigationProviderPolicy.Failure.NAVIGATION_QUOTA_EXHAUSTED);
                } else if (result == Navigator.RouteStatus.NETWORK_ERROR) {
                    status.onGoogleFailure(
                        NavigationProviderPolicy.Failure.GOOGLE_SERVICE_UNAVAILABLE);
                } else {
                    // NO_ROUTE_FOUND is not a provider outage; do not silently
                    // retry billable routes or switch to unrelated data.
                    status.onStatus("Google-Route nicht verfügbar: " + result);
                }
            }
            }));
        } catch (RuntimeException routeFailure) {
            calculating = false;
            status.onGoogleFailure(
                NavigationProviderPolicy.Failure.GOOGLE_SERVICE_UNAVAILABLE);
        }
    }

    private void notifyRemainingProgress() {
        if (closed || !startedRoute || navigator == null) return;
        TimeAndDistance estimate = navigator.getCurrentTimeAndDistance();
        if (estimate != null && estimate.getSeconds() >= 0
                && estimate.getMeters() >= 0)
            status.onRouteProgress(estimate.getSeconds(), estimate.getMeters());
    }

    @Override public void setNativeEtaVisible(boolean visible) {
        if (map != null && !closed) map.setEtaCardEnabled(visible);
    }

    private void applyAudioPreference() {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            host.runOnUiThread(this::applyAudioPreference);
            return;
        }
        if (navigator == null || closed) return;
        // v31: voice defaults to muted. Do not vibrate for routine Google
        // guidance, even when speech is switched on. Saphe is unaffected.
        // Preserve an existing user override in NavigationVoicePreferences.
        int mode = NavigationVoicePreferences.isMuted(host)
            ? 0
            : Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE
                | Navigator.AudioGuidance.BLUETOOTH_AUDIO;
        navigator.setAudioGuidance(mode);
    }

    @Override public void showFuelPrices(FuelMapSnapshot.Snapshot snapshot,
                                         FuelMarkerSelection selection) {
        if (map == null || closed) return;
        final int generation = ++fuelMarkerGeneration;
        map.getMapAsync(googleMap -> {
            if (closed || generation != fuelMarkerGeneration
                    || host.isDestroyed() || priceMarkers == null) return;
            priceMarkers.display(googleMap, snapshot, selection);
        });
    }

    @Override public void setVoiceMuted(boolean muted) {
        // The activity persists the selection before calling this method.
        if (navigator == null || closed) return;
        host.runOnUiThread(this::applyAudioPreference);
    }

    @Override public void stopGuidanceAndClearRoute() {
        routeGeneration++; // Ignore the result of a canceled async request.
        calculating = false;
        startedRoute = false;
        status.onGuidanceStateChanged(false);
        if (navigator == null) return;
        navigator.stopGuidance();
        navigator.clearDestinations();
        status.onStatus("Navigation gestoppt.");
    }

    @Override public void onStart() {if (map != null) map.onStart();}
    @Override public void onResume() {
        if (map != null) map.onResume();
        if (navigator != null && !closed) {
            startedRoute = navigator.isGuidanceRunning();
            status.onGuidanceStateChanged(startedRoute);
            if (startedRoute) notifyRemainingProgress();
        }
    }
    @Override public void onPause() {if (map != null) map.onPause();}
    @Override public void onStop() {if (map != null) map.onStop();}
    @Override public void onDestroy(boolean activityFinishing) {
        if (navigator != null && progressListener != null) {
            navigator.removeRemainingTimeOrDistanceChangedListener(progressListener);
            progressListener = null;
        }
        if (navigator != null && arrivalListener != null) {
            navigator.removeArrivalListener(arrivalListener);
            arrivalListener = null;
        }
        closed = true;
        ++fuelMarkerGeneration;
        if (priceMarkers != null) priceMarkers.clear();
        priceMarkers = null;
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
