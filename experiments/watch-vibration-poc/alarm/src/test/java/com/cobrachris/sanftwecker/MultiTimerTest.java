package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class MultiTimerTest {
    private static final long WALL=1_800_000_000_000L;
    private static final long UPTIME=200_000L;

    @Test public void independentTimersKeepSeparateDeadlines(){
        TimerEntry shortTimer=TimerEntry.create(1,"Training",45_000).start(WALL,UPTIME);
        TimerEntry longTimer=TimerEntry.create(2,"Pause",120_000).start(WALL,UPTIME);
        assertEquals(40_000,shortTimer.remaining(WALL+5000,UPTIME+5000));
        assertEquals(115_000,longTimer.remaining(WALL+5000,UPTIME+5000));
        assertEquals(0,shortTimer.remaining(WALL+50000,UPTIME+50000));
        assertEquals(70_000,longTimer.remaining(WALL+50000,UPTIME+50000));
    }
    @Test public void pauseAndResumeDoNotResetProgress(){
        TimerEntry first=TimerEntry.create(3,"Plank",60_000).start(WALL,UPTIME);
        TimerEntry paused=first.pause(WALL+12_345,UPTIME+12_345);
        assertEquals(TimerEntry.PAUSED,paused.state);
        assertEquals(47_655,paused.remaining(WALL+80000,UPTIME+80000));
        TimerEntry running=paused.start(WALL+80000,UPTIME+80000);
        assertEquals(47_655,running.remaining(WALL+80000,UPTIME+80000));
        assertEquals(12_655,running.remaining(WALL+115000,UPTIME+115000));
    }
    @Test public void resetAfterCompletionRestoresDuration(){
        TimerEntry finished=TimerEntry.create(4,"Egg",25_000)
                .start(WALL,UPTIME).finish();
        assertEquals(0,finished.remaining(WALL+100000,UPTIME+100000));
        TimerEntry reset=finished.reset();
        assertEquals(TimerEntry.READY,reset.state);
        assertEquals(25_000,reset.remaining(WALL+100000,UPTIME+100000));
    }
    @Test public void wallClockCorrectionDoesNotChangeRunningCountdown(){
        TimerEntry timer=TimerEntry.create(5,"Clock",60_000).start(WALL,UPTIME);
        // The wall offset exceeds tolerance, so the persisted RTC deadline is fallback.
        assertEquals(0,timer.remaining(WALL+70_000,UPTIME+10_000));
    }
    @Test public void renamingResetsOnlyEditedTimer(){
        TimerEntry first=TimerEntry.create(7,"Old",30000);
        TimerEntry second=TimerEntry.create(8,"Other",90000);
        assertEquals(45000,first.edit("New",45000).durationMs);
        assertEquals(90000,second.durationMs);
        assertEquals(8,second.id);
    }
    @Test public void formattingNeverTruncatesRemainingSecond(){
        assertEquals("00:00:01",ClockFormat.countdown(1));
        assertEquals("00:00:00",ClockFormat.countdown(0));
        assertEquals("01:01:01",ClockFormat.countdown(3_661_000));
        assertEquals("00:00:46",ClockFormat.countdown(45_001));
    }
    @Test(expected=IllegalArgumentException.class)
    public void cannotCreateZeroDuration(){TimerEntry.create(1,"Invalid",0);}
    @Test(expected=IllegalArgumentException.class)
    public void cannotCreateOverOneDay(){TimerEntry.create(1,"Invalid",86_401_000L);}
}
