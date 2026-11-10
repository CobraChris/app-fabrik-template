package com.cobrachris.fahrcockpit;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Pure, no-billing Google Maps navigation handoff rules.
 * Does not claim to calculate routes or provide embedded turn-by-turn guidance.
 */
final class RouteTargetRules {
    static final int MAX_LENGTH = 160;
    private RouteTargetRules() {}

    static String clean(String raw) {
        if (raw == null) return null;
        String value = raw.replaceAll("\\s+", " ").trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH) return null;
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) return null;
        }
        return value;
    }

    static String coordinates(String latitude, String longitude) {
        if (latitude == null || longitude == null) return null;
        try {
            double lat = Double.parseDouble(latitude);
            double lng = Double.parseDouble(longitude);
            if (!Double.isFinite(lat) || !Double.isFinite(lng)
                    || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                return null;
            }
            return String.format(Locale.US, "%.6f,%.6f", lat, lng);
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    static String directionsUrl(String raw) {
        String target = clean(raw);
        if (target == null) return null;
        return "https://www.google.com/maps/dir/?api=1&destination="
            + encode(target) + "&travelmode=driving";
    }

    static String navigationUri(String raw) {
        String target = clean(raw);
        if (target == null) return null;
        return "google.navigation:q=" + encode(target) + "&mode=d";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
