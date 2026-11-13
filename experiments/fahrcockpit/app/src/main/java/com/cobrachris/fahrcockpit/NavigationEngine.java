package com.cobrachris.fahrcockpit;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

/**
 * The real Navigation SDK adapter is optional. The normal signed APK builds
 * without a key, SDK, billable calls or any new Android permission.
 */
interface NavigationEngine {
    interface Status {
        void onStatus(String message);
        /** Typed failure path, without changing the existing functional interface. */
        default void onGoogleFailure(NavigationProviderPolicy.Failure reason) {}
        /** Active guidance affects the host window's keep-awake flag. */
        default void onGuidanceStateChanged(boolean active) {}
    }

    boolean configured();
    View createNavigationView(Bundle savedState, Status status);
    void initializeAfterLocationPermission();
    boolean ready();
    void startConfirmedRoute(double lat, double lng, String label);
    void stopGuidanceAndClearRoute();
    /** Explicit user preference; does not start or alter a route. */
    default void setVoiceMuted(boolean muted) {}
    void onStart();
    void onResume();
    void onPause();
    void onStop();
    void onDestroy(boolean activityFinishing);
    void onSaveInstanceState(Bundle state);
    void onConfigurationChanged(Configuration config);
    void onTrimMemory(int level);
}
