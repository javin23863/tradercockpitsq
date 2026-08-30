package SQ.Blocks.Indicators.QQE;

import SQ.Calculators.AverageCalculator;
import SQ.Calculators.RSICalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;

@BuildingBlock(name = "(QQE) Quantitative Qualitative Estimation", display = "QQE(@Chart@#RSIPeriod#)[#Shift#]", returnType = 1)
@Indicator(oscillator = true, middleValue = 50.0, min = 0.0, max = 100.0, step = 1.0)
@ParameterSet(set = "RSIPeriod=14,sF=5,wF=4.236")
public class QQE extends IndicatorBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(category = "Default", name = "RSIPeriod", minValue = 2.0, maxValue = 10000.0, defaultValue = "14", step = 1.0, isPeriod = true)
   public int RSIPeriod;
   @Parameter(
      category = "Default",
      name = "sF",
      defaultValue = "5",
      minValue = 2.0,
      maxValue = 650.0,
      step = 1.0,
      builderMinValue = 1.0,
      builderMaxValue = 650.0,
      builderStep = 1.0,
      isPeriod = true
   )
   public int sF;
   @Parameter(
      category = "Default",
      name = "wF",
      defaultValue = "4.236",
      minValue = 0.1,
      maxValue = 100.0,
      builderMinValue = 0.01,
      builderMaxValue = 100.0,
      builderStep = 0.025
   )
   public double wF;
   @Output(name = "QQE Value1", color = "#008000")
   public DataSeries Value1;
   @Output(name = "QQE Value2", color = "#FF0000")
   public DataSeries Value2;
   private RSICalculator rsiCalculator;
   private AverageCalculator rsiEmaCalculator;
   private AverageCalculator atrRsiEmaCalculator;
   private AverageCalculator maAtrRsiEmaCalculator;
   private int WildersPeriod;

   protected void OnInit() throws TradingException {
      this.WildersPeriod = this.RSIPeriod * 2 - 1;
      this.rsiCalculator = new RSICalculator(this.RSIPeriod);
      this.rsiEmaCalculator = new AverageCalculator(1, this.sF);
      this.atrRsiEmaCalculator = new AverageCalculator(1, this.WildersPeriod);
      this.maAtrRsiEmaCalculator = new AverageCalculator(1, this.WildersPeriod);
   }

   protected void OnBarUpdate() throws TradingException {
      this.rsiCalculator.onBarUpdate(this.Chart.Close.get(0), this.getCurrentBar());
      this.rsiEmaCalculator.onBarUpdate(this.rsiCalculator.getValue(), this.getCurrentBar());
      double var7 = this.Value2.get(1);
      double var3 = this.Value1.get(1);
      double var1 = this.rsiEmaCalculator.getValue();
      double var11 = Math.abs(var3 - var1);
      this.atrRsiEmaCalculator.onBarUpdate(var11, this.getCurrentBar());
      this.maAtrRsiEmaCalculator.onBarUpdate(this.atrRsiEmaCalculator.getValue(), this.getCurrentBar());
      double var5 = this.maAtrRsiEmaCalculator.getValue() * this.wF;
      double var9 = var7;
      if (var1 < var7) {
         var7 = var1 + var5;
         if (var3 < var9 && var7 > var9) {
            var7 = var9;
         }
      } else if (var1 > var7) {
         var7 = var1 - var5;
         if (var3 > var9 && var7 < var9) {
            var7 = var9;
         }
      }

      this.Value1.set(0, var1);
      this.Value2.set(0, var7);
   }
}
