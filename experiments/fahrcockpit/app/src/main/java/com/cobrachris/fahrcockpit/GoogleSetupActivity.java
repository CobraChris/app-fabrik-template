package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Google-only onboarding for the ORIGINAL app/signing identity.
 * The Android restricted SDK key is never sent to ChatGPT, GitHub or logs.
 * Saving the key locally does not itself create a billable Navigation Request.
 */
public final class GoogleSetupActivity extends Activity {
    private EditText keyInput;
    private TextView message;
    private Button launch;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!BuildConfig.GOOGLE_ONLY) {
            finish();
            return;
        }
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setBackgroundColor(Ui.BG);
        LinearLayout root = Ui.column(this);
        root.setPadding(Ui.dp(this,24),Ui.dp(this,20),
            Ui.dp(this,24),Ui.dp(this,24));
        scroller.addView(root);
        setContentView(scroller);
        root.addView(Ui.text(this,"FahrCockpit · Google",26,Ui.FG,true));
        root.addView(Ui.text(this,
            "70 % echte Google-Navigation, 30 % Spotify. "
            + "Die Spotify-Seite ist im Querformat frei wählbar. "
            + "Eine einzige integrierte App ohne verschiebbare Trennlinie.",
            15,Ui.MUTED,false));
        Ui.section(this,root,"Einmalige Google-SDK-Einrichtung",
            "Der bereits in Google Cloud auf com.cobrachris.fahrcockpit "
            + "und das bestehende Originalzertifikat begrenzte "
            + "Navigation-SDK-Schlüssel wird nur auf deinem Android-Handy "
            + "gespeichert (Android Keystore, AES-GCM). "
            + "Bitte nicht in ChatGPT oder GitHub einfügen.");

        keyInput = new EditText(this);
        keyInput.setHint("Eingeschränkten Google-API-Schlüssel einfügen");
        keyInput.setSingleLine(true);
        keyInput.setTextColor(Ui.FG);
        keyInput.setHintTextColor(Ui.MUTED);
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(keyInput);
        Button save=Ui.button(this,"Schlüssel lokal verschlüsselt speichern");
        root.addView(save);
        message=Ui.text(this,"Keine Navigationsanfrage gestartet.",
            13,Ui.WARN,false);
        root.addView(message);
        save.setOnClickListener(v -> {
            String value=keyInput.getText().toString().trim();
            boolean ok=NavigationSdkKeyStore.save(this,value);
            keyInput.getText().clear();
            if (!ok) {
                message.setText("Nicht gespeichert. Ungültiges Schlüsselformat "
                    + "oder Android Keystore nicht verfügbar.");
                return;
            }
            message.setText("Schlüssel ausschließlich auf diesem Handy gespeichert. "
                + "Das Google-Cockpit kann nun gestartet werden.");
            updateLaunchState();
        });
        launch=Ui.button(this,"Festes Google-70/30-Cockpit öffnen");
        root.addView(launch);
        launch.setOnClickListener(v -> {
            if (!hasKey()) {
                message.setText("Google Navigation SDK erfordert zuerst "
                    + "einen gültig gespeicherten Schlüssel.");
                return;
            }
            startActivity(new Intent(this,IntegratedNavigationActivity.class));
            finish();
        });
        Button clear=Ui.button(this,"Gespeicherten Schlüssel entfernen");
        root.addView(clear);
        clear.setOnClickListener(v -> {
            NavigationSdkKeyStore.clear(this);
            keyInput.getText().clear();
            message.setText("Schlüssel lokal entfernt. "
                + "Google-Navigation ist wieder gesperrt.");
            updateLaunchState();
        });
        Ui.section(this,root,"Kosten und Verfügbarkeit",
            "Google Navigation SDK setzt ein aktives Cloud-Abrechnungskonto "
            + "voraus, auch nach Ablauf des Startguthabens. "
            + "Jede Routenanforderung wird vorher bestätigt und lokal auf "
            + "20 Anfragen je UTC-Tag begrenzt. Die lokale Grenze und "
            + "der 1-Euro-Budgetalarm sind KEINE harte Kostensperre.");
        updateLaunchState();
    }

    private boolean hasKey() {
        return NavigationSdkKeyStore.validKey(
            NavigationSdkKeyStore.read(this));
    }
    private void updateLaunchState() {
        if (launch!=null) launch.setEnabled(hasKey());
    }
}
