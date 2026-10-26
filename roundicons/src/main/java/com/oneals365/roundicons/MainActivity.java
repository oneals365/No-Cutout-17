package com.oneals365.roundicons;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    /** Replaced with true by Hook when Vector/LSPosed has loaded the module. */
    public static boolean isModuleActive() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(48), pad, pad);

        TextView title = text("Round Icons 17", 28, true);
        root.addView(title);

        boolean active = isModuleActive();
        TextView status = text(active ? "✓  Module active" : "✕  Module not active", 18, true);
        status.setTextColor(Color.WHITE);
        status.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(active ? Color.parseColor("#2E7D32") : Color.parseColor("#B71C1C"));
        status.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        lp.bottomMargin = dp(20);
        root.addView(status, lp);

        root.addView(text(
                "What it does\n" +
                "Draws adaptive app icons as circles inside the apps you tick in Vector. Experimental: " +
                "One UI may draw some icons with its own code that this doesn't reach.\n\n" +
                "Setup\n" +
                "1. Vector \u2192 Modules \u2192 Round Icons 17: turn it on. System UI, One UI Home and Settings are pre-ticked.\n" +
                "2. Optionally tick more apps where you want round icons.\n" +
                "3. Reboot.\n\n" +
                "To undo: turn the module off in Vector and reboot.", 15, false));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setGravity(Gravity.START);
        t.setLineSpacing(0, 1.15f);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
