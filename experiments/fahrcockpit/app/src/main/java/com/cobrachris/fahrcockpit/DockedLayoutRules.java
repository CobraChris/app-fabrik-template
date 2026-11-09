package com.cobrachris.fahrcockpit;
/** Screen-space proportions. This does NOT resize Google Maps itself. */
final class DockedLayoutRules {
    static final int COVER_PERCENT = 30;
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
    static int x(int screen, int panel, boolean landscape) {
        return landscape ? Math.max(0,screen-panel) : 0;
    }
    static int y(int screen, int panel, boolean landscape) {
        return landscape ? 0 : Math.max(0,screen-panel);
    }
}
