package com.cobrachris.watchnotificationpoc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * System-scheduled broadcast, independent from activity lifecycle or screen state.
 */
public final class TestNotificationReceiver extends BroadcastReceiver {
    static final String ACTION = "com.cobrachris.watchnotificationpoc.ACTION_STANDBY_TEST";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) return;
        NotificationTest.post(context);
    }
}
