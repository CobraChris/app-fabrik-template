package com.cobrachris.sanftwecker;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;

/** Regression tests for the exact ten screenshot timers and deduplication. */
public final class TimerPresetsTest {
    @Test public void screenshotHasTenDifferentTimersAndFourFavorites(){
        List<TimerPresets.Preset> presets=TimerPresets.SCREENSHOT_2026_11_07;
        assertEquals(10,presets.size());
        Set<String> keys=new HashSet<>();
        int favorites=0;
        for(TimerPresets.Preset p:presets){
            assertTrue(keys.add(p.name+"#"+p.durationMs));
            assertTrue(p.durationMs>=TimerEntry.MIN_MS);
            assertTrue(p.durationMs<=TimerEntry.MAX_MS);
            if(p.favorite)favorites++;
        }
        assertEquals(4,favorites);
    }
    @Test public void namesDurationsAndFavoritesMatchScreenshots(){
        String[] expectedNames={
            "Pause / Dehnen /24cm","Übungsdauer","Timer 8","Timer 6",
            "Rekord","Timer 7","Grill","Hartgekochtes Ei",
            "Weichgekochtes Ei","Erinnerungs-Countdown"
        };
        long[] expectedMillis={
            120_000L,45_000L,60_000L,90_000L,
            25_000L,300_000L,210_000L,690_000L,
            360_000L,3_000_000L
        };
        for(int i=0;i<10;i++){
            TimerPresets.Preset p=TimerPresets.SCREENSHOT_2026_11_07.get(i);
            assertEquals(expectedNames[i],p.name);
            assertEquals(expectedMillis[i],p.durationMs);
            assertEquals(i<4,p.favorite);
        }
    }
    @Test public void alreadyExistingTimerIsNotDuplicatedOrOverwritten(){
        TimerEntry existing=TimerEntry.create(4,"Grill",210_000L).start(
                1_800_000_000_000L,200_000L);
        List<TimerPresets.Preset> missing=TimerPresets.missing(
                Collections.singletonList(existing));
        assertEquals(9,missing.size());
        assertEquals(TimerEntry.RUNNING,existing.state);
        assertEquals(4,existing.id);
        assertEquals("Grill",existing.name);
        assertFalse(missing.stream().anyMatch(p->p.name.equals("Grill")));
    }
    @Test public void noDuplicatedImportIfAllEntriesArePresent(){
        List<TimerEntry> entries=new java.util.ArrayList<>();
        int id=1;
        for(TimerPresets.Preset p:TimerPresets.SCREENSHOT_2026_11_07)
            entries.add(TimerEntry.create(id++,p.name,p.durationMs));
        assertTrue(TimerPresets.missing(entries).isEmpty());
    }
    @Test public void sameNameWithDifferentDurationDoesNotReplaceUserTimer(){
        List<TimerPresets.Preset> missing=TimerPresets.missing(
            Arrays.asList(TimerEntry.create(1,"Übungsdauer",60_000L)));
        assertEquals(10,missing.size());
    }
}
