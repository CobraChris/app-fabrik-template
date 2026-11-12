package com.cobrachris.fahrcockpit;

/**
 * Pure state rules for the user-selected cockpit presentation.
 * FULL retains all controls, DRIVING favors the map, COLLAPSED shows the title.
 */
final class CockpitViewRules {
    enum Mode { FULL, DRIVING, COLLAPSED }

    static final String DISPLAY_PREFS = "fahrcockpit_display_v1";
    static final String KEY_OPEN_MAPS = "open_maps_when_started";
    static final String KEY_AUTO_DRIVING = "auto_landscape_driving";

    private CockpitViewRules() {}

    static boolean shouldAutoDrive(boolean landscape, boolean enabled, Mode mode) {
        return landscape && enabled && mode == Mode.FULL;
    }

    static Mode driveToggle(Mode current) {
        return current == Mode.DRIVING ? Mode.FULL : Mode.DRIVING;
    }

    static String driveButtonLabel(Mode current) {
        return current == Mode.DRIVING ? "Voll" : "Fahrt";
    }
}
