package com.cobrachris.fahrcockpit;

import java.util.Locale;

/** Pure policies shared by the native fixed-pane shell and Nav SDK adapter. */
final class IntegratedNavigationRules {
    static final int MAP_WEIGHT = 7;
    static final int CONTROLS_WEIGHT = 3;
    static final String ENGINE_CLASS =
        "com.cobrachris.fahrcockpit.GoogleNavigationEngine";
    private IntegratedNavigationRules() {}

    static boolean isValidTarget(String value) {
        return RouteTargetRules.clean(value) != null;
    }

    /** User-supplied coordinates must be range checked before SDK route requests. */
    static double[] parseCoordinates(String value) {
        String clean = RouteTargetRules.clean(value);
        if (clean == null) return null;
        String[] parts = clean.split(",", -1);
        if (parts.length != 2) return null;
        String normalized = RouteTargetRules.coordinates(
            parts[0].trim(), parts[1].trim());
        if (normalized == null) return null;
        String[] result = normalized.split(",", -1);
        return new double[] {Double.parseDouble(result[0]),
            Double.parseDouble(result[1])};
    }

    static String displayCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) return null;
        return String.format(Locale.US, "%.6f, %.6f", latitude, longitude);
    }
}
