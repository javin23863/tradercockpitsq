package SQ.Trading.Commissions;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Parameter;

@ClassConfig(name = "Per trade ", display = "$ #Commission# per trade")
@Help("<b>Per trade commission</b><br/>Applies commission to every trade entry and exit.")
public class PerTrade extends CommissionsMethod {
   @Parameter(defaultValue = "0", minValue = -100000.0, name = "Commission", maxValue = 100000.0, step = 1.0, category = "Default")
   @Help("Commission in $ per trade side")
   public double Commission;

   public double computeCommissionsOnOpen(ILiveOrder var1, double var2, double var4) throws Exception {
      return this.Commission;
   }

   public double computeCommissionsOnClose(ILiveOrder var1, double var2, double var4) throws Exception {
      return var1.isPendingOrder() ? 0.0 : this.Commission;
   }
}
