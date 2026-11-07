package com.cobrachris.sanftwecker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
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
import android.view.WindowInsets;
import android.graphics.Insets;
import android.widget.ScrollView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Locale;

/**
 * Dedicated timer detail screen. Mirrors the useful navigation pattern from
 * the screenshot without implementing nonfunctional 'coming soon' switches.
 * All changes remain local to the selected countdown.
 */
public final class TimerDetailActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable ticker=new Runnable(){
        @Override public void run(){
            updateState();
            handler.postDelayed(this,250);
        }
    };
    private int timerId;
    private TextView title,time,target,state,star,plannedStartLabel;
    private ProgressBar progress;
    private Button primary,reset;
    private Switch tone,vibration,repeat,prepareCue,oneShot;
    private TextView timeFormatChoice,extraInfoChoice,startDelayChoice,intervalChoice;
    private boolean updating;

    @Override public void onCreate(Bundle savedState){
        super.onCreate(savedState);
        timerId=getIntent().getIntExtra(TimerScheduler.EXTRA_ID,-1);
        if(TimerRepository.get(this,timerId)==null){finish();return;}
        UiKit.window(this);
        TimerAlerts.ensureChannel(this);
        build();
    }
    @Override public void onResume(){
        super.onResume();
        updateState();
        handler.removeCallbacks(ticker);
        handler.postDelayed(ticker,250);
    }
    @Override public void onPause(){
        handler.removeCallbacks(ticker);
        super.onPause();
    }
    @Override protected void onDestroy(){
        handler.removeCallbacks(ticker);
        super.onDestroy();
    }

    private LinearLayout section(LinearLayout root,String label){
        LinearLayout card=UiKit.card(this);
        card.addView(UiKit.heading(this,label));
        UiKit.placeCard(this,root,card);
        return card;
    }
    private void addHint(LinearLayout root,String message){
        TextView hint=UiKit.label(this,message,13,UiKit.MUTED);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=UiKit.dp(this,5);
        root.addView(hint,p);
    }
    private void build(){
        // A fixed header remains visible while scrolling. Android 15+ draws
        // edge-to-edge; explicit system bar insets prevent the clipped title
        // and bottom text seen on Samsung's Android 16.
        LinearLayout page=UiKit.column(this);
        page.setBackgroundColor(UiKit.BG);
        LinearLayout header=UiKit.row(this);
        header.setPadding(UiKit.dp(this,12),UiKit.dp(this,2),
                UiKit.dp(this,12),UiKit.dp(this,4));
        TextView back=UiKit.action(this,"‹");
        back.setContentDescription("Zur Timer-Liste zurück");
        back.setOnClickListener(v->finish());
        header.addView(back);
        header.addView(UiKit.heading(this,"Timer-Detail"),UiKit.weight());
        star=UiKit.action(this,"☆");
        header.addView(star);
        star.setOnClickListener(v->toggleFavorite());
        page.addView(header);
        LinearLayout root=UiKit.mainColumn(this);

        LinearLayout overview=section(root,"");
        // Empty card heading is removed; the large timer title is the heading.
        overview.removeViewAt(0);
        title=UiKit.label(this,"",23,UiKit.TEXT);
        title.setGravity(Gravity.CENTER);
        title.setMinHeight(UiKit.dp(this,52));
        title.setOnClickListener(v->editTimer());
        title.setContentDescription("Timername und Dauer bearbeiten");
        overview.addView(title);
        time=UiKit.label(this,"00:00:00",52,UiKit.BLUE);
        time.setGravity(Gravity.CENTER);
        time.setSingleLine(true);
        time.setAutoSizeTextTypeUniformWithConfiguration(34,56,1,
                android.util.TypedValue.COMPLEX_UNIT_SP);
        time.setOnClickListener(v->editTimer());
        time.setContentDescription("Timerdauer bearbeiten");
        overview.addView(time);
        target=UiKit.label(this,"",15,UiKit.MUTED);
        target.setGravity(Gravity.CENTER);
        overview.addView(target);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(UiKit.BLUE));
        LinearLayout.LayoutParams pg=new LinearLayout.LayoutParams(-1,UiKit.dp(this,6));
        pg.topMargin=UiKit.dp(this,18);
        overview.addView(progress,pg);

        LinearLayout controls=UiKit.row(this);
        reset=UiKit.button(this,"↻  Zurücksetzen",false);
        primary=UiKit.button(this,"▶  Starten",true);
        LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,UiKit.dp(this,64),1);
        a.rightMargin=UiKit.dp(this,6);
        controls.addView(reset,a);
        LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(0,UiKit.dp(this,64),1);
        b.leftMargin=UiKit.dp(this,6);
        controls.addView(primary,b);
        overview.addView(controls);
        reset.setOnClickListener(v->runAction(0));
        primary.setOnClickListener(v->runAction(1));
        state=UiKit.label(this,"",14,UiKit.MUTED);
        state.setGravity(Gravity.CENTER);
        overview.addView(state);
        Button edit=UiKit.button(this,"Name und Dauer bearbeiten",false);
        overview.addView(edit);
        edit.setOnClickListener(v->editTimer());

        LinearLayout schedule=section(root,"Timer-Zeitplan");
        plannedStartLabel=UiKit.label(this,"Kein Start geplant",16,UiKit.MUTED);
        schedule.addView(plannedStartLabel);
        Button chooseTime=UiKit.button(this,"Startzeit festlegen",true);
        Button cancelTime=UiKit.button(this,"Geplanten Start entfernen",false);
        LinearLayout scheduleActions=UiKit.column(this);
        scheduleActions.addView(chooseTime);
        scheduleActions.addView(cancelTime);
        schedule.addView(scheduleActions);
        chooseTime.setOnClickListener(v->chooseScheduledStart());
        cancelTime.setOnClickListener(v->{
            try{
                TimerStartSchedule.cancel(this,timerId);
                updateState();
            }catch(RuntimeException e){showError(e.getMessage());}
        });
        addHint(schedule,"Startet den Countdown einmalig zur nächsten gewählten "+
                "Uhrzeit (heute oder morgen). Die normale Startverzögerung gilt "+
                "zusätzlich. Manueller Start und Zurücksetzen heben den Zeitplan auf.");

        LinearLayout display=section(root,"Anzeige & Wiederholung");
        timeFormatChoice=choiceRow(display,"Zeitformat",TimerDisplay.formatLabel(
                TimerAlertSettings.timeFormat(this,timerId)),()->{
            final String[] values={"Std.:Min.:Sek.","Min.:Sek."};
            new AlertDialog.Builder(this).setTitle("Zeitformat")
                .setSingleChoiceItems(values,TimerAlertSettings.timeFormat(this,timerId),
                    (dialog,which)->{
                        dialog.dismiss();
                        try{TimerAlertSettings.setTimeFormat(this,timerId,which);updateState();}
                        catch(RuntimeException e){showError(e.getMessage());}
                    })
                .setNegativeButton("Abbrechen",null).show();
        });
        extraInfoChoice=choiceRow(display,"Zusatz-Info unter dem Timer",
                TimerDisplay.extraLabel(TimerAlertSettings.extraInfo(this,timerId)),()->{
            final String[] values={"Ziel-Uhrzeit","Ursprüngliche Dauer","Keine"};
            new AlertDialog.Builder(this).setTitle("Zusatz-Info")
                .setSingleChoiceItems(values,TimerAlertSettings.extraInfo(this,timerId),
                    (dialog,which)->{
                        dialog.dismiss();
                        try{TimerAlertSettings.setExtraInfo(this,timerId,which);updateState();}
                        catch(RuntimeException e){showError(e.getMessage());}
                    })
                .setNegativeButton("Abbrechen",null).show();
        });
        startDelayChoice=choiceRow(display,"Startverzögerung",
                delayLabel(TimerAlertSettings.startDelaySeconds(this,timerId)),()->{
            final int[] seconds={0,3,5,10,30,60};
            final String[] labels={"Aus","3 Sekunden","5 Sekunden","10 Sekunden",
                    "30 Sekunden","60 Sekunden"};
            int current=0;
            for(int i=0;i<seconds.length;i++){
                if(seconds[i]==TimerAlertSettings.startDelaySeconds(this,timerId))
                    current=i;
            }
            new AlertDialog.Builder(this).setTitle("Startverzögerung")
                .setSingleChoiceItems(labels,current,(dialog,which)->{
                    dialog.dismiss();
                    try{
                        TimerAlertSettings.setStartDelaySeconds(this,timerId,seconds[which]);
                        updateState();
                    }catch(RuntimeException e){showError(e.getMessage());}
                })
                .setNegativeButton("Abbrechen",null).show();
        });
        addHint(display,"Die Verzögerung gilt beim nächsten manuellen Start. "+
                "Ein laufender Timer wird dadurch nicht verändert.");
        intervalChoice=choiceRow(display,"Intervall-Timer",
                intervalLabel(TimerAlertSettings.intervalSeconds(this,timerId)),()->{
            final int[] seconds=TimerAlertSettings.INTERVAL_CHOICES;
            final String[] labels={"Aus","Alle 30 Sekunden","Jede Minute",
                    "Alle 2 Minuten","Alle 5 Minuten"};
            int current=0;
            for(int i=0;i<seconds.length;i++)
                if(seconds[i]==TimerAlertSettings.intervalSeconds(this,timerId))
                    current=i;
            new AlertDialog.Builder(this).setTitle("Intervall-Timer")
                .setSingleChoiceItems(labels,current,(dialog,which)->{
                    dialog.dismiss();
                    try{TimerAlertSettings.setIntervalSeconds(this,timerId,
                            seconds[which]);updateState();}
                    catch(RuntimeException ex){showError(ex.getMessage());}
                }).setNegativeButton("Abbrechen",null).show();
        });
        addHint(display,"Kurzer Hinweis nach jedem Intervall; nicht gleichzeitig "+
                "mit dem Abschlussalarm. Für längere Timer geeignet.");
        LinearLayout preparationRow=UiKit.row(this);
        preparationRow.addView(UiKit.label(this,"Vorbereitungsalarm",16,UiKit.TEXT),
                UiKit.weight());
        prepareCue=UiKit.toggle(this);
        preparationRow.addView(prepareCue);
        display.addView(preparationRow);
        addHint(display,"Drei Sekunden vor dem Countdown-Start. "+
                "Startverzögerung von mindestens fünf Sekunden nötig.");
        prepareCue.setOnCheckedChangeListener((view,enabled)->{
            if(updating)return;
            try{
                if(enabled&&TimerAlertSettings.startDelaySeconds(this,timerId)<5){
                    updating=true;prepareCue.setChecked(false);updating=false;
                    new AlertDialog.Builder(this)
                            .setTitle("Startverzögerung erforderlich")
                            .setMessage("Für den Vorbereitungsalarm sind mindestens "+
                                    "5 Sekunden Startverzögerung erforderlich.")
                            .setNegativeButton("Abbrechen",null)
                            .setPositiveButton("5 Sekunden einstellen",(d,w)->{
                                try{
                                    TimerAlertSettings.setStartDelaySeconds(this,timerId,5);
                                    TimerAlertSettings.setPrepareCue(this,timerId,true);
                                    updateState();
                                }catch(RuntimeException ex){showError(ex.getMessage());}
                            }).show();
                    return;
                }
                TimerAlertSettings.setPrepareCue(this,timerId,enabled);
            }catch(RuntimeException ex){showError(ex.getMessage());}
            updateState();
        });
        LinearLayout repeatRow=UiKit.row(this);
        repeatRow.addView(UiKit.label(this,"Automatische Wiederholung",16,UiKit.TEXT),
                UiKit.weight());
        repeat=UiKit.toggle(this);
        repeatRow.addView(repeat);
        display.addView(repeatRow);
        addHint(display,"Nach dem Ablauf startet derselbe Timer automatisch neu. "+
                "Nur ab 10 Sekunden; nicht für den Weckalarm.");
        repeat.setOnCheckedChangeListener((view,enabled)->{
            if(updating)return;
            try{TimerAlertSettings.setAutoRepeat(this,timerId,enabled);}
            catch(RuntimeException e){showError(e.getMessage());}
            updateState();
        });
        LinearLayout oneShotRow=UiKit.row(this);
        oneShotRow.addView(UiKit.label(this,"Einmal-Timer",16,UiKit.TEXT),
                UiKit.weight());
        oneShot=UiKit.toggle(this);
        oneShotRow.addView(oneShot);
        display.addView(oneShotRow);
        addHint(display,"Entfernt diesen Timer nach dem nächsten vollständigen "+
                "Ablauf automatisch aus der Liste. Nicht rückgängig zu machen.");
        oneShot.setOnCheckedChangeListener((view,enabled)->{
            if(updating)return;
            if(enabled){
                updating=true;oneShot.setChecked(false);updating=false;
                new AlertDialog.Builder(this).setTitle("Einmal-Timer aktivieren?")
                        .setMessage("Dieser Timer wird nach dem Ablauf automatisch "+
                                "gelöscht. Die anderen Timer bleiben erhalten.")
                        .setNegativeButton("Abbrechen",null)
                        .setPositiveButton("Aktivieren",(d,w)->{
                            try{TimerAlertSettings.setOneShot(this,timerId,true);}
                            catch(RuntimeException ex){showError(ex.getMessage());}
                            updateState();
                        }).show();
            }else{
                try{TimerAlertSettings.setOneShot(this,timerId,false);}
                catch(RuntimeException e){showError(e.getMessage());}
                updateState();
            }
        });

        LinearLayout alert=section(root,"Alarm beim Ablauf");
        LinearLayout toneRow=UiKit.row(this);
        toneRow.addView(UiKit.label(this,"Alarmton",17,UiKit.TEXT),UiKit.weight());
        tone=UiKit.toggle(this);
        toneRow.addView(tone);
        alert.addView(toneRow);
        LinearLayout vibRow=UiKit.row(this);
        vibRow.addView(UiKit.label(this,"Vibration",17,UiKit.TEXT),UiKit.weight());
        vibration=UiKit.toggle(this);
        vibRow.addView(vibration);
        alert.addView(vibRow);
        addHint(alert,"Gilt nur für diesen Timer. Android-Benachrichtigungskanäle "+
                "können Ton und Vibration zusätzlich begrenzen.");
        tone.setOnCheckedChangeListener((view,enabled)->{
            if(updating)return;
            try{TimerAlertSettings.setTone(this,timerId,enabled);}
            catch(RuntimeException e){showError(e.getMessage());updateState();}
        });
        vibration.setOnCheckedChangeListener((view,enabled)->{
            if(updating)return;
            try{TimerAlertSettings.setVibration(this,timerId,enabled);}
            catch(RuntimeException e){showError(e.getMessage());updateState();}
        });

        Button systemSettings=UiKit.button(this,"Android-Benachrichtigungen prüfen",false);
        alert.addView(systemSettings);
        systemSettings.setOnClickListener(v->{
            try{
                startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
            }catch(RuntimeException e){
                showError("Android-Benachrichtigungseinstellungen nicht verfügbar.");
            }
        });

        LinearLayout extra=section(root,"Verwaltung");
        Button duplicate=UiKit.button(this,"Timer duplizieren",false);
        extra.addView(duplicate);
        duplicate.setOnClickListener(v->duplicateTimer());
        Button delete=UiKit.button(this,"Timer löschen",false);
        extra.addView(delete);
        delete.setOnClickListener(v->deleteTimer());
        addHint(extra,"Sprachalarm, fester Timer-Zeitplan und individuelle "+
                "Alarmdauer werden separat ergänzt. Android kontrolliert weiterhin "+
                "die Lautstärke der Benachrichtigungen.");

        ScrollView content=UiKit.scroll(this,root);
        page.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        if(Build.VERSION.SDK_INT>=35){
            page.setOnApplyWindowInsetsListener((view,insets)->{
                Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(0,bars.top,0,bars.bottom);
                return insets;
            });
        }
        setContentView(page);
        if(Build.VERSION.SDK_INT>=35)page.requestApplyInsets();
        updateState();
    }
    private void chooseScheduledStart(){
        TimerEntry timer=TimerRepository.get(this,timerId);
        if(timer==null)return;
        if(timer.state==TimerEntry.RUNNING||timer.state==TimerEntry.WAITING){
            showError("Zum Planen bitte zuerst den laufenden Timer pausieren.");
            return;
        }
        if(Build.VERSION.SDK_INT>=33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        !=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},96);
            showError("Erlaube zuerst die Benachrichtigungen, damit das Timer-Ende meldet.");
            return;
        }
        NotificationManager manager=
                (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(manager==null||!manager.areNotificationsEnabled()){
            showError("Bitte zuerst Android-Benachrichtigungen aktivieren.");
            return;
        }
        if(!AlarmScheduler.canSchedule(this)){
            allowExactAlarm();
            return;
        }
        Calendar defaultTime=Calendar.getInstance();
        defaultTime.add(Calendar.MINUTE,10);
        new TimePickerDialog(this,(picker,hour,minute)->{
            try{
                long at=TimerStartSchedule.nextLocalTime(hour,minute,
                        System.currentTimeMillis(),TimeZone.getDefault());
                TimerStartSchedule.set(this,timerId,at);
                updateState();
            }catch(RuntimeException e){showError(e.getMessage());}
        },defaultTime.get(Calendar.HOUR_OF_DAY),
                defaultTime.get(Calendar.MINUTE),true).show();
    }
    private static String scheduleLabel(long at){
        if(at<=0)return "Kein Start geplant";
        return "Geplanter Start: "+new SimpleDateFormat(
                "EEE, dd.MM. · HH:mm",Locale.GERMANY).format(new Date(at));
    }
    private static String intervalLabel(int seconds){
        if(seconds==0)return "Aus";
        if(seconds==60)return "Jede Minute";
        if(seconds==120)return "Alle 2 Minuten";
        if(seconds==300)return "Alle 5 Minuten";
        return "Alle "+seconds+" Sekunden";
    }
    private static String delayLabel(int seconds){
        return seconds==0?"Aus":seconds+" Sekunden";
    }
    private TextView choiceRow(LinearLayout parent,String title,String subtitle,
            Runnable click){
        LinearLayout row=UiKit.column(this);
        row.setPadding(UiKit.dp(this,2),UiKit.dp(this,8),
                UiKit.dp(this,2),UiKit.dp(this,10));
        row.addView(UiKit.label(this,title,16,UiKit.TEXT));
        TextView sub=UiKit.label(this,subtitle+"  ›",14,UiKit.MUTED);
        row.addView(sub);
        row.setContentDescription(title+" – "+subtitle);
        row.setOnClickListener(v->click.run());
        parent.addView(row);
        return sub;
    }
    private void updateState(){
        if(isFinishing()||title==null)return;
        TimerEntry t=TimerRepository.get(this,timerId);
        if(t==null){finish();return;}
        long now=System.currentTimeMillis();
        long left=t.remaining(now,SystemClock.elapsedRealtime());
        if((t.state==TimerEntry.RUNNING || t.state==TimerEntry.WAITING) && left==0){
            TimerScheduler.fired(this,t.id,t.dueWallMs);
            t=TimerRepository.get(this,timerId);
            if(t==null)return;
            left=t.remaining(System.currentTimeMillis(),SystemClock.elapsedRealtime());
        }
        title.setText(t.name);
        plannedStartLabel.setText(scheduleLabel(TimerStartSchedule.get(this,timerId)));
        time.setText(TimerDisplay.time(left,TimerAlertSettings.timeFormat(this,timerId)));
        star.setText(t.favorite?"★":"☆");
        star.setContentDescription(t.favorite?"Favorit entfernen":"Als Favorit markieren");
        int completed=(int)Math.max(0,Math.min(1000,
                Math.round(1000.0*(t.durationMs-left)/t.durationMs)));
        progress.setProgress(completed);
        if(t.state==TimerEntry.RUNNING || t.state==TimerEntry.WAITING){
            long delay=t.delayRemaining(now,SystemClock.elapsedRealtime());
            state.setText(delay>0?"Start in "+
                    TimerDisplay.time(delay,TimerAlertSettings.FORMAT_MS):"Läuft");
            primary.setText("Ⅱ  Pausieren");
        }else if(t.state==TimerEntry.FINISHED){
            state.setText("Fertig");
            primary.setText("▶  Neu starten");
        }else{
            state.setText(t.state==TimerEntry.PAUSED?"Pausiert":"Bereit");
            primary.setText(TimerStartSchedule.get(this,timerId)>0?
                    "▶  Jetzt starten":"▶  Starten");
        }
        int info=TimerAlertSettings.extraInfo(this,timerId);
        target.setVisibility(info==TimerAlertSettings.EXTRA_NONE?View.GONE:View.VISIBLE);
        if(info==TimerAlertSettings.EXTRA_DURATION){
            target.setText("Dauer: "+TimerDisplay.time(t.durationMs,
                    TimerAlertSettings.timeFormat(this,timerId)));
        }else if(info==TimerAlertSettings.EXTRA_TARGET){
            target.setText(t.state==TimerEntry.FINISHED?
                    "Timer abgelaufen":
                    (t.state==TimerEntry.RUNNING || t.state==TimerEntry.WAITING?
                            "Ziel-Uhrzeit: ":
                    "Ziel-Uhrzeit bei Start jetzt: ")+
                    formatTime((t.state==TimerEntry.RUNNING ||
                            t.state==TimerEntry.WAITING)?t.dueWallMs:now+left));
        }
        timeFormatChoice.setText(TimerDisplay.formatLabel(
                TimerAlertSettings.timeFormat(this,timerId))+"  ›");
        extraInfoChoice.setText(TimerDisplay.extraLabel(info)+"  ›");
        startDelayChoice.setText(delayLabel(
                TimerAlertSettings.startDelaySeconds(this,timerId))+"  ›");
        intervalChoice.setText(intervalLabel(
                TimerAlertSettings.intervalSeconds(this,timerId))+"  ›");
        updating=true;
        tone.setChecked(TimerAlertSettings.tone(this,timerId));
        vibration.setChecked(TimerAlertSettings.vibration(this,timerId));
        repeat.setChecked(TimerAlertSettings.autoRepeat(this,timerId) &&
                TimerAlertSettings.canRepeat(t.durationMs));
        prepareCue.setChecked(TimerAlertSettings.prepareCue(this,timerId));
        oneShot.setChecked(TimerAlertSettings.oneShot(this,timerId));
        updating=false;
    }
    private static String formatTime(long when){
        return new SimpleDateFormat("HH:mm:ss",Locale.GERMANY).format(new Date(when));
    }
    private void editTimer(){
        TimerEntry entry=TimerRepository.get(this,timerId);
        if(entry!=null)TimerEditor.show(this,entry,this::updateState);
    }
    private void toggleFavorite(){
        try{
            TimerEntry t=TimerRepository.get(this,timerId);
            if(t!=null)TimerRepository.put(this,t.favorite(!t.favorite));
            updateState();
        }catch(RuntimeException e){showError(e.getMessage());}
    }
    private void runAction(int action){
        try{
            TimerEntry current=TimerRepository.get(this,timerId);
            if(current==null)return;
            if(action==0){
                TimerScheduler.reset(this,timerId);
            }else if(current.state==TimerEntry.RUNNING ||
                    current.state==TimerEntry.WAITING){
                TimerScheduler.pause(this,timerId);
            }else{
                if(!canStart())return;
                if(current.state==TimerEntry.FINISHED)TimerScheduler.reset(this,timerId);
                TimerScheduler.start(this,timerId);
            }
            updateState();
        }catch(SecurityException e){allowExactAlarm();}
        catch(RuntimeException e){showError("Timer-Aktion fehlgeschlagen: "+e.getMessage());}
    }
    private boolean canStart(){
        if(Build.VERSION.SDK_INT>=33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        !=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},94);
            return false;
        }
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(manager==null||!manager.areNotificationsEnabled()){
            showError("Bitte zuerst Android-Benachrichtigungen für Sanftwecker aktivieren.");
            return false;
        }
        if(!AlarmScheduler.canSchedule(this)){allowExactAlarm();return false;}
        return true;
    }
    private void allowExactAlarm(){
        new AlertDialog.Builder(this).setTitle("Exakte Timer")
                .setMessage("Android muss Alarme und Erinnerungen für Sanftwecker erlauben.")
                .setNegativeButton("Abbrechen",null)
                .setPositiveButton("Einstellungen",(d,w)->{
                    try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(Uri.parse("package:"+getPackageName())));}
                    catch(RuntimeException e){showError("Bitte Alarme manuell erlauben.");}
                }).show();
    }
    private void duplicateTimer(){
        try{
            TimerEntry old=TimerRepository.get(this,timerId);
            if(old==null)return;
            TimerEntry copy=TimerRepository.create(this,old.name+" Kopie",old.durationMs);
            if(old.favorite)TimerRepository.put(this,copy.favorite(true));
            TimerAlertSettings.copy(this,old.id,copy.id);
            startActivity(new Intent(this,TimerDetailActivity.class)
                    .putExtra(TimerScheduler.EXTRA_ID,copy.id));
            finish();
        }catch(RuntimeException e){showError("Duplizieren fehlgeschlagen: "+e.getMessage());}
    }
    private void deleteTimer(){
        TimerEntry t=TimerRepository.get(this,timerId);
        if(t==null)return;
        new AlertDialog.Builder(this).setTitle("Timer löschen?")
                .setMessage("„"+t.name+"“ dauerhaft entfernen?")
                .setNegativeButton("Abbrechen",null)
                .setPositiveButton("Löschen",(dialog,which)->{
                    try{TimerScheduler.delete(this,timerId);finish();}
                    catch(RuntimeException e){showError("Löschen fehlgeschlagen: "+e.getMessage());}
                }).show();
    }
    private void showError(String text){
        new AlertDialog.Builder(this).setMessage(text).setPositiveButton("OK",null).show();
    }
}
