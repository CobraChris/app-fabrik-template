package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Android OS geocoder: not Google Places, no project API key and no billing. */
final class AndroidAddressSuggestions {
    interface Callback { void onSuggestions(List<String> addresses); }

    private AndroidAddressSuggestions() {}

    static void find(Activity activity, String query, Callback callback) {
        if (!DestinationSuggestionRules.shouldQueryAndroidGeocoder(query)
                || !Geocoder.isPresent()) {
            callback.onSuggestions(new ArrayList<>());
            return;
        }
        Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
        if (Build.VERSION.SDK_INT >= 33) {
            geocoder.getFromLocationName(query, 3, new Geocoder.GeocodeListener() {
                @Override public void onGeocode(List<Address> matches) {
                    activity.runOnUiThread(() -> callback.onSuggestions(toLabels(matches)));
                }
                @Override public void onError(String message) {
                    activity.runOnUiThread(() -> callback.onSuggestions(new ArrayList<>()));
                }
            });
        } else {
            new Thread(() -> {
                List<Address> matches = null;
                try { matches = geocoder.getFromLocationName(query, 3); }
                catch (java.io.IOException | IllegalArgumentException ignored) {}
                final List<String> labels = toLabels(matches);
                activity.runOnUiThread(() -> callback.onSuggestions(labels));
            }, "fahrcockpit-address-suggestions").start();
        }
    }

    private static List<String> toLabels(List<Address> matches) {
        List<String> result = new ArrayList<>();
        if (matches == null) return result;
        for (Address address : matches) {
            if (address == null) continue;
            String label = RouteTargetRules.clean(address.getAddressLine(0));
            if (label == null) continue;
            if (!result.contains(label)) result.add(label);
        }
        return result;
    }
}
