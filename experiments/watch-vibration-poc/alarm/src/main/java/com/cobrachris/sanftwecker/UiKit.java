package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** Small reusable dark UI kit; no external UI dependency or copied artwork. */
public final class UiKit {
    public static final int BG = Color.rgb(19,20,24);
    public static final int CARD = Color.rgb(31,30,38);
    public static final int CARD_2 = Color.rgb(42,43,54);
    public static final int TEXT = Color.rgb(236,237,249);
    public static final int MUTED = Color.rgb(167,174,195);
    public static final int BLUE = Color.rgb(155,192,255);
    public static final int BLUE_DARK = Color.rgb(45,88,145);
    public static final int OUTLINE = Color.rgb(77,82,102);

    private UiKit() { }

    public static int dp(Activity c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static void window(Activity c) {
        c.getWindow().setStatusBarColor(BG);
        c.getWindow().setNavigationBarColor(BG);
        c.getWindow().getDecorView().setSystemUiVisibility(0);
    }

    public static GradientDrawable shape(int color, Activity c, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radius));
        return d;
    }

    public static LinearLayout column(Activity c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Activity c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout card(Activity c) {
        LinearLayout card = column(c);
        card.setBackground(shape(CARD, c, 22));
        card.setPadding(dp(c, 20), dp(c, 16), dp(c, 20), dp(c, 20));
        return card;
    }

    public static void placeCard(Activity c, LinearLayout parent, View card) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(c, 12);
        parent.addView(card, p);
    }

    public static TextView label(Activity c, String text, int sp, int color) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    public static TextView heading(Activity c, String value) {
        TextView t = label(c, value, 20, TEXT);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    public static Button button(Activity c, String value, boolean primary) {
        Button b = new Button(c);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextColor(primary ? BG : TEXT);
        b.setTextSize(16);
        b.setBackground(shape(primary ? BLUE : CARD_2, c, 16));
        b.setMinHeight(dp(c, 52));
        return b;
    }

    public static TextView action(Activity c, String text) {
        TextView t = label(c, text, 19, BLUE);
        t.setGravity(Gravity.CENTER);
        t.setMinWidth(dp(c, 48));
        t.setMinHeight(dp(c, 48));
        t.setContentDescription(text);
        return t;
    }

    public static TextView chip(Activity c, String value, boolean selected) {
        TextView t = label(c, value, 14, selected ? BG : MUTED);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setBackground(shape(selected ? BLUE : CARD_2, c, 12));
        t.setMinHeight(dp(c, 38));
        t.setMinWidth(dp(c, 36));
        return t;
    }

    public static void setChip(Activity c, TextView t, boolean selected) {
        t.setTextColor(selected ? BG : MUTED);
        t.setBackground(shape(selected ? BLUE : CARD_2, c, 12));
    }

    public static Switch toggle(Activity c) {
        Switch s = new Switch(c);
        s.setMinHeight(dp(c, 48));
        s.setThumbTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},
                new int[]{BLUE, MUTED}));
        s.setTrackTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},
                new int[]{BLUE_DARK, OUTLINE}));
        return s;
    }

    public static SeekBar seek(Activity c, int max) {
        SeekBar b = new SeekBar(c);
        b.setMax(max);
        b.setMinHeight(dp(c, 42));
        b.setProgressTintList(ColorStateList.valueOf(BLUE));
        b.setThumbTintList(ColorStateList.valueOf(BLUE));
        return b;
    }

    public static View separator(Activity c) {
        View v = new View(c);
        v.setBackgroundColor(OUTLINE);
        return v;
    }

    public static LinearLayout mainColumn(Activity c) {
        LinearLayout l = column(c);
        l.setPadding(dp(c, 18), dp(c, 16), dp(c, 18), dp(c, 92));
        l.setBackgroundColor(BG);
        return l;
    }

    public static ScrollView scroll(Activity c, LinearLayout column) {
        ScrollView s = new ScrollView(c);
        s.setFillViewport(true);
        s.setBackgroundColor(BG);
        s.addView(column);
        return s;
    }

    public static LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
    }
}
