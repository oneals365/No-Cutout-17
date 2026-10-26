#!/system/bin/sh
cmd overlay disable --user 0 com.android.shell:NoCutout17 </dev/null >/dev/null 2>&1
rm -rf /data/adb/nocutout17
