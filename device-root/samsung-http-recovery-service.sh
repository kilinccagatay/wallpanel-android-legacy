#!/system/bin/sh
# Install as /data/adb/service.d/93-samsung-http-recovery.sh. Samsung only.
[ "$(getprop ro.product.model)" = SM-J510FN ] || exit 0
[ "$(getprop ro.build.version.sdk)" = 23 ] || exit 0
BB=/data/adb/magisk/busybox
WORKER=/data/local/tmp/samsung-http-recovery.sh
PIDFILE=/data/local/tmp/samsung-http-recovery.pid
LOCKDIR=/data/local/tmp/samsung-http-recovery-lock
[ -x "$BB" ] && [ -r "$WORKER" ] || exit 1
running() {
    OLD=$(cat "$PIDFILE" 2>/dev/null)
    [ -n "$OLD" ] && kill -0 "$OLD" 2>/dev/null && \
        "$BB" grep -q '93-samsung-http-recovery.sh' "/proc/$OLD/cmdline" 2>/dev/null
}
running && exit 0
if ! mkdir "$LOCKDIR" 2>/dev/null; then
    # Allow an in-progress launcher to publish its PID before testing staleness.
    sleep 2
    running && exit 0
    rmdir "$LOCKDIR" 2>/dev/null || exit 0
    mkdir "$LOCKDIR" 2>/dev/null || exit 0
fi
(
    CHILD=
    stop() {
        [ -z "$CHILD" ] || kill "$CHILD" 2>/dev/null
        [ -z "$CHILD" ] || wait "$CHILD" 2>/dev/null
        rm -f "$PIDFILE"
        rmdir "$LOCKDIR" 2>/dev/null
        exit 0
    }
    trap stop HUP INT TERM
    while true; do
        /system/bin/sh "$WORKER" &
        CHILD=$!; wait "$CHILD"
        sleep 10 & CHILD=$!; wait "$CHILD"
    done
) </dev/null >/data/local/tmp/samsung-http-recovery-boot.log 2>&1 &
echo $! > "$PIDFILE"
