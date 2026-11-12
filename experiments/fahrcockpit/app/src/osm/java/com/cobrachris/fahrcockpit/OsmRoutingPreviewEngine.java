package com.cobrachris.fahrcockpit;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Looper;
import android.os.SystemClock;
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
 * EXPERIMENTAL: native OSM/BRouter turn guidance, foreground location tracking,
 * local German TTS and route deviation rerouting. NOT approved for real driving
 * until live-device testing, offline map plan and route accuracy validation.
 *
 * Google Navigation remains the preferred provider in IntegratedNavigationActivity.
 * All BRouter requests use independent Android GPS + user-submitted destinations.
 * Google route/Places/SDK data are NEVER passed to OSM.
 */
public final class OsmRoutingPreviewEngine implements NavigationEngine {
    private static final String SOURCE = "fahrcockpit-brouter-route-source";
    private static final String LAYER = "fahrcockpit-brouter-route-line";
    private static final String EMPTY =
        "{\"type\":\"FeatureCollection\",\"features\":[]}";
    private static final String STYLE = "https://tiles.openfreemap.org/styles/liberty";
    private static final long REROUTE_COOLDOWN_MS = 60000L;

    private final Activity host;
    private MapView view;
    private MapLibreMap map;
    private Status status;
    private OsmSpeechGuide voice;
    private OsmTurnGuidance guidance;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean closed;
    private boolean foreground;
    private boolean styleReady;
    private boolean hasTarget;
    private final AtomicBoolean pending = new AtomicBoolean();
    private int requestEpoch;
    private double destinationLat, destinationLon;
    private long lastRerouteElapsed;
    private String lastVisualInstruction;

    public OsmRoutingPreviewEngine(Activity activity) { host=activity; }
    @Override public boolean configured() { return true; }

    @Override public View createNavigationView(Bundle saved, Status listener) {
        status=listener;
        voice=new OsmSpeechGuide(host,status);
        MapLibre.getInstance(host);
        FrameLayout container=new FrameLayout(host);
        view=new MapView(host);
        container.addView(view,new FrameLayout.LayoutParams(-1,-1));
        TextView attribution=new TextView(host);
        attribution.setText("© OpenStreetMap contributors · OpenFreeMap");
        attribution.setTextColor(Color.WHITE);
        attribution.setTextSize(10f);
        attribution.setPadding(12,6,12,6);
        attribution.setBackgroundColor(0xbb08151c);
        FrameLayout.LayoutParams credit = new FrameLayout.LayoutParams(-2,-2,
            Gravity.BOTTOM|Gravity.LEFT);
        credit.setMargins(8,8,8,28);
        container.addView(attribution,credit);
        attribution.setOnClickListener(v -> new android.app.AlertDialog.Builder(host)
            .setTitle("OSM Kartendaten")
            .setMessage("© OpenStreetMap contributors (ODbL), Karten: OpenFreeMap. "
                + "https://www.openstreetmap.org/copyright")
            .setPositiveButton("OK",(d,w)->{})
            .show());
        view.onCreate(saved);
        view.getMapAsync(ml -> {
            if(closed)return;
            map=ml;
            map.setStyle(new Style.Builder().fromUri(STYLE), style -> {
                if(closed)return;
                styleReady=true;
                status.onStatus(BRouterOfflineRouteClient.available(host)
                    ? "OSM-Karte bereit. BRouter-Abbiegeführung im Entwicklungstest. "
                        + "Nicht während der Fahrt benutzen."
                    : "OSM-Karte bereit. BRouter und regionale Offline-Routingdaten "
                        + "fehlen noch. Keine Fahrnavigation.");
            });
        });
        return container;
    }

    @Override public void initializeAfterLocationPermission() {
        if(!closed && status!=null)
            status.onStatus("GPS-Freigabe erkannt. OSM-Testroute kann geplant werden.");
    }

    @Override public boolean ready() {
        return !closed && styleReady && !pending.get()
            && BRouterOfflineRouteClient.available(host);
    }

