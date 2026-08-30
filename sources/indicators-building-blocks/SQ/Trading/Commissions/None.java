package SQ.Trading.Commissions;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.ILiveOrder;

@ClassConfig(name = "None", display = "No commissions")
public class None extends CommissionsMethod {
   public double computeCommissionsOnOpen(ILiveOrder var1, double var2, double var4) throws Exception {
      return 0.0;
   }

   public double computeCommissionsOnClose(ILiveOrder var1, double var2, double var4) throws Exception {
      return 0.0;
   }
}
