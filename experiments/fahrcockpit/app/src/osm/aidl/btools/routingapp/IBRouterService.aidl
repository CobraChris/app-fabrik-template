package btools.routingapp;

// BRouter's published Android IPC contract. This is only a client-side copy
// of the one-method interface; it does not include or redistribute its engine.
interface IBRouterService {
    String getTrackFromParams(in android.os.Bundle params);
}
