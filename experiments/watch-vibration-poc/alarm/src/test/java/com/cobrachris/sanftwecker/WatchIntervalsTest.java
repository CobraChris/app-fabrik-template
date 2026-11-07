package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class WatchIntervalsTest {
    @Test public void fasterWatchOptionsExist() {
        assertEquals(3, WatchIntervals.secondsAt(0));
        assertEquals(5, WatchIntervals.secondsAt(1));
        assertEquals(10, WatchIntervals.secondsAt(2));
        assertEquals(120,WatchIntervals.secondsAt(WatchIntervals.maxProgress()));
    }
    @Test public void previousSavedIntervalsRetainTheirExactMeaning(){
        for(int seconds=10;seconds<=120;seconds+=10)
            assertEquals(seconds,WatchIntervals.secondsAt(WatchIntervals.progressFor(seconds)));
    }
    @Test public void newIntervalsSurviveRoundtrip(){
        for(int seconds:new int[]{3,5,10,20,30,40,50,60,70,80,90,100,110,120})
            assertEquals(seconds,WatchIntervals.secondsAt(WatchIntervals.progressFor(seconds)));
    }
    @Test public void unsupportedIntervalsCannotBeAccidentallyStored(){
        for(int seconds:new int[]{-1,0,1,2,4,6,9,11,121})
            assertFalse(WatchIntervals.isSupported(seconds));
    }
    @Test(expected=IllegalArgumentException.class)
    public void invalidProgressRejected(){
        WatchIntervals.secondsAt(-1);
    }
}
