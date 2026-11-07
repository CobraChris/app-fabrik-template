package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class TimerAlertSettingsTest {
    @Test public void allFourSelectionsUseStableDistinctAndroidChannels(){
        assertEquals("sanftwecker_timer_done_v1",
                TimerAlertSettings.channelId(true,true));
        assertEquals("sanftwecker_timer_tone_only_v1",
                TimerAlertSettings.channelId(true,false));
        assertEquals("sanftwecker_timer_vibration_only_v1",
                TimerAlertSettings.channelId(false,true));
        assertEquals("sanftwecker_timer_silent_v1",
                TimerAlertSettings.channelId(false,false));
    }
    @Test public void noOptionsReuseAnotherChannel(){
        java.util.Set<String> ids=new java.util.HashSet<>();
        for(boolean tone:new boolean[]{true,false})
            for(boolean vibration:new boolean[]{true,false})
                assertTrue(ids.add(TimerAlertSettings.channelId(tone,vibration)));
        assertEquals(4,ids.size());
    }
}
