package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * User-initiated updates from the existing, protected, signed FahrCockpit
 * GitHub Releases. No background polling and no unattended installation.
 */
public final class UpdateActivity extends Activity {
    private static final String RELEASES =
        "https://api.github.com/repos/CobraChris/app-fabrik-template/releases?per_page=100";
    private static final String APK = "fahrcockpit-release.apk";
    private static final int INSTALL_SOURCE_REQUEST = 821;
    private static final long MAX_APK_BYTES = 180L * 1024L * 1024L;
    private TextView status;
    private Button check, install, browser;
    private Candidate candidate;
    private boolean downloading, checking, awaitingSourceApproval;

    static final class Candidate {
        final long code;
        final String url;
        final String sha256;
        Candidate(long code, String url, String sha256) {
            this.code = code;
            this.url = url;
            this.sha256 = sha256;
        }
    }

    /** Accept only the official release tag, exact APK asset and GitHub digest. */
    static Candidate newestSignedAsset(String response) {
        try {
            JSONArray releases = new JSONArray(response);
            Candidate newest = null;
            for (int i = 0; i < releases.length(); i++) {
                JSONObject release = releases.optJSONObject(i);
                if (release == null || release.optBoolean("draft", true)) continue;
                String tag = release.optString("tag_name", "");
                long code = UpdateUrlPolicy.versionCode(tag);
                if (code <= 0) continue;
                JSONArray assets = release.optJSONArray("assets");
                if (assets == null) continue;
                for (int j = 0; j < assets.length(); j++) {
                    JSONObject asset = assets.optJSONObject(j);
                    if (asset == null || !APK.equals(asset.optString("name", "")))
                        continue;
                    String url = asset.optString("browser_download_url", "");
                    String digest = asset.optString("digest", "");
                    long size = asset.optLong("size", -1L);
                    if (!UpdateUrlPolicy.isForTag(url, tag)
                            || VerifiedUpdatePolicy.parseSha256(digest) == null
                            || size <= 0L || size > MAX_APK_BYTES)
                        continue;
                    if (newest == null || code > newest.code)
                        newest = new Candidate(code, url,
                            VerifiedUpdatePolicy.parseSha256(digest));
                }
            }
            return newest;
        } catch (Exception ignored) { return null; }
    }

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout root = Ui.column(this);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 22),
            Ui.dp(this, 20), Ui.dp(this, 30));
        scroll.addView(root);
        setContentView(scroll);
        getWindow().getDecorView().setOnApplyWindowInsetsListener((view,insets) -> {
            android.graphics.Insets bars =
                insets.getInsets(android.view.WindowInsets.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        root.addView(Ui.text(this, "FahrCockpit aktualisieren", 23, Ui.FG, true));
        root.addView(Ui.text(this,
            "Installiert: " + currentVersionName() + " (Build " +
                currentVersionCode() + ")", 14, Ui.MUTED, false));
        status = Ui.text(this, "Noch nicht nach Updates gesucht.", 14, Ui.FG, false);
        root.addView(status);

        check = Ui.button(this, "Nach Updates suchen");
        check.setOnClickListener(v -> checkForUpdate());
        root.addView(check);

        install = Ui.button(this, "Update herunterladen und installieren");
        install.setOnClickListener(v -> installUpdate());
        root.addView(install);

        browser = Ui.button(this, "Alternativ: APK im Browser öffnen");
        browser.setOnClickListener(v -> openBrowser());
        root.addView(browser);

        root.addView(Ui.text(this,
            "Die APK wird nur auf deinen Wunsch heruntergeladen. " +
            "Vor dem Android-Installationsdialog prüfen wir SHA-256, Paketname, " +
            "Versionscode und das bisherige Originalsignierzertifikat. " +
            "Android verlangt anschließend deine ausdrückliche Bestätigung. " +
            "Bei einer Signaturabweichung: nicht deinstallieren.",
            12, Ui.MUTED, false));
        updateActions();
    }

    private long currentVersionCode() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0)
                .getLongVersionCode();
        } catch (Exception ignored) { return -1L; }
    }

    private String currentVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0)
                .versionName;
        } catch (Exception ignored) { return "unbekannt"; }
    }

    private void updateActions() {
        if (check == null || install == null || browser == null) return;
        boolean busy = checking || downloading;
        boolean hasCandidate = candidate != null;
        check.setEnabled(!busy);
        check.setText(checking ? "Suche nach Updates …" :
            hasCandidate ? "Erneut nach Updates suchen" : "Nach Updates suchen");
        install.setVisibility(hasCandidate ? View.VISIBLE : View.GONE);
        install.setEnabled(hasCandidate && !busy);
        install.setText(hasCandidate ? "Build " + candidate.code +
            " herunterladen und installieren" : "Update installieren");
        browser.setVisibility(hasCandidate ? View.VISIBLE : View.GONE);
        browser.setEnabled(hasCandidate && !busy);
    }

    private static String fetchReleases() throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(RELEASES).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "FahrCockpit-Verified-Updater");
            if (connection.getResponseCode() != 200)
                throw new IllegalStateException("GitHub HTTP " +
                    connection.getResponseCode());
            StringBuilder result = new StringBuilder();
            try (InputStream in = connection.getInputStream();
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                char[] chunk = new char[4096];
                int count;
                while ((count = reader.read(chunk)) != -1) {
                    result.append(chunk, 0, count);
                    if (result.length() > 2_500_000)
                        throw new SecurityException("Release-Metadaten zu groß");
                }
            }
            return result.toString();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void checkForUpdate() {
        if (checking || downloading) return;
        checking = true;
        candidate = null;
        updateActions();
        status.setText("Prüfe freigegebene GitHub-Releases …");
        new Thread(() -> {
            Candidate selected = null;
            String error = null;
            try {
                selected = newestSignedAsset(fetchReleases());
            } catch (Exception ex) {
                error = "GitHub derzeit nicht erreichbar. Internet prüfen.";
            }
            Candidate found = selected;
            String failure = error;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                checking = false;
                if (failure != null) status.setText(failure);
                else if (found == null)
                    status.setText("Kein überprüfbarer FahrCockpit-Release gefunden.");
                else if (!UpdateUrlPolicy.isNewer(found.code, currentVersionCode()))
                    status.setText("Aktuell: kein neuerer Build als " +
                        currentVersionCode() + " verfügbar.");
                else {
                    candidate = found;
                    status.setText("Update verfügbar: Build " + found.code +
                        ". Download nur nach deiner Bestätigung.");
                }
                updateActions();
            });
        }, "fahrcockpit-verified-release-check").start();
    }

    private void openBrowser() {
        Candidate selected = candidate;
        if (selected == null || downloading || checking) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(selected.url))
                .addCategory(Intent.CATEGORY_BROWSABLE));
        } catch (ActivityNotFoundException ex) {
            status.setText("Kein Browser verfügbar.");
        }
    }

    private void installUpdate() {
        Candidate selected = candidate;
        if (selected == null || downloading || checking) return;
        if (!VerifiedUpdatePolicy.isApproved(
                selected.url, "sha256:" + selected.sha256,
                selected.code, currentVersionCode())) {
            candidate = null;
            updateActions();
            status.setText("Updatequelle oder Version ungültig. Erneut suchen.");
            return;
        }
        if (!getPackageManager().canRequestPackageInstalls()) {
            awaitingSourceApproval = true;
            status.setText("Einmalig die Installation aus FahrCockpit erlauben. " +
                "Android fragt danach erneut vor der Installation.");
            try {
                startActivityForResult(new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName())),
                    INSTALL_SOURCE_REQUEST);
            } catch (ActivityNotFoundException ex) {
                awaitingSourceApproval = false;
                status.setText("Android-Quelleinstellung nicht verfügbar. " +
                    "Alternativ APK im Browser öffnen.");
            }
            return;
        }
        downloading = true;
        updateActions();
        status.setText("Lade APK herunter und prüfe die Originalsignatur …");
        new Thread(() -> {
            try {
                Uri verified = VerifiedApkDownloader.download(
                    getApplicationContext(), selected.url, selected.sha256,
                    selected.code, percent -> runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed())
                            status.setText("APK laden und prüfen: " + percent + " %");
                    }));
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    downloading = false;
                    updateActions();
                    try {
                        Intent view = new Intent(Intent.ACTION_VIEW);
                        view.setDataAndType(
                            verified, VerifiedApkDownloader.mimeType());
                        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        view.setClipData(ClipData.newUri(getContentResolver(),
                            "Geprüftes FahrCockpit-Update", verified));
                        startActivity(view);
                        status.setText("Signierte APK geprüft. " +
                            "Bitte das Android-Update bestätigen.");
                    } catch (ActivityNotFoundException | SecurityException ex) {
                        status.setText("Android-Installer nicht verfügbar. " +
                            "Alternativ APK im Browser öffnen.");
                    }
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    downloading = false;
                    updateActions();
                    status.setText("Update abgebrochen: " + ex.getMessage() +
                        ". Nicht deinstallieren; bei Bedarf erneut suchen.");
                });
            }
        }, "FahrCockpit-Verified-Apk-Download").start();
    }

    @Override protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != INSTALL_SOURCE_REQUEST ||
                !awaitingSourceApproval) return;
        awaitingSourceApproval = false;
        if (getPackageManager().canRequestPackageInstalls()) installUpdate();
        else status.setText("Installation aus dieser Quelle nicht erlaubt. " +
            "Du kannst weiterhin den Browser nutzen.");
    }
}
