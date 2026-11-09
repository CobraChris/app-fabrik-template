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

    /** Only start fallback after a split launch was attempted and not accepted. */
    static boolean shouldStartFallback(boolean inMultiWindow,
                                       boolean adjacentLaunchAttempted,
                                       boolean alreadyRunning,
                                       boolean overlayPermission) {
        return !inMultiWindow && adjacentLaunchAttempted
            && !alreadyRunning && overlayPermission;
    }

    static String statusText(boolean multiWindow) {
        return multiWindow
            ? "Split-Ansicht aktiv · Trennlinie auf 70/30 einstellen"
            : "Split noch nicht bestätigt · Samsung Multi Window/App-Paar verwenden";
    }
}
