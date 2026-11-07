package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class MultiStopwatchTest {
    private static final long WALL=1_800_000_000_000L;
    private static final long UP=25_000L;

    @Test public void multipleWatchesMeasureIndependently(){
        StopwatchEntry a=StopwatchEntry.create(1,"Sprint").start(WALL,UP);
        StopwatchEntry b=StopwatchEntry.create(2,"Dehnen").start(WALL+5000,UP+5000);
        assertEquals(15_000,a.elapsed(WALL+15000,UP+15000));
        assertEquals(10_000,b.elapsed(WALL+15000,UP+15000));
    }
    @Test public void lapsRetainCumulativeAndSplitTimes(){
        StopwatchEntry t=StopwatchEntry.create(3,"Laps").start(WALL,UP)
                .lap(WALL+1500,UP+1500).lap(WALL+4300,UP+4300);
        assertEquals(2,t.laps.size());
        assertEquals(Long.valueOf(1500L),t.laps.get(0));
        assertEquals(Long.valueOf(4300L),t.laps.get(1));
        assertEquals("00:00:02.800",ClockFormat.split(t.laps.get(1),t.laps.get(0)));
    }
    @Test public void pauseAndResumeKeepAccumulatedDuration(){
        StopwatchEntry t=StopwatchEntry.create(4,"Stop")
                .start(WALL,UP).pause(WALL+12000,UP+12000);
        assertEquals(12000,t.elapsed(WALL+99999,UP+99999));
        t=t.start(WALL+99000,UP+99000);
        assertEquals(17000,t.elapsed(WALL+104000,UP+104000));
    }
    @Test public void monotonicClockSurvivesWallTimeAdjustment(){
        StopwatchEntry t=StopwatchEntry.create(5,"Safe").start(WALL,UP);
        assertEquals(5_000,t.elapsed(WALL+5000,UP+5000));
        // Reboot or major wall-clock jump: freeze, never jump to a huge value.
        assertEquals(0,t.elapsed(WALL+3600000,UP+5000));
        assertEquals(0,t.elapsed(WALL+5000,UP-1));
    }
    @Test public void resetClearsLapsAndRestoresPaused(){
        StopwatchEntry t=StopwatchEntry.create(6,"Sport").start(WALL,UP)
                .lap(WALL+5000,UP+5000).reset();
        assertFalse(t.running);
        assertTrue(t.laps.isEmpty());
        assertEquals("00:00:00.000",ClockFormat.stopwatch(t.accumulatedMs));
    }
    @Test public void favoritesAndRenamesKeepStoredLaps(){
        StopwatchEntry t=StopwatchEntry.create(7,"A").start(WALL,UP)
                .lap(WALL+1000,UP+1000).favorite(true).rename("B");
        assertTrue(t.favorite);
        assertEquals("B",t.name);
        assertEquals(1,t.laps.size());
    }
}
