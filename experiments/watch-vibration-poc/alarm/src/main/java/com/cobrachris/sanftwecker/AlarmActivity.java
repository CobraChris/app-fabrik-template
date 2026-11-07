package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.Calendar;
import java.util.Locale;

/** Calming full-screen alarm display: large stacked live time, swipe-to-stop and snooze. */
public final class AlarmActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView hourView, minuteView;
    private boolean closing;
    private boolean receiverRegistered;
    private final BroadcastReceiver finishedReceiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){
            if(RingingService.RING_FINISHED.equals(i.getAction()) && !closing){
                closing=true;
                finish();
            }
        }
    };
    private int alarmId;
    private AlarmProfile profile;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (closing || hourView == null) return;
            Calendar c = Calendar.getInstance();
            hourView.setText(String.format(Locale.GERMANY,"%02d",c.get(Calendar.HOUR_OF_DAY)));
            minuteView.setText(String.format(Locale.GERMANY,"%02d",c.get(Calendar.MINUTE)));
            handler.postDelayed(this, 5000L);
        }
    };

    private int dp(float size) { return UiKit.dp(this, size); }

    private TextView text(String value, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(sp);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        return t;
    }

    private GradientDrawable rounded(int fill,int border,int radius) {
        GradientDrawable d=UiKit.shape(fill,this,radius);
        if(border!=Color.TRANSPARENT)d.setStroke(dp(1),border);
        return d;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        UiKit.window(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Show above the lock screen, but never dismiss the user's device security.
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        alarmId=getIntent().getIntExtra(AlarmScheduler.EXTRA_ID,-1);
        profile=AlarmRepository.get(this,alarmId);
        if(profile==null){finish();return;}
        createScreen();
    }

    private void createScreen() {
        FrameLayout screen=new FrameLayout(this);
        screen.addView(new AlarmBackdrop(this),new FrameLayout.LayoutParams(-1,-1));
        LinearLayout page=new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(24),dp(32),dp(24),dp(22));
        screen.addView(page,new FrameLayout.LayoutParams(-1,-1));

        TextView kicker=text("SANFTWECKER",13,0xffb6dedf);
        kicker.setLetterSpacing(.22f);
        page.addView(kicker,new LinearLayout.LayoutParams(-1,dp(25)));

        TextView name=text(profile.name,23,Color.WHITE);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);
        np.topMargin=dp(9);
        page.addView(name,np);

        TextView note=text("Zeit aufzustehen",14,0xffb2d1d3);
        LinearLayout.LayoutParams sn=new LinearLayout.LayoutParams(-1,-2);
        sn.topMargin=dp(8);
        page.addView(note,sn);

        LinearLayout center=new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams centerParams=new LinearLayout.LayoutParams(-1,0,1f);
        page.addView(center,centerParams);

        float screenDp=getResources().getDisplayMetrics().widthPixels /
                getResources().getDisplayMetrics().density;
        float digits=Math.max(112,Math.min(158,screenDp*.41f));
        hourView=text("00",digits,Color.WHITE);
        minuteView=text("00",digits,Color.WHITE);
        hourView.setTypeface(android.graphics.Typeface.create("sans-serif-thin",
                android.graphics.Typeface.NORMAL));
        minuteView.setTypeface(android.graphics.Typeface.create("sans-serif-thin",
                android.graphics.Typeface.NORMAL));
        center.addView(hourView,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout.LayoutParams minuteParams=new LinearLayout.LayoutParams(-1,-2);
        minuteParams.topMargin=-dp(17);
        center.addView(minuteView,minuteParams);

        TextView instruction=text("Zum Abstellen nach rechts schieben",15,Color.WHITE);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,-2);
        ip.bottomMargin=dp(12);
        page.addView(instruction,ip);

        FrameLayout stopTrack=new FrameLayout(this);
        stopTrack.setBackground(rounded(0x20030e13,0x88e4f7f5,40));
        LinearLayout.LayoutParams trackParams=new LinearLayout.LayoutParams(-1,dp(82));
        page.addView(stopTrack,trackParams);
        TextView stopLabel=text("WECKER ABSTELLEN  →",15,Color.WHITE);
        stopLabel.setLetterSpacing(.04f);
        FrameLayout.LayoutParams textPos=new FrameLayout.LayoutParams(-1,-1);
        textPos.leftMargin=dp(53);
        stopTrack.addView(stopLabel,textPos);

        SeekBar swipe=new SeekBar(this);
        swipe.setMax(100);
        swipe.setProgress(0);
        swipe.setSplitTrack(false);
        swipe.setMinHeight(dp(82));
        swipe.setContentDescription("Wecker abstellen: Schieber ganz nach rechts bewegen");
        swipe.setProgressTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        swipe.setProgressBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        swipe.setThumb(new StopThumb(dp(66)));
        FrameLayout.LayoutParams sliderParams=new FrameLayout.LayoutParams(-1,-1);
        stopTrack.addView(swipe,sliderParams);
        swipe.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar b,int p,boolean fromUser) {
                if(fromUser)stopLabel.setText(p>=90?"LOS LASSEN ZUM STOPPEN":"WECKER ABSTELLEN  →");
            }
            @Override public void onStartTrackingTouch(SeekBar b){}
            @Override public void onStopTrackingTouch(SeekBar b){
                if(b.getProgress()>=90){
                    b.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                    stopAlarm();
                } else {
                    b.setProgress(0);
                    stopLabel.setText("WECKER ABSTELLEN  →");
                }
            }
        });

        if(profile.snoozeEnabled){
            Button snooze=new Button(this);
            snooze.setAllCaps(false);
            snooze.setText("Schlummern · "+profile.snoozeMinutes+" Minuten");
            snooze.setTextSize(18);
            snooze.setTextColor(Color.WHITE);
            snooze.setMinHeight(dp(64));
            snooze.setBackground(rounded(0x66415b67,0x557caeb4,24));
            LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(64));
            sp.topMargin=dp(15);
            page.addView(snooze,sp);
            snooze.setOnClickListener(v->snoozeAlarm());
        }

        // Accessible alternative for screen readers and users who cannot swipe.
        TextView accessibleStop=text("Alternativ: direkt stoppen",13,0xffb0d0d0);
        accessibleStop.setMinHeight(dp(48));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(48));
        ap.topMargin=dp(4);
        page.addView(accessibleStop,ap);
        accessibleStop.setOnClickListener(v->stopAlarm());
        accessibleStop.setContentDescription("Wecker sofort stoppen");
        setContentView(screen);
    }

    private void stopAlarm() { end(RingingService.STOP); }
    private void snoozeAlarm() { end(RingingService.SNOOZE); }

    private void end(String action) {
        if(closing)return;
        closing=true;
        try {
            startService(new Intent(this,RingingService.class).setAction(action));
            finish();
        } catch (RuntimeException error) {
            closing=false;
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Wecker konnte nicht beendet werden")
                    .setMessage("Bitte über die Alarmbenachrichtigung stoppen.")
                    .setPositiveButton("OK",null).show();
        }
    }

    // API 30–32 have no exported-flag overload; API 33+ registers NOT_EXPORTED explicitly.
    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override protected void onResume() {
        super.onResume();
        if(!receiverRegistered){
            IntentFilter filter=new IntentFilter(RingingService.RING_FINISHED);
            if(Build.VERSION.SDK_INT>=33)
                registerReceiver(finishedReceiver,filter,Context.RECEIVER_NOT_EXPORTED);
            else
                registerReceiver(finishedReceiver,filter);
            receiverRegistered=true;
        }
        if(profile!=null){handler.removeCallbacks(clockTick);handler.post(clockTick);}
    }
    @Override protected void onPause() {
        handler.removeCallbacks(clockTick);
        if(receiverRegistered){
            unregisterReceiver(finishedReceiver);
            receiverRegistered=false;
        }
        super.onPause();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacks(clockTick);
        super.onDestroy();
    }
    @Override public void onBackPressed() {
        // Prevent accidental dismissal of the alarm screen.
    }

    /** Distinctive large white thumb, drawn in-app. No image or font assets required. */
    private static final class StopThumb extends Drawable {
        private final int size;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        StopThumb(int size){this.size=size;}
        @Override public int getIntrinsicWidth(){return size;}
        @Override public int getIntrinsicHeight(){return size;}
        @Override public void draw(Canvas canvas){
            Rect b=getBounds();
            float x=b.centerX(),y=b.centerY(),r=Math.min(b.width(),b.height())*.47f;
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x,y,r,paint);
            paint.setColor(0xff123d47);
            paint.setStrokeWidth(r*.12f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawArc(x-r*.27f,y-r*.35f,x+r*.27f,y+r*.14f,180,180,false,paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(x-r*.38f,y-r*.03f,x+r*.38f,y+r*.43f,
                    r*.08f,r*.08f,paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(x,y+r*.20f,r*.065f,paint);
        }
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}
        @Override public void setColorFilter(ColorFilter colorFilter){paint.setColorFilter(colorFilter);}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
