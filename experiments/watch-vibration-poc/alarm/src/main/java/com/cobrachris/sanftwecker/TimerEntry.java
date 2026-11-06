package com.cobrachris.sanftwecker;

/** Pure immutable timer state, suitable for independent concurrent countdowns. */
public final class TimerEntry {
    public static final int READY=0, RUNNING=1, PAUSED=2, FINISHED=3;
    public static final long MIN_MS=1000L, MAX_MS=24L*60*60*1000;
    public final int id, state;
    public final String name;
    public final long durationMs, remainingMs, dueWallMs, dueElapsedMs, bootOffsetMs;
    public final boolean favorite;

    public TimerEntry(int id, String name, long durationMs, long remainingMs,
            int state, long dueWallMs, long dueElapsedMs,
            long bootOffsetMs, boolean favorite) {
        if(id<1 || id>9999 || durationMs<MIN_MS || durationMs>MAX_MS ||
                remainingMs<0 || remainingMs>durationMs ||
                state<READY || state>FINISHED)
            throw new IllegalArgumentException("Invalid timer");
        this.id=id;
        this.name=name==null||name.trim().isEmpty() ? "Timer "+id : name.trim();
        this.durationMs=durationMs;
        this.remainingMs=remainingMs;
        this.state=state;
        this.dueWallMs=dueWallMs;
        this.dueElapsedMs=dueElapsedMs;
        this.bootOffsetMs=bootOffsetMs;
        this.favorite=favorite;
    }
    public static TimerEntry create(int id,String name,long durationMs){
        return new TimerEntry(id,name,durationMs,durationMs,READY,0,0,0,false);
    }
    public long remaining(long wallNow,long elapsedNow){
        if(state!=RUNNING)return remainingMs;
        // Elapsed realtime does not jump if the system time is manually adjusted.
        // On reboot the offset changes; use the RTC deadline as a fallback.
        long offset=wallNow-elapsedNow;
        boolean sameBoot=dueElapsedMs>0 && Math.abs(offset-bootOffsetMs)<60000;
        long remaining=sameBoot?dueElapsedMs-elapsedNow:dueWallMs-wallNow;
        return Math.max(0,Math.min(durationMs,remaining));
    }
    public TimerEntry start(long wallNow,long elapsedNow){
        if(state==RUNNING || remainingMs==0)return this;
        return new TimerEntry(id,name,durationMs,remainingMs,RUNNING,
                wallNow+remainingMs,elapsedNow+remainingMs,wallNow-elapsedNow,favorite);
    }
    public TimerEntry pause(long wallNow,long elapsedNow){
        if(state!=RUNNING)return this;
        return new TimerEntry(id,name,durationMs,remaining(wallNow,elapsedNow),PAUSED,
                0,0,0,favorite);
    }
    public TimerEntry finish(){
        return new TimerEntry(id,name,durationMs,0,FINISHED,0,0,0,favorite);
    }
    public TimerEntry reset(){
        return new TimerEntry(id,name,durationMs,durationMs,READY,0,0,0,favorite);
    }
    public TimerEntry edit(String title,long duration){
        return new TimerEntry(id,title,duration,duration,READY,0,0,0,favorite);
    }
    public TimerEntry favorite(boolean value){
        return new TimerEntry(id,name,durationMs,remainingMs,state,
                dueWallMs,dueElapsedMs,bootOffsetMs,value);
    }
}
