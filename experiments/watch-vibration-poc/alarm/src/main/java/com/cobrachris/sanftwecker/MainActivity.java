package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Dark alarm list with arbitrary saved alarms, shortcuts and one-off actions. */
public final class MainActivity extends Activity {
    private final String[] dayNames = {"M","D","M","D","F","S","S"};
    private LinearLayout list;
    private TextView subheading;
    private Button fullScreenPermissionButton;
    private boolean sortByNext = true;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        UiKit.window(this);
        AlarmVolumeGuard.recoverIfIdle(this);
        RingingService.ensureChannels(this);
        sortByNext = getPreferences(MODE_PRIVATE).getBoolean("next_sort", true);
        buildScreen();
    }

    @Override public void onResume() {
        super.onResume();
        if (list != null) {
            if (!AlarmScheduler.canSchedule(this)) {
                for (AlarmProfile p : AlarmRepository.all(this))
                    if (p.enabled) AlarmRepository.next(this,p.id,0L);
            } else {
                for (AlarmProfile p : AlarmRepository.all(this)) {
                    if (p.enabled && AlarmRepository.next(this,p.id)==0L) {
                        try { AlarmScheduler.reschedule(this,p.id); }
                        catch(RuntimeException e) {
                            AlarmRepository.diagnostic(this,"WARNUNG: "+p.name+" nicht eingeplant");
                        }
                    }
                }
            }
            renderList();
        }
        refreshFullScreenPermissionUi();
    }

    private void buildScreen() {
        FrameLayout page = new FrameLayout(this);
        page.setBackgroundColor(UiKit.BG);
        LinearLayout base = UiKit.mainColumn(this);

        LinearLayout header = UiKit.row(this);
        LinearLayout headerText = UiKit.column(this);
        headerText.addView(UiKit.label(this, "Meine Wecker", 28, UiKit.TEXT));
        subheading = UiKit.label(this, "Noch keine Wecker", 14, UiKit.MUTED);
        headerText.addView(subheading);
        header.addView(headerText, UiKit.weight());
        TextView order = UiKit.action(this, "☷");
        order.setContentDescription("Sortierung wählen");
        header.addView(order);
        order.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(this, v);
            menu.getMenu().add("Nach nächster Auslösung").setOnMenuItemClickListener(item -> {
                sortByNext = true; saveSort(); renderList(); return true;
            });
            menu.getMenu().add("Nach Uhrzeit").setOnMenuItemClickListener(item -> {
                sortByNext = false; saveSort(); renderList(); return true;
            });
            menu.show();
        });
        base.addView(header);
        ClockTabs.add(this,base,ClockTabs.ALARMS);
        Button updates=UiKit.button(this,"App aktualisieren",false);
        base.addView(updates);
        updates.setOnClickListener(v->startActivity(
                new Intent(this,UpdateActivity.class)));
        TextView safety = UiKit.label(this,
                "Beta · bitte bisherigen Wecker weiterhin benutzen", 13, UiKit.MUTED);
        LinearLayout.LayoutParams safetyParams = new LinearLayout.LayoutParams(-1,-2);
        safetyParams.topMargin = UiKit.dp(this, 8);
        safetyParams.bottomMargin = UiKit.dp(this, 14);
        base.addView(safety, safetyParams);

        Button stopNow=UiKit.button(this,"SOFORT STOPPEN – Ton und Vibration",true);
        base.addView(stopNow);
        stopNow.setOnClickListener(v->{
            PhoneVibration.stop();
            AlarmScheduler.cancelTest(this);
            try{
                startService(new Intent(this,RingingService.class).setAction(RingingService.STOP));
                new android.app.AlertDialog.Builder(this)
                        .setMessage("Not-Stopp angefordert: Ton und Handyvibration beenden.")
                        .setPositiveButton("OK",null).show();
            }catch(RuntimeException e){
                new android.app.AlertDialog.Builder(this)
                        .setMessage("Handyvibration gestoppt. Falls der Alarm weiterläuft: "+
                                "Einstellungen → Apps → Sanftwecker (Test) → Stopp erzwingen.")
                        .setPositiveButton("OK",null).show();
            }
        });
        if(android.os.Build.VERSION.SDK_INT>=34){
            fullScreenPermissionButton=UiKit.button(this,"Klingelbildschirm erlauben",false);
            base.addView(fullScreenPermissionButton);
            fullScreenPermissionButton.setOnClickListener(v->{
                try{
                    startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                            .setData(android.net.Uri.parse("package:"+getPackageName())));
                }catch(RuntimeException e){
                    new android.app.AlertDialog.Builder(this)
                            .setMessage("Bitte Vollbild-Benachrichtigungen für Sanftwecker "+
                                    "in den Android-Einstellungen freigeben.")
                            .setPositiveButton("OK",null).show();
                }
            });
            refreshFullScreenPermissionUi();
        }

        list = UiKit.column(this);
        base.addView(list);
        page.addView(UiKit.scroll(this, base));

        TextView add = UiKit.label(this, "+", 36, UiKit.BG);
        add.setGravity(Gravity.CENTER);
        add.setBackground(UiKit.shape(UiKit.BLUE, this, 25));
        add.setContentDescription("Neuen Wecker anlegen");
        FrameLayout.LayoutParams fab = new FrameLayout.LayoutParams(
                UiKit.dp(this, 64), UiKit.dp(this, 64),
                Gravity.END | Gravity.BOTTOM);
        fab.setMargins(0,0,UiKit.dp(this,24),UiKit.dp(this,24));
        page.addView(add, fab);
        add.setOnClickListener(v -> {
            try {
                int id = AlarmRepository.create(this, null);
                openDetail(id);
            } catch (RuntimeException ex) {
                error("Wecker konnte nicht erstellt werden: " + ex.getMessage());
            }
        });
        setContentView(page);
        renderList();
    }

    private void saveSort() {
        getPreferences(MODE_PRIVATE).edit().putBoolean("next_sort",sortByNext).apply();
    }

    private void renderList() {
        list.removeAllViews();
        List<AlarmProfile> profiles = AlarmRepository.all(this);
        List<AlarmProfile> ordered = new ArrayList<>(profiles);
        if (sortByNext) {
            Collections.sort(ordered, (a,b) -> {
                long aa = a.enabled && AlarmRepository.next(this,a.id)>0 ?
                        AlarmRepository.next(this,a.id) : Long.MAX_VALUE;
                long bb = b.enabled && AlarmRepository.next(this,b.id)>0 ?
                        AlarmRepository.next(this,b.id) : Long.MAX_VALUE;
                return aa == bb ? Integer.compare(a.id,b.id) : Long.compare(aa,bb);
            });
        } else {
            Collections.sort(ordered, (a,b) -> {
                int byTime=Integer.compare(a.hour*60+a.minute,b.hour*60+b.minute);
                return byTime==0 ? Integer.compare(a.id,b.id) : byTime;
            });
        }
        long earliest = Long.MAX_VALUE;
        for (AlarmProfile p : profiles) if (p.enabled) {
            long time=AlarmRepository.next(this,p.id);
            if (time>0) earliest=Math.min(earliest,time);
        }
        subheading.setText(profiles.isEmpty() ? "Noch keine Wecker" :
                profiles.size() + (profiles.size()==1 ? " Wecker" : " Wecker") +
                (earliest==Long.MAX_VALUE ? " · keiner aktiv" :
                        " · nächster " + shortTime(earliest)));
        if (profiles.isEmpty()) {
            LinearLayout empty=UiKit.card(this);
            TextView message=UiKit.label(this,
                    "Noch kein Wecker eingerichtet.\nTippe auf +, um deinen ersten Sanftwecker anzulegen.",
                    18, UiKit.TEXT);
            empty.addView(message);
            UiKit.placeCard(this,list,empty);
        }
        for (AlarmProfile p : ordered) UiKit.placeCard(this,list,renderCard(p,earliest));
        TextView hint=UiKit.label(this,
                "Die Galaxy Watch vibriert nur, wenn Galaxy Wearable die " +
                "Benachrichtigungen von „Sanftwecker (Test)“ weiterleitet. " +
                "Im Schlafmodus kann diese Weiterleitung ausbleiben.",
                13,UiKit.MUTED);
        list.addView(hint);
    }

    private LinearLayout renderCard(AlarmProfile p, long earliest) {
        LinearLayout card=UiKit.card(this);
        card.setAlpha(p.enabled?1f:0.74f);
        LinearLayout top=UiKit.row(this);
        TextView name=UiKit.label(this,p.name,16,UiKit.MUTED);
        name.setMaxLines(1);
        top.addView(name,UiKit.weight());
        TextView menu=UiKit.action(this,"⋯");
        menu.setContentDescription("Weitere Aktionen für "+p.name);
        top.addView(menu);
        card.addView(top);

        LinearLayout middle=UiKit.row(this);
        TextView time=UiKit.label(this,
                String.format(Locale.GERMANY,"%02d:%02d",p.hour,p.minute),
                59,p.enabled?UiKit.BLUE:UiKit.MUTED);
        middle.addView(time,UiKit.weight());
        Switch active=UiKit.toggle(this);
        active.setChecked(p.enabled);
        active.setContentDescription(p.name + (p.enabled?" deaktivieren":" aktivieren"));
        middle.addView(active);
        card.addView(middle);

        LinearLayout days=UiKit.row(this);
        for(int i=0;i<7;i++){
            TextView chip=UiKit.chip(this,dayNames[i],(p.days&(1<<i))!=0);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(
                    0,UiKit.dp(this,34),1);
            cp.rightMargin=UiKit.dp(this,5);
            days.addView(chip,cp);
        }
        card.addView(days);
        LinearLayout.LayoutParams pad=new LinearLayout.LayoutParams(-1,-2);
        pad.topMargin=UiKit.dp(this,9);
        TextView subtitle=UiKit.label(this,
                "Ton +" + p.delaySeconds + " s  ·  Rampe " + p.rampSeconds/60 +
                " Min.  ·  Watch " + (p.watchPulses==0 ? "aus" : p.watchPulses+"×"),
                13,UiKit.MUTED);
        card.addView(subtitle,pad);
        if (p.vibrationMode != 0) {
            card.addView(UiKit.label(this,
                    "Vibration: "+VibrationPresets.LABELS[p.vibrationMode],
                    12,UiKit.MUTED));
        }
        if (p.enabled && AlarmRepository.next(this,p.id)==earliest) {
            TextView next=UiKit.label(this,"NÄCHSTER ALARM · "+shortTime(earliest),12,UiKit.BLUE);
            card.addView(next);
        }
        if (p.enabled && AlarmRepository.next(this,p.id)==0L) {
            card.addView(UiKit.label(this,
                    "ACHTUNG: Wecker nicht eingeplant – Berechtigung prüfen",
                    13,Color.rgb(255,171,128)));
        }
        if (p.skipAt > 0) {
            card.addView(UiKit.label(this,"Ein Termin wird übersprungen",12,UiKit.MUTED));
        }
        if (p.oneTimeAt > System.currentTimeMillis()) {
            card.addView(UiKit.label(this,"Nächste Auslösung einmalig geändert",12,UiKit.BLUE));
        }
        card.setOnClickListener(v->openDetail(p.id));
        time.setOnClickListener(v->openDetail(p.id));
        active.setOnCheckedChangeListener((v,isChecked)-> {
            if (p.enabled==isChecked) return;
            try {
                AlarmScheduler.toggle(this,p.id,isChecked);
            } catch (RuntimeException ex) {
                v.setChecked(p.enabled);
                if (isChecked && !AlarmScheduler.canSchedule(this))
                    showAlarmPermission();
                else error("Wecker konnte nicht umgeschaltet werden: "+ex.getMessage());
            }
            renderList();
        });
        menu.setOnClickListener(v->showMenu(menu,p));
        return card;
    }

    private String shortTime(long millis) {
        return new SimpleDateFormat("EEE dd.MM. HH:mm",Locale.GERMANY).format(new Date(millis));
    }

    private void showMenu(View anchor,AlarmProfile p) {
        PopupMenu menu=new PopupMenu(this,anchor);
        menu.getMenu().add("Bearbeiten").setOnMenuItemClickListener(i->{
            openDetail(p.id); return true;
        });
        menu.getMenu().add("Duplizieren").setOnMenuItemClickListener(i->{
            try { int id=AlarmRepository.create(this,p); openDetail(id); }
            catch(RuntimeException e){error("Duplizieren fehlgeschlagen");}
            return true;
        });
        if(p.enabled) {
            menu.getMenu().add("Nächsten Wecker ändern").setOnMenuItemClickListener(i->{
                chooseNext(p); return true;
            });
            menu.getMenu().add("Nächsten Wecker überspringen").setOnMenuItemClickListener(i->{
                try { AlarmScheduler.skipNext(this,p.id); renderList(); }
                catch(RuntimeException e){error("Überspringen fehlgeschlagen");}
                return true;
            });
            if(p.skipAt>0 || p.oneTimeAt>0) {
                menu.getMenu().add("Ausnahme rückgängig machen").setOnMenuItemClickListener(i->{
                    try {
                        AlarmProfile updated=AlarmRepository.get(this,p.id);
                        if(updated!=null){
                            AlarmRepository.put(this,updated.withExceptions(0,0,0));
                            AlarmScheduler.reschedule(this,p.id);
                        }
                        renderList();
                    } catch(RuntimeException e){error("Rückgängig fehlgeschlagen");}
                    return true;
                });
            }
        }
        menu.getMenu().add("Löschen").setOnMenuItemClickListener(i->{
            new AlertDialog.Builder(this).setTitle("Wecker löschen?")
                    .setMessage("„"+p.name+"“ dauerhaft löschen?")
                    .setNegativeButton("Abbrechen",null)
                    .setPositiveButton("Löschen",(dialog,which)->{
                        try {AlarmScheduler.delete(this,p.id);renderList();}
                        catch(RuntimeException e){error("Löschen fehlgeschlagen");}
                    }).show();
            return true;
        });
        menu.show();
    }

    private void chooseNext(AlarmProfile p) {
        Calendar c=Calendar.getInstance();
        long upcoming=AlarmRepository.next(this,p.id);
        if(upcoming>System.currentTimeMillis()) c.setTimeInMillis(upcoming);
        new DatePickerDialog(this,(view,y,m,d)->{
            Calendar candidate=Calendar.getInstance();
            candidate.set(y,m,d,p.hour,p.minute,0);
            candidate.set(Calendar.MILLISECOND,0);
            new TimePickerDialog(this,(time,h,min)->{
                candidate.set(Calendar.HOUR_OF_DAY,h);
                candidate.set(Calendar.MINUTE,min);
                try {
                    AlarmScheduler.changeNext(this,p.id,candidate.getTimeInMillis());
                    renderList();
                }catch(RuntimeException e){error("Ungültiger Zeitpunkt: "+e.getMessage());}
            },p.hour,p.minute,true).show();
        },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openDetail(int id) {
        startActivity(new Intent(this,DetailActivity.class)
                .putExtra(AlarmScheduler.EXTRA_ID,id));
    }

    private void refreshFullScreenPermissionUi() {
        if (android.os.Build.VERSION.SDK_INT < 34 || fullScreenPermissionButton == null) return;
        android.app.NotificationManager manager =
                (android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        boolean allowed = manager != null && manager.canUseFullScreenIntent();
        fullScreenPermissionButton.setVisibility(allowed ? View.GONE : View.VISIBLE);
    }

    private void showAlarmPermission() {
        new AlertDialog.Builder(this).setTitle("Alarme und Erinnerungen")
                .setMessage("Android muss exakte Alarme für Sanftwecker (Test) erlauben. " +
                        "Sonst darf der Schalter nicht eingeschaltet werden.")
                .setNegativeButton("Abbrechen",null)
                .setPositiveButton("Einstellungen",(d,w)->{
                    try {
                        startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .setData(Uri.parse("package:"+getPackageName())));
                    }catch(RuntimeException e){error("Bitte speziellen App-Zugriff manuell erlauben.");}
                }).show();
    }

    private void error(String message) {
        new AlertDialog.Builder(this).setTitle("Hinweis").setMessage(message)
                .setPositiveButton("OK",null).show();
    }
}
