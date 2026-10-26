# No Cutout 17

A Vector / LSPosed module that forces apps to draw **full screen around the camera cutout**, in portrait and landscape. A replacement for *No Cutout Display*, which stopped working on Android 17.

Samsung's built-in *Settings → Display → Camera cutout* option is easy to lose: apps that set their own cutout mode override it, and the choice can reset when an app updates. This module rewrites each app window's cutout mode to **Always** every time a window is created or updated, so it sticks.

## Option 1 (recommended): Magisk / Alpha module — system-wide, like No Cutout Display
1. Download `NoCutout17-Magisk-vX.zip` from **Releases** and install it in Magisk/Alpha → Modules → Install from storage. It turns on straight away.
2. To switch it on/off, tap the **Action** button on No Cutout 17 in the Modules list. Your choice is remembered across reboots.

How it works: the module asks Android to build a "fabricated" overlay (`com.android.shell:NoCutout17`) that sets `config_mainBuiltInDisplayCutout` to empty, so the system treats the screen as having no camera hole and every app goes full screen. A regular overlay APK can't be used because Samsung's Android 17 only loads preinstalled overlays from its own allowlist. Because of this it doesn't appear under Developer options → Simulate display with cutout; use the Action button instead.

To turn it off from a terminal: `su -c 'cmd overlay disable --user 0 com.android.shell:NoCutout17 </dev/null >/dev/null 2>&1'`

## Option 2: Xposed module (per-app)
1. Download the latest APK from **Releases** (built automatically by GitHub Actions).
2. Install it, open **Vector → Modules → No Cutout 17**, and enable it.
3. Tick the apps you want full screen. No System Framework scope is needed.
4. Force stop those apps (or reboot). Open No Cutout 17 to check it says **Module active**.

## Notes
- System UI and Android System are always skipped, even if ticked.
- Only app windows (activities, dialogs, popups) are changed; system overlays are left alone.
- If an app puts controls under the camera hole, untick that app.
- Targets Android 11+ (tested target: Android 15–17, One UI). Uses the classic Xposed API, which Vector still supports.

## How it works
`Hook.java` hooks `Window.setAttributes`, `WindowManagerGlobal.addView` and `WindowManagerGlobal.updateViewLayout` inside each scoped app and sets `layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`.

The signing key in `keystore/` is committed on purpose so every build has the same signature and installs over the last one. It's a personal module key, not used for anything else.
