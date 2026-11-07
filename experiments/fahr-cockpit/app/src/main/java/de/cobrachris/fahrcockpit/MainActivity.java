package de.cobrachris.fahrcockpit;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Explicit, user-initiated setup. No auto-start and no background GPS. */
public final class MainActivity extends Activity {
    private LinearLayout content;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(28), dp(20), dp(28));
        content.setBackgroundColor(Color.rgb(21, 41, 44));
        scroll.addView(content);
        setContentView(scroll);

        TextView heading = label("FahrCockpit · Android-Testversion", 23, true);
        content.addView(heading);
        content.addView(label("Google Maps bleibt deine Navigation. Nur die kompakte Spotify-Leiste erscheint darüber.", 16, false));
        content.addView(label("1. Zwei Android-Freigaben erteilen. 2. Spotify starten. 3. Cockpit aktivieren. 4. Google Maps wie gewohnt öffnen.", 15, false));
        button("1 · Über anderen Apps anzeigen erlauben", () -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        button("2 · Medien-/Benachrichtigungszugriff erlauben", () ->
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        button("3 · Spotify-Leiste starten", () -> {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Bitte zuerst 'Über anderen Apps anzeigen' erlauben.",
                    Toast.LENGTH_LONG).show();
                return;
            }
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
                return;
            }
            Intent intent = new Intent(this, CockpitOverlayService.class);
            intent.setAction(CockpitOverlayService.ACTION_START);
            startForegroundService(intent);
            Toast.makeText(this, "Cockpit gestartet. Google Maps jetzt öffnen.", Toast.LENGTH_LONG).show();
        });
        button("Spotify öffnen", () -> {
            Intent launch = getPackageManager().getLaunchIntentForPackage("com.spotify.music");
            if (launch == null) {
                Toast.makeText(this, "Spotify ist nicht installiert.", Toast.LENGTH_LONG).show();
            } else startActivity(launch);
        });
        button("Cockpit ausschalten", () -> {
            Intent intent = new Intent(this, CockpitOverlayService.class);
            intent.setAction(CockpitOverlayService.ACTION_STOP);
            startService(intent);
        });
        content.addView(label("Datenschutz: Es werden keine Benachrichtigungen ausgelesen, gespeichert oder versendet. Nur aktive Spotify-Medienbefehle werden angesprochen.", 13, false));
        content.addView(label("Noch nicht in dieser Testversion: Tankstellenabfrage, Preise und automatische Arbeitsweg-Erkennung. Keine Blitzerwarnungen.", 13, false));
    }

    private TextView label(String text, int sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(sp);
        v.setPadding(0, 0, 0, dp(20));
        if (bold) v.setTypeface(null, Typeface.BOLD);
        return v;
    }
    private void button(String title, Runnable command) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setOnClickListener(v -> command.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(8);
        content.addView(b, p);
    }
    private int dp(int d) {
        return Math.round(d * getResources().getDisplayMetrics().density);
    }
}
