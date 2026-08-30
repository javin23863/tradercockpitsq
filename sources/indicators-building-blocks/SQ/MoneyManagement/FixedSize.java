package SQ.MoneyManagement;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Description;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;

@ClassConfig(name = "Fixed size", display = "Fixed size: #Size# lots")
@Help("<b>No money management</b><br/>Strategy will trade with fixed number of lots.")
@Description("Fixed size, #Size# lots")
@SortOrder(100)
@ForEngine("*,-SP,-SA")
public class FixedSize extends MoneyManagementMethod {
   @Parameter(defaultValue = "0.1", minValue = 0.01, name = "Order size", maxValue = 1000000.0, step = 0.1, category = "Default")
   @Help("Order size (number of lots for forex)")
   public double Size;

   public double computeTradeSize(StrategyBase var1, String var2, byte var3, double var4, double var6, double var8, double var10, double var12) throws Exception {
      return this.Size * this.weight;
   }
}
