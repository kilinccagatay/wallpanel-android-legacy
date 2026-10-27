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

# Give the launcher, keyguard, and WallPanel boot receiver time to settle.
sleep 12

# Denver's Android 4.1 ROM shows the insecure swipe keyguard even when disabled.
# Dismiss it only when it is actually the focused window.
if dumpsys window | busybox grep -q 'mCurrentFocus=.*Keyguard'; then
    input keyevent 82
    sleep 2
fi

# Open the legacy browser directly. Reusing WelcomeActivity's existing task can
# restore SettingsActivity instead, which has no camera surface for motion wake.
am start -n com.thanksmister.iot.wallpanellegacy/.ui.activities.BrowserActivityLegacy

# Keep Android logically awake for WallPanel's camera, but switch the LCD
# panel fully off whenever WallPanel enters its minimum-brightness state.
if [ -x /system/xbin/wallpanel-backlight ]; then
    busybox nohup /system/xbin/wallpanel-backlight \
        >/data/local/tmp/wallpanel-backlight-boot.log 2>&1 &
fi

# Recover the dashboard after a low-memory kill and recycle Crosswalk before
# its gradual memory leak can exhaust this tablet's limited RAM.
if [ -x /system/xbin/wallpanel-watchdog ]; then
    busybox nohup /system/xbin/wallpanel-watchdog \
        >/data/local/tmp/wallpanel-watchdog-boot.log 2>&1 &
fi

# Reclaim RAM and background I/O from apps that are unnecessary while the
# dedicated Home Assistant dashboard is running.
if [ -x /system/xbin/wallpanel-runtime-optimizer ]; then
    busybox nohup /system/xbin/wallpanel-runtime-optimizer \
        >/data/local/tmp/wallpanel-runtime-optimizer-boot.log 2>&1 &
fi

# The rooted firmware mounts /system read-write during boot. The installed
# panel services need no further writes there, so restore it to read-only.
busybox mount -o remount,ro /dev/block/nandd /system 2>/dev/null || true
