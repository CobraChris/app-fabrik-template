package com.cobrachris.fahrcockpit;

import java.util.Locale;

/** Deterministic, offline search rules; does not access any cloud API. */
final class DestinationSuggestionRules {
    static final int MIN_GEOCODER_LENGTH = 4;
    static final int MAX_VISIBLE_SUGGESTIONS = 5;
    static final long GEOCODER_DELAY_MS = 650L;
    private DestinationSuggestionRules() {}

    static String normalized(String value) {
        String clean = RouteTargetRules.clean(value);
        return clean == null ? "" : clean.toLowerCase(Locale.ROOT);
    }

    static boolean matches(String query, String label, String target) {
        String q = normalized(query);
        return !q.isEmpty() && (normalized(label).contains(q)
            || normalized(target).contains(q));
    }

    static boolean shouldQueryAndroidGeocoder(String query) {
        return normalized(query).length() >= MIN_GEOCODER_LENGTH;
    }

    /** No Places calls, SDK clients, keys or billing without a separate approval. */
    static boolean googlePlacesEnabled() {
        return false;
    }
}
