package com.cobrachris.fahrcockpit;

import org.junit.Test;
import static org.junit.Assert.*;

public final class SapheNotificationRulesTest {
    @Test public void exactPackageOnlyWithExplicitConsent() {
        assertTrue(SapheNotificationRules.shouldRecord("my.saphelink", true));
        assertFalse(SapheNotificationRules.shouldRecord("my.saphelink", false));
        assertFalse(SapheNotificationRules.shouldRecord("my.saphelink.fake", true));
        assertFalse(SapheNotificationRules.shouldRecord("com.spotify.music", true));
        assertFalse(SapheNotificationRules.shouldRecord(null, true));
    }

    @Test public void onlyFreshTimestampIsShown() {
        long now = 1763370000000L;
        assertFalse(SapheNotificationRules.isRecent(0, now));
        assertTrue(SapheNotificationRules.isRecent(now - 7500L, now));
        assertFalse(SapheNotificationRules.isRecent(now - 10000L, now));
        assertFalse(SapheNotificationRules.isRecent(now + 100L, now));
    }
}
