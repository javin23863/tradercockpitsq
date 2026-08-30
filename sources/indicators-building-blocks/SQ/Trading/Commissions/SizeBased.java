package SQ.Trading.Commissions;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Parameter;

@ClassConfig(name = "Size based", display = "$ #Commission# per full lot")
@Help(
   "<b>Simple size-based commissions</b><br/>Simple method of commissions computation - it uses commissions in $ per lot and multiplies it by actual trade size."
)
public class SizeBased extends CommissionsMethod {
   @Parameter(defaultValue = "0", minValue = -100000.0, name = "Commission", maxValue = 100000.0, step = 1.0, category = "Default")
   @Help("Commission in $ per complete lot")
   public double Commission;

   public double computeCommissionsOnOpen(ILiveOrder var1, double var2, double var4) throws Exception {
      return this.Commission * var1.getSize();
   }

   public double computeCommissionsOnClose(ILiveOrder var1, double var2, double var4) throws Exception {
      return 0.0;
   }
}
