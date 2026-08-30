package SQ.MoneyManagement;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Description;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;

@ClassConfig(name = "Crypto fixed amount", display = "Crypto fixed amount: #RiskedMoney# $")
@Help("<b>Strategy will risk given amount of money for every trade</b>")
@SortOrder(400)
@Description("Crypto fixed amount, #RiskedMoney# $")
@ForEngine("MC")
public class CryptoFixedAmount extends MoneyManagementMethod {
   @Parameter(defaultValue = "500", minValue = 1.0, name = "RiskedMoney", maxValue = 1000000.0, step = 100.0, category = "Default")
   @Help("Risk in $")
   public double RiskedMoney;
   @Parameter(defaultValue = "1", minValue = 0.0, name = "Size Decimals", maxValue = 6.0, step = 1.0, category = "Default")
   @Help("Order size will be rounded to the selected number of decimal places before multiplying")
   public int Decimals;

   public double computeTradeSize(StrategyBase var1, String var2, byte var3, double var4, double var6, double var8, double var10, double var12) throws Exception {
      if (this.RiskedMoney < 0.0) {
         throw new Exception("Money management wasn't properly initialized. Call init() method before computing trade size!");
      }

      if (var1.isEngineDefined() && var1.getEngine() != 938213070) {
         throw new Exception("Crypto Fixed Amount money management method is available for Multicharts engine only");
      }

      double var14 = var4 > 0.0 ? var4 : (OrderTypes.isLongOrder(var3) ? var1.MarketData.Chart(var2).Ask() : var1.MarketData.Chart(var2).Bid());
      double var16 = this.RiskedMoney * this.weight / var14;
      return this.round(var16, var12, this.Decimals);
   }
}
