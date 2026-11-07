package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class TimerDisplayTest {
    @Test public void showsHoursMinutesSecondsWithoutTruncation(){
        assertEquals("00:02:00",TimerDisplay.time(120_000L,TimerAlertSettings.FORMAT_HMS));
        assertEquals("01:01:01",TimerDisplay.time(3_661_000L,TimerAlertSettings.FORMAT_HMS));
        assertEquals("00:00:01",TimerDisplay.time(1L,TimerAlertSettings.FORMAT_HMS));
    }
    @Test public void minuteSecondDisplayHandlesLongTimers(){
        assertEquals("02:00",TimerDisplay.time(120_000L,TimerAlertSettings.FORMAT_MS));
        assertEquals("61:01",TimerDisplay.time(3_661_000L,TimerAlertSettings.FORMAT_MS));
        assertEquals("00:01",TimerDisplay.time(1L,TimerAlertSettings.FORMAT_MS));
        assertEquals("00:00",TimerDisplay.time(-1L,TimerAlertSettings.FORMAT_MS));
        assertEquals("1440:00",TimerDisplay.time(86_400_000L,TimerAlertSettings.FORMAT_MS));
    }
    @Test public void secondaryLabelOptionsAreDistinct(){
        assertEquals("Ziel-Uhrzeit",TimerDisplay.extraLabel(TimerAlertSettings.EXTRA_TARGET));
        assertEquals("Ursprüngliche Dauer",TimerDisplay.extraLabel(TimerAlertSettings.EXTRA_DURATION));
        assertEquals("Keine",TimerDisplay.extraLabel(TimerAlertSettings.EXTRA_NONE));
        assertEquals("Min.:Sek.",TimerDisplay.formatLabel(TimerAlertSettings.FORMAT_MS));
    }
    @Test public void autoRepeatHasMinimumIntervalToAvoidRapidNotificationLoops(){
        assertFalse(TimerAlertSettings.canRepeat(1000));
        assertFalse(TimerAlertSettings.canRepeat(9999));
        assertTrue(TimerAlertSettings.canRepeat(10_000));
        assertTrue(TimerAlertSettings.canRepeat(3_000_000));
        assertFalse(TimerAlertSettings.canRepeat(86_400_001));
    }
    @Test public void existingTimerStillKeepsItsDeadlineWhenPresentationChanges(){
        long wall=1_800_000_000_000L, elapsed=250_000L;
        TimerEntry running=TimerEntry.create(3,"Workout",120_000L).start(wall,elapsed);
        assertEquals("01:55",TimerDisplay.time(running.remaining(wall+5000,elapsed+5000),
                TimerAlertSettings.FORMAT_MS));
        assertEquals(120_000L,running.durationMs);
        assertEquals(wall+120_000L,running.dueWallMs);
        assertEquals(TimerEntry.RUNNING,running.state);
    }
}
