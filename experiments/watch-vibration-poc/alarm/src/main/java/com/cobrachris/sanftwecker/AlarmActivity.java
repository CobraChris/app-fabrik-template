package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

/** Explicit, lock-screen-friendly controls while Android's alarm service rings. */
public final class AlarmActivity extends Activity {
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        UiKit.window(this);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        int id=getIntent().getIntExtra(AlarmScheduler.EXTRA_ID,1);
        AlarmProfile p=AlarmRepository.get(this,id);
        LinearLayout root=UiKit.mainColumn(this);
        root.setGravity(Gravity.CENTER);
        root.addView(UiKit.label(this,"SANFTWECKER",16,UiKit.BLUE));
        TextView title=UiKit.label(this,p==null?"Weckzeit":p.name,30,UiKit.TEXT);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView time=UiKit.label(this,p==null?"":String.format(Locale.GERMANY,
                "%02d:%02d",p.hour,p.minute),70,UiKit.BLUE);
        time.setGravity(Gravity.CENTER);
        root.addView(time);
        root.addView(UiKit.label(this,
                "Der Ton wird langsam lauter. Watch-Benachrichtigungen können "+
                "je nach Galaxy-Wearable-Einstellungen vibrieren.",16,UiKit.MUTED));

        Button stop=UiKit.button(this,"Wecker stoppen",true);
        root.addView(stop);
        stop.setOnClickListener(v->{
            startService(new Intent(this,RingingService.class).setAction(RingingService.STOP));
            finish();
        });
        Button snooze=UiKit.button(this,"Schlummern",false);
        root.addView(snooze);
        snooze.setOnClickListener(v->{
            startService(new Intent(this,RingingService.class).setAction(RingingService.SNOOZE));
            finish();
        });
        root.addView(UiKit.label(this,"Beta: bisherige Wecker vorerst beibehalten.",
                13,UiKit.MUTED));
        setContentView(root);
    }

    @Override public void onBackPressed(){
        // Prevent closing the ringing screen without an explicit stop/snooze action.
    }
}
