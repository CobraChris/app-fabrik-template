package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.view.Gravity;
import java.io.InputStream;
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
            byte[] bytes = in.readAllBytes();
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
