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
        if(ComputerDisplayPolicy.shouldSleep(ComputerDisplayPolicy.Presence.OFFLINE,true,true,115,29999)) throw new AssertionError("off grace");
        if(!ComputerDisplayPolicy.shouldSleep(ComputerDisplayPolicy.Presence.OFFLINE,true,true,115,30000)) throw new AssertionError("camera must not postpone computer off");
        if(!ComputerDisplayPolicy.shouldSleep(ComputerDisplayPolicy.Presence.OFFLINE,true,true,0,0)) throw new AssertionError("already dimmed closes immediately");
        for(ComputerDisplayPolicy.Presence p:new ComputerDisplayPolicy.Presence[]{ComputerDisplayPolicy.Presence.ONLINE,ComputerDisplayPolicy.Presence.GRACE,ComputerDisplayPolicy.Presence.UNKNOWN})
            if(ComputerDisplayPolicy.shouldSleep(p,true,true,0,99999)) throw new AssertionError("only confirmed offline closes");
        if(ComputerDisplayPolicy.shouldSleep(ComputerDisplayPolicy.Presence.OFFLINE,false,true,115,99999)) throw new AssertionError("already sleeping");
        if(ComputerDisplayPolicy.shouldSleep(ComputerDisplayPolicy.Presence.OFFLINE,true,false,115,99999)) throw new AssertionError("do not close settings or permission pages");
        if(ComputerDisplayPolicy.shouldSleep(ComputerDisplayPolicy.Presence.OFFLINE,true,true,-1,29999)) throw new AssertionError("unknown brightness is not zero");
        if(!ComputerDisplayPolicy.shouldSleep(ComputerDisplayPolicy.Presence.OFFLINE,true,true,-1,30000)) throw new AssertionError("brightness read failure must not suppress confirmed off");
        for(ComputerDisplayPolicy.Presence p:ComputerDisplayPolicy.Presence.values()) {
            if(ComputerDisplayPolicy.shouldWake(false,p)) throw new AssertionError("never own manual sleep");
            if(ComputerDisplayPolicy.shouldWake(true,p)!=(p==ComputerDisplayPolicy.Presence.ONLINE)) throw new AssertionError("only confirmed online may wake; not outages");
        }
        System.out.println("Computer display lease: fresh, grace, stale, missing and clock-skew passed.");
    }
}
