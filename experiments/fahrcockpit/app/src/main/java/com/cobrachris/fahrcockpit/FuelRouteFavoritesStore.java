package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Local-only station bookmarks grouped in user-named route profiles.
 * One station may belong to several routes; no background requests or
 * location history. Private Android storage never contains a Tankerkoenig key.
 */
final class FuelRouteFavoritesStore {
    private static final String PREFS = "fahrcockpit_fuel_route_favorites_v1";
    private static final String DATA = "data";
    private static final String ACTIVE_ROUTE = "active_route";
    static final int MAX_ROUTES = 20;
    static final int MAX_STATIONS_PER_ROUTE = 10; // Tankerkoenig prices.php cap.
    private static final int MAX_TOTAL_STATIONS = 150;
    private static final String ROUTES = "routes", STATIONS = "stations", MEMBERSHIPS = "routes";

    static final class Route {
        final String id, name;
        Route(String id, String name) {this.id=id;this.name=name;}
    }
    static final class Favorite {
        final String id, name, address, fuelType, status;
        final double lat, lng, price;
        final long checkedAt;
        final List<String> routeIds;
        Favorite(JSONObject j) {
            id=j.optString("id","");
            name=j.optString("name","Tankstelle");
            address=j.optString("address","");
            lat=j.optDouble("lat",Double.NaN);
            lng=j.optDouble("lng",Double.NaN);
            price=j.optDouble("price",-1);
            fuelType=j.optString("fuelType","");
            status=j.optString("status","unknown");
            checkedAt=j.optLong("checkedAt",0L);
            JSONArray membership=j.optJSONArray(MEMBERSHIPS);
            List<String> groups=new ArrayList<>();
            if(membership!=null)
                for(int i=0;i<membership.length();i++)groups.add(membership.optString(i,""));
            routeIds=Collections.unmodifiableList(groups);
        }
        boolean currentFor(String type,long now) {
            return "open".equals(status) && fuelType.equals(type)
                && Double.isFinite(price) && price>0 && price<9
                && checkedAt>0 && checkedAt<=now
                && now-checkedAt<15L*60_000L;
        }
        String priceText(String type,long now) {
            if(!"open".equals(status) && checkedAt>0) {
                boolean stale=checkedAt>now || now-checkedAt>=15L*60_000L;
                if("closed".equals(status))
                    return stale?"Geschlossen · ALT":"Geschlossen";
                if("no prices".equals(status))
                    return stale?"Kein Preis gemeldet · ALT":"Kein Preis gemeldet";
            }
            if(!fuelType.equals(type))return "Bitte "+type.toUpperCase(Locale.GERMANY)+" aktualisieren";
            if(!Double.isFinite(price)||price<=0||price>=9)return "Kein Preis gemeldet";
            String shown=FuelPriceFormat.perLiter(price);
            return currentFor(type,now)?shown:shown+" · ALT";
        }
    }

