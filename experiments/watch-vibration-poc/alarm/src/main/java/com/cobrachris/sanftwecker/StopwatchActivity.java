package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Multiple independent stopwatches with cumulative lap history and local persistence. */
public final class StopwatchActivity extends Activity {
    private LinearLayout cards;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Map<Integer,TextView> faces=new HashMap<>();
    private final Map<Integer,TextView> details=new HashMap<>();
    private final Runnable tick=new Runnable(){
        @Override public void run(){
            long wall=System.currentTimeMillis(),now=SystemClock.elapsedRealtime();
            for(StopwatchEntry s:StopwatchRepository.all(StopwatchActivity.this)){
                TextView face=faces.get(s.id);
                if(face!=null)face.setText(ClockFormat.stopwatch(s.elapsed(wall,now)));
                TextView detail=details.get(s.id);
                if(detail!=null&&s.running)detail.setText("Läuft · "+s.laps.size()+" Runden");
            }
            handler.postDelayed(this,80);
        }
    };
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        UiKit.window(this);
        createScreen();
    }
    @Override public void onResume(){
        super.onResume();
        StopwatchRepository.reconcile(this);
        render();
        handler.removeCallbacks(tick);handler.post(tick);
    }
    @Override public void onPause(){
        handler.removeCallbacks(tick);super.onPause();
    }
    private void createScreen(){
        LinearLayout root=UiKit.mainColumn(this);
        root.addView(UiKit.label(this,"Meine Stoppuhren",28,UiKit.TEXT));
        ClockTabs.add(this,root,ClockTabs.STOPWATCH);
        root.addView(UiKit.label(this,
                "Mehrere unabhängige Messungen · Zwischenzeiten und Runden",
                14,UiKit.MUTED));
        LinearLayout toolbar=UiKit.row(this);
        Button add=UiKit.button(this,"+ Stoppuhr",true);
        toolbar.addView(add,UiKit.weight());
        Button pauseAll=UiKit.button(this,"Alle pausieren",false);
        toolbar.addView(pauseAll,UiKit.weight());
        LinearLayout.LayoutParams tb=new LinearLayout.LayoutParams(-1,-2);
        tb.topMargin=UiKit.dp(this,18);tb.bottomMargin=UiKit.dp(this,16);
        root.addView(toolbar,tb);
        add.setOnClickListener(v->nameEditor(null));
        pauseAll.setOnClickListener(v->{
            long wall=System.currentTimeMillis(),now=SystemClock.elapsedRealtime();
            for(StopwatchEntry s:StopwatchRepository.all(this))
                if(s.running)StopwatchRepository.put(this,s.pause(wall,now));
            render();
        });
        cards=UiKit.column(this);
        root.addView(cards);
        root.addView(UiKit.label(this,
                "Die Stoppuhren laufen auch weiter, wenn du eine andere App öffnest. "+
                "Nach einem Handy-Neustart werden laufende Messungen aus "+
                "Sicherheitsgründen angehalten. Es werden keine Daten versendet.",
                13,UiKit.MUTED));
        setContentView(UiKit.scroll(this,root));
        render();
    }
    private LinearLayout.LayoutParams weight(){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);
        p.leftMargin=UiKit.dp(this,4);p.rightMargin=UiKit.dp(this,4);
        return p;
    }
    private void render(){
        if(cards==null)return;
        cards.removeAllViews();faces.clear();details.clear();
        List<StopwatchEntry> all=StopwatchRepository.all(this);
        if(all.isEmpty()){
            LinearLayout empty=UiKit.card(this);
            empty.addView(UiKit.label(this,
                    "Noch keine Stoppuhren. Tippe auf „+ Stoppuhr“, um eine Messung anzulegen.",
                    16,UiKit.TEXT));
            UiKit.placeCard(this,cards,empty);return;
        }
        for(int i=0;i<all.size();i+=2){
            LinearLayout row=UiKit.row(this);row.setGravity(Gravity.TOP);
            row.addView(card(all.get(i)),weight());
            if(i+1<all.size())row.addView(card(all.get(i+1)),weight());
            else row.addView(new View(this),weight());
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);
            rp.bottomMargin=UiKit.dp(this,10);
            cards.addView(row,rp);
        }
    }
    private LinearLayout card(StopwatchEntry s){
        LinearLayout card=UiKit.card(this);
        card.setPadding(UiKit.dp(this,9),UiKit.dp(this,10),
                UiKit.dp(this,9),UiKit.dp(this,12));
        LinearLayout header=UiKit.row(this);
        TextView favorite=UiKit.action(this,s.favorite?"★":"☆");
        favorite.setContentDescription(s.favorite?"Favorit entfernen":"Als Favorit markieren");
        header.addView(favorite,UiKit.weight());
        favorite.setOnClickListener(v->{
            StopwatchRepository.put(this,s.favorite(!s.favorite));render();
        });
        TextView more=UiKit.action(this,"⋮");
        header.addView(more);
        more.setOnClickListener(v->more(more,s));
        card.addView(header);

        TextView name=UiKit.label(this,s.name,16,UiKit.TEXT);
        name.setGravity(Gravity.CENTER);name.setMaxLines(2);
        name.setMinHeight(UiKit.dp(this,45));
        card.addView(name);
        TextView display=UiKit.label(this,
                ClockFormat.stopwatch(s.elapsed(System.currentTimeMillis(),
                        SystemClock.elapsedRealtime())),18,UiKit.BLUE);
        display.setGravity(Gravity.CENTER);display.setSingleLine(true);
        faces.put(s.id,display);
        card.addView(display);

        TextView status=UiKit.label(this,
                (s.running?"Läuft":"Pausiert")+" · "+s.laps.size()+" Runden",
                12,UiKit.MUTED);
        details.put(s.id,status);
        status.setGravity(Gravity.CENTER);
        card.addView(status);

        LinearLayout buttons=UiKit.row(this);
        Button reset=UiKit.button(this,"↺",false);
        reset.setContentDescription("Stoppuhr zurücksetzen");
        buttons.addView(reset,new LinearLayout.LayoutParams(0,UiKit.dp(this,54),1));
        Button play=UiKit.button(this,s.running?"Ⅱ":"▶",true);
        play.setContentDescription(s.running?"Stoppuhr pausieren":"Stoppuhr starten");
        buttons.addView(play,new LinearLayout.LayoutParams(0,UiKit.dp(this,54),1));
        card.addView(buttons);
        Button lap=UiKit.button(this,
                s.running?"Runde speichern":"Runden ansehen",false);
        card.addView(lap);

        reset.setOnClickListener(v->{
            if(!s.running){StopwatchRepository.put(this,s.reset());render();}
            else new AlertDialog.Builder(this).setTitle("Laufende Stoppuhr zurücksetzen?")
                    .setNegativeButton("Abbrechen",null)
                    .setPositiveButton("Zurücksetzen",(d,w)->{
                        StopwatchRepository.put(this,s.reset());render();
                    }).show();
        });
        play.setOnClickListener(v->{
            long wall=System.currentTimeMillis(),now=SystemClock.elapsedRealtime();
            StopwatchRepository.put(this,s.running?s.pause(wall,now):s.start(wall,now));
            render();
        });
        lap.setOnClickListener(v->{
            if(s.running){
                long wall=System.currentTimeMillis(),now=SystemClock.elapsedRealtime();
                StopwatchRepository.put(this,s.lap(wall,now));
                render();
            }else showLaps(s);
        });
        display.setOnClickListener(v->showLaps(
                StopwatchRepository.get(this,s.id)));
        return card;
    }
    private void showLaps(StopwatchEntry s){
        if(s==null)return;
        StringBuilder text=new StringBuilder();
        long previous=0;
        for(int i=0;i<s.laps.size();i++){
            long total=s.laps.get(i);
            text.append(i+1).append(".  ")
                    .append(ClockFormat.split(total,previous))
                    .append("    Σ ").append(ClockFormat.stopwatch(total)).append('\n');
            previous=total;
        }
        new AlertDialog.Builder(this).setTitle("Runden – "+s.name)
                .setMessage(text.length()==0?"Noch keine Rundenzeiten":text.toString())
                .setPositiveButton("Schließen",null).show();
    }
    private void nameEditor(StopwatchEntry existing){
        EditText input=new EditText(this);
        input.setSingleLine(true);
        input.setTextColor(UiKit.TEXT);
        input.setText(existing==null?"Neue Stoppuhr":existing.name);
        input.setSelection(input.length());
        new AlertDialog.Builder(this).setTitle(
                existing==null?"Stoppuhr anlegen":"Stoppuhr umbenennen")
                .setView(input)
                .setNegativeButton("Abbrechen",null)
                .setPositiveButton("Speichern",(d,w)->{
                    String name=input.getText().toString().trim();
                    if(name.isEmpty())name="Stoppuhr";
                    try{
                        if(existing==null)StopwatchRepository.create(this,name);
                        else StopwatchRepository.put(this,existing.rename(name));
                        render();
                    }catch(RuntimeException e){
                        new AlertDialog.Builder(this).setMessage(e.getMessage())
                                .setPositiveButton("OK",null).show();
                    }
                }).show();
    }
    private void more(View anchor,StopwatchEntry s){
        PopupMenu menu=new PopupMenu(this,anchor);
        menu.getMenu().add("Umbenennen").setOnMenuItemClickListener(i->{
            nameEditor(s);return true;
        });
        menu.getMenu().add("Runden anzeigen").setOnMenuItemClickListener(i->{
            showLaps(s);return true;
        });
        menu.getMenu().add("Löschen").setOnMenuItemClickListener(i->{
            new AlertDialog.Builder(this).setTitle("Stoppuhr löschen?")
                    .setMessage(s.name+" und ihre Rundenzeiten löschen?")
                    .setNegativeButton("Abbrechen",null)
                    .setPositiveButton("Löschen",(d,w)->{
                        StopwatchRepository.delete(this,s.id);render();
                    }).show();
            return true;
        });
        menu.show();
    }
}
