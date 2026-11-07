package com.cobrachris.sanftwecker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Independent parallel countdowns. No direct vibrator or ringing-service reuse. */
public final class TimerActivity extends Activity {
    private LinearLayout cards;
    private final Map<Integer,TextView> clocks=new HashMap<>();
    private final Map<Integer,Integer> lastStates=new HashMap<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable ticker=new Runnable(){
        @Override public void run(){
            if(cards==null)return;
            long wall=System.currentTimeMillis(),elapsed=SystemClock.elapsedRealtime();
            for(TimerEntry t:TimerRepository.all(TimerActivity.this)){
                if(t.state==TimerEntry.RUNNING && t.remaining(wall,elapsed)==0){
                    TimerScheduler.fired(TimerActivity.this,t.id,t.dueWallMs);
                    render();break;
                }
                if(lastStates.containsKey(t.id)&&lastStates.get(t.id)!=t.state){
                    render();break;
                }
                TextView time=clocks.get(t.id);
                if(time!=null)time.setText(TimerDisplay.time(t.remaining(wall,elapsed),
                        TimerAlertSettings.timeFormat(TimerActivity.this,t.id)));
            }
            handler.postDelayed(this,250);
        }
    };

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        UiKit.window(this);
        TimerAlerts.ensureChannel(this);
        String importError=null;
        try{
            TimerRepository.importScreenshotPresets(this);
        }catch(RuntimeException e){
            importError=e.getMessage();
        }
        createScreen();
        if(importError!=null)message("Screenshot-Timer konnten nicht übernommen werden: "+importError);
    }
    @Override public void onResume(){
        super.onResume();render();
        handler.removeCallbacks(ticker);handler.post(ticker);
    }
    @Override public void onPause(){
        handler.removeCallbacks(ticker);super.onPause();
    }

    private void createScreen(){
        LinearLayout root=UiKit.mainColumn(this);
        root.addView(UiKit.label(this,"Multi-Timer",28,UiKit.TEXT));
        ClockTabs.add(this,root,ClockTabs.TIMERS);
        root.addView(UiKit.label(this,
                "Mehrere Timer gleichzeitig · alle Daten bleiben auf deinem Handy",
                14,UiKit.MUTED));
        LinearLayout quick=UiKit.row(this);
        Button a=UiKit.button(this,"+ 45 Sek.",false);
        Button b=UiKit.button(this,"+ 2 Min.",false);
        Button c=UiKit.button(this,"+ Timer",true);
        quick.addView(a,UiKit.weight());quick.addView(b,UiKit.weight());
        quick.addView(c,UiKit.weight());
        LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(-1,-2);
        qp.topMargin=UiKit.dp(this,18);qp.bottomMargin=UiKit.dp(this,12);
        root.addView(quick,qp);
        a.setOnClickListener(v->quick("Übungsdauer",45_000));
        b.setOnClickListener(v->quick("Pause / Dehnen",120_000));
        c.setOnClickListener(v->TimerEditor.show(this,null,this::render));
        Button stopAll=UiKit.button(this,"Alle laufenden Timer pausieren",false);
        root.addView(stopAll);
        stopAll.setOnClickListener(v->{
            for(TimerEntry t:TimerRepository.all(this))
                if(t.state==TimerEntry.RUNNING)TimerScheduler.pause(this,t.id);
            render();
        });
        cards=UiKit.column(this);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=UiKit.dp(this,16);
        root.addView(cards,p);
        root.addView(UiKit.label(this,
                "Fertige Timer melden sich mit einer einmaligen Android-Benachrichtigung. "+
                "Für Benachrichtigungen auf der Galaxy Watch muss diese App in "+
                "Galaxy Wearable freigegeben sein; Samsungs Ruhemodus kann sie unterdrücken.",
                13,UiKit.MUTED));
        setContentView(UiKit.scroll(this,root));
        render();
    }
    private void quick(String label,long millis){
        try{TimerRepository.create(this,label,millis);render();}
        catch(RuntimeException e){message(e.getMessage());}
    }
    private void render(){
        if(cards==null)return;
        cards.removeAllViews();clocks.clear();lastStates.clear();
        List<TimerEntry> timers=TimerRepository.all(this);
        if(timers.isEmpty()){
            LinearLayout empty=UiKit.card(this);
            empty.addView(UiKit.label(this,
                    "Noch keine Timer. Lege einen neuen an oder wähle einen der Schnellstarts.",
                    16,UiKit.TEXT));
            UiKit.placeCard(this,cards,empty);return;
        }
        for(int i=0;i<timers.size();i+=2){
            LinearLayout row=UiKit.row(this);
            row.setGravity(Gravity.TOP);
            row.addView(card(timers.get(i)),weighted());
            if(i+1<timers.size())row.addView(card(timers.get(i+1)),weighted());
            else row.addView(new View(this),weighted());
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);
            rp.bottomMargin=UiKit.dp(this,10);
            cards.addView(row,rp);
        }
    }
    private LinearLayout.LayoutParams weighted(){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);
        p.leftMargin=UiKit.dp(this,4);p.rightMargin=UiKit.dp(this,4);
        return p;
    }
    private LinearLayout card(TimerEntry t){
        lastStates.put(t.id,t.state);
        LinearLayout card=UiKit.card(this);
        card.setPadding(UiKit.dp(this,10),UiKit.dp(this,10),
                UiKit.dp(this,10),UiKit.dp(this,12));
        LinearLayout top=UiKit.row(this);
        TextView star=UiKit.action(this,t.favorite?"★":"☆");
        star.setContentDescription(t.favorite?"Favorit entfernen":"Als Favorit markieren");
        top.addView(star,UiKit.weight());
        star.setOnClickListener(v->{
            TimerRepository.put(this,t.favorite(!t.favorite));render();
        });
        TextView overflow=UiKit.action(this,"⋮");
        top.addView(overflow);
        overflow.setOnClickListener(v->menu(overflow,t));
        card.addView(top);
        TextView title=UiKit.label(this,t.name,16,UiKit.TEXT);
        title.setGravity(Gravity.CENTER);title.setMaxLines(2);
        title.setMinHeight(UiKit.dp(this,48));
        card.addView(title);
        long remaining=t.remaining(System.currentTimeMillis(),SystemClock.elapsedRealtime());
        TextView value=UiKit.label(this,TimerDisplay.time(remaining,
                        TimerAlertSettings.timeFormat(this,t.id)),23,
                t.state==TimerEntry.FINISHED?0xffffb199:UiKit.BLUE);
        value.setGravity(Gravity.CENTER);
        value.setSingleLine(true);
        card.addView(value);
        clocks.put(t.id,value);
        String status=t.state==TimerEntry.RUNNING?"Läuft":
                t.state==TimerEntry.PAUSED?"Pausiert":
                t.state==TimerEntry.FINISHED?"FERTIG":"Bereit";
        TextView state=UiKit.label(this,status,13,UiKit.MUTED);
        state.setGravity(Gravity.CENTER);card.addView(state);
        LinearLayout actions=UiKit.row(this);
        Button reset=UiKit.button(this,"↺",false);
        reset.setContentDescription("Timer zurücksetzen");
        reset.setMinWidth(UiKit.dp(this,45));
        actions.addView(reset,new LinearLayout.LayoutParams(0,UiKit.dp(this,54),1));
        Button start=UiKit.button(this,t.state==TimerEntry.RUNNING?"Ⅱ":
                t.state==TimerEntry.FINISHED?"Neu":"▶",true);
        start.setContentDescription(t.state==TimerEntry.RUNNING?
                "Timer pausieren":t.state==TimerEntry.FINISHED?"Timer neu starten":"Timer starten");
        actions.addView(start,new LinearLayout.LayoutParams(0,UiKit.dp(this,54),1));
        card.addView(actions);
        reset.setOnClickListener(v->{
            TimerScheduler.reset(this,t.id);render();
        });
        start.setOnClickListener(v->toggle(t));
        card.setOnClickListener(v->openDetail(t.id));
        return card;
    }
    private void openDetail(int id){
        startActivity(new Intent(this,TimerDetailActivity.class)
                .putExtra(TimerScheduler.EXTRA_ID,id));
    }
    private void toggle(TimerEntry t){
        try{
            if(t.state==TimerEntry.RUNNING)TimerScheduler.pause(this,t.id);
            else{
                if(!canAlert())return;
                if(t.state==TimerEntry.FINISHED)TimerScheduler.reset(this,t.id);
                TimerScheduler.start(this,t.id);
            }
            render();
        }catch(SecurityException e){allowExactAlarm();}
        catch(RuntimeException e){message("Timer konnte nicht gestartet werden: "+e.getMessage());}
    }
    private boolean canAlert(){
        if(Build.VERSION.SDK_INT>=33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                !=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},93);
            return false;
        }
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(nm==null||!nm.areNotificationsEnabled()){
            message("Aktiviere zuerst die Benachrichtigungen für Sanftwecker in Android.");
            return false;
        }
        if(!AlarmScheduler.canSchedule(this)){allowExactAlarm();return false;}
        return true;
    }
    private void allowExactAlarm(){
        new AlertDialog.Builder(this).setTitle("Genaue Timer")
                .setMessage("Für zuverlässige Timer ist der Android-Zugriff "+
                        "„Alarme und Erinnerungen“ erforderlich.")
                .setNegativeButton("Abbrechen",null)
                .setPositiveButton("Einstellungen",(d,w)->{
                    try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(Uri.parse("package:"+getPackageName())));}
                    catch(RuntimeException ex){message("Bitte Alarme manuell erlauben.");}
                }).show();
    }
    private void menu(View view,TimerEntry t){
        PopupMenu m=new PopupMenu(this,view);
        m.getMenu().add("Timer-Details").setOnMenuItemClickListener(i->{
            openDetail(t.id);return true;
        });
        m.getMenu().add("Duplizieren").setOnMenuItemClickListener(i->{
            quick(t.name+" Kopie",t.durationMs);return true;
        });
        m.getMenu().add("Löschen").setOnMenuItemClickListener(i->{
            new AlertDialog.Builder(this).setTitle("Timer löschen?")
                    .setMessage(t.name)
                    .setNegativeButton("Abbrechen",null)
                    .setPositiveButton("Löschen",(d,w)->{
                        TimerScheduler.delete(this,t.id);render();
                    }).show();return true;
        });
        m.show();
    }
    private void message(String text){
        new AlertDialog.Builder(this).setMessage(text)
                .setPositiveButton("OK",null).show();
    }
}
