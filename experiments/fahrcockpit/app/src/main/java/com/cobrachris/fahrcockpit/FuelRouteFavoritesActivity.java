package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Full-screen route-filtered favorites. None of its UI changes cockpit
 * map size or native Google guidance, and no price query starts on opening.
 */
public final class FuelRouteFavoritesActivity extends Activity {
    private Spinner routeSpinner;
    private LinearLayout stationList;
    private TextView info;
    private Button fetchPrices, deleteRoute;
    private final FuelRoutePriceLookup[] lookup={null};
    private String selectedRoute="";
    private boolean populating;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout root=Ui.column(this);
        root.setPadding(Ui.dp(this,16),Ui.dp(this,8),
            Ui.dp(this,16),Ui.dp(this,24));
        scroll.addView(root);
        setContentView(scroll);
        getWindow().getDecorView().setOnApplyWindowInsetsListener((view,insets)->{
            android.graphics.Insets bars=insets.getInsets(
                WindowInsets.Type.systemBars());
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            return insets;
        });

        Button back=Ui.button(this,"← Zurück zu FahrCockpit");
        back.setOnClickListener(v->finish());
        root.addView(back);
        root.addView(Ui.text(this,"★ Strecken & Tankstellenfavoriten",
            23,Ui.FG,true));
        root.addView(Ui.text(this,
            "Eigene Streckenprofile statt einer langen Favoritenliste. "
            +"Eine Station kann zu mehreren Strecken gehören. "
            +"Vor der Abfahrt Preise je Strecke ausdrücklich abrufen.",
            13,Ui.MUTED,false));

        LinearLayout routeRow=Ui.row(this);
        routeSpinner=new Spinner(this);
        routeSpinner.setContentDescription("Streckenprofil auswählen");
        routeRow.addView(routeSpinner,new LinearLayout.LayoutParams(0,
            Ui.dp(this,50),1f));
        Button newRoute=Ui.button(this,"+ Strecke");
        newRoute.setOnClickListener(v->createRouteDialog(null));
        routeRow.addView(newRoute,new LinearLayout.LayoutParams(
            Ui.dp(this,120),Ui.dp(this,50)));
        root.addView(routeRow);

        deleteRoute=Ui.button(this,"Ausgewählte Strecke löschen");
        deleteRoute.setOnClickListener(v->confirmDeleteRoute());
        root.addView(deleteRoute);

        info=Ui.text(this,"",13,Ui.WARN,false);
        root.addView(info);

        fetchPrices=Ui.button(this,"Preise dieser Strecke jetzt vergleichen");
        fetchPrices.setOnClickListener(v->checkCurrentPrices());
        root.addView(fetchPrices);

        Button find=Ui.button(this,"Tankstellen suchen und ★ speichern");
        find.setOnClickListener(v->startActivity(new Intent(this,FuelActivity.class)));
        root.addView(find);

