package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Safe companion setup for the official Saphe Link app.
 *
 * No unofficial Saphe SDK, cloud API, Bluetooth pairing, notification scraping,
 * background app launch, false live-connection status or radar-alert automation.
 * The real Saphe device remains controlled exclusively by Saphe Link.
 */
public final class SapheSetupActivity extends Activity {
    private static final String PLAY_STORE =
        "https://play.google.com/store/apps/details?id=my.saphelink";
    private static final String OFFICIAL_HELP =
        "https://help.saphe.com/hc/de/articles/36700091104541-"
        + "So-stellst-du-sicher-dass-sich-dein-Saphe-automatisch-verbindet";

    private TextView appStatus;
    private Button openApp;
    private Button appSettings;
    private Button playStore;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout root = Ui.column(this);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 20),
            Ui.dp(this, 18), Ui.dp(this, 32));
        scroll.addView(root);
        setContentView(scroll);

        root.addView(Ui.text(this, "Saphe Link · Einrichtung", 23, Ui.FG, true));
        appStatus = Ui.text(this, "", 15, Ui.FG, true);
        root.addView(appStatus);

        root.addView(Ui.text(this,
            "FahrCockpit kann feststellen, ob Saphe Link installiert ist. "
            + "Eine aktive Bluetooth-Verbindung, den Gerätestatus und laufende "
            + "Saphe-Warnungen kann es nicht zuverlässig auslesen.",
            14, Ui.MUTED, false));

        openApp = Ui.button(this, "Saphe Link öffnen");
        root.addView(openApp);
        openApp.setOnClickListener(v -> {
            if (!WarningSource.openSaphe(this))
                Toast.makeText(this, "Saphe Link konnte nicht geöffnet werden.",
                    Toast.LENGTH_LONG).show();
        });

        appSettings = Ui.button(this, "Android-Einstellungen für Saphe Link");
        root.addView(appSettings);
        appSettings.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + WarningSource.SAPHE_PACKAGE)));
            } catch (RuntimeException unavailable) {
                Toast.makeText(this, "App-Einstellungen nicht verfügbar.",
                    Toast.LENGTH_LONG).show();
            }
        });

        playStore = Ui.button(this, "Saphe Link im Play Store ansehen");
        root.addView(playStore);
        playStore.setOnClickListener(v -> openExternal(PLAY_STORE));

        Ui.section(this, root, "Verbindung in Saphe Link prüfen",
            "1. Bluetooth am Handy einschalten.\n"
            + "2. Saphe Link öffnen und das Saphe-Gerät dort verbinden "
            + "(nicht über die normalen Android-Bluetooth-Einstellungen).\n"
            + "3. In Android → Einstellungen → Apps → Saphe Link → "
            + "Berechtigungen die Standortberechtigung kontrollieren. "
            + "Laut Saphe wird für die Hintergrundverbindung 'Immer' benötigt.\n"
            + "4. Den tatsächlichen Gerätestatus in Saphe Link oder am "
            + "Gerät überprüfen. FahrCockpit zeigt keinen erfundenen Status an.");

        Button help = Ui.button(this, "Offizielle Saphe-Hilfe öffnen");
        root.addView(help);
        help.setOnClickListener(v -> openExternal(OFFICIAL_HELP));

        Ui.section(this, root, "Deutschland: rechtlicher Hinweis",
            "Als Fahrzeugführer darfst du während der Fahrt keine "
            + "Blitzerwarnfunktion verwenden (§ 23 Abs. 1c StVO). "
            + "Saphe und seine Warnfunktionen werden durch FahrCockpit "
            + "weder automatisch gestartet noch gesteuert.");

        Button back = Ui.button(this, "Zurück zu FahrCockpit");
        root.addView(back);
        back.setOnClickListener(v -> finish());
    }

    @Override protected void onResume() {
        super.onResume();
        // Reflect only installation status. Never claim an active device link.
        boolean installed = WarningSource.isSapheInstalled(this);
        appStatus.setText(installed
            ? "Saphe Link ist installiert · Geräteverbindung nicht auslesbar"
            : "Saphe Link ist nicht installiert");
        openApp.setEnabled(installed);
        appSettings.setEnabled(installed);
        playStore.setEnabled(true);
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (RuntimeException unavailable) {
            Toast.makeText(this, "Webseite konnte nicht geöffnet werden.",
                Toast.LENGTH_LONG).show();
        }
    }
}
