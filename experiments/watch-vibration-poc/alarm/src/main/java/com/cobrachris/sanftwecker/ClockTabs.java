package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared native navigation: independent pages, consistent dark appearance. */
public final class ClockTabs {
    public static final int ALARMS=0, TIMERS=1, STOPWATCH=2;
    private ClockTabs(){}
    public static void add(Activity c,LinearLayout parent,int selected){
        LinearLayout bar=UiKit.row(c);
        String[] names={"Wecker","Timer","Stoppuhr"};
        Class<?>[] targets={MainActivity.class,TimerActivity.class,StopwatchActivity.class};
        for(int i=0;i<names.length;i++){
            final int index=i;
            TextView tab=UiKit.label(c,names[i],15,
                    i==selected?UiKit.BLUE:UiKit.MUTED);
            tab.setTypeface(Typeface.DEFAULT,i==selected?Typeface.BOLD:Typeface.NORMAL);
            tab.setGravity(Gravity.CENTER);
            tab.setMinHeight(UiKit.dp(c,52));
            tab.setBackground(UiKit.shape(i==selected?UiKit.CARD_2:UiKit.BG,c,12));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,UiKit.dp(c,52),1);
            p.setMargins(UiKit.dp(c,2),UiKit.dp(c,8),UiKit.dp(c,2),UiKit.dp(c,12));
            bar.addView(tab,p);
            tab.setContentDescription(names[i]+(i==selected?" ausgewählt":" öffnen"));
            tab.setOnClickListener(v->{
                if(index==selected)return;
                Intent open=new Intent(c,targets[index])
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                c.startActivity(open);
                c.finish();
            });
        }
        parent.addView(bar);
    }
}
