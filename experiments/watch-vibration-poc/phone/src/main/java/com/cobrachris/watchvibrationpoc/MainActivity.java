package com.cobrachris.watchvibrationpoc;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Manual proof of concept only; not an alarm clock. */
public final class MainActivity extends Activity implements MessageClient.OnMessageReceivedListener {
    private static final String VIBRATE_PATH = "/watch-vibration-poc/vibrate";
    private static final String ACK_PATH = "/watch-vibration-poc/ack";
    private TextView status;
    private Button testButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(32, 40, 32, 40);
        layout.setBackgroundColor(Color.WHITE);

        TextView heading = new TextView(this);
        heading.setText("Galaxy Watch – Vibrationstest");
        heading.setTextSize(24);
        heading.setTextColor(Color.BLACK);
        layout.addView(heading);

        TextView explanation = new TextView(this);
        explanation.setText("Die Watch-App muss ebenfalls installiert sein. Dieser Test stellt noch keinen Wecker.");
        explanation.setTextSize(16);
        explanation.setPadding(0, 28, 0, 28);
        layout.addView(explanation);

        testButton = new Button(this);
        testButton.setText("Uhr 5 Sekunden vibrieren lassen");
        layout.addView(testButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        status = new TextView(this);
        status.setText("Bereit zum Test");
        status.setTextSize(16);
        status.setPadding(0, 32, 0, 0);
        layout.addView(status);
        setContentView(layout);

        testButton.setOnClickListener(v -> sendVibrationRequest());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    protected void onStop() {
        Wearable.getMessageClient(this).removeListener(this);
        super.onStop();
    }

    private void sendVibrationRequest() {
        testButton.setEnabled(false);
        status.setText("Suche verbundene Wear-OS-Uhr ...");
        Wearable.getNodeClient(this).getConnectedNodes()
                .addOnSuccessListener(this::sendToFirstWatch)
                .addOnFailureListener(e -> showError("Verbindungssuche fehlgeschlagen: " + e.getMessage()));
    }

    private void sendToFirstWatch(List<Node> nodes) {
        if (nodes.isEmpty()) {
            showError("Keine Uhr über Wear OS Data Layer gefunden. Bluetooth und die Watch-App prüfen.");
            return;
        }
        Node selected = nodes.get(0);
        for (Node node : nodes) {
            if (node.isNearby()) {
                selected = node;
                break;
            }
        }
        final Node watch = selected;
        status.setText("Sende an " + watch.getDisplayName() + " ...");
        Wearable.getMessageClient(this)
                .sendMessage(watch.getId(), VIBRATE_PATH, new byte[0])
                .addOnSuccessListener(result -> {
                    testButton.setEnabled(true);
                    status.setText("Anfrage übermittelt. Warte auf Bestätigung der Uhr ...");
                })
                .addOnFailureListener(e -> showError("Senden fehlgeschlagen: " + e.getMessage()));
    }

    private void showError(String message) {
        testButton.setEnabled(true);
        status.setText(message);
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        if (!ACK_PATH.equals(event.getPath())) return;
        String result = new String(event.getData(), StandardCharsets.UTF_8);
        runOnUiThread(() -> status.setText(
                "OK".equals(result)
                        ? "Uhr bestätigt: 5 Sekunden Vibration gestartet."
                        : "Uhr erreichbar, aber Vibrationsmotor nicht verfügbar."));
    }
}
