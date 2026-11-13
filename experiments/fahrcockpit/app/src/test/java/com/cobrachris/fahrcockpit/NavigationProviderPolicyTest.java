package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

public final class NavigationProviderPolicyTest {
    @Test public void googleIsAlwaysFirstWhenConfigured() {
        assertEquals(NavigationProviderPolicy.Provider.GOOGLE_NAVIGATION_SDK,
            NavigationProviderPolicy.preferred(true, true));
        assertEquals(NavigationProviderPolicy.Provider.GOOGLE_NAVIGATION_SDK,
            NavigationProviderPolicy.preferred(true, false));
    }
    @Test public void osmIsOnlyChosenIfACompleteOsmNavigatorExists() {
        assertEquals(NavigationProviderPolicy.Provider.OPENSTREETMAP_NAVIGATION,
            NavigationProviderPolicy.preferred(false, true));
        assertEquals(NavigationProviderPolicy.Provider.GOOGLE_MAPS_EXTERNAL,
            NavigationProviderPolicy.preferred(false, false));
    }
    @Test public void fallbackAfterQuotaOrBillingLossDoesNotReturnToGoogleSdk() {
        for (NavigationProviderPolicy.Failure reason :
                new NavigationProviderPolicy.Failure[] {
                    NavigationProviderPolicy.Failure.NAVIGATION_QUOTA_EXHAUSTED,
                    NavigationProviderPolicy.Failure.LOCAL_REQUEST_LIMIT,
                    NavigationProviderPolicy.Failure.BILLING_OR_AUTHORIZATION_UNAVAILABLE,
                    NavigationProviderPolicy.Failure.GOOGLE_SERVICE_UNAVAILABLE }) {
            assertEquals(NavigationProviderPolicy.Provider.OPENSTREETMAP_NAVIGATION,
                NavigationProviderPolicy.fallback(reason, true));
            assertEquals(NavigationProviderPolicy.Provider.GOOGLE_MAPS_EXTERNAL,
                NavigationProviderPolicy.fallback(reason, false));
            assertTrue(NavigationProviderPolicy.fallbackRecommended(reason));
        }
    }
    @Test public void noFailureDoesNotSilentlySwitch() {
        assertEquals(NavigationProviderPolicy.Provider.GOOGLE_NAVIGATION_SDK,
            NavigationProviderPolicy.fallback(NavigationProviderPolicy.Failure.NONE, true));
        assertFalse(NavigationProviderPolicy.fallbackRecommended(null));
        assertFalse(NavigationProviderPolicy.fallbackRecommended(
            NavigationProviderPolicy.Failure.NONE));
        assertTrue(NavigationProviderPolicy.routeRequiresExplicitApproval(
            NavigationProviderPolicy.Provider.GOOGLE_NAVIGATION_SDK));
    }
    @Test public void labelsKeepGooglePrimary() {
        assertEquals("Google Navigation (Standard)", NavigationProviderPolicy.displayName(
            NavigationProviderPolicy.Provider.GOOGLE_NAVIGATION_SDK));
        assertEquals("OpenStreetMap (Ersatznavigation)", NavigationProviderPolicy.displayName(
            NavigationProviderPolicy.Provider.OPENSTREETMAP_NAVIGATION));
    }
}
