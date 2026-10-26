package com.oneals365.roundicons;

import android.graphics.Path;
import android.graphics.RectF;
import android.content.res.Resources;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Makes app icons round on One UI.
 *
 * Two masks decide icon shape:
 *  - AdaptiveIconDrawable.sMask: AOSP adaptive icons.
 *  - android.app.SemAppIconSolution.MASK: Samsung's "icon tray" squircle, used by
 *    PackageManager.semGet*ForIconTray, which One UI Home, System UI and Settings
 *    call to fetch icons (it also wraps legacy icons).
 * Both are Path objects. Rather than replacing the fields (they may be final),
 * each Path is reshaped in place into a circle filling its own original bounds,
 * so whatever coordinate system Samsung uses is kept.
 */
public class Hook implements IXposedHookLoadPackage {

    private static final String TAG = "RoundIcons17: ";
    private static final String SELF = "com.oneals365.roundicons";
    private static final Set<Path> DONE = Collections.newSetFromMap(new WeakHashMap<>());

    /** Reshape a mask path into the circle inscribed in its bounds (once per Path). */
    private static void roundInPlace(Object o) {
        if (!(o instanceof Path)) return;
        Path p = (Path) o;
        synchronized (DONE) {
            if (DONE.contains(p)) return;
            RectF b = new RectF();
            p.computeBounds(b, true);
            if (b.width() <= 0 || b.height() <= 0) return;
            float r = Math.min(b.width(), b.height()) / 2f;
            float cx = b.centerX(), cy = b.centerY();
            p.reset();
            p.addCircle(cx, cy, r, Path.Direction.CW);
            DONE.add(p);
        }
    }

    private static Class<?> semSolution;

    private static void roundAll() {
        try { roundInPlace(XposedHelpers.getStaticObjectField(AdaptiveIconDrawable.class, "sMask")); }
        catch (Throwable ignored) { }
        if (semSolution != null) {
            try { roundInPlace(XposedHelpers.getStaticObjectField(semSolution, "MASK")); }
            catch (Throwable ignored) { }
        }
    }

    private static final XC_MethodHook ROUND_FIRST = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) { roundAll(); }
    };

    private static void hookAll(Class<?> c, String method) {
        try { XposedBridge.hookAllMethods(c, method, ROUND_FIRST); }
        catch (Throwable t) { XposedBridge.log(TAG + "no " + c.getSimpleName() + "." + method); }
    }

    /** Resource id -> whether it's a squircle icon background (e.g. notification_icon_squircle). */
    private static final Map<Integer, Boolean> SQUIRCLE_IDS = new HashMap<>();

    private static boolean isSquircleIcon(Resources res, int id) {
        if (id == 0) return false;
        synchronized (SQUIRCLE_IDS) {
            Boolean known = SQUIRCLE_IDS.get(id);
            if (known != null) return known;
            boolean yes = false;
            try {
                String name = res.getResourceEntryName(id).toLowerCase();
                yes = name.contains("icon") && (name.contains("squircle")
                        || (name.startsWith("noti") && (name.endsWith("_bg") || name.endsWith("icon_background"))));
            } catch (Throwable ignored) { }
            SQUIRCLE_IDS.put(id, yes);
            return yes;
        }
    }

    /** First argument that looks like a resource id (package byte set), else 0. */
    private static int resId(Object[] args) {
        if (args == null) return 0;
        for (Object a : args) {
            if (a instanceof Integer && (((Integer) a) >>> 24) != 0) return (Integer) a;
        }
        return 0;
    }

    private static final XC_MethodHook WRAP_SQUIRCLE = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            Object result = param.getResult();
            if (!(result instanceof Drawable) || result instanceof CircleClipDrawable) return;
            int id = resId(param.args);
            if (id != 0 && isSquircleIcon((Resources) param.thisObject, id)) {
                param.setResult(new CircleClipDrawable((Drawable) result));
            }
        }
    };

    /**
     * Wrap squircle icon backgrounds (notification small-icon backgrounds etc.) in a circular clip.
     * Resources.loadDrawable is the common path for code (getDrawable) and layouts/styles
     * (TypedArray), so hooking it catches backgrounds set in XML too.
     */
    private static void hookSquircleDrawables() {
        for (String m : new String[]{"loadDrawable", "getDrawableForDensity"}) {
            try { XposedBridge.hookAllMethods(Resources.class, m, WRAP_SQUIRCLE); }
            catch (Throwable t) { XposedBridge.log(TAG + "drawable hook " + m + " failed: " + t); }
        }
    }

    private static final ViewOutlineProvider OVAL = new ViewOutlineProvider() {
        @Override
        public void getOutline(View v, Outline o) { o.setOval(0, 0, v.getWidth(), v.getHeight()); }
    };

    /** System UI: notification small icons get their squircle background in NotificationIconManager. */
    private static void hookNotificationIcons(ClassLoader cl) {
        Class<?> mgr;
        try { mgr = XposedHelpers.findClass("com.android.systemui.notification.NotificationIconManager", cl); }
        catch (Throwable t) { return; }
        XC_MethodHook clip = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.args == null) return;
                for (Object a : param.args) {
                    if (a instanceof ImageView) {
                        ImageView v = (ImageView) a;
                        v.setOutlineProvider(OVAL);
                        v.setClipToOutline(true);
                        v.invalidateOutline();
                    }
                }
            }
        };
        for (String m : new String[]{"updateIconColorAndBackground", "setNonGrayScaleIconBackground", "updateSmallIcon"}) {
            try { XposedBridge.hookAllMethods(mgr, m, clip); }
            catch (Throwable t) { XposedBridge.log(TAG + "no NotificationIconManager." + m); }
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (SELF.equals(lpparam.packageName)) {
            try {
                XposedHelpers.findAndHookMethod(SELF + ".MainActivity", lpparam.classLoader,
                        "isModuleActive", new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                param.setResult(Boolean.TRUE);
                            }
                        });
            } catch (Throwable t) {
                XposedBridge.log(TAG + "self hook failed: " + t);
            }
            return;
        }

        try {
            semSolution = XposedHelpers.findClass("android.app.SemAppIconSolution", lpparam.classLoader);
        } catch (Throwable t) {
            semSolution = null; // not a Samsung build
        }

        roundAll();
        hookSquircleDrawables();
        if ("com.android.systemui".equals(lpparam.packageName)) hookNotificationIcons(lpparam.classLoader);

        // AOSP adaptive icons: masks are (re)built in the constructor and on bounds changes.
        try { XposedBridge.hookAllConstructors(AdaptiveIconDrawable.class, ROUND_FIRST); }
        catch (Throwable t) { XposedBridge.log(TAG + "ctor hook failed: " + t); }
        hookAll(AdaptiveIconDrawable.class, "updateMaskBoundsInternal");
        hookAll(AdaptiveIconDrawable.class, "getIconMask");

        // Samsung icon tray: every entry point that hands out tray-packed icons.
        try {
            Class<?> apm = XposedHelpers.findClass("android.app.ApplicationPackageManager", lpparam.classLoader);
            hookAll(apm, "semGetApplicationIconForIconTray");
            hookAll(apm, "hidden_semGetApplicationIconForIconTray");
            hookAll(apm, "semGetActivityIconForIconTray");
            hookAll(apm, "semGetDrawableForIconTray");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "ApplicationPackageManager hooks failed: " + t);
        }
        if (semSolution != null) {
            hookAll(semSolution, "maskDrawable");
            hookAll(semSolution, "getInstance");
        }
    }
}
