package com.cobrachris.fahrcockpit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validate a BRouter GeoJSON LineString before passing it to MapLibre.
 * No Google Maps SDK, Google route, Google POI or Google SDK coordinates
 * are ever used here. Inputs are BRouter's independent offline results.
 */
final class BRouterRouteGeometry {
    private static final int MAX_CHARS = 6_000_000;
    private static final int MAX_POINTS = 150_000;

    static final class Route {
        final String geoJson;
        final List<double[]> points; // lat, lon for native location checks
        Route(String json, List<double[]> points) {
            this.geoJson = json;
            this.points = Collections.unmodifiableList(points);
        }
    }
    private BRouterRouteGeometry() {}

    static Route parse(String raw) {
        if (raw == null || raw.length() < 50 || raw.length() > MAX_CHARS)
            throw new IllegalArgumentException("No valid BRouter route result");
        try {
            JsonObject fc = JsonParser.parseString(raw).getAsJsonObject();
            if (!"FeatureCollection".equals(fc.get("type").getAsString()))
                throw new IllegalArgumentException("Not GeoJSON");
            JsonArray features = fc.getAsJsonArray("features");
            if (features == null || features.isEmpty()) throw new IllegalArgumentException("No features");
            JsonObject first = features.get(0).getAsJsonObject();
            JsonObject geom = first.getAsJsonObject("geometry");
            if (geom == null || !"LineString".equals(geom.get("type").getAsString()))
                throw new IllegalArgumentException("Not a route line");
            JsonArray coords = geom.getAsJsonArray("coordinates");
            if (coords == null || coords.size() < 2 || coords.size() > MAX_POINTS)
                throw new IllegalArgumentException("Invalid route length");
            List<double[]> points = new ArrayList<>(coords.size());
            for (JsonElement e : coords) {
                JsonArray pair = e.getAsJsonArray();
                if (pair.size() < 2) throw new IllegalArgumentException("Missing coordinates");
                double lon = pair.get(0).getAsDouble();
                double lat = pair.get(1).getAsDouble();
                if (!Double.isFinite(lat) || !Double.isFinite(lon)
                        || lat < -90 || lat > 90 || lon < -180 || lon > 180)
                    throw new IllegalArgumentException("Bad coordinates");
                points.add(new double[] {lat, lon});
            }
            // Return the exact validated route. MapLibre's LineLayer renders
            // only the LineString; other BRouter POI features are ignored.
            return new Route(raw, points);
        } catch (RuntimeException malformed) {
            throw new IllegalArgumentException("BRouter returned invalid route geometry", malformed);
        }
    }
}
