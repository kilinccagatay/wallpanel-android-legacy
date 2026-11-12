#!/system/bin/sh
# Samsung only. HTTP recovery does not own display/camera/power state.
RECOVERY_PENDING=0
FAILURES=0
SUCCESSES=0
LAST_RECOVERY=-300

recovery_policy() {
    ACTION=none
    case "$HTTP_CODE" in
        200)
            FAILURES=0
            SUCCESSES=$((SUCCESSES + 1))
            [ "$SUCCESSES" -gt 2 ] && SUCCESSES=2
            if [ "$RECOVERY_PENDING" -eq 1 ] && [ "$SUCCESSES" -ge 2 ] && \
               [ "$CAN_RELOAD" = yes ] && [ $((NOW - LAST_RECOVERY)) -ge 300 ]; then
                ACTION=reload
            fi
            ;;
        *)
            SUCCESSES=0
            [ "$FAILURES" -lt 2 ] && FAILURES=$((FAILURES + 1))
            if [ "$HTTP_CODE" = 404 ] || [ "$FAILURES" -ge 2 ]; then
                RECOVERY_PENDING=1
            fi
            ;;
    esac
}

recovery_main() {
    [ "$(getprop ro.product.model)" = SM-J510FN ] || exit 1
    [ "$(getprop ro.build.version.sdk)" = 23 ] || exit 1
    BB=/data/adb/magisk/busybox
    APP=com.thanksmister.iot.wallpanellegacy.samsung
    URL_FILE=/data/local/tmp/samsung-http-recovery.url
    LOG=/data/local/tmp/samsung-http-recovery.log
    [ -x "$BB" ] && [ -x /system/bin/curl ] || exit 1
    while [ "$(getprop sys.boot_completed)" != 1 ]; do sleep 2; done
    while true; do
        URL=$(cat "$URL_FILE" 2>/dev/null)
        case "$URL" in http://*|https://*) ;; *) sleep 30; continue ;; esac
        HTTP_CODE=$(/system/bin/curl --head --silent --max-time 5 --noproxy '*' \
            --output /dev/null --write-out '%{http_code}' "$URL" 2>/dev/null)
        NOW=$("$BB" awk '{print int($1)}' /proc/uptime)
        CAN_RELOAD=no
        if [ "$RECOVERY_PENDING" = 1 ] && [ "$HTTP_CODE" = 200 ]; then
            # Never wake a panel put to sleep by the Mac or physical power key.
            # Android Settings, permission prompts and other apps are left alone.
            if dumpsys power | "$BB" grep -q 'mWakefulness=Awake' && \
               dumpsys window windows | "$BB" grep 'mCurrentFocus=' | \
                   "$BB" grep -q "$APP/.*BrowserActivity"; then
                CAN_RELOAD=yes
            fi
        fi
        recovery_policy
        if [ "$ACTION" = reload ]; then
            echo "$(date) http_recovered: reloading_samsung_panel" >> "$LOG"
            am force-stop "$APP" >/dev/null 2>&1
            sleep 2
            # WelcomeActivity is the verified Samsung launch path. Do not use
            # Denver's direct BrowserActivityLegacy launch or package name.
            if am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
                -n "$APP/com.thanksmister.iot.wallpanellegacy.ui.activities.WelcomeActivity" >/dev/null 2>&1; then
                RECOVERY_PENDING=0
            fi
            LAST_RECOVERY=$NOW
            SIZE=$("$BB" wc -c < "$LOG")
            if [ "$SIZE" -gt 65536 ]; then
                "$BB" tail -100 "$LOG" > "$LOG.tmp" && "$BB" mv "$LOG.tmp" "$LOG"
            fi
        fi
        sleep 30
    done
}

[ "$1" = --library ] || recovery_main
