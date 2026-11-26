package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Exactly one explicit tap -> at most one Tankerkoenig prices.php request.
 * Up to ten saved station UUIDs per route, no GPS or route position uploaded.
 * Shares the existing persistent rate gate and fail-closed backoffs.
 */
final class FuelRoutePriceLookup {
    interface Result {
        void finished(boolean success,String description);
    }
    private final Activity host;
    private final Handler main=new Handler(Looper.getMainLooper());
    private boolean running;
    FuelRoutePriceLookup(Activity host){this.host=host;}
    boolean isRunning(){return running;}

    void refresh(String routeId,String fuelType,Result callback){
        if(running)return;
        if(!"diesel".equals(fuelType)&&!"e5".equals(fuelType)&&!"e10".equals(fuelType)){
            callback.finished(false,"Unbekannte Kraftstoffsorte.");return;
        }
        List<FuelRouteFavoritesStore.Favorite> favs=
            FuelRouteFavoritesStore.favorites(host,routeId);
        if(routeId==null||routeId.isEmpty()||favs.isEmpty()
                ||favs.size()>FuelRouteFavoritesStore.MAX_STATIONS_PER_ROUTE){
            callback.finished(false,"Strecke auswählen (maximal zehn Tankstellen).");return;
        }
        List<String> ids=new ArrayList<>();
        for(FuelRouteFavoritesStore.Favorite fav:favs){
            if(!FuelRules.validKey(fav.id)){
                callback.finished(false,"Ungültige Tankstellen-ID im Favoriten.");return;
            }
            if(!ids.contains(fav.id))ids.add(fav.id);
        }
        String key=host.getSharedPreferences("fuel_settings_v1",Context.MODE_PRIVATE)
            .getString("tankerkoenig_local_key","").trim();
        if(!FuelRules.validKey(key)){
            callback.finished(false,"Tankerkönig-Schlüssel fehlt: Einstellungen öffnen.");return;
        }
        // IDs and key are format-validated UUIDs: no untrusted URL parameters.
        String endpoint="https://creativecommons.tankerkoenig.de/json/prices.php?ids="
            +String.join(",",ids)+"&apikey="+key;
        running=true;
        new Thread(()->{
            HttpURLConnection connection=null;
            boolean ok=false;
            String message="Preisabruf fehlgeschlagen.";
            try {
                FuelRequestGate.Permit permit=FuelRequestGate.tryBegin(host);
                if(!permit.allowed) {
                    message="Noch kein neuer Abruf: "+permit.explanation();
                } else {
                    connection=(HttpURLConnection)new URL(endpoint).openConnection();
                    connection.setConnectTimeout(9_000);
                    connection.setReadTimeout(9_000);
                    connection.setRequestProperty("Accept","application/json");
                    int code=connection.getResponseCode();
                    FuelRequestGate.reportHttp(host,code);
                    if(code!=200)throw new IllegalStateException("Status "+code);
                    StringBuilder payload=new StringBuilder();
                    try(BufferedReader br=new BufferedReader(new InputStreamReader(
                            connection.getInputStream(),StandardCharsets.UTF_8))) {
                        String line;
                        while((line=br.readLine())!=null){
                            payload.append(line);
                            if(payload.length()>128_000)
                                throw new IllegalStateException("Response oversized");
                        }
                    }
                    JSONObject body=new JSONObject(payload.toString());
                    if(!body.optBoolean("ok",false))
                        throw new IllegalStateException("API refused");
                    JSONObject prices=body.optJSONObject("prices");
                    if(prices==null)throw new IllegalStateException("Missing prices");
                    long checkedAt=System.currentTimeMillis();
                    int updated=FuelRouteFavoritesStore.applyPrices(
                        host,routeId,fuelType,prices,checkedAt);
                    if(updated<0)throw new IllegalStateException("Local save failed");
                    ok=true;
                    message=updated+" von "+ids.size()+" Favoriten geprüft · "
                        +"Preisstand aktualisiert, soweit vom Anbieter verfügbar.";
                }
            }catch(Exception exception){
                FuelRequestGate.reportTransportFailure(host);
                message="Preisabruf nicht möglich oder Anbieter-Sperre aktiv. "
                    +"Keine neuen Preise gespeichert.";
            }finally {
                if(connection!=null)connection.disconnect();
            }
            final boolean complete=ok;
            final String feedback=message;
            main.post(()->{
                running=false;
                if(!host.isFinishing()&&!host.isDestroyed())
                    callback.finished(complete,feedback);
            });
        },"fuel-route-manual-price-check").start();
    }
}
