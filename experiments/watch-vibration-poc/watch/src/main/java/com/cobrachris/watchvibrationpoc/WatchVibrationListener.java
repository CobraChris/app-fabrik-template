package com.cobrachris.watchvibrationpoc;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.StandardCharsets;

/** Manifest listener handles messages even when the watch activity is closed. */
public final class WatchVibrationListener extends WearableListenerService {
    private static final String VIBRATE_PATH = "/watch-vibration-poc/vibrate";
    private static final String ACK_PATH = "/watch-vibration-poc/ack";

    @Override
    public void onMessageReceived(MessageEvent event) {
        if (!VIBRATE_PATH.equals(event.getPath())) return;
        boolean started;
        try {
            started = VibrationHelper.vibrate(this);
        } catch (RuntimeException e) {
            started = false;
        }
        Wearable.getMessageClient(this).sendMessage(
                event.getSourceNodeId(),
                ACK_PATH,
                (started ? "OK" : "NO_VIBRATOR").getBytes(StandardCharsets.UTF_8));
    }
}
