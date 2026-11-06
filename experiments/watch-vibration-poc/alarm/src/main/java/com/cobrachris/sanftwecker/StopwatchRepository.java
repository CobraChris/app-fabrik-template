package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Local named stopwatch sessions with cumulative lap history. */
public final class StopwatchRepository {
    private static final String PREF="sanftwecker_stopwatches_v1";
    private static final String ITEMS="sessions";
    private static final String NEXT="next_id";
    private static final int LIMIT=50;
    private StopwatchRepository(){}
    private static SharedPreferences prefs(Context c){
        return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
    }
    public static synchronized List<StopwatchEntry> all(Context c){
        List<StopwatchEntry> out=new ArrayList<>();
        try{
            JSONArray stored=new JSONArray(prefs(c).getString(ITEMS,"[]"));
            for(int i=0;i<stored.length();i++){
                try{
                    JSONObject j=stored.getJSONObject(i);
                    JSONArray raw=j.optJSONArray("laps");
                    List<Long> laps=new ArrayList<>();
                    if(raw!=null)for(int n=0;n<Math.min(300,raw.length());n++)
                        laps.add(raw.getLong(n));
                    out.add(new StopwatchEntry(j.getInt("id"),j.optString("name","Stoppuhr"),
                            j.optLong("accumulated",0),j.optLong("started",0),
                            j.optLong("bootOffset",0),j.optBoolean("running",false),
                            j.optBoolean("favorite",false),laps));
                }catch(Exception invalid){AlarmRepository.diagnostic(c,"Defekte Stoppuhr übersprungen");}
            }
        }catch(Exception error){AlarmRepository.diagnostic(c,"Stoppuhrdaten nicht lesbar");}
        Collections.sort(out,(a,b)->{
            if(a.favorite!=b.favorite)return a.favorite?-1:1;
            return Integer.compare(a.id,b.id);
        });
        return out;
    }
    public static synchronized StopwatchEntry get(Context c,int id){
        for(StopwatchEntry s:all(c))if(s.id==id)return s;
        return null;
    }
    private static void save(Context c,List<StopwatchEntry> items){
        JSONArray out=new JSONArray();
        for(StopwatchEntry s:items){
            try{
                JSONObject j=new JSONObject();
                j.put("id",s.id);
                j.put("name",s.name);
                j.put("accumulated",s.accumulatedMs);
                j.put("started",s.startedElapsedMs);
                j.put("bootOffset",s.bootOffsetMs);
                j.put("running",s.running);
                j.put("favorite",s.favorite);
                JSONArray laps=new JSONArray();
                for(Long lap:s.laps)laps.put(lap);
                j.put("laps",laps);
                out.put(j);
            }catch(Exception error){throw new IllegalStateException("Stoppuhr speichern",error);}
        }
        if(!prefs(c).edit().putString(ITEMS,out.toString()).commit())
            throw new IllegalStateException("Stoppuhr konnte nicht gespeichert werden");
    }
    public static synchronized StopwatchEntry create(Context c,String name){
        List<StopwatchEntry> all=all(c);
        if(all.size()>=LIMIT)throw new IllegalStateException("Maximal 50 Stoppuhren");
        int id=prefs(c).getInt(NEXT,1);
        if(id>9999)throw new IllegalStateException("Maximale Stoppuhr-ID erreicht");
        StopwatchEntry item=StopwatchEntry.create(id,name);
        all.add(item);
        save(c,all);
        prefs(c).edit().putInt(NEXT,id+1).commit();
        return item;
    }
    public static synchronized void put(Context c,StopwatchEntry newEntry){
        List<StopwatchEntry> all=all(c);
        boolean found=false;
        for(int i=0;i<all.size();i++)if(all.get(i).id==newEntry.id){
            all.set(i,newEntry);found=true;break;
        }
        if(!found)throw new IllegalStateException("Stoppuhr fehlt");
        save(c,all);
    }
    public static synchronized void delete(Context c,int id){
        List<StopwatchEntry> all=all(c);
        all.removeIf(s->s.id==id);
        save(c,all);
    }
    public static synchronized void pauseAllAfterBoot(Context c){
        // Uptime resets during a reboot; a running session cannot be measured safely.
        for(StopwatchEntry s:all(c))if(s.running) {
            put(c,new StopwatchEntry(s.id,s.name,s.accumulatedMs,0,0,
                    false,s.favorite,s.laps));
        }
    }
    public static synchronized void reconcile(Context c){
        long wall=System.currentTimeMillis(),now=SystemClock.elapsedRealtime();
        for(StopwatchEntry s:all(c))if(s.running && !s.sameBoot(wall,now)){
            put(c,new StopwatchEntry(s.id,s.name,s.accumulatedMs,0,0,
                    false,s.favorite,s.laps));
            AlarmRepository.diagnostic(c,"Stoppuhr nach Systemzeit-/Neustart angehalten");
        }
    }
}
