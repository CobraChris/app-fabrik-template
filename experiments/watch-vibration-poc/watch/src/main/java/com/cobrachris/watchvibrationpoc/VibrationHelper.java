package com.cobrachris.watchvibrationpoc;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

final class VibrationHelper {
    private VibrationHelper() {}

    static boolean vibrate(Context context) {
        final Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = manager == null ? null : manager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (vibrator == null || !vibrator.hasVibrator()) return false;
        vibrator.vibrate(VibrationEffect.createOneShot(5000L, VibrationEffect.DEFAULT_AMPLITUDE));
        return true;
    }
}
