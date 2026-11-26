package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

/** Physical BLE link may be confirmed even when cockpit notifications are off. */
public final class SapheIndicatorRulesTest {
    @Test public void unknownBluetoothNeverClaimsHardwareConnected() {
        assertEquals(SapheIndicatorRules.State.DETECTION_READY_UNKNOWN,
            SapheIndicatorRules.state(true,true,true));
        assertEquals(SapheIndicatorRules.State.DETECTION_READY_UNKNOWN,
            SapheIndicatorRules.state(true,true,true,
                SapheIndicatorRules.Evidence.UNKNOWN));
        assertTrue(SapheIndicatorRules.description(true,true,true)
            .contains("nicht bestätigt"));
    }

    @Test public void greenOnlyForExplicitGattObservationIndependentlyOfNotifications() {
        for(boolean access : new boolean[]{true,false})
            for(boolean optedIn : new boolean[]{true,false}) {
                assertEquals(SapheIndicatorRules.State.DEVICE_OBSERVED_CONNECTED,
                    SapheIndicatorRules.state(true,access,optedIn,
                        SapheIndicatorRules.Evidence.OBSERVED_CONNECTED));
                assertEquals(SapheIndicatorRules.State.DETECTION_READY_UNKNOWN,
                    SapheIndicatorRules.state(true,access,optedIn,
                        SapheIndicatorRules.Evidence.UNKNOWN));
            }
        assertEquals(SapheIndicatorRules.State.ACTION_REQUIRED,
            SapheIndicatorRules.state(false,true,true,
                SapheIndicatorRules.Evidence.OBSERVED_CONNECTED));
        assertTrue(SapheIndicatorRules.description(true,true,true,
            SapheIndicatorRules.Evidence.OBSERVED_CONNECTED)
            .contains("Warnbereitschaft nicht überprüft"));
    }

    @Test public void redWhenSelectedDeviceNotObservedWithoutClaimingSapheFailure() {
        assertEquals(SapheIndicatorRules.State.DEVICE_NOT_OBSERVED,
            SapheIndicatorRules.state(true,true,true,
                SapheIndicatorRules.Evidence.NOT_OBSERVED));
        String explanation=SapheIndicatorRules.description(true,true,true,
            SapheIndicatorRules.Evidence.NOT_OBSERVED);
        assertTrue(explanation.contains("nicht in Androids"));
        assertTrue(explanation.contains("keine sichere Aussage"));
    }

    @Test public void notificationPermissionsDoNotImplyBluetoothDisconnect() {
        assertEquals(SapheIndicatorRules.State.ACTION_REQUIRED,
            SapheIndicatorRules.state(false,true,true));
        assertEquals(SapheIndicatorRules.State.DETECTION_READY_UNKNOWN,
            SapheIndicatorRules.state(true,false,true));
        assertEquals(SapheIndicatorRules.State.DETECTION_READY_UNKNOWN,
            SapheIndicatorRules.state(true,true,false));
        assertTrue(SapheIndicatorRules.description(false,true,true)
            .contains("nicht installiert"));
        assertTrue(SapheIndicatorRules.description(true,false,true)
            .contains("Benachrichtigungszugriff"));
        assertTrue(SapheIndicatorRules.description(true,true,false)
            .contains("Warnanzeige ausgeschaltet"));
    }
}
