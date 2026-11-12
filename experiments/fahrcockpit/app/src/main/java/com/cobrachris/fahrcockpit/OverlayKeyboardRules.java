package com.cobrachris.fahrcockpit;

import android.view.WindowManager;

/**
 * An overlay with FLAG_NOT_FOCUSABLE alone is placed above a third-party IME
 * and can obscure the keys (reproduced on Samsung / Google Maps).
 *
 * Android's documented NOT_FOCUSABLE + ALT_FOCUSABLE_IM combination orders
 * this non-editable overlay BEHIND the system IME while preserving focus in
 * Google Maps. NOT_TOUCH_MODAL keeps taps outside the overlay in Maps.
 *
 * See: https://developer.android.com/develop/ui/views/touch-and-input/keyboard-input/visibility
 */
final class OverlayKeyboardRules {
    private OverlayKeyboardRules() {}

    static int nonFocusableBehindKeyboardFlags() {
        return WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
    }
}