    private FuelRouteFavoritesStore(){}
    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }
    private static JSONObject read(Context c) {
        try {
            JSONObject j=new JSONObject(prefs(c).getString(DATA,"{}"));
            if(j.optJSONArray(ROUTES)==null)j.put(ROUTES,new JSONArray());
            if(j.optJSONArray(STATIONS)==null)j.put(STATIONS,new JSONArray());
            return j;
        }catch(Exception e){
            JSONObject fresh=new JSONObject();
            try{fresh.put(ROUTES,new JSONArray());fresh.put(STATIONS,new JSONArray());}
            catch(Exception ignored){}
            return fresh;
        }
    }
    private static boolean write(Context c,JSONObject data) {
        return prefs(c).edit().putString(DATA,data.toString()).commit();
    }
    private static boolean containsRoute(JSONArray routes,String id){
        if(id==null||id.isEmpty())return false;
        for(int i=0;i<routes.length();i++)
            if(routes.optJSONObject(i)!=null
                    && id.equals(routes.optJSONObject(i).optString("id","")))return true;
        return false;
    }
    static List<Route> routes(Context c) {
        List<Route> result=new ArrayList<>();
        JSONArray entries=read(c).optJSONArray(ROUTES);
        for(int i=0;i<entries.length();i++){
            JSONObject row=entries.optJSONObject(i);
            if(row!=null && FuelRules.validKey(row.optString("id","")))
                result.add(new Route(row.optString("id"),row.optString("name","Strecke")));
        }
        return result;
    }
    static String active(Context c){
        String id=prefs(c).getString(ACTIVE_ROUTE,"");
        return containsRoute(read(c).optJSONArray(ROUTES),id)?id:"";
    }
    static boolean activate(Context c,String id){
        if(id!=null && !id.isEmpty()
                && !containsRoute(read(c).optJSONArray(ROUTES),id))return false;
        return prefs(c).edit().putString(ACTIVE_ROUTE,id==null?"":id).commit();
    }
    static synchronized String createRoute(Context c,String raw){
        if(raw==null)return null;
        String name=raw.trim().replaceAll("\\s+"," ");
        if(name.isEmpty()||name.length()>65||name.contains("\n"))return null;
        JSONObject data=read(c);
        JSONArray routes=data.optJSONArray(ROUTES);
        if(routes.length()>=MAX_ROUTES)return null;
        for(int i=0;i<routes.length();i++){
            JSONObject row=routes.optJSONObject(i);
            if(row!=null && name.equalsIgnoreCase(row.optString("name","")))return null;
        }
        String id=UUID.randomUUID().toString();
        try{
            routes.put(new JSONObject().put("id",id).put("name",name));
        }catch(Exception invalid){return null;}
        return write(c,data)?id:null;
    }
    static synchronized boolean removeRoute(Context c,String id){
        JSONObject data=read(c);
        JSONArray oldRoutes=data.optJSONArray(ROUTES);
        if(!containsRoute(oldRoutes,id))return false;
        JSONArray nextRoutes=new JSONArray();
        JSONArray entries=data.optJSONArray(STATIONS);
        JSONArray nextStations=new JSONArray();
        try{
            for(int i=0;i<oldRoutes.length();i++){
                JSONObject route=oldRoutes.optJSONObject(i);
                if(route!=null&&!id.equals(route.optString("id")))nextRoutes.put(route);
            }
            for(int i=0;i<entries.length();i++){
                JSONObject row=entries.optJSONObject(i);
                if(row==null)continue;
                JSONArray groups=row.optJSONArray(MEMBERSHIPS),kept=new JSONArray();
                if(groups!=null)for(int j=0;j<groups.length();j++){
                    String g=groups.optString(j,"");
                    if(!id.equals(g))kept.put(g);
                }
                if(kept.length()>0){row.put(MEMBERSHIPS,kept);nextStations.put(row);}
            }
            data.put(ROUTES,nextRoutes);
            data.put(STATIONS,nextStations);
        }catch(Exception invalid){return false;}
        if(!write(c,data))return false;
        if(id.equals(active(c)))activate(c,"");
        // active() checks available routes; clear orphaned selection explicitly.
        if(id.equals(prefs(c).getString(ACTIVE_ROUTE,"")))activate(c,"");
        return true;
    }
    static List<Favorite> favorites(Context c,String groupId){
        List<Favorite> items=new ArrayList<>();
        JSONArray entries=read(c).optJSONArray(STATIONS);
        for(int i=0;i<entries.length();i++){
            JSONObject item=entries.optJSONObject(i);
            if(item==null)continue;
            Favorite fav=new Favorite(item);
            if(!FuelRules.validKey(fav.id))continue;
            if(groupId==null||groupId.isEmpty()||fav.routeIds.contains(groupId))
                items.add(fav);
        }
        return items;
    }
    static synchronized boolean add(Context c,FuelMapSnapshot.Station station,String groupId,
                       String fuelType,long priceFetchedAt) {
        if(station==null||!FuelRules.validKey(station.id)||station.name==null
                ||station.name.isEmpty()||FuelRules.stationCoordinates(station.lat,station.lng)==null)
            return false;
        if(!"diesel".equals(fuelType)&&!"e5".equals(fuelType)&&!"e10".equals(fuelType))
            return false;
        JSONObject data=read(c);
        if(!containsRoute(data.optJSONArray(ROUTES),groupId))return false;
        JSONArray entries=data.optJSONArray(STATIONS);
        JSONObject row=null;
        int inGroup=0;
        for(int i=0;i<entries.length();i++){
            JSONObject e=entries.optJSONObject(i);
            if(e==null)continue;
            if(station.id.equalsIgnoreCase(e.optString("id","")))row=e;
            JSONArray groups=e.optJSONArray(MEMBERSHIPS);
            if(groups!=null)for(int j=0;j<groups.length();j++)
                if(groupId.equals(groups.optString(j)))inGroup++;
        }
        if(row==null && entries.length()>=MAX_TOTAL_STATIONS)return false;
        JSONArray groupIds=row==null?new JSONArray():row.optJSONArray(MEMBERSHIPS);
        if(groupIds==null)groupIds=new JSONArray();
        boolean linked=false;
        for(int j=0;j<groupIds.length();j++)
            if(groupId.equals(groupIds.optString(j)))linked=true;
        if(!linked&&inGroup>=MAX_STATIONS_PER_ROUTE)return false;
        try{
            if(!linked)groupIds.put(groupId);
            if(row==null)row=new JSONObject();
            row.put("id",station.id.toLowerCase(Locale.US));
            row.put("name",station.name.length()>90?station.name.substring(0,90):station.name);
            row.put("address",station.address==null?"":station.address.length()>150
                ?station.address.substring(0,150):station.address);
            row.put("lat",station.lat);row.put("lng",station.lng);
            row.put(MEMBERSHIPS,groupIds);
            // Do not overwrite a newer verified manual price with stale map data.
            long previous=row.optLong("checkedAt",0L);
            if(priceFetchedAt>previous && priceFetchedAt<=System.currentTimeMillis()){
                row.put("price",station.price);
                row.put("checkedAt",priceFetchedAt);
                row.put("fuelType",fuelType);
                row.put("status",station.isOpen?"open":"closed");
            }
            if(!exists(entries,station.id))entries.put(row);
        }catch(Exception e){return false;}
        return write(c,data);
    }
    private static boolean exists(JSONArray entries,String id){
        for(int i=0;i<entries.length();i++)
            if(entries.optJSONObject(i)!=null
                    && id.equalsIgnoreCase(
                        entries.optJSONObject(i).optString("id","")))return true;
        return false;
    }
    static synchronized boolean remove(Context c,String stationId,String groupId) {
        JSONObject data=read(c);
        JSONArray entries=data.optJSONArray(STATIONS),result=new JSONArray();
        boolean changed=false;
        try{
            for(int i=0;i<entries.length();i++){
                JSONObject row=entries.optJSONObject(i);
                if(row==null)continue;
                if(!stationId.equalsIgnoreCase(row.optString("id",""))){
                    result.put(row);continue;
                }
                changed=true;
                if(groupId==null||groupId.isEmpty())continue;
                JSONArray groups=row.optJSONArray(MEMBERSHIPS),keep=new JSONArray();
                if(groups!=null)for(int j=0;j<groups.length();j++){
                    String id=groups.optString(j,"");
                    if(!groupId.equals(id))keep.put(id);
                }
                if(keep.length()>0){row.put(MEMBERSHIPS,keep);result.put(row);}
            }
            data.put(STATIONS,result);
        }catch(Exception e){return false;}
        return changed&&write(c,data);
    }
    static synchronized int applyPrices(Context c,String groupId,String fuelType,
                           JSONObject apiPrices,long checkedAt) {
        if(apiPrices==null||checkedAt<=0||checkedAt>System.currentTimeMillis()
                ||!containsRoute(read(c).optJSONArray(ROUTES),groupId)
                ||(!"diesel".equals(fuelType)&&!"e5".equals(fuelType)&&!"e10".equals(fuelType)))
            return -1;
        JSONObject data=read(c);
        JSONArray entries=data.optJSONArray(STATIONS);
        int updated=0;
        try{
            for(int i=0;i<entries.length();i++){
                JSONObject row=entries.optJSONObject(i);
                if(row==null)continue;
                String id=row.optString("id","");
                if(!FuelRules.validKey(id))continue;
                JSONArray groups=row.optJSONArray(MEMBERSHIPS);
                boolean member=false;
                if(groups!=null)for(int j=0;j<groups.length();j++)
                    if(groupId.equals(groups.optString(j)))member=true;
                if(!member)continue;
                JSONObject price=apiPrices.optJSONObject(id);
                if(price==null)continue;
                String status=price.optString("status","");
                if(!"open".equals(status)&&!"closed".equals(status)
                        &&!"no prices".equals(status))continue;
                double amount=price.optDouble(fuelType,-1d);
                boolean usable="open".equals(status)&&Double.isFinite(amount)
                    &&amount>0d&&amount<9d;
                row.put("price",usable?amount:-1d);
                row.put("fuelType",fuelType);
                row.put("status",usable?"open":status);
                row.put("checkedAt",checkedAt);
                updated++;
            }
        }catch(Exception e){return -1;}
        if(updated>0&&!write(c,data))return -1;
        return updated;
    }
}
