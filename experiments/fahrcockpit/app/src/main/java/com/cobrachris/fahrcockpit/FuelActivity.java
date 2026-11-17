package com.cobrachris.fahrcockpit;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * German fuel prices from Tankerkoenig, only on explicit user action.
 * No auto polling; no location history or server-side account.
 */
public final class FuelActivity extends Activity {
    static final String EXTRA_RETURN_TO_COCKPIT = "fuel_return_to_cockpit";
    static final String RESULT_LATITUDE = "fuel_station_lat";
    static final String RESULT_LONGITUDE = "fuel_station_lng";
    static final String RESULT_NAME = "fuel_station_name";

    private boolean returnToCockpit;
    private static final String PREFS = "fuel_settings_v1";
    private static final String KEY = "tankerkoenig_local_key";
    private EditText apiKey;
    private Spinner fuelKind;
    private Spinner radius;
    private Spinner sortOrder;
    private JSONArray lastStations;
    private String lastFetchedAt;
    private TextView message;
    private LinearLayout results;
    private Button search;
    private boolean pendingSearch;
    // Concurrent network/GPS fixes: the first valid reply wins. No background polling.
    private CancellationSignal gpsSignal;
    private CancellationSignal networkSignal;
    private int locationGeneration;
    private int pendingLocationProviders;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        returnToCockpit = getIntent().getBooleanExtra(EXTRA_RETURN_TO_COCKPIT, false);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout root = Ui.column(this);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 40));
        scroll.addView(root);
        setContentView(scroll);
        getWindow().getDecorView().setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        root.addView(Ui.text(this, "⛽ Tankstellen & Preise", 25, Ui.FG, true));
        root.addView(Ui.text(this,
            "Live-Preise für E5, E10 und Diesel in Deutschland. " +
            "Der Abruf erfolgt nur, wenn du die Suche startest.", 14, Ui.MUTED, false));

        Ui.section(this, root, "Kostenfreier Tankerkönig-Schlüssel",
            "Persönlichen API-Key einmalig auf diesem Handy speichern. " +
            "Der Schlüssel wird nicht zu GitHub hochgeladen.");
        apiKey = new EditText(this);
        apiKey.setSingleLine(true);
        apiKey.setTextColor(Ui.FG);
        apiKey.setHintTextColor(Ui.MUTED);
        apiKey.setHint("API-Key als UUID");
        apiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        apiKey.setText(getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY, ""));
        root.addView(apiKey);
        Button saveKey = Ui.button(this, "Schlüssel lokal speichern");
        root.addView(saveKey);
        saveKey.setOnClickListener(v -> {
            String key = apiKey.getText().toString().trim();
            if (!key.isEmpty() && !FuelRules.validKey(key)) {
                message.setText("Ungültiger Schlüssel: Er muss das UUID-Format haben.");
                return;
            }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY, key).apply();
            message.setText(key.isEmpty() ? "Schlüssel auf diesem Handy gelöscht."
                                         : "Schlüssel auf diesem Handy gespeichert.");
        });
        Button provider = Ui.button(this, "Kostenlosen API-Key beantragen");
        root.addView(provider);
        provider.setOnClickListener(v -> openBrowser("https://onboarding.tankerkoenig.de/"));

        Ui.section(this, root, "Kraftstoff und Suchradius", "");
        fuelKind = new Spinner(this);
        ArrayAdapter<String> types = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, new String[] {
                "Super E5", "Super E10", "Diesel"
            });
        fuelKind.setAdapter(types);
        fuelKind.setSelection(FuelQuickSettings.typeIndex(this));
        root.addView(fuelKind);
        fuelKind.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                    android.view.View view, int position, long id) {
                FuelQuickSettings.saveType(FuelActivity.this, position);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        radius = new Spinner(this);
        ArrayAdapter<String> distances = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, new String[] {
                "5 km", "10 km", "20 km", "25 km"
            });
        radius.setAdapter(distances);
        radius.setSelection(FuelQuickSettings.radiusIndex(this));
        root.addView(radius);
        radius.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                    android.view.View view, int position, long id) {
                FuelQuickSettings.saveRadius(FuelActivity.this, position);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        sortOrder = new Spinner(this);
        ArrayAdapter<String> sorting = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {"Nach Entfernung", "Nach Preis (günstigste zuerst)"});
        sortOrder.setAdapter(sorting);
        sortOrder.setSelection(FuelQuickSettings.sortIndex(this));
        root.addView(sortOrder);
        sortOrder.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                    android.view.View view, int position, long id) {
                FuelQuickSettings.saveSort(FuelActivity.this, position);
                if (lastStations != null) showStations(lastStations);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        search = Ui.button(this, "Preise in meiner Nähe abrufen");
        root.addView(search);
        search.setOnClickListener(v -> search());
        Button google = Ui.button(this, "Ohne Preisvergleich: Tankstellen in Google Maps");
        root.addView(google);
        google.setOnClickListener(v -> openBrowser(
            "https://www.google.com/maps/search/?api=1&query=Tankstellen"));

        message = Ui.text(this,
            "Standortfreigabe erfolgt erst bei deiner Suche. " +
            "Der Standort wird dann direkt an Tankerkönig gesendet.",
            14, Ui.WARN, false);
        root.addView(message);
        results = Ui.column(this);
        root.addView(results);

        root.addView(Ui.text(this,
            "Datenquelle: Tankerkönig (CC BY 4.0), MTS-K. Preise können abweichen. " +
            "Keine Echtzeit-Preise außerhalb Deutschlands. " +
            "Tankerkönig gibt keine Gewähr für Vollständigkeit oder Aktualität.",
            13, Ui.MUTED, false));
        Button attribution = Ui.button(this, "Tankerkönig – Datenquelle");
        root.addView(attribution);
        attribution.setOnClickListener(v -> openBrowser("https://creativecommons.tankerkoenig.de/"));
    }

    private int radiusKm() {
        return new int[] {5, 10, 20, 25}[radius.getSelectedItemPosition()];
    }

    private String fuelType() {
        return new String[] {"e5", "e10", "diesel"}[fuelKind.getSelectedItemPosition()];
    }

    private void search() {
        String key = apiKey.getText().toString().trim();
        if (!FuelRules.validKey(key)) {
            message.setText("Zuerst einen persönlichen Tankerkönig-API-Key eingeben.");
            return;
        }
        // Private Android app storage; no GitHub or external telemetry.
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY, key).apply();
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            pendingSearch = true;
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, 301);
            return;
        }
        locateAndSearch(key);
    }

    @Override public void onRequestPermissionsResult(
        int requestCode, String[] names, int[] grants) {
        super.onRequestPermissionsResult(requestCode, names, grants);
        if (requestCode == 301 && pendingSearch) {
            pendingSearch = false;
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                search();
            } else {
                message.setText("Standortberechtigung fehlt. Alternativ Google Maps verwenden.");
            }
        }
    }

    private void locateAndSearch(String key) {
        message.setText("Standort wird ermittelt …");
        search.setEnabled(false);
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) {
            fail("Standortdienst fehlt.");
            return;
        }
        boolean fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
        boolean coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) {
            fail("Standortberechtigung fehlt.");
            return;
        }
        try {
            boolean gpsAvailable = fine && manager.isProviderEnabled(
                LocationManager.GPS_PROVIDER);
            boolean networkAvailable = manager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER);
            if (!gpsAvailable && !networkAvailable) {
                fail("Ortung nicht aktiviert. Bitte Standortdienste einschalten.");
                return;
            }
            // The original implementation only inspected GPS history when GPS
            // was enabled, even when a fresh network fix was already available.
            Location recent = null;
            if (networkAvailable) {
                recent = recentLocation(manager.getLastKnownLocation(
                    LocationManager.NETWORK_PROVIDER));
            }
            if (gpsAvailable) {
                Location gps = recentLocation(manager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER));
                if (gps != null && (recent == null || gps.getTime() > recent.getTime())) {
                    recent = gps;
                }
            }
            if (recent != null) {
                fetch(recent, key);
                return;
            }

            stopLocationRequests();
            final int generation = ++locationGeneration;
            // Prefer neither GPS nor network exclusively: request both at once,
            // and accept whichever succeeds. Only request GPS with fine permission.
            if (networkAvailable) {
                networkSignal = new CancellationSignal();
                pendingLocationProviders++;
                try {
                    manager.getCurrentLocation(LocationManager.NETWORK_PROVIDER,
                        networkSignal, getMainExecutor(),
                        location -> onLocationCandidate(generation, location, key));
                } catch (RuntimeException networkFailure) {
                    pendingLocationProviders--;
                    networkSignal = null;
                }
            }
            if (gpsAvailable) {
                gpsSignal = new CancellationSignal();
                pendingLocationProviders++;
                try {
                    manager.getCurrentLocation(LocationManager.GPS_PROVIDER,
                        gpsSignal, getMainExecutor(),
                        location -> onLocationCandidate(generation, location, key));
                } catch (RuntimeException gpsFailure) {
                    pendingLocationProviders--;
                    gpsSignal = null;
                }
            }
            if (pendingLocationProviders == 0) {
                stopLocationRequests();
                fail("Standortdienste konnten nicht gestartet werden.");
                return;
            }
            main.postDelayed(() -> {
                if (generation != locationGeneration || search.isEnabled()) return;
                stopLocationRequests();
                fail("Standort nicht erreichbar. Bitte Standort und Berechtigung prüfen.");
            }, 14000);
        } catch (RuntimeException serviceFailure) {
            stopLocationRequests();
            fail("Standortberechtigung oder Standortdienst fehlt.");
        }
    }

    private static Location recentLocation(Location position) {
        if (position == null) return null;
        long age = System.currentTimeMillis() - position.getTime();
        return age >= 0 && age < 120000
            && Double.isFinite(position.getLatitude())
            && Double.isFinite(position.getLongitude()) ? position : null;
    }

    private void onLocationCandidate(int generation, Location position, String key) {
        if (generation != locationGeneration || search.isEnabled()) return;
        if (position != null && Double.isFinite(position.getLatitude())
                && Double.isFinite(position.getLongitude())) {
            stopLocationRequests();
            fetch(position, key);
            return;
        }
        pendingLocationProviders--;
        if (pendingLocationProviders <= 0) {
            stopLocationRequests();
            fail("Standort nicht ermittelbar. Bitte Standortdienste prüfen.");
        }
    }

    private void stopLocationRequests() {
        ++locationGeneration; // Invalidate queued callbacks and the timeout.
        if (gpsSignal != null) gpsSignal.cancel();
        if (networkSignal != null) networkSignal.cancel();
        gpsSignal = null;
        networkSignal = null;
        pendingLocationProviders = 0;
    }

    private void fetch(Location location, String key) {
        if (!FuelRules.inGermany(location.getLatitude(), location.getLongitude())) {
            fail("Tankerkönig liefert nur Daten für Deutschland. " +
                "Hier bitte Google Maps verwenden.");
            return;
        }
        results.removeAllViews();
        message.setText("Aktuelle Tankstellenpreise werden abgerufen …");
        String url;
        final String selectedFuelType = fuelType();
        try {
            url = FuelRules.requestUrl(key, location.getLatitude(),
                location.getLongitude(), radiusKm(), selectedFuelType);
        } catch (IllegalArgumentException e) {
            fail(e.getMessage());
            return;
        }
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(9000);
                connection.setReadTimeout(9000);
                connection.setRequestProperty("Accept", "application/json");
                if (connection.getResponseCode() != 200)
                    throw new IllegalStateException("HTTP " + connection.getResponseCode());
                StringBuilder json = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        json.append(line);
                        if (json.length() > 524288) throw new IllegalStateException("Antwort zu groß");
                    }
                }
                JSONObject response = new JSONObject(json.toString());
                if (!response.optBoolean("ok", false)) {
                    throw new IllegalStateException("API-Antwort: " + response.optString("message", "Fehler"));
                }
                JSONArray stations = response.optJSONArray("stations");
                if (stations == null) throw new IllegalStateException("Keine Tankstellenliste");
                main.post(() -> {
                    // Share only the validated station data, never the API key.
                    // Rendering the cache on the map does not re-query the API.
                    FuelMapSnapshot.save(this, stations, selectedFuelType);
                    showStations(stations);
                });
            } catch (Exception e) {
                // Don't log URLs, as they contain the private API key.
                main.post(() -> fail("Preisabruf fehlgeschlagen. " +
                    "Schlüssel, Internetverbindung und API-Freigabe prüfen."));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "fuel-request").start();
    }

    private void showStations(JSONArray stations) {
        if (lastStations != stations) {
            lastFetchedAt = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date());
        }
        lastStations = stations;
        search.setEnabled(true);
        results.removeAllViews();
        String at = lastFetchedAt == null ? "–" : lastFetchedAt;
        message.setText(stations.length() + " Treffer · abgerufen um " + at +
            (returnToCockpit
                ? "\nTankstelle auswählen. Eine Google-Navigationsroute wird erst nach deiner Bestätigung berechnet."
                : "\nPreise ohne Garantie; ausgewähltes Ziel in Google Maps öffnen."));
        if (stations.length() == 0) {
            results.addView(Ui.text(this, "Keine Meldungen im gewählten Radius.", 14, Ui.MUTED, false));
        }
        // Sort only the presentation of the same API response; never create
        // extra requests when switching the sort order.
        List<JSONObject> ordered = new ArrayList<>();
        for (int i = 0; i < stations.length(); i++) {
            JSONObject station = stations.optJSONObject(i);
            if (station != null) ordered.add(station);
        }
        boolean byPrice = sortOrder != null && sortOrder.getSelectedItemPosition() == 1;
        Collections.sort(ordered, (a, b) -> {
            double va = a.optDouble(byPrice ? "price" : "dist", -1);
            double vb = b.optDouble(byPrice ? "price" : "dist", -1);
            if (!Double.isFinite(va) || va < 0) va = Double.POSITIVE_INFINITY;
            if (!Double.isFinite(vb) || vb < 0) vb = Double.POSITIVE_INFINITY;
            return Double.compare(va, vb);
        });
        for (JSONObject s : ordered) {
            if (s == null) continue;
            String name = s.optString("name", "Tankstelle");
            String brand = s.optString("brand", "");
            String street = s.optString("street", "");
            String town = s.optString("place", "");
            double price = s.optDouble("price", -1);
            double distance = s.optDouble("dist", -1);
            double lat = s.optDouble("lat", Double.NaN);
            double lng = s.optDouble("lng", Double.NaN);
            String priceText = price >= 0 ? String.format(Locale.GERMANY, "%.2f €/l", price)
                                          : "Preis nicht gemeldet";
            String distText = distance >= 0 ? String.format(Locale.GERMANY, "%.1f km", distance)
                                            : "Entfernung unbekannt";
            String openText = s.optBoolean("isOpen", false) ? "geöffnet" : "geschlossen/unbekannt";
            LinearLayout station = Ui.column(this);
            station.setPadding(Ui.dp(this, 10), Ui.dp(this, 6),
                Ui.dp(this, 10), Ui.dp(this, 6));
            station.setBackground(Ui.background(this, Ui.PANEL, 12));
            TextView heading = Ui.text(this, priceText + " · " + name, 15, Ui.FG, true);
            heading.setMaxLines(2);
            heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
            heading.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 2));
            station.addView(heading);
            TextView details = Ui.text(this, brand + " · " + street + ", " + town +
                "\n" + distText + " · " + openText, 12, Ui.MUTED, false);
            details.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 2));
            station.addView(details);
            LinearLayout actions = Ui.row(this);
            String coordinates = FuelRules.stationCoordinates(lat, lng);
            if (returnToCockpit) {
                Button inApp = Ui.button(this, "Als FahrCockpit-Ziel übernehmen");
                inApp.setEnabled(coordinates != null);
                inApp.setOnClickListener(v -> selectForCockpit(lat, lng,
                    (brand.trim().isEmpty() ? "" : brand.trim() + " · ") + name));
                inApp.setText("Als Ziel");
                inApp.setTextSize(12);
                inApp.setMinHeight(Ui.dp(this, 40));
                actions.addView(inApp, new LinearLayout.LayoutParams(0, -2, 1f));
            }
            // Preserve the external Maps alternative without forcing a switch.
            Button route = Ui.button(this, "In Google Maps öffnen");
            route.setEnabled(coordinates != null);
            route.setOnClickListener(v -> openMapsDestination(lat, lng));
            route.setText("Google Maps");
            route.setTextSize(12);
            route.setMinHeight(Ui.dp(this, 40));
            actions.addView(route, new LinearLayout.LayoutParams(0, -2, 1f));
            station.addView(actions);
            results.addView(station, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            Ui.gap(this, results, 5);
        }
    }

    private void selectForCockpit(double lat, double lng, String name) {
        String coordinates = FuelRules.stationCoordinates(lat, lng);
        if (!returnToCockpit || coordinates == null) {
            message.setText("Tankstellenziel ungültig oder keine FahrCockpit-Verbindung.");
            return;
        }
        String label = RouteTargetRules.clean(name);
        Intent result = new Intent()
            .putExtra(RESULT_LATITUDE, lat)
            .putExtra(RESULT_LONGITUDE, lng)
            .putExtra(RESULT_NAME, label == null ? "Tankstelle" : label);
        setResult(RESULT_OK, result);
        finish(); // Returns to the existing Google NavigationView and route.
    }

    private void openMapsDestination(double lat, double lng) {
        String destination = String.format(Locale.US, "%.6f,%.6f", lat, lng);
        openBrowser("https://www.google.com/maps/dir/?api=1&destination=" + destination);
    }

    private void openBrowser(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (RuntimeException e) {
            Toast.makeText(this, "Link konnte nicht geöffnet werden.", Toast.LENGTH_LONG).show();
        }
    }

    private void fail(String error) {
        search.setEnabled(true);
        message.setText(error);
    }

    @Override public void onDestroy() {
        stopLocationRequests();
        super.onDestroy();
    }
}