    @Override public void startConfirmedRoute(double lat,double lon,String label) {
        if(!ready()) {
            status.onStatus("BRouter nicht bereit. App und regionale "
                + "Offline-Routingdaten prüfen.");
            return;
        }
        if(!Double.isFinite(lat)||!Double.isFinite(lon)
                ||lat < -90||lat > 90||lon < -180||lon > 180) {
            status.onStatus("Ungültige Zielkoordinaten.");
            return;
        }
        if(host.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            status.onStatus("GPS-Standortberechtigung fehlt.");
            return;
        }
        if(!pending.compareAndSet(false,true))return;
        hasTarget=true;
        destinationLat=lat;
        destinationLon=lon;
        guidance=null;
        stopGpsTracking();
        if(voice!=null)voice.stop();
        final int epoch=++requestEpoch;
        LocationManager lm=locationManager();
        String provider;
        if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
            provider=LocationManager.GPS_PROVIDER;
        else if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            provider=LocationManager.NETWORK_PROVIDER;
        else {
            done("GPS ausgeschaltet. Keine Route.");
            return;
        }
        status.onStatus("Aktuelle Android-Position für Offline-Route ermitteln …");
        try {
            lm.getCurrentLocation(provider,(CancellationSignal)null,
                host.getMainExecutor(),position -> {
                if(closed||epoch!=requestEpoch)return;
                if(!fresh(position,75f)) {
                    done("GPS-Position fehlt oder ist zu ungenau. "
                        + "Im Freien erneut versuchen.");
                    return;
                }
                calculateFrom(position,epoch,false);
            });
        } catch(SecurityException|IllegalArgumentException error) {
            done("GPS konnte nicht gelesen werden.");
        }
    }

    private LocationManager locationManager() {
        if(locationManager==null) {
            locationManager=(LocationManager)host.getSystemService(Activity.LOCATION_SERVICE);
        }
        return locationManager;
    }

    private static boolean fresh(Location loc,float maxAccuracy) {
        return loc!=null &&loc.hasAccuracy()
            &&loc.getAccuracy()<=maxAccuracy
            &&loc.getAccuracy()>=0
            &&Math.abs(System.currentTimeMillis()-loc.getTime())<15000L
            &&Double.isFinite(loc.getLatitude())&&Double.isFinite(loc.getLongitude());
    }

    private void calculateFrom(Location position,int epoch,boolean reroute) {
        if(closed||epoch!=requestEpoch||!hasTarget)return;
        status.onStatus(reroute
            ? "OSM: Route verlassen – BRouter berechnet lokal neu …"
            : "BRouter berechnet echte Straßenroute mit Abbiegehinweisen …");
        BRouterOfflineRouteClient.route(host,position.getLatitude(),
            position.getLongitude(),destinationLat,destinationLon,
            new BRouterOfflineRouteClient.Callback() {
            @Override public void success(BRouterRouteGeometry.Route route) {
                if(closed||epoch!=requestEpoch)return;
                renderRoute(route,epoch);
                pending.set(false);
                if(route.turns.isEmpty()) {
                    guidance=null;
                    stopGpsTracking();
                    status.onStatus("OSM-Route dargestellt. BRouter lieferte keine "
                        + "Abbiegehinweise: keine Sprachführung gestartet.");
                    return;
                }
                guidance=new OsmTurnGuidance(route);
                lastVisualInstruction=null;
                status.onStatus("OSM-Abbiegeführung im Entwicklungstest aktiv "
                    + "(" + route.turns.size() + " echte BRouter-Hinweise). "
                    + "Noch NICHT für Fahrten freigegeben.");
                if(foreground)startGpsTracking();
            }
            @Override public void failure(String explanation) {
                if(closed||epoch!=requestEpoch)return;
                pending.set(false);
                guidance=null;
                stopGpsTracking();
                status.onStatus(explanation);
            }
        });
    }

