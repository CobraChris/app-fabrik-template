package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Opt-in update checker. Opens a verified GitHub RELEASE APK in the user's browser.
 * No background download, no install permission, no silent installation.
 *
 * A true in-place update additionally requires a stable private signing key,
 * which CI must receive through GitHub Actions secrets; never bundle keys here.
 */
public final class UpdateActivity extends Activity {
    private static final String RELEASES_API =
            "https://api.github.com/repos/CobraChris/app-fabrik-template/releases?per_page=20";
    private static final String ASSET_NAME = "sanftwecker-release.apk";
    private TextView status;
    private Button check, download;
    private String apkUrl;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        UiKit.window(this);
        LinearLayout root=UiKit.mainColumn(this);
        root.addView(UiKit.heading(this,"App aktualisieren"));
        ClockTabs.add(this,root,ClockTabs.ALARMS);
        root.addView(UiKit.label(this,"Installierte Version: "+
                getCurrentVersionName()+" (Build "+getCurrentVersion()+")",
                16,UiKit.TEXT));
        root.addView(UiKit.label(this,
                "Eine neue APK kann die vorhandene App ersetzen, wenn sie mit "+
                "demselben dauerhaften Schlüssel signiert wurde. "+
                "Deine gespeicherten Wecker und Berechtigungen bleiben bei einem "+
                "regulären Update normalerweise erhalten.",
                14,UiKit.MUTED));
        status=UiKit.label(this,"Noch nicht geprüft.",15,UiKit.TEXT);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=UiKit.dp(this,18);p.bottomMargin=UiKit.dp(this,14);
        root.addView(status,p);
        check=UiKit.button(this,"Nach Update suchen",true);
        root.addView(check);
        check.setOnClickListener(v->checkUpdates());
        download=UiKit.button(this,"APK im Browser herunterladen",false);
        download.setEnabled(false);
        root.addView(download);
        download.setOnClickListener(v->{
            if(apkUrl==null)return;
            try{
                startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(apkUrl))
                        .addCategory(Intent.CATEGORY_BROWSABLE));
            }catch(ActivityNotFoundException e){
                new AlertDialog.Builder(this)
                        .setMessage("Kein Browser verfügbar.")
                        .setPositiveButton("OK",null).show();
            }
        });
        root.addView(UiKit.label(this,
                "Android verlangt eventuell eine Bestätigung für die Installation. "+
                "Der Update-Button deinstalliert oder löscht die bestehende App niemals. "+
                "Wird die Installation wegen einer anderen Signatur verweigert, "+
                "nicht vorschnell deinstallieren: Dabei gehen die Weckerdaten verloren.",
                13,UiKit.MUTED));
        setContentView(UiKit.scroll(this,root));
    }

    private long getCurrentVersion(){
        try{return getPackageManager().getPackageInfo(getPackageName(),0)
                .getLongVersionCode();}
        catch(Exception ignored){return -1;}
    }
    private String getCurrentVersionName(){
        try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}
        catch(Exception ignored){return "unbekannt";}
    }

    private static final class Candidate {
        final long code; final String url;
        Candidate(long code,String url){this.code=code;this.url=url;}
    }

    /** Pure parser also used by unit tests. Rejects prerelease artifact links and other assets. */
    static Candidate newestSignedAsset(String raw){
        try{
            JSONArray releases=new JSONArray(raw);
            Candidate best=null;
            for(int i=0;i<releases.length();i++){
                JSONObject release=releases.getJSONObject(i);
                if(release.optBoolean("draft",true))continue;
                String tag=release.optString("tag_name","");
                long code=UpdateUrlPolicy.versionCode(tag);
                if(code<=0)continue;
                JSONArray assets=release.optJSONArray("assets");
                if(assets==null)continue;
                for(int j=0;j<assets.length();j++){
                    JSONObject asset=assets.getJSONObject(j);
                    if(!ASSET_NAME.equals(asset.optString("name","")))continue;
                    String url=asset.optString("browser_download_url","");
                    if(!UpdateUrlPolicy.isForTag(url,tag))continue;
                    if(best==null||code>best.code)best=new Candidate(code,url);
                }
            }
            return best;
        }catch(Exception e){return null;}
    }

    private void checkUpdates(){
        status.setText("Prüfe GitHub-Veröffentlichungen …");
        apkUrl=null;download.setEnabled(false);check.setEnabled(false);
        new Thread(()->{
            HttpURLConnection conn=null;
            Candidate candidate=null;
            String failure=null;
            try{
                conn=(HttpURLConnection)new URL(RELEASES_API).openConnection();
                conn.setRequestProperty("Accept","application/vnd.github+json");
                conn.setRequestProperty("User-Agent","Sanftwecker-UpdateCheck");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setInstanceFollowRedirects(false);
                if(conn.getResponseCode()!=200)
                    throw new IllegalStateException("GitHub HTTP "+conn.getResponseCode());
                StringBuilder json=new StringBuilder();
                try(InputStream in=conn.getInputStream();
                    BufferedReader reader=new BufferedReader(new InputStreamReader(in,
                            java.nio.charset.StandardCharsets.UTF_8))){
                    char[] buf=new char[2048];int count;
                    while((count=reader.read(buf))!=-1) {
                        json.append(buf,0,count);
                        if(json.length()>500_000)throw new IllegalStateException("Antwort zu groß");
                    }
                }
                candidate=newestSignedAsset(json.toString());
            }catch(Exception e){failure="Updateprüfung fehlgeschlagen. "+
                    "Bitte Internetverbindung prüfen.";}
            finally{if(conn!=null)conn.disconnect();}
            final Candidate selected=candidate;
            final String error=failure;
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                check.setEnabled(true);
                if(error!=null){status.setText(error);return;}
                if(selected==null){
                    status.setText("Noch keine dauerhaft signierte APK veröffentlicht. "+
                            "Versuche es später erneut oder prüfe die GitHub-Releases.");
                }else if(!UpdateUrlPolicy.isNewer(selected.code,getCurrentVersion())){
                    status.setText("Die installierte Version ist aktuell. "+
                            "(Veröffentlichter Build "+selected.code+")");
                }else{
                    status.setText("Update verfügbar: Build "+selected.code+
                            ". Android installiert es erst nach deiner Bestätigung.");
                    apkUrl=selected.url;
                    download.setEnabled(true);
                }
            });
        },"Sanftwecker-Update-Check").start();
    }
}
