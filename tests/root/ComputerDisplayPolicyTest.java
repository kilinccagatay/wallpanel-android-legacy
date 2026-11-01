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
        System.out.println("Computer display lease: fresh, grace, stale, missing and clock-skew passed.");
    }
}
