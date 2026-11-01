package com.thanksmister.iot.wallpanellegacy.root;

/** Epoch-based presence lease. Meeting on/off is deliberately irrelevant. */
public final class ComputerDisplayPolicy {
    public enum Presence { ONLINE, GRACE, OFFLINE, UNKNOWN }
    public static Presence presence(long now, long lastReport, long staleAfter) {
        long age = now - lastReport;
        if (lastReport <= 0 || age < -30) return Presence.UNKNOWN;
        if (age <= 40) return Presence.ONLINE;
        return age >= staleAfter ? Presence.OFFLINE : Presence.GRACE;
    }
}
