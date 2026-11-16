package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public final class DestinationStoreTest {
    @Test public void managerIncludesModernFavoritesOlderFavoritesAndHistory() {
        List<DestinationStore.Entry> entries = DestinationStore.combineManageable(
            Arrays.asList(new DestinationStore.Entry("Eppendorf HH",
                "Hamburg Eppendorf", DestinationStore.Source.FAVORITE)),
            Arrays.asList(new DestinationStore.Entry("kyoto", "kyoto",
                DestinationStore.Source.LEGACY_FAVORITE)),
            Arrays.asList(new DestinationStore.Entry("Barkhausenweg 1",
                "Barkhausenweg 1, Hamburg", DestinationStore.Source.RECENT)));
        assertEquals(3, entries.size());
        assertEquals("Eppendorf HH", entries.get(0).label);
        assertEquals(DestinationStore.Source.FAVORITE, entries.get(0).source);
        assertEquals("kyoto", entries.get(1).label);
        assertEquals(DestinationStore.Source.LEGACY_FAVORITE, entries.get(1).source);
        assertTrue(entries.get(1).favorite);
        assertEquals(DestinationStore.Source.RECENT, entries.get(2).source);
        assertFalse(entries.get(2).favorite);
    }

    @Test public void managerPrefersModernFavoriteForDuplicateDestination() {
        List<DestinationStore.Entry> entries = DestinationStore.combineManageable(
            Arrays.asList(new DestinationStore.Entry("Zuhause", "Hamburg",
                DestinationStore.Source.FAVORITE)),
            Arrays.asList(new DestinationStore.Entry("Hamburg", "hamburg",
                DestinationStore.Source.LEGACY_FAVORITE)),
            Arrays.asList(new DestinationStore.Entry("Letzte Fahrt", " HAMBURG ",
                DestinationStore.Source.RECENT)));
        assertEquals(1, entries.size());
        assertEquals("Zuhause", entries.get(0).label);
        assertEquals(DestinationStore.Source.FAVORITE, entries.get(0).source);
    }

    @Test public void legacyFavoriteOutranksRecentAndLabelsAreNotUniqueKeys() {
        List<DestinationStore.Entry> entries = DestinationStore.combineManageable(
            Arrays.asList(new DestinationStore.Entry("Ziel", "Berlin",
                DestinationStore.Source.FAVORITE)),
            Arrays.asList(new DestinationStore.Entry("Ziel", "Kyoto",
                DestinationStore.Source.LEGACY_FAVORITE)),
            Arrays.asList(new DestinationStore.Entry("Verlauf", "kyoto",
                DestinationStore.Source.RECENT)));
        assertEquals(2, entries.size());
        assertEquals(DestinationStore.Source.LEGACY_FAVORITE, entries.get(1).source);
    }

    @Test public void legacyDeleteKeysContainOnlyExactMatchingSlots() {
        Map<String, Object> preferences = new HashMap<>();
        preferences.put("target_0", "kyoto");
        preferences.put("target_1", "Hamburg");
        preferences.put("target_2", " KYOTO ");
        preferences.put("target_3", "Tokio");
        preferences.put("other_setting", "do not touch");
        assertEquals(Arrays.asList("target_0", "target_2"),
            DestinationStore.matchingLegacyKeys(preferences, "Kyoto"));
        assertTrue(DestinationStore.matchingLegacyKeys(preferences, "Osaka").isEmpty());
    }

    @Test public void emptyDestinationManagerIsEmpty() {
        assertTrue(DestinationStore.combineManageable(
            Arrays.asList(), Arrays.asList(), Arrays.asList()).isEmpty());
    }
}
