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

/**
 * German fuel prices from Tankerkoenig, only on explicit user action.
 * No auto polling; no location history or server-side account.
 */
public final class FuelActivity extends Activity {
    private static final String PREFS = "fuel_settings_v1";
    private static final String KEY = "tankerkoenig_local_key";
    private EditText apiKey;
    private Spinner fuelKind;
    private Spinner radius;
    private TextView message;
    private LinearLayout results;
    private Button search;
    private boolean pendingSearch;
    private CancellationSignal locationSignal;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        root.addView(fuelKind);
        radius = new Spinner(this);
        ArrayAdapter<String> distances = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, new String[] {
                "5 km", "10 km", "20 km", "25 km"
            });
        radius.setAdapter(distances);
        radius.setSelection(1);
        root.addView(radius);

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
        try {
            String provider;
            if (fine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                provider = LocationManager.GPS_PROVIDER;
            } else if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                provider = LocationManager.NETWORK_PROVIDER;
            } else {
                fail("Ortung nicht aktiviert. Bitte Standortdienste einschalten.");
                return;
            }
            Location last = manager.getLastKnownLocation(provider);
            if (last != null && System.currentTimeMillis() - last.getTime() < 120000) {
                fetch(last, key);
                return;
            }
            locationSignal = new CancellationSignal();
            final CancellationSignal signal = locationSignal;
            main.postDelayed(() -> {
                if (locationSignal == signal && search != null && !search.isEnabled()) {
                    signal.cancel();
                    locationSignal = null;
                    fail("Standort nicht erreichbar. Bitte GPS prüfen oder Google Maps öffnen.");
                }
            }, 14000);
            manager.getCurrentLocation(provider, signal, getMainExecutor(), location -> {
                if (locationSignal != signal) return;
                locationSignal = null;
                if (location == null) {
                    fail("Standort nicht ermittelbar.");
                } else {
                    fetch(location, key);
                }
            });
        } catch (RuntimeException e) {
            locationSignal = null;
            fail("Standortberechtigung oder Standortdienst fehlt.");
        }
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
        try {
            url = FuelRules.requestUrl(key, location.getLatitude(),
                location.getLongitude(), radiusKm(), fuelType());
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
                main.post(() -> showStations(stations));
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
        search.setEnabled(true);
        results.removeAllViews();
        String at = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date());
        message.setText(stations.length() + " Treffer · abgerufen um " + at +
            "\nPreise ohne Garantie; Tippen überträgt das Ziel an Google Maps.");
        if (stations.length() == 0) {
            results.addView(Ui.text(this, "Keine Meldungen im gewählten Radius.", 14, Ui.MUTED, false));
        }
        // Show all returned stations. No hidden filtering or data redistribution.
        for (int i = 0; i < stations.length(); i++) {
            JSONObject s = stations.optJSONObject(i);
            if (s == null) continue;
            String name = s.optString("name", "Tankstelle");
            String brand = s.optString("brand", "");
            String street = s.optString("street", "");
            String town = s.optString("place", "");
            double price = s.optDouble("price", -1);
            double distance = s.optDouble("dist", -1);
            double lat = s.optDouble("lat", Double.NaN);
            double lng = s.optDouble("lng", Double.NaN);
            String priceText = price >= 0 ? String.format(Locale.GERMANY, "%.3f €/l", price)
                                          : "Preis nicht gemeldet";
            String distText = distance >= 0 ? String.format(Locale.GERMANY, "%.1f km", distance)
                                            : "Entfernung unbekannt";
            String openText = s.optBoolean("isOpen", false) ? "geöffnet" : "geschlossen/unbekannt";
            LinearLayout station = Ui.column(this);
            station.setPadding(Ui.dp(this, 12), Ui.dp(this, 8),
                Ui.dp(this, 12), Ui.dp(this, 8));
            station.setBackground(Ui.background(this, Ui.PANEL, 12));
            station.addView(Ui.text(this, priceText + "  ·  " + name, 17, Ui.FG, true));
            station.addView(Ui.text(this, brand + " · " + street + ", " + town +
                "\n" + distText + " · " + openText, 13, Ui.MUTED, false));
            Button route = Ui.button(this, "Als Ziel in Google Maps öffnen");
            route.setEnabled(Double.isFinite(lat) && Double.isFinite(lng));
            route.setOnClickListener(v -> openMapsDestination(lat, lng));
            station.addView(route);
            results.addView(station, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            Ui.gap(this, results, 10);
        }
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
        if (locationSignal != null) locationSignal.cancel();
        locationSignal = null;
        super.onDestroy();
    }
}
