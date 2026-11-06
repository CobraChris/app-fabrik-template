package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class VibrationSafetyTest {
    @Test public void previewAlwaysEndsWithinFiveSeconds() {
        for (int i=0;i<VibrationPresets.LABELS.length;i++)
            assertTrue(VibrationSafety.isFinite(VibrationSafety.previewDuration(i)));
    }
    @Test public void normalAlarmHasShortFiniteSafetyLimit() {
        assertEquals(12_000, VibrationSafety.MAX_ALARM_MS);
        assertEquals(5_000, VibrationSafety.MAX_PREVIEW_MS);
    }
    @Test public void neverRequestInfiniteHardwareWaveform() {
        assertEquals(-1, VibrationSafety.NON_REPEATING);
    }
    @Test public void continuousPreviewStopsWithinFiveSeconds() {
        assertEquals(4_500, VibrationSafety.previewDuration(4));
    }
}
