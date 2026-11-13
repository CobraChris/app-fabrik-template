package com.cobrachris.fahrcockpit;

/**
 * Permanent 70/30 split in landscape.
 * In portrait, reserve adequate height for Spotify and controls.
 * These weights affect only the native same-Activity layout.
 */
final class CockpitLayoutRules {
    static final int PORTRAIT_MAP_WEIGHT = 9;
    static final int PORTRAIT_CONTROLS_WEIGHT = 11;
    private CockpitLayoutRules() {}

    static int mapWeight(boolean landscape) {
        return landscape ? IntegratedNavigationRules.MAP_WEIGHT : PORTRAIT_MAP_WEIGHT;
    }

    static int controlsWeight(boolean landscape) {
        return landscape ? IntegratedNavigationRules.CONTROLS_WEIGHT
                         : PORTRAIT_CONTROLS_WEIGHT;
    }

    // While the keyboard is on screen, give the destination input the full
    // usable viewport. Restore the permanent 70/30 split after dismissal.
    static boolean shouldShowMap(boolean keyboardVisible) {
        return !keyboardVisible;
    }

    static int safeBottomInset(int systemBarBottom, int keyboardBottom) {
        return Math.max(systemBarBottom, keyboardBottom);
    }
}
