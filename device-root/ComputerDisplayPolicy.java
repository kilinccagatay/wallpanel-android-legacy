package com.thanksmister.iot.wallpanellegacy.root;

/** Epoch-based presence lease. Meeting on/off is deliberately irrelevant. */
public final class ComputerDisplayPolicy {
    public enum Presence { ONLINE, GRACE, OFFLINE, UNKNOWN }
    /** Camera activity must not postpone explicit computer-off indefinitely. */
    public static boolean shouldSleep(Presence presence, boolean interactive,
                                      boolean panelForeground, int brightness,
                                      long offlineElapsedMillis) {
        return presence == Presence.OFFLINE && interactive && panelForeground
                && (brightness == 0 || offlineElapsedMillis >= 30000);
    }
    public static Presence computerPresence(long now, long seen, long staleAfter, String state) {
        if("on".equals(state)) return presence(now,seen,staleAfter,Boolean.TRUE);
        if("off".equals(state)) return presence(now,seen,staleAfter,Boolean.FALSE);
        Presence lease=presence(now,seen,staleAfter);
        // HA expires availability before the shutdown fallback. During that
        // interval preserve an owned sleeping screen; unavailable is not a wake.
        return lease==Presence.ONLINE?Presence.UNKNOWN:lease;
    }
    public static Presence presence(long now, long lastReport, long staleAfter) {
        return presence(now, lastReport, staleAfter, null);
    }
    public static Presence presence(long now, long lastReport, long staleAfter, Boolean computerActive) {
        long age = now - lastReport;
        if (lastReport <= 0 || age < -30) return Presence.UNKNOWN;
        if (age <= 40) return Boolean.FALSE.equals(computerActive) ? Presence.OFFLINE : Presence.ONLINE;
        if (age < staleAfter && Boolean.FALSE.equals(computerActive)) return Presence.OFFLINE;
        return age >= staleAfter ? Presence.OFFLINE : Presence.GRACE;
    }
}
