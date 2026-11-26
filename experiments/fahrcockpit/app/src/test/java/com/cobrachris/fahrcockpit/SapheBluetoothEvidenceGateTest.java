package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

/** Absence must be corroborated; unavailable Android inventory is unknown. */
public final class SapheBluetoothEvidenceGateTest {
    @Test public void oneNegativeSampleNeverProvesDisconnect() {
        SapheBluetoothEvidenceGate gate = new SapheBluetoothEvidenceGate();
        assertEquals(SapheIndicatorRules.Evidence.UNKNOWN,
            gate.sample(SapheBluetoothStatus.Link.NOT_OBSERVED));
        assertEquals(SapheIndicatorRules.Evidence.UNKNOWN, gate.current());
        assertEquals(SapheIndicatorRules.Evidence.NOT_OBSERVED,
            gate.sample(SapheBluetoothStatus.Link.NOT_OBSERVED));
    }

    @Test public void positiveSampleImmediatelyProvesObservedLink() {
        SapheBluetoothEvidenceGate gate = new SapheBluetoothEvidenceGate();
        assertEquals(SapheIndicatorRules.Evidence.OBSERVED_CONNECTED,
            gate.sample(SapheBluetoothStatus.Link.OBSERVED_CONNECTED));
        assertEquals(SapheIndicatorRules.Evidence.UNKNOWN,
            gate.sample(SapheBluetoothStatus.Link.NOT_OBSERVED));
        assertEquals(SapheIndicatorRules.Evidence.OBSERVED_CONNECTED,
            gate.sample(SapheBluetoothStatus.Link.OBSERVED_CONNECTED));
    }

    @Test public void failedInventoryInvalidatesOldEvidence() {
        SapheBluetoothEvidenceGate gate = new SapheBluetoothEvidenceGate();
        gate.sample(SapheBluetoothStatus.Link.OBSERVED_CONNECTED);
        assertEquals(SapheIndicatorRules.Evidence.UNKNOWN,
            gate.sample(SapheBluetoothStatus.Link.UNKNOWN));
        gate.sample(SapheBluetoothStatus.Link.NOT_OBSERVED);
        assertEquals(SapheIndicatorRules.Evidence.UNKNOWN,gate.current());
        gate.sample(SapheBluetoothStatus.Link.NOT_OBSERVED);
        assertEquals(SapheIndicatorRules.Evidence.NOT_OBSERVED,gate.current());
        gate.reset();
        assertEquals(SapheIndicatorRules.Evidence.UNKNOWN,gate.current());
    }

    @Test public void repeatedMissingSamplesRemainStable() {
        SapheBluetoothEvidenceGate gate = new SapheBluetoothEvidenceGate();
        for (int i=0;i<5;i++) {
            SapheIndicatorRules.Evidence seen = gate.sample(
                SapheBluetoothStatus.Link.NOT_OBSERVED);
            assertEquals(i==0?SapheIndicatorRules.Evidence.UNKNOWN
                :SapheIndicatorRules.Evidence.NOT_OBSERVED,seen);
        }
    }
}
