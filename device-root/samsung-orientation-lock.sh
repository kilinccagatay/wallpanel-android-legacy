#!/system/bin/sh
# Samsung J5 (2016) only; install before long-running service.d helpers.
[ "$(getprop ro.product.model)" = "SM-J510FN" ] || exit 0
[ "$(getprop ro.build.version.sdk)" = "23" ] || exit 0
# Return immediately so boot and later helpers cannot block each other.
(
    attempt=0
    while [ "$(getprop sys.boot_completed)" != "1" ]; do
        attempt=$((attempt + 1))
        [ "$attempt" -ge 60 ] && exit 1
        sleep 2
    done
    settings put system accelerometer_rotation 0
    settings put system user_rotation 1
    export CLASSPATH=/data/local/tmp/samsung-landscape.jar
    exec app_process /system/bin com.thanksmister.iot.wallpanellegacy.root.SamsungLandscapeGuard
) >/data/local/tmp/samsung-orientation-boot.log 2>&1 </dev/null &
exit 0
