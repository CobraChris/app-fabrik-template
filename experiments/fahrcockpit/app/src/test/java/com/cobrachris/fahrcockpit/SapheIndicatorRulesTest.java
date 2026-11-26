package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

/** Notification readiness is not proof of Saphe Mini Bluetooth connection. */
public final class SapheIndicatorRulesTest {
    @Test public void readyNeverClaimsHardwareConnected() {
        SapheIndicatorRules.State state =
            SapheIndicatorRules.state(true,true,true);
        assertEquals(SapheIndicatorRules.State.DETECTION_READY_UNKNOWN,state);
        assertTrue(SapheIndicatorRules.description(true,true,true)
            .contains("nicht bestätigt"));
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
