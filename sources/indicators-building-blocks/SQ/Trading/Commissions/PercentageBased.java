package SQ.Trading.Commissions;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Parameter;

@ClassConfig(name = "Percentage based", display = "#CommissionPct# % of equity")
@Help("<b>Percentage based commissions</b><br/>Used mainly for stocks, it computes commission as % of actual price of purchased asset.")
public class PercentageBased extends CommissionsMethod {
   @Parameter(defaultValue = "0", minValue = -100.0, name = "Commission", maxValue = 100.0, step = 1.0, category = "Default", decimals = 4)
   @Help("Commission in % of price per full lot (5 means 5%)")
   public double CommissionPct;

   public double computeCommissionsOnOpen(ILiveOrder var1, double var2, double var4) throws Exception {
      double var6 = this.CommissionPct / 100.0;
      double var8 = var1.getSize() * var1.getOpenPrice() * var4;
      return var6 * var8;
   }

   public double computeCommissionsOnClose(ILiveOrder var1, double var2, double var4) throws Exception {
      return 0.0;
   }
}
