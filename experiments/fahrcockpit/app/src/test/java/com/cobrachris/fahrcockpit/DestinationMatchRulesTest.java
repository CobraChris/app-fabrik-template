package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

public final class DestinationMatchRulesTest {
    @Test public void recognizesAddressLabelsAndIgnoresCase() {
        assertTrue(DestinationMatchRules.matches("bark", "Barkhausenweg 1", "Hamburg"));
        assertTrue(DestinationMatchRules.matches("HAMBURG", "Zuhause", "Hamburg Eppendorf"));
        assertFalse(DestinationMatchRules.matches("Muenchen", "Arbeit", "Hamburg"));
    }

    @Test public void emptyInputShowsFavoritesAndRecent() {
        assertTrue(DestinationMatchRules.matches("", "Zuhause", "Hamburg"));
        assertTrue(DestinationMatchRules.matches(null, "Arbeit", "Hamburg"));
    }

    @Test public void filtersAndTruncatesLongLabels() {
        assertNull(DestinationMatchRules.label(" "));
        assertEquals(64, DestinationMatchRules.label("a".repeat(75)).length());
        assertTrue(DestinationMatchRules.sameTarget(" Hamburg ", "hamburg"));
    }

    @Test public void avoidsOnlineLookupForCoordinatesOrShortInput() {
        assertFalse(DestinationMatchRules.maySuggestOnline("Ab"));
        assertFalse(DestinationMatchRules.maySuggestOnline("53.5511,10.0000"));
        assertTrue(DestinationMatchRules.maySuggestOnline("Barkhausenweg"));
    }
}
