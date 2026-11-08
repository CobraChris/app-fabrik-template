package com.cobrachris.sanftwecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Multi-session stopwatch based on monotonic elapsed time, not wall-clock time. */
public final class StopwatchEntry {
    public final int id;
    public final String name;
    public final long accumulatedMs, startedElapsedMs, bootOffsetMs;
    public final boolean running, favorite;
    public final List<Long> laps;

    public StopwatchEntry(int id,String name,long accumulatedMs,long startedElapsedMs,
            long bootOffsetMs,boolean running,boolean favorite,List<Long> laps){
        if(id<1||id>9999||accumulatedMs<0||startedElapsedMs<0||
                laps==null||laps.size()>300)
            throw new IllegalArgumentException("Invalid stopwatch");
        this.id=id;
        this.name=name==null||name.trim().isEmpty()?"Stoppuhr "+id:name.trim();
        this.accumulatedMs=accumulatedMs;
        this.startedElapsedMs=startedElapsedMs;
        this.bootOffsetMs=bootOffsetMs;
        this.running=running;
        this.favorite=favorite;
        this.laps=Collections.unmodifiableList(new ArrayList<>(laps));
    }
    public static StopwatchEntry create(int id,String name){
        return new StopwatchEntry(id,name,0,0,0,false,false,new ArrayList<>());
    }
    public boolean sameBoot(long wallNow,long elapsedNow){
        return running && elapsedNow>=startedElapsedMs &&
                Math.abs((wallNow-elapsedNow)-bootOffsetMs)<60_000;
    }
    public long elapsed(long wallNow,long elapsedNow){
        if(!sameBoot(wallNow,elapsedNow))return accumulatedMs;
        long more=Math.max(0,elapsedNow-startedElapsedMs);
        return accumulatedMs>=Long.MAX_VALUE-more?Long.MAX_VALUE:accumulatedMs+more;
    }
    public StopwatchEntry start(long wallNow,long elapsedNow){
        if(running)return this;
        return new StopwatchEntry(id,name,accumulatedMs,elapsedNow,
                wallNow-elapsedNow,true,favorite,laps);
    }
    public StopwatchEntry pause(long wallNow,long elapsedNow){
        if(!running)return this;
        return new StopwatchEntry(id,name,elapsed(wallNow,elapsedNow),0,0,
                false,favorite,laps);
    }
    public StopwatchEntry reset(){
        return create(id,name).favorite(favorite);
    }
    public StopwatchEntry lap(long wallNow,long elapsedNow){
        if(!running||laps.size()>=300)return this;
        List<Long> next=new ArrayList<>(laps);
        next.add(elapsed(wallNow,elapsedNow)); // cumulative split
        return new StopwatchEntry(id,name,accumulatedMs,startedElapsedMs,
                bootOffsetMs,running,favorite,next);
    }
    public StopwatchEntry rename(String title){
        return new StopwatchEntry(id,title,accumulatedMs,startedElapsedMs,
                bootOffsetMs,running,favorite,laps);
    }
    public StopwatchEntry favorite(boolean value){
        return new StopwatchEntry(id,name,accumulatedMs,startedElapsedMs,
                bootOffsetMs,running,value,laps);
    }
}
