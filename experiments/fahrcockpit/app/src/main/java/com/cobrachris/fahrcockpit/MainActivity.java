package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

/**
 * First native FahrCockpit slice: a deliberate Blitzer.de PRO START/STOP switch
 * and separate mode commands. The active running state cannot be read back.
 */
public final class MainActivity extends Activity {
    private static final String PREFS = "fahrcockpit_blitzer_v1";
    private static final String KEY_HAS_REQUEST = "has_request";
    private static final String KEY_LAST_START_REQUEST = "last_start_requested";
    private static final int BG = Color.rgb(16, 19, 25);
    private static final int FG = Color.rgb(245, 246, 249);
    private static final int MUTED = Color.rgb(177, 181, 190);
    private static final int ACCENT = Color.rgb(255, 196, 98);

    private SharedPreferences prefs;
    private Switch enabledSwitch;
    private Button backgroundButton;
    private Button widgetButton;
    private TextView status;
    private boolean changingProgrammatically;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(24), dp(22), dp(36));
        scroll.addView(root);
        setContentView(scroll);

        // Android 15/16 edge-to-edge: keep controls outside system bars.
        getWindow().getDecorView().setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        root.addView(text("FahrCockpit", 30, FG, true));
        root.addView(text("Erster Android-Test: Blitzer.de PRO steuern", 16, MUTED, false));
        root.addView(text("Blitzer.de PRO", 22, FG, true));

        enabledSwitch = new Switch(this);
        enabledSwitch.setText("Blitzerwarnungen EIN / AUS");
        enabledSwitch.setTextColor(FG);
        enabledSwitch.setTextSize(17);
        enabledSwitch.setPadding(0, dp(16), 0, dp(16));
        enabledSwitch.setChecked(prefs.getBoolean(KEY_LAST_START_REQUEST, false));
        root.addView(enabledSwitch);

        status = text("", 15, ACCENT, false);
        root.addView(status);

        // Stop works even if Blitzer.de was started independently of FahrCockpit.
        Button stopNow = button("Blitzer.de jetzt beenden");
        root.addView(stopNow);

        root.addView(text("Darstellung (nur nach Startbefehl)", 17, FG, true));
        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        backgroundButton = button("Hintergrund");
        widgetButton = button("Mini-App");
        modes.addView(backgroundButton,
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        modes.addView(widgetButton,
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(modes);

        root.addView(text(
            "EIN sendet den dokumentierten START-Befehl. Mit 'Hintergrund' oder 'Mini-App' " +
            "forderst du anschließend den jeweiligen Blitzer.de-Modus an. " +
            "AUS sendet den dokumentierten Beenden-Befehl.",
            14, MUTED, false));

        root.addView(text("Andere Apps", 20, FG, true));
        Button maps = button("Google Maps öffnen");
        root.addView(maps);
        Button settings = button("Blitzer.de-Berechtigungen öffnen");
        root.addView(settings);

        root.addView(text(
            "Der Schalter zeigt nur deinen letzten angeforderten Zustand. " +
            "Blitzer.de bestätigt den tatsächlichen Betriebszustand nicht. " +
            "Ein Neustart von FahrCockpit löst keine Befehle aus.",
            14, MUTED, false));

        root.addView(text(
            "Deutschland: Die Benutzung aktiver Blitzerwarnfunktionen durch " +
            "Fahrzeugführende ist nach § 23 Abs. 1c StVO verboten. " +
            "Vor der Fahrt ausschalten. In anderen Ländern gelten deren Regeln.",
            14, ACCENT, false));

        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!changingProgrammatically) {
                sendMainCommand(isChecked);
            }
        });
        stopNow.setOnClickListener(v -> sendMainCommand(false));
        backgroundButton.setOnClickListener(v ->
            sendMode(BlitzerContract.Command.BACKGROUND, "Hintergrund"));
        widgetButton.setOnClickListener(v ->
            sendMode(BlitzerContract.Command.WIDGET, "Mini-App"));
        maps.setOnClickListener(v -> openMaps());
        settings.setOnClickListener(v -> openBlitzerSettings());

        restoreDisplay();
    }

    private void sendMainCommand(boolean requestOn) {
        BlitzerContract.Command command = requestOn
            ? BlitzerContract.Command.START : BlitzerContract.Command.STOP;
        BlitzerBridge.SendResult result = BlitzerBridge.send(this, command);
        if (result != BlitzerBridge.SendResult.SENT_UNCONFIRMED) {
            changingProgrammatically = true;
            enabledSwitch.setChecked(prefs.getBoolean(KEY_LAST_START_REQUEST, false));
            changingProgrammatically = false;
            status.setText(errorText(result));
            return;
        }

        prefs.edit()
            .putBoolean(KEY_HAS_REQUEST, true)
            .putBoolean(KEY_LAST_START_REQUEST, requestOn)
            .apply();
        changingProgrammatically = true;
        enabledSwitch.setChecked(requestOn);
        changingProgrammatically = false;
        status.setText(requestOn
            ? "START-Befehl gesendet – tatsächlichen Status in Blitzer.de prüfen."
            : "BEENDEN-Befehl gesendet – tatsächlichen Status in Blitzer.de prüfen.");
        refreshButtons();
    }

    private void sendMode(BlitzerContract.Command command, String label) {
        if (!enabledSwitch.isChecked()) {
            status.setText("Zuerst EIN wählen.");
            return;
        }
        BlitzerBridge.SendResult result = BlitzerBridge.send(this, command);
        status.setText(result == BlitzerBridge.SendResult.SENT_UNCONFIRMED
            ? label + "-Befehl gesendet – Status nicht bestätigt."
            : errorText(result));
    }

    private void restoreDisplay() {
        if (!prefs.getBoolean(KEY_HAS_REQUEST, false)) {
            status.setText("Standard: AUS. Noch kein Befehl gesendet.");
        } else {
            status.setText(enabledSwitch.isChecked()
                ? "Zuletzt START angefordert. Kein automatischer Neustart; Status unbestätigt."
                : "Zuletzt BEENDEN angefordert. Status unbestätigt.");
        }
        refreshButtons();
    }

    private void refreshButtons() {
        boolean requestedOn = enabledSwitch.isChecked();
        backgroundButton.setEnabled(requestedOn);
        widgetButton.setEnabled(requestedOn);
    }

    private String errorText(BlitzerBridge.SendResult result) {
        return result == BlitzerBridge.SendResult.NOT_INSTALLED
            ? "Blitzer.de PRO ist nicht installiert oder nicht sichtbar."
            : "Befehl konnte nicht gesendet werden. Berechtigungen und App prüfen.";
    }

    private void openMaps() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(
            "com.google.android.apps.maps");
        if (launch == null) {
            status.setText("Google Maps ist nicht installiert oder nicht sichtbar.");
            return;
        }
        try {
            startActivity(launch);
        } catch (RuntimeException e) {
            status.setText("Google Maps konnte nicht geöffnet werden.");
        }
    }

    private void openBlitzerSettings() {
        if (!BlitzerBridge.installed(this)) {
            status.setText("Zuerst Blitzer.de PRO installieren.");
            return;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + BlitzerContract.PACKAGE));
            startActivity(intent);
        } catch (RuntimeException e) {
            status.setText("Die Android-App-Einstellungen konnten nicht geöffnet werden.");
        }
    }

    private TextView text(String value, int sp, int color, boolean heading) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(sp);
        if (heading) {
            view.setTypeface(null, Typeface.BOLD);
        }
        view.setPadding(0, dp(12), 0, dp(12));
        return view;
    }

    private Button button(String label) {
        Button view = new Button(this);
        view.setText(label);
        view.setAllCaps(false);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
