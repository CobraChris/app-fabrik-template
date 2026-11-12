package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

public final class TrackProgressRulesTest {
    @Test public void progressesAndSeeksByTrackDuration() {
        assertEquals(500, TrackProgressRules.sliderPosition(90_000L, 180_000L));
        assertEquals(90_000L, TrackProgressRules.targetPosition(500, 180_000L));
        assertEquals(0, TrackProgressRules.sliderPosition(-1L, 180_000L));
    }

    @Test public void clampsTargets() {
        assertEquals(0, TrackProgressRules.sliderPosition(-500L, 2000L));
        assertEquals(1000, TrackProgressRules.sliderPosition(5000L, 2000L));
        assertEquals(0L, TrackProgressRules.targetPosition(-5, 60_000L));
        assertEquals(60_000L, TrackProgressRules.targetPosition(1200, 60_000L));
        assertEquals(-1L, TrackProgressRules.targetPosition(500, -1L));
    }

    @Test public void formatsElapsedAndDuration() {
        assertEquals("2:41", TrackProgressRules.time(161999L));
        assertEquals("8:01", TrackProgressRules.time(481000L));
        assertEquals("1:01:09", TrackProgressRules.time(3669000L));
        assertEquals("--:--", TrackProgressRules.time(-1L));
    }

    @Test public void handlesVeryLongMediaSafely() {
        assertEquals(1000, TrackProgressRules.sliderPosition(Long.MAX_VALUE, Long.MAX_VALUE));
        assertTrue(TrackProgressRules.targetPosition(500, Long.MAX_VALUE) > 0L);
        assertTrue(TrackProgressRules.targetPosition(500, Long.MAX_VALUE) < Long.MAX_VALUE);
    }
}
