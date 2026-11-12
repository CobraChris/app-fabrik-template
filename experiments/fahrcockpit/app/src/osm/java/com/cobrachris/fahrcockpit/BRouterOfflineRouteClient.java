package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import java.util.Locale;
import btools.routingapp.IBRouterService;

/**
 * Optional offline BRouter backend. Explicit IPC, not a network API.
 * Only routes between independent Android GPS and user-supplied/geocoded
 * destination coordinates. Install, profile, and rd5 segments are user-owned.
 */
final class BRouterOfflineRouteClient {
    private static final String PACKAGE = "btools.routingapp";
    private static final String SERVICE = "btools.routingapp.BRouterService";
    private final Handler ui = new Handler(Looper.getMainLooper());

    interface Callback {
        void success(BRouterRouteGeometry.Route route);
        void failure(String explanation);
    }
    private BRouterOfflineRouteClient() {}

    static boolean available(Activity activity) {
        try {
            activity.getPackageManager().getPackageInfo(PACKAGE, 0);
            return true;
        } catch (android.content.pm.PackageManager.NameNotFoundException notInstalled) {
            return false;
        }
    }
    private static String coords(double lat, double lon) {
        if (!Double.isFinite(lat) || !Double.isFinite(lon)
                || lat < -90 || lat > 90 || lon < -180 || lon > 180)
            throw new IllegalArgumentException("Invalid coordinates");
        return String.format(Locale.US, "%.7f,%.7f", lon, lat);
    }

    static void route(Activity host, double fromLat, double fromLon,
                      double toLat, double toLon, Callback callback) {
        if (!available(host)) {
            callback.failure("BRouter fehlt. Die kostenlose App und regionale Offline-Routingdaten müssen manuell eingerichtet werden.");
            return;
        }
        final String lonlats;
        try {
            lonlats = coords(fromLat, fromLon) + "|" + coords(toLat, toLon);
        } catch (IllegalArgumentException badCoordinates) {
            callback.failure("Ungültige Start- oder Zielkoordinaten.");
            return;
        }
        Context context = host.getApplicationContext();
        Intent intent = new Intent();
        intent.setClassName(PACKAGE, SERVICE);
        ServiceConnection connection = new ServiceConnection() {
            boolean complete;
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                IBRouterService service = IBRouterService.Stub.asInterface(binder);
                new Thread(() -> {
                    try {
                        Bundle params = new Bundle();
                        params.putString("lonlats", lonlats);
                        params.putString("v", "motorcar");
                        params.putString("fast", "1");
                        params.putString("trackFormat", "json");
                        params.putString("maxRunningTime", "60");
                        // IPC result is computed locally by BRouter; no paid API.
                        String result = service.getTrackFromParams(params);
                        BRouterRouteGeometry.Route route = BRouterRouteGeometry.parse(result);
                        ui().post(() -> {
                            finish(context, this);
                            if (!host.isFinishing() && !host.isDestroyed()) callback.success(route);
                        });
                    } catch (RemoteException | RuntimeException error) {
                        ui().post(() -> {
                            finish(context, this);
                            if (!host.isFinishing() && !host.isDestroyed())
                                callback.failure("Offline-Route nicht verfügbar. BRouter-Profile und Routingdaten prüfen.");
                        });
                    }
                }, "fahrcockpit-brouter-route").start();
            }
            @Override public void onServiceDisconnected(ComponentName name) {
                finish(context, this);
                if (!host.isFinishing() && !host.isDestroyed())
                    callback.failure("BRouter-Verbindung beendet.");
            }
            private synchronized void finish(Context ctx, ServiceConnection conn) {
                if (complete) return;
                complete = true;
                try { ctx.unbindService(conn); } catch (IllegalArgumentException ignored) {}
            }
        };
        try {
            if (!context.bindService(intent, connection, Context.BIND_AUTO_CREATE))
                callback.failure("BRouter-Dienst ist nicht erreichbar.");
        } catch (SecurityException | IllegalArgumentException unavailable) {
            callback.failure("BRouter-Dienst konnte nicht verbunden werden.");
        }
    }
    private static Handler ui() {
        return new Handler(Looper.getMainLooper());
    }
}
