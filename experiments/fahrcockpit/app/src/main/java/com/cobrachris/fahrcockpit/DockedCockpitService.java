package com.cobrachris.fahrcockpit;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

/**
 * User-started 30% overlay LEFT of Google Maps (landscape), or below it (portrait).
 * Maps remains FULL-SCREEN behind the opaque controls; its window is never resized.
 * Starts only from an explicit user action; no boot receivers or vendor APIs.
 */
public final class DockedCockpitService extends Service {
    static final String SHOW = "com.cobrachris.fahrcockpit.DOCK_SHOW";
    static final String HIDE = "com.cobrachris.fahrcockpit.DOCK_HIDE";
    private static final String CHANNEL = "fahrcockpit_docked_v1";
    private WindowManager windows;
    private WindowManager.LayoutParams params;
    private LinearLayout panel;
    private ScrollView contents;
    private TextView track,elapsed,total;
    private SeekBar seekBar;
    private Button play,prev,next,minus,plus,fold;
    private boolean collapsed,scrubbing;
    private long duration = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (panel != null && !collapsed) updateMusic();
            if (panel != null) handler.postDelayed(this,1700L);
        }
    };

    @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onCreate() {
        super.onCreate();
        windows=(WindowManager)getSystemService(WINDOW_SERVICE);
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(manager!=null) manager.createNotificationChannel(new NotificationChannel(
            CHANNEL,"FahrCockpit Fahrmodus",NotificationManager.IMPORTANCE_LOW));
    }
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        if(intent!=null && HIDE.equals(intent.getAction())) {
            stopSelf(); return START_NOT_STICKY;
        }
        if(!Settings.canDrawOverlays(this)) {
            toast("Einblendung für FahrCockpit freigeben");
            stopSelf(); return START_NOT_STICKY;
        }
        try {
            startForeground(5112,notification());
            if(panel==null) attach();
        } catch(RuntimeException e) {
            toast("Fahrmodus konnte nicht geöffnet werden.");
            stopSelf();
        }
        return START_NOT_STICKY;
    }
    private Notification notification() {
        PendingIntent open=PendingIntent.getActivity(this,30,
            new Intent(this,MainActivity.class).setAction("FAHR_SETTINGS"),
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,31,
            new Intent(this,DockedCockpitService.class).setAction(HIDE),
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("FahrCockpit").setContentText("Fahrmodus läuft")
            .setContentIntent(open).setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                "Schließen",stop).build();
    }
    private Button button(String label) {
        Button b=Ui.button(this,label);
        b.setTextSize(12); b.setMinWidth(0); b.setPadding(0,0,0,0);
        return b;
    }
    private Button addButton(LinearLayout row,String title) {
        Button b=button(title);
        row.addView(b,new LinearLayout.LayoutParams(0,Ui.dp(this,47),1));
        return b;
    }
    private void attach() {
        params=new WindowManager.LayoutParams(1,1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.START;
        calculateBounds();
        panel=Ui.column(this);
        panel.setPadding(Ui.dp(this,7),Ui.dp(this,5),Ui.dp(this,7),Ui.dp(this,6));
        panel.setBackground(Ui.background(this,Ui.PANEL,12));
        panel.setElevation(Ui.dp(this,12));

        LinearLayout top=Ui.row(this);
        TextView name=Ui.text(this,"FahrCockpit",13,Ui.FG,true);
        name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.END);
        top.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        fold=button("−");
        Button settings=button("⚙"),close=button("×");
        for(Button b:new Button[]{fold,settings,close})
            top.addView(b,new LinearLayout.LayoutParams(Ui.dp(this,38),Ui.dp(this,43)));
        panel.addView(top);
        contents=new ScrollView(this);
        LinearLayout inside=Ui.column(this);
        contents.addView(inside);
        panel.addView(contents,new LinearLayout.LayoutParams(-1,0,1));

        track=Ui.text(this,"Spotify öffnen und Musik starten",12,Ui.FG,true);
        track.setMaxLines(2);
        inside.addView(track);
        seekBar=new SeekBar(this);seekBar.setMax(TrackProgressRules.MAX);
        seekBar.setEnabled(false);inside.addView(seekBar);
        LinearLayout times=Ui.row(this);
        elapsed=Ui.text(this,"--:--",10,Ui.MUTED,false);
        total=Ui.text(this,"--:--",10,Ui.MUTED,false);
        times.addView(elapsed,new LinearLayout.LayoutParams(0,-2,1));
        times.addView(total);inside.addView(times);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onStartTrackingTouch(SeekBar s){scrubbing=true;}
            @Override public void onProgressChanged(SeekBar s,int value,boolean user){
                if(user) elapsed.setText(TrackProgressRules.time(
                    TrackProgressRules.targetPosition(value,duration)));
            }
            @Override public void onStopTrackingTouch(SeekBar s){
                long target=TrackProgressRules.targetPosition(s.getProgress(),duration);
                scrubbing=false;
                if(target>=0) toast(SpotifyMedia.seekAbsolute(DockedCockpitService.this,target));
                handler.postDelayed(()->{if(panel!=null) updateMusic();},500L);
            }
        });

        LinearLayout media=Ui.row(this);
        prev=addButton(media,"⏮");play=addButton(media,"▶");
        next=addButton(media,"⏭");inside.addView(media);
        LinearLayout jumps=Ui.row(this);
        minus=addButton(jumps,"−15");plus=addButton(jumps,"+15");
        inside.addView(jumps);
        prev.setOnClickListener(v->send(SpotifyMedia.Action.PREVIOUS));
        play.setOnClickListener(v->send(SpotifyMedia.Action.TOGGLE));
        next.setOnClickListener(v->send(SpotifyMedia.Action.NEXT));
        minus.setOnClickListener(v->send(SpotifyMedia.Action.SEEK_BACK));
        plus.setOnClickListener(v->send(SpotifyMedia.Action.SEEK_FORWARD));

        Button maps=Ui.button(this,"Google Maps");
        inside.addView(maps);
        maps.setOnClickListener(v->openMaps());
        Button fuel=Ui.button(this,"⛽ Tankstellen");
        inside.addView(fuel);
        fuel.setOnClickListener(v->{
            try {startActivity(new Intent(this,FuelActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
            catch(RuntimeException e){toast("Tankstellen nicht erreichbar.");}
        });
        Button saphe=Ui.button(this,"Saphe öffnen");
        inside.addView(saphe);
        saphe.setOnClickListener(v->toast(WarningSource.openSaphe(this)
            ?"Saphe geöffnet – Gerätestatus unbekannt.":"Saphe nicht installiert."));

        fold.setOnClickListener(v->{
            collapsed=!collapsed;
            contents.setVisibility(collapsed?View.GONE:View.VISIBLE);
            fold.setText(collapsed?"+":"−");applyBounds();
        });
        close.setOnClickListener(v->stopSelf());
        settings.setOnClickListener(v->{
            stopSelf();
            try {startActivity(new Intent(this,MainActivity.class)
                .setAction("FAHR_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
            catch(RuntimeException e){toast("Einstellungen nicht erreichbar.");}
        });
        windows.addView(panel,params);
        handler.post(refresh);
    }
    private void send(SpotifyMedia.Action a) {
        toast(SpotifyMedia.send(this,a));updateMusic();
    }
    private void openMaps() {
        if(!GoogleMapsLaunch.installed(this)){
            toast("Google Maps nicht installiert."); return;
        }
        try{startActivity(GoogleMapsLaunch.mapIntent());}
        catch(RuntimeException e){toast("Google Maps nicht erreichbar.");}
    }
    private void updateMusic() {
        SpotifyMedia.State s=SpotifyMedia.read(this);
        track.setText(s.line);
        play.setText(s.playing?"⏸":"▶");
        play.setEnabled(s.connected);
        prev.setEnabled(s.canPrevious);next.setEnabled(s.canNext);
        minus.setEnabled(s.canSeek);plus.setEnabled(s.canSeek);
        duration=s.durationMs;
        boolean has=TrackProgressRules.hasDuration(s.durationMs)&&s.positionMs>=0;
        seekBar.setVisibility(has?View.VISIBLE:View.GONE);
        elapsed.setVisibility(has?View.VISIBLE:View.GONE);
        total.setVisibility(has?View.VISIBLE:View.GONE);
        seekBar.setEnabled(has&&s.canSeek);
        if(has){
            if(!scrubbing){
                seekBar.setProgress(TrackProgressRules.sliderPosition(s.positionMs,s.durationMs));
                elapsed.setText(TrackProgressRules.time(s.positionMs));
            }
            total.setText(TrackProgressRules.time(s.durationMs));
        }
    }
    private void calculateBounds() {
        int w=getResources().getDisplayMetrics().widthPixels;
        int h=getResources().getDisplayMetrics().heightPixels;
        boolean wide=DockedLayoutRules.landscape(w,h);
        if(collapsed) {
            params.width=Math.min(w,Ui.dp(this,190));
            params.height=Math.min(h,Ui.dp(this,59));
            params.x=DockedLayoutRules.collapsedX(w,params.width,wide);
            params.y=wide?0:Math.max(0,h-params.height);
        } else {
            params.width=DockedLayoutRules.width(w,wide);
            params.height=DockedLayoutRules.height(h,wide);
            params.x=DockedLayoutRules.x(w,params.width,wide);
            params.y=DockedLayoutRules.y(h,params.height,wide);
        }
    }
    private void applyBounds() {
        if(panel==null||windows==null)return;
        calculateBounds();
        try{windows.updateViewLayout(panel,params);}
        catch(IllegalArgumentException ignored){}
    }
    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);applyBounds();
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if(panel!=null&&windows!=null)try{windows.removeView(panel);}
        catch(IllegalArgumentException ignored){}
        panel=null;super.onDestroy();
    }
}
