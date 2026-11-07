package de.cobrachris.fahrcockpit;

import android.service.notification.NotificationListenerService;

/**
 * Gives the application access to active MediaSessions only after explicit
 * Notification Access consent. No notification content is logged or stored.
 */
public final class MediaAccessListenerService extends NotificationListenerService {}
