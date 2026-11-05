package com.cobrachris.sanftwecker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** Three local alarm presets; no advertisements, accounts or network requests. */
public final class MainActivity extends Activity {
    private static final int NOTIFY_PERMISSION = 15;
    private int selectedId = 1;
    private int selectedHour;
    private int selectedMinute;
    private boolean pendingPreview;
    private Spinner slots;
    private EditText name;
    private CheckBox enabled;
    private final CheckBox[] weekdays = new CheckBox[7];
    private Spinner toneSpinner;
    private SeekBar delayBar, rampBar, volumeBar, pulsesBar, intervalBar, snoozeBar;
    private TextView delayText, rampText, volumeText, pulsesText, intervalText, snoozeText;
    private TextView timeText, status;
    private boolean updating;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                getIntent().getBooleanExtra("from_alarm", false)) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        selectedId = Math.max(1, Math.min(AlarmProfile.COUNT,
                getIntent().getIntExtra(AlarmScheduler.EXTRA_ID, 1)));
        RingingService.ensureChannels(this);
        buildUi();
    }

    @Override protected void onResume() {
        super.onResume();
        if (status != null) refreshStatus();
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setPadding(0, 11, 0, 7);
        return view;
    }

    private Button button(LinearLayout into, String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        into.addView(b, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return b;
    }

    private void buildUi() {
        ScrollView scrolling = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 30, 32, 60);
        root.setBackgroundColor(Color.WHITE);
        scrolling.addView(root);

        root.addView(text("Sanftwecker", 30, Color.BLACK));
        root.addView(text(
                "Beginnt wirklich bei 0 % App-Lautstärke. Die Galaxy Watch erhält " +
                "gesonderte Benachrichtigungen. Beta – bisherigen Wecker noch behalten.",
                15, Color.DKGRAY));

        root.addView(text("Wecker auswählen", 19, Color.BLACK));
        slots = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Wecker 1", "Wecker 2", "Wecker 3"});
        slots.setAdapter(adapter);
        root.addView(slots);
        slots.setSelection(selectedId - 1);
        slots.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> view,
                    android.view.View selected, int index, long itemId) {
                if (selectedId != index + 1) {
                    selectedId = index + 1;
                    load();
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        root.addView(text("Name", 17, Color.BLACK));
        name = new EditText(this);
        name.setSingleLine(true);
        name.setHint("z. B. Werktage");
        root.addView(name);

        root.addView(text("Weckzeit", 19, Color.BLACK));
        timeText = text("", 28, Color.BLACK);
        root.addView(timeText);
        Button selectTime = button(root, "Uhrzeit einstellen");
        selectTime.setOnClickListener(v -> new TimePickerDialog(this,
                (view, hour, minute) -> {
                    selectedHour = hour;
                    selectedMinute = minute;
                    updateTime();
                }, selectedHour, selectedMinute, true).show());

        root.addView(text("Wochentage", 19, Color.BLACK));
        String[] days = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};
        for (int row = 0; row < 2; row++) {
            LinearLayout dayRow = new LinearLayout(this);
            for (int col=0; col<4; col++) {
                int i=row*4+col;
                if (i >= 7) break;
                CheckBox day = new CheckBox(this);
                day.setText(days[i]);
                day.setTextSize(14);
                day.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.rgb(27,112,190)));
                dayRow.addView(day, new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                weekdays[i] = day;
            }
            root.addView(dayRow);
        }

        enabled = new CheckBox(this);
        enabled.setText("Diesen Wecker einschalten");
        enabled.setTextSize(17);
        root.addView(enabled);

        root.addView(text("Sanfter Weckton", 20, Color.BLACK));
        toneSpinner = new Spinner(this);
        toneSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Sanftes Glockenspiel", "Reiner weicher Ton", "Tiefe Harmonie"}));
        root.addView(toneSpinner);

        delayText = text("", 17, Color.BLACK);
        root.addView(delayText);
        delayBar = bar(root, 30, () -> updateSliders()); // 0–300 s in 10-s-Schritten

        rampText = text("", 17, Color.BLACK);
        root.addView(rampText);
        rampBar = bar(root, 29, () -> updateSliders()); // 1–30 min

        volumeText = text("", 17, Color.BLACK);
        root.addView(volumeText);
        volumeBar = bar(root, 95, () -> updateSliders()); // 5–100 %

        root.addView(text("Galaxy-Watch-Vibration", 20, Color.BLACK));
        pulsesText = text("", 17, Color.BLACK);
        root.addView(pulsesText);
        pulsesBar = bar(root, 12, () -> updateSliders());

        intervalText = text("", 17, Color.BLACK);
        root.addView(intervalText);
        intervalBar = bar(root, 11, () -> updateSliders()); // 10–120 s

        snoozeText = text("", 17, Color.BLACK);
        root.addView(snoozeText);
        snoozeBar = bar(root, 29, () -> updateSliders()); // 1–30 min

        Button save = button(root, "Einstellungen speichern");
        save.setOnClickListener(v -> persist(false));
        Button preview = button(root, "Testwecker in 30 Sekunden");
        preview.setOnClickListener(v -> persist(true));
        Button cancel = button(root, "Geplanten Test abbrechen");
        cancel.setOnClickListener(v -> {
            AlarmScheduler.cancelTest(this);
            refreshStatus();
        });
        Button stop = button(root, "Klingeln stoppen");
        stop.setOnClickListener(v -> {
            startService(new Intent(this, RingingService.class).setAction(RingingService.STOP));
            refreshStatus();
        });
        Button snooze = button(root, "Jetzt schlummern");
        snooze.setOnClickListener(v ->
                startService(new Intent(this, RingingService.class).setAction(RingingService.SNOOZE)));

        root.addView(text("Systemstatus und Diagnose", 20, Color.BLACK));
        status = text("", 15, Color.DKGRAY);
        root.addView(status);

        Button rights = button(root, "App- und Benachrichtigungseinstellungen öffnen");
        rights.setOnClickListener(v -> {
            Intent settings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + getPackageName()));
            startActivity(settings);
        });
        root.addView(text(
                "Wichtig: In Galaxy Wearable → Uhreneinstellungen → Benachrichtigungen → " +
                "App-Benachrichtigungen den Eintrag „Sanftwecker (Test)“ freigeben. " +
                "Das ist nicht die ältere App „Watch-Benachrichtigungstest“.",
                15, Color.DKGRAY));

        setContentView(scrolling);
        load();
    }

    private interface Changed { void onChange(); }

    private SeekBar bar(LinearLayout parent, int max, Changed callback) {
        SeekBar seek = new SeekBar(this);
        seek.setMax(max);
        parent.addView(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int value, boolean fromUser) {
                if (fromUser && !updating) callback.onChange();
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        return seek;
    }

    private void updateTime() {
        timeText.setText(String.format(Locale.GERMANY, "%02d:%02d",
                selectedHour, selectedMinute));
    }

    private void updateSliders() {
        delayText.setText("Ton erst nach " + delayBar.getProgress() * 10
                + " Sekunden starten (Watch früher)");
        rampText.setText("Ton über " + (rampBar.getProgress() + 1)
                + " Minuten langsam lauter");
        volumeText.setText("Maximale App-Lautstärke: " + (volumeBar.getProgress() + 5)
                + " % des System-Weckerpegels");
        pulsesText.setText("Watch-Benachrichtigungen: " + pulsesBar.getProgress()
                + (pulsesBar.getProgress() == 0 ? " (aus)" : " Impulse"));
        intervalText.setText("Abstand der Impulse: " + (intervalBar.getProgress() + 1) * 10 + " s");
        snoozeText.setText("Schlummern: " + (snoozeBar.getProgress() + 1) + " Minuten");
    }

    private void load() {
        updating = true;
        AlarmProfile p = AlarmRepository.get(this, selectedId);
        name.setText(p.name);
        selectedHour = p.hour;
        selectedMinute = p.minute;
        updateTime();
        for (int i=0; i<7; i++) weekdays[i].setChecked((p.days & (1<<i)) != 0);
        enabled.setChecked(p.enabled);
        delayBar.setProgress(p.delaySeconds / 10);
        rampBar.setProgress(p.rampSeconds / 60 - 1);
        volumeBar.setProgress(p.maxVolume - 5);
        pulsesBar.setProgress(p.watchPulses);
        intervalBar.setProgress(p.watchIntervalSeconds / 10 - 1);
        snoozeBar.setProgress(p.snoozeMinutes - 1);
        toneSpinner.setSelection(p.tone);
        updating = false;
        updateSliders();
        refreshStatus();
    }

    private AlarmProfile readForm() {
        int mask = 0;
        for (int i=0; i<7; i++) if (weekdays[i].isChecked()) mask |= (1<<i);
        return new AlarmProfile(selectedId, name.getText().toString(),
                enabled.isChecked(), selectedHour, selectedMinute, mask,
                delayBar.getProgress() * 10, (rampBar.getProgress()+1)*60,
                volumeBar.getProgress()+5, pulsesBar.getProgress(),
                (intervalBar.getProgress()+1)*10, snoozeBar.getProgress()+1,
                toneSpinner.getSelectedItemPosition());
    }

    private void persist(boolean preview) {
        AlarmProfile p;
        try {
            p = readForm();
        } catch (IllegalArgumentException invalid) {
            new AlertDialog.Builder(this).setMessage(
                    "Bitte mindestens einen Wochentag auswählen.").setPositiveButton("OK", null).show();
            return;
        }
        if (!AlarmScheduler.canSchedule(this)) {
            status.setText("Exakte Alarme fehlen. Bitte 'Alarme und Erinnerungen' zulassen, " +
                    "danach erneut speichern oder testen.");
            try {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.parse("package:" + getPackageName())));
            } catch (RuntimeException ignored) { }
            return;
        }
        AlarmRepository.put(this, p);
        try {
            AlarmScheduler.reschedule(this, p.id);
        } catch (RuntimeException e) {
            status.setText("FEHLER: Alarm konnte nicht geplant werden. " + e.getMessage());
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            pendingPreview = preview;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFY_PERMISSION);
            refreshStatus();
            return;
        }
        if (preview) previewNow();
        else refreshStatus();
    }

    private void previewNow() {
        try {
            long when = AlarmScheduler.test(this, selectedId);
            status.setText("Test für " + format(when) +
                    " eingeplant. Bildschirm sperren; Weckton + Uhr beobachten.");
        } catch (RuntimeException e) {
            status.setText("Test konnte nicht geplant werden: " + e.getMessage());
        }
    }

    @Override public void onRequestPermissionsResult(int request, String[] names, int[] results) {
        super.onRequestPermissionsResult(request, names, results);
        if (request != NOTIFY_PERMISSION) return;
        boolean wasPreview = pendingPreview;
        pendingPreview = false;
        if (results.length>0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            if (wasPreview) previewNow();
            else refreshStatus();
        } else {
            status.setText("Benachrichtigungen gesperrt: Handyton möglicherweise aktiv, " +
                    "Uhr-Vibration nicht verlässlich möglich.");
        }
    }

    private static String format(long when) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(when));
    }

    private void refreshStatus() {
        if (status == null) return;
        StringBuilder s = new StringBuilder();
        for (int id=1; id<=AlarmProfile.COUNT; id++) {
            AlarmProfile p = AlarmRepository.get(this, id);
            long next = AlarmRepository.next(this, id);
            s.append(p.name).append(": ")
                    .append(!p.enabled ? "aus" : next>0 ? format(next) : "NICHT GEPLANT!")
                    .append("\n");
        }
        long test = AlarmScheduler.testWhen(this);
        if (test > 0) s.append("Testwecker: ").append(format(test)).append("\n");
        s.append("Letzter Systemeintrag: ").append(AlarmRepository.lastEvent(this)).append("\n");
        if (!AlarmScheduler.canSchedule(this)) s.append("⚠ Exakte Weckerlaubnis fehlt.\n");
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm == null || !nm.areNotificationsEnabled()) {
            s.append("⚠ Benachrichtigungen sind ausgeschaltet (Watch-Vibration fehlt).\n");
        } else {
            NotificationChannel channel = nm.getNotificationChannel(RingingService.WATCH_CHANNEL);
            if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE)
                s.append("⚠ Watch-Benachrichtigungskanal wurde blockiert.\n");
        }
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audio != null && audio.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
            s.append("⚠ System-Weckerlautstärke = 0. Kein Ton hörbar.\n");
        }
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        if (power != null && power.isPowerSaveMode()) s.append("Hinweis: Energiesparmodus aktiv.\n");
        s.append("Schlafmodus/Nicht stören auf der Uhr kann die Vibration unterdrücken. ");
        s.append("Samsung-Gerätetest steht noch aus.");
        status.setText(s.toString());
    }
}
