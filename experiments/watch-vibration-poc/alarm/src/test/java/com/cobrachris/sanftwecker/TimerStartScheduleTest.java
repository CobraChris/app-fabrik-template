package com.cobrachris.sanftwecker;

import org.junit.Test;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.Assert.*;

public final class TimerStartScheduleTest {
    private static final TimeZone TOKYO=TimeZone.getTimeZone("Asia/Tokyo");
    private static long local(int year,int month,int day,int hour,int minute){
        Calendar calendar=Calendar.getInstance(TOKYO);
        calendar.clear();
        calendar.set(year,month-1,day,hour,minute,0);
        return calendar.getTimeInMillis();
    }
    @Test public void schedulesLaterSameDayExactly(){
        long now=local(2026,11,7,15,11);
        assertEquals(local(2026,11,7,17,30),
                TimerStartSchedule.nextLocalTime(17,30,now,TOKYO));
    }
    @Test public void schedulesTomorrowWhenTimeAlreadyPassed(){
        long now=local(2026,11,7,15,11);
        assertEquals(local(2026,11,8,6,45),
                TimerStartSchedule.nextLocalTime(6,45,now,TOKYO));
    }
    @Test public void schedulingAtCurrentMinuteIsNextDay(){
        long now=local(2026,11,7,15,11)+10_000L;
        assertEquals(local(2026,11,8,15,11),
                TimerStartSchedule.nextLocalTime(15,11,now,TOKYO));
    }
    @Test public void yearBoundaryAndTimezoneAreHandled(){
        long now=local(2026,12,31,23,59);
        assertEquals(local(2027,1,1,0,0),
                TimerStartSchedule.nextLocalTime(0,0,now,TOKYO));
    }
    @Test public void onlyExactStoredTimeCanStart(){
        long at=1_800_000_000_000L;
        assertTrue(TimerStartSchedule.isOnTime(at,at,at));
        assertTrue(TimerStartSchedule.isOnTime(at,at,at+5000L));
        assertFalse(TimerStartSchedule.isOnTime(at,at+60_000L,at));
        assertFalse(TimerStartSchedule.isOnTime(0,at,at));
        assertFalse(TimerStartSchedule.isOnTime(at,at,at-5000L));
        assertFalse(TimerStartSchedule.isOnTime(at,at,at+300_001L));
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsInvalidHour(){
        TimerStartSchedule.nextLocalTime(24,0,local(2026,11,7,15,11),TOKYO);
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsInvalidMinute(){
        TimerStartSchedule.nextLocalTime(12,60,local(2026,11,7,15,11),TOKYO);
    }
}
