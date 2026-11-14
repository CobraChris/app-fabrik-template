package com.cobrachris.fahrcockpit;

import android.app.Activity;
import java.util.List;

/** Optional, never activated by the v23 release candidate.
 * The Android Places SDK is a separate billable Google Maps Platform service.
 */
interface PlacesAutocompleteBridge {
    final class Prediction {
        final String placeId;
        final String display;
        Prediction(String placeId, String display) {
            this.placeId = placeId;
            this.display = display;
        }
    }
    interface Predictions {
        void success(List<Prediction> values);
        void failure();
    }
    interface Selection {
        void success(String label, String coordinates);
        void failure();
    }
    void suggest(String query, Predictions callback);
    void resolve(Prediction prediction, Selection callback);
    void reset();

    static PlacesAutocompleteBridge approved(Activity activity) {
        // Two separate gates: reviewed build approval and encrypted Places key.
        // Both are disabled in v23, so not even an autocomplete request occurs.
        if (!BuildConfig.GOOGLE_PLACES_ACTIVATED || !BuildConfig.WITH_GOOGLE_PLACES)
            return null;
        String key = PlacesSdkKeyStore.read(activity);
        if (!PlacesSdkKeyStore.validKey(key)) return null;
        try {
            Class<?> type = Class.forName(
                "com.cobrachris.fahrcockpit.PaidPlacesAutocompleteBridge");
            return (PlacesAutocompleteBridge) type.getConstructor(Activity.class,
                String.class).newInstance(activity, key);
        } catch (ReflectiveOperationException unavailable) {
            return null;
        }
    }
}
