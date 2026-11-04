package io.github.cobrachris.chatlinkopener;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.net.URI;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent incoming = getIntent();
        if (Intent.ACTION_VIEW.equals(incoming.getAction())) {
            Uri link = incoming.getData();
            String rawTarget = link == null ? null : link.getQueryParameter("url");
            URI target = TargetUrl.parse(rawTarget);
            if (target == null) {
                Toast.makeText(this, "Link ungültig oder nicht erlaubt", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            openDefaultBrowser(target);
            finish();
            return;
        }
        showTestScreen();
    }

    private void showTestScreen() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        TextView heading = new TextView(this);
        heading.setText("Chat-Link-Öffner (Prototyp)");
        heading.setTextSize(22);
        layout.addView(heading);

        TextView status = new TextView(this);
        status.setText("Öffnet Links über den Android-Standardbrowser. "
                + "Für ChatGPT-Links muss die Webadresse verifiziert sein.");
        status.setTextSize(16);
        layout.addView(status);

        Button test = new Button(this);
        test.setText("Startpage testweise öffnen");
        test.setOnClickListener(view ->
                openDefaultBrowser(URI.create("https://www.startpage.com/")));
        layout.addView(test);
        setContentView(layout);
    }

    private void openDefaultBrowser(URI target) {
        // Opens another Android activity, not an embedded browser Custom Tab.
        // Android normally selects the user's default browser for HTTP(S) URLs.
        // Verified App Links may instead open an associated target application.
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(target.toString()));
        view.addCategory(Intent.CATEGORY_BROWSABLE);
        view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(view);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "Kein Browser verfügbar", Toast.LENGTH_LONG).show();
        }
    }
}
