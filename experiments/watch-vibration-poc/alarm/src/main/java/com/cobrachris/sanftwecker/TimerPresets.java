package com.cobrachris.sanftwecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** One-time, local-only timer presets transcribed from the user's screenshots. */
public final class TimerPresets {
    private TimerPresets(){}

    public static final class Preset {
        public final String name;
        public final long durationMs;
        public final boolean favorite;
        public Preset(String name,long durationMs,boolean favorite){
            this.name=name;this.durationMs=durationMs;this.favorite=favorite;
        }
    }

    public static final List<Preset> SCREENSHOT_2026_11_07 =
            Collections.unmodifiableList(Arrays.asList(
                new Preset("Pause / Dehnen /24cm",120_000L,true),
                new Preset("Übungsdauer",45_000L,true),
                new Preset("Timer 8",60_000L,true),
                new Preset("Timer 6",90_000L,true),
                new Preset("Rekord",25_000L,false),
                new Preset("Timer 7",300_000L,false),
                new Preset("Grill",210_000L,false),
                new Preset("Hartgekochtes Ei",690_000L,false),
                new Preset("Weichgekochtes Ei",360_000L,false),
                new Preset("Erinnerungs-Countdown",3_000_000L,false)
            ));

    /** Exact name and duration matches are never duplicated or overwritten. */
    public static List<Preset> missing(List<TimerEntry> existing){
        List<Preset> missing=new ArrayList<>();
        for(Preset candidate:SCREENSHOT_2026_11_07){
            boolean found=false;
            for(TimerEntry old:existing){
                if(candidate.name.equals(old.name) && candidate.durationMs==old.durationMs){
                    found=true;break;
                }
            }
            if(!found)missing.add(candidate);
        }
        return missing;
    }
}
