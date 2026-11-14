package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Free Android Geocoder suggestions. No Google Cloud or Places API key. */
final class AddressSuggestionResolver {
    interface Callback {
        void completed(List<DestinationStore.Entry> suggestions);
    }

    private AddressSuggestionResolver() {}

    static void suggest(Activity activity, String query, Callback callback) {
        if (!DestinationMatchRules.maySuggestOnline(query) || !Geocoder.isPresent()) {
            callback.completed(new ArrayList<>());
            return;
        }
        Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
        if (Build.VERSION.SDK_INT >= 33) {
            geocoder.getFromLocationName(query, 5, new Geocoder.GeocodeListener() {
                @Override public void onGeocode(List<Address> addresses) {
                    activity.runOnUiThread(() -> callback.completed(convert(addresses)));
                }
                @Override public void onError(String error) {
                    activity.runOnUiThread(() -> callback.completed(new ArrayList<>()));
                }
            });
        } else {
            new Thread(() -> {
                List<Address> found;
                try {
                    found = geocoder.getFromLocationName(query, 5);
                } catch (IOException | IllegalArgumentException unavailable) {
                    found = new ArrayList<>();
                }
                final List<DestinationStore.Entry> list = convert(found);
                activity.runOnUiThread(() -> callback.completed(list));
            }, "fahrcockpit-free-address-suggest").start();
        }
    }

    private static List<DestinationStore.Entry> convert(List<Address> addresses) {
        List<DestinationStore.Entry> suggestions = new ArrayList<>();
        if (addresses == null) return suggestions;
        for (Address address : addresses) {
            if (address == null) continue;
            String line = address.getMaxAddressLineIndex() >= 0
                ? address.getAddressLine(0) : null;
            String target = RouteTargetRules.clean(line);
            if (target == null) continue;
            boolean duplicate = false;
            for (DestinationStore.Entry existing : suggestions)
                if (DestinationMatchRules.sameTarget(existing.target, target))
                    duplicate = true;
            if (!duplicate) suggestions.add(new DestinationStore.Entry(
                target, target, false));
            if (suggestions.size() >= 5) break;
        }
        return suggestions;
    }
}
