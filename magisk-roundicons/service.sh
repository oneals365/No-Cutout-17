#!/system/bin/sh
MODDIR=${0%/*}
. "$MODDIR/common.sh"
until [ "$(getprop sys.boot_completed)" = "1" ]; do sleep 2; done
sleep 5
[ -f "$RI_STATE/off" ] || ri_on
