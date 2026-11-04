package com.cobrachris.watchvibrationpoc;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class WatchMainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(20, 20, 20, 20);
        layout.setGravity(Gravity.CENTER);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView status = new TextView(this);
        status.setText("Watch-Test bereit");
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        layout.addView(status);

        Button localTest = new Button(this);
        localTest.setText("Lokal vibrieren");
        layout.addView(localTest);
        localTest.setOnClickListener(v ->
                status.setText(VibrationHelper.vibrate(this)
                        ? "Vibration gestartet"
                        : "Kein Vibrationsmotor erkannt"));
        setContentView(layout);
    }
}
