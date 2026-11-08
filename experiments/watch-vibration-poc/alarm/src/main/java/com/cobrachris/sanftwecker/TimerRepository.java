package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Local-only durable timer definitions, independent from existing alarm profiles. */
public final class TimerRepository {
    private static final String PREF="sanftwecker_multi_timers_v1";
    private static final String DATA="items";
    private static final String NEXT="next_id";
    private static final int LIMIT=50;
    private TimerRepository(){}

    private static SharedPreferences prefs(Context c){
        return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
    }
    public static synchronized List<TimerEntry> all(Context c) {
        List<TimerEntry> out=new ArrayList<>();
        try{
            JSONArray items=new JSONArray(prefs(c).getString(DATA,"[]"));
            for(int i=0;i<items.length();i++){
                try{
                    JSONObject j=items.getJSONObject(i);
                    out.add(new TimerEntry(j.getInt("id"),j.optString("name","Timer"),
                            j.getLong("duration"),j.getLong("remaining"),
                            j.getInt("state"),j.optLong("dueWall",0),
                            j.optLong("dueElapsed",0),j.optLong("bootOffset",0),
                            j.optBoolean("favorite",false)));
                }catch(Exception invalid){
                    AlarmRepository.diagnostic(c,"Übersprungener beschädigter Timer-Eintrag");
                }
            }
        }catch(Exception error){
            AlarmRepository.diagnostic(c,"Timer-Speicher nicht lesbar");
        }
        Collections.sort(out,(a,b)->{
            if(a.favorite!=b.favorite)return a.favorite?-1:1;
            return Integer.compare(a.id,b.id);
        });
        return out;
    }
    public static synchronized TimerEntry get(Context c,int id){
        for(TimerEntry t:all(c))if(t.id==id)return t;
        return null;
    }
    private static JSONArray toJson(List<TimerEntry> timers) {
        JSONArray arr=new JSONArray();
        for(TimerEntry t:timers){
            JSONObject j=new JSONObject();
            try{
                j.put("id",t.id);j.put("name",t.name);
                j.put("duration",t.durationMs);j.put("remaining",t.remainingMs);
                j.put("state",t.state);j.put("dueWall",t.dueWallMs);
                j.put("dueElapsed",t.dueElapsedMs);j.put("bootOffset",t.bootOffsetMs);
                j.put("favorite",t.favorite);
                arr.put(j);
            }catch(Exception error){throw new IllegalStateException("Timer serialisieren",error);}
        }
        return arr;
    }
    private static void save(Context c,List<TimerEntry> timers) {
        if(!prefs(c).edit().putString(DATA,toJson(timers).toString()).commit())
            throw new IllegalStateException("Timer konnten nicht gespeichert werden");
    }

    /**
     * A one-time import of screenshot timer presets. Preserves existing items,
     * running countdowns, and IDs. Imported timers remain stopped. If the user
     * later deletes a preset it must NOT reappear when the app opens again.
     */
    public static synchronized int importScreenshotPresets(Context c){
        final String importedKey="screenshot_timers_20261107_imported";
        SharedPreferences storage=prefs(c);
        if(storage.getBoolean(importedKey,false))return 0;
        List<TimerEntry> items=all(c);
        List<TimerPresets.Preset> missing=TimerPresets.missing(items);
        if(items.size()+missing.size()>LIMIT)
            throw new IllegalStateException(
                "Nicht genug Plätze für die 10 Screenshot-Timer (maximal 50).");
        int next=storage.getInt(NEXT,1);
        for(TimerEntry item:items)next=Math.max(next,item.id+1);
        if(next+missing.size()-1>9999)
            throw new IllegalStateException("Timer-ID-Kontingent erschöpft");
        for(TimerPresets.Preset preset:missing){
            TimerEntry item=TimerEntry.create(next++,preset.name,preset.durationMs);
            items.add(preset.favorite?item.favorite(true):item);
        }
        if(!storage.edit().putString(DATA,toJson(items).toString())
                .putInt(NEXT,next).putBoolean(importedKey,true).commit())
            throw new IllegalStateException("Screenshot-Timer konnten nicht gespeichert werden");
        return missing.size();
    }
    public static synchronized TimerEntry create(Context c,String name,long durationMs){
        List<TimerEntry> timers=all(c);
        if(timers.size()>=LIMIT)throw new IllegalStateException("Maximal 50 Timer");
        int id=prefs(c).getInt(NEXT,1);
        // Never reuse IDs, so old PendingIntents cannot accidentally trigger a new timer.
        if(id>9999)throw new IllegalStateException("Timer-ID-Kontingent erschöpft");
        TimerEntry t=TimerEntry.create(id,name,durationMs);
        timers.add(t);
        save(c,timers);
        prefs(c).edit().putInt(NEXT,id+1).commit();
        return t;
    }
    public static synchronized void put(Context c,TimerEntry item){
        List<TimerEntry> entries=all(c);
        boolean replaced=false;
        for(int i=0;i<entries.size();i++)if(entries.get(i).id==item.id){
            entries.set(i,item);replaced=true;break;
        }
        if(!replaced)throw new IllegalStateException("Timer fehlt");
        save(c,entries);
    }
    public static synchronized void delete(Context c,int id){
        List<TimerEntry> entries=all(c);
        entries.removeIf(t->t.id==id);
        save(c,entries);
    }
}
