package com.thanksmister.iot.wallpanellegacy.root;

import android.os.SystemClock;
import android.view.KeyEvent;

import java.lang.reflect.Method;

/** Injects a trusted held POWER key when run as root through app_process. */
public final class PowerLongPress {
    private static final int INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2;
    private static final long HOLD_MILLIS = 1200L;

    private PowerLongPress() {
    }

    public static void main(String[] args) throws Exception {
        Class<?> inputManagerClass = Class.forName("android.hardware.input.InputManager");
        Class<?> inputEventClass = Class.forName("android.view.InputEvent");
        Object inputManager = inputManagerClass.getMethod("getInstance").invoke(null);
        Method inject = inputManagerClass.getMethod(
                "injectInputEvent", inputEventClass, Integer.TYPE);

        long downTime = SystemClock.uptimeMillis();
        KeyEvent down = new KeyEvent(
                downTime, downTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_POWER, 0);
        inject.invoke(inputManager, down, INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);

        Thread.sleep(HOLD_MILLIS);

        long upTime = SystemClock.uptimeMillis();
        KeyEvent up = new KeyEvent(
                downTime, upTime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_POWER, 0);
        inject.invoke(inputManager, up, INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
    }
}
