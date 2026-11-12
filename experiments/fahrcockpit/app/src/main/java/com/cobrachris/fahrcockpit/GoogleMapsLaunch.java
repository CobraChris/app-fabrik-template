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

    /** The user chooses "Route ansehen": show the route before starting it. */
    static Intent directionsIntent(String target) {
        String url = RouteTargetRules.directionsUrl(target);
        if (url == null) return null;
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .setPackage(GoogleMapsUrlRules.GOOGLE_MAPS_PACKAGE);
    }

    /** Explicit driving navigation in Google's own app; no Maps SDK/billing. */
    static Intent navigationIntent(String target) {
        String url = RouteTargetRules.navigationUri(target);
        if (url == null) return null;
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .setPackage(GoogleMapsUrlRules.GOOGLE_MAPS_PACKAGE);
    }

    /**
     * Request an actual system split with the unmodified Google Maps app.
     * Android (not FahrCockpit) decides whether adjacent launch succeeds.
     * No OverlayService and no embedded navigation, keys or cloud billing.
     */
    static Intent adjacentDrivingIntent(String rawTarget) {
        Intent result = rawTarget == null ? mapIntent() : navigationIntent(rawTarget);
        if (result == null) return null;
        result.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        return result;
    }

    static Intent mapIntent() {
        return new Intent(Intent.ACTION_VIEW,
            Uri.parse(GoogleMapsUrlRules.MAP_WITH_TRAFFIC))
            .setPackage(GoogleMapsUrlRules.GOOGLE_MAPS_PACKAGE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
