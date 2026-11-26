package com.cobrachris.fahrcockpit;

import java.util.Locale;

/** Fuel prices are published to the mill (0.1 cent). Never round to cents. */
final class FuelPriceFormat {
    private FuelPriceFormat() {}

    static boolean valid(double price) {
        return Double.isFinite(price) && price > 0d && price < 9d;
    }

    static String perLiter(double price) {
        return valid(price)
            ? String.format(Locale.GERMANY, "%.3f €/l", price)
            : "Preis nicht gemeldet";
    }

    static String compact(double price) {
        return valid(price)
            ? String.format(Locale.GERMANY, "%.3f €", price)
            : "–";
    }
}
