package SQ.Formulas.RangeLevel;

import SQ.Internal.FormulaBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SLPTValue;
import com.strategyquant.tradinglib.StrategyBase;

@Formula(order = 300, name = "ATR-based value", formula = "RangeLevel")
public class ATRBasedValue extends FormulaBlock {
   @Parameter(defaultValue = "1", minValue = 0.01, builderMinValue = 1.0, builderMaxValue = 5.0, maxValue = 9999.0, step = 0.1, postfix = "* ATR(")
   @SLPTValue(-2000)
   public double Value;
   @Parameter(defaultValue = "20", minValue = 1.0, maxValue = 9999.0, step = 5.0, postfix = ")")
   @SLPTValue(-3000)
   public int AtrPeriod;
   private int cachedBars = -1;
   private StrategyBase cachedStrategy = null;
   private double cachedATR;

   public double evaluateFormula(StrategyBase var1, String var2, double var3, int var5) throws TradingException {
      if (this.Value == 0.0) {
         return -9.9999999E7;
      }

      ChartData var6 = var1.MarketData.Chart(var2);
      double var7 = SQUtils.round(this.getCachedATR(var6, var1), 6);
      double var9 = this.Value * var7;
      return var5 == 1 ? SQUtils.round(var3 - var9, 5) : SQUtils.round(var3 + var9, 5);
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
