package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

public final class FuelNearbySelectionTest {
    private static FuelMapSnapshot.Station station(double lat, double lng, double price) {
        return new FuelMapSnapshot.Station("Tankstelle", "Testweg", lat, lng, price, true);
    }
    private static FuelMapSnapshot.Snapshot sample(FuelMapSnapshot.Station... stations) {
        List<FuelMapSnapshot.Station> input = new ArrayList<>();
        java.util.Collections.addAll(input, stations);
        return new FuelMapSnapshot.Snapshot("diesel", 123456L, input);
    }

    @Test public void showsStationsNearMovingVehicleNotPreviousSearchCenter() {
        FuelMapSnapshot.Station first = station(53.5500, 10.0000, 1.99);
        FuelMapSnapshot.Station next = station(53.5500, 10.0700, 1.70);
        FuelMapSnapshot.Station distant = station(53.5500, 10.2500, 1.20);
        FuelMapSnapshot.Snapshot original = sample(distant, first, next);
        FuelMapSnapshot.Snapshot nearStart =
            FuelNearbySelection.nearby(original, 53.55, 10.00, 5);
        assertEquals(2, nearStart.stations.size());
        assertSame(first, nearStart.stations.get(0));
        FuelMapSnapshot.Snapshot afterDriving =
            FuelNearbySelection.nearby(original, 53.55, 10.25, 5);
        assertEquals(1, afterDriving.stations.size());
        assertSame(distant, afterDriving.stations.get(0));
        assertEquals("diesel", afterDriving.type);
        assertEquals(123456L, afterDriving.fetchedAt);
    }

    @Test public void nearestFortyWinRegardlessOfPrice() {
        List<FuelMapSnapshot.Station> many = new ArrayList<>();
        for (int i = 99; i >= 0; i--)
            many.add(station(53.55 + i * 0.0004, 10.0, 1.10 + i * 0.01));
        FuelMapSnapshot.Snapshot original =
            new FuelMapSnapshot.Snapshot("diesel", 500L, many);
        FuelMapSnapshot.Snapshot result =
            FuelNearbySelection.nearby(original, 53.55, 10, 10);
        assertEquals(FuelNearbySelection.MAX_VISIBLE_STATIONS, result.stations.size());
        assertEquals(53.55, result.stations.get(0).lat, 0.0000001);
        assertEquals(53.55 + 39 * 0.0004,
            result.stations.get(39).lat, 0.0000001);
    }

    @Test public void refusesInvalidPositionAndExcludesNonlocalStations() {
        FuelMapSnapshot.Snapshot original = sample(station(53.55, 10, 1.79));
        assertTrue(FuelNearbySelection.nearby(original, Double.NaN, 10, 10)
            .stations.isEmpty());
        assertTrue(FuelNearbySelection.nearby(original, 53.55, 10, 0)
            .stations.isEmpty());
        assertTrue(FuelNearbySelection.nearby(original, 48, 11, 10)
            .stations.isEmpty());
    }
}
