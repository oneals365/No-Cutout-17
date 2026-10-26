#!/system/bin/sh
MODDIR=${0%/*}
. "$MODDIR/common.sh"
mkdir -p "$NC_STATE"
if nc_is_on; then
  nc_off; touch "$NC_STATE/off"
  echo "No Cutout: OFF"
  echo "Apps now leave room for the camera hole again."
else
  rm -f "$NC_STATE/off"; nc_on
  if nc_is_on; then echo "No Cutout: ON"; echo "Apps now go full screen around the camera hole."
  else echo "Couldn't turn it on. Try again after a reboot."; fi
fi
