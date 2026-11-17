package com.cobrachris.fahrcockpit;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * A single foreground-only call -> at most one Tankerkoenig HTTP request.
 * The caller enforces auto-refresh minimum interval, navigation and visibility.
 * Does not activate Google Places or issue billable Navigation SDK route requests.
 */
final class FuelQuickSearch {
    interface Result {
        void onSuccess(FuelMapSnapshot.Snapshot snapshot);
        default void onSuccess(FuelMapSnapshot.Snapshot snapshot, double lat, double lng) {
            onSuccess(snapshot);
        }
        void onFailure(String message);
    }
    private final Activity host;
    private final Handler main = new Handler(Looper.getMainLooper());
    private CancellationSignal gps, network;
    private int generation;
    private int pendingProviders;
    private boolean searching;
    private boolean locationResolved;

    FuelQuickSearch(Activity host) { this.host = host; }
    boolean isSearching() { return searching; }

    void start(Result callback) {
        if (searching) return;
        String key = host.getSharedPreferences("fuel_settings_v1", Context.MODE_PRIVATE)
            .getString("tankerkoenig_local_key", "").trim();
        if (!FuelRules.validKey(key)) {
            callback.onFailure("Tankerkönig-Schlüssel fehlt – Einstellungen öffnen.");
            return;
        }
        boolean fine = host.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
        boolean coarse = host.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) {
            callback.onFailure("Standortberechtigung fehlt – Tankstellen-Einstellungen öffnen.");
            return;
        }
        LocationManager manager = (LocationManager) host.getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) {
            callback.onFailure("Standortdienst nicht verfügbar.");
            return;
        }
        try {
            boolean gpsEnabled = fine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean netEnabled = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!gpsEnabled && !netEnabled) {
                callback.onFailure("Ortung einschalten und erneut versuchen.");
                return;
            }
            searching = true;
            locationResolved = false;
            final int token = ++generation;
            Location recent = null;
            if (netEnabled) recent = recent(manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
            if (gpsEnabled) {
                Location other = recent(manager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
                if (other != null && (recent == null || other.getTime() > recent.getTime()))
                    recent = other;
            }
            if (recent != null) {
                locationResolved = true;
                fetch(token, recent, key, callback);
                return;
            }
            if (netEnabled) {
                network = new CancellationSignal();
                pendingProviders++;
                try {
                    manager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, network,
                        host.getMainExecutor(), location -> candidate(token, location, key, callback));
                } catch (RuntimeException ignored) {pendingProviders--;network = null;}
            }
            if (gpsEnabled) {
                gps = new CancellationSignal();
                pendingProviders++;
                try {
                    manager.getCurrentLocation(LocationManager.GPS_PROVIDER, gps,
                        host.getMainExecutor(), location -> candidate(token, location, key, callback));
                } catch (RuntimeException ignored) {pendingProviders--;gps = null;}
            }
            if (pendingProviders == 0) {
                fail(token, "Standortdienst konnte nicht gestartet werden.", callback);
                return;
            }
            main.postDelayed(() -> {
                if (token == generation && searching)
                    fail(token, "Standort nicht erreichbar. GPS oder Standortfreigabe prüfen.", callback);
            }, 14000L);
        } catch (RuntimeException e) {
            fail(generation, "Standortberechtigung oder Standortdienst fehlt.", callback);
        }
    }

    private static Location recent(Location position) {
        if (position == null) return null;
        long age = System.currentTimeMillis() - position.getTime();
        return age >= 0 && age < 120000 && Double.isFinite(position.getLatitude())
            && Double.isFinite(position.getLongitude()) ? position : null;
    }

    private void candidate(int token, Location location, String key, Result callback) {
        if (token != generation || !searching || locationResolved) return;
        if (location != null && Double.isFinite(location.getLatitude())
                && Double.isFinite(location.getLongitude())) {
            locationResolved = true;
            cancelProviders();
            fetch(token, location, key, callback);
            return;
        }
        pendingProviders--;
        if (pendingProviders <= 0)
            fail(token, "Standort nicht ermittelbar. Standortdienste prüfen.", callback);
    }

    private void fetch(int token, Location location, String key, Result callback) {
        // Do not update markers with coordinates outside the country or an invalid fix.
        String type = FuelQuickSettings.type(host);
        int radius = FuelQuickSettings.radiusKm(host);
        final String url;
        try {
            url = FuelRules.requestUrl(key, location.getLatitude(), location.getLongitude(),
                radius, type);
        } catch (IllegalArgumentException e) {
            fail(token, e.getMessage(), callback);
            return;
        }
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(9000);
                connection.setReadTimeout(9000);
                connection.setRequestProperty("Accept", "application/json");
                if (connection.getResponseCode() != 200) throw new IllegalStateException("HTTP error");
                StringBuilder json = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        json.append(line);
                        if (json.length() > 524288) throw new IllegalStateException("Too large");
                    }
                }
                JSONObject response = new JSONObject(json.toString());
                if (!response.optBoolean("ok", false)) throw new IllegalStateException("Invalid API");
                JSONArray stations = response.optJSONArray("stations");
                if (stations == null) throw new IllegalStateException("Missing stations");
                main.post(() -> {
                    if (token != generation || !searching || host.isFinishing()
                            || host.isDestroyed()) return;
                    FuelMapSnapshot.save(host, stations, type);
                    searching = false;
                    callback.onSuccess(FuelMapSnapshot.read(host),
                        location.getLatitude(), location.getLongitude());
                });
            } catch (Exception ignored) {
                // Never reveal URLs, location coordinates or API keys in logs/UI.
                main.post(() -> fail(token,
                    "Preisabruf fehlgeschlagen. Verbindung oder API-Schlüssel prüfen.", callback));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "fuel-cockpit-quick-search").start();
    }

    private void fail(int token, String message, Result callback) {
        if (token != generation) return;
        searching = false;
        locationResolved = false;
        cancelProviders();
        if (!host.isFinishing() && !host.isDestroyed()) callback.onFailure(message);
    }

    private void cancelProviders() {
        if (gps != null) gps.cancel();
        if (network != null) network.cancel();
        gps = null;
        network = null;
        pendingProviders = 0;
    }

    void cancel() {
        ++generation;
        searching = false;
        locationResolved = false;
        cancelProviders();
        main.removeCallbacksAndMessages(null);
    }
}
