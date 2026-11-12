import com.thanksmister.iot.wallpanellegacy.modules.ComputerWakeGatePolicy;
public final class ComputerWakeGatePolicyTest {
 public static void main(String[] args){
  String pkg=ComputerWakeGatePolicy.PACKAGE;
  if(!ComputerWakeGatePolicy.blocks(pkg,"1000:31000",1000))throw new AssertionError("active lease");
  if(!ComputerWakeGatePolicy.blocks(pkg,"1000:31000",30999))throw new AssertionError("lease before expiry");
  if(ComputerWakeGatePolicy.blocks(pkg,"1000:31000",31000))throw new AssertionError("expired lease");
  if(ComputerWakeGatePolicy.blocks("com.thanksmister.iot.wallpanellegacy","1000:31000",2000))throw new AssertionError("Denver isolation");
  if(ComputerWakeGatePolicy.blocks(pkg,"1000:31000",999))throw new AssertionError("old boot / future lease");
  for(String value:new String[]{null,"","bad","1:999999","-1:100","100:50","9223372036854775807:1","1:2:3"})
   if(ComputerWakeGatePolicy.blocks(pkg,value,1000))throw new AssertionError("invalid lease");
  System.out.println("Computer wake gate: Samsung-only, expiry, reboot/future and corrupt leases PASS");
 }
}
