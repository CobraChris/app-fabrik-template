package com.cobrachris.fahrcockpit;

/**
 * Pure state rules for the user-selected cockpit presentation.
 * FULL retains all controls, DRIVING favors the map, COLLAPSED shows the title.
 */
final class CockpitViewRules {
    enum Mode { FULL, DRIVING, COLLAPSED }

    private CockpitViewRules() {}

    static Mode driveToggle(Mode current) {
        return current == Mode.DRIVING ? Mode.FULL : Mode.DRIVING;
    }

    static String driveButtonLabel(Mode current) {
        return current == Mode.DRIVING ? "Voll" : "Fahrt";
    }
}
