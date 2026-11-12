package com.cobrachris.fahrcockpit;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.concurrent.atomic.AtomicBoolean;

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.sources.GeoJsonSource;
import static org.maplibre.android.style.layers.PropertyFactory.lineColor;
import static org.maplibre.android.style.layers.PropertyFactory.lineWidth;

/**
 * Native MapLibre OpenStreetMap plus independently calculated BRouter
 * road geometry. This is a ROUTE PLANNING PREVIEW, NOT turn-by-turn guidance.
 *
 * It never consumes Google Maps Navigation SDK routes, traffic or POIs.
 * BRouter is user-installed and uses user-downloaded regional offline data.
 */
public final class OsmRoutingPreviewEngine implements NavigationEngine {
    private static final String SOURCE = "fahrcockpit-brouter-route-source";
    private static final String LAYER = "fahrcockpit-brouter-route-line";
    private static final String EMPTY = "{\"type\":\"FeatureCollection\",\"features\":[]}";
    private static final String STYLE = "https://tiles.openfreemap.org/styles/liberty";

    private final Activity host;
    private MapView view;
    private MapLibreMap map;
    private Status status;
    private boolean closed;
    private boolean styleReady;
    private final AtomicBoolean pending = new AtomicBoolean();

    public OsmRoutingPreviewEngine(Activity activity) {host = activity;}

    @Override public boolean configured() {return true;}

    @Override public View createNavigationView(Bundle saved, Status listener) {
        status = listener;
        MapLibre.getInstance(host);
        FrameLayout root = new FrameLayout(host);
        view = new MapView(host);
        root.addView(view, new FrameLayout.LayoutParams(-1,-1));

        TextView credits = new TextView(host);
        credits.setText("© OpenStreetMap contributors · OpenFreeMap");
        credits.setTextColor(Color.WHITE);
        credits.setTextSize(10f);
        credits.setPadding(12,6,12,6);
        credits.setBackgroundColor(0xbb08151c);
        FrameLayout.LayoutParams label = new FrameLayout.LayoutParams(-2,-2,
            Gravity.BOTTOM | Gravity.LEFT);
        label.setMargins(8,8,8,28);
        root.addView(credits,label);
        credits.setOnClickListener(v -> new android.app.AlertDialog.Builder(host)
            .setTitle("Kartendaten und Quellen")
            .setMessage("Kartendaten © OpenStreetMap contributors (ODbL). "
                + "Karten: OpenFreeMap / OpenMapTiles. "
                + "https://www.openstreetmap.org/copyright "
                + "und https://openfreemap.org")
            .setPositiveButton("Schließen", (d,w)->{})
            .show());

        view.onCreate(saved);
        view.getMapAsync(ml -> {
            if (closed) return;
            map = ml;
            map.setStyle(new Style.Builder().fromUri(STYLE), style -> {
                if (closed) return;
                styleReady = true;
                if (BRouterOfflineRouteClient.available(host)) {
                    status.onStatus("OSM-Karte bereit. BRouter kann eine echte "
                        + "Fahrroute offline planen. Noch keine Sprachführung.");
                } else {
                    status.onStatus("OSM-Karte bereit. Für Straßenrouten BRouter "
                        + "und regionale Offline-Routingdaten installieren. "
                        + "Keine Abbiegenavigation.");
                }
            });
        });
        return root;
    }

    @Override public void initializeAfterLocationPermission() {
        if (!closed && status != null) status.onStatus(
            "OSM-Kartenmodus: aktuelle GPS-Position für BRouter benötigt.");
    }
    @Override public boolean ready() {
        return !closed && styleReady && !pending.get()
            && BRouterOfflineRouteClient.available(host);
    }

