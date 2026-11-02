import com.thanksmister.iot.wallpanellegacy.root.ComputerDisplayPolicy;
public final class ComputerDisplayPolicyTest {
    private static void expect(long now,long seen,ComputerDisplayPolicy.Presence want){
        if(ComputerDisplayPolicy.presence(now,seen,120)!=want) throw new AssertionError(now+" / "+seen);
    }
    public static void main(String[] args){
        expect(1000,995,ComputerDisplayPolicy.Presence.ONLINE);
        expect(1000,960,ComputerDisplayPolicy.Presence.ONLINE);
        expect(1000,959,ComputerDisplayPolicy.Presence.GRACE);
        expect(1000,881,ComputerDisplayPolicy.Presence.GRACE);
        expect(1000,880,ComputerDisplayPolicy.Presence.OFFLINE);
        expect(1000,0,ComputerDisplayPolicy.Presence.UNKNOWN);
        expect(1000,1100,ComputerDisplayPolicy.Presence.UNKNOWN);
        expect(1000,1010,ComputerDisplayPolicy.Presence.ONLINE);
        if(ComputerDisplayPolicy.presence(1000,995,120,false)!=ComputerDisplayPolicy.Presence.OFFLINE) throw new AssertionError("fresh explicit lock");
        if(ComputerDisplayPolicy.presence(1000,950,120,false)!=ComputerDisplayPolicy.Presence.OFFLINE) throw new AssertionError("lock grace");
        if(ComputerDisplayPolicy.presence(1000,995,120,true)!=ComputerDisplayPolicy.Presence.ONLINE) throw new AssertionError("fresh unlock");
        if(ComputerDisplayPolicy.presence(1000,950,120,true)!=ComputerDisplayPolicy.Presence.GRACE) throw new AssertionError("stale active is not fresh");
        if(ComputerDisplayPolicy.presence(1000,0,120,false)!=ComputerDisplayPolicy.Presence.UNKNOWN) throw new AssertionError("no report");
        if(ComputerDisplayPolicy.computerPresence(1000,950,120,"unavailable")!=ComputerDisplayPolicy.Presence.GRACE) throw new AssertionError("HA expiry must not wake an owned screen");
        if(ComputerDisplayPolicy.computerPresence(1000,880,120,"unavailable")!=ComputerDisplayPolicy.Presence.OFFLINE) throw new AssertionError("shutdown fallback");
        if(ComputerDisplayPolicy.computerPresence(1000,995,120,"unknown")!=ComputerDisplayPolicy.Presence.UNKNOWN) throw new AssertionError("unknown is not active");
        System.out.println("Computer display lease: fresh, grace, stale, missing and clock-skew passed.");
    }
}
