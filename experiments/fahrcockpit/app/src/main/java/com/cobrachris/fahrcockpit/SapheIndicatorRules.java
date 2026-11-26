package com.cobrachris.fahrcockpit;

/**
 * Device connection and FahrCockpit's optional notification detection are
 * independent. RED only for Saphe app missing or selected device not observed,
 * AMBER for unknown BT state, GREEN for explicitly chosen GATT device seen
 * connected by Android (not proof that warnings or vendor app are working).
 */
final class SapheIndicatorRules {
    enum State {
        ACTION_REQUIRED,          // Red: official Saphe Link app missing.
        DETECTION_READY_UNKNOWN,  // Amber: BT identity/observation unknown.
        DEVICE_NOT_OBSERVED,      // Red: selected device absent from GATT.
        DEVICE_OBSERVED_CONNECTED // Green: explicitly chosen device in GATT.
    }

    enum Evidence { UNKNOWN, OBSERVED_CONNECTED, NOT_OBSERVED }

    private SapheIndicatorRules() {}

    static State state(boolean appInstalled, boolean notificationAccess,
                       boolean optedIntoProbe) {
        return state(appInstalled, notificationAccess, optedIntoProbe,
            Evidence.UNKNOWN);
    }

    static State state(boolean appInstalled, boolean notificationAccess,
                       boolean optedIntoProbe, Evidence evidence) {
        // Do not mistake missing FahrCockpit notification access for a BLE
        // disconnect: Saphe may still be connected and work independently.
        if (!appInstalled) return State.ACTION_REQUIRED;
        if (evidence == Evidence.OBSERVED_CONNECTED)
            return State.DEVICE_OBSERVED_CONNECTED;
        if (evidence == Evidence.NOT_OBSERVED)
            return State.DEVICE_NOT_OBSERVED;
        return State.DETECTION_READY_UNKNOWN;
    }

    static String description(boolean appInstalled, boolean notificationAccess,
                              boolean optedIntoProbe) {
        return description(appInstalled, notificationAccess, optedIntoProbe,
            Evidence.UNKNOWN);
    }

    static String description(boolean appInstalled, boolean notificationAccess,
                              boolean optedIntoProbe, Evidence evidence) {
        if (!appInstalled) return "Saphe-App nicht installiert; Verbindung unbekannt";
        String detector = !notificationAccess
            ? " FahrCockpit-Benachrichtigungszugriff fehlt."
            : !optedIntoProbe
                ? " FahrCockpit-Saphe-Warnanzeige ausgeschaltet."
                : "";
        if (evidence == Evidence.OBSERVED_CONNECTED)
            return "Gewähltes Saphe-Bluetooth-Gerät laut Android verbunden. "
                + "Saphe-Warnbereitschaft nicht überprüft." + detector;
        if (evidence == Evidence.NOT_OBSERVED)
            return "Gewähltes Saphe-Bluetooth-Gerät derzeit nicht in Androids "
                + "GATT-Verbindungsliste; keine sichere Aussage über Saphe Link."
                + detector;
        return "Bluetooth-Geräteverbindung nicht bestätigt." + detector;
    }
}
