package SQ.MoneyManagement;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Description;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "ATR Risk-Based Sizing", display = "ATR Risk-Based Sizing: Risk #Risk#% of equity, Stop = #ATRMult# × ATR(#ATRPeriod#)")
@Help("<b>ATR Risk-Based Position Sizing</b><br>Adjusts trade size so each trade risks a fixed % of equity based on ATR volatility.")
@Description("Risk-based position sizing using ATR.")
@SortOrder(300)
@ForEngine("*,-SP,-SA")
public class ATRRiskBasedSizing extends MoneyManagementMethod {
   public static final Logger Log = LoggerFactory.getLogger("ATRRiskBasedSizing");
   @Parameter(name = "Risk in %", category = "Risk", defaultValue = "2", minValue = 0.1, maxValue = 100000.0, step = 0.1)
   @Help("Percent of account equity to risk per trade")
   public double Risk;
   @Parameter(defaultValue = "14", minValue = 2.0, name = "ATR Period", maxValue = 480.0, step = 1.0, category = "ATR")
   @Help("ATR lookback period")
   public int ATRPeriod;
   @Parameter(defaultValue = "2.0", minValue = 0.1, name = "ATR Multiplier", maxValue = 10.0, step = 0.1, category = "ATR")
   @Help("ATR multiple for stop distance")
   public double ATRMult;
   @Parameter(defaultValue = "1", minValue = 0.0, name = "Size Decimals", maxValue = 6.0, step = 1.0, category = "Default")
   @Help("Order size rounding decimals. Use 2 for microlots, 1 for mini lots, 0 for stocks/futures.")
   public int Decimals;
   @Parameter(name = "Size if no MM", category = "Lots", defaultValue = "1", minValue = 0.0, maxValue = 1.0E9, step = 0.1)
   @Help("Lots traded if computed trade size is 0 or MM disabled")
   public double LotsIfNoMM;
   @Parameter(name = "Maximum lots", category = "Lots", defaultValue = "5", minValue = 0.01, maxValue = 1.0E9, step = 0.1)
   @Help("Maximum lot size allowed")
   public double MaxLots;

   public double computeTradeSize(StrategyBase var1, String var2, byte var3, double var4, double var6, double var8, double var10, double var12) throws Exception {
      if (this.Risk < 0.0) {
         throw new Exception("Money management wasn't properly initialized. Call init() before computing trade size!");
      }

      double var14 = var1.getATRValue(var1.MarketData.Chart(var2), this.ATRPeriod, 1);
      double var16 = var14 * this.ATRMult / var8;
      double var18 = SQUtils.round7(var16 * var8 * var10);
      double var20 = this.Risk * this.weight;
      if (var20 > 100.0) {
         var20 = 100.0;
      }

      double var22 = SQUtils.round7(var1.getAccountEquity() * (var20 / 100.0));
      double var24 = SQUtils.round7(var22 / var18);
      var24 = this.round(var24, var12, this.Decimals);
      if (var24 <= 0.0) {
         var24 = this.LotsIfNoMM;
      }

      if (var24 > this.MaxLots) {
         var24 = this.MaxLots;
      }

      return var24;
   }
}
