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

@ClassConfig(name = "Risk fixed % of account", display = "Risk fixed % of account: #Risk#%")
@Help(
   "<b>Risk fixed percentage of account size</b><br/>Strategy will risk a given % of equity for every trade.<br/><br/>This is a simple but very effective money management that will allow the strategy to increase the number of lots as your account grows.<br/><br/>It is recommended to risk around 2-5% of the account per trade."
)
@Description("Risk #Risk#% of account")
@SortOrder(300)
@ForEngine("*,-SP,-SA")
public class RiskFixedPctOfAccount extends MoneyManagementMethod {
   public static final Logger Log = LoggerFactory.getLogger("RiskFixedPctOfAccount");
   @Parameter(name = "Risk in %", category = "Risk", defaultValue = "5", minValue = 0.1, maxValue = 100000.0, step = 0.1)
   @Help("How big percentage of your account will be risked in every trade?")
   public double Risk;
   @Parameter(defaultValue = "1", minValue = 0.0, name = "Size Decimals", maxValue = 6.0, step = 1.0, category = "Default")
   @Help("Order size will be rounded to the selected number of decimal places. Use 2 for microlots, 1 for mini lots and 0 for stocks and futures.")
   public int Decimals;
   @Parameter(name = "StopLoss in pips", category = "Risk", defaultValue = "100", minValue = 0.0, maxValue = 1000000.0, step = 10.0)
   @Help("If order doesn't have SL then this predefined StopLoss will be used to compute the correct trade size according to money management")
   public int StopLossInPips;
   @Parameter(name = "Size if no MM", category = "Lots", defaultValue = "1", minValue = 0.0, maxValue = 1.0E9, step = 0.1)
   @Help("How many lots should be traded if we disable or cannot use Money Management - for example when computed trade size is 0")
   public double LotsIfNoMM;
   @Parameter(name = "Maximum lots", category = "Lots", defaultValue = "5", minValue = 0.01, maxValue = 1.0E9, step = 0.1)
   @Help("The biggest lot size allowed")
   public double MaxLots;

   public double computeTradeSize(StrategyBase var1, String var2, byte var3, double var4, double var6, double var8, double var10, double var12) throws Exception {
      if (this.Risk < 0.0) {
         throw new Exception("Money management wasn't properly initialized. Call init() method before computing trade size!");
      }

      double var14 = 0.0;
      double var16;
      if (var6 > 0.0) {
         double var18 = var4 > 0.0 ? var4 : (OrderTypes.isLongOrder(var3) ? var1.MarketData.Chart(var2).Ask() : var1.MarketData.Chart(var2).Bid());
         var16 = Math.abs(var18 - var6) / var8;
      } else {
         var16 = this.StopLossInPips;
      }

      double var26 = SQUtils.round7(var16 * var8 * var10);
      double var20 = this.Risk * this.weight;
      if (var20 > 100.0) {
         var20 = 100.0;
      }

      double var22 = SQUtils.round7(var1.getAccountEquity() * (var20 / 100.0));
      var14 = SQUtils.round7(var22 / var26);
      var14 = this.round(var14, var12, this.Decimals);
      if (var14 <= 0.0) {
         var14 = this.LotsIfNoMM;
      }

      if (var14 > this.MaxLots) {
         var14 = this.MaxLots;
      }

      return var14;
   }
}
