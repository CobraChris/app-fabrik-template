package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class TimerStartDelayTest {
    private static final long WALL=1_800_000_000_000L;
    private static final long ELAPSED=200_000L;
    @Test public void delayKeepsCountdownStillUntilScheduledStart(){
        TimerEntry delayed=TimerEntry.create(1,"Dehnen",120_000L)
                .startDelayed(WALL,ELAPSED,3000L);
        assertEquals(TimerEntry.WAITING,delayed.state);
        assertEquals(WALL+123_000L,delayed.dueWallMs);
        assertEquals(3000L,delayed.delayRemaining(WALL,ELAPSED));
        assertEquals(120_000L,delayed.remaining(WALL+2000L,ELAPSED+2000L));
        assertEquals(1000L,delayed.delayRemaining(WALL+2000L,ELAPSED+2000L));
        assertEquals(0L,delayed.delayRemaining(WALL+3000L,ELAPSED+3000L));
        assertEquals(120_000L,delayed.remaining(WALL+3000L,ELAPSED+3000L));
        assertEquals(115_000L,delayed.remaining(WALL+8000L,ELAPSED+8000L));
        assertEquals(0L,delayed.remaining(WALL+123_000L,ELAPSED+123_000L));
    }
    @Test public void pendingStartCanBePausedWithoutTriggeringAlert(){
        TimerEntry delayed=TimerEntry.create(2,"Pause",25_000L)
                .startDelayed(WALL,ELAPSED,10000L);
        TimerEntry paused=delayed.pause(WALL+1000L,ELAPSED+1000L);
        assertEquals(TimerEntry.PAUSED,paused.state);
        assertEquals(25_000L,paused.remainingMs);
        TimerEntry restarted=paused.startDelayed(WALL+20000,ELAPSED+20000,5000L);
        assertEquals(TimerEntry.WAITING,restarted.state);
        assertEquals(WALL+50_000L,restarted.dueWallMs);
    }
    @Test public void pendingCountdownCannotBeStartedTwice(){
        TimerEntry delayed=TimerEntry.create(3,"Training",45_000L)
                .startDelayed(WALL,ELAPSED,3000L);
        assertSame(delayed,delayed.start(WALL+100,ELAPSED+100));
        assertSame(delayed,delayed.startDelayed(WALL+100,ELAPSED+100,10000));
    }
    @Test public void noDelayPreservesOriginalRunningBehavior(){
        TimerEntry timer=TimerEntry.create(4,"Ei",6000L).startDelayed(WALL,ELAPSED,0);
        assertEquals(TimerEntry.RUNNING,timer.state);
        assertEquals(0L,timer.delayRemaining(WALL,ELAPSED));
        assertEquals(WALL+6000L,timer.dueWallMs);
    }
    @Test public void afterRebootWallClockFallbackPreservesTimerDeadline(){
        TimerEntry timer=TimerEntry.create(5,"Grill",30_000L)
                .startDelayed(WALL,ELAPSED,10_000L);
        assertEquals(4000L,timer.delayRemaining(WALL+6000L,1500L));
        assertEquals(30_000L,timer.remaining(WALL+6000L,1500L));
    }
    @Test(expected=IllegalArgumentException.class)
    public void negativeDelayRejected(){
        TimerEntry.create(1,"Invalid",1000).startDelayed(WALL,ELAPSED,-1);
    }
    @Test(expected=IllegalArgumentException.class)
    public void longDelayRejected(){
        TimerEntry.create(1,"Invalid",1000).startDelayed(WALL,ELAPSED,61_000L);
    }
}
