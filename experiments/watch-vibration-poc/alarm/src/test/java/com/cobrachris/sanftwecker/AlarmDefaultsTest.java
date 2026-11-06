package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

/** New alarm defaults must stay gentle and be independent of user-edited profiles. */
public final class AlarmDefaultsTest {
    @Test public void newAlarmStartsWithWatchFirstAndAudiblePeak() {
        for (int id : new int[]{1,2,3,10,25}) {
            AlarmProfile p=AlarmProfile.defaults(id);
            assertFalse(p.enabled);
            assertEquals(10,p.delaySeconds);
            assertEquals(60,p.rampSeconds);
            assertEquals(55,p.maxVolume);
            assertEquals(6,p.watchPulses);
            assertTrue(p.rampEnabled);
        }
    }

    @Test public void ordinaryAlarmRemainsSilentForTheFirstTenSeconds() {
        AlarmProfile p=AlarmProfile.defaults(5);
        assertEquals(0f,AlarmEnvelope.gain(p,false,0),0f);
        assertEquals(0f,AlarmEnvelope.gain(p,false,10000),0f);
        assertTrue(AlarmEnvelope.gain(p,false,11000)>0f);
        assertEquals(.55f,AlarmEnvelope.gain(p,false,70000),.00001f);
    }

    @Test public void duplicationPreservesCustomSettings() {
        AlarmProfile custom=new AlarmProfile(9,"Mein Wecker",true,8,17,127,
                70,300,35,6,20,5,0);
        AlarmProfile copy=custom.duplicateAs(11);
        assertEquals(70,copy.delaySeconds);
        assertEquals(300,copy.rampSeconds);
        assertEquals(35,copy.maxVolume);
        assertFalse(copy.enabled);
    }
}
