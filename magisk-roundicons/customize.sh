ui_print "- Round Icons 17"
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
. "$MODPATH/common.sh"
rm -f "$RI_STATE/off"
ri_on
if ri_is_on; then ui_print "- Round Icons is ON. Reboot to see it everywhere."
else ui_print "- Will turn on at next boot"; fi
ui_print "- Toggle: Modules > Round Icons 17 > Action button"
