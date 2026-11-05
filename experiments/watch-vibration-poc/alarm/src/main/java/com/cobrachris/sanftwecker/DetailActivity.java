package com.cobrachris.sanftwecker;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Dark alarm details screen. Input state is only saved through explicit action. */
public final class DetailActivity extends Activity {
    private static final int NOTIFY_REQ=18;
    private final String[] weekdays={"Mo","Di","Mi","Do","Fr","Sa","So"};
    private int id,hour,minute;
    private boolean loading,dirty,advancedOpen;
    private AlarmProfile saved;
    private EditText name;
    private TextView clock,status,advanced;
    private final TextView[] chips=new TextView[7];
    private final boolean[] days=new boolean[7];
    private Switch enabled,rising,watch,snoozeEnabled;
    private Spinner tone;
    private SeekBar delay,ramp,volume,pulses,interval,snooze,autoStop;
    private TextView delayLabel,rampLabel,volumeLabel,pulsesLabel,intervalLabel,snoozeLabel,stopLabel;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        UiKit.window(this);
        id=getIntent().getIntExtra(AlarmScheduler.EXTRA_ID,-1);
        saved=AlarmRepository.get(this,id);
        if(saved==null){finish();return;}
        RingingService.ensureChannels(this);
        build();
        load(saved);
    }
    @Override public void onResume(){super.onResume();if(status!=null)diagnostics();}
    private TextView label(String s,int size){return UiKit.label(this,s,size,UiKit.TEXT);}
    private LinearLayout section(LinearLayout root,String title){
        LinearLayout card=UiKit.card(this);
        card.addView(UiKit.heading(this,title));
        UiKit.placeCard(this,root,card);
        return card;
    }
    private void rowSwitch(LinearLayout into,String text,Switch control){
        LinearLayout row=UiKit.row(this);
        row.addView(label(text,17),UiKit.weight());
        row.addView(control);
        into.addView(row);
    }
    private SeekBar slider(LinearLayout card,int max){
        SeekBar s=UiKit.seek(this,max);
        card.addView(s);
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean user){
                updateLabels();if(user)changed();
            }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        return s;
    }
    private void build(){
        LinearLayout root=UiKit.mainColumn(this);
        LinearLayout header=UiKit.row(this);
        TextView back=UiKit.action(this,"‹");
        back.setContentDescription("Zurück");
        back.setOnClickListener(v->back());
        header.addView(back);
        header.addView(UiKit.heading(this,"Wecker-Detail"),UiKit.weight());
        TextView test=UiKit.action(this,"▶");
        test.setContentDescription("Test in 30 Sekunden");
        test.setOnClickListener(v->testAlarm());
        header.addView(test);
        TextView save=UiKit.action(this,"✓");
        save.setContentDescription("Speichern");
        save.setOnClickListener(v->{if(save())finish();});
        header.addView(save);
        TextView overflow=UiKit.action(this,"⋮");
        overflow.setOnClickListener(v->more(overflow));
        header.addView(overflow);
        root.addView(header);
        root.addView(UiKit.label(this,"Beta – bisherigen zuverlässigen Wecker beibehalten",13,UiKit.MUTED));

        LinearLayout top=UiKit.card(this);
        name=new EditText(this);
        name.setSingleLine(true);name.setTextSize(19);name.setTextColor(UiKit.TEXT);
        name.setHint("Weckername");name.setHintTextColor(UiKit.MUTED);
        name.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int c,int f){}
            public void onTextChanged(CharSequence s,int a,int b,int c){changed();}
            public void afterTextChanged(Editable e){}
        });
        top.addView(name);
        LinearLayout time=UiKit.row(this);
        clock=UiKit.label(this,"07:00",64,UiKit.BLUE);
        clock.setContentDescription("Uhrzeit ändern");
        time.addView(clock,UiKit.weight());
        clock.setOnClickListener(v->new TimePickerDialog(this,(picker,h,m)->{
            hour=h;minute=m;showTime();changed();
        },hour,minute,true).show());
        enabled=UiKit.toggle(this);
        enabled.setContentDescription("Wecker aktivieren");
        enabled.setOnCheckedChangeListener((b,value)->changed());
        time.addView(enabled);
        top.addView(time);
        LinearLayout row=UiKit.row(this);
        for(int i=0;i<7;i++){
            final int which=i;
            chips[i]=UiKit.chip(this,weekdays[i],false);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,UiKit.dp(this,42),1);
            p.rightMargin=UiKit.dp(this,4);
            row.addView(chips[i],p);
            chips[i].setOnClickListener(v->{
                days[which]=!days[which];
                UiKit.setChip(this,chips[which],days[which]);
                changed();
            });
        }
        top.addView(row);UiKit.placeCard(this,root,top);

        LinearLayout music=section(root,"Melodie & sanfter Ton");
        tone=new Spinner(this);
        tone.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Sanftes Glockenspiel","Reiner weicher Ton","Tiefe Harmonie"}));
        music.addView(tone);
        tone.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long key){changed();}
            public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        rising=UiKit.toggle(this);
        rising.setOnCheckedChangeListener((b,value)->{
            if(ramp!=null)ramp.setEnabled(value);changed();
        });
        rowSwitch(music,"Ansteigende Lautstärke",rising);
        delayLabel=label("",15);music.addView(delayLabel);
        delay=slider(music,30);
        rampLabel=label("",15);music.addView(rampLabel);
        ramp=slider(music,29);
        volumeLabel=label("",15);music.addView(volumeLabel);
        volume=slider(music,95);
        Button preview=UiKit.button(this,"Tonprobe starten (15 Sek.)",false);
        music.addView(preview);
        preview.setOnClickListener(v->previewTone());

        LinearLayout wrist=section(root,"Galaxy Watch");
        watch=UiKit.toggle(this);
        watch.setOnCheckedChangeListener((b,value)->{
            if(!loading && pulses!=null){
                if(!value)pulses.setProgress(0);
                else if(pulses.getProgress()==0)pulses.setProgress(6);
                pulses.setEnabled(value);
                interval.setEnabled(value);
            }
            changed();
        });
        rowSwitch(wrist,"Uhr benachrichtigen",watch);
        pulsesLabel=label("",15);wrist.addView(pulsesLabel);
        pulses=slider(wrist,12);
        intervalLabel=label("",15);wrist.addView(intervalLabel);
        interval=slider(wrist,11);
        Button watchTest=UiKit.button(this,"Watch-Test senden",false);
        wrist.addView(watchTest);watchTest.setOnClickListener(v->watchTest());
        wrist.addView(UiKit.label(this,"Die Uhr empfängt gespiegelte Benachrichtigungen. "+
                "Samsung entscheidet, ob sie vibriert.",13,UiKit.MUTED));

        LinearLayout end=section(root,"Schlummern & Stoppen");
        snoozeEnabled=UiKit.toggle(this);
        rowSwitch(end,"Schlummern aktiv",snoozeEnabled);
        snoozeEnabled.setOnCheckedChangeListener((b,value)->{
            if(snooze!=null)snooze.setEnabled(value);
            changed();
        });
        snoozeLabel=label("",15);end.addView(snoozeLabel);
        snooze=slider(end,29);
        stopLabel=label("",15);end.addView(stopLabel);
        autoStop=slider(end,29);
        end.addView(UiKit.label(this,"Stoppen und Schlummern erscheinen beim Klingeln "+
                "in der App und in der Android-Benachrichtigung.",13,UiKit.MUTED));

        LinearLayout extra=section(root,"Erweitert & Diagnose");
        TextView expand=UiKit.action(this,"Diagnose anzeigen ▾");
        expand.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        extra.addView(expand);
        advanced=UiKit.label(this,"",14,UiKit.MUTED);
        extra.addView(advanced);
        advanced.setVisibility(View.GONE);
        expand.setOnClickListener(v->{
            advancedOpen=!advancedOpen;
            advanced.setVisibility(advancedOpen?View.VISIBLE:View.GONE);
            expand.setText(advancedOpen?"Diagnose ausblenden ▴":"Diagnose anzeigen ▾");
            diagnostics();
        });
        Button settings=UiKit.button(this,"App-Berechtigungen",false);
        extra.addView(settings);
        settings.setOnClickListener(v->startActivity(new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:"+getPackageName()))));

        Button bottom=UiKit.button(this,"Speichern",true);
        root.addView(bottom);bottom.setOnClickListener(v->{if(save())finish();});
        Button stopTest=UiKit.button(this,"Geplanten Test abbrechen",false);
        root.addView(stopTest);
        stopTest.setOnClickListener(v->{AlarmScheduler.cancelTest(this);diagnostics();});
        status=UiKit.label(this,"",13,UiKit.MUTED);
        root.addView(status);
        setContentView(UiKit.scroll(this,root));
    }

    private void changed(){
        if (loading || saved == null || autoStop == null || snoozeEnabled == null) return;
        try { dirty = !saved.sameConfiguration(read()); }
        catch (RuntimeException invalid) { dirty = true; }
    }
    private void showTime(){clock.setText(String.format(Locale.GERMANY,"%02d:%02d",hour,minute));}
    private void updateLabels(){
        if(delayLabel!=null&&delay!=null)delayLabel.setText("Tonstart: +"+delay.getProgress()*10+" Sekunden");
        if(rampLabel!=null&&ramp!=null)rampLabel.setText("Anstieg: "+(ramp.getProgress()+1)+" Minuten");
        if(volumeLabel!=null&&volume!=null)volumeLabel.setText("Maximaler App-Pegel: "+
                (volume.getProgress()+5)+" % des Android-Weckerpegels");
        if(pulsesLabel!=null&&pulses!=null)pulsesLabel.setText("Watch-Impulse: "+pulses.getProgress());
        if(intervalLabel!=null&&interval!=null)intervalLabel.setText("Intervall: "+
                (interval.getProgress()+1)*10+" Sekunden");
        if(snoozeLabel!=null&&snooze!=null)snoozeLabel.setText("Schlummern: "+
                (snooze.getProgress()+1)+" Minuten");
        if(stopLabel!=null&&autoStop!=null)stopLabel.setText("Auto-Stopp nach "+
                (autoStop.getProgress()+1)+" Minuten");
    }

    private void load(AlarmProfile p){
        loading=true;saved=p;name.setText(p.name);hour=p.hour;minute=p.minute;showTime();
        enabled.setChecked(p.enabled);
        for(int i=0;i<7;i++){
            days[i]=(p.days&(1<<i))!=0;
            UiKit.setChip(this,chips[i],days[i]);
        }
        tone.setSelection(p.tone);rising.setChecked(p.rampEnabled);
        delay.setProgress(p.delaySeconds/10);ramp.setProgress(p.rampSeconds/60-1);
        volume.setProgress(p.maxVolume-5);watch.setChecked(p.watchPulses>0);
        pulses.setProgress(p.watchPulses);interval.setProgress(p.watchIntervalSeconds/10-1);
        snoozeEnabled.setChecked(p.snoozeEnabled);
        snooze.setProgress(p.snoozeMinutes-1);snooze.setEnabled(p.snoozeEnabled);
        autoStop.setProgress(p.autoStopMinutes-1);
        ramp.setEnabled(p.rampEnabled);
        pulses.setEnabled(p.watchPulses>0);interval.setEnabled(p.watchPulses>0);
        loading=false;dirty=false;updateLabels();diagnostics();
    }

    private AlarmProfile read(){
        int mask=0;for(int i=0;i<7;i++)if(days[i])mask|=1<<i;
        boolean changedTime=hour!=saved.hour||minute!=saved.minute||mask!=saved.days;
        return new AlarmProfile(id,name.getText().toString(),enabled.isChecked(),hour,minute,mask,
                delay.getProgress()*10,(ramp.getProgress()+1)*60,volume.getProgress()+5,
                watch.isChecked()?pulses.getProgress():0,(interval.getProgress()+1)*10,
                snooze.getProgress()+1,tone.getSelectedItemPosition(),rising.isChecked(),
                autoStop.getProgress()+1,changedTime?0:saved.skipAt,
                changedTime?0:saved.oneTimeAt,changedTime?0:saved.replacedAt,
                snoozeEnabled.isChecked());
    }

    private boolean save(){
        AlarmProfile update;
        try{update=read();}catch(RuntimeException e){
            notice("Bitte mindestens einen Wochentag auswählen.");return false;
        }
        if(update.enabled&&!AlarmScheduler.canSchedule(this)){
            new AlertDialog.Builder(this).setTitle("Exakte Alarme erlauben")
                .setMessage("Ohne Android-Berechtigung kann der Wecker nicht aktiviert werden.")
                .setNegativeButton("Abbrechen",null)
                .setPositiveButton("Einstellungen",(d,w)->{
                    try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(Uri.parse("package:"+getPackageName())));}
                    catch(RuntimeException e){notice("Bitte speziellen App-Zugriff erlauben.");}
                }).show();
            return false;
        }
        AlarmProfile old=AlarmRepository.get(this,id);
        if(old==null){notice("Wecker existiert nicht mehr.");return false;}
        try{
            AlarmRepository.put(this,update);AlarmScheduler.reschedule(this,id);
            saved=update;dirty=false;diagnostics();return true;
        }catch(RuntimeException e){
            try{AlarmRepository.put(this,old);AlarmScheduler.reschedule(this,id);}
            catch(RuntimeException ignored){}
            notice("Speichern fehlgeschlagen: "+e.getMessage());return false;
        }
    }

    private void testAlarm(){
        if(!save())return;
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                !=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFY_REQ);return;
        }
        try{
            long when=AlarmScheduler.test(this,id);
            notice("Test geplant um "+new java.text.SimpleDateFormat("HH:mm:ss",
                    Locale.GERMANY).format(new Date(when))+". Bitte Handy sperren.");
        }catch(RuntimeException e){notice("Test konnte nicht starten: "+e.getMessage());}
    }

    private void previewTone(){
        if(!save())return;
        try{
            startForegroundService(new Intent(this,RingingService.class)
                    .setAction(RingingService.START).putExtra(AlarmScheduler.EXTRA_ID,id)
                    .putExtra("tone_only_preview",true));
            notice("Tonprobe läuft 15 Sekunden. Über die Alarmbenachrichtigung stoppen.");
        }catch(RuntimeException e){notice("Tonprobe fehlgeschlagen.");}
    }

    private void watchTest(){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                !=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFY_REQ);return;
        }
        NotificationManager m=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(m==null||!m.areNotificationsEnabled()){notice("Benachrichtigungen deaktiviert.");return;}
        RingingService.ensureChannels(this);
        Notification n=new Notification.Builder(this,RingingService.WATCH_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Sanftwecker – Watch-Test")
                .setContentText("Bitte prüfen, ob deine Galaxy Watch vibriert.")
                .setCategory(Notification.CATEGORY_REMINDER).setAutoCancel(true).build();
        try{
            m.notify((int)(System.currentTimeMillis()&0x7fffffff),n);
            notice("Test gesendet. Ob die Uhr vibriert hat, bitte selbst prüfen.");
        }catch(SecurityException e){notice("Benachrichtigungsrecht fehlt.");}
    }

    private void diagnostics(){
        if(status==null)return;
        long upcoming=AlarmRepository.next(this,id);
        status.setText("Nächster Termin: "+(upcoming<=0?"nicht eingeplant":
                new java.text.SimpleDateFormat("dd.MM. HH:mm",Locale.GERMANY)
                        .format(new Date(upcoming))));
        if(advanced!=null){
            NotificationManager m=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            advanced.setText("Exakte Alarme: "+(AlarmScheduler.canSchedule(this)?"OK":"FEHLEN")+
                    "\nBenachrichtigungen: "+(m!=null&&m.areNotificationsEnabled()?"OK":"FEHLEN")+
                    "\nLetzter Status: "+AlarmRepository.lastEvent(this)+
                    "\nUhr-Empfang: nicht messbar; Schlafmodus ungeprüft.");
        }
    }

    private void more(View anchor){
        PopupMenu menu=new PopupMenu(this,anchor);
        menu.getMenu().add("Duplizieren").setOnMenuItemClickListener(item->{
            if(save())try{
                int newId=AlarmRepository.create(this,saved);
                startActivity(new Intent(this,DetailActivity.class)
                        .putExtra(AlarmScheduler.EXTRA_ID,newId));finish();
            }catch(RuntimeException e){notice("Duplizieren fehlgeschlagen.");}
            return true;
        });
        menu.getMenu().add("Nächsten Termin überspringen").setOnMenuItemClickListener(item->{
            if(save())try{AlarmScheduler.skipNext(this,id);load(AlarmRepository.get(this,id));}
            catch(RuntimeException e){notice("Überspringen fehlgeschlagen.");}
            return true;
        });
        menu.getMenu().add("Ausnahme rückgängig").setOnMenuItemClickListener(item->{
            if(save())try{
                AlarmProfile p=AlarmRepository.get(this,id);
                AlarmRepository.put(this,p.withExceptions(0,0,0));
                AlarmScheduler.reschedule(this,id);load(AlarmRepository.get(this,id));
            }catch(RuntimeException e){notice("Rückgängig fehlgeschlagen.");}
            return true;
        });
        menu.getMenu().add("Löschen").setOnMenuItemClickListener(item->{
            new AlertDialog.Builder(this).setTitle("Wecker löschen?")
                    .setMessage("„"+saved.name+"“ wirklich löschen?")
                    .setNegativeButton("Abbrechen",null)
                    .setPositiveButton("Löschen",(d,w)->{
                        try{AlarmScheduler.delete(this,id);dirty=false;finish();}
                        catch(RuntimeException e){notice("Löschen fehlgeschlagen.");}
                    }).show();
            return true;
        });
        menu.show();
    }

    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==NOTIFY_REQ)notice(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED
                ?"Benachrichtigungen erlaubt. Test erneut starten."
                :"Ohne Benachrichtigungsrecht kann die Uhr stumm bleiben.");
    }
    private void notice(String text){
        new AlertDialog.Builder(this).setMessage(text).setPositiveButton("OK",null).show();
    }
    private void back(){
        if(!dirty){finish();return;}
        new AlertDialog.Builder(this).setTitle("Änderungen speichern?")
                .setPositiveButton("Speichern",(d,w)->{if(save())finish();})
                .setNeutralButton("Verwerfen",(d,w)->{dirty=false;finish();})
                .setNegativeButton("Weiter bearbeiten",null).show();
    }
    @Override public void onBackPressed(){back();}
}
