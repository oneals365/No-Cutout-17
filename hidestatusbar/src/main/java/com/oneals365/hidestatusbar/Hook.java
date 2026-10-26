package com.oneals365.hidestatusbar;

import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Stops System UI's status bar views from drawing, so they stay hidden no matter
 * what resets icon settings. The views still exist and keep their size, so
 * pulling down the shade and touch handling are untouched.
 *
 * One UI 9 (Android 17) class names, from SystemUI.apk:
 *  - PhoneStatusBarView: the status bar over apps and home
 *  - KeyguardStatusBarView: lock screen / shade header (carrier name, battery)
 *  - CarrierText: carrier name wherever it appears
 */
public class Hook implements IXposedHookLoadPackage {

    private static final String TAG = "HideStatusBar17: ";
    private static final String SELF = "com.oneals365.hidestatusbar";
    private static final String[] TARGETS = {
            "com.android.systemui.statusbar.phone.PhoneStatusBarView",
            "com.android.systemui.statusbar.phone.KeyguardStatusBarView",
            "com.android.keyguard.CarrierText",
    };
    private static Class<?>[] targets = new Class<?>[0];

    private static boolean isTarget(Object o) {
        for (Class<?> c : targets) if (c.isInstance(o)) return true;
        return false;
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
        if (!"com.android.systemui".equals(lpparam.packageName)) return;

        java.util.List<Class<?>> found = new java.util.ArrayList<>();
        for (String n : TARGETS) {
            try { found.add(XposedHelpers.findClass(n, lpparam.classLoader)); }
            catch (Throwable t) { XposedBridge.log(TAG + "missing " + n); }
        }
        targets = found.toArray(new Class<?>[0]);
        if (targets.length == 0) return;

        XC_MethodHook skipDraw = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (isTarget(param.thisObject)) param.setResult(null);
            }
        };
        // Containers: skip drawing their children. Leaf views (CarrierText): skip draw().
        try {
            XposedHelpers.findAndHookMethod(ViewGroup.class, "dispatchDraw", Canvas.class, skipDraw);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "dispatchDraw hook failed: " + t);
        }
        try {
            XposedHelpers.findAndHookMethod(View.class, "draw", Canvas.class, skipDraw);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "draw hook failed: " + t);
        }
    }
}
