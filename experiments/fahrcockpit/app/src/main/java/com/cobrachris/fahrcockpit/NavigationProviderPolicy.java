package com.cobrachris.fahrcockpit;

/**
 * Google Navigation SDK is ALWAYS the first-choice embedded navigation.
 * OSM is selected only after a Google failure/unavailability and only when
 * a complete independent OSM navigation engine is actually ready.
 * Never transfer Google SDK route/map/POI content to the OSM provider.
 */
final class NavigationProviderPolicy {
    enum Provider { GOOGLE_NAVIGATION_SDK, OPENSTREETMAP_NAVIGATION, GOOGLE_MAPS_EXTERNAL }
    enum Failure {
        NONE, NAVIGATION_QUOTA_EXHAUSTED, LOCAL_REQUEST_LIMIT,
        BILLING_OR_AUTHORIZATION_UNAVAILABLE, GOOGLE_SERVICE_UNAVAILABLE
    }

    private NavigationProviderPolicy() {}

    static Provider preferred(boolean googleSdkConfigured, boolean osmNavigatorReady) {
        if (googleSdkConfigured) return Provider.GOOGLE_NAVIGATION_SDK;
        return osmNavigatorReady
            ? Provider.OPENSTREETMAP_NAVIGATION : Provider.GOOGLE_MAPS_EXTERNAL;
    }

    static Provider fallback(Failure googleFailure, boolean osmNavigatorReady) {
        if (googleFailure == null || googleFailure == Failure.NONE)
            return Provider.GOOGLE_NAVIGATION_SDK;
        return osmNavigatorReady
            ? Provider.OPENSTREETMAP_NAVIGATION : Provider.GOOGLE_MAPS_EXTERNAL;
    }

    static boolean fallbackRecommended(Failure reason) {
        return reason != null && reason != Failure.NONE;
    }

    static boolean routeRequiresExplicitApproval(Provider provider) {
        return provider == Provider.GOOGLE_NAVIGATION_SDK;
    }

    static String displayName(Provider provider) {
        if (provider == Provider.GOOGLE_NAVIGATION_SDK)
            return "Google Navigation (Standard)";
        if (provider == Provider.OPENSTREETMAP_NAVIGATION)
            return "OpenStreetMap (Ersatznavigation)";
        return "Google Maps App (separates Android-Fenster)";
    }
}
