package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

public final class FuelPriceFormatTest {
    @Test public void showsExactTenthsOfCentInsteadOfMisleadingRoundingToCents() {
        assertEquals("1,994 €/l", FuelPriceFormat.perLiter(1.994));
        assertEquals("2,009 €/l", FuelPriceFormat.perLiter(2.009));
        assertEquals("1,994 €", FuelPriceFormat.compact(1.994));
        assertEquals("2,009 €", FuelPriceFormat.compact(2.009));
        assertEquals("2,000 €/l", FuelPriceFormat.perLiter(2));
    }
    @Test public void refusesInvalidOrUnavailablePrices() {
        for(double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, -1, 0, 9.0, 900.0}) {
            assertFalse(FuelPriceFormat.valid(invalid));
            assertEquals("Preis nicht gemeldet", FuelPriceFormat.perLiter(invalid));
            assertEquals("–", FuelPriceFormat.compact(invalid));
        }
    }
}
