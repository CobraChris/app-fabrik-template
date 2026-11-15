package com.cobrachris.fahrcockpit;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Native, monochrome 24dp speaker glyph. Never uses Samsung/Google color emoji.
 * Muted: speaker plus slash, no sound waves. Enabled: speaker plus waves.
 */
final class NavigationAudioIcon extends Drawable {
    private final boolean muted;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    NavigationAudioIcon(boolean muted) {
        this.muted = muted;
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(1.9f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        int save = canvas.save();
        float unit = Math.min(bounds.width(), bounds.height()) / 24f;
        canvas.translate(bounds.centerX() - unit * 12f,
                         bounds.centerY() - unit * 12f);
        canvas.scale(unit, unit);

        Path speaker = new Path();
        speaker.moveTo(4.2f, 9.5f);
        speaker.lineTo(8.2f, 9.5f);
        speaker.lineTo(13.0f, 5.8f);
        speaker.lineTo(13.0f, 18.2f);
        speaker.lineTo(8.2f, 14.5f);
        speaker.lineTo(4.2f, 14.5f);
        speaker.close();
        canvas.drawPath(speaker, paint);

        Path waves = new Path();
        if (muted) {
            // Cross out the entire speaker; do not render any sound waves.
            waves.moveTo(3.2f, 3.2f);
            waves.lineTo(20.8f, 20.8f);
        } else {
            waves.moveTo(16.0f, 9.0f);
            waves.quadTo(19.2f, 12.0f, 16.0f, 15.0f);
            waves.moveTo(18.2f, 6.4f);
            waves.quadTo(24.4f, 12.0f, 18.2f, 17.6f);
        }
        canvas.drawPath(waves, paint);
        canvas.restoreToCount(save);
    }

    @Override public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        paint.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override public int getIntrinsicWidth() { return 24; }
    @Override public int getIntrinsicHeight() { return 24; }
}
