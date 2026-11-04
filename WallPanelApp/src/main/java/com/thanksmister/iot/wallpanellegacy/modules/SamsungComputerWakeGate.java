package com.thanksmister.iot.wallpanellegacy.modules;

import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;

/** No new Android permission. Denver returns before reading Samsung's lease. */
public final class SamsungComputerWakeGate {
    public static boolean blockAutomatic(Context context) {
        if (!ComputerWakeGatePolicy.PACKAGE.equals(context.getPackageName())) return false;
        try {
            return ComputerWakeGatePolicy.blocks(context.getPackageName(),
                    Settings.System.getString(context.getContentResolver(),ComputerWakeGatePolicy.SETTING),
                    SystemClock.elapsedRealtime());
        } catch (RuntimeException unavailable) { return false; }
    }
    @SuppressWarnings("deprecation")
    public static boolean blockBrightness(Context context) {
        if(!blockAutomatic(context)) return false;
        PowerManager pm=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
        // A real physical/manual wake remains usable even with the Mac offline.
        return pm!=null && !pm.isScreenOn();
    }
}