        root.addView(Ui.text(this,
            "Datenquelle: Tankerkönig (CC BY 4.0) · freiwillige Preisangaben "
            +"ohne Aktualitätsgarantie.",
            12,Ui.MUTED,false));
        Button attribution=Ui.button(this,"Tankerkönig – Datenquelle");
        attribution.setOnClickListener(v->{
            android.content.Intent source=new android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://creativecommons.tankerkoenig.de/"));
            try{startActivity(source);}
            catch(android.content.ActivityNotFoundException ignored){
                Toast.makeText(this,"Kein Browser verfügbar.",Toast.LENGTH_LONG).show();
            }
        });
        root.addView(attribution);
        root.addView(Ui.text(this,
            "Nur die angezeigte Strecke wird geprüft: maximal 10 bekannte "
            +"Tankstellen-IDs in einer Tankerkönig-Anfrage. Keine Hintergrundabfragen. "
            +"Preisstand, Öffnungsstatus und fehlende Werte werden gekennzeichnet. "
            +"Der günstigste Preis ist nicht automatisch der beste Umweg.",
            12,Ui.MUTED,false));
        stationList=Ui.column(this);
        root.addView(stationList,new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        routeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> parent,View view,
                                                   int position,long id){
                if(populating)return;
                List<FuelRouteFavoritesStore.Route> groups=
                    FuelRouteFavoritesStore.routes(FuelRouteFavoritesActivity.this);
                selectedRoute=position==0||position-1>=groups.size()?""
                    :groups.get(position-1).id;
                FuelRouteFavoritesStore.activate(FuelRouteFavoritesActivity.this,
                    selectedRoute);
                render();
            }
            @Override public void onNothingSelected(AdapterView<?> parent){}
        });
        reloadRoutes(FuelRouteFavoritesStore.active(this));
        lookup[0]=new FuelRoutePriceLookup(this);
    }

    @Override protected void onResume(){
        super.onResume();
        // A station search can create a new route in FuelActivity. Refresh
        // spinner options as well as cards after returning to this screen.
        if(routeSpinner!=null)
            reloadRoutes(FuelRouteFavoritesStore.active(this));
    }

    static void chooseRoute(Context host,FuelMapSnapshot.Station station,
                             String fuelType,long fetchedAt,Runnable after){
        if(station==null||!FuelRules.validKey(station.id)){
            Toast.makeText(host,
                "Tankstelle hat noch keine ID. Bitte Preise einmal neu abrufen.",
                Toast.LENGTH_LONG).show();
            return;
        }
        List<FuelRouteFavoritesStore.Route> options=FuelRouteFavoritesStore.routes(host);
        String[] names=new String[options.size()+1];
        for(int i=0;i<options.size();i++)names[i]=options.get(i).name;
        names[options.size()]="+ Neue Strecke erstellen";
        new AlertDialog.Builder(host)
            .setTitle("★ "+station.name+" zu Strecke hinzufügen")
            .setItems(names,(dialog,which)->{
                if(which==options.size()){
                    promptNewRoute(host,id->{
                        if(id!=null)saveInto(host,station,id,fuelType,fetchedAt,after);
                    });
                }else {
                    saveInto(host,station,options.get(which).id,fuelType,fetchedAt,after);
                }
            })
            .setNegativeButton("Abbrechen",null).show();
    }

    private static void saveInto(Context host,FuelMapSnapshot.Station station,
                                 String route,String fuelType,long fetchedAt,Runnable after){
        boolean success=FuelRouteFavoritesStore.add(host,station,route,
            fuelType,fetchedAt);
        if(success)FuelRouteFavoritesStore.activate(host,route);
        Toast.makeText(host,success?"Tankstelle als Streckenfavorit gespeichert."
                :"Nicht gespeichert: maximal 10 pro Strecke oder Speicherfehler.",
            Toast.LENGTH_LONG).show();
        if(success&&after!=null)after.run();
    }

    private interface Created {void accept(String id);}
    private static void promptNewRoute(Context host,Created created){
        EditText name=new EditText(host);
        name.setSingleLine(true);
        name.setHint("z. B. Oldenburg (Holstein) ↔ Hamburg");
        name.setContentDescription("Name des Streckenprofils");
        new AlertDialog.Builder(host)
            .setTitle("Neue Strecke")
            .setMessage("Hin- und Rückfahrt können dieselben Tankstellenfavoriten nutzen.")
            .setView(name)
            .setNegativeButton("Abbrechen",null)
            .setPositiveButton("Strecke erstellen",(dialog,which)->{
                String id=FuelRouteFavoritesStore.createRoute(host,
                    name.getText().toString());
                if(id==null)Toast.makeText(host,
                    "Name ungültig, doppelt vergeben oder Speicher voll.",
                    Toast.LENGTH_LONG).show();
                created.accept(id);
            })
            .show();
    }
    private void createRouteDialog(String unused) {
        promptNewRoute(this,id->{
            if(id!=null){
                FuelRouteFavoritesStore.activate(this,id);
                reloadRoutes(id);
            }
        });
    }

    private void reloadRoutes(String target){
        List<FuelRouteFavoritesStore.Route> groups=FuelRouteFavoritesStore.routes(this);
        List<String> names=new ArrayList<>();
        names.add("Alle Favoriten");
        int selected=0;
        for(int i=0;i<groups.size();i++){
            names.add(groups.get(i).name);
            if(groups.get(i).id.equals(target))selected=i+1;
        }
        populating=true;
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item,names);
        routeSpinner.setAdapter(adapter);
        routeSpinner.setSelection(selected);
        selectedRoute=selected==0?"":groups.get(selected-1).id;
        populating=false;
        render();
    }

    private void render(){
        if(stationList==null)return;
        List<FuelRouteFavoritesStore.Favorite> items=
            FuelRouteFavoritesStore.favorites(this,selectedRoute);
        String fuelType=FuelQuickSettings.type(this);
        final long now=System.currentTimeMillis();
        items.sort(Comparator.comparingInt((FuelRouteFavoritesStore.Favorite fav)->
                fav.currentFor(fuelType,now)?0:1)
            .thenComparingDouble(fav->fav.currentFor(fuelType,now)
                ?fav.price:Double.POSITIVE_INFINITY)
            .thenComparing(fav->fav.name,String.CASE_INSENSITIVE_ORDER));
        stationList.removeAllViews();
        deleteRoute.setVisibility(selectedRoute.isEmpty()?View.GONE:View.VISIBLE);
        fetchPrices.setEnabled(!selectedRoute.isEmpty()&&!items.isEmpty()
            &&(lookup[0]==null||!lookup[0].isRunning()));
        String description=selectedRoute.isEmpty()
            ?items.size()+" gespeicherte Favoriten in allen Strecken"
            :items.size()+" / "+FuelRouteFavoritesStore.MAX_STATIONS_PER_ROUTE
                +" Favoriten · "+fuelType.toUpperCase(Locale.GERMANY);
        info.setText(description);
        if(items.isEmpty()){
            stationList.addView(Ui.text(this,
                selectedRoute.isEmpty()
                    ?"Noch keine Favoriten. Neue Strecke erstellen und Stationen mit ★ speichern."
                    :"Noch keine Stationen zugeordnet. Über Tankstellensuche mit ★ speichern.",
                14,Ui.MUTED,false));
        }
        int checked=0;
        for(FuelRouteFavoritesStore.Favorite fav:items) {
            if(fav.currentFor(fuelType,now))checked++;
            LinearLayout card=Ui.column(this);
            card.setPadding(Ui.dp(this,10),Ui.dp(this,7),
                Ui.dp(this,10),Ui.dp(this,7));
            card.setBackground(Ui.background(this,Ui.PANEL,10));
            card.addView(Ui.text(this,fav.name,16,Ui.FG,true));
            card.addView(Ui.text(this,fav.address,12,Ui.MUTED,false));
            String price=fav.priceText(fuelType,now);
            String at=fav.checkedAt>0?" · Preisstand "
                +DateFormat.getDateTimeInstance(DateFormat.SHORT,
                    DateFormat.SHORT,Locale.GERMANY)
                     .format(new java.util.Date(fav.checkedAt)):" · kein Preisstand";
            TextView line=Ui.text(this,price+at,13,
                fav.currentFor(fuelType,now)?Ui.ACCENT:Ui.WARN,true);
            card.addView(line);
            LinearLayout buttons=Ui.row(this);
            Button navigate=Ui.button(this,"Als Ziel");
            navigate.setTextSize(12);
            navigate.setOnClickListener(v->chooseDestination(fav));
            buttons.addView(navigate,new LinearLayout.LayoutParams(0,
                Ui.dp(this,44),1f));
            Button attach=Ui.button(this,"Zuordnen");
            attach.setTextSize(12);
            attach.setOnClickListener(v->chooseRoute(this,
                new FuelMapSnapshot.Station(fav.id,fav.name,fav.address,
                    fav.lat,fav.lng,fav.price,"open".equals(fav.status)),
                fav.fuelType,fav.checkedAt,
                ()->reloadRoutes(FuelRouteFavoritesStore.active(this))));
            buttons.addView(attach,new LinearLayout.LayoutParams(0,
                Ui.dp(this,44),1f));
            Button remove=Ui.button(this,"Entfernen");
            remove.setTextSize(12);
            remove.setOnClickListener(v->confirmRemove(fav));
            buttons.addView(remove,new LinearLayout.LayoutParams(0,
                Ui.dp(this,44),1f));
            card.addView(buttons);
            stationList.addView(card);
            Ui.gap(this,stationList,7);
        }
        if(!selectedRoute.isEmpty()&&checked>0){
            stationList.addView(Ui.text(this,checked+
                " Preise innerhalb der letzten 15 Minuten geprüft. "
                +"Für einen vollständigen aktuellen Vergleich auf "
                +"„Preise dieser Strecke jetzt vergleichen“ tippen.",
                12,Ui.MUTED,false));
        }
    }

    private void chooseDestination(FuelRouteFavoritesStore.Favorite fav){
        if(FuelRules.stationCoordinates(fav.lat,fav.lng)==null)return;
        Intent selected=new Intent();
        selected.putExtra(FuelActivity.RESULT_LATITUDE,fav.lat);
        selected.putExtra(FuelActivity.RESULT_LONGITUDE,fav.lng);
        selected.putExtra(FuelActivity.RESULT_NAME,fav.name);
        setResult(RESULT_OK,selected);
        finish();
    }
    private void confirmRemove(FuelRouteFavoritesStore.Favorite fav){
        new AlertDialog.Builder(this)
            .setTitle("Favorit entfernen?")
            .setMessage(selectedRoute.isEmpty()
                ?"Aus allen Strecken entfernen: "+fav.name+"?"
                :"Nur aus dieser Strecke entfernen: "+fav.name+"?")
            .setNegativeButton("Abbrechen",null)
            .setPositiveButton("Entfernen",(dialog,which)->{
                if(!FuelRouteFavoritesStore.remove(this,fav.id,selectedRoute))
                    Toast.makeText(this,"Änderung konnte nicht gespeichert werden.",
                        Toast.LENGTH_LONG).show();
                render();
            }).show();
    }
    private void confirmDeleteRoute(){
        List<FuelRouteFavoritesStore.Route> groups=FuelRouteFavoritesStore.routes(this);
        FuelRouteFavoritesStore.Route match=null;
        for(FuelRouteFavoritesStore.Route g:groups)
            if(g.id.equals(selectedRoute))match=g;
        if(match==null)return;
        final FuelRouteFavoritesStore.Route doomed=match;
        new AlertDialog.Builder(this)
            .setTitle("Strecke löschen?")
            .setMessage("Nur „"+doomed.name+"“ wird gelöscht. "
                +"Tankstellen bleiben in ihren anderen Strecken erhalten.")
            .setNegativeButton("Abbrechen",null)
            .setPositiveButton("Löschen",(dialog,which)->{
                if(FuelRouteFavoritesStore.removeRoute(this,doomed.id))
                    reloadRoutes("");
                else Toast.makeText(this,"Strecke nicht gelöscht.",
                    Toast.LENGTH_LONG).show();
            }).show();
    }
    private void checkCurrentPrices(){
        if(selectedRoute.isEmpty()||lookup[0]==null||lookup[0].isRunning())return;
        fetchPrices.setEnabled(false);
        info.setText("Preise der gespeicherten Streckentankstellen werden abgerufen …");
        final String requestedRoute = selectedRoute;
        lookup[0].refresh(requestedRoute,FuelQuickSettings.type(this),(ok,feedback)->{
            render();
            // The user may select a different route before the request returns.
            // Never describe the newly selected route as freshly updated.
            if (requestedRoute.equals(selectedRoute))
                info.setText(feedback);
        });
    }
}
