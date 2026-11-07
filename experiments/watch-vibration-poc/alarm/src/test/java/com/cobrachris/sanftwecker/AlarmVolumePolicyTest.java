package com.cobrachris.sanftwecker;
import org.junit.Test;
import static org.junit.Assert.*;

public final class AlarmVolumePolicyTest {
    @Test public void temporaryOverrideHasFiniteAudibleTarget(){
        assertEquals(1,AlarmVolumePolicy.temporaryLevel(1));
        assertEquals(8,AlarmVolumePolicy.temporaryLevel(10));
        assertEquals(12,AlarmVolumePolicy.temporaryLevel(15));
    }
    @Test public void restoreOnlyIfUserHasNotChangedVolume(){
        assertTrue(AlarmVolumePolicy.shouldRestore(0,12,12));
        assertFalse(AlarmVolumePolicy.shouldRestore(0,10,12));
        assertFalse(AlarmVolumePolicy.shouldRestore(5,12,12));
        assertFalse(AlarmVolumePolicy.shouldRestore(0,0,12));
    }
    @Test(expected=IllegalArgumentException.class)
    public void invalidAlarmVolumeRangeRejected(){
        AlarmVolumePolicy.temporaryLevel(0);
    }
}
