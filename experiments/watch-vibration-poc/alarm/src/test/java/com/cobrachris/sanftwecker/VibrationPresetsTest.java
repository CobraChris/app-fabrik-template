package com.cobrachris.sanftwecker;
import org.junit.Test;
import static org.junit.Assert.*;

public final class VibrationPresetsTest {
    @Test public void fiveSelectablePatternsExist() {
        assertEquals(5,VibrationPresets.LABELS.length);
        for(int i=0;i<5;i++){
            long[] rhythm=VibrationPresets.pattern(i);
            assertTrue(rhythm.length>=3);
            assertEquals(0,rhythm[0]);
            assertTrue(VibrationPresets.duration(i)>=1500);
            assertTrue(VibrationPresets.duration(i)<=10000);
        }
    }
    @Test public void tripleShortHasThreeDistinctPulses() {
        assertArrayEquals(new long[]{0,300,230,300,230,300,1300},
                VibrationPresets.pattern(2));
    }
    @Test public void longShortLongHasCorrectRelativePulses() {
        long[] t=VibrationPresets.pattern(1);
        assertEquals(t[1],t[5]);
        assertTrue(t[1]>t[3]);
    }
    @Test public void continuousIsBoundedDutyCycle() {
        assertArrayEquals(new long[]{0,4000,500},VibrationPresets.pattern(4));
    }
    @Test(expected=IllegalArgumentException.class)
    public void invalidPatternFailsSafely() {VibrationPresets.pattern(10);}
    @Test public void oldConstructorRetainsLegacyMode() {
        AlarmProfile old=new AlarmProfile(1,"Alt",false,7,0,127,10,60,55,6,20,5,0);
        assertEquals(0,old.vibrationMode);
        assertEquals(0,old.duplicateAs(2).vibrationMode);
    }
    @Test public void copiedProfilePreservesSelectedRhythm() {
        AlarmProfile p=AlarmProfile.defaults(1);
        assertEquals(1,p.vibrationMode);
        assertEquals(1,p.duplicateAs(2).vibrationMode);
        assertFalse(p.duplicateAs(2).enabled);
    }
}
