package com.thanksmister.iot.wallpanellegacy.modules;

/** Short-lived root-owned sleep lease; no network, location or personal data. */
public final class ComputerWakeGatePolicy {
    public static final String PACKAGE = "com.thanksmister.iot.wallpanellegacy.samsung";
    public static final String SETTING = "wallpanel_samsung_computer_sleep_lease";
    public static boolean blocks(String packageName, String value, long elapsed) {
        if (!PACKAGE.equals(packageName) || value == null || value.length() > 48) return false;
        try {
            String[] pair=value.split(":",-1);
            if(pair.length!=2) return false;
            long issued=Long.parseLong(pair[0]),expiry=Long.parseLong(pair[1]);
            return issued>=0 && expiry>issued && expiry-issued<=45000
                    && elapsed>=issued && elapsed<expiry;
        } catch (NumberFormatException malformed) { return false; }
    }
}