    @Override public void startConfirmedRoute(double targetLat, double targetLon,
                                               String title) {
        if (!ready()) {
            status.onStatus("BRouter nicht bereit. App und regionale "
                + "Offline-Routingdaten prüfen.");
            return;
        }
        if (host.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            status.onStatus("GPS-Standort nicht freigegeben.");
            return;
        }
        if (!pending.compareAndSet(false,true)) {
            status.onStatus("BRouter-Routenberechnung läuft bereits.");
            return;
        }
        LocationManager lm=(LocationManager)host.getSystemService(Activity.LOCATION_SERVICE);
        String provider;
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
            provider=LocationManager.GPS_PROVIDER;
        else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            provider=LocationManager.NETWORK_PROVIDER;
        else {
            done("GPS ausgeschaltet. Keine Route berechnet.");
            return;
        }
        status.onStatus("Aktuelle Android-GPS-Position wird ermittelt …");
        try {
            lm.getCurrentLocation(provider,(CancellationSignal)null,
                host.getMainExecutor(), location -> {
                if (closed){pending.set(false);return;}
                if(location==null ||
                    Math.abs(System.currentTimeMillis()-location.getTime())>120000L ||
                    (location.hasAccuracy() && location.getAccuracy()>200f)) {
                    done("GPS-Position fehlt oder ungenau. Im Freien erneut versuchen.");
                    return;
                }
                status.onStatus("BRouter berechnet offline eine echte Straßenroute …");
                BRouterOfflineRouteClient.route(host, location.getLatitude(),
                    location.getLongitude(),targetLat,targetLon,
                    new BRouterOfflineRouteClient.Callback(){
                    @Override public void success(BRouterRouteGeometry.Route route){
                        if(closed){pending.set(false);return;}
                        renderRoute(route);
                        done("OSM-Straßenroute angezeigt (" + route.points.size()
                            + " Punkte). Noch KEINE Abbiege- oder Sprachnavigation!");
                    }
                    @Override public void failure(String error){done(error);}
                });
            });
        } catch (SecurityException | IllegalArgumentException failure) {
            done("GPS-Standort konnte nicht bestimmt werden.");
        }
    }

    private void renderRoute(BRouterRouteGeometry.Route route) {
        if(map==null || closed)return;
        map.getStyle(style -> {
            if(closed)return;
            GeoJsonSource source=(GeoJsonSource)style.getSource(SOURCE);
            if(source==null){
                source=new GeoJsonSource(SOURCE, route.geoJson);
                style.addSource(source);
                style.addLayer(new LineLayer(LAYER,SOURCE).withProperties(
                    lineColor(Color.rgb(65,225,160)),lineWidth(5f)));
            } else {
                source.setGeoJson(route.geoJson);
            }
            double[] first=route.points.get(0);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(first[0],first[1]),14.0));
        });
    }
    private void done(String text){
        pending.set(false);
        if(!closed && status!=null)status.onStatus(text);
    }

    @Override public void stopGuidanceAndClearRoute(){
        pending.set(false);
        if(map!=null && styleReady){
            map.getStyle(style -> {
                GeoJsonSource s=(GeoJsonSource)style.getSource(SOURCE);
                if(s!=null)s.setGeoJson(EMPTY);
            });
        }
        if(status!=null)status.onStatus(
            "OSM-Routenplanung gelöscht. Keine aktive Abbiegenavigation.");
    }
    @Override public void onStart(){if(view!=null)view.onStart();}
    @Override public void onResume(){if(view!=null)view.onResume();}
    @Override public void onPause(){if(view!=null)view.onPause();}
    @Override public void onStop(){if(view!=null)view.onStop();}
    @Override public void onDestroy(boolean finishing){
        closed=true;
        pending.set(false);
        if(view!=null){view.onDestroy();view=null;}
    }
    @Override public void onSaveInstanceState(Bundle saved){
        if(view!=null)view.onSaveInstanceState(saved);
    }
    @Override public void onConfigurationChanged(Configuration config){}
    @Override public void onTrimMemory(int level){
        if(view!=null)view.onLowMemory();
    }
}
