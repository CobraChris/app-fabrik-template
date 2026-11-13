package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.media.AudioAttributes;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

/**
 * German turn instructions through the phone's locally available Android TTS.
 * No cloud TTS API. If no German speech engine is present, fail silently
 * and show visual instruction instead; NEVER invent a maneuver.
 */
final class OsmSpeechGuide implements TextToSpeech.OnInitListener {
    private TextToSpeech tts;
    private boolean ready;
    private boolean closed;
    private int sequence;
    private final NavigationEngine.Status status;

    OsmSpeechGuide(Activity activity, NavigationEngine.Status status) {
        this.status=status;
        try {
            tts=new TextToSpeech(activity,this);
        } catch (RuntimeException unavailable) {
            status.onStatus("Android-Sprachausgabe fehlt – nur visuelle Hinweise.");
        }
    }

    @Override public void onInit(int result) {
        if (closed || tts==null) return;
        if (result!=TextToSpeech.SUCCESS) {
            status.onStatus("Android-Sprachausgabe nicht bereit – nur visuelle Hinweise.");
            return;
        }
        int language=tts.setLanguage(Locale.GERMANY);
        if (language==TextToSpeech.LANG_MISSING_DATA ||
                language==TextToSpeech.LANG_NOT_SUPPORTED) {
            ready=false;
            status.onStatus("Deutsche TTS-Stimme fehlt – nur visuelle Hinweise.");
            return;
        }
        tts.setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build());
        ready=true;
    }

    boolean ready() {return ready&&!closed;}

    void speak(String words) {
        if (!ready() || words==null || words.isEmpty()) return;
        tts.speak(words,TextToSpeech.QUEUE_FLUSH,null,
            "fahrcockpit-osm-turn-"+(++sequence));
    }

    void stop() {
        if(tts!=null)tts.stop();
    }

    void close() {
        closed=true;
        ready=false;
        if(tts!=null){
            tts.stop();
            tts.shutdown();
            tts=null;
        }
    }
}
