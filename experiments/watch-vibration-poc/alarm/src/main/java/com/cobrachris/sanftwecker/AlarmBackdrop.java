package com.cobrachris.sanftwecker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

/** Original, quiet faceted teal alarm background, generated locally without image assets. */
public final class AlarmBackdrop extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final float[][] SHAPES = {
        {0f,0f, .38f,0f, .27f,.25f},
        {.38f,0f, .76f,0f, .57f,.22f},
        {.76f,0f, 1f,0f, 1f,.31f},
        {0f,.05f, .27f,.25f, 0f,.49f},
        {.27f,.25f, .57f,.22f, .43f,.49f},
        {.57f,.22f, 1f,.31f, .75f,.58f},
        {0f,.49f, .43f,.49f, .18f,.74f},
        {.43f,.49f, .75f,.58f, .55f,.83f},
        {.75f,.58f, 1f,.31f, 1f,.85f},
        {0f,.87f, .18f,.74f, .48f,1f},
        {.18f,.74f, .55f,.83f, .48f,1f},
        {.55f,.83f, 1f,.85f, 1f,1f},
        {0f,.49f, .18f,.74f, 0f,.87f},
        {.27f,.25f, .43f,.49f, 0f,.49f}
    };
    private static final int[] COLORS = {
        0x172BABA2,0x11239C9A,0x1420A9A5,0x102ED6C2,
        0x1E0A191D,0x10239F9D,0x150A191D,0x160A1920,
        0x17071319,0x1320A59A,0x1007191D,0x0C2BB0A7,
        0x142EB3A8,0x0D348E8C
    };
    public AlarmBackdrop(Context context) { super(context); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w=getWidth(), h=getHeight();
        if(w<=0||h<=0)return;
        paint.setShader(new LinearGradient(0f,0f,w,h,
                new int[]{0xff155c60,0xff083b44,0xff091c29},null,Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,w,h,paint);
        paint.setShader(null);
        for(int i=0;i<SHAPES.length;i++) {
            float[] q=SHAPES[i];
            Path p=new Path();
            p.moveTo(q[0]*w,q[1]*h);
            p.lineTo(q[2]*w,q[3]*h);
            p.lineTo(q[4]*w,q[5]*h);
            p.close();
            paint.setColor(COLORS[i]);
            canvas.drawPath(p,paint);
        }
        paint.setColor(0x10000000);
        canvas.drawRect(0,h*.78f,w,h,paint);
    }
}
