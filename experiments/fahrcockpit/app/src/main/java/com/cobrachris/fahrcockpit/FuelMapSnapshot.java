package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Small local snapshot of a user-triggered Tankerkoenig search.
 * Does not store the key, user coordinates, or location history.
 * No background network calls; data expires after 15 minutes.
 */
final class FuelMapSnapshot {
    static final long MAX_AGE_MS = 15 * 60 * 1000L;
    private static final String PREFS = "fahrcockpit_fuel_map_v1";
    private static final String STATIONS = "stations";
    private static final String SAVED_AT = "saved_at";
    private static final String TYPE = "fuel_type";
    private static final int MAX_STATIONS = 40;

    private FuelMapSnapshot() {}

    static final class Station {
        final String name, address;
        final double lat, lng, price;
        final boolean isOpen;
        Station(String name, String address, double lat, double lng,
                double price, boolean isOpen) {
            this.name = name;
            this.address = address;
            this.lat = lat;
            this.lng = lng;
            this.price = price;
            this.isOpen = isOpen;
        }
        String priceLabel() {
            return String.format(Locale.GERMANY, "%.2f €", price);
        }
    }

    static final class Snapshot {
        final String type;
        final long fetchedAt;
        final List<Station> stations;
        Snapshot(String type, long fetchedAt, List<Station> stations) {
            this.type = type;
            this.fetchedAt = fetchedAt;
            this.stations = Collections.unmodifiableList(stations);
        }
    }

    static void save(Context context, JSONArray response, String type) {
        if (context == null || response == null || !validType(type)) return;
        JSONArray safe = new JSONArray();
        for (int i = 0; i < response.length() && safe.length() < MAX_STATIONS; i++) {
            JSONObject src = response.optJSONObject(i);
            if (src == null) continue;
            Station s = fromJson(src);
            if (s == null) continue;
            JSONObject clean = new JSONObject();
            try {
                clean.put("name", s.name);
                clean.put("address", s.address);
                clean.put("lat", s.lat);
                clean.put("lng", s.lng);
                clean.put("price", s.price);
                clean.put("isOpen", s.isOpen);
                safe.put(clean);
            } catch (JSONException ignored) { /* Skip malformed source item. */ }
        }
        // Commit both fields together; a failed write must not silently show stale prices.
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(STATIONS, safe.toString())
            .putString(TYPE, type)
            .putLong(SAVED_AT, System.currentTimeMillis()).commit();
    }

    static Snapshot read(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long fetched = prefs.getLong(SAVED_AT, 0L);
        long now = System.currentTimeMillis();
        String type = prefs.getString(TYPE, "");
        if (fetched <= 0L || fetched > now || now - fetched > MAX_AGE_MS
                || !validType(type)) {
            return new Snapshot("", 0L, new ArrayList<>());
        }
        List<Station> result = new ArrayList<>();
        try {
            JSONArray values = new JSONArray(prefs.getString(STATIONS, "[]"));
            for (int i = 0; i < values.length() && result.size() < MAX_STATIONS; i++) {
                Station s = fromJson(values.optJSONObject(i));
                if (s != null) result.add(s);
            }
        } catch (JSONException ignored) {
            return new Snapshot("", 0L, new ArrayList<>());
        }
        return new Snapshot(type, fetched, result);
    }

    private static boolean validType(String s) {
        return "e5".equals(s) || "e10".equals(s) || "diesel".equals(s);
    }

    private static Station fromJson(JSONObject s) {
        if (s == null) return null;
        double lat = s.optDouble("lat", Double.NaN);
        double lng = s.optDouble("lng", Double.NaN);
        double price = s.optDouble("price", -1);
        if (FuelRules.stationCoordinates(lat, lng) == null
                || !Double.isFinite(price) || price <= 0 || price >= 9) return null;
        String name = s.optString("name", "Tankstelle").trim();
        if (name.isEmpty()) name = "Tankstelle";
        if (name.length() > 85) name = name.substring(0, 85);
        String address = s.optString("address", "").trim();
        if (address.isEmpty()) {
            String street = s.optString("street", "");
            String town = s.optString("place", "");
            address = (street + ", " + town).trim();
        }
        if (address.length() > 150) address = address.substring(0, 150);
        return new Station(name, address, lat, lng, price,
            s.optBoolean("isOpen", false));
    }
}
