package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

/** Fail closed: notification readiness is not itself a BLE connection. */
public final class SapheIndicatorRulesTest {
    @Test public void readyDoesNotClaimHardwareConnectedWithoutGATTObservation() {
        assertEquals(SapheIndicatorRules.State.DETECTION_READY_UNKNOWN,
            SapheIndicatorRules.state(true,true,true));
        assertEquals(SapheIndicatorRules.State.DETECTION_READY_UNKNOWN,
            SapheIndicatorRules.state(true,true,true,
                SapheIndicatorRules.Evidence.UNKNOWN));
        assertTrue(SapheIndicatorRules.description(true,true,true)
            .contains("nicht bestätigt"));
    }

    @Test public void greenOnlyForExplicitObservedHardwareAndWorkingDetection() {
        assertEquals(SapheIndicatorRules.State.DEVICE_OBSERVED_CONNECTED,
            SapheIndicatorRules.state(true,true,true,
                SapheIndicatorRules.Evidence.OBSERVED_CONNECTED));
        assertTrue(SapheIndicatorRules.description(true,true,true,
            SapheIndicatorRules.Evidence.OBSERVED_CONNECTED)
            .contains("Warnbereitschaft nicht überprüft"));
        for (SapheIndicatorRules.Evidence evidence : SapheIndicatorRules.Evidence.values()) {
            assertEquals(SapheIndicatorRules.State.ACTION_REQUIRED,
                SapheIndicatorRules.state(false,true,true,evidence));
            assertEquals(SapheIndicatorRules.State.ACTION_REQUIRED,
                SapheIndicatorRules.state(true,false,true,evidence));
            assertEquals(SapheIndicatorRules.State.ACTION_REQUIRED,
                SapheIndicatorRules.state(true,true,false,evidence));
        }
    }

    @Test public void redWhenSelectedDeviceNotObservedButDoesNotClaimFailureOfSapheLink() {
        assertEquals(SapheIndicatorRules.State.DEVICE_NOT_OBSERVED,
            SapheIndicatorRules.state(true,true,true,
                SapheIndicatorRules.Evidence.NOT_OBSERVED));
        String explanation=SapheIndicatorRules.description(true,true,true,
            SapheIndicatorRules.Evidence.NOT_OBSERVED);
        assertTrue(explanation.contains("nicht in Androids"));
        assertTrue(explanation.contains("keine sichere Aussage"));
    }

    @Test public void missingAppPermissionOrOptInNeedsAction() {
        assertEquals(SapheIndicatorRules.State.ACTION_REQUIRED,
            SapheIndicatorRules.state(false,true,true));
        assertEquals(SapheIndicatorRules.State.ACTION_REQUIRED,
            SapheIndicatorRules.state(true,false,true));
        assertEquals(SapheIndicatorRules.State.ACTION_REQUIRED,
            SapheIndicatorRules.state(true,true,false));
        assertTrue(SapheIndicatorRules.description(false,true,true)
            .contains("nicht installiert"));
        assertTrue(SapheIndicatorRules.description(true,false,true)
            .contains("Benachrichtigungszugriff"));
        assertTrue(SapheIndicatorRules.description(true,true,false)
            .contains("ausgeschaltet"));
    }
}
