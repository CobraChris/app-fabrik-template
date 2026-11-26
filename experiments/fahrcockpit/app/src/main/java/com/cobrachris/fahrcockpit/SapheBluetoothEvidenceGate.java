package com.cobrachris.fahrcockpit;

/**
 * Foreground-only volatile debounce for Android BLE GATT observations.
 * One missing 5-second sample is not a reliable disconnect; two consecutive
 * complete negative samples are required before showing red.
 * Invalid/unavailable readings return UNKNOWN and never prove disconnection.
 */
final class SapheBluetoothEvidenceGate {
    private static final int NEGATIVE_CONFIRMATIONS = 2;
    private int consecutiveNegative;
    private SapheIndicatorRules.Evidence current =
        SapheIndicatorRules.Evidence.UNKNOWN;

    SapheIndicatorRules.Evidence sample(SapheBluetoothStatus.Link link) {
        if (link == SapheBluetoothStatus.Link.OBSERVED_CONNECTED) {
            consecutiveNegative = 0;
            current = SapheIndicatorRules.Evidence.OBSERVED_CONNECTED;
        } else if (link == SapheBluetoothStatus.Link.NOT_OBSERVED) {
            consecutiveNegative = Math.min(NEGATIVE_CONFIRMATIONS,
                consecutiveNegative + 1);
            current = consecutiveNegative >= NEGATIVE_CONFIRMATIONS
                ? SapheIndicatorRules.Evidence.NOT_OBSERVED
                : SapheIndicatorRules.Evidence.UNKNOWN;
        } else {
            reset();
        }
        return current;
    }

    SapheIndicatorRules.Evidence current() {
        return current;
    }

    void reset() {
        consecutiveNegative = 0;
        current = SapheIndicatorRules.Evidence.UNKNOWN;
    }
}
