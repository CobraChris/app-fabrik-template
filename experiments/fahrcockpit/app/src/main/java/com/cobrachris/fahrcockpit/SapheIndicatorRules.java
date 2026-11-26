package com.cobrachris.fahrcockpit;

/**
 * Do not interpret Android NotificationListener permission or a Saphe package
 * notification as proof of a Bluetooth hardware connection. Such proof is not
 * available via the current public, integrated Saphe contract.
 */
final class SapheIndicatorRules {
    enum State {
        ACTION_REQUIRED,          // Red: local detection/launch is not ready.
        DETECTION_READY_UNKNOWN    // Amber: notification detection ready, device unknown.
    }

    private SapheIndicatorRules() {}

    static State state(boolean appInstalled, boolean notificationAccess,
                       boolean optedIntoProbe) {
        return appInstalled && notificationAccess && optedIntoProbe
            ? State.DETECTION_READY_UNKNOWN : State.ACTION_REQUIRED;
    }

    static String description(boolean appInstalled, boolean notificationAccess,
                              boolean optedIntoProbe) {
        if (!appInstalled) return "Saphe-App nicht installiert; Verbindung unbekannt";
        if (!notificationAccess) return "FahrCockpit-Benachrichtigungszugriff fehlt; "
            + "Saphe-Geräteverbindung unbekannt";
        if (!optedIntoProbe) return "Saphe-Erkennung in FahrCockpit ausgeschaltet; "
            + "Saphe-Geräteverbindung unbekannt";
        return "Saphe-Erkennung bereit; Bluetooth-Geräteverbindung nicht bestätigt";
    }
}
