package com.cobrachris.fahrcockpit;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final String PREFS = "fahrcockpit_blitzer_v1";
    private static final String KEY_HAS_REQUEST = "has_request";
    private static final String KEY_LAST_START_REQUEST = "last_start_requested";
    private SharedPreferences prefs;
    private Switch blitzerSwitch;
    private TextView blitzerStatus;
    private TextView permissions;
    private TextView warningStatus;
    private boolean programmatic;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout root = Ui.column(this);
        root.setPadding(Ui.dp(this, 22), Ui.dp(this, 20),
            Ui.dp(this, 22), Ui.dp(this, 40));
        scroll.addView(root);
        setContentView(scroll);
        getWindow().getDecorView().setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars =
                insets.getInsets(android.view.WindowInsets.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        root.addView(Ui.text(this, "FahrCockpit", 30, Ui.FG, true));
        root.addView(Ui.text(this,
            "Google Maps bleibt deine Navigation. Wähle die echte geteilte " +
            "Fahransicht oder das bisherige schwebende Cockpit.",
            15, Ui.MUTED, false));

        Ui.section(this, root, "70/30 Fahransicht mit Google Maps",
            "Echter Android-Splitscreen statt Karte unter einem großen Overlay. " +
            "Google Maps bleibt die Navigations-App. Die Trennlinie legt Samsung fest.");
        Button split = Ui.button(this, "70/30-Fahransicht starten");
        root.addView(split);
        split.setOnClickListener(v -> {
            try {
                // Avoid a stale overlay being visible during the split transition.
                stopService(new Intent(this, OverlayService.class));
                startActivity(new Intent(this, SplitCockpitActivity.class));
            } catch (RuntimeException e) {
                permissions.setText("Split-Modus konnte nicht gestartet werden. " +
                    "Nutze die schwebende Ansicht oder Samsung Multi Window.");
            }
        });
        root.addView(Ui.text(this,
            "FahrCockpit versucht, Google Maps direkt daneben zu starten. " +
            "Wenn Samsung die Fenster nicht automatisch teilt, nutze " +
            "einmalig die App-Paar-Funktion. Das genaue 70/30-Verhältnis " +
            "stellst du an der System-Trennlinie ein.",
            12, Ui.MUTED, false));

        Ui.section(this, root, "Schwebendes Cockpit",
            "Spotify • Tankstellen • Warn-App (Saphe oder Blitzer.de)");
        Button overlay = Ui.button(this, "Cockpit über Google Maps starten");
        root.addView(overlay);
        overlay.setOnClickListener(v -> startOverlay());
        Switch openMapsAtStart = new Switch(this);
        openMapsAtStart.setText("Google Maps beim Cockpit-Start öffnen");
        openMapsAtStart.setTextColor(Ui.FG);
        openMapsAtStart.setTextSize(14);
        openMapsAtStart.setChecked(getSharedPreferences(CockpitViewRules.DISPLAY_PREFS,
            MODE_PRIVATE).getBoolean(CockpitViewRules.KEY_OPEN_MAPS, true));
        openMapsAtStart.setOnCheckedChangeListener((button, checked) ->
            getSharedPreferences(CockpitViewRules.DISPLAY_PREFS, MODE_PRIVATE).edit()
                .putBoolean(CockpitViewRules.KEY_OPEN_MAPS, checked).apply());
        root.addView(openMapsAtStart);
        Switch autoLand = new Switch(this);
        autoLand.setText("Querformat: automatisch kompakter Fahrmodus");
        autoLand.setTextColor(Ui.FG);
        autoLand.setTextSize(14);
        autoLand.setChecked(getSharedPreferences(CockpitViewRules.DISPLAY_PREFS,
            MODE_PRIVATE).getBoolean(CockpitViewRules.KEY_AUTO_DRIVING, true));
        autoLand.setOnCheckedChangeListener((button, checked) ->
            getSharedPreferences(CockpitViewRules.DISPLAY_PREFS, MODE_PRIVATE).edit()
                .putBoolean(CockpitViewRules.KEY_AUTO_DRIVING, checked).apply());
        root.addView(autoLand);
        root.addView(Ui.text(this,
            "Kartenstart AUS verhindert nur das automatische Öffnen. Eine bereits " +
            "geöffnete Google-Maps-App wird nicht geschlossen oder verkleinert. " +
            "Im Querformat kannst du den Fahrmodus jederzeit manuell wechseln.",
            12, Ui.MUTED, false));
        Button stopOverlay = Ui.button(this, "Schwebendes Cockpit schließen");
        root.addView(stopOverlay);
        stopOverlay.setOnClickListener(v -> stopService(
            new Intent(this, OverlayService.class)));

        permissions = Ui.text(this, "", 14, Ui.MUTED, false);
        root.addView(permissions);
        Button overlayPermission = Ui.button(this, "Einblendung freigeben");
        overlayPermission.setOnClickListener(v -> requestOverlayPermission());
        root.addView(overlayPermission);
        Button notificationPermission = Ui.button(this, "Spotify-Medienzugriff freigeben");
        notificationPermission.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } catch (RuntimeException e) {
                permissions.setText("Medienzugriff in Android-Einstellungen manuell freigeben.");
            }
        });
        root.addView(notificationPermission);

        Ui.section(this, root, "Warnsystem auswählen",
            "Saphe ist voreingestellt. Die Auswahl startet oder stoppt keine Warn-App und " +
            "ändert weder dein Gerät noch dessen Abonnement.");
        android.widget.RadioGroup warningChoice = new android.widget.RadioGroup(this);
        android.widget.RadioButton sapheChoice = new android.widget.RadioButton(this);
        sapheChoice.setId(android.view.View.generateViewId());
        sapheChoice.setText("Saphe Link (vorhandenes Gerät)");
        sapheChoice.setTextColor(Ui.FG);
        warningChoice.addView(sapheChoice);
        android.widget.RadioButton blitzerChoice = new android.widget.RadioButton(this);
        blitzerChoice.setId(android.view.View.generateViewId());
        blitzerChoice.setText("Blitzer.de PRO (optional)");
        blitzerChoice.setTextColor(Ui.FG);
        warningChoice.addView(blitzerChoice);
        android.widget.RadioButton noneChoice = new android.widget.RadioButton(this);
        noneChoice.setId(android.view.View.generateViewId());
        noneChoice.setText("Kein Warnsystem");
        noneChoice.setTextColor(Ui.FG);
        warningChoice.addView(noneChoice);
        root.addView(warningChoice);
        String selected = WarningSource.selected(this);
        warningChoice.check(WarningSource.BLITZER.equals(selected) ? blitzerChoice.getId()
            : WarningSource.NONE.equals(selected) ? noneChoice.getId() : sapheChoice.getId());
        warningStatus = Ui.text(this, "", 14, Ui.MUTED, false);
        root.addView(warningStatus);
        Button sapheLaunch = Ui.button(this, "Saphe Link öffnen");
        root.addView(sapheLaunch);
        sapheLaunch.setOnClickListener(v -> warningStatus.setText(
            WarningSource.openSaphe(this) ? "Saphe Link geöffnet – Warnstatus wird von Saphe verwaltet."
                : "Saphe Link ist auf diesem Handy nicht installiert."));
        warningChoice.setOnCheckedChangeListener((group, checkedId) -> {
            WarningSource.setSelected(this, checkedId == blitzerChoice.getId()
                ? WarningSource.BLITZER : checkedId == noneChoice.getId()
                ? WarningSource.NONE : WarningSource.SAPHE);
            updateWarningStatus(warningStatus);
        });
        updateWarningStatus(warningStatus);
        root.addView(Ui.text(this,
            "Saphe lässt sich aktuell nur öffnen. Das FahrCockpit kontrolliert " +
            "weder dein Saphe-Gerät noch dessen Warnstatus. Das Gerätemodell " +
            "prüfen wir nach deiner Rückkehr nach Deutschland.",
            13, Ui.MUTED, false));

        Ui.section(this, root, "Blitzer.de PRO (optional)",
            "Diese Funktionen benötigen die separat installierte Blitzer.de PRO-App. " +
            "Sie funktionieren nicht über Saphe.");
        blitzerSwitch = new Switch(this);
        blitzerSwitch.setText("Zuletzt angeforderter Zustand: EIN / AUS");
        blitzerSwitch.setTextColor(Ui.FG);
        blitzerSwitch.setTextSize(16);
        blitzerSwitch.setChecked(prefs.getBoolean(KEY_LAST_START_REQUEST, false));
        root.addView(blitzerSwitch);
        blitzerStatus = Ui.text(this, "", 14, Ui.WARN, false);
        root.addView(blitzerStatus);

        Button stop = Ui.button(this, "Blitzer.de jetzt beenden");
        root.addView(stop);
        stop.setOnClickListener(v -> blitzer(false));
        Button widget = Ui.button(this, "Blitzer.de Mini-App öffnen");
        root.addView(widget);
        widget.setOnClickListener(v -> blitzerMode(BlitzerContract.Command.WIDGET));
        Button background = Ui.button(this, "Blitzer.de in den Hintergrund");
        root.addView(background);
        background.setOnClickListener(v -> blitzerMode(BlitzerContract.Command.BACKGROUND));
        blitzerSwitch.setOnCheckedChangeListener((button, enabled) -> {
            if (!programmatic) blitzer(enabled);
        });
        Button blitzerSettings = Ui.button(this, "Blitzer.de-Berechtigungen");
        root.addView(blitzerSettings);
        blitzerSettings.setOnClickListener(v -> {
            if (!BlitzerBridge.installed(this)) {
                blitzerStatus.setText("Blitzer.de PRO ist nicht installiert.");
                return;
            }
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + BlitzerContract.PACKAGE)));
            } catch (RuntimeException e) {
                blitzerStatus.setText("Einstellungen konnten nicht geöffnet werden.");
            }
        });
        root.addView(Ui.text(this,
            "Wichtig: Ein gesendeter Befehl bestätigt keinen aktiven Blitzer.de-Zustand. " +
            "EIN wird beim Öffnen dieser App nicht automatisch erneut gesendet.",
            13, Ui.MUTED, false));

        Ui.section(this, root, "Tankstellen und aktuelle Preise",
            "Für Deutschland: E5, E10 und Diesel von Tankerkönig. " +
            "Ein kostenfreier persönlicher API-Key ist erforderlich.");
        Button fuel = Ui.button(this, "Tankstellen & Preise öffnen");
        root.addView(fuel);
        fuel.setOnClickListener(v -> startActivity(new Intent(this, FuelActivity.class)));

        Ui.section(this, root, "Navigation und Musik",
            "Google Maps wird nicht ersetzt. Spotify läuft über die Android-Mediensteuerung.");
        Button maps = Ui.button(this, "Google Maps öffnen");
        root.addView(maps);
        maps.setOnClickListener(v -> openPackage("com.google.android.apps.maps", "Google Maps"));
        Button spotify = Ui.button(this, "Spotify öffnen");
        root.addView(spotify);
        spotify.setOnClickListener(v -> openPackage("com.spotify.music", "Spotify"));

        Ui.section(this, root, "App aktualisieren",
            "Direkte, dauerhaft signierte APK über GitHub Releases – ohne ZIP.");
        Button updateButton = Ui.button(this, "Auf Updates prüfen");
        root.addView(updateButton);
        updateButton.setOnClickListener(v ->
            startActivity(new Intent(this, UpdateActivity.class)));

        Ui.section(this, root, "Hinweise", "");
        root.addView(Ui.text(this,
            "FahrCockpit startet keine Dienste beim Booten. Schließe das Cockpit über " +
            "die App, die Schwebefläche oder die Android-Benachrichtigung. " +
            "Android-Einstellungen und Berechtigungen bleiben bei regulären Updates erhalten.",
            13, Ui.MUTED, false));
        root.addView(Ui.text(this,
            "Deutschland: Aktive Blitzerwarnungen dürfen durch Fahrzeugführende " +
            "während der Fahrt nicht verwendet werden (§ 23 Abs. 1c StVO). " +
            "Nur im Stand bedienen. Andere Länder haben eigene Regeln.",
            13, Ui.WARN, false));

        blitzerStatus.setText(prefs.getBoolean(KEY_HAS_REQUEST, false)
            ? "Zuletzt " + (blitzerSwitch.isChecked() ? "EIN" : "AUS") +
              " angefordert – tatsächlicher Zustand unbekannt."
            : "Standard: AUS. Noch kein Befehl gesendet.");
    }

    @Override public void onResume() {
        super.onResume();
        if (warningStatus != null) updateWarningStatus(warningStatus);
        if (blitzerSwitch != null) {
            programmatic = true;
            blitzerSwitch.setChecked(prefs.getBoolean(KEY_LAST_START_REQUEST, false));
            programmatic = false;
            if (blitzerStatus != null && prefs.getBoolean(KEY_HAS_REQUEST, false)) {
                blitzerStatus.setText("Zuletzt " + (blitzerSwitch.isChecked() ? "EIN" : "AUS") +
                    " angefordert – tatsächlicher Zustand unbekannt.");
            }
        }
        if (permissions != null) {
            boolean draw = Settings.canDrawOverlays(this);
            boolean listener = Permissions.isMediaListenerEnabled(this);
            permissions.setText("Einblendung: " + (draw ? "freigegeben" : "fehlt") +
                "  •  Spotify-Medienzugriff: " + (listener ? "freigegeben" : "fehlt"));
        }
    }

    private void updateWarningStatus(TextView label) {
        String chosen = WarningSource.selected(this);
        if (WarningSource.SAPHE.equals(chosen)) {
            label.setText(WarningSource.isSapheInstalled(this)
                ? "Saphe ausgewählt · App installiert · Gerätestatus unbekannt."
                : "Saphe ausgewählt · Saphe Link noch nicht installiert.");
        } else if (WarningSource.BLITZER.equals(chosen)) {
            label.setText(BlitzerBridge.installed(this)
                ? "Blitzer.de PRO ausgewählt · App installiert · Warnstatus unbekannt."
                : "Blitzer.de PRO ausgewählt · App fehlt.");
        } else {
            label.setText("Kein Cockpit-Warnsystem gewählt. Bereits laufende externe Apps werden dadurch nicht beendet.");
        }
    }

    private void requestOverlayPermission() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())));
        } catch (RuntimeException e) {
            permissions.setText("Overlay-Berechtigung bitte unter Android > Apps freigeben.");
        }
    }

    private void startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, 101);
            return; // Do not cover the Android permission dialog with Google Maps.
        }
        showCockpitAndMaps();
    }

    @Override public void onRequestPermissionsResult(
        int requestCode, String[] names, int[] grants) {
        super.onRequestPermissionsResult(requestCode, names, grants);
        if (requestCode == 101) {
            // Notifications may be denied. Android still permits an explicitly
            // user-started foreground service, but the app cannot post alerts.
            showCockpitAndMaps();
        }
    }

    private void showCockpitAndMaps() {
        try {
            startForegroundService(new Intent(this, OverlayService.class)
                .setAction(OverlayService.ACTION_SHOW));
            if (getSharedPreferences(CockpitViewRules.DISPLAY_PREFS, MODE_PRIVATE)
                .getBoolean(CockpitViewRules.KEY_OPEN_MAPS, true)) {
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                    () -> openPackage("com.google.android.apps.maps", "Google Maps"), 450);
            }
        } catch (RuntimeException e) {
            permissions.setText("Cockpit konnte nicht gestartet werden: " +
                "Android-Berechtigungen und Akku-Einstellungen prüfen.");
        }
    }

    private void blitzer(boolean enabled) {
        BlitzerBridge.SendResult result = BlitzerBridge.send(this,
            enabled ? BlitzerContract.Command.START : BlitzerContract.Command.STOP);
        if (result == BlitzerBridge.SendResult.SENT_UNCONFIRMED) {
            prefs.edit().putBoolean(KEY_HAS_REQUEST, true)
                .putBoolean(KEY_LAST_START_REQUEST, enabled).apply();
            programmatic = true;
            blitzerSwitch.setChecked(enabled);
            programmatic = false;
            blitzerStatus.setText(enabled
                ? "Startbefehl gesendet – Blitzer.de-Status nicht bestätigt."
                : "Beenden-Befehl gesendet – Blitzer.de-Status nicht bestätigt.");
        } else {
            programmatic = true;
            blitzerSwitch.setChecked(prefs.getBoolean(KEY_LAST_START_REQUEST, false));
            programmatic = false;
            blitzerStatus.setText(result == BlitzerBridge.SendResult.NOT_INSTALLED
                ? "Blitzer.de PRO fehlt oder ist nicht sichtbar."
                : "Befehl konnte nicht gesendet werden.");
        }
    }

    private void blitzerMode(BlitzerContract.Command mode) {
        BlitzerBridge.SendResult result = BlitzerBridge.send(this, mode);
        if (result == BlitzerBridge.SendResult.SENT_UNCONFIRMED) {
            programmatic = true;
            blitzerSwitch.setChecked(true);
            programmatic = false;
        }
        blitzerStatus.setText(result == BlitzerBridge.SendResult.SENT_UNCONFIRMED
            ? "Modus-Befehl gesendet – keine Empfangsbestätigung."
            : "Blitzer.de PRO fehlt oder Befehl wurde blockiert.");
    }

    private void openPackage(String packageName, String label) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch == null) {
            permissions.setText(label + " nicht installiert oder nicht sichtbar.");
            return;
        }
        try {
            startActivity(launch);
        } catch (RuntimeException e) {
            permissions.setText(label + " konnte nicht geöffnet werden.");
        }
    }
}
