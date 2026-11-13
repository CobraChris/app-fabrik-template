package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.view.Gravity;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * OFFLINE INTERACTIVE PREVIEW ONLY: no GPS, no routes, no Google Navigation SDK.
 * Deliberately separate from the real driving activity. This view never
 * contacts Google or an OSM tile provider, and cannot start real guidance.
 */
public final class NavigationPreviewActivity extends Activity {
    private WebView preview;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        try (InputStream in = getAssets().open("fahrcockpit-v21-preview.html")) {
            // InputStream.readAllBytes() is not available on every minSdk=30 device.
            ByteArrayOutputStream out = new ByteArrayOutputStream(16384);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            byte[] bytes = out.toByteArray();
            String html = new String(bytes, StandardCharsets.UTF_8);
            preview = new WebView(this);
            preview.setBackgroundColor(Ui.BG);
            preview.getSettings().setJavaScriptEnabled(true);
            preview.getSettings().setAllowFileAccess(false);
            preview.getSettings().setAllowContentAccess(false);
            preview.getSettings().setDomStorageEnabled(false);
            preview.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
            preview.getSettings().setSupportMultipleWindows(false);
            preview.setWebViewClient(new WebViewClient() {
                @Override public boolean shouldOverrideUrlLoading(
                        WebView view, WebResourceRequest request) {
                    // No external navigation from a simulation.
                    return true;
                }
            });
            setContentView(preview);
            // Opaque about:blank origin. All assets are inline, no network.
            preview.loadDataWithBaseURL("about:blank", html, "text/html", "UTF-8", null);
        } catch (Exception unavailable) {
            TextView error = Ui.text(this,
                "FahrCockpit-Demo nicht verfügbar. Keine echte Navigation gestartet.",
                16, Ui.FG, true);
            error.setGravity(Gravity.CENTER);
            setContentView(error);
        }
    }

    @Override protected void onDestroy() {
        if (preview != null) {
            preview.stopLoading();
            preview.destroy();
            preview = null;
        }
        super.onDestroy();
    }
}
