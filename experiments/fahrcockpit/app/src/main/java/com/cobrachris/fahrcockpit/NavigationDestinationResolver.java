package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** Android's OS geocoder only, not a new Google Cloud/Places billable API. */
final class NavigationDestinationResolver {
    interface Callback {
        void resolved(double lat, double lng, String name);
        void failed(String reason);
    }

    private NavigationDestinationResolver() {}

    static void resolve(Activity activity, String raw, Callback callback) {
        String target = RouteTargetRules.clean(raw);
        if (target == null) {
            callback.failed("Bitte ein gültiges Ziel eingeben.");
            return;
        }
        double[] parsed = IntegratedNavigationRules.parseCoordinates(target);
        if (parsed != null) {
            callback.resolved(parsed[0], parsed[1], target);
            return;
        }
        if (!Geocoder.isPresent()) {
            callback.failed("Die Android-Ortssuche ist nicht verfügbar. "
                + "Bitte Koordinaten verwenden.");
            return;
        }
        final Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
        if (Build.VERSION.SDK_INT >= 33) {
            geocoder.getFromLocationName(target, 1, new Geocoder.GeocodeListener() {
                @Override public void onGeocode(List<Address> matches) {
                    activity.runOnUiThread(() -> deliver(target, matches, callback));
                }
                @Override public void onError(String errorMessage) {
                    activity.runOnUiThread(() ->
                        callback.failed("Ort konnte nicht gefunden werden."));
                }
            });
        } else {
            new Thread(() -> {
                try {
                    List<Address> matches = geocoder.getFromLocationName(target, 1);
                    activity.runOnUiThread(() -> deliver(target, matches, callback));
                } catch (IOException | IllegalArgumentException error) {
                    activity.runOnUiThread(() ->
                        callback.failed("Ortssuche fehlgeschlagen. Koordinaten eingeben."));
                }
            }, "fahrcockpit-geocoder").start();
        }
    }

    private static void deliver(String target, List<Address> matches,
                                Callback callback) {
        if (matches == null || matches.isEmpty()) {
            callback.failed("Kein passender Ort gefunden. Koordinaten eingeben.");
            return;
        }
        Address first = matches.get(0);
        String coordinate = IntegratedNavigationRules.displayCoordinates(
            first.getLatitude(), first.getLongitude());
        if (coordinate == null) {
            callback.failed("Ungültiges Ergebnis der Standortsuche.");
            return;
        }
        callback.resolved(first.getLatitude(), first.getLongitude(),
            target + " (" + coordinate + ")");
    }
}
