package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class VibrationSafetyTest {
    @Test public void previewAlwaysEndsWithinFiveSeconds() {
        for (int i=0;i<VibrationPresets.LABELS.length;i++)
            assertTrue(VibrationSafety.isFinite(VibrationSafety.previewDuration(i)));
    }
    @Test public void normalAlarmHasShortFiniteSafetyLimit() {
        assertEquals(90_000, VibrationSafety.MAX_ALARM_MS);
        assertEquals(5_000, VibrationSafety.MAX_PREVIEW_MS);
    }
    @Test public void neverRequestInfiniteHardwareWaveform() {
        assertEquals(-1, VibrationSafety.NON_REPEATING);
    }
    @Test public void maximumRequestedAmplitudeIsAppliedOnlyDuringOnPhases() {
        for(int mode=0; mode<VibrationPresets.LABELS.length; mode++){
            long[] pattern=VibrationPresets.pattern(mode);
            int[] amplitudes=VibrationSafety.fullStrengthAmplitudes(pattern);
            assertEquals(pattern.length,amplitudes.length);
            for(int i=0;i<amplitudes.length;i++)
                assertEquals(i%2==1?255:0,amplitudes[i]);
        }
    }
    @Test public void vibrationSafetyRejectsUnboundedHardwarePhases(){
        try{VibrationSafety.fullStrengthAmplitudes(new long[]{0,4_001,300});fail();}
        catch(IllegalArgumentException expected){}
        try{VibrationSafety.fullStrengthAmplitudes(new long[]{0,4_000,300,200});fail();}
        catch(IllegalArgumentException expected){}
    }
    @Test public void continuousPreviewStopsWithinFiveSeconds() {
        assertEquals(4_500, VibrationSafety.previewDuration(4));
    }
}
