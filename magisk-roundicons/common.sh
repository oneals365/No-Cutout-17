# cmd must not write to a terminal from a root context, so always redirect.
RI_ID=com.android.shell:RoundIcons
RI_STATE=/data/adb/roundicons17
RI_MASK="M50,0A50,50,0,1,1,50,100A50,50,0,1,1,50,0Z"

ri_list()  { cmd overlay list android </dev/null 2>/dev/null; }
ri_has()   { ri_list | grep -q "$RI_ID"; }
ri_is_on() { ri_list | grep -q "\[x\] $RI_ID"; }
ri_create() {
  cmd overlay fabricate --target android --name RoundIcons \
    android:string/config_icon_mask 0x03 "$RI_MASK" </dev/null >/dev/null 2>&1
}
ri_on()  { ri_has || ri_create; cmd overlay enable --user 0 "$RI_ID" </dev/null >/dev/null 2>&1; }
ri_off() { cmd overlay disable --user 0 "$RI_ID" </dev/null >/dev/null 2>&1; }