    private void startGpsTracking() {
        if(closed||!foreground||guidance==null||locationListener!=null)return;
        if(host.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                !=PackageManager.PERMISSION_GRANTED) {
            status.onStatus("GPS-Berechtigung entzogen – Führung angehalten.");
            return;
        }
        try {
            LocationManager lm=locationManager();
            if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                status.onStatus("GPS deaktiviert – Führung angehalten.");
                return;
            }
            locationListener=this::onGpsLocation;
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000L,3f,
                locationListener,Looper.getMainLooper());
        } catch(SecurityException|IllegalArgumentException failure) {
            locationListener=null;
            status.onStatus("GPS-Updates nicht verfügbar – Führung angehalten.");
        }
    }

    private void stopGpsTracking() {
        if(locationListener!=null) {
            try {locationManager().removeUpdates(locationListener);}
            catch(SecurityException ignored) {}
            locationListener=null;
        }
    }

    private void onGpsLocation(Location loc) {
        if(closed||!foreground||guidance==null||pending.get())return;
        if(!fresh(loc,35f))return; // never speak from uncertain/stale GPS
        OsmTurnGuidance.Step step=guidance.onPosition(
            loc.getLatitude(),loc.getLongitude(),loc.getAccuracy());
        if(!step.reliable)return;
        if(step.offRoute) {
            long now=SystemClock.elapsedRealtime();
            if(hasTarget&&!pending.get()
                    &&(lastRerouteElapsed==0 || now-lastRerouteElapsed>REROUTE_COOLDOWN_MS)
                    &&pending.compareAndSet(false,true)) {
                lastRerouteElapsed=now;
                int epoch=++requestEpoch;
                guidance=null;
                stopGpsTracking();
                if(voice!=null)voice.speak("Route verlassen. Route wird neu berechnet.");
                calculateFrom(loc,epoch,true);
            }
            return;
        }
        if(step.arrived) {
            stopGpsTracking();
            if(voice!=null)voice.speak("Ziel erreicht");
            status.onStatus("OSM-Testziel erreicht.");
            guidance=null;
            hasTarget=false;
            return;
        }
        if(step.instruction!=null
                &&!step.instruction.equals(lastVisualInstruction)) {
            lastVisualInstruction=step.instruction;
            status.onStatus("OSM-Turn-by-Turn (Test): "+step.instruction
                + " · nicht für den Straßenverkehr freigegeben");
        }
        if(step.speech!=null&&voice!=null)voice.speak(step.speech);
        if(map!=null&&styleReady) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(loc.getLatitude(),loc.getLongitude()),15.0));
        }
    }

    private void renderRoute(BRouterRouteGeometry.Route route,int epoch) {
        if(map==null||closed||epoch!=requestEpoch)return;
        map.getStyle(style -> {
            if(closed||epoch!=requestEpoch)return;
            GeoJsonSource source=(GeoJsonSource)style.getSource(SOURCE);
            if(source==null) {
                source=new GeoJsonSource(SOURCE,route.geoJson);
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

    private void done(String message) {
        pending.set(false);
        if(!closed&&status!=null)status.onStatus(message);
    }

    @Override public void stopGuidanceAndClearRoute() {
        requestEpoch++;
        pending.set(false);
        guidance=null;
        hasTarget=false;
        lastVisualInstruction=null;
        stopGpsTracking();
        if(voice!=null)voice.stop();
        if(map!=null&&styleReady) {
            map.getStyle(style -> {
                GeoJsonSource line=(GeoJsonSource)style.getSource(SOURCE);
                if(line!=null)line.setGeoJson(EMPTY);
            });
        }
        if(status!=null)status.onStatus("OSM-Testführung beendet.");
    }

    @Override public void onStart() {if(view!=null)view.onStart();}
    @Override public void onResume() {
        foreground=true;
        if(view!=null)view.onResume();
        startGpsTracking();
    }
    @Override public void onPause() {
        foreground=false;
        stopGpsTracking();
        if(voice!=null)voice.stop();
        if(view!=null)view.onPause();
    }
    @Override public void onStop() {if(view!=null)view.onStop();}
    @Override public void onDestroy(boolean finishing) {
        closed=true;
        requestEpoch++;
        pending.set(false);
        guidance=null;
        stopGpsTracking();
        if(voice!=null){voice.close();voice=null;}
        if(view!=null){view.onDestroy();view=null;}
    }
    @Override public void onSaveInstanceState(Bundle saved) {
        if(view!=null)view.onSaveInstanceState(saved);
    }
    @Override public void onConfigurationChanged(Configuration config) {}
    @Override public void onTrimMemory(int level) {
        if(view!=null)view.onLowMemory();
    }
}
