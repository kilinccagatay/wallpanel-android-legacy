package com.thanksmister.iot.wallpanellegacy.root;

import android.content.Intent;

import java.lang.reflect.Method;

/** Starts or stops Android's SystemUI service from a rooted app_process. */
public final class SystemUiControl {
    private static final String SYSTEMUI_PACKAGE = "com.android.systemui";
    private static final String SYSTEMUI_SERVICE = "com.android.systemui.SystemUIService";

    private SystemUiControl() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !("start".equals(args[0]) || "stop".equals(args[0]))) {
            throw new IllegalArgumentException("Expected start or stop");
        }

        Intent intent = new Intent();
        intent.setClassName(SYSTEMUI_PACKAGE, SYSTEMUI_SERVICE);
        if ("start".equals(args[0])) {
            throw new UnsupportedOperationException("Use am startservice for start");
        } else {
            Class<?> activityManagerNative = Class.forName("android.app.ActivityManagerNative");
            Object activityManager = activityManagerNative.getMethod("getDefault").invoke(null);
            Class<?> applicationThread = Class.forName("android.app.IApplicationThread");
            Method stopService = activityManager.getClass().getMethod(
                    "stopService", applicationThread, Intent.class, String.class);
            stopService.invoke(activityManager, new Object[] { null, intent, null });
        }
    }
}
