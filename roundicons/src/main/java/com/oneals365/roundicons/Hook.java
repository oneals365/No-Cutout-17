package com.oneals365.roundicons;

import android.graphics.Path;
import android.graphics.drawable.AdaptiveIconDrawable;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Replaces the adaptive icon mask with a circle inside each scoped app.
 *
 * AdaptiveIconDrawable keeps the system mask in a static field (sMask, in a
 * 100x100 viewport) and copies it into each icon (mMask) before scaling it to
 * the icon's bounds. One UI ignores config_icon_mask, so the overlay approach
 * doesn't work there; overwriting the mask in-process does.
 */
public class Hook implements IXposedHookLoadPackage {

    private static final String TAG = "RoundIcons17: ";
    private static final String SELF = "com.oneals365.roundicons";

    private static Path circle() {
        Path p = new Path();
        p.addCircle(50f, 50f, 50f, Path.Direction.CW);
        return p;
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

        // 1) Shared mask used for every icon created from now on.
        try {
            XposedHelpers.setStaticObjectField(AdaptiveIconDrawable.class, "sMask", circle());
        } catch (Throwable t) {
            XposedBridge.log(TAG + "sMask not found in " + lpparam.packageName + ": " + t);
        }

        // 2) Icons created before we ran, or that copied the mask already: fix per instance.
        try {
            XposedBridge.hookAllConstructors(AdaptiveIconDrawable.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try { XposedHelpers.setObjectField(param.thisObject, "mMask", circle()); }
                    catch (Throwable ignored) { }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "constructor hook failed: " + t);
        }

        // 3) Anyone asking for the icon shape directly (launchers, badges) gets a circle.
        try {
            XposedHelpers.findAndHookMethod(AdaptiveIconDrawable.class, "getIconMask", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try { XposedHelpers.setStaticObjectField(AdaptiveIconDrawable.class, "sMask", circle()); }
                    catch (Throwable ignored) { }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "getIconMask hook failed: " + t);
        }
    }
}
