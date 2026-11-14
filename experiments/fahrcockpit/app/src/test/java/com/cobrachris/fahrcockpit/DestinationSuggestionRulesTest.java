package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

public final class DestinationSuggestionRulesTest {
    @Test public void matchesSavedTargetsAndNamesCaseInsensitively() {
        assertTrue(DestinationSuggestionRules.matches("haus", "Zuhause", "Musterstraße 1"));
        assertTrue(DestinationSuggestionRules.matches("bark", "Zuhause", "Barkhausenweg 1"));
        assertFalse(DestinationSuggestionRules.matches("xy", "Arbeit", "Barkhausenweg 1"));
        assertFalse(DestinationSuggestionRules.matches(" ", "Arbeit", "Barkhausenweg 1"));
    }

    @Test public void waitsForEnoughTypedCharactersBeforeGeocoding() {
        assertFalse(DestinationSuggestionRules.shouldQueryAndroidGeocoder("Ab"));
        assertTrue(DestinationSuggestionRules.shouldQueryAndroidGeocoder("Bark"));
        assertEquals(5, DestinationSuggestionRules.MAX_VISIBLE_SUGGESTIONS);
        assertTrue(DestinationSuggestionRules.GEOCODER_DELAY_MS >= 500);
    }

    @Test public void paidPlacesAutocompleteIsUnconditionallyDisabled() {
        assertFalse(DestinationSuggestionRules.googlePlacesEnabled());
    }
}
