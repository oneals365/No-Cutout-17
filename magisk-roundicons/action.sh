#!/system/bin/sh
MODDIR=${0%/*}
. "$MODDIR/common.sh"
mkdir -p "$RI_STATE"
if ri_is_on; then
  ri_off; touch "$RI_STATE/off"
  echo "Round Icons: OFF"
else
  rm -f "$RI_STATE/off"; ri_on
  if ri_is_on; then echo "Round Icons: ON"; else echo "Couldn't turn it on. Try again after a reboot."; fi
fi
echo "Reboot to see the new icon shape everywhere."
