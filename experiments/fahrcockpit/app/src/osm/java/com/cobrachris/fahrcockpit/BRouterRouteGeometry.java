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

    static final class Turn {
        final int pointIndex;
        final int code;
        final int exit;
        Turn(int pointIndex, int code, int exit) {
            this.pointIndex = pointIndex;
            this.code = code;
            this.exit = exit;
        }

        /** BRouter FormatJson / Formatter numeric maneuver codes. */
        String germanInstruction() {
            switch (code) {
                case 1: return "Geradeaus weiterfahren";
                case 2: return "Links abbiegen";
                case 3: return "Leicht links abbiegen";
                case 4: return "Scharf links abbiegen";
                case 5: return "Rechts abbiegen";
                case 6: return "Leicht rechts abbiegen";
                case 7: return "Scharf rechts abbiegen";
                case 8: return "Links halten";
                case 9: return "Rechts halten";
                case 10: case 15: return "Bitte wenden";
                case 11: return "Rechts wenden";
                case 13: case 14:
                    return exit > 0 ? "Im Kreisverkehr die " + exit
                        + ". Ausfahrt nehmen" : "In den Kreisverkehr einfahren";
                case 17: return "Links abfahren";
                case 18: return "Rechts abfahren";
                default: return null; // Never invent an unknown maneuver.
            }
        }
    }

    static final class Route {
        final String geoJson;
        final List<double[]> points; // independent BRouter lat, lon geometry
        final List<Turn> turns;
        Route(String json, List<double[]> points, List<Turn> turns) {
            this.geoJson = json;
            this.points = Collections.unmodifiableList(points);
            this.turns = Collections.unmodifiableList(turns);
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
            // FormatJson puts voicehints into properties of the first feature.
            // [indexInTrack, commandIndex, exitNumber, distanceToNext, angle].
            // Missing hints are a non-guidable route, never guessed turns.
            List<Turn> turns = new ArrayList<>();
            JsonObject props = first.getAsJsonObject("properties");
            JsonArray hints = props == null ? null : props.getAsJsonArray("voicehints");
            if (hints != null) {
                if (hints.size() > 10000)
                    throw new IllegalArgumentException("Too many voice hints");
                int previous = -1;
                for (JsonElement element : hints) {
                    JsonArray hint = element.getAsJsonArray();
                    if (hint.size() < 5) throw new IllegalArgumentException("Incomplete voice hint");
                    int index = hint.get(0).getAsInt();
                    int code = hint.get(1).getAsInt();
                    int exit = hint.get(2).getAsInt();
                    if (index < 0 || index >= points.size() || index <= previous
                            || code < 1 || code > 18 || exit < 0 || exit > 30)
                        throw new IllegalArgumentException("Invalid voice hint");
                    previous = index;
                    Turn turn = new Turn(index, code, exit);
                    if (turn.germanInstruction() != null) turns.add(turn);
                }
            }
            // MapLibre renders only the independent BRouter LineString; other
            // features are ignored. Turn hints come from BRouter, not geometry.
            return new Route(raw, points, turns);
        } catch (RuntimeException malformed) {
            throw new IllegalArgumentException("BRouter returned invalid route geometry", malformed);
        }
    }
}
