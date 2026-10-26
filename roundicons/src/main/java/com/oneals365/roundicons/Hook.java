package com.oneals365.roundicons;

import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;

import java.util.Collections;
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
