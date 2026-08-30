package SQ.Formulas.Range;

import SQ.Internal.FormulaBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;

@Formula(order = 300, name = "ATR-based value", formula = "Range")
public class ATRBasedValue extends FormulaBlock {
   @Parameter(defaultValue = "1", minValue = 0.01, builderMinValue = 1.0, builderMaxValue = 5.0, maxValue = 9999.0, step = 0.1, postfix = "* ATR(")
   public double Value;
   @Parameter(defaultValue = "20", minValue = 1.0, maxValue = 9999.0, step = 5.0, postfix = ")")
   public int AtrPeriod;
   private int cachedBars = -1;
   private StrategyBase cachedStrategy = null;
   private double cachedATR;
   private SettingsMap params = new SettingsMap();

   public double evaluateFormula(StrategyBase var1, String var2, double var3, int var5) throws TradingException {
      if (this.Value == 0.0) {
         return 0.0;
      }

      double var6 = 0.0;
      if (this.Strategy.isStockpicker()) {
         this.params.set("TimePeriod", this.AtrPeriod);
         byte var8 = 0;
         float[][] var9 = this.Strategy.Stockpicker.TALibIndicators.calculate("ATR", this.params, this.Strategy, 1, var8, null);
         if (var9 != null) {
            int var10 = this.Strategy.Stockpicker.getCurrentBar(0);
            var6 = var9[var8][var10 > 0 ? var10 - 1 : var10];
         }
      } else {
         ChartData var11 = var1.MarketData.Chart(var2);
         var6 = this.getCachedATR(var11, var1);
      }

      double var12 = this.Value * var6;
      return SQUtils.round(var12, 5);
   }

   private double getCachedATR(ChartData var1, StrategyBase var2) throws TradingException {
      int var3 = var1.Bars();
      if (var3 != this.cachedBars || this.cachedStrategy != var2) {
         this.cachedBars = var3;
         this.cachedStrategy = var2;
         this.cachedATR = SQUtils.round6(var2.getATRValue(var1, this.AtrPeriod, 1));
      }

      return this.cachedATR;
   }
}
