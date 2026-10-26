# Shared helpers. "cmd" must never write to a terminal from a root context
# (system_server can't use the fds), so stdin/out are always redirected.
NC_ID=com.android.shell:NoCutout17
NC_STATE=/data/adb/nocutout17

nc_list() { cmd overlay list android </dev/null 2>/dev/null; }
nc_has()  { nc_list | grep -q "$NC_ID"; }
nc_is_on() { nc_list | grep -q "\[x\] $NC_ID"; }

# Android builds the overlay itself (a "fabricated" overlay), so it skips the
# APK allowlist that rejects module overlays on Samsung Android 17.
nc_create() {
  cmd overlay fabricate --target android --name NoCutout17 \
    android:string/config_mainBuiltInDisplayCutout 0x03 "" </dev/null >/dev/null 2>&1
}
nc_on()  { nc_has || nc_create; cmd overlay enable --user 0 "$NC_ID" </dev/null >/dev/null 2>&1; }
nc_off() { cmd overlay disable --user 0 "$NC_ID" </dev/null >/dev/null 2>&1; }
