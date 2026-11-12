package com.thanksmister.iot.wallpanellegacy.root;

import android.content.Context;
import android.graphics.PixelFormat;
import android.net.LocalServerSocket;
import android.os.Build;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;

/** Root-only orientation window for the Samsung J5 Android 6 kiosk. */
public final class SamsungLandscapeGuard {
    private static LocalServerSocket singleton;
    public static void main(String[] args) throws Exception {
        if (!"SM-J510FN".equals(Build.MODEL) || Build.VERSION.SDK_INT != 23) return;
        try {
            singleton = new LocalServerSocket("wallpanel-samsung-landscape");
        } catch (java.io.IOException alreadyRunning) {
            return;
        }
        Looper.prepareMainLooper();
        Class<?> activityThread = Class.forName("android.app.ActivityThread");
        Object thread = activityThread.getMethod("systemMain").invoke(null);
        Context context = (Context) activityThread.getMethod("getSystemContext").invoke(thread);
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        View anchor = new View(context);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.screenOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        params.alpha = 0;
        params.setTitle("Samsung panel landscape guard");
        manager.addView(anchor, params);
        // No timer, sensor, input capture or polling: Binder removes the window
        // automatically when this process exits. Existing display helpers own light/power.
        Looper.loop();
    }
}
