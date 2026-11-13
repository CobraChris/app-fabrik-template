package com.cobrachris.fahrcockpit;

import java.util.Locale;
import java.util.regex.Pattern;

public final class FuelRules {
    private static final Pattern KEY = Pattern.compile(
        "[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}");

    private FuelRules() {}

    public static boolean validKey(String value) {
        return value != null && KEY.matcher(value.trim()).matches();
    }

    public static boolean inGermany(double lat, double lng) {
        // Rough geographic bounding box, not a legal national-border test.
        return lat >= 47.1 && lat <= 55.2 && lng >= 5.5 && lng <= 15.6;
    }

    // Pure validation for safe in-app handoff; never initiates a route request.
    public static String stationCoordinates(double lat, double lng) {
        if (!Double.isFinite(lat) || !Double.isFinite(lng)
                || !inGermany(lat, lng)) return null;
        return String.format(Locale.US, "%.6f,%.6f", lat, lng);
    }

    public static String requestUrl(String key, double lat, double lng,
                                    int radiusKm, String type) {
        if (!validKey(key)) throw new IllegalArgumentException("Ungültiger API-Key");
        if (!Double.isFinite(lat) || !Double.isFinite(lng) || !inGermany(lat, lng)) {
            throw new IllegalArgumentException("Tankerkönig liefert nur Daten für Deutschland");
        }
        if (radiusKm < 1 || radiusKm > 25) {
            throw new IllegalArgumentException("Suchradius 1 bis 25 km");
        }
        if (!"e5".equals(type) && !"e10".equals(type) && !"diesel".equals(type)) {
            throw new IllegalArgumentException("Unbekannte Kraftstoffsorte");
        }
        return String.format(Locale.US,
            "https://creativecommons.tankerkoenig.de/json/list.php?lat=%.6f&lng=%.6f" +
            "&rad=%d&type=%s&sort=price&apikey=%s",
            lat, lng, radiusKm, type, key.trim());
    }
}
