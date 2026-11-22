package com.cobrachris.fahrcockpit;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;

/**
 * Lifecycle-scoped, screen-visible, active-route-only automatic price update
 * controller. Completely inert until the provider explicitly approves the
 * intended automatic use AND the user opts in. No background service or jobs.
 */
final class FuelAutoRefreshController {
    private final Activity host;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable requestPrices;
    private LocationManager locationManager;
    private LocationListener listener;
    private Location lastFix;
    private boolean visible, active, tracking;
    private long lastAttemptAt;
    private double lastCenterLat, lastCenterLng;
    private boolean hasLastCenter;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!tracking) return;
            long now = System.currentTimeMillis();
            if (lastFix != null && !host.isFinishing() && !host.isDestroyed()
                    && FuelAutoRefreshPolicy.locationUsable(now, lastFix.getTime(),
                        lastFix.hasAccuracy() ? lastFix.getAccuracy() : Float.NaN,
                        lastFix.getLatitude(), lastFix.getLongitude())) {
                FuelMapSnapshot.Snapshot latest = FuelMapSnapshot.read(host);
                if (FuelAutoRefreshPolicy.isDue(now, lastAttemptAt, latest.fetchedAt,
                    hasLastCenter, lastCenterLat, lastCenterLng,
                    lastFix.getLatitude(), lastFix.getLongitude())) {
                    lastAttemptAt = now;
                    hasLastCenter = true;
                    lastCenterLat = lastFix.getLatitude();
                    lastCenterLng = lastFix.getLongitude();
                    // The main search still enforces the shared persistent
                    // rate gate. One tick can cause at most one API attempt.
                    requestPrices.run();
                }
            }
            if (tracking)
                handler.postDelayed(this, FuelAutoRefreshPolicy.UI_TICK_INTERVAL_MS);
        }
    };

    FuelAutoRefreshController(Activity host, Runnable requestPrices) {
        this.host = host;
        this.requestPrices = requestPrices;
    }

    void setNavigationActive(boolean value) {
        active = value;
        reconcile();
    }

    void setVisible(boolean value) {
        visible = value;
        reconcile();
    }

    void refreshUserSetting() {
        reconcile();
    }

    private void reconcile() {
        boolean permitted = FuelAutoRefreshPolicy.PROVIDER_APPROVED
            && FuelAutoRefreshPreferences.enabled(host)
            && visible && active && !host.isFinishing() && !host.isDestroyed();
        if (!permitted) {
            stop();
            return;
        }
        if (tracking) return;
        if (host.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;
        LocationManager manager = (LocationManager) host.getSystemService(
            Context.LOCATION_SERVICE);
        if (manager == null) return;
        LocationListener candidate = loc -> {
            if (loc != null && loc.hasAccuracy()
                    && loc.getAccuracy() <= FuelAutoRefreshPolicy.LOCATION_MAX_ACCURACY_METERS)
                lastFix = new Location(loc);
        };
        try {
            boolean registered = false;
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    30_000L, 100f, candidate, Looper.getMainLooper());
                Location previous = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (previous != null) candidate.onLocationChanged(previous);
                registered = true;
            }
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    30_000L, 150f, candidate, Looper.getMainLooper());
                Location previous = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (previous != null && (lastFix == null
                        || previous.getTime() > lastFix.getTime()))
                    candidate.onLocationChanged(previous);
                registered = true;
            }
            if (!registered) return;
            locationManager = manager;
            listener = candidate;
            tracking = true;
            handler.removeCallbacks(tick);
            handler.post(tick);
        } catch (SecurityException | IllegalArgumentException issue) {
            manager.removeUpdates(candidate);
            stop();
        }
    }

    private void stop() {
        handler.removeCallbacks(tick);
        if (locationManager != null && listener != null)
            locationManager.removeUpdates(listener);
        tracking = false;
        locationManager = null;
        listener = null;
        lastFix = null;
        // No location-history persistence. Keep the latest center only
        // in RAM during this Activity lifetime.
    }

    void shutdown() {
        visible = false;
        active = false;
        stop();
    }
}
