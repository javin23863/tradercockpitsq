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

@ClassConfig(name = "Fixed amount", display = "Fixed amount: #RiskedMoney# $")
@Help(
   "<b>Strategy will risk given amount of money for every trade</b><br/>This is basic money management without compounding. It can be used to test real performance of strategies whose Stop Loss is based on ATR."
)
@SortOrder(400)
@Description("Fixed amount, #RiskedMoney# $")
@ForEngine("*,-SP,-SA")
public class FixedAmount extends MoneyManagementMethod {
   @Parameter(defaultValue = "500", minValue = 1.0, name = "RiskedMoney", maxValue = 1000000.0, step = 100.0, category = "Default")
   @Help("Risk in $")
   public double RiskedMoney;
   @Parameter(defaultValue = "1", minValue = 0.0, name = "Size Decimals", maxValue = 6.0, step = 1.0, category = "Default")
   @Help("Order size will be rounded to the selected number of decimal places. Use 2 for microlots, 1 for mini lots and 0 for stocks and futures.")
   public int Decimals;
   @Parameter(name = "Size if no MM", category = "Lots", defaultValue = "0.1", minValue = 0.0, maxValue = 1.0E9, step = 0.1)
   @Help("How many lots should be traded if we disable or cannot use Money Management - for example when computed trade size is 0")
   public double LotsIfNoMM;
   @Parameter(name = "Maximum lots", category = "Lots", defaultValue = "0.5", minValue = 0.01, maxValue = 1.0E9, step = 0.1)
   @Help("The biggest lot size allowed")
   public double MaxLots;

   public double computeTradeSize(StrategyBase var1, String var2, byte var3, double var4, double var6, double var8, double var10, double var12) throws Exception {
      if (this.RiskedMoney < 0.0) {
         throw new Exception("Money management wasn't properly initialized. Call init() method before computing trade size!");
      }

      double var14 = var4 > 0.0 ? var4 : (OrderTypes.isLongOrder(var3) ? var1.MarketData.Chart(var2).Ask() : var1.MarketData.Chart(var2).Bid());
      double var16 = 0.0;
      if (var6 > 0.0) {
         double var18 = Math.abs(var14 - var6) / var8;
         double var20 = var18 * var8 * var10;
         double var22 = this.RiskedMoney * this.weight / var20;
         var16 = SQUtils.round7(var22);
         var16 = this.round(var16, var12, this.Decimals);
         if (var16 <= 0.0) {
            var16 = this.LotsIfNoMM;
         }

         if (var16 > this.MaxLots) {
            var16 = this.MaxLots;
         }

         return var16;
      } else {
         return Math.min(this.LotsIfNoMM, this.MaxLots);
      }
   }
}
