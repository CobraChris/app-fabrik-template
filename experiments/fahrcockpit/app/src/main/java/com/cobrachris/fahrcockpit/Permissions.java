package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.provider.Settings;

/** Read-only check for the explicit NotificationListenerService user grant. */
final class Permissions {
    private Permissions() {}

    static boolean isMediaListenerEnabled(Context context) {
        String enabled = Settings.Secure.getString(context.getContentResolver(),
            "enabled_notification_listeners");
        if (enabled == null) return false;
        for (String component : enabled.split(":")) {
            if (component.startsWith(context.getPackageName() + "/")
                && component.contains("SpotifyNotificationListener")) return true;
        }
        return false;
    }
}
