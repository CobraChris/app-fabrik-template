package com.cobrachris.fahrcockpit;
/** Screen-space proportions. This does NOT resize Google Maps itself. */
final class DockedLayoutRules {
    static final int COVER_PERCENT = 30;
    static final String PREFS = "fahrcockpit_dock_display_v1";
    static final String KEY_LANDSCAPE_RIGHT = "landscape_right";
    private DockedLayoutRules() {}
    static boolean landscape(int w, int h) { return w > h; }
    private static int fraction(int size, int percent) {
        return Math.max(1, Math.round(Math.max(1,size) * percent/100f));
    }
    static int width(int w, boolean landscape) {
        return landscape ? fraction(w, COVER_PERCENT) : Math.max(1,w);
    }
    static int height(int h, boolean landscape) {
        return landscape ? fraction(h,96) : fraction(h,COVER_PERCENT);
    }
    /** User-selected landscape side, default RIGHT to keep Maps' bottom tabs visible. */
    static int x(int screen, int panel, boolean landscape, boolean right) {
        return landscape && right ? Math.max(0,screen-panel) : 0;
    }
    /** The minimised bar follows the selected side; portrait is unchanged. */
    static int collapsedX(int screen, int panel, boolean landscape, boolean right) {
        return landscape ? x(screen,panel,true,right) : Math.max(0,screen-panel);
    }
    static int y(int screen, int panel, boolean landscape) {
        return landscape ? 0 : Math.max(0,screen-panel);
    }
}
