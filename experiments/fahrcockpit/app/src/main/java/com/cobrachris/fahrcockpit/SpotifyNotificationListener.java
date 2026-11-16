package com.cobrachris.fahrcockpit;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/**
 * Android notification-listener grant is used for Spotify MediaSession access.
 * In addition, with separate in-app consent, record only the TIME of a
 * notification from the exact Saphe Link package for diagnostic purposes.
 *
 * Never read, copy, log, or transmit message text or notification contents.
 * This is NOT a documented Saphe warning API and does not infer danger events.
 */
public final class SpotifyNotificationListener extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification notification) {
        if (notification == null) return;
        SapheNotificationProbe.onNotificationPosted(this, notification.getPackageName());
    }
}
