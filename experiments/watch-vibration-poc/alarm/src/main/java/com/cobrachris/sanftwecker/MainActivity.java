package com.cobrachris.sanftwecker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import java.text.DateFormat;
import java.util.Date;

/** One weekly alarm with a very quiet start and Galaxy Watch notification pulses. */
public final class MainActivity extends Activity {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 20;
    private TimePicker timePicker;
    private final CheckBox[] weekdays = new CheckBox[7];
    private SeekBar rampBar;
    private SeekBar gainBar;
    private TextView rampLabel;
    private TextView gainLabel;
    private TextView status;
    private int requestedAction; // 1 = enable alarm, 2 = test

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        drawUi();
        refreshStatus();
    }

    @Override protected void onResume() {
        super.onResume();
        if (status != null) refreshStatus();
    }

    private void drawUi() {
        SharedPreferences prefs = AlarmScheduler.prefs(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(35, 32, 35, 50);
        layout.setBackgroundColor(Color.WHITE);
        scroll.addView(layout);

        TextView title = label("Sanftwecker (Test)", 27, Color.BLACK);
        layout.addView(title);
        TextView note = label(
                "Eigenständiger Android-Wecker. Handyton beginnt bei 0 % und steigt " +
                "langsam an. Die Galaxy Watch bekommt wiederholte Benachrichtigungen. " +
                "Noch nicht als einzigen Wecker nutzen.", 15, Color.DKGRAY);
        layout.addView(note);

        timePicker = new TimePicker(this);
        timePicker.setIs24HourView(true);
        timePicker.setHour(prefs.getInt(AlarmScheduler.HOUR, 7));
        timePicker.setMinute(prefs.getInt(AlarmScheduler.MINUTE, 0));
        layout.addView(timePicker);

        layout.addView(label("Wochentage", 19, Color.BLACK));
        final String[] days = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};
        LinearLayout dayRow = new LinearLayout(this);
        dayRow.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < days.length; i++) {
            weekdays[i] = new CheckBox(this);
            weekdays[i].setText(days[i]);
            weekdays[i].setTextSize(15);
            weekdays[i].setChecked(
                    (prefs.getInt(AlarmScheduler.DAYS, AlarmScheduler.DEFAULT_DAYS) & (1 << i)) != 0);
            dayRow.addView(weekdays[i]);
        }
        layout.addView(dayRow);

        rampLabel = label("", 18, Color.BLACK);
        layout.addView(rampLabel);
        rampBar = new SeekBar(this);
        rampBar.setMax(14);
        rampBar.setProgress(Math.max(0,
                Math.min(14, prefs.getInt(AlarmScheduler.RAMP, 300) / 60 - 1)));
        layout.addView(rampBar);
        rampBar.setOnSeekBarChangeListener(seekListener(this::updateSliders));

        gainLabel = label("", 18, Color.BLACK);
        layout.addView(gainLabel);
        gainBar = new SeekBar(this);
        gainBar.setMax(90);
        gainBar.setProgress(Math.max(0,
                Math.min(90, prefs.getInt(AlarmScheduler.MAX_VOLUME, 35) - 10)));
        layout.addView(gainBar);
        gainBar.setOnSeekBarChangeListener(seekListener(this::updateSliders));
        updateSliders();

        Button enable = button("Wecker speichern und aktivieren", layout);
        enable.setOnClickListener(v -> checkPermissionsAndRun(1));

        Button disable = button("Regelmäßigen Wecker deaktivieren", layout);
        disable.setOnClickListener(v -> {
            AlarmScheduler.prefs(this).edit().putBoolean(AlarmScheduler.ENABLED, false).apply();
            AlarmScheduler.cancelDaily(this);
            refreshStatus();
        });

        Button preview = button("Testwecker in 30 Sekunden", layout);
        preview.setOnClickListener(v -> checkPermissionsAndRun(2));
        Button cancelPreview = button("Geplanten Test abbrechen", layout);
        cancelPreview.setOnClickListener(v -> {
            AlarmScheduler.cancelPreview(this);
            refreshStatus();
        });

        Button stop = button("Klingeln jetzt stoppen", layout);
        stop.setOnClickListener(v -> {
            stopRinging(RingingService.STOP);
            refreshStatus();
        });
        Button snooze = button("5 Minuten schlummern", layout);
        snooze.setOnClickListener(v -> stopRinging(RingingService.SNOOZE));

        status = label("", 15, Color.DKGRAY);
        layout.addView(status);
        setContentView(scroll);
    }

    private interface Changed { void run(); }
    private SeekBar.OnSeekBarChangeListener seekListener(Changed callback) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                callback.run();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        };
    }

    private void updateSliders() {
        if (rampBar != null && rampLabel != null)
            rampLabel.setText("Lautstärkeanstieg: " + (rampBar.getProgress() + 1) + " Minuten");
        if (gainBar != null && gainLabel != null)
            gainLabel.setText("Max. App-Lautstärke: " + (gainBar.getProgress() + 10) +
                    "% (abhängig von der System-Weckerlautstärke)");
    }

    private TextView label(String text, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setPadding(0, 14, 0, 14);
        return view;
    }

    private Button button(String title, LinearLayout parent) {
        Button button = new Button(this);
        button.setText(title);
        parent.addView(button, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return button;
    }

    private void checkPermissionsAndRun(int operation) {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED) {
            requestedAction = operation;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
            return;
        }
        if (!AlarmScheduler.canSchedule(this)) {
            status.setText("Bitte einmalig 'Alarme und Erinnerungen' für " +
                    "'Sanftwecker (Test)' erlauben. Danach erneut auf den Knopf tippen.");
            try {
                Intent request = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                request.setData(Uri.parse("package:" + getPackageName()));
                startActivity(request);
            } catch (RuntimeException e) {
                status.setText("In Android Einstellungen → Apps → Spezieller Zugriff → " +
                        "Alarme und Erinnerungen für Sanftwecker (Test) erlauben.");
            }
            return;
        }
        if (operation == 1) enableDaily();
        else if (operation == 2) runPreview();
    }

    private void enableDaily() {
        int mask = 0;
        for (int i = 0; i < 7; i++) if (weekdays[i].isChecked()) mask |= (1 << i);
        if (mask == 0) {
            status.setText("Bitte mindestens einen Wochentag auswählen.");
            return;
        }
        SharedPreferences.Editor editor = AlarmScheduler.prefs(this).edit();
        editor.putInt(AlarmScheduler.HOUR, timePicker.getHour())
                .putInt(AlarmScheduler.MINUTE, timePicker.getMinute())
                .putInt(AlarmScheduler.DAYS, mask)
                .putInt(AlarmScheduler.RAMP, (rampBar.getProgress() + 1) * 60)
                .putInt(AlarmScheduler.MAX_VOLUME, gainBar.getProgress() + 10)
                .putBoolean(AlarmScheduler.ENABLED, true).apply();
        try {
            AlarmScheduler.scheduleDaily(this);
            refreshStatus();
        } catch (RuntimeException e) {
            status.setText("Alarm konnte nicht eingeplant werden: " + e.getMessage());
        }
    }

    private void runPreview() {
        // Preview uses the sliders too, but does not enable the weekly schedule.
        AlarmScheduler.prefs(this).edit()
                .putInt(AlarmScheduler.RAMP, (rampBar.getProgress() + 1) * 60)
                .putInt(AlarmScheduler.MAX_VOLUME, gainBar.getProgress() + 10)
                .apply();
        try {
            long when = AlarmScheduler.schedulePreview(this);
            status.setText("Test für " + time(when) +
                    " geplant. Bildschirm sperren; später über die Benachrichtigung stoppen.");
        } catch (RuntimeException e) {
            status.setText("Test konnte nicht eingeplant werden: " + e.getMessage());
        }
    }

    private void stopRinging(String action) {
        // Explicit user action while Activity is foregrounded.
        startService(new Intent(this, RingingService.class).setAction(action));
    }

    private static String time(long millis) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(millis));
    }

    private void refreshStatus() {
        if (status == null) return;
        SharedPreferences p = AlarmScheduler.prefs(this);
        boolean enabled = p.getBoolean(AlarmScheduler.ENABLED, false);
        long next = p.getLong(AlarmScheduler.NEXT, 0L);
        long preview = p.getLong(AlarmScheduler.PREVIEW_NEXT, 0L);
        String message = "Regelmäßiger Wecker: " + (enabled ? "aktiviert" : "deaktiviert") +
                "\nNächster Alarm: " + (next == 0 ? "nicht geplant" : time(next)) +
                "\nTestalarm: " + (preview == 0 ? "keiner" : time(preview));
        if (enabled && !AlarmScheduler.canSchedule(this))
            message += "\nAchtung: Exakte Alarmberechtigung fehlt!";
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audio != null && audio.getStreamVolume(AudioManager.STREAM_ALARM) == 0)
            message += "\nAchtung: Android-System-Weckerlautstärke ist 0. Kein Ton hörbar!";
        message += "\nGalaxy Wearable: 'Sanftwecker (Test)' bei App-Benachrichtigungen aktivieren.";
        status.setText(message);
    }

    @Override public void onRequestPermissionsResult(int code, String[] names, int[] results) {
        super.onRequestPermissionsResult(code, names, results);
        if (code != NOTIFICATION_PERMISSION_REQUEST) return;
        int requested = requestedAction;
        requestedAction = 0;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED)
            checkPermissionsAndRun(requested);
        else
            status.setText("Benachrichtigungen fehlen – Watch-Vibration wäre nicht möglich. " +
                    "Bitte in Android für Sanftwecker (Test) erlauben.");
    }
}
