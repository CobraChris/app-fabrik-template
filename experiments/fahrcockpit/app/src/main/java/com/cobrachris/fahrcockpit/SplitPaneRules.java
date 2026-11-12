package com.cobrachris.fahrcockpit;

/** Native multi-window presentation: Android owns placement and ratio. */
final class SplitPaneRules {
    static final int DESIRED_MAP_PERCENT = 70;
    static final int DESIRED_COCKPIT_PERCENT = 30;

    private SplitPaneRules() {}

    /** Split is exclusive: no system overlay should obscure native controls. */
    static boolean mustRemoveOverlay(boolean inMultiWindow) {
        return inMultiWindow;
    }

    static String statusText(boolean multiWindow) {
        return multiWindow
            ? "Split aktiv · 70/30 über Trennlinie einstellen; als App-Paar speichern"
            : "Split nicht bestätigt · App-Paar-Hilfe öffnen, kein Overlay-Autostart";
    }
}
