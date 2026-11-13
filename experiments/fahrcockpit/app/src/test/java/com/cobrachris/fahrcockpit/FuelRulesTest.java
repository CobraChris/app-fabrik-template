package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

public final class FuelRulesTest {
    private static final String KEY = "12345678-abcd-1234-9876-abcdef123456";
    @Test public void validatesKey() {
        assertTrue(FuelRules.validKey(KEY));
        assertFalse(FuelRules.validKey(""));
        assertFalse(FuelRules.validKey("secret&x=1"));
        assertFalse(FuelRules.validKey(null));
    }
    @Test public void supportsOnlyGermanArea() {
        assertTrue(FuelRules.inGermany(53.55, 10.0));
        assertFalse(FuelRules.inGermany(35.68, 139.69));
    }
    @Test public void generatedUrlUsesHttpsAndAllowedParameters() {
        String url = FuelRules.requestUrl(KEY, 53.5511, 10.0, 10, "e10");
        assertTrue(url.startsWith("https://creativecommons.tankerkoenig.de/json/list.php?"));
        assertTrue(url.contains("rad=10&type=e10&sort=price&apikey=" + KEY));
    }
    @Test public void acceptsFuelStationCoordinatesWithoutNavigationRequest() {
        assertEquals("53.551100,10.000000",
            FuelRules.stationCoordinates(53.5511, 10.0));
        assertNull(FuelRules.stationCoordinates(Double.NaN, 10.0));
        assertNull(FuelRules.stationCoordinates(91.0, 10.0));
        assertNull(FuelRules.stationCoordinates(35.68, 139.69));
    }

    @Test(expected=IllegalArgumentException.class)
    public void blocksExcessiveRadius() {
        FuelRules.requestUrl(KEY, 53.55, 10, 50, "e5");
    }
    @Test(expected=IllegalArgumentException.class)
    public void blocksInjectionAsFuelType() {
        FuelRules.requestUrl(KEY, 53.55, 10, 10, "e5&some=other");
    }
}
