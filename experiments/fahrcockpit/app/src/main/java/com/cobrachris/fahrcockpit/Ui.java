package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BG = Color.rgb(16, 19, 25);
    static final int PANEL = Color.rgb(34, 39, 48);
    static final int FG = Color.WHITE;
    static final int MUTED = Color.rgb(177, 184, 194);
    static final int ACCENT = Color.rgb(114, 206, 235);
    static final int WARN = Color.rgb(245, 196, 104);

    private Ui() {}

    static int dp(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable background(Context context, int color, int radius) {
        GradientDrawable result = new GradientDrawable();
        result.setColor(color);
        result.setCornerRadius(dp(context, radius));
        return result;
    }

    static LinearLayout column(Context context) {
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        return column;
    }

    static LinearLayout row(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        return row;
    }

    static TextView text(Context context, String content, int sp, int color, boolean bold) {
        TextView label = new TextView(context);
        label.setText(content);
        label.setTextColor(color);
        label.setTextSize(sp);
        if (bold) label.setTypeface(null, Typeface.BOLD);
        label.setPadding(0, dp(context, 9), 0, dp(context, 9));
        return label;
    }

    static Button button(Context context, String content) {
        Button button = new Button(context);
        button.setText(content);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setMinHeight(dp(context, 46));
        return button;
    }

    static void gap(Context context, LinearLayout into, int dp) {
        View spacer = new View(context);
        into.addView(spacer, new LinearLayout.LayoutParams(1, Ui.dp(context, dp)));
    }

    static void section(Context context, LinearLayout into, String title, String description) {
        gap(context, into, 12);
        into.addView(text(context, title, 20, FG, true));
        if (description != null && !description.isEmpty()) {
            into.addView(text(context, description, 14, MUTED, false));
        }
    }
}
