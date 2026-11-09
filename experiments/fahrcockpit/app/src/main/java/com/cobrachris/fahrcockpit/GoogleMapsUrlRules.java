package com.cobrachris.fahrcockpit;

/** A supported, explicit map request; NOT an undocumented Google UI automation. */
final class GoogleMapsUrlRules {
    static final String GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps";
    // Google's documented Maps URL opens a clean map near the user's location.
    // layer=traffic requests the existing traffic display; no Cloud API key.
    static final String MAP_WITH_TRAFFIC =
        "https://www.google.com/maps/@?api=1&map_action=map&layer=traffic";

    private GoogleMapsUrlRules() {}
}
