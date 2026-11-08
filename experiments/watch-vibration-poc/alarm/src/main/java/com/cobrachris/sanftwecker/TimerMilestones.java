package com.cobrachris.sanftwecker;

/** Pure milestone-time calculations; scheduling and notification are separate. */
public final class TimerMilestones {
    private TimerMilestones(){}
    public static long startWall(TimerEntry timer){
        return timer.dueWallMs-timer.remainingMs;
    }
    /** Returns the first interval after now, strictly before completion. */
    public static long nextIntervalWall(TimerEntry timer,int intervalSeconds,long nowWall){
        if(intervalSeconds<=0||timer==null ||
                (timer.state!=TimerEntry.RUNNING&&timer.state!=TimerEntry.WAITING))
            return -1;
        long length=intervalSeconds*1000L;
        long start=startWall(timer);
        long elapsed=Math.max(0L,nowWall-start);
        long index=elapsed/length+1L;
        long next=start+index*length;
        return next<timer.dueWallMs?next:-1;
    }
    /** Exactly three seconds before a delayed countdown begins. */
    public static long preparationWall(TimerEntry timer,long nowWall){
        if(timer==null||timer.state!=TimerEntry.WAITING)return -1;
        long event=startWall(timer)-3000L;
        return event>nowWall?event:-1L;
    }
}