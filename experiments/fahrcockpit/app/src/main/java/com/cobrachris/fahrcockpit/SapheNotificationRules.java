package com.cobrachris.fahrcockpit;

/**
 * Package-only detection for a user-opted-in diagnostic.
 * A package notification is NOT necessarily a traffic or camera warning.
 */
final class SapheNotificationRules {
    private SapheNotificationRules() {}

    static boolean shouldRecord(String packageName, boolean optedIn) {
        return optedIn && WarningSource.SAPHE_PACKAGE.equals(packageName);
    }

    static boolean isRecent(long observedAt, long now) {
        return observedAt > 0 && now >= observedAt && now - observedAt < 10000L;
    }
}
