package com.cobrachris.fahrcockpit;

import android.view.WindowManager;
import org.junit.Test;
import static org.junit.Assert.*;

/** Tests both the IME Z-layer contract and unchanged touch/focus semantics. */
public final class OverlayKeyboardRulesTest {
    @Test public void keyboardIsInFrontOfNonFocusableOverlay() {
        int flags=OverlayKeyboardRules.nonFocusableBehindKeyboardFlags();
        assertTrue((flags & WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0);
        assertTrue((flags & WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM) != 0);
    }

    @Test public void mapsAndKeyboardKeepTouchFocusOutsidePane() {
        int flags=OverlayKeyboardRules.nonFocusableBehindKeyboardFlags();
        assertTrue((flags & WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL) != 0);
        assertEquals(0, flags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }
}
