package com.oneals365.hidestatusbar;

import android.view.View;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Makes System UI's status bar views fully transparent, so they stay hidden no matter
 * what resets icon settings. The views still exist and keep their size, so
 * pulling down the shade and touch handling are untouched.
 *
 * One UI 9 (Android 17) class names, from SystemUI.apk:
 *  - PhoneStatusBarView: the status bar over apps and home
 *  - KeyguardStatusBarView: lock screen / shade header (carrier name, battery)
 */
public class Hook implements IXposedHookLoadPackage {

    private static final String TAG = "HideStatusBar17: ";
    private static final String SELF = "com.oneals365.hidestatusbar";
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

        // transitionAlpha multiplies with normal alpha but is only used by transitions,
        // which System UI doesn't run on these views, so setting it to 0 keeps them
        // invisible without any per-frame hook.
        XC_MethodHook hide = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.thisObject instanceof View) ((View) param.thisObject).setTransitionAlpha(0f);
            }
        };
        String[][] hooks = {
                {"com.android.systemui.statusbar.phone.PhoneStatusBarView", "onFinishInflate", "onAttachedToWindow"},
                {"com.android.systemui.statusbar.phone.KeyguardStatusBarView", "onFinishInflate", "onLayout"},
        };
        for (String[] h : hooks) {
            Class<?> c;
            try { c = XposedHelpers.findClass(h[0], lpparam.classLoader); }
            catch (Throwable t) { XposedBridge.log(TAG + "missing " + h[0]); continue; }
            for (int i = 1; i < h.length; i++) {
                try { XposedBridge.hookAllMethods(c, h[i], hide); }
                catch (Throwable t) { XposedBridge.log(TAG + "no " + h[0] + "." + h[i]); }
            }
        }
    }
}
