package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/** Opens map content instead of resuming the last Maps launcher tab. */
final class GoogleMapsLaunch {
    private GoogleMapsLaunch() {}

    static boolean installed(Context context) {
        return context.getPackageManager()
            .getLaunchIntentForPackage(GoogleMapsUrlRules.GOOGLE_MAPS_PACKAGE) != null;
    }

    static Intent mapIntent() {
        return new Intent(Intent.ACTION_VIEW,
            Uri.parse(GoogleMapsUrlRules.MAP_WITH_TRAFFIC))
            .setPackage(GoogleMapsUrlRules.GOOGLE_MAPS_PACKAGE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
