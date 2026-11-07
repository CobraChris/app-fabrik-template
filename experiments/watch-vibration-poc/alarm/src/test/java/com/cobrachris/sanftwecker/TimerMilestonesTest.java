package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class TimerMilestonesTest {
    private static final long WALL=1_800_000_000_000L;
    private static final long ELAPSED=200_000L;

    @Test public void intervalBeginsAfterCountdownStartAndNeverReplacesFinish(){
        TimerEntry running=TimerEntry.create(1,"Training",120_000L).start(WALL,ELAPSED);
        assertEquals(WALL,TimerMilestones.startWall(running));
        assertEquals(WALL+30_000L,TimerMilestones.nextIntervalWall(running,30,WALL));
        assertEquals(WALL+60_000L,TimerMilestones.nextIntervalWall(running,30,WALL+30_000L));
        assertEquals(WALL+90_000L,TimerMilestones.nextIntervalWall(running,30,WALL+63_000L));
        assertEquals(-1L,TimerMilestones.nextIntervalWall(running,30,WALL+90_000L));
        assertEquals(-1L,TimerMilestones.nextIntervalWall(running,0,WALL));
    }
    @Test public void delayedStartDoesNotProduceIntervalsDuringWaiting(){
        TimerEntry pending=TimerEntry.create(2,"Dehnen",90_000L)
                .startDelayed(WALL,ELAPSED,10_000L);
        assertEquals(WALL+10_000L,TimerMilestones.startWall(pending));
        assertEquals(WALL+40_000L,TimerMilestones.nextIntervalWall(pending,30,WALL));
        assertEquals(WALL+7_000L,TimerMilestones.preparationWall(pending,WALL));
        assertEquals(-1L,TimerMilestones.preparationWall(pending,WALL+7_000L));
    }
    @Test public void intervalOfFullDurationDoesNotRepeatAtCompletion(){
        TimerEntry timer=TimerEntry.create(3,"Rekord",30_000L).start(WALL,ELAPSED);
        assertEquals(-1L,TimerMilestones.nextIntervalWall(timer,30,WALL));
    }
    @Test public void stalePausedOrCompletedTimersHaveNoMilestones(){
        TimerEntry running=TimerEntry.create(4,"Ei",60_000L).start(WALL,ELAPSED);
        TimerEntry paused=running.pause(WALL+5_000L,ELAPSED+5_000L);
        assertEquals(-1L,TimerMilestones.nextIntervalWall(paused,30,WALL+5000));
        assertEquals(-1L,TimerMilestones.preparationWall(running,WALL));
        assertEquals(-1L,TimerMilestones.nextIntervalWall(running.finish(),30,WALL));
    }
    @Test public void nonzeroPauseResumeUsesNewCountdownStart(){
        TimerEntry a=TimerEntry.create(5,"Pause",90_000L).start(WALL,ELAPSED);
        TimerEntry b=a.pause(WALL+20_000L,ELAPSED+20_000L);
        TimerEntry resumed=b.start(WALL+60_000L,ELAPSED+60_000L);
        assertEquals(WALL+60_000L,TimerMilestones.startWall(resumed));
        assertEquals(WALL+90_000L,TimerMilestones.nextIntervalWall(resumed,30,WALL+60_000L));
    }
    @Test public void optionsRequireExplicitValidIntervals(){
        assertArrayEquals(new int[]{0,30,60,120,300},TimerAlertSettings.INTERVAL_CHOICES);
        assertTrue(TimerAlertSettings.validIntervalSeconds(0));
        assertTrue(TimerAlertSettings.validIntervalSeconds(30));
        assertFalse(TimerAlertSettings.validIntervalSeconds(1));
        assertFalse(TimerAlertSettings.validIntervalSeconds(15));
        assertFalse(TimerAlertSettings.validIntervalSeconds(301));
    }
}
