package com.cobrachris.fahrcockpit;

import android.app.Activity;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Compiled only in explicitly selected Places test builds, never activated in v23. */
public final class PaidPlacesAutocompleteBridge implements PlacesAutocompleteBridge {
    private final Activity activity;
    private final PlacesClient client;
    private AutocompleteSessionToken session;

    public PaidPlacesAutocompleteBridge(Activity activity, String restrictedKey) {
        this.activity = activity;
        if (!BuildConfig.GOOGLE_PLACES_ACTIVATED
                || !PlacesSdkKeyStore.validKey(restrictedKey))
            throw new IllegalStateException("Google Places was not approved");
        if (!Places.isInitialized())
            Places.initializeWithNewPlacesApiEnabled(activity, restrictedKey);
        client = Places.createClient(activity);
    }

    @Override public void suggest(String query, Predictions callback) {
        if (!DestinationMatchRules.maySuggestOnline(query)) {
            callback.success(new ArrayList<>());
            return;
        }
        if (session == null) session = AutocompleteSessionToken.newInstance();
        FindAutocompletePredictionsRequest request =
            FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setSessionToken(session)
                .build();
        client.findAutocompletePredictions(request)
            .addOnSuccessListener(response -> {
                List<Prediction> values = new ArrayList<>();
                for (AutocompletePrediction candidate :
                        response.getAutocompletePredictions()) {
                    if (candidate.getPlaceId() == null) continue;
                    values.add(new Prediction(candidate.getPlaceId(),
                        candidate.getFullText(null).toString()));
                }
                callback.success(values);
            })
            .addOnFailureListener(error -> callback.failure());
    }

    @Override public void resolve(Prediction prediction, Selection callback) {
        if (session == null || prediction == null) {
            callback.failure();
            return;
        }
        FetchPlaceRequest request = FetchPlaceRequest.builder(prediction.placeId,
                Arrays.asList(Place.Field.LOCATION, Place.Field.FORMATTED_ADDRESS))
            .setSessionToken(session).build();
        client.fetchPlace(request).addOnSuccessListener(response -> {
            reset();
            Place place = response.getPlace();
            LatLng point = place.getLocation();
            String coords = point == null ? null :
                RouteTargetRules.coordinates(Double.toString(point.latitude),
                    Double.toString(point.longitude));
            if (coords == null) {
                callback.failure();
                return;
            }
            String formatted = RouteTargetRules.clean(place.getFormattedAddress());
            callback.success(formatted == null ? prediction.display : formatted, coords);
        }).addOnFailureListener(error -> {
            reset();
            callback.failure();
        });
    }

    @Override public void reset() {
        session = null;
    }
}
