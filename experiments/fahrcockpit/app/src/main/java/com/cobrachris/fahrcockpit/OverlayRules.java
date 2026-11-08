package com.cobrachris.fahrcockpit;

/** Pure presentation and geometry rules for the user-initiated overlay. */
final class OverlayRules {
    private OverlayRules() {}

    /** Keep the complete panel on-screen when it fits, otherwise pin it to zero. */
    static int clampPosition(int position, int viewport, int panelExtent) {
        if (viewport <= 0 || panelExtent <= 0) return 0;
        int maximum = Math.max(0, viewport - panelExtent);
        return Math.max(0, Math.min(position, maximum));
    }

    static String primaryLabel(String warningSource) {
        if (WarningSource.SAPHE.equals(warningSource)) return "Saphe öffnen";
        if (WarningSource.BLITZER.equals(warningSource)) return "Blitzer Start";
        return "Keine Auswahl";
    }

    static boolean primaryEnabled(String warningSource) {
        return WarningSource.SAPHE.equals(warningSource)
            || WarningSource.BLITZER.equals(warningSource);
    }

    static boolean showBlitzerStop(String warningSource) {
        return WarningSource.BLITZER.equals(warningSource);
    }
}
