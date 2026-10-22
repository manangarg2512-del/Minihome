package com.mini.home;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    // Dock order. Matches app names, so no package names needed.
    static final String[] FAV = {"tizentube", "mirror", "youtube", "netflix", "prime", "plex", "kodi"};
    static final int ROTATE_MS = 20000;

    static class App { String label, pkg; ComponentName cn; Drawable icon; }

    Wall wall;
    LinearLayout dock;
    List<App> apps = new ArrayList<>();
    Handler h = new Handler();
    int scene = 0;

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        FrameLayout root = new FrameLayout(this);
        wall = new Wall(this);
        root.addView(wall, new FrameLayout.LayoutParams(-1, -1));

        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm a");
        clock.setFormat24Hour("HH:mm");
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(28);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        cp.setMargins(0, dp(24), dp(40), 0);
        root.addView(clock, cp);

        dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(24), dp(16), dp(24), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x66000000);
        bg.setCornerRadius(dp(28));
        dock.setBackground(bg);
        FrameLayout.LayoutParams dp2 = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dp2.setMargins(0, 0, 0, dp(40));
        root.addView(dock, dp2);

        setContentView(root);
    }

    @Override protected void onResume() {
        super.onResume();
        loadApps();
        buildDock();
        h.removeCallbacksAndMessages(null);
        h.postDelayed(new Runnable() { public void run() {
            scene = (scene + 1) % Wall.COUNT;
            wall.fadeTo(scene);
            h.postDelayed(this, ROTATE_MS);
        }}, ROTATE_MS);
    }

    @Override protected void onPause() { super.onPause(); h.removeCallbacksAndMessages(null); }
    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); if (dock.getChildCount() > 0) dock.getChildAt(0).requestFocus(); }
    @Override public void onBackPressed() { /* stay on home */ }

    void loadApps() {
        apps.clear();
        PackageManager pm = getPackageManager();
        Set<String> seen = new HashSet<>();
        String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String c : cats) {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(c);
            for (ResolveInfo r : pm.queryIntentActivities(i, 0)) {
                String p = r.activityInfo.packageName;
                if (p.equals(getPackageName()) || !seen.add(p)) continue;
                App a = new App();
                a.pkg = p;
                a.label = String.valueOf(r.loadLabel(pm));
                a.cn = new ComponentName(p, r.activityInfo.name);
                a.icon = r.loadIcon(pm);
                apps.add(a);
            }
        }
        Collections.sort(apps, new Comparator<App>() {
            public int compare(App x, App y) { return x.label.compareToIgnoreCase(y.label); }
        });
    }

    void buildDock() {
        dock.removeAllViews();
        List<App> fav = new ArrayList<>();
        for (String k : FAV)
            for (App a : apps)
                if (!fav.contains(a) && a.label.toLowerCase().contains(k)) { fav.add(a); break; }
        if (fav.isEmpty()) for (int i = 0; i < Math.min(6, apps.size()); i++) fav.add(apps.get(i));
        for (final App a : fav) dock.addView(tile(a.label, a.icon, new View.OnClickListener() {
            public void onClick(View v) { launch(a); }
        }));
        dock.addView(tile("All apps", null, new View.OnClickListener() {
            public void onClick(View v) { showAll(); }
        }));
        dock.getChildAt(0).requestFocus();
    }

    View tile(String label, Drawable icon, View.OnClickListener click) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(10), dp(10), dp(10), dp(8));
        t.setFocusable(true);
        t.setClickable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(120), -2);
        lp.setMargins(dp(8), 0, dp(8), 0);
        t.setLayoutParams(lp);

        View iv;
        if (icon != null) {
            ImageView im = new ImageView(this);
            im.setImageDrawable(icon);
            iv = im;
        } else {
            TextView g = new TextView(this);
            g.setText("\u2630");
            g.setTextColor(Color.WHITE);
            g.setTextSize(36);
            g.setGravity(Gravity.CENTER);
            iv = g;
        }
        t.addView(iv, new LinearLayout.LayoutParams(dp(80), dp(80)));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(13);
        tv.setSingleLine(true);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tv.setGravity(Gravity.CENTER);
        t.addView(tv, new LinearLayout.LayoutParams(-1, -2));

        final GradientDrawable fg = new GradientDrawable();
        fg.setColor(0x44FFFFFF);
        fg.setCornerRadius(dp(18));
        t.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean f) {
                v.setBackground(f ? fg : null);
                v.animate().scaleX(f ? 1.2f : 1f).scaleY(f ? 1.2f : 1f).setDuration(120).start();
            }
        });
        t.setOnClickListener(click);
        return t;
    }

    void launch(App a) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(a.cn).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Can't open " + a.label, Toast.LENGTH_SHORT).show();
        }
    }

    void showAll() {
        String[] names = new String[apps.size()];
        for (int i = 0; i < names.length; i++) names[i] = apps.get(i).label;
        new AlertDialog.Builder(this).setTitle("All apps").setItems(names,
            new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface d, int w) { launch(apps.get(w)); }
            }).show();
    }

    // ---- Nature wallpapers drawn in code (no images, no network) ----
    static class Wall extends View {
        static final int COUNT = 4;
        int cur = 0, next = -1;
        float fade = 0f;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Wall(android.content.Context c) { super(c); }

        void fadeTo(int s) {
            next = s;
            ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
            va.setDuration(1500);
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator a) { fade = (float) a.getAnimatedValue(); invalidate(); }
            });
            va.addListener(new android.animation.AnimatorListenerAdapter() {
                public void onAnimationEnd(android.animation.Animator a) { cur = next; next = -1; fade = 0; invalidate(); }
            });
            va.start();
        }

        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            draw(c, w, h, cur, 255);
            if (next >= 0) draw(c, w, h, next, (int) (fade * 255));
        }

        void sky(Canvas c, int w, int h, int top, int bot) {
            p.setShader(new LinearGradient(0, 0, 0, h, top, bot, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, p);
            p.setShader(null);
        }

        void ridge(Canvas c, int w, int h, float base, float amp, int color, int seed) {
            Path path = new Path();
            path.moveTo(0, h);
            Random r = new Random(seed);
            float x = 0, y = h * base;
            path.lineTo(0, y);
            while (x < w) {
                x += w / 8f;
                y = h * base - r.nextFloat() * h * amp;
                path.lineTo(x, y);
            }
            path.lineTo(w, h);
            path.close();
            p.setColor(color);
            c.drawPath(path, p);
        }

        void pines(Canvas c, int w, int h, float base, int color, int seed) {
            Random r = new Random(seed);
            p.setColor(color);
            for (int i = 0; i < 28; i++) {
                float x = r.nextFloat() * w, s = h * (0.08f + r.nextFloat() * 0.1f);
                float y = h * base;
                Path t = new Path();
                t.moveTo(x, y - s * 1.6f);
                t.lineTo(x - s * 0.45f, y);
                t.lineTo(x + s * 0.45f, y);
                t.close();
                c.drawPath(t, p);
            }
            c.drawRect(0, h * base, w, h, p);
        }

        void draw(Canvas c, int w, int h, int s, int alpha) {
            c.saveLayerAlpha(0, 0, w, h, alpha);
            switch (s) {
                case 0: // sunset mountains
                    sky(c, w, h, 0xFF2B1B4D, 0xFFFF9A5A);
                    p.setColor(0xFFFFE2A8); c.drawCircle(w * 0.7f, h * 0.55f, h * 0.09f, p);
                    ridge(c, w, h, 0.72f, 0.25f, 0xFF5A3A6B, 1);
                    ridge(c, w, h, 0.82f, 0.18f, 0xFF3A2650, 2);
                    ridge(c, w, h, 0.92f, 0.12f, 0xFF1E1230, 3);
                    break;
                case 1: // misty forest
                    sky(c, w, h, 0xFF9BC7B4, 0xFFE8F1E6);
                    ridge(c, w, h, 0.7f, 0.2f, 0xFF7FAE98, 4);
                    pines(c, w, h, 0.78f, 0xFF3F7A5C, 5);
                    pines(c, w, h, 0.9f, 0xFF1F4A38, 6);
                    break;
                case 2: // ocean
                    sky(c, w, h, 0xFF4FA3D9, 0xFFBFE6F5);
                    p.setColor(0xFFFFF4C2); c.drawCircle(w * 0.3f, h * 0.38f, h * 0.07f, p);
                    p.setColor(0xFF1E78B4); c.drawRect(0, h * 0.55f, w, h, p);
                    p.setColor(0x33FFFFFF);
                    for (int i = 0; i < 9; i++) c.drawRect(w * (i % 3) * 0.3f, h * (0.6f + i * 0.04f), w * (0.2f + (i % 3) * 0.3f), h * (0.61f + i * 0.04f), p);
                    break;
                default: // night hills
                    sky(c, w, h, 0xFF05071A, 0xFF1B2B54);
                    p.setColor(Color.WHITE);
                    Random r = new Random(9);
                    for (int i = 0; i < 90; i++) c.drawCircle(r.nextFloat() * w, r.nextFloat() * h * 0.6f, 1 + r.nextFloat() * 2, p);
                    p.setColor(0xFFF2F0D8); c.drawCircle(w * 0.8f, h * 0.2f, h * 0.06f, p);
                    ridge(c, w, h, 0.8f, 0.15f, 0xFF0F1A33, 7);
                    ridge(c, w, h, 0.92f, 0.1f, 0xFF070C1C, 8);
            }
            c.restore();
        }
    }
}
