package com.cobrachris.fahrcockpit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Display-only filter: current position is never persisted or transmitted. */
final class FuelNearbySelection {
    static final int MAX_VISIBLE_STATIONS = 40;

    private FuelNearbySelection() {}

    static FuelMapSnapshot.Snapshot nearby(FuelMapSnapshot.Snapshot original,
                                           double currentLat, double currentLng,
                                           int radiusKm) {
        if (original == null) return empty();
        if (!FuelRules.inGermany(currentLat, currentLng)
                || radiusKm < 1 || radiusKm > 25) return empty();
        List<FuelMapSnapshot.Station> nearby = new ArrayList<>();
        for (FuelMapSnapshot.Station station : original.stations) {
            double distance = FuelAutoRefreshPolicy.distanceMeters(
                currentLat, currentLng, station.lat, station.lng);
            if (distance <= radiusKm * 1000d) nearby.add(station);
        }
        // Select nearest stations to the *moving car*, not cheapest stations
        // close to an earlier search center. Stable for equal distances.
        nearby.sort(Comparator.comparingDouble(station ->
            FuelAutoRefreshPolicy.distanceMeters(currentLat, currentLng,
                station.lat, station.lng)));
        if (nearby.size() > MAX_VISIBLE_STATIONS)
            nearby = new ArrayList<>(nearby.subList(0, MAX_VISIBLE_STATIONS));
        return new FuelMapSnapshot.Snapshot(original.type, original.fetchedAt, nearby);
    }

    private static FuelMapSnapshot.Snapshot empty() {
        return new FuelMapSnapshot.Snapshot("", 0L, new ArrayList<>());
    }
}
