package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Explicitly user-initiated and allowlisted GitHub-release updater. */
public final class UpdateActivity extends Activity {
    private static final String RELEASES =
        "https://api.github.com/repos/CobraChris/app-fabrik-template/releases?per_page=100";
    private static final String APK = "fahrcockpit-release.apk";
    private TextView status;
    private Button check, download;
    private String verifiedUrl;

    static final class Candidate {
        final long code;
        final String url;
        Candidate(long code, String url) { this.code = code; this.url = url; }
    }

    /** Parse only exact, non-draft APK assets belonging to FahrCockpit tags. */
    static Candidate newestSignedAsset(String response) {
        try {
            JSONArray releases = new JSONArray(response);
            Candidate newest = null;
            for (int i = 0; i < releases.length(); i++) {
                JSONObject release = releases.optJSONObject(i);
                if (release == null || release.optBoolean("draft", true)) continue;
                String tag = release.optString("tag_name", "");
                long version = UpdateUrlPolicy.versionCode(tag);
                if (version <= 0) continue;
                JSONArray assets = release.optJSONArray("assets");
                if (assets == null) continue;
                for (int j = 0; j < assets.length(); j++) {
                    JSONObject asset = assets.optJSONObject(j);
                    if (asset == null || !APK.equals(asset.optString("name", ""))) continue;
                    String url = asset.optString("browser_download_url", "");
                    if (!UpdateUrlPolicy.isForTag(url, tag)) continue;
                    if (newest == null || version > newest.code) {
                        newest = new Candidate(version, url);
                    }
                }
            }
            return newest;
        } catch (Exception ignored) { return null; }
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout root = Ui.column(this);
        root.setPadding(Ui.dp(this, 22), Ui.dp(this, 24),
            Ui.dp(this, 22), Ui.dp(this, 38));
        scroll.addView(root);
        setContentView(scroll);
        getWindow().getDecorView().setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars =
                insets.getInsets(android.view.WindowInsets.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        root.addView(Ui.text(this, "FahrCockpit aktualisieren", 26, Ui.FG, true));
        root.addView(Ui.text(this,
            "Aktuelle Version " + currentVersionName() + " (Build " +
            currentVersionCode() + "). Ein Update wird nur auf deinen Klick geprüft.",
            15, Ui.MUTED, false));

        status = Ui.text(this, "Noch nicht geprüft.", 15, Ui.FG, false);
        root.addView(status);
        check = Ui.button(this, "Auf GitHub nach einem Update suchen");
        root.addView(check);
        check.setOnClickListener(v -> checkForUpdate());
        download = Ui.button(this, "APK direkt im Browser herunterladen");
        download.setEnabled(false);
        root.addView(download);
        download.setOnClickListener(v -> {
            if (verifiedUrl == null) return;
            try {
                Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(verifiedUrl))
                    .addCategory(Intent.CATEGORY_BROWSABLE);
                startActivity(view);
            } catch (RuntimeException error) {
                status.setText("Es konnte kein Browser geöffnet werden.");
            }
        });

        root.addView(Ui.text(this,
            "Keine ZIP-Dateien und keine ChatGPT-Dateidownloads. Android fragt " +
            "bei der Installation nach einer Bestätigung. Ein Update ohne " +
            "Deinstallation ist nur möglich, wenn die vorhandene Version mit " +
            "demselben Zertifikat signiert ist und der Versionscode steigt.",
            14, Ui.MUTED, false));
        root.addView(Ui.text(this,
            "Einmaliger Wechsel von Debug-APK zu dauerhaft signierter Release-APK: " +
            "Android kann eine Neuinstallation verlangen. Zuvor gespeicherte " +
            "Einstellungen können dabei verloren gehen. Nicht vorschnell löschen.",
            14, Ui.WARN, false));
    }

    private long currentVersionCode() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0)
                .getLongVersionCode();
        } catch (Exception ignored) { return -1; }
    }

    private String currentVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0)
                .versionName;
        } catch (Exception ignored) { return "unbekannt"; }
    }

    private void checkForUpdate() {
        verifiedUrl = null;
        download.setEnabled(false);
        check.setEnabled(false);
        status.setText("Prüfe GitHub Releases …");

        new Thread(() -> {
            HttpURLConnection connection = null;
            Candidate candidate = null;
            boolean failed = false;
            try {
                connection = (HttpURLConnection) new URL(RELEASES).openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "FahrCockpit-update-check");
                if (connection.getResponseCode() != 200) {
                    throw new IllegalStateException("Unexpected GitHub response");
                }
                StringBuilder buffer = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(),
                        StandardCharsets.UTF_8))) {
                    char[] chunk = new char[4096];
                    int length;
                    while ((length = reader.read(chunk)) != -1) {
                        buffer.append(chunk, 0, length);
                        if (buffer.length() > 2_500_000) {
                            throw new IllegalStateException("Response too large");
                        }
                    }
                }
                candidate = newestSignedAsset(buffer.toString());
            } catch (Exception ignored) {
                failed = true;
            } finally {
                if (connection != null) connection.disconnect();
            }
            final Candidate best = candidate;
            final boolean networkFailed = failed;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                check.setEnabled(true);
                if (networkFailed) {
                    status.setText("GitHub nicht erreichbar. Internet prüfen.");
                } else if (best == null) {
                    status.setText("Noch keine signierte FahrCockpit-Version veröffentlicht.");
                } else if (!UpdateUrlPolicy.isNewer(best.code, currentVersionCode())) {
                    status.setText("Kein neuerer Build gefunden (Release " + best.code + ").");
                } else {
                    verifiedUrl = best.url;
                    status.setText("Neues FahrCockpit-Update: Build " + best.code);
                    download.setEnabled(true);
                }
            });
        }, "fahrcockpit-release-check").start();
    }
}
