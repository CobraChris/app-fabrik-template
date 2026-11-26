package com.cobrachris.fahrcockpit;

/**
 * Do not interpret Android NotificationListener permission as a Bluetooth
 * connection. Green is only for an explicitly selected BLE device which
 * Android reports as GATT connected at this moment; this does NOT establish
 * that Saphe Link is operational or receiving warnings.
 */
final class SapheIndicatorRules {
    enum State {
        ACTION_REQUIRED,          // Red: app/notification detection not ready.
        DETECTION_READY_UNKNOWN,  // Amber: BT identity or GATT observation unknown.
        DEVICE_NOT_OBSERVED,      // Red: previously selected device absent from GATT.
        DEVICE_OBSERVED_CONNECTED // Green: explicitly selected BLE device in GATT.
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
        if (!appInstalled || !notificationAccess || !optedIntoProbe)
            return State.ACTION_REQUIRED;
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
        if (!notificationAccess) return "FahrCockpit-Benachrichtigungszugriff fehlt; "
            + "Saphe-Geräteverbindung unbekannt";
        if (!optedIntoProbe) return "Saphe-Erkennung in FahrCockpit ausgeschaltet; "
            + "Saphe-Geräteverbindung unbekannt";
        if (evidence == Evidence.OBSERVED_CONNECTED)
            return "Gewähltes Saphe-Bluetooth-Gerät laut Android verbunden; "
                + "Saphe-Warnbereitschaft nicht überprüft";
        if (evidence == Evidence.NOT_OBSERVED)
            return "Gewähltes Saphe-Bluetooth-Gerät derzeit nicht in Androids "
                + "GATT-Verbindungsliste; keine sichere Aussage über Saphe Link";
        return "Saphe-Erkennung bereit; Bluetooth-Geräteverbindung nicht bestätigt";
    }
}
