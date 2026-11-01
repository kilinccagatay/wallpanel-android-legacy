#!/system/bin/sh
# Samsung J5 Android 6 only. Private configuration is provisioned separately.
[ "$(getprop ro.product.model)" = SM-J510FN ] || exit 0
[ "$(getprop ro.build.version.sdk)" = 23 ] || exit 0
[ -r /data/local/tmp/samsung-computer-display.json ] || exit 0
[ -r /data/local/tmp/samsung-computer-display.jar ] || exit 0
PIDFILE=/data/local/tmp/samsung-computer-display.pid
OLD=$(cat "$PIDFILE" 2>/dev/null)
if [ -n "$OLD" ] && kill -0 "$OLD" 2>/dev/null && grep -q '88-samsung-computer-display.sh' "/proc/$OLD/cmdline" 2>/dev/null; then exit 0; fi
(
    CHILD=
    stop() { [ -n "$CHILD" ] && kill "$CHILD" 2>/dev/null; [ -n "$CHILD" ] && wait "$CHILD" 2>/dev/null; rm -f "$PIDFILE"; exit 0; }
    trap stop HUP INT TERM
    attempt=0
    while [ "$(getprop sys.boot_completed)" != 1 ]; do
        attempt=$((attempt + 1)); [ "$attempt" -ge 60 ] && exit 1; sleep 2
    done
    export CLASSPATH=/data/local/tmp/samsung-computer-display.jar
    while true; do
        app_process /system/bin com.thanksmister.iot.wallpanellegacy.root.SamsungComputerDisplay &
        CHILD=$!; wait "$CHILD"
        sleep 10 & CHILD=$!; wait "$CHILD"
    done
) >/data/local/tmp/samsung-computer-display.log 2>&1 </dev/null &
echo $! > "$PIDFILE"
