#!/system/bin/sh
# Samsung J5 (2016) only. Run once per boot; no polling daemon or APK changes.
[ "$(getprop ro.product.model)" = "SM-J510FN" ] || exit 0
[ "$(getprop ro.build.version.sdk)" = "23" ] || exit 0
attempt=0
while [ "$(getprop sys.boot_completed)" != "1" ]; do
    attempt=$((attempt + 1))
    [ "$attempt" -ge 60 ] && exit 1
    sleep 2
done
settings put system accelerometer_rotation 0
settings put system user_rotation 1
