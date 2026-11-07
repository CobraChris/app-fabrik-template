package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * Uses Blitzer.de PRO's documented explicit receiver. No scraping, analytics,
 * location access, undocumented APIs, or background app-start automation.
 */
public final class BlitzerBridge {
    public enum SendResult {
        SENT_UNCONFIRMED,
        NOT_INSTALLED,
        FAILED
    }

    private BlitzerBridge() {
    }

    public static boolean installed(Context context) {
        try {
            context.getPackageManager().getPackageInfo(BlitzerContract.PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static SendResult send(Context context, BlitzerContract.Command command) {
        if (!installed(context)) {
            return SendResult.NOT_INSTALLED;
        }
        Intent intent = new Intent(BlitzerContract.actionFor(command));
        intent.setClassName(BlitzerContract.PACKAGE, BlitzerContract.RECEIVER);
        String mode = BlitzerContract.modeFor(command);
        if (mode != null) {
            intent.putExtra(BlitzerContract.EXTRA_MODE, mode);
        }
        try {
            context.sendBroadcast(intent);
            // Save the LAST REQUESTED command, not a claimed active state.
            context.getSharedPreferences("fahrcockpit_blitzer_v1", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("has_request", true)
                .putBoolean("last_start_requested", command != BlitzerContract.Command.STOP)
                .apply();
            // sendBroadcast provides no confirmation that Blitzer.de processed it.
            return SendResult.SENT_UNCONFIRMED;
        } catch (RuntimeException e) {
            return SendResult.FAILED;
        }
    }
}
