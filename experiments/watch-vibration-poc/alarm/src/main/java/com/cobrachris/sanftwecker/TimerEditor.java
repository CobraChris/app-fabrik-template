package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** One editing flow for new and existing named countdowns. */
public final class TimerEditor {
    private TimerEditor(){}
    private static EditText field(Activity c,String value,String hint){
        EditText e=new EditText(c);
        e.setText(value);e.setHint(hint);e.setTextColor(UiKit.TEXT);
        e.setHintTextColor(UiKit.MUTED);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setSelectAllOnFocus(true);
        e.setGravity(Gravity.CENTER);
        return e;
    }
    private static int parse(EditText v){
        try{return Integer.parseInt(v.getText().toString().trim());}
        catch(NumberFormatException e){return -1;}
    }
    public static void show(Activity c,TimerEntry previous,Runnable onSaved) {
        boolean edit=previous!=null;
        LinearLayout content=UiKit.column(c);
        content.setPadding(UiKit.dp(c,18),0,UiKit.dp(c,18),0);
        content.addView(UiKit.label(c,"Name",14,UiKit.MUTED));
        EditText title=new EditText(c);
        title.setSingleLine(true);title.setTextColor(UiKit.TEXT);
        title.setHint("z. B. Dehnen");title.setHintTextColor(UiKit.MUTED);
        title.setText(edit?previous.name:"Neuer Timer");
        content.addView(title);
        content.addView(UiKit.label(c,"Stunden : Minuten : Sekunden",14,UiKit.MUTED));
        long seconds=(edit?previous.durationMs:120000L)/1000;
        EditText hours=field(c,String.valueOf(seconds/3600),"Std.");
        EditText minutes=field(c,String.valueOf((seconds/60)%60),"Min.");
        EditText secs=field(c,String.valueOf(seconds%60),"Sek.");
        LinearLayout row=UiKit.row(c);
        row.addView(hours,UiKit.weight());
        row.addView(minutes,UiKit.weight());
        row.addView(secs,UiKit.weight());
        content.addView(row);
        TextView hint=UiKit.label(c,"Dauer: mindestens 1 Sekunde, höchstens 24 Stunden. "+
                "Beim Bearbeiten wird ein laufender Timer zurückgesetzt.",
                13,UiKit.MUTED);
        content.addView(hint);
        AlertDialog dialog=new AlertDialog.Builder(c)
                .setTitle(edit?"Timer bearbeiten":"Neuen Timer anlegen")
                .setView(content)
                .setNegativeButton("Abbrechen",null)
                .setPositiveButton(edit?"Speichern":"Anlegen",null).create();
        dialog.setOnShowListener(ignore->dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v->{
                    int h=parse(hours),m=parse(minutes),s=parse(secs);
                    if(h<0||h>24||m<0||m>59||s<0||s>59||
                            (h==24&&(m!=0||s!=0))||(h==0&&m==0&&s==0)){
                        Toast.makeText(c,"Bitte gültige Zeit einstellen",Toast.LENGTH_LONG).show();
                        return;
                    }
                    long duration=((h*3600L)+(m*60L)+s)*1000L;
                    String text=title.getText().toString().trim();
                    if(text.isEmpty()){title.setError("Name erforderlich");return;}
                    try{
                        if(edit)TimerScheduler.edit(c,previous.id,text,duration);
                        else TimerRepository.create(c,text,duration);
                        dialog.dismiss();onSaved.run();
                    }catch(RuntimeException error){
                        new AlertDialog.Builder(c).setMessage(
                                "Timer konnte nicht gespeichert werden: "+error.getMessage())
                                .setPositiveButton("OK",null).show();
                    }
                }));
        dialog.show();
    }
}
