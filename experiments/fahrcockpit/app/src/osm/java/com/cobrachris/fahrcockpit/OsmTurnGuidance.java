package com.cobrachris.fahrcockpit;

import java.util.List;

/**
 * Pure Java route-position tracking for independent BRouter routes.
 * Maneuvers are BRouter-provided voice hints, NEVER guessed from bends.
 * Location accuracy above 35m is refused for spoken instructions.
 *
 * Foreground-only pilot. No automatic driving or safety guarantees.
 */
final class OsmTurnGuidance {
    static final class Step {
        final String instruction;
        final String speech;
        final boolean offRoute;
        final boolean arrived;
        final boolean reliable;
        final double deviationMeters;
        final double metersToTurn;
        Step(String instruction, String speech, boolean offRoute,
             boolean arrived, boolean reliable, double deviationMeters,
             double metersToTurn) {
            this.instruction=instruction;
            this.speech=speech;
            this.offRoute=offRoute;
            this.arrived=arrived;
            this.reliable=reliable;
            this.deviationMeters=deviationMeters;
            this.metersToTurn=metersToTurn;
        }
    }

    private final BRouterRouteGeometry.Route route;
    private final double[] meters;
    private int segmentCursor;
    private int earlyAnnounced = -1;
    private int nearAnnounced = -1;
    private int offRouteCount;
    private boolean rerouteRequested;
    private boolean arrived;

    OsmTurnGuidance(BRouterRouteGeometry.Route route) {
        if (route == null || route.points.size() < 2)
            throw new IllegalArgumentException("Missing route");
        this.route = route;
        List<double[]> points = route.points;
        meters = new double[points.size()];
        for (int i=1;i<points.size();i++) {
            meters[i]=meters[i-1]+distance(points.get(i-1)[0],points.get(i-1)[1],
                                           points.get(i)[0],points.get(i)[1]);
        }
    }

    boolean hasManeuvers() { return !route.turns.isEmpty(); }

    Step onPosition(double lat, double lon, float accuracyMeters) {
        if (!Double.isFinite(lat)||!Double.isFinite(lon)
                ||lat < -90 ||lat > 90 ||lon < -180 ||lon > 180
                ||Float.isNaN(accuracyMeters)||accuracyMeters < 0
                ||accuracyMeters > 35f || arrived) {
            return new Step("GPS-Position nicht ausreichend genau",null,false,
                            arrived,false,Double.NaN,Double.NaN);
        }
        List<double[]> points=route.points;
        // Search near previous matched segment only; do not snap a distant
        // parallel road or a future repeated section to an unrelated turn.
        int lo=Math.max(0,segmentCursor-8);
        int hi=Math.min(points.size()-2,segmentCursor+350);
        double nearest=Double.POSITIVE_INFINITY;
        double position=0;
        int bestSegment=segmentCursor;
        for(int i=lo;i<=hi;i++){
            double[] a=points.get(i),b=points.get(i+1);
            double latScale=111195.0;
            double lonScale=latScale*Math.cos(Math.toRadians((a[0]+b[0]+lat)/3.0));
            double dx=(b[1]-a[1])*lonScale;
            double dy=(b[0]-a[0])*latScale;
            double px=(lon-a[1])*lonScale;
            double py=(lat-a[0])*latScale;
            double length2=dx*dx+dy*dy;
            double fraction=length2>0
                ?Math.max(0,Math.min(1,(px*dx+py*dy)/length2)):0;
            double d=Math.hypot(px-fraction*dx,py-fraction*dy);
            if(d<nearest){
                nearest=d;
                position=meters[i]+(meters[i+1]-meters[i])*fraction;
                bestSegment=i;
            }
        }
        if(nearest>65) {
            offRouteCount++;
            boolean reroute=offRouteCount>=3&&!rerouteRequested;
            if(reroute)rerouteRequested=true;
            return new Step("Route verlassen – sichere Neuberechnung",null,reroute,
                            false,true,nearest,Double.NaN);
        }
        offRouteCount=0;
        rerouteRequested=false;
        segmentCursor=Math.max(segmentCursor,bestSegment);
        double remaining=meters[meters.length-1]-position;
        if(bestSegment>=points.size()-2&&remaining<=30
                &&distance(lat,lon,points.get(points.size()-1)[0],
                            points.get(points.size()-1)[1])<=30){
            arrived=true;
            return new Step("Ziel erreicht","Ziel erreicht",false,true,true,nearest,0);
        }
        if(!hasManeuvers())
            return new Step("Route sichtbar – keine BRouter-Abbiegehinweise",
                            null,false,false,true,nearest,Double.NaN);
        for(int j=0;j<route.turns.size();j++){
            BRouterRouteGeometry.Turn turn=route.turns.get(j);
            double distance=meters[turn.pointIndex]-position;
            if(distance < -12)continue;
            String text=turn.germanInstruction();
            if(text==null)continue;
            String say=null;
            if(distance<=38&&distance>=-12 &&nearAnnounced!=j){
                nearAnnounced=j;
                say="Jetzt "+lowerFirst(text);
            } else if(distance<=220 &&distance>38 &&earlyAnnounced!=j){
                earlyAnnounced=j;
                int rounded=(int)(Math.round(distance/10.0)*10);
                say="In "+Math.max(10,rounded)+" Metern "+lowerFirst(text);
            }
            return new Step(text,say,false,false,true,nearest,Math.max(0,distance));
        }
        return new Step("Der Route weiter folgen",null,false,false,true,nearest,
                        Double.NaN);
    }

    private static String lowerFirst(String s) {
        return s.substring(0,1).toLowerCase(java.util.Locale.GERMAN)+s.substring(1);
    }

    static double distance(double aLat,double aLon,double bLat,double bLon) {
        double dLat=Math.toRadians(bLat-aLat);
        double dLon=Math.toRadians(bLon-aLon);
        double q=Math.sin(dLat/2)*Math.sin(dLat/2)
            +Math.cos(Math.toRadians(aLat))*Math.cos(Math.toRadians(bLat))
            *Math.sin(dLon/2)*Math.sin(dLon/2);
        return 2*6371000*Math.asin(Math.min(1,Math.sqrt(q)));
    }
}
