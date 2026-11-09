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
 * User-started 30% overlay beside Google Maps (landscape), or below it (portrait).
 * Landscape side defaults RIGHT and can be changed in FahrCockpit settings.
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
    private LinearLayout fullHeader,miniHeader;
    private Button miniPlay;
    private ScrollView contents;
    private TextView track,elapsed,total;
    private SeekBar seekBar;
    private Button play,prev,next,minus,plus,fold;
    private boolean collapsed,scrubbing,dragging;
    private long duration = -1;
    private float downRawX,downRawY;
    private int startX,startY;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long EXPANDED_TIMEOUT_MS = 18000L;
    private final Runnable autoCollapse = () -> {
        if(panel!=null && !collapsed && !scrubbing) setCollapsed(true);
    };
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (panel != null) updateMusic();
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
            // Every explicit app-icon start restores a navigation-safe HUD.
            collapsed=getSharedPreferences(DockedLayoutRules.PREFS,MODE_PRIVATE)
                .getBoolean(DockedLayoutRules.KEY_START_COMPACT,
                    DockedLayoutRules.DEFAULT_COMPACT);
            if(panel==null) attach();
            else setCollapsed(collapsed);
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
            OverlayKeyboardRules.nonFocusableBehindKeyboardFlags(),
            PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.START;
        calculateBounds();
        panel=Ui.column(this);
        panel.setPadding(Ui.dp(this,7),Ui.dp(this,5),Ui.dp(this,7),Ui.dp(this,6));
        panel.setBackground(Ui.background(this,Ui.PANEL,12));
        panel.setElevation(Ui.dp(this,12));

        // Small, three-button HUD is the default: Maps keeps Start,
        // route controls and the location marker unobstructed.
        miniHeader=Ui.row(this);
        Button expand=button("♫");
        miniPlay=button("▶");
        Button miniClose=button("×");
        for(Button b:new Button[]{expand,miniPlay,miniClose})
            miniHeader.addView(b,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));
        panel.addView(miniHeader);
        expand.setContentDescription("FahrCockpit öffnen oder verschieben");
        miniPlay.setContentDescription("Spotify Wiedergabe umschalten");
        miniClose.setContentDescription("FahrCockpit schließen");
        miniPlay.setOnClickListener(v->send(SpotifyMedia.Action.TOGGLE));
        miniClose.setOnClickListener(v->stopSelf());
        expand.setOnTouchListener((view,event)->{
            if(params==null||windows==null) return false;
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    downRawX=event.getRawX(); downRawY=event.getRawY();
                    startX=params.x; startY=params.y; dragging=false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx=event.getRawX()-downRawX;
                    float dy=event.getRawY()-downRawY;
                    if(Math.abs(dx)>ViewConfiguration.get(this).getScaledTouchSlop()
                        ||Math.abs(dy)>ViewConfiguration.get(this).getScaledTouchSlop())
                        dragging=true;
                    if(dragging){
                        int w=getResources().getDisplayMetrics().widthPixels;
                        int h=getResources().getDisplayMetrics().heightPixels;
                        params.x=DockedLayoutRules.clamp(
                            startX+Math.round(dx),w,params.width);
                        params.y=DockedLayoutRules.clamp(
                            startY+Math.round(dy),h,params.height);
                        try{windows.updateViewLayout(panel,params);}
                        catch(IllegalArgumentException ignored){}
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if(!dragging) setCollapsed(false);
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    return true;
                default:
                    return false;
            }
        });

        fullHeader=Ui.row(this);
        TextView name=Ui.text(this,"FahrCockpit",13,Ui.FG,true);
        name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.END);
        fullHeader.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        fold=button("−");
        Button settings=button("⚙"),close=button("×");
        for(Button b:new Button[]{fold,settings,close})
            fullHeader.addView(b,new LinearLayout.LayoutParams(Ui.dp(this,38),Ui.dp(this,43)));
        panel.addView(fullHeader);
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
            @Override public void onStartTrackingTouch(SeekBar s){
                scrubbing=true;handler.removeCallbacks(autoCollapse);
            }
            @Override public void onProgressChanged(SeekBar s,int value,boolean user){
                if(user) elapsed.setText(TrackProgressRules.time(
                    TrackProgressRules.targetPosition(value,duration)));
            }
            @Override public void onStopTrackingTouch(SeekBar s){
                long target=TrackProgressRules.targetPosition(s.getProgress(),duration);
                scrubbing=false;resetAutoCollapse();
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
        maps.setOnClickListener(v->{setCollapsed(true);openMaps();});
        Button fuel=Ui.button(this,"⛽ Tankstellen");
        inside.addView(fuel);
        fuel.setOnClickListener(v->{
            setCollapsed(true);
            try {startActivity(new Intent(this,FuelActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
            catch(RuntimeException e){toast("Tankstellen nicht erreichbar.");}
        });
        Button saphe=Ui.button(this,"Saphe öffnen");
        inside.addView(saphe);
        saphe.setOnClickListener(v->{
            setCollapsed(true);
            toast(WarningSource.openSaphe(this)
                ?"Saphe geöffnet – Gerätestatus unbekannt.":"Saphe nicht installiert.");
        });

        fold.setOnClickListener(v->setCollapsed(true));
        close.setOnClickListener(v->stopSelf());
        settings.setOnClickListener(v->{
            stopSelf();
            try {startActivity(new Intent(this,MainActivity.class)
                .setAction("FAHR_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
            catch(RuntimeException e){toast("Einstellungen nicht erreichbar.");}
        });
        // Full controls are available on tap, but must never occupy the
        // map when the user has not explicitly expanded them.
        miniHeader.setVisibility(collapsed?View.VISIBLE:View.GONE);
        fullHeader.setVisibility(collapsed?View.GONE:View.VISIBLE);
        contents.setVisibility(collapsed?View.GONE:View.VISIBLE);
        windows.addView(panel,params);
        if(!collapsed) resetAutoCollapse();
        handler.post(refresh);
    }
    private void resetAutoCollapse() {
        handler.removeCallbacks(autoCollapse);
        if(!collapsed) handler.postDelayed(autoCollapse,EXPANDED_TIMEOUT_MS);
    }
    private void setCollapsed(boolean value) {
        if(panel==null) return;
        collapsed=value;
        miniHeader.setVisibility(value?View.VISIBLE:View.GONE);
        fullHeader.setVisibility(value?View.GONE:View.VISIBLE);
        contents.setVisibility(value?View.GONE:View.VISIBLE);
        if(value) handler.removeCallbacks(autoCollapse);
        else resetAutoCollapse();
        applyBounds();
    }
    private void send(SpotifyMedia.Action a) {
        toast(SpotifyMedia.send(this,a));updateMusic();
        resetAutoCollapse();
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
        miniPlay.setText(s.playing?"⏸":"▶");
        miniPlay.setEnabled(s.connected);
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
        boolean right=getSharedPreferences(DockedLayoutRules.PREFS,MODE_PRIVATE)
            .getBoolean(DockedLayoutRules.KEY_LANDSCAPE_RIGHT,true);
        if(collapsed) {
            params.width=Math.min(w,Ui.dp(this,164));
            params.height=Math.min(h,Ui.dp(this,60));
            params.x=DockedLayoutRules.miniX(w,params.width,wide,right);
            params.y=DockedLayoutRules.miniY(h,params.height);
        } else {
            params.width=DockedLayoutRules.width(w,wide);
            params.height=DockedLayoutRules.height(h,wide);
            params.x=DockedLayoutRules.x(w,params.width,wide,right);
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
