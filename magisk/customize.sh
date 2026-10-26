ui_print "- No Cutout 17"
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
. "$MODPATH/common.sh"
rm -f "$NC_STATE/off"
nc_on
if nc_is_on; then ui_print "- No Cutout is ON now (no reboot needed)"
else ui_print "- Will turn on at next boot"; fi
ui_print "- Toggle: Modules > No Cutout 17 > Action button"
