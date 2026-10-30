#!/system/bin/sh

# Keep ADB reachable over the trusted home Wi-Fi after every boot. Android 4.1
# does not reliably preserve `adb tcpip`, so set both the persistent and active
# properties before restarting adbd. This old ROM may leave the USB transport
# offline while adbd is in TCP mode; Wi-Fi is the persistent management path.
setprop persist.service.adb.enable 1
setprop persist.service.adb.tcp.port 5555
setprop service.adb.tcp.port 5555
stop adbd
start adbd

# Wait until Android's activity and window services are ready.
WAIT_COUNT=0
while [ "$WAIT_COUNT" -lt 60 ]; do
    if service check activity 2>/dev/null | busybox grep -q found; then
        break
    fi
    sleep 1
    WAIT_COUNT=$((WAIT_COUNT + 1))
done

# The kiosk helper leaves SystemUI disabled so Android 4.1 cannot resurrect its
# touch-triggered bar. Restore the package at the beginning of every boot; ADB
# is already available above if manual recovery is ever needed.
pm enable com.android.systemui >/dev/null 2>&1
am startservice -n com.android.systemui/.SystemUIService >/dev/null 2>&1

# Give the launcher, keyguard, and WallPanel boot receiver time to settle.
sleep 12

# Android 4.1 briefly animates its tablet SystemBar back into view after every
# touch, even when the foreground app immediately requests HIDE_NAVIGATION.
# Disable those legacy transition animations for a stable kiosk surface.
sqlite3 /data/data/com.android.providers.settings/databases/settings.db \
    "update system set value='0.0' where name in ('window_animation_scale','transition_animation_scale');"

# Denver's Android 4.1 ROM shows the insecure swipe keyguard even when disabled.
# Dismiss it only when it is actually the focused window.
if dumpsys window | busybox grep -q 'mCurrentFocus=.*Keyguard'; then
    input keyevent 82
    sleep 2
fi

# Open the legacy browser directly. Reusing WelcomeActivity's existing task can
# restore SettingsActivity instead, which has no camera surface for motion wake.
am start -n com.thanksmister.iot.wallpanellegacy/.ui.activities.BrowserActivityLegacy

# `install-recovery.sh` runs as a one-shot init service on this ROM. Put each
# long-lived helper in a new session so init does not kill it when this script
# exits after boot.

# Keep Android logically awake for WallPanel's camera, but switch the LCD
# panel fully off whenever WallPanel enters its minimum-brightness state.
if [ -x /system/xbin/wallpanel-backlight ]; then
    busybox setsid /system/xbin/wallpanel-backlight </dev/null \
        >/data/local/tmp/wallpanel-backlight-boot.log 2>&1 &
fi

# Recover the dashboard after a low-memory kill and recycle Crosswalk before
# its gradual memory leak can exhaust this tablet's limited RAM.
if [ -x /system/xbin/wallpanel-watchdog ]; then
    busybox setsid /system/xbin/wallpanel-watchdog </dev/null \
        >/data/local/tmp/wallpanel-watchdog-boot.log 2>&1 &
fi

# Reclaim RAM and background I/O from apps that are unnecessary while the
# dedicated Home Assistant dashboard is running.
if [ -x /system/xbin/wallpanel-runtime-optimizer ]; then
    busybox setsid /system/xbin/wallpanel-runtime-optimizer </dev/null \
        >/data/local/tmp/wallpanel-runtime-optimizer-boot.log 2>&1 &
fi

# Preserve Android's original long-press power menu while the physical PMIC
# key is remapped to CAMERA for camera-safe short presses.
if [ -x /system/xbin/wallpanel-power-menu ]; then
    busybox setsid /system/xbin/wallpanel-power-menu </dev/null \
        >/data/local/tmp/wallpanel-power-menu-boot.log 2>&1 &
fi

# Android 4.1's SystemBar is revealed by every application touch. Keep the
# SystemUI service stopped during kiosk use, while allowing WallPanel's battery
# badge to expose it temporarily through explicit show/hide triggers.
if [ -x /system/xbin/wallpanel-systemui ]; then
    busybox setsid /system/xbin/wallpanel-systemui </dev/null \
        >/data/local/tmp/wallpanel-systemui-boot.log 2>&1 &
fi

# The rooted firmware mounts /system read-write during boot. The installed
# panel services need no further writes there, so restore it to read-only.
busybox mount -o remount,ro /dev/block/nandd /system 2>/dev/null || true

# Keep this init service alive and recover helpers that disappear. A stale PID
# file is not proof of a running helper: verify both the process and command.
# This also avoids depending solely on detached children of a one-shot service.
while true; do
    for HELPER_NAME in backlight watchdog runtime-optimizer power-menu systemui; do
        HELPER_PATH=/system/xbin/wallpanel-$HELPER_NAME
        HELPER_PID=$(cat /data/local/tmp/wallpanel-$HELPER_NAME.pid 2>/dev/null)
        if [ -n "$HELPER_PID" ] && kill -0 "$HELPER_PID" 2>/dev/null && \
                busybox grep -q "$HELPER_PATH" "/proc/$HELPER_PID/cmdline" 2>/dev/null; then
            continue
        fi
        if [ -x "$HELPER_PATH" ]; then
            busybox setsid "$HELPER_PATH" </dev/null \
                >"/data/local/tmp/wallpanel-$HELPER_NAME-boot.log" 2>&1 &
        fi
    done
    sleep 30
done
