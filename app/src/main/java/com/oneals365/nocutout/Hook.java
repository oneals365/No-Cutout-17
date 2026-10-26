package com.oneals365.nocutout;

import android.view.Window;
import android.view.WindowManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Forces every app window in scope to lay out into the display cutout area.
 *
 * Samsung's per-app "Camera cutout" setting is easily overridden: an app that
 * sets its own cutout mode wins, and the setting can reset when the app
 * updates. This hook rewrites the window parameters inside the app process
 * every time a window is created or updated, so the app can't undo it.
 */
public class Hook implements IXposedHookLoadPackage {

    private static final String TAG = "NoCutout17: ";
    private static final String SELF = "com.oneals365.nocutout";

    /** WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS (API 30). */
    private static final int MODE_ALWAYS = 3;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        String pkg = lpparam.packageName;

        if (SELF.equals(pkg)) {
            hookSelf(lpparam);
            return;
        }
        // Never touch the framework or System UI even if ticked by accident.
        if ("android".equals(pkg) || "com.android.systemui".equals(pkg)) return;

        XC_MethodHook forceParams = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args == null) return;
                for (Object arg : param.args) {
                    if (arg instanceof WindowManager.LayoutParams) {
                        apply((WindowManager.LayoutParams) arg);
                    }
                }
            }
        };

        // App sets its own window attributes (most common way apps opt out).
        try {
            XposedHelpers.findAndHookMethod(Window.class, "setAttributes",
                    WindowManager.LayoutParams.class, forceParams);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "setAttributes hook failed in " + pkg + ": " + t);
        }

        // Every window the app adds or updates passes through WindowManagerGlobal.
        try {
            Class<?> wmg = XposedHelpers.findClass("android.view.WindowManagerGlobal", lpparam.classLoader);
            XposedBridge.hookAllMethods(wmg, "addView", forceParams);
            XposedBridge.hookAllMethods(wmg, "updateViewLayout", forceParams);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "WindowManagerGlobal hook failed in " + pkg + ": " + t);
        }
    }

    /** Only app windows (activities, dialogs, popups); leave system overlays alone. */
    private static void apply(WindowManager.LayoutParams lp) {
        if (lp.type >= WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW
                && lp.type <= WindowManager.LayoutParams.LAST_SUB_WINDOW) {
            lp.layoutInDisplayCutoutMode = MODE_ALWAYS;
        }
    }

    /** Lets the app's own screen show whether the framework loaded the module. */
    private static void hookSelf(XC_LoadPackage.LoadPackageParam lpparam) {
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
    }
}
