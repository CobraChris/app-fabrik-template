package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

public final class NavigationVoicePreferencesTest {
    @Test public void voiceToggleHasDistinctUnambiguousLabels() {
        assertEquals("🔇 Ansagen aus", NavigationVoicePreferences.buttonLabel(true));
        assertEquals("🔊 Ansagen an", NavigationVoicePreferences.buttonLabel(false));
    }
}
