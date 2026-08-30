package SQ.MoneyManagement;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Description;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Crypto fixed % balance", display = "Crypto fixed % balance: #Risk#%")
@Help("<b>Crypto fixed percentage of balance</b>")
@Description("Crypto #Risk#% of balance")
@SortOrder(200)
@ForEngine("MC")
public class CryptoFixedBalancePct extends MoneyManagementMethod {
   public static final Logger Log = LoggerFactory.getLogger("CryptoFixedBalancePct");
   @Parameter(name = "Risk in %", defaultValue = "5", minValue = 0.1, maxValue = 100000.0, step = 0.1)
   @Help("How big percentage of your account will be risked in every trade?")
   public double Risk;
   @Parameter(defaultValue = "1", minValue = 0.0, name = "Size Decimals", maxValue = 6.0, step = 1.0, category = "Default")
   @Help("Order size will be rounded to the selected number of decimal places.")
   public int Decimals;

   public double computeTradeSize(StrategyBase var1, String var2, byte var3, double var4, double var6, double var8, double var10, double var12) throws Exception {
      if (this.Risk < 0.0) {
         throw new Exception("Money management wasn't properly initialized. Call init() method before computing trade size!");
      }

      if (var1.isEngineDefined() && var1.getEngine() != 938213070) {
         throw new Exception("Crypto fixed % balance money management method is available for Multicharts engine only");
      }

      double var14 = 0.0;
      double var16 = this.Risk * this.weight;
      if (var16 > 100.0) {
         var16 = 100.0;
      }

      double var18 = SQUtils.round2(var1.getAccountBalance() * var16 / 100.0);
      double var20 = var4 > 0.0 ? var4 : (OrderTypes.isLongOrder(var3) ? var1.MarketData.Chart(var2).Ask() : var1.MarketData.Chart(var2).Bid());
      return this.round(var18 / var20, var12, this.Decimals);
   }
}
